package services;

import entity.Parcelle;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParcelleService {

    private final Connection cnx = MyDataBase.getInstance().getCnx();

    // CREATE
    public void addParcelle(Parcelle p) throws SQLException {
        String sql = "INSERT INTO parcelle (nom, surface, localisation, type_sol, statut) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, p.getNom());
        ps.setDouble(2, p.getSurface());
        ps.setString(3, p.getLocalisation());
        ps.setString(4, p.getTypeSol());
        ps.setString(5, p.getStatut());
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
    }

    // DELETE
    public void deleteParcelle(int id) throws SQLException {
        String sql = "DELETE FROM parcelle WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // Get remaining surface for a parcelle
    public double getRemainingParcelleSize(int parcelleId) throws SQLException {
        // Get total parcelle surface
        String parcelleSql = "SELECT surface FROM parcelle WHERE id = ?";
        PreparedStatement parcellePs = cnx.prepareStatement(parcelleSql);
        parcellePs.setInt(1, parcelleId);
        ResultSet parcelleRs = parcellePs.executeQuery();

        double totalSurface = 0;
        if (parcelleRs.next()) {
            totalSurface = parcelleRs.getDouble("surface");
        }

        // Get used surface
        String usedSql = "SELECT SUM(surface) as used_surface FROM culture WHERE parcelle_id = ?";
        PreparedStatement usedPs = cnx.prepareStatement(usedSql);
        usedPs.setInt(1, parcelleId);
        ResultSet usedRs = usedPs.executeQuery();

        double usedSurface = 0;
        if (usedRs.next()) {
            usedSurface = usedRs.getDouble("used_surface");
        }

        return totalSurface - usedSurface;
    }
}