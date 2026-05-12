package services;

import entity.Culture;
import entity.Parcellehistorique;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CultureService {

    private Connection cnx;
    private final Parcellehistoriqueservice historiqueService = new Parcellehistoriqueservice();

    public CultureService() {
        cnx = MyDataBase.getInstance().getCnx();
    }

    // CREATE
    public void addCulture(Culture c) throws SQLException {
        Connection conn = null;
        try {
            conn = cnx;
            conn.setAutoCommit(false);

            String sql = "INSERT INTO culture (nom, type_culture, date_plantation, date_recolte, etat, parcelle_id, surface, img) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, c.getNom());
            ps.setString(2, c.getTypeCulture());
            ps.setDate(3, c.getDatePlantation());
            ps.setDate(4, c.getDateRecolte());
            ps.setString(5, c.getEtat());
            ps.setInt(6, c.getParcelleId());
            ps.setDouble(7, c.getSurface());
            ps.setString(8, c.getImg());
            ps.executeUpdate();

            ResultSet genKeys = ps.getGeneratedKeys();
            int newId = 0;
            if (genKeys.next()) { newId = genKeys.getInt(1); c.setId(newId); }

            PreparedStatement updatePs = conn.prepareStatement(
                    "UPDATE parcelle SET surface_restant = surface_restant - ? WHERE id = ?");
            updatePs.setDouble(1, c.getSurface());
            updatePs.setInt(2, c.getParcelleId());
            updatePs.executeUpdate();

            updateParcelleStatus(c.getParcelleId(), conn);
            conn.commit();

            historiqueService.logAction(new Parcellehistorique(
                    c.getParcelleId(), "CULTURE_AJOUTEE", newId > 0 ? newId : null,
                    c.getNom(), c.getTypeCulture(), c.getSurface(), null, c.getEtat(),
                    "Culture \"" + c.getNom() + "\" (" + c.getTypeCulture() + ") ajoutee · "
                            + c.getSurface() + " m2 · du " + c.getDatePlantation() + " au " + c.getDateRecolte(),
                    null
            ));

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    // READ
    public List<Culture> getAllCultures() throws SQLException {
        List<Culture> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM culture");
        while (rs.next()) {
            list.add(new Culture(
                    rs.getInt("id"), rs.getString("nom"), rs.getString("type_culture"),
                    rs.getDate("date_plantation"), rs.getDate("date_recolte"),
                    rs.getString("etat"), rs.getInt("parcelle_id"),
                    rs.getDouble("surface"), rs.getString("img")
            ));
        }
        return list;
    }

    public List<Culture> getCulturesByFarm(int farmId) throws SQLException {
        List<Culture> list = new ArrayList<>();
        String sql = "SELECT c.* FROM culture c " +
                     "JOIN parcelle p ON c.parcelle_id = p.id " +
                     "WHERE p.farm_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, farmId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new Culture(
                    rs.getInt("id"), rs.getString("nom"), rs.getString("type_culture"),
                    rs.getDate("date_plantation"), rs.getDate("date_recolte"),
                    rs.getString("etat"), rs.getInt("parcelle_id"),
                    rs.getDouble("surface"), rs.getString("img")
            ));
        }
        return list;
    }

    // UPDATE
    public void updateCulture(Culture c) throws SQLException {
        Connection conn = null;
        String oldEtat = null, oldNom = null;
        double oldSurface = 0;
        int oldParcelleId = 0;

        try {
            conn = cnx;
            conn.setAutoCommit(false);

            PreparedStatement sel = conn.prepareStatement(
                    "SELECT parcelle_id, surface, etat, nom FROM culture WHERE id = ?");
            sel.setInt(1, c.getId());
            ResultSet rs = sel.executeQuery();
            if (rs.next()) {
                oldParcelleId = rs.getInt("parcelle_id");
                oldSurface    = rs.getDouble("surface");
                oldEtat       = rs.getString("etat");
                oldNom        = rs.getString("nom");
            }

            PreparedStatement upd = conn.prepareStatement(
                    "UPDATE culture SET nom=?,type_culture=?,date_plantation=?,date_recolte=?,etat=?,parcelle_id=?,surface=?,img=? WHERE id=?");
            upd.setString(1, c.getNom()); upd.setString(2, c.getTypeCulture());
            upd.setDate(3, c.getDatePlantation()); upd.setDate(4, c.getDateRecolte());
            upd.setString(5, c.getEtat()); upd.setInt(6, c.getParcelleId());
            upd.setDouble(7, c.getSurface()); upd.setString(8, c.getImg());
            upd.setInt(9, c.getId());
            upd.executeUpdate();

            if (oldParcelleId == c.getParcelleId()) {
                PreparedStatement adj = conn.prepareStatement(
                        "UPDATE parcelle SET surface_restant = surface_restant - ? WHERE id = ?");
                adj.setDouble(1, c.getSurface() - oldSurface);
                adj.setInt(2, c.getParcelleId());
                adj.executeUpdate();
                updateParcelleStatus(c.getParcelleId(), conn);
            } else {
                PreparedStatement r1 = conn.prepareStatement(
                        "UPDATE parcelle SET surface_restant = surface_restant + ? WHERE id = ?");
                r1.setDouble(1, oldSurface); r1.setInt(2, oldParcelleId); r1.executeUpdate();
                PreparedStatement r2 = conn.prepareStatement(
                        "UPDATE parcelle SET surface_restant = surface_restant - ? WHERE id = ?");
                r2.setDouble(1, c.getSurface()); r2.setInt(2, c.getParcelleId()); r2.executeUpdate();
                updateParcelleStatus(oldParcelleId, conn);
                updateParcelleStatus(c.getParcelleId(), conn);
            }
            conn.commit();

            StringBuilder desc = new StringBuilder("Culture \"" + c.getNom() + "\" modifiee.");
            if (oldEtat != null && !oldEtat.equals(c.getEtat()))
                desc.append(" Etat: ").append(oldEtat).append(" -> ").append(c.getEtat()).append(".");
            if (c.getSurface() != oldSurface)
                desc.append(" Surface: ").append(oldSurface).append(" -> ").append(c.getSurface()).append(" m2.");

            historiqueService.logAction(new Parcellehistorique(
                    c.getParcelleId(), "CULTURE_MODIFIEE", c.getId(),
                    c.getNom(), c.getTypeCulture(), c.getSurface(),
                    oldEtat, c.getEtat(), desc.toString(), null
            ));

            if (oldParcelleId != c.getParcelleId()) {
                historiqueService.logAction(new Parcellehistorique(
                        oldParcelleId, "CULTURE_SUPPRIMEE", c.getId(),
                        oldNom, c.getTypeCulture(), oldSurface, oldEtat, null,
                        "Culture deplacee vers parcelle #" + c.getParcelleId(), null
                ));
            }

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    // DELETE (suppression manuelle — log CULTURE_SUPPRIMEE)
    public void deleteCulture(int id) throws SQLException {
        Connection conn = null;
        String nom = "N/A", type = null, etat = null;
        double surface = 0;
        int parcelleId = 0;

        try {
            conn = cnx;
            conn.setAutoCommit(false);

            PreparedStatement sel = conn.prepareStatement(
                    "SELECT parcelle_id, surface, nom, type_culture, etat FROM culture WHERE id = ?");
            sel.setInt(1, id);
            ResultSet rs = sel.executeQuery();
            if (rs.next()) {
                parcelleId = rs.getInt("parcelle_id");
                surface    = rs.getDouble("surface");
                nom        = rs.getString("nom");
                type       = rs.getString("type_culture");
                etat       = rs.getString("etat");
            }

            PreparedStatement del = conn.prepareStatement("DELETE FROM culture WHERE id = ?");
            del.setInt(1, id); del.executeUpdate();

            if (parcelleId > 0) {
                PreparedStatement restore = conn.prepareStatement(
                        "UPDATE parcelle SET surface_restant = surface_restant + ? WHERE id = ?");
                restore.setDouble(1, surface); restore.setInt(2, parcelleId); restore.executeUpdate();
                updateParcelleStatus(parcelleId, conn);
            }
            conn.commit();

            if (parcelleId > 0) {
                historiqueService.logAction(new Parcellehistorique(
                        parcelleId, "CULTURE_SUPPRIMEE", id, nom, type, surface, etat, null,
                        "Culture \"" + nom + "\" supprimee · " + surface + " m2 liberes.", null
                ));
            }

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    // ✅ NEW — Récolte officielle avec quantité ML
    // Appelé par CultureController APRÈS que HarvestAIService a calculé la quantité.
    // Remplace l'appel à deleteCulture() dans performHarvest().
    public void recolterEtSupprimerCulture(Culture culture, double quantiteKg) throws SQLException {
        Connection conn = null;
        try {
            conn = cnx;
            conn.setAutoCommit(false);

            // 1. Supprimer la culture
            PreparedStatement del = conn.prepareStatement("DELETE FROM culture WHERE id = ?");
            del.setInt(1, culture.getId());
            del.executeUpdate();

            // 2. Libérer la surface sur la parcelle
            PreparedStatement restore = conn.prepareStatement(
                    "UPDATE parcelle SET surface_restant = surface_restant + ? WHERE id = ?");
            restore.setDouble(1, culture.getSurface());
            restore.setInt(2, culture.getParcelleId());
            restore.executeUpdate();

            // 3. Mettre à jour le statut parcelle
            updateParcelleStatus(culture.getParcelleId(), conn);
            conn.commit();

            // ✅ 4. Log RECOLTE avec la quantité calculée par le ML
            String desc = "Recolte de \"" + culture.getNom() + "\" (" + culture.getTypeCulture() + ")"
                    + " · Surface: " + culture.getSurface() + " m2 liberee"
                    + " · Quantite ML: " + String.format("%.1f", quantiteKg) + " kg"
                    + " · Date recolte prevue: " + culture.getDateRecolte();

            historiqueService.logAction(new Parcellehistorique(
                    culture.getParcelleId(), "RECOLTE", culture.getId(),
                    culture.getNom(), culture.getTypeCulture(), culture.getSurface(),
                    culture.getEtat(), "Recoltee",
                    desc,
                    quantiteKg   // ← quantité réelle du modèle ML
            ));

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    private void updateParcelleStatus(int parcelleId, Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "SELECT surface_restant FROM parcelle WHERE id = ?");
        ps.setInt(1, parcelleId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String statut = (rs.getDouble("surface_restant") <= 0.01) ? "Occupee" : "Libre";
            PreparedStatement upd = conn.prepareStatement(
                    "UPDATE parcelle SET statut = ? WHERE id = ?");
            upd.setString(1, statut); upd.setInt(2, parcelleId); upd.executeUpdate();
        }
    }
}