package services;

import entity.application;
import entity.application.Status;
import entity.application.DesiredRole;
import entity.user;
import services.EmailService;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class applicationservice {

    private Connection cnx;

    public applicationservice() throws SQLException {
        cnx = MyDataBase.getInstance().getCnx();
    }

    // ===============================
    // SUBMIT APPLICATION
    // ===============================
    public void submit(application app) throws SQLException {
        String sql = "INSERT INTO application (user_id, farm_id, desired_role, cv_path, status, applied_at) " +
                "VALUES (?, ?, ?, ?, 'PENDING', NOW())";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, app.getUserId());
        ps.setInt(2, app.getFarmId());
        ps.setString(3, app.getDesiredRole().name());
        ps.setString(4, app.getCvPath());
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) app.setId(keys.getInt(1));
        System.out.println("✅ Application submitted, id=" + app.getId());
    }

    // ===============================
    // GET PENDING APPLICATIONS — for all farms owned by a given owner
    // ===============================
    public List<application> getPendingByOwner(int ownerId) throws SQLException {
        List<application> list = new ArrayList<>();
        String sql = "SELECT a.*, u.name AS user_name, u.email AS user_email, f.name AS farm_name " +
                "FROM application a " +
                "JOIN user u ON a.user_id = u.id " +
                "JOIN farm f ON a.farm_id = f.id " +
                "WHERE f.owner_id = ? AND a.status = 'PENDING' " +
                "ORDER BY a.applied_at DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, ownerId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    // ===============================
    // GET BY FARM AND STATUS
    // ===============================
    public List<application> getByFarmAndStatus(int farmId, String status) throws SQLException {
        List<application> list = new ArrayList<>();
        String sql = "SELECT a.*, u.name AS user_name, u.email AS user_email, f.name AS farm_name " +
                "FROM application a " +
                "JOIN user u ON a.user_id = u.id " +
                "JOIN farm f ON a.farm_id = f.id " +
                "WHERE a.farm_id = ? AND a.status = ? " +
                "ORDER BY a.applied_at DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, farmId);
        ps.setString(2, status);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    // ===============================
    // CHECK IF USER ALREADY APPLIED TO A FARM
    // ===============================
    public boolean hasApplied(int userId, int farmId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM application WHERE user_id = ? AND farm_id = ? AND status IN ('PENDING','ACCEPTED')";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, farmId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1) > 0;
        return false;
    }

    // ===============================
    // ACCEPT APPLICATION → update status + update user role
    // ===============================
    public void accept(int applicationId, user.Role assignedRole) throws SQLException {
        application app = getById(applicationId);
        if (app == null) throw new SQLException("Application not found: " + applicationId);

        updateStatus(applicationId, Status.ACCEPTED);

        userservice us = new userservice();
        us.updateUserRole(app.getUserId(), assignedRole);

        System.out.println("✅ Application " + applicationId + " accepted → role = " + assignedRole.name());
    }

    // ===============================
    // REJECT APPLICATION → update status + send email
    // ===============================
    public void reject(int applicationId) throws SQLException {
        application app = getById(applicationId);
        if (app == null) throw new SQLException("Application not found: " + applicationId);

        updateStatus(applicationId, Status.REJECTED);
        System.out.println("✅ Application " + applicationId + " rejected");

        // Send rejection email if EmailService is enabled
        if (EmailService.isEnabled() && app.getUserEmail() != null && !app.getUserEmail().isEmpty()) {
            try {
                String userName    = app.getUserName()  != null ? app.getUserName()  : "Candidat";
                String farmName    = app.getFarmName()  != null ? app.getFarmName()  : "la ferme";
                String desiredRole = app.getDesiredRole() == DesiredRole.ROLE_GERANT ? "Gérant" : "Ouvrier";

                EmailService.sendRejectionEmail(app.getUserEmail(), userName, farmName, desiredRole);
                System.out.println("📧 Rejection email sent to " + app.getUserEmail());
            } catch (Exception e) {
                // Email failure should not block the rejection
                System.err.println("⚠ Could not send rejection email: " + e.getMessage());
            }
        }
    }

    // ===============================
    // GET ACCEPTED USER IDs FOR A LIST OF FARMS (for ouvrier filtering)
    // ===============================
    public List<Integer> getAcceptedUserIdsByFarms(List<Integer> farmIds) throws SQLException {
        List<Integer> userIds = new ArrayList<>();
        if (farmIds == null || farmIds.isEmpty()) return userIds;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < farmIds.size(); i++) {
            placeholders.append(i == 0 ? "?" : ",?");
        }
        String sql = "SELECT DISTINCT user_id FROM application WHERE farm_id IN (" +
                placeholders + ") AND status = 'ACCEPTED'";
        PreparedStatement ps = cnx.prepareStatement(sql);
        for (int i = 0; i < farmIds.size(); i++) ps.setInt(i + 1, farmIds.get(i));
        ResultSet rs = ps.executeQuery();
        while (rs.next()) userIds.add(rs.getInt("user_id"));
        return userIds;
    }

    // ===============================
    // GET BY ID
    // ===============================
    public application getById(int id) throws SQLException {
        String sql = "SELECT a.*, u.name AS user_name, u.email AS user_email, f.name AS farm_name " +
                "FROM application a " +
                "JOIN user u ON a.user_id = u.id " +
                "JOIN farm f ON a.farm_id = f.id " +
                "WHERE a.id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    // ===============================
    // PRIVATE HELPERS
    // ===============================
    private void updateStatus(int appId, Status status) throws SQLException {
        String sql = "UPDATE application SET status = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, status.name());
        ps.setInt(2, appId);
        ps.executeUpdate();
    }

    private application mapRow(ResultSet rs) throws SQLException {
        application app = new application();
        app.setId(rs.getInt("id"));
        app.setUserId(rs.getInt("user_id"));
        app.setFarmId(rs.getInt("farm_id"));
        app.setDesiredRole(DesiredRole.valueOf(rs.getString("desired_role")));
        app.setCvPath(rs.getString("cv_path"));
        app.setStatus(Status.valueOf(rs.getString("status")));
        Timestamp ts = rs.getTimestamp("applied_at");
        if (ts != null) app.setAppliedAt(ts.toLocalDateTime());

        try { app.setUserName(rs.getString("user_name")); }   catch (SQLException ignored) {}
        try { app.setUserEmail(rs.getString("user_email")); } catch (SQLException ignored) {}
        try { app.setFarmName(rs.getString("farm_name")); }   catch (SQLException ignored) {}

        return app;
    }
}