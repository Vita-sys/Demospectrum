package ru.miet.demospectrum.parser;

import ru.miet.demospectrum.dao.CityDAO;
import ru.miet.demospectrum.dao.PopulationDAO;
import ru.miet.demospectrum.model.City;
import ru.miet.demospectrum.model.PopulationRecord;
import ru.miet.demospectrum.service.ErrMsgLog;
import ru.miet.demospectrum.service.OperationLogger;

import java.util.List;

public class BulkPopulationParser {

    private static final int PAUSE_MS = 400;       // пауза между городами
    private static final int MAX_CITIES = 1117;    // максимум городов

    public static void main(String[] args) {
        try {
            ErrMsgLog errLog = new ErrMsgLog();
            OperationLogger cityLogger = new OperationLogger("city");
            OperationLogger recordLogger = new OperationLogger("record");

            CityDAO cityDAO = new CityDAO();
            PopulationDAO populationDAO = new PopulationDAO();
            PopulationParser parser = new PopulationParser();

            // 1. Загружаем все города
            List<City> cities = cityDAO.getAll();
            System.out.println("Всего городов: " + cities.size());

            int processedCities = 0;
            int totalRecords = 0;
            int emptyCities = 0;

            long startTime = System.currentTimeMillis();

            // 2. Проходим по каждому городу
            for (City city : cities) {
                if (processedCities >= MAX_CITIES) break;

                processedCities++;
                String url = "https://ru.wikipedia.org/wiki/" + city.getName();

                cityLogger.start();
                try {
                    List<PopulationRecord> records = parser.parse(url, city.getId());

                    if (records.isEmpty()) {
                        emptyCities++;
                    } else {
                        // Сохраняем в БД
                        for (PopulationRecord r : records) {
                            recordLogger.start();
                            try {
                                populationDAO.insert(r);
                                totalRecords++;
                                recordLogger.stop();
                            } catch (Exception e) {
                                recordLogger.stop();
                                errLog.addErrWithLog(e);
                            }
                        }
                    }
                    cityLogger.stop();
                } catch (Exception e) {
                    cityLogger.stop();
                    errLog.addErrWithLog(e);
                }

                // Прогресс каждые 10 городов
                if (processedCities % 10 == 0) {
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    System.out.printf("[%d/%d] Городов: %d, Записей: %d, Пусто: %d, Ошибок: %d, Время: %d сек%n",
                            processedCities, cities.size(), processedCities, totalRecords,
                            emptyCities, errLog.getErrCount(), elapsed);
                }

                // Пауза
                Thread.sleep(PAUSE_MS);
            }

            // 3. Итоги
            long totalTime = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println("=== ПАРСИНГ ЗАВЕРШЁН ===");
            System.out.println("Обработано городов: " + processedCities);
            System.out.println("Записей о населении: " + totalRecords);
            System.out.println("Пустых городов (нет данных): " + emptyCities);
            System.out.println("Всего ошибок: " + errLog.getErrCount());
            System.out.println("Общее время: " + totalTime + " сек");
            cityLogger.printSummary();
            recordLogger.printSummary();

        } catch (Exception e) {
            System.err.println("❌ Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}