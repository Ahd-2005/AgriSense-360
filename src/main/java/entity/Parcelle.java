package entity;

public class Parcelle {

    private int id;
    private String nom;
    private double surface;
    private String localisation;
    private String typeSol;
    private String statut;

    // Empty constructor
    public Parcelle() {}

    // Constructor without id (for INSERT)
    public Parcelle(String nom, double surface, String localisation, String typeSol, String statut) {
        this.nom = nom;
        this.surface = surface;
        this.localisation = localisation;
        this.typeSol = typeSol;
        this.statut = statut;
    }

    // Constructor with id (for SELECT)
    public Parcelle(int id, String nom, double surface, String localisation, String typeSol, String statut) {
        this.id = id;
        this.nom = nom;
        this.surface = surface;
        this.localisation = localisation;
        this.typeSol = typeSol;
        this.statut = statut;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public double getSurface() { return surface; }
    public void setSurface(double surface) { this.surface = surface; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public String getTypeSol() { return typeSol; }
    public void setTypeSol(String typeSol) { this.typeSol = typeSol; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
