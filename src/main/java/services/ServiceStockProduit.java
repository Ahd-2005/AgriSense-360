package services;

import entity.Produit;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceStockProduit implements IServiceStock<Produit> {

    private final Connection cnx = MyDataBase.getInstance().getCnx();



    public int ajouter(Produit p) throws SQLException {
        String sql = "INSERT INTO produit (agriculteur_id, categorie, nom, description, prix_unitaire, photo_url, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, p.getAgriculteurId());
            pst.setString(2, p.getCategorie());
            pst.setString(3, p.getNom());
            pst.setString(4, p.getDescription());
            pst.setBigDecimal(5, p.getPrixUnitaire());
            pst.setString(6, p.getPhotoUrl());
            pst.setTimestamp(7, p.getCreatedAt());
            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }
    @Override
    public void modifier(Produit p) throws SQLException {
        String sql = "UPDATE produit SET categorie=?, nom=?, description=?, prix_unitaire=?, photo_url=? WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, p.getCategorie());
            pst.setString(2, p.getNom());
            pst.setString(3, p.getDescription());
            pst.setBigDecimal(4, p.getPrixUnitaire());
            pst.setString(5, p.getPhotoUrl());
            pst.setInt(6, p.getId());
            pst.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM produit WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    @Override
    public List<Produit> afficher() throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Produit p = new Produit();
                p.setId(rs.getInt("id"));
                p.setAgriculteurId(rs.getInt("agriculteur_id"));
                p.setCategorie(rs.getString("categorie"));
                p.setNom(rs.getString("nom"));
                p.setDescription(rs.getString("description"));
                p.setPrixUnitaire(rs.getBigDecimal("prix_unitaire"));
                p.setPhotoUrl(rs.getString("photo_url"));
                p.setCreatedAt(rs.getTimestamp("created_at"));
                produits.add(p);
                p.setBarcodeUrl(rs.getString("barcode_url"));  // ← LIGNE MANQUANTE

            }
        }
        return produits;
    }

    @Override
    public Produit recupererParId(int id) throws SQLException {
        String sql = "SELECT * FROM produit WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Produit p = new Produit();
                    p.setId(rs.getInt("id"));
                    p.setAgriculteurId(rs.getInt("agriculteur_id"));
                    p.setCategorie(rs.getString("categorie"));
                    p.setNom(rs.getString("nom"));
                    p.setDescription(rs.getString("description"));
                    p.setPrixUnitaire(rs.getBigDecimal("prix_unitaire"));
                    p.setPhotoUrl(rs.getString("photo_url"));
                    p.setCreatedAt(rs.getTimestamp("created_at"));
                    p.setBarcodeUrl(rs.getString("barcode_url"));
                    return p;
                }
            }
        }
        return null;
    }
    public List<Produit> getAllProduits() {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Produit p = new Produit();
                p.setId(rs.getInt("id"));
                p.setAgriculteurId(rs.getInt("agriculteur_id"));
                p.setCategorie(rs.getString("categorie"));
                p.setNom(rs.getString("nom"));
                p.setDescription(rs.getString("description"));
                p.setPrixUnitaire(rs.getBigDecimal("prix_unitaire"));
                p.setPhotoUrl(rs.getString("photo_url"));
                p.setCreatedAt(rs.getTimestamp("created_at"));
                produits.add(p);
                p.setBarcodeUrl(rs.getString("barcode_url"));  // ← LIGNE MANQUANTE

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return produits;
    }
    public void updateBarcode(int id, String barcodeUrl) throws SQLException {
        String req = "UPDATE produit SET barcode_url = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, barcodeUrl);
        ps.setInt(2, id);
        ps.executeUpdate();
    }
}