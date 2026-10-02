package ru.miet.demospectrum.service;

import ru.miet.demospectrum.dao.PopulationDAO;
import ru.miet.demospectrum.model.PopulationRecord;

import java.util.List;

public class AnomalyProcessor {

    public static void main(String[] args) {
        try {
            ErrMsgLog errLog = new ErrMsgLog();
            OperationLogger detectLogger = new OperationLogger("detect");
            OperationLogger updateLogger = new OperationLogger("update");

            // 1. Загружаем записи из БД
            PopulationDAO dao = new PopulationDAO();
            List<PopulationRecord> records = dao.getAll();
            System.out.println("Загружено записей: " + records.size());

            // 2. Ищем аномалии
            AnomalyDetector detector = new AnomalyDetector();

            detectLogger.start();
            List<PopulationRecord> anomalies = detector.detectAnomalies(records);
            detectLogger.stop();

            System.out.println("Найдено аномалий: " + anomalies.size());
            System.out.println("Порог: " + detector.getThreshold() + "%");

            for (PopulationRecord r : anomalies) {
                System.out.printf("  Год: %d, Население: %d, Изменение: %.2f%%%n",
                        r.getYear(), r.getPopulation(), r.getChangePercent());
            }
            detectLogger.printSummary();

            // 3. Обновляем БД
            int updated = 0;
            for (PopulationRecord r : records) {
                if (r.getChangePercent() != 0) {
                    updateLogger.start();
                    try {
                        dao.updateAnomaly(r.getId(), r.getChangePercent(), r.isAnomaly());
                        updated++;
                    } catch (Exception e) {
                        errLog.addErrWithLog(e);
                    }
                    updateLogger.stop();
                }
            }

            System.out.println("✅ Обновлено в БД: " + updated);
            updateLogger.printSummary();

            System.out.println("=== Итоги ===");
            System.out.println("Всего ошибок: " + errLog.getErrCount());

        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}