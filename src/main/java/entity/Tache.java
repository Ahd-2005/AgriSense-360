package entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Tache {

    public enum Statut   { EN_ATTENTE, EN_COURS, TERMINEE }
    public enum Priorite { BASSE, NORMALE, HAUTE }

    private int           id;
    private String        titre;
    private String        description;
    private int           ouvrierId;
    private Statut        statut;
    private Priorite      priorite;
    private LocalDateTime dateCreation;
    private LocalDate     dateEcheance;

    // Transient — rempli à l'affichage, pas stocké en BD
    private String ouvrierNom;

    // ── Constructeurs ──────────────────────────────
    public Tache() {
        this.statut   = Statut.EN_ATTENTE;
        this.priorite = Priorite.NORMALE;
    }

    public Tache(String titre, String description, int ouvrierId,
                 Priorite priorite, LocalDate dateEcheance) {
        this();
        this.titre        = titre;
        this.description  = description;
        this.ouvrierId    = ouvrierId;
        this.priorite     = priorite;
        this.dateEcheance = dateEcheance;
    }

    // ── Getters / Setters ──────────────────────────
    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public String getTitre()                  { return titre; }
    public void setTitre(String titre)        { this.titre = titre; }

    public String getDescription()            { return description; }
    public void setDescription(String d)      { this.description = d; }

    public int getOuvrierId()                 { return ouvrierId; }
    public void setOuvrierId(int ouvrierId)   { this.ouvrierId = ouvrierId; }

    public Statut getStatut()                 { return statut; }
    public void setStatut(Statut statut)      { this.statut = statut; }

    public Priorite getPriorite()             { return priorite; }
    public void setPriorite(Priorite p)       { this.priorite = p; }

    public LocalDateTime getDateCreation()    { return dateCreation; }
    public void setDateCreation(LocalDateTime d) { this.dateCreation = d; }

    public LocalDate getDateEcheance()        { return dateEcheance; }
    public void setDateEcheance(LocalDate d)  { this.dateEcheance = d; }

    public String getOuvrierNom()             { return ouvrierNom; }
    public void setOuvrierNom(String nom)     { this.ouvrierNom = nom; }

    // ── Helpers d'affichage ───────────────────────
    public String getStatutLabel() {
        switch (statut) {
            case EN_ATTENTE: return "En attente";
            case EN_COURS:   return "En cours";
            case TERMINEE:   return "Terminée";
            default:         return statut.name();
        }
    }

    public String getPrioriteLabel() {
        switch (priorite) {
            case BASSE:   return "Basse";
            case NORMALE: return "Normale";
            case HAUTE:   return "Haute";
            default:      return priorite.name();
        }
    }

    public String getStatutStyle() {
        switch (statut) {
            case EN_ATTENTE:
                return "-fx-background-color:rgba(243,156,18,0.15);-fx-text-fill:#f39c12;";
            case EN_COURS:
                return "-fx-background-color:rgba(41,128,185,0.15);-fx-text-fill:#2980b9;";
            case TERMINEE:
                return "-fx-background-color:rgba(39,174,96,0.15);-fx-text-fill:#27ae60;";
            default:
                return "";
        }
    }

    public String getPrioriteStyle() {
        switch (priorite) {
            case HAUTE:   return "-fx-background-color:rgba(231,76,60,0.15);-fx-text-fill:#e74c3c;";
            case NORMALE: return "-fx-background-color:rgba(52,152,219,0.15);-fx-text-fill:#3498db;";
            case BASSE:   return "-fx-background-color:rgba(149,165,166,0.15);-fx-text-fill:#95a5a6;";
            default:      return "";
        }
    }

    @Override
    public String toString() {
        return "Tache{id=" + id + ", titre='" + titre + "', statut=" + statut + "}";
    }
}
