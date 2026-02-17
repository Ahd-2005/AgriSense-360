package services;

import entity.Stock;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceStockStock implements IServiceStock<Stock> {

    private final Connection cnx = MyDataBase.getInstance().getCnx();

    @Override
    public int ajouter(Stock s) throws SQLException {
        String sql = "INSERT INTO stock (produit_id, quantite_actuelle, seuil_alerte, unite_mesure, date_reception, date_expiration, emplacement) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, s.getProduitId());
            pst.setBigDecimal(2, s.getQuantiteActuelle());
            pst.setBigDecimal(3, s.getSeuilAlerte());
            pst.setString(4, s.getUniteMesure());
            pst.setDate(5, s.getDateReception());
            pst.setDate(6, s.getDateExpiration());
            pst.setString(7, s.getEmplacement());

            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    s.setId(rs.getInt(1));
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    @Override
    public void modifier(Stock s) throws SQLException {
        if (s == null || s.getProduitId() <= 0) {
            throw new SQLException("Stock ou produit_id invalide : impossible de modifier.");
        }

        System.out.println("Modification du stock pour produit_id: " + s.getProduitId());

        String sql = "UPDATE stock SET quantite_actuelle=?, seuil_alerte=?, unite_mesure=?, date_reception=?, date_expiration=?, emplacement=? WHERE produit_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setBigDecimal(1, s.getQuantiteActuelle());
            pst.setBigDecimal(2, s.getSeuilAlerte());
            pst.setString(3, s.getUniteMesure());
            pst.setDate(4, s.getDateReception());
            pst.setDate(5, s.getDateExpiration());
            pst.setString(6, s.getEmplacement());
            pst.setInt(7, s.getProduitId());

            int rowsAffected = pst.executeUpdate();
            System.out.println("Lignes affectées: " + rowsAffected);

            if (rowsAffected == 0) {
                throw new SQLException("Aucun stock trouvé pour ce produit_id. Mise à jour impossible.");
            }
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM stock WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    @Override
    public List<Stock> afficher() throws SQLException {
        List<Stock> stocks = new ArrayList<>();
        String sql = "SELECT * FROM stock";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Stock s = new Stock();
                s.setId(rs.getInt("id"));
                s.setProduitId(rs.getInt("produit_id"));
                s.setQuantiteActuelle(rs.getBigDecimal("quantite_actuelle"));
                s.setSeuilAlerte(rs.getBigDecimal("seuil_alerte"));
                s.setUniteMesure(rs.getString("unite_mesure"));
                s.setDateReception(rs.getDate("date_reception"));
                s.setDateExpiration(rs.getDate("date_expiration"));
                s.setEmplacement(rs.getString("emplacement"));
                stocks.add(s);
            }
        }
        return stocks;
    }

    @Override
    public Stock recupererParId(int id) throws SQLException {
        String sql = "SELECT * FROM stock WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Stock s = new Stock();
                    s.setId(rs.getInt("id"));
                    s.setProduitId(rs.getInt("produit_id"));
                    s.setQuantiteActuelle(rs.getBigDecimal("quantite_actuelle"));
                    s.setSeuilAlerte(rs.getBigDecimal("seuil_alerte"));
                    s.setUniteMesure(rs.getString("unite_mesure"));
                    s.setDateReception(rs.getDate("date_reception"));
                    s.setDateExpiration(rs.getDate("date_expiration"));
                    s.setEmplacement(rs.getString("emplacement"));
                    return s;
                }
            }
        }
        return null;
    }

    // Méthode spécifique à Stock (non dans IService, mais utilisée dans le contrôleur)
    public Stock recupererParProduitId(int produitId) {
        Stock stock = null;
        String sql = "SELECT * FROM stock WHERE produit_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, produitId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                stock = new Stock();
                // Récupération EXPLICITE de l'ID
                int id = rs.getInt("id");
                System.out.println("ID récupéré de la BD: " + id); // DEBUG

                stock.setId(id);  // ← C'EST CRITIQUE
                stock.setProduitId(rs.getInt("produit_id"));
                stock.setQuantiteActuelle(rs.getBigDecimal("quantite_actuelle"));
                stock.setSeuilAlerte(rs.getBigDecimal("seuil_alerte"));
                stock.setUniteMesure(rs.getString("unite_mesure"));
                stock.setDateReception(rs.getDate("date_reception"));
                stock.setDateExpiration(rs.getDate("date_expiration"));
                stock.setEmplacement(rs.getString("emplacement"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stock;
    }
}