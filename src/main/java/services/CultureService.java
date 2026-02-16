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

    // CREATE - Add culture and update surface_restant
    public void addCulture(Culture c) throws SQLException {
        Connection conn = null;
        try {
            conn = cnx;
            conn.setAutoCommit(false);

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
            ps.setString(8, c.getImg());
            ps.executeUpdate();

            // 2. Update surface_restant: decrease by culture surface
            String updateRestant = "UPDATE parcelle SET surface_restant = surface_restant - ? WHERE id = ?";
            PreparedStatement updatePs = conn.prepareStatement(updateRestant);
            updatePs.setDouble(1, c.getSurface());
            updatePs.setInt(2, c.getParcelleId());
            updatePs.executeUpdate();

            // 3. Update status
            updateParcelleStatus(c.getParcelleId(), conn);

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

    // READ
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
                    rs.getString("img")
            );
            list.add(c);
        }
        return list;
    }

    // UPDATE - Update culture and adjust surface_restant
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
            String updateSql = "UPDATE culture SET nom = ?, type_culture = ?, date_plantation = ?, date_recolte = ?, etat = ?, parcelle_id = ?, surface = ?, img = ? WHERE id = ?";
            PreparedStatement updatePs = conn.prepareStatement(updateSql);
            updatePs.setString(1, c.getNom());
            updatePs.setString(2, c.getTypeCulture());
            updatePs.setDate(3, c.getDatePlantation());
            updatePs.setDate(4, c.getDateRecolte());
            updatePs.setString(5, c.getEtat());
            updatePs.setInt(6, c.getParcelleId());
            updatePs.setDouble(7, c.getSurface());
            updatePs.setString(8, c.getImg());
            updatePs.setInt(9, c.getId());
            updatePs.executeUpdate();

            // 3. Adjust surface_restant
            if (oldParcelleId == c.getParcelleId()) {
                // Same parcelle: add back old surface, subtract new surface
                double diff = c.getSurface() - oldSurface;
                String adjustSql = "UPDATE parcelle SET surface_restant = surface_restant - ? WHERE id = ?";
                PreparedStatement adjustPs = conn.prepareStatement(adjustSql);
                adjustPs.setDouble(1, diff);
                adjustPs.setInt(2, c.getParcelleId());
                adjustPs.executeUpdate();

                updateParcelleStatus(c.getParcelleId(), conn);
            } else {
                // Different parcelle: restore old, reduce new
                String restoreOld = "UPDATE parcelle SET surface_restant = surface_restant + ? WHERE id = ?";
                PreparedStatement restorePs = conn.prepareStatement(restoreOld);
                restorePs.setDouble(1, oldSurface);
                restorePs.setInt(2, oldParcelleId);
                restorePs.executeUpdate();

                String reduceNew = "UPDATE parcelle SET surface_restant = surface_restant - ? WHERE id = ?";
                PreparedStatement reducePs = conn.prepareStatement(reduceNew);
                reducePs.setDouble(1, c.getSurface());
                reducePs.setInt(2, c.getParcelleId());
                reducePs.executeUpdate();

                updateParcelleStatus(oldParcelleId, conn);
                updateParcelleStatus(c.getParcelleId(), conn);
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

    // DELETE - Delete culture and restore surface_restant
    public void deleteCulture(int id) throws SQLException {
        Connection conn = null;
        try {
            conn = cnx;
            conn.setAutoCommit(false);

            // 1. Get culture data before deleting
            String selectSql = "SELECT parcelle_id, surface FROM culture WHERE id = ?";
            PreparedStatement selectPs = conn.prepareStatement(selectSql);
            selectPs.setInt(1, id);
            ResultSet rs = selectPs.executeQuery();

            int parcelleId = 0;
            double cultureSurface = 0;
            if (rs.next()) {
                parcelleId = rs.getInt("parcelle_id");
                cultureSurface = rs.getDouble("surface");
            }

            // 2. Delete culture
            String deleteSql = "DELETE FROM culture WHERE id = ?";
            PreparedStatement deletePs = conn.prepareStatement(deleteSql);
            deletePs.setInt(1, id);
            deletePs.executeUpdate();

            // 3. Restore surface_restant: add back culture surface
            if (parcelleId > 0) {
                String restoreSql = "UPDATE parcelle SET surface_restant = surface_restant + ? WHERE id = ?";
                PreparedStatement restorePs = conn.prepareStatement(restoreSql);
                restorePs.setDouble(1, cultureSurface);
                restorePs.setInt(2, parcelleId);
                restorePs.executeUpdate();

                updateParcelleStatus(parcelleId, conn);
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

    // Update parcelle status based on surface_restant
    private void updateParcelleStatus(int parcelleId, Connection conn) throws SQLException {
        String getSql = "SELECT surface_restant FROM parcelle WHERE id = ?";
        PreparedStatement getPs = conn.prepareStatement(getSql);
        getPs.setInt(1, parcelleId);
        ResultSet rs = getPs.executeQuery();

        if (rs.next()) {
            double restant = rs.getDouble("surface_restant");
            String newStatut = (restant <= 0.01) ? "Occupée" : "Libre";

            String updateSql = "UPDATE parcelle SET statut = ? WHERE id = ?";
            PreparedStatement updatePs = conn.prepareStatement(updateSql);
            updatePs.setString(1, newStatut);
            updatePs.setInt(2, parcelleId);
            updatePs.executeUpdate();
        }
    }

}