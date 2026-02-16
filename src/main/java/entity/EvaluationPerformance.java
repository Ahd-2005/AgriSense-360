package entity;

import java.time.LocalDate;

public class EvaluationPerformance {

    private int idEvaluation;
    private int idAffectation; // FK
    private int note;
    private String qualite;
    private String commentaire;
    private LocalDate dateEvaluation;

    public EvaluationPerformance() {}

    public EvaluationPerformance(int idEvaluation, int idAffectation, int note,
                                 String qualite, String commentaire, LocalDate dateEvaluation) {
        this.idEvaluation = idEvaluation;
        this.idAffectation = idAffectation;
        this.note = note;
        this.qualite = qualite;
        this.commentaire = commentaire;
        this.dateEvaluation = dateEvaluation;
    }

    public int getIdEvaluation() {
        return idEvaluation;
    }

    public void setIdEvaluation(int idEvaluation) {
        this.idEvaluation = idEvaluation;
    }

    public int getIdAffectation() {
        return idAffectation;
    }

    public void setIdAffectation(int idAffectation) {
        this.idAffectation = idAffectation;
    }

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    public String getQualite() {
        return qualite;
    }

    public void setQualite(String qualite) {
        this.qualite = qualite;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public LocalDate getDateEvaluation() {
        return dateEvaluation;
    }

    public void setDateEvaluation(LocalDate dateEvaluation) {
        this.dateEvaluation = dateEvaluation;
    }
}
