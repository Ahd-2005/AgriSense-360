package entity;

import java.time.LocalDate;

public class AffectationTravail {

    private int idAffectation;
    private String typeTravail;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String zoneTravail;
    private String statut;

    public AffectationTravail() {}

    public AffectationTravail(int idAffectation, String typeTravail, LocalDate dateDebut,
                              LocalDate dateFin, String zoneTravail, String statut) {
        this.idAffectation = idAffectation;
        this.typeTravail = typeTravail;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.zoneTravail = zoneTravail;
        this.statut = statut;
    }

    public int getIdAffectation() {
        return idAffectation;
    }

    public void setIdAffectation(int idAffectation) {
        this.idAffectation = idAffectation;
    }

    public String getTypeTravail() {
        return typeTravail;
    }

    public void setTypeTravail(String typeTravail) {
        this.typeTravail = typeTravail;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public String getZoneTravail() {
        return zoneTravail;
    }

    public void setZoneTravail(String zoneTravail) {
        this.zoneTravail = zoneTravail;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}
