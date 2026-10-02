package ru.miet.demospectrum.service;

import ru.miet.demospectrum.model.PopulationRecord;

import java.util.List;

public class StatisticsCalculator {

    /**
     * Средний процент изменения по всем записям.
     */
    public double calculateAverageChange(List<PopulationRecord> records) {
        if (records.size() < 2) return 0;

        double sum = 0;
        int count = 0;

        for (int i = 1; i < records.size(); i++) {
            PopulationRecord prev = records.get(i - 1);
            PopulationRecord curr = records.get(i);

            if (prev.getCityId() == curr.getCityId() && prev.getPopulation() > 0) {
                double change = ((double) (curr.getPopulation() - prev.getPopulation())
                        / prev.getPopulation()) * 100.0;
                sum += change;
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }

    /**
     * Находит максимальное изменение населения (по модулю).
     */
    public double findMaxChange(List<PopulationRecord> records) {
        double max = 0;
        for (PopulationRecord r : records) {
            if (Math.abs(r.getChangePercent()) > Math.abs(max)) {
                max = r.getChangePercent();
            }
        }
        return max;
    }

    /**
     * Находит минимальное изменение населения (по модулю).
     */
    public double findMinChange(List<PopulationRecord> records) {
        double min = Double.MAX_VALUE;
        boolean found = false;
        for (PopulationRecord r : records) {
            if (r.getChangePercent() != 0) {
                if (Math.abs(r.getChangePercent()) < Math.abs(min)) {
                    min = r.getChangePercent();
                    found = true;
                }
            }
        }
        return found ? min : 0;
    }
}