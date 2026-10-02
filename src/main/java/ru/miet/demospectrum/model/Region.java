package ru.miet.demospectrum.model;

import java.util.Objects;

public class Region extends DataEntity {

    private String name;
    private int parentId;

    public Region() {}

    public Region(String name) {
        this.name = name;
    }

    public Region(int id, String name, int parentId) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getParentId() { return parentId; }
    public void setParentId(int parentId) { this.parentId = parentId; }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String toString() {
        return "Region{id=" + id + ", name='" + name + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Region region = (Region) o;
        return Objects.equals(name, region.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}