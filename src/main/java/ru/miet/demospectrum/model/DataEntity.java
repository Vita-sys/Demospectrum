package ru.miet.demospectrum.model;

public abstract class DataEntity {
    protected int id;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public abstract String getDisplayName();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + "}";
    }
}