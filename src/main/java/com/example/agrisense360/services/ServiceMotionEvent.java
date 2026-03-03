package com.example.agrisense360.services;

import com.example.agrisense360.entity.MotionEvent;
import com.example.agrisense360.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceMotionEvent implements IService<MotionEvent> {
    public ServiceMotionEvent() {
    }

    @Override
    public void add(MotionEvent motionEvent) throws SQLException {
        Connection connection = requireConnection();
        String sql = "INSERT INTO MotionEvent (camera_id, detection_time, motion_frame_count, severity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, motionEvent.getCameraId());
            pstmt.setTimestamp(2, Timestamp.valueOf(motionEvent.getDetectionTime()));
            pstmt.setInt(3, motionEvent.getMotionFrameCount());
            pstmt.setString(4, motionEvent.getSeverity());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    motionEvent.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(MotionEvent motionEvent) throws SQLException {
        Connection connection = requireConnection();
        String sql = "UPDATE MotionEvent SET camera_id = ?, detection_time = ?, motion_frame_count = ?, severity = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, motionEvent.getCameraId());
            pstmt.setTimestamp(2, Timestamp.valueOf(motionEvent.getDetectionTime()));
            pstmt.setInt(3, motionEvent.getMotionFrameCount());
            pstmt.setString(4, motionEvent.getSeverity());
            pstmt.setInt(5, motionEvent.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        Connection connection = requireConnection();
        String sql = "DELETE FROM MotionEvent WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public MotionEvent getById(int id) throws SQLException {
        Connection connection = requireConnection();
        String sql = "SELECT * FROM MotionEvent WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMotionEvent(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<MotionEvent> getAll() throws SQLException {
        Connection connection = requireConnection();
        List<MotionEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM MotionEvent ORDER BY detection_time DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                events.add(mapResultSetToMotionEvent(rs));
            }
        }
        return events;
    }

    public List<MotionEvent> getMotionEventsByCamera(int cameraId) throws SQLException {
        Connection connection = requireConnection();
        List<MotionEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM MotionEvent WHERE camera_id = ? ORDER BY detection_time DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, cameraId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToMotionEvent(rs));
                }
            }
        }
        return events;
    }

    public List<MotionEvent> getMotionEventsByCameraAndDateRange(int cameraId, LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        Connection connection = requireConnection();
        List<MotionEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM MotionEvent WHERE camera_id = ? AND detection_time BETWEEN ? AND ? ORDER BY detection_time DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, cameraId);
            pstmt.setTimestamp(2, Timestamp.valueOf(startDate));
            pstmt.setTimestamp(3, Timestamp.valueOf(endDate));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToMotionEvent(rs));
                }
            }
        }
        return events;
    }

    private MotionEvent mapResultSetToMotionEvent(ResultSet rs) throws SQLException {
        MotionEvent event = new MotionEvent();
        event.setId(rs.getInt("id"));
        event.setCameraId(rs.getInt("camera_id"));
        Timestamp detectionTs = rs.getTimestamp("detection_time");
        if (detectionTs != null) {
            event.setDetectionTime(detectionTs.toLocalDateTime());
        }
        event.setMotionFrameCount(rs.getInt("motion_frame_count"));
        event.setSeverity(rs.getString("severity"));
        return event;
    }

    private Connection requireConnection() throws SQLException {
        Connection connection = MyDataBase.getInstance().getMyConnection();
        if (connection == null) {
            String reason = MyDataBase.getInstance().getLastConnectionError();
            throw new SQLException("Database connection is not initialized. Root cause: " + (reason != null ? reason : "unknown") + ". Check that WAMP MySQL service is running and DB credentials are correct.");
        }
        return connection;
    }
}
