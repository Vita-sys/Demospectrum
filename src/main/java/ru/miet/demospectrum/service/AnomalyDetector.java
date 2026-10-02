package ru.miet.demospectrum.service;

import ru.miet.demospectrum.model.PopulationRecord;

import java.util.ArrayList;
import java.util.List;

public class AnomalyDetector {

    // Порог аномалии: 5% (по заданию)
    private static final double ANOMALY_THRESHOLD = 5.0;

    /**
     * Находит аномальные изменения населения.
     * Записи должны быть отсортированы по cityId и year.
     */
    public List<PopulationRecord> detectAnomalies(List<PopulationRecord> records) {
        List<PopulationRecord> anomalies = new ArrayList<>();

        for (int i = 1; i < records.size(); i++) {
            PopulationRecord prev = records.get(i - 1);
            PopulationRecord curr = records.get(i);

            if (prev.getCityId() == curr.getCityId()) {
                double changePercent = calculateChangePercent(
                        prev.getPopulation(), curr.getPopulation());
                curr.setChangePercent(changePercent);

                if (Math.abs(changePercent) > ANOMALY_THRESHOLD) {
                    curr.setAnomaly(true);
                    anomalies.add(curr);
                }
            }
        }
        return anomalies;
    }

    /**
     * Вычисляет процент изменения населения.
     */
    public double calculateChangePercent(long previous, long current) {
        if (previous == 0) return 0;
        return ((double) (current - previous) / previous) * 100.0;
    }

    public double getThreshold() {
        return ANOMALY_THRESHOLD;
    }
}