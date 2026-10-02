package ru.miet.demospectrum.parser;

import ru.miet.demospectrum.dao.PopulationDAO;
import ru.miet.demospectrum.model.PopulationRecord;
import ru.miet.demospectrum.service.ErrMsgLog;
import ru.miet.demospectrum.service.OperationLogger;

import java.util.List;

public class SavePopulationToDB {

    public static void main(String[] args) {
        try {
            // 1. Создаём обработчик ошибок и логгеры
            ErrMsgLog errLog = new ErrMsgLog();
            OperationLogger parseLogger = new OperationLogger("parse");
            OperationLogger saveLogger = new OperationLogger("save");

            System.out.println("=== Начало работы ===");

            // 2. Парсинг
            PopulationParser parser = new PopulationParser();
            String url = "https://ru.wikipedia.org/wiki/Зеленоград";
            int cityId = 1;

            parseLogger.start();
            List<PopulationRecord> records = parser.parse(url, cityId);
            parseLogger.stop();

            System.out.println("Спарсено записей: " + records.size());
            parseLogger.printSummary();

            // 3. Сохранение в БД
            PopulationDAO dao = new PopulationDAO();
            int saved = 0;

            for (PopulationRecord record : records) {
                saveLogger.start();
                try {
                    dao.insert(record);
                    saved++;
                    saveLogger.stopAndLog(record.getYear());
                } catch (Exception e) {
                    saveLogger.stop();
                    errLog.addErrWithLog(e);
                    errLog.showErrText(e);
                }
            }

            System.out.println("✅ Сохранено в БД: " + saved + " из " + records.size());
            saveLogger.printSummary();

            // 4. Итоги
            System.out.println("=== Итоги ===");
            System.out.println("Всего ошибок: " + errLog.getErrCount());

        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}