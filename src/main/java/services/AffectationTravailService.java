package services;

import entity.AffectationTravail;
import utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AffectationTravailService {

    public void add(AffectationTravail a) throws SQLException {
        String sql = """
            INSERT INTO affectation_travail (type_travail, date_debut, date_fin, zone_travail, statut)
            VALUES (?, ?, ?, ?, ?)
            """;
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) throw new SQLException("No connection");
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, a.getTypeTravail());
            ps.setDate(2, a.getDateDebut() != null ? Date.valueOf(a.getDateDebut()) : null);
            ps.setDate(3, a.getDateFin() != null ? Date.valueOf(a.getDateFin()) : null);
            ps.setString(4, a.getZoneTravail());
            ps.setString(5, a.getStatut());
            ps.executeUpdate();
        }
    }

    public void update(AffectationTravail a) throws SQLException {
        String sql = """
            UPDATE affectation_travail
            SET type_travail = ?, date_debut = ?, date_fin = ?, zone_travail = ?, statut = ?
            WHERE id_affectation = ?
            """;
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) throw new SQLException("No connection");
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, a.getTypeTravail());
            ps.setDate(2, a.getDateDebut() != null ? Date.valueOf(a.getDateDebut()) : null);
            ps.setDate(3, a.getDateFin() != null ? Date.valueOf(a.getDateFin()) : null);
            ps.setString(4, a.getZoneTravail());
            ps.setString(5, a.getStatut());
            ps.setInt(6, a.getIdAffectation());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM affectation_travail WHERE id_affectation = ?";
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) throw new SQLException("No connection");
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<AffectationTravail> getAll() throws SQLException {
        String sql = "SELECT id_affectation, type_travail, date_debut, date_fin, zone_travail, statut FROM affectation_travail";
        List<AffectationTravail> list = new ArrayList<>();
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) throw new SQLException("No connection");
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public AffectationTravail getById(int id) throws SQLException {
        String sql = "SELECT id_affectation, type_travail, date_debut, date_fin, zone_travail, statut FROM affectation_travail WHERE id_affectation = ?";
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) throw new SQLException("No connection");
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    private AffectationTravail mapResultSet(ResultSet rs) throws SQLException {
        AffectationTravail a = new AffectationTravail();
        a.setIdAffectation(rs.getInt("id_affectation"));
        a.setTypeTravail(rs.getString("type_travail"));
        Date dDeb = rs.getDate("date_debut");
        a.setDateDebut(dDeb != null ? dDeb.toLocalDate() : null);
        Date dFin = rs.getDate("date_fin");
        a.setDateFin(dFin != null ? dFin.toLocalDate() : null);
        a.setZoneTravail(rs.getString("zone_travail"));
        a.setStatut(rs.getString("statut"));
        return a;
    }
}
