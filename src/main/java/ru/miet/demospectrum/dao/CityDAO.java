package ru.miet.demospectrum.dao;

import ru.miet.demospectrum.config.DBConnection;
import ru.miet.demospectrum.model.City;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CityDAO {

    public void insert(City city) throws SQLException {
        String sql = "INSERT INTO cities (name, region_id, latitude, longitude) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, city.getName());
            pstmt.setInt(2, city.getRegionId());
            pstmt.setDouble(3, city.getLatitude());
            pstmt.setDouble(4, city.getLongitude());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) city.setId(keys.getInt(1));
            }
        }
    }

    public List<City> getAll() throws SQLException {
        List<City> cities = new ArrayList<>();
        String sql = "SELECT id, name, region_id, latitude, longitude FROM cities ORDER BY name";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cities.add(mapRow(rs));
            }
        }
        return cities;
    }

    public City getById(int id) throws SQLException {
        String sql = "SELECT id, name, region_id, latitude, longitude FROM cities WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public City getByName(String name) throws SQLException {
        String sql = "SELECT id, name, region_id, latitude, longitude FROM cities WHERE name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM cities";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public void deleteAll() throws SQLException {
        String sql = "DELETE FROM cities";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private City mapRow(ResultSet rs) throws SQLException {
        City c = new City();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setRegionId(rs.getInt("region_id"));
        c.setLatitude(rs.getDouble("latitude"));
        c.setLongitude(rs.getDouble("longitude"));
        return c;
    }
}