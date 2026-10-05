package ru.miet.demospectrum.dao;

import ru.miet.demospectrum.config.DBConnection;
import ru.miet.demospectrum.model.Region;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RegionDAO {

    /**
     * Находит регион по имени. Если нет — создаёт и возвращает с id.
     */
    public Region findOrCreate(String regionName) throws SQLException {
        // 1. Ищем
        String findSql = "SELECT id, name, parent_id FROM regions WHERE name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(findSql)) {
            pstmt.setString(1, regionName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Region r = new Region();
                    r.setId(rs.getInt("id"));
                    r.setName(rs.getString("name"));
                    r.setParentId(rs.getInt("parent_id"));
                    return r;
                }
            }
        }

        // 2. Создаём
        String insertSql = "INSERT INTO regions (name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, regionName);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    Region r = new Region();
                    r.setId(keys.getInt(1));
                    r.setName(regionName);
                    return r;
                }
            }
        }
        return null;
    }

    public List<Region> getAll() throws SQLException {
        List<Region> regions = new ArrayList<>();
        String sql = "SELECT id, name, parent_id FROM regions ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Region r = new Region();
                r.setId(rs.getInt("id"));
                r.setName(rs.getString("name"));
                r.setParentId(rs.getInt("parent_id"));
                regions.add(r);
            }
        }
        return regions;
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM regions";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }
}