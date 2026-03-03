package services;

import entity.Tache;
import entity.Tache.Statut;
import entity.Tache.Priorite;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TacheService {

    private Connection cnx;

    public TacheService() throws SQLException {
        cnx = MyDataBase.getInstance().getCnx();
    }

    // ───────────────────────────────────────────────
    // AJOUTER
    // ───────────────────────────────────────────────
    public void ajouterTache(Tache t) throws SQLException {
        String sql = "INSERT INTO tache (titre, description, ouvrier_id, statut, priorite, date_echeance) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, t.getTitre());
        ps.setString(2, t.getDescription());
        ps.setInt(3, t.getOuvrierId());
        ps.setString(4, t.getStatut().name());
        ps.setString(5, t.getPriorite().name());
        if (t.getDateEcheance() != null)
            ps.setDate(6, Date.valueOf(t.getDateEcheance()));
        else
            ps.setNull(6, Types.DATE);
        ps.executeUpdate();
        System.out.println("✅ Tâche ajoutée : " + t.getTitre());
    }

    // ───────────────────────────────────────────────
    // MODIFIER
    // ───────────────────────────────────────────────
    public void updateTache(Tache t) throws SQLException {
        String sql = "UPDATE tache SET titre=?, description=?, priorite=?, date_echeance=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, t.getTitre());
        ps.setString(2, t.getDescription());
        ps.setString(3, t.getPriorite().name());
        if (t.getDateEcheance() != null)
            ps.setDate(4, Date.valueOf(t.getDateEcheance()));
        else
            ps.setNull(4, Types.DATE);
        ps.setInt(5, t.getId());
        ps.executeUpdate();
    }

    // ───────────────────────────────────────────────
    // METTRE À JOUR STATUT
    // ───────────────────────────────────────────────
    public void updateStatut(int tacheId, Statut statut) throws SQLException {
        String sql = "UPDATE tache SET statut=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, statut.name());
        ps.setInt(2, tacheId);
        ps.executeUpdate();
        System.out.println("✅ Statut tâche " + tacheId + " → " + statut);
    }

    // ───────────────────────────────────────────────
    // SUPPRIMER
    // ───────────────────────────────────────────────
    public void supprimerTache(int tacheId) throws SQLException {
        String sql = "DELETE FROM tache WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, tacheId);
        ps.executeUpdate();
    }

    // ───────────────────────────────────────────────
    // TÂCHES D'UN OUVRIER
    // ───────────────────────────────────────────────
    public List<Tache> getTachesParOuvrier(int ouvrierId) throws SQLException {
        List<Tache> list = new ArrayList<>();
        String sql = "SELECT * FROM tache WHERE ouvrier_id=? ORDER BY date_creation DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, ouvrierId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    // ───────────────────────────────────────────────
    // TOUTES LES TÂCHES (avec nom ouvrier)
    // ───────────────────────────────────────────────
    public List<Tache> getAllTaches() throws SQLException {
        List<Tache> list = new ArrayList<>();
        String sql = "SELECT t.*, u.name as ouvrier_nom FROM tache t " +
                     "JOIN user u ON t.ouvrier_id = u.id " +
                     "ORDER BY t.date_creation DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Tache t = mapRow(rs);
            t.setOuvrierNom(rs.getString("ouvrier_nom"));
            list.add(t);
        }
        return list;
    }

    // ───────────────────────────────────────────────
    // COMPTER TÂCHES NON TERMINÉES D'UN OUVRIER
    // ───────────────────────────────────────────────
    public int countTachesActives(int ouvrierId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tache WHERE ouvrier_id=? AND statut != 'TERMINEE'";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, ouvrierId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);
        return 0;
    }

    // ───────────────────────────────────────────────
    // MAPPER UNE LIGNE RS → TACHE
    // ───────────────────────────────────────────────
    private Tache mapRow(ResultSet rs) throws SQLException {
        Tache t = new Tache();
        t.setId(rs.getInt("id"));
        t.setTitre(rs.getString("titre"));
        t.setDescription(rs.getString("description"));
        t.setOuvrierId(rs.getInt("ouvrier_id"));
        t.setStatut(Statut.valueOf(rs.getString("statut")));
        t.setPriorite(Priorite.valueOf(rs.getString("priorite")));
        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) t.setDateCreation(ts.toLocalDateTime());
        Date d = rs.getDate("date_echeance");
        if (d != null) t.setDateEcheance(d.toLocalDate());
        return t;
    }
}
