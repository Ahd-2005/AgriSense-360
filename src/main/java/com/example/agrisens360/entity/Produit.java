package com.example.agrisens360.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

public class Produit {
    private int id;
    private int agriculteurId;          // FK vers utilisateur
    private String categorie;
    private String nom;
    private String description;
    private BigDecimal prixUnitaire;
    private String photoUrl;
    private Timestamp createdAt;
    private Timestamp updatedAt;


    public Produit() {
    }

    public Produit(int id, int agriculteurId, String categorie, String nom, String description, BigDecimal prixUnitaire, String photoUrl, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.agriculteurId = agriculteurId;
        this.categorie = categorie;
        this.nom = nom;
        this.description = description;
        this.prixUnitaire = prixUnitaire;
        this.photoUrl = photoUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public int getAgriculteurId() {
        return agriculteurId;
    }

    public String getNom() {
        return nom;
    }

    public String getCategorie() {
        return categorie;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }


    public String getPhotoUrl() {
        return photoUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setId(int id) {
        this.id = id;
    }
    public void setAgriculteurId(int agriculteurId) {
        this.agriculteurId = agriculteurId;
    }
    public void setNom(String nom) {
        this.nom = nom;
    }
    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Produit{" +
                "id=" + id +
                ", agriculteurId=" + agriculteurId +
                ", categorie='" + categorie + '\'' +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", prixUnitaire=" + prixUnitaire +
                ", photoUrl='" + photoUrl + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Produit produit = (Produit) o;
        return id == produit.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}



