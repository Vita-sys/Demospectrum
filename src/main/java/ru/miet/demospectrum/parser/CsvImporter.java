package ru.miet.demospectrum.parser;

import ru.miet.demospectrum.dao.CityDAO;
import ru.miet.demospectrum.dao.RegionDAO;
import ru.miet.demospectrum.model.City;
import ru.miet.demospectrum.model.Region;
import ru.miet.demospectrum.service.ErrMsgLog;
import ru.miet.demospectrum.service.OperationLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CsvImporter {

    public static void main(String[] args) {
        try {
            ErrMsgLog errLog = new ErrMsgLog();
            OperationLogger logger = new OperationLogger("import");
            RegionDAO regionDAO = new RegionDAO();
            CityDAO cityDAO = new CityDAO();

            // Кэш регионов: "Хакасия" -> Region (id)
            Map<String, Region> regionCache = new HashMap<>();

            // 1. Открываем CSV из ресурсов
            InputStream is = CsvImporter.class.getResourceAsStream("/cities.csv");
            if (is == null) {
                System.err.println("❌ Файл cities.csv не найден в resources!");
                return;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8));

            // 2. Читаем построчно
            String line;
            int lineNumber = 0;
            int saved = 0;
            int skipped = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Пропускаем заголовок
                if (lineNumber == 1) continue;

                // Парсим CSV: "1","Абаза","Хакасия","52.65","90.08..."
                String[] parts = parseCsvLine(line);
                if (parts.length < 5) {
                    System.err.println("⚠ Строка " + lineNumber + ": мало колонок");
                    continue;
                }

                try {
                    int id = Integer.parseInt(parts[0]);
                    String name = parts[1];
                    String regionName = parts[2];
                    double lat = Double.parseDouble(parts[3]);
                    double lon = Double.parseDouble(parts[4]);

                    // Ищем город — может, уже есть (например, Зеленоград id=1)
                    City existing = cityDAO.getByName(name);
                    if (existing != null) {
                        skipped++;
                        continue;
                    }

                    // Регион: ищем в кэше или в БД
                    Region region = regionCache.get(regionName);
                    if (region == null) {
                        region = regionDAO.findOrCreate(regionName);
                        regionCache.put(regionName, region);
                    }

                    // Создаём город
                    City city = new City(name, lat, lon);
                    city.setRegionId(region.getId());

                    logger.start();
                    cityDAO.insert(city);
                    logger.stopAndLog(city.getId());
                    saved++;

                    if (saved % 100 == 0) {
                        System.out.println("Прогресс: " + saved + " городов");
                    }

                } catch (Exception e) {
                    errLog.addErrWithLog(e);
                    System.err.println("⚠ Строка " + lineNumber + ": " + e.getMessage());
                }
            }

            reader.close();

            // 3. Итоги
            System.out.println("=== ИМПОРТ ЗАВЕРШЁН ===");
            System.out.println("Всего строк: " + (lineNumber - 1));
            System.out.println("Сохранено городов: " + saved);
            System.out.println("Пропущено (уже есть): " + skipped);
            System.out.println("Регионов в кэше: " + regionCache.size());
            System.out.println("Всего ошибок: " + errLog.getErrCount());
            logger.printSummary();

        } catch (Exception e) {
            System.err.println("❌ Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Простой парсер CSV-строки с учётом кавычек.
     * "1","Абаза","Хакасия","52.65","90.08" → [1, Абаза, Хакасия, 52.65, 90.08]
     */
    private static String[] parseCsvLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }
}