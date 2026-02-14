package services;

import entity.Culture;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CultureService {

    private Connection cnx;

    public CultureService() {
        cnx = MyDataBase.getInstance().getCnx();
    }

    // CREATE
    // ADD with surface management
    public void addCulture(Culture c) throws SQLException {
        Connection conn = null;
        try {
            conn = cnx;
            conn.setAutoCommit(false); // Start transaction

            // 1. Insert culture
            // 1. Insert culture
            String sql = "INSERT INTO culture (nom, type_culture, date_plantation, date_recolte, etat, parcelle_id, surface, img) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, c.getNom());
            ps.setString(2, c.getTypeCulture());
            ps.setDate(3, c.getDatePlantation());
            ps.setDate(4, c.getDateRecolte());
            ps.setString(5, c.getEtat());
            ps.setInt(6, c.getParcelleId());
            ps.setDouble(7, c.getSurface());
            ps.setString(8, c.getImg());  // NEW
            ps.executeUpdate();

            // 2. Update parcelle surface and status
            updateParcelleSurfaceAndStatus(c.getParcelleId(), conn);

            conn.commit(); // Commit transaction

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback(); // Rollback on error
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    // READ (JOIN)
    // READ (JOIN)
    public List<Culture> getAllCultures() throws SQLException {
        List<Culture> list = new ArrayList<>();
        String sql = "SELECT * FROM culture";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Culture c = new Culture(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("type_culture"),
                    rs.getDate("date_plantation"),
                    rs.getDate("date_recolte"),
                    rs.getString("etat"),
                    rs.getInt("parcelle_id"),
                    rs.getDouble("surface"),
                    rs.getString("img")  // NEW
            );
            list.add(c);
        }
        return list;
    }

    // UPDATE
    // UPDATE with surface management
    public void updateCulture(Culture c) throws SQLException {
        Connection conn = null;
        try {
            conn = cnx;
            conn.setAutoCommit(false);

            // 1. Get old culture data
            String selectSql = "SELECT parcelle_id, surface FROM culture WHERE id = ?";
            PreparedStatement selectPs = conn.prepareStatement(selectSql);
            selectPs.setInt(1, c.getId());
            ResultSet rs = selectPs.executeQuery();

            int oldParcelleId = 0;
            double oldSurface = 0;
            if (rs.next()) {
                oldParcelleId = rs.getInt("parcelle_id");
                oldSurface = rs.getDouble("surface");
            }

            // 2. Update culture
            // 2. Update culture
            String updateSql = "UPDATE culture SET nom = ?, type_culture = ?, date_plantation = ?, date_recolte = ?, etat = ?, parcelle_id = ?, surface = ?, img = ? WHERE id = ?";
            PreparedStatement updatePs = conn.prepareStatement(updateSql);
            updatePs.setString(1, c.getNom());
            updatePs.setString(2, c.getTypeCulture());
            updatePs.setDate(3, c.getDatePlantation());
            updatePs.setDate(4, c.getDateRecolte());
            updatePs.setString(5, c.getEtat());
            updatePs.setInt(6, c.getParcelleId());
            updatePs.setDouble(7, c.getSurface());
            updatePs.setString(8, c.getImg());  // NEW
            updatePs.setInt(9, c.getId());  // Changed from 8 to 9
            updatePs.executeUpdate();

            // 3. Update both old and new parcelle surfaces
            if (oldParcelleId == c.getParcelleId()) {
                // Same parcelle, just recalculate
                updateParcelleSurfaceAndStatus(c.getParcelleId(), conn);
            } else {
                // Different parcelle, update both
                updateParcelleSurfaceAndStatus(oldParcelleId, conn);
                updateParcelleSurfaceAndStatus(c.getParcelleId(), conn);
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    // DELETE
    // DELETE with surface management
    public void deleteCulture(int id) throws SQLException {
        Connection conn = null;
        try {
            conn = cnx;
            conn.setAutoCommit(false);

            // 1. Get parcelle_id before deleting
            String selectSql = "SELECT parcelle_id FROM culture WHERE id = ?";
            PreparedStatement selectPs = conn.prepareStatement(selectSql);
            selectPs.setInt(1, id);
            ResultSet rs = selectPs.executeQuery();

            int parcelleId = 0;
            if (rs.next()) {
                parcelleId = rs.getInt("parcelle_id");
            }

            // 2. Delete culture
            String deleteSql = "DELETE FROM culture WHERE id = ?";
            PreparedStatement deletePs = conn.prepareStatement(deleteSql);
            deletePs.setInt(1, id);
            deletePs.executeUpdate();

            // 3. Update parcelle surface and status
            if (parcelleId > 0) {
                updateParcelleSurfaceAndStatus(parcelleId, conn);
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }
    // Helper method to update parcelle surface and status
    private void updateParcelleSurfaceAndStatus(int parcelleId, Connection conn) throws SQLException {
        // Calculate total surface used by all cultures on this parcelle
        String calcSql = "SELECT SUM(surface) as used_surface FROM culture WHERE parcelle_id = ?";
        PreparedStatement calcPs = conn.prepareStatement(calcSql);
        calcPs.setInt(1, parcelleId);
        ResultSet rs = calcPs.executeQuery();

        double usedSurface = 0;
        if (rs.next()) {
            usedSurface = rs.getDouble("used_surface");
        }

        // Get parcelle total surface
        String parcelleSQL = "SELECT surface FROM parcelle WHERE id = ?";
        PreparedStatement parcellePs = conn.prepareStatement(parcelleSQL);
        parcellePs.setInt(1, parcelleId);
        ResultSet parcelleRs = parcellePs.executeQuery();

        double totalSurface = 0;
        if (parcelleRs.next()) {
            totalSurface = parcelleRs.getDouble("surface");
        }

        // Calculate remaining surface
        double remainingSurface = totalSurface - usedSurface;

        // Update parcelle status
        String newStatut = (remainingSurface <= 0.01) ? "Occupée" : "Libre"; // 0.01 for floating point precision

        String updateParcelleSQL = "UPDATE parcelle SET statut = ? WHERE id = ?";
        PreparedStatement updateParcellePs = conn.prepareStatement(updateParcelleSQL);
        updateParcellePs.setString(1, newStatut);
        updateParcellePs.setInt(2, parcelleId);
        updateParcellePs.executeUpdate();
    }

}
