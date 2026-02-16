package services;

import entity.Parcelle;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParcelleService {

    private final Connection cnx = MyDataBase.getInstance().getCnx();

    // CREATE - Initialize surface_restant = surface
    public void addParcelle(Parcelle p) throws SQLException {
        String sql = "INSERT INTO parcelle (nom, surface, surface_restant, localisation, type_sol, statut) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, p.getNom());
        ps.setDouble(2, p.getSurface());
        ps.setDouble(3, p.getSurface()); // surface_restant = surface initially
        ps.setString(4, p.getLocalisation());
        ps.setString(5, p.getTypeSol());
        ps.setString(6, "Libre");
        ps.executeUpdate();
    }

    // READ
    public List<Parcelle> getAllParcelles() throws SQLException {
        List<Parcelle> list = new ArrayList<>();
        String sql = "SELECT * FROM parcelle";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Parcelle p = new Parcelle(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getDouble("surface"),
                    rs.getString("localisation"),
                    rs.getString("type_sol"),
                    rs.getString("statut")
            );
            list.add(p);
        }
        return list;
    }

    // UPDATE
    public void updateParcelle(Parcelle p) throws SQLException {
        String sql = "UPDATE parcelle SET nom = ?, surface = ?, localisation = ?, type_sol = ?, statut = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, p.getNom());
        ps.setDouble(2, p.getSurface());
        ps.setString(3, p.getLocalisation());
        ps.setString(4, p.getTypeSol());
        ps.setString(5, p.getStatut());
        ps.setInt(6, p.getId());
        ps.executeUpdate();

        // Recalculate surface_restant after update
        recalculateSurfaceRestant(p.getId());
    }

    // DELETE
    public void deleteParcelle(int id) throws SQLException {
        String sql = "DELETE FROM parcelle WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // Get remaining surface from database
    public double getRemainingParcelleSize(int parcelleId) throws SQLException {
        String sql = "SELECT surface_restant FROM parcelle WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, parcelleId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getDouble("surface_restant");
        }
        return 0;
    }

    // Recalculate and update surface_restant
    public void recalculateSurfaceRestant(int parcelleId) throws SQLException {
        // Get total surface
        String getTotalSql = "SELECT surface FROM parcelle WHERE id = ?";
        PreparedStatement getTotal = cnx.prepareStatement(getTotalSql);
        getTotal.setInt(1, parcelleId);
        ResultSet totalRs = getTotal.executeQuery();

        double totalSurface = 0;
        if (totalRs.next()) {
            totalSurface = totalRs.getDouble("surface");
        }

        // Get used surface
        String getUsedSql = "SELECT COALESCE(SUM(surface), 0) as used FROM culture WHERE parcelle_id = ?";
        PreparedStatement getUsed = cnx.prepareStatement(getUsedSql);
        getUsed.setInt(1, parcelleId);
        ResultSet usedRs = getUsed.executeQuery();

        double usedSurface = 0;
        if (usedRs.next()) {
            usedSurface = usedRs.getDouble("used");
        }

        // Calculate restant
        double restant = totalSurface - usedSurface;

        // Update surface_restant in database
        String updateSql = "UPDATE parcelle SET surface_restant = ? WHERE id = ?";
        PreparedStatement updatePs = cnx.prepareStatement(updateSql);
        updatePs.setDouble(1, restant);
        updatePs.setInt(2, parcelleId);
        updatePs.executeUpdate();
    }
}