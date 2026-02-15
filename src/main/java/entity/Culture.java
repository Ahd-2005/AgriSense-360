package entity;

import java.sql.Date;

public class Culture {

    private int id;
    private String nom;
    private String typeCulture;
    private Date datePlantation;
    private Date dateRecolte;
    private String etat;
    private double surface;
    private String img;
    private int parcelleId; // FK

    public Culture() {}

    // Constructor with all parameters
    public Culture(int id, String nom, String typeCulture, Date datePlantation,
                   Date dateRecolte, String etat, int parcelleId, double surface, String img) {
        this.id = id;
        this.nom = nom;
        this.typeCulture = typeCulture;
        this.datePlantation = datePlantation;
        this.dateRecolte = dateRecolte;
        this.etat = etat;
        this.parcelleId = parcelleId;
        this.surface = surface;
        this.img = img;
    }

    // getters & setters


    public double getSurface() { return surface; }
    public void setSurface(double surface) { this.surface = surface; }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getTypeCulture() {
        return typeCulture;
    }

    public void setTypeCulture(String typeCulture) {
        this.typeCulture = typeCulture;
    }

    public Date getDatePlantation() {
        return datePlantation;
    }

    public void setDatePlantation(Date datePlantation) {
        this.datePlantation = datePlantation;
    }

    public Date getDateRecolte() {
        return dateRecolte;
    }

    public void setDateRecolte(Date dateRecolte) {
        this.dateRecolte = dateRecolte;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public int getParcelleId() {
        return parcelleId;
    }

    public void setParcelleId(int parcelleId) {
        this.parcelleId = parcelleId;
    }
}
