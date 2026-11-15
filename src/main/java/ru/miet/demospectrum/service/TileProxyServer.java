package ru.miet.demospectrum.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.Executors;

public class TileProxyServer {
    private static HttpServer server;
    private static int actualPort = -1;
    private static final String CACHE_DIR = "tiles_cache";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // Возвращает номер порта, на котором запустился сервер
    public static synchronized int start() {
        if (server != null && actualPort != -1) {
            return actualPort;
        }
        try {
            Files.createDirectories(Paths.get(CACHE_DIR));
            // 0 означает, что Windows сама выдаст свободный порт
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/tiles/", new TileHandler());
            server.setExecutor(Executors.newFixedThreadPool(16));
            server.start();
            actualPort = server.getAddress().getPort();
            System.out.println("Прокси-сервер для карт запущен на http://127.0.0.1:" + actualPort);
        } catch (IOException e) {
            System.err.println("Ошибка запуска прокси-сервера: " + e.getMessage());
            e.printStackTrace();
        }
        return actualPort;
    }

    static class TileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "image/png");

            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 5) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }

            String zoom = parts[2];
            String x = parts[3];
            String y = parts[4].replace(".png", "");

            Path cacheFile = Paths.get(CACHE_DIR, zoom + "_" + x + "_" + y + ".png");

            if (Files.exists(cacheFile)) {
                byte[] data = Files.readAllBytes(cacheFile);
                sendImage(exchange, data);
                return;
            }

            byte[] data = downloadTile("https://tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png");
            if (data == null) {
                data = downloadTile("https://cartocdn.com/light_all/" + zoom + "/" + x + "/" + y + ".png");
            }

            if (data != null) {
                Files.write(cacheFile, data);
                sendImage(exchange, data);
            } else {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        }

        private byte[] downloadTile(String urlStr) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlStr))
                        // Честный User-Agent, как требует политика OSM
                        .header("User-Agent", "DemospectrumDesktopApp/1.0 (MIET Student Project)")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200) {
                    return response.body();
                } else {
                    System.err.println("Сервер карт вернул код " + response.statusCode() + " для " + urlStr);
                }
            } catch (Exception e) {
                System.err.println("Ошибка загрузки тайла " + urlStr + ": " + e.getMessage());
            }
            return null;
        }

        private void sendImage(HttpExchange exchange, byte[] data) throws IOException {
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.getResponseBody().close();
        }
    }
}