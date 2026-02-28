package services;

import entity.Parcellehistorique;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Parcellehistoriqueservice {

    private final Connection cnx = MyDataBase.getInstance().getCnx();

    // ── INSERT ────────────────────────────────────────────────────────────────

    public void logAction(Parcellehistorique h) throws SQLException {
        String sql = "INSERT INTO parcelle_historique " +
                "(parcelle_id, type_action, culture_id, culture_nom, type_culture, " +
                " surface, etat_avant, etat_apres, date_action, description, quantite_recolte) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, h.getParcelleId());
        ps.setString(2, h.getTypeAction());
        if (h.getCultureId() != null) ps.setInt(3, h.getCultureId()); else ps.setNull(3, Types.INTEGER);
        ps.setString(4, h.getCultureNom());
        ps.setString(5, h.getTypeCulture());
        if (h.getSurface() != null) ps.setDouble(6, h.getSurface()); else ps.setNull(6, Types.DOUBLE);
        ps.setString(7, h.getEtatAvant());
        ps.setString(8, h.getEtatApres());
        ps.setString(9, h.getDescription());
        if (h.getQuantiteRecolte() != null) ps.setDouble(10, h.getQuantiteRecolte()); else ps.setNull(10, Types.DOUBLE);
        ps.executeUpdate();
    }

    // ── READ all for a parcelle ───────────────────────────────────────────────

    public List<Parcellehistorique> getHistoriqueByParcelle(int parcelleId) throws SQLException {
        List<Parcellehistorique> list = new ArrayList<>();
        String sql = "SELECT * FROM parcelle_historique WHERE parcelle_id = ? ORDER BY date_action DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, parcelleId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Parcellehistorique h = mapRow(rs);
            list.add(h);
        }
        return list;
    }

    // ── READ filtered by type ─────────────────────────────────────────────────

    public List<Parcellehistorique> getHistoriqueByType(int parcelleId, String typeAction) throws SQLException {
        List<Parcellehistorique> list = new ArrayList<>();
        String sql = "SELECT * FROM parcelle_historique WHERE parcelle_id = ? AND type_action = ? ORDER BY date_action DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, parcelleId);
        ps.setString(2, typeAction);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    // ── Helper: map ResultSet row to entity ───────────────────────────────────

    private Parcellehistorique mapRow(ResultSet rs) throws SQLException {
        Parcellehistorique h = new Parcellehistorique();
        h.setId(rs.getInt("id"));
        h.setParcelleId(rs.getInt("parcelle_id"));
        h.setTypeAction(rs.getString("type_action"));
        int cid = rs.getInt("culture_id");
        if (!rs.wasNull()) h.setCultureId(cid);
        h.setCultureNom(rs.getString("culture_nom"));
        h.setTypeCulture(rs.getString("type_culture"));
        double surf = rs.getDouble("surface");
        if (!rs.wasNull()) h.setSurface(surf);
        h.setEtatAvant(rs.getString("etat_avant"));
        h.setEtatApres(rs.getString("etat_apres"));
        Timestamp ts = rs.getTimestamp("date_action");
        if (ts != null) h.setDateAction(ts.toLocalDateTime());
        h.setDescription(rs.getString("description"));
        double qty = rs.getDouble("quantite_recolte");
        if (!rs.wasNull()) h.setQuantiteRecolte(qty);
        return h;
    }
}