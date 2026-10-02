package ru.miet.demospectrum.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.miet.demospectrum.dao.CityDAO;
import ru.miet.demospectrum.model.City;
import ru.miet.demospectrum.service.ErrMsgLog;
import ru.miet.demospectrum.service.OperationLogger;

import java.io.IOException;

public class CityParser {

    private static final String CITIES_LIST_URL = "https://ru.wikipedia.org/wiki/Список_городов_России";

    /**
     * Парсит города из Википедии и сохраняет в БД.
     * @param maxCities максимум городов (для теста — например, 10)
     * @return количество сохранённых городов
     */
    public int parseAndSave(int maxCities) throws IOException, java.sql.SQLException {
        ErrMsgLog errLog = new ErrMsgLog();
        OperationLogger logger = new OperationLogger("city");

        Document mainPage = Jsoup.connect(CITIES_LIST_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(15000)
                .maxBodySize(0)
                .get();

        // В таблице городов: строки со ссылками на города
        Elements rows = mainPage.select("table.standard.sortable tbody tr");

        CityDAO dao = new CityDAO();
        int saved = 0;

        for (int i = 1; i < rows.size() && saved < maxCities; i++) {
            Element row = rows.get(i);
            Elements cols = row.select("td");

            if (cols.size() > 2) {
                Element link = cols.get(2).selectFirst("a");
                if (link == null) continue;

                String cityName = link.text();
                String cityUrl = link.attr("abs:href");

                if (cityName.isEmpty() || cityUrl.isEmpty()) continue;

                logger.start();
                try {
                    double[] coords = getCoordinates(cityUrl);
                    if (coords == null) {
                        logger.stop();
                        continue;
                    }

                    City city = new City(cityName, coords[0], coords[1]);
                    dao.insert(city);
                    saved++;
                    logger.stopAndLog(saved);

                    System.out.println("✓ " + cityName + " (" + coords[0] + ", " + coords[1] + ")");

                    Thread.sleep(300); // пауза, чтобы не забанили
                } catch (Exception e) {
                    logger.stop();
                    errLog.addErrWithLog(e);
                    System.err.println("✗ " + cityName + ": " + e.getMessage());
                }
            }
        }

        logger.printSummary();
        System.out.println("Сохранено городов: " + saved);
        System.out.println("Всего ошибок: " + errLog.getErrCount());

        return saved;
    }

    /**
     * Получает координаты города со страницы Википедии.
     */
    private double[] getCoordinates(String cityUrl) {
        try {
            Document cityPage = Jsoup.connect(cityUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(10000)
                    .get();

            // Вариант 1: элемент с data-lat и data-lon
            Element mapElement = cityPage.selectFirst("[data-lat][data-lon]");
            if (mapElement != null) {
                return new double[]{
                        Double.parseDouble(mapElement.attr("data-lat")),
                        Double.parseDouble(mapElement.attr("data-lon"))
                };
            }

            // Вариант 2: элемент .geo
            Element geoElement = cityPage.selectFirst(".geo");
            if (geoElement != null) {
                String[] parts = geoElement.text().split(";");
                if (parts.length == 2) {
                    return new double[]{
                            Double.parseDouble(parts[0].trim()),
                            Double.parseDouble(parts[1].trim())
                    };
                }
            }
        } catch (Exception e) {
            // тихо — вернём null
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        CityParser parser = new CityParser();
        // Для теста — 10 городов. Потом увеличим до 100.
        parser.parseAndSave(10);
    }
}