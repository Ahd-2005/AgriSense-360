package tn.esprit.entities;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

public class Stock {
    private int id;
    private int produitId;              // FK vers produit
    private BigDecimal quantiteActuelle;
    private BigDecimal seuilAlerte;
    private String uniteMesure;
    private Date dateReception;
    private Date dateExpiration;
    private String emplacement;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Stock() {
    }

    public Stock(int id, int produitId, BigDecimal quantiteActuelle, BigDecimal seuilAlerte, String uniteMesure, Date dateReception, Date dateExpiration, String emplacement, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.produitId = produitId;
        this.quantiteActuelle = quantiteActuelle;
        this.seuilAlerte = seuilAlerte;
        this.uniteMesure = uniteMesure;
        this.dateReception = dateReception;
        this.dateExpiration = dateExpiration;
        this.emplacement = emplacement;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public int getProduitId() {
        return produitId;
    }

    public BigDecimal getQuantiteActuelle() {
        return quantiteActuelle;
    }

    public BigDecimal getSeuilAlerte() {
        return seuilAlerte;
    }

    public String getUniteMesure() {
        return uniteMesure;
    }

    public Date getDateReception() {
        return dateReception;
    }

    public Date getDateExpiration() {
        return dateExpiration;
    }

    public String getEmplacement() {
        return emplacement;
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
    public void setProduitId(int produitId) {
        this.produitId = produitId;
    }
    public void setQuantiteActuelle(BigDecimal quantiteActuelle) {
        this.quantiteActuelle = quantiteActuelle;
    }
    public void setSeuilAlerte(BigDecimal seuilAlerte) {
        this.seuilAlerte = seuilAlerte;
    }
    public void setUniteMesure(String uniteMesure) {
        this.uniteMesure = uniteMesure;
    }
    public void setDateReception(Date dateReception) {
        this.dateReception = dateReception;
    }
    public void setDateExpiration(Date dateExpiration) {
        this.dateExpiration = dateExpiration;
    }
    public void setEmplacement(String emplacement) {
        this.emplacement = emplacement;
    }
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return id == stock.id;
    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    @Override
    public String toString() {
        return "Stock{" +
                "id=" + id +
                ", produitId=" + produitId +
                ", quantiteActuelle=" + quantiteActuelle +
                ", seuilAlerte=" + seuilAlerte +
                ", uniteMesure='" + uniteMesure + '\'' +
                ", dateReception=" + dateReception +
                ", dateExpiration=" + dateExpiration +
                ", emplacement='" + emplacement + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
