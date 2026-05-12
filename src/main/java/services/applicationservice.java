package services;

import entity.application;
import entity.application.Status;
import entity.application.DesiredRole;
import entity.user;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class applicationservice {

    private Connection cnx;

    public applicationservice() throws SQLException {
        cnx = MyDataBase.getInstance().getCnx();
    }

    // ===============================
    // SUBMIT APPLICATION (Postuler)
    // Updates the USER table to set pending status and farm link
    // ===============================
    public void submit(application app) throws SQLException {
        String sql = "UPDATE user SET farm_id = ?, status = 'pending', cv_file = ?, ai_suggested_role = ?, roles = '[\"ROLE_PENDING\"]' " +
                     "WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, app.getFarmId());
        ps.setString(2, app.getCvPath());
        ps.setString(3, app.getDesiredRole().name());
        ps.setInt(4, app.getUserId());
        ps.executeUpdate();
        System.out.println("✅ Application submitted for user_id=" + app.getUserId());
    }

    // ===============================
    // GET PENDING APPLICATIONS — from user table
    // ===============================
    public List<application> getPendingByOwner(int ownerId) throws SQLException {
        List<application> list = new ArrayList<>();
        String sql = "SELECT u.*, f.name AS farm_name " +
                     "FROM user u " +
                     "JOIN farm f ON u.farm_id = f.id " +
                     "WHERE f.owner_id = ? AND u.status = 'pending' " +
                     "ORDER BY u.created_at DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, ownerId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            application app = new application();
            app.setUserId(rs.getInt("id"));
            app.setId(rs.getInt("id")); // For UI convenience
            app.setUserName(rs.getString("name"));
            app.setUserEmail(rs.getString("email"));
            app.setFarmId(rs.getInt("farm_id"));
            app.setFarmName(rs.getString("farm_name"));
            app.setCvPath(rs.getString("cv_file"));
            String desired = rs.getString("ai_suggested_role");
            if (desired != null) app.setDesiredRole(DesiredRole.valueOf(desired));
            app.setStatus(Status.PENDING);
            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) app.setAppliedAt(ts.toLocalDateTime());
            list.add(app);
        }
        return list;
    }

    // ===============================
    // ACCEPT APPLICATION
    // ===============================
    public void accept(int userId, user.Role assignedRole) throws SQLException {
        String rolesJson = "[\"" + assignedRole.name() + "\"]";
        String sql = "UPDATE user SET roles = ?, status = 'active', ai_suggested_role = NULL WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, rolesJson);
        ps.setInt(2, userId);
        ps.executeUpdate();
        System.out.println("✅ User " + userId + " accepted as " + assignedRole.name());
    }

    // ===============================
    // REJECT APPLICATION
    // ===============================
    public void reject(int userId) throws SQLException {
        String sql = "UPDATE user SET farm_id = NULL, status = 'pending', roles = '[\"ROLE_PENDING\"]', cv_file = NULL, ai_suggested_role = NULL " +
                     "WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
        System.out.println("✅ Application for user " + userId + " rejected");
    }

    // ===============================
    // CHECK IF USER ALREADY APPLIED
    // ===============================
    public boolean hasApplied(int userId, int farmId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE id = ? AND farm_id = ? AND status = 'pending'";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, farmId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1) > 0;
        return false;
    }

    // ===============================
    // GET ACCEPTED USER IDs BY FARMS
    // Returns IDs of active workers assigned to any of the given farms
    // ===============================
    public List<Integer> getAcceptedUserIdsByFarms(List<Integer> farmIds) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        if (farmIds == null || farmIds.isEmpty()) return ids;

        // Build IN clause: (?, ?, ...)
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < farmIds.size(); i++) {
            placeholders.append(i == 0 ? "?" : ", ?");
        }

        String sql = "SELECT id FROM user WHERE farm_id IN (" + placeholders + ") AND status = 'active'";
        PreparedStatement ps = cnx.prepareStatement(sql);
        for (int i = 0; i < farmIds.size(); i++) {
            ps.setInt(i + 1, farmIds.get(i));
        }
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            ids.add(rs.getInt("id"));
        }
        return ids;
    }
}