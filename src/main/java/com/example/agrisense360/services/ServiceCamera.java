package com.example.agrisense360.services;

import com.example.agrisense360.entity.Camera;
import com.example.agrisense360.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceCamera implements IService<Camera> {
    private Connection connection;

    public ServiceCamera() {
        this.connection = MyDataBase.getInstance().getMyConnection();
    }

    @Override
    public void add(Camera camera) throws SQLException {
        String sql = "INSERT INTO Camera (camera_name, stream_url, location, equipment_id, sensitivity_level, alerts_enabled, is_active, created_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, camera.getCameraName());
            pstmt.setString(2, camera.getStreamUrl());
            pstmt.setString(3, camera.getLocation());
            pstmt.setObject(4, camera.getEquipmentId());
            pstmt.setString(5, camera.getSensitivityLevel());
            pstmt.setBoolean(6, camera.isAlertsEnabled());
            pstmt.setBoolean(7, camera.isActive());
            pstmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    camera.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Camera camera) throws SQLException {
        String sql = "UPDATE Camera SET camera_name = ?, stream_url = ?, location = ?, equipment_id = ?, sensitivity_level = ?, alerts_enabled = ?, is_active = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, camera.getCameraName());
            pstmt.setString(2, camera.getStreamUrl());
            pstmt.setString(3, camera.getLocation());
            pstmt.setObject(4, camera.getEquipmentId());
            pstmt.setString(5, camera.getSensitivityLevel());
            pstmt.setBoolean(6, camera.isAlertsEnabled());
            pstmt.setBoolean(7, camera.isActive());
            pstmt.setInt(8, camera.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM Camera WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public Camera getById(int id) throws SQLException {
        String sql = "SELECT * FROM Camera WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCamera(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Camera> getAll() throws SQLException {
        List<Camera> cameras = new ArrayList<>();
        String sql = "SELECT * FROM Camera WHERE is_active = TRUE";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cameras.add(mapResultSetToCamera(rs));
            }
        }
        return cameras;
    }

    public List<Camera> getAllCameras() throws SQLException {
        List<Camera> cameras = new ArrayList<>();
        String sql = "SELECT * FROM Camera ORDER BY created_date DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cameras.add(mapResultSetToCamera(rs));
            }
        }
        return cameras;
    }

    public Camera getCameraByName(String cameraName) throws SQLException {
        String sql = "SELECT * FROM Camera WHERE camera_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, cameraName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCamera(rs);
                }
            }
        }
        return null;
    }

    private Camera mapResultSetToCamera(ResultSet rs) throws SQLException {
        Camera camera = new Camera();
        camera.setId(rs.getInt("id"));
        camera.setCameraName(rs.getString("camera_name"));
        camera.setStreamUrl(rs.getString("stream_url"));
        camera.setLocation(rs.getString("location"));
        camera.setEquipmentId(rs.getObject("equipment_id") != null ? rs.getInt("equipment_id") : null);
        camera.setSensitivityLevel(rs.getString("sensitivity_level"));
        camera.setAlertsEnabled(rs.getBoolean("alerts_enabled"));
        camera.setActive(rs.getBoolean("is_active"));
        Timestamp createdTs = rs.getTimestamp("created_date");
        if (createdTs != null) {
            camera.setCreatedDate(createdTs.toLocalDateTime());
        }
        return camera;
    }
}
