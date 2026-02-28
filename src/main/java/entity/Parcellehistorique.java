package entity;

import java.time.LocalDateTime;

public class Parcellehistorique {

    private int id;
    private int parcelleId;
    private String typeAction;      // CULTURE_AJOUTEE | CULTURE_MODIFIEE | CULTURE_SUPPRIMEE | RECOLTE
    private Integer cultureId;
    private String cultureNom;
    private String typeCulture;
    private Double surface;
    private String etatAvant;
    private String etatApres;
    private LocalDateTime dateAction;
    private String description;
    private Double quantiteRecolte;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Parcellehistorique() {}

    public Parcellehistorique(int parcelleId, String typeAction, Integer cultureId,
                              String cultureNom, String typeCulture, Double surface,
                              String etatAvant, String etatApres, String description,
                              Double quantiteRecolte) {
        this.parcelleId      = parcelleId;
        this.typeAction      = typeAction;
        this.cultureId       = cultureId;
        this.cultureNom      = cultureNom;
        this.typeCulture     = typeCulture;
        this.surface         = surface;
        this.etatAvant       = etatAvant;
        this.etatApres       = etatApres;
        this.description     = description;
        this.quantiteRecolte = quantiteRecolte;
        this.dateAction      = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getParcelleId() { return parcelleId; }
    public void setParcelleId(int parcelleId) { this.parcelleId = parcelleId; }

    public String getTypeAction() { return typeAction; }
    public void setTypeAction(String typeAction) { this.typeAction = typeAction; }

    public Integer getCultureId() { return cultureId; }
    public void setCultureId(Integer cultureId) { this.cultureId = cultureId; }

    public String getCultureNom() { return cultureNom; }
    public void setCultureNom(String cultureNom) { this.cultureNom = cultureNom; }

    public String getTypeCulture() { return typeCulture; }
    public void setTypeCulture(String typeCulture) { this.typeCulture = typeCulture; }

    public Double getSurface() { return surface; }
    public void setSurface(Double surface) { this.surface = surface; }

    public String getEtatAvant() { return etatAvant; }
    public void setEtatAvant(String etatAvant) { this.etatAvant = etatAvant; }

    public String getEtatApres() { return etatApres; }
    public void setEtatApres(String etatApres) { this.etatApres = etatApres; }

    public LocalDateTime getDateAction() { return dateAction; }
    public void setDateAction(LocalDateTime dateAction) { this.dateAction = dateAction; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getQuantiteRecolte() { return quantiteRecolte; }
    public void setQuantiteRecolte(Double quantiteRecolte) { this.quantiteRecolte = quantiteRecolte; }

    // ── Helper: emoji icon for type ───────────────────────────────────────────
    public String getTypeIcon() {
        if (typeAction == null) return "📋";
        switch (typeAction) {
            case "CULTURE_AJOUTEE":    return "🌱";
            case "CULTURE_MODIFIEE":   return "✏️";
            case "CULTURE_SUPPRIMEE":  return "🗑";
            case "RECOLTE":            return "🌾";
            default:                   return "📋";
        }
    }

    public String getTypeLabelFr() {
        if (typeAction == null) return "Action";
        switch (typeAction) {
            case "CULTURE_AJOUTEE":    return "Culture ajoutée";
            case "CULTURE_MODIFIEE":   return "Culture modifiée";
            case "CULTURE_SUPPRIMEE":  return "Culture supprimée";
            case "RECOLTE":            return "Récolte effectuée";
            default:                   return typeAction;
        }
    }
}