package ru.miet.demospectrum.dao;

import ru.miet.demospectrum.config.DBConnection;
import ru.miet.demospectrum.model.PopulationRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PopulationDAO {

    public void insert(PopulationRecord record) throws SQLException {
        String sql = "INSERT INTO population (city_id, year, population) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, record.getCityId());
            pstmt.setInt(2, record.getYear());
            pstmt.setLong(3, record.getPopulation());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) record.setId(keys.getInt(1));
            }
        }
    }

    public List<PopulationRecord> getAll() throws SQLException {
        List<PopulationRecord> records = new ArrayList<>();
        String sql = "SELECT id, city_id, year, population, change_percent, is_anomaly " +
                "FROM population ORDER BY city_id, year";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) records.add(mapRow(rs));
        }
        return records;
    }

    public List<PopulationRecord> getByYear(int year) throws SQLException {
        List<PopulationRecord> records = new ArrayList<>();
        String sql = "SELECT id, city_id, year, population, change_percent, is_anomaly " +
                "FROM population WHERE year = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) records.add(mapRow(rs));
            }
        }
        return records;
    }

    public void updateAnomaly(int id, double changePercent, boolean isAnomaly) throws SQLException {
        String sql = "UPDATE population SET change_percent = ?, is_anomaly = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, changePercent);
            pstmt.setBoolean(2, isAnomaly);
            pstmt.setInt(3, id);
            pstmt.executeUpdate();
        }
    }

    private PopulationRecord mapRow(ResultSet rs) throws SQLException {
        PopulationRecord r = new PopulationRecord();
        r.setId(rs.getInt("id"));
        r.setCityId(rs.getInt("city_id"));
        r.setYear(rs.getInt("year"));
        r.setPopulation(rs.getLong("population"));
        r.setChangePercent(rs.getDouble("change_percent"));
        r.setAnomaly(rs.getBoolean("is_anomaly"));
        return r;
    }
}
