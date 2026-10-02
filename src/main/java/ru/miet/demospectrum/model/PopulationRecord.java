package ru.miet.demospectrum.model;

import java.util.Objects;

public class PopulationRecord extends DataEntity {

    private int cityId;
    private int year;
    private long population;
    private double changePercent;
    private boolean isAnomaly;

    public PopulationRecord() {}

    public PopulationRecord(int cityId, int year, long population) {
        this.cityId = cityId;
        this.year = year;
        this.population = population;
    }

    public int getCityId() { return cityId; }
    public void setCityId(int cityId) { this.cityId = cityId; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public long getPopulation() { return population; }
    public void setPopulation(long population) { this.population = population; }

    public double getChangePercent() { return changePercent; }
    public void setChangePercent(double changePercent) { this.changePercent = changePercent; }

    public boolean isAnomaly() { return isAnomaly; }
    public void setAnomaly(boolean anomaly) { isAnomaly = anomaly; }

    @Override
    public String getDisplayName() {
        return "Население " + year + " г.";
    }

    @Override
    public String toString() {
        return String.format("PopulationRecord{id=%d, cityId=%d, year=%d, population=%d, change=%.2f%%, anomaly=%b}",
                id, cityId, year, population, changePercent, isAnomaly);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PopulationRecord that = (PopulationRecord) o;
        return cityId == that.cityId && year == that.year;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cityId, year);
    }
}