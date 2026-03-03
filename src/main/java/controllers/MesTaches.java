package controllers;

import entity.Tache;
import entity.Tache.Statut;
import entity.user;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.SessionManager;
import services.TacheService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class MesTaches {

    @FXML private Label  welcomeLabel;
    @FXML private HBox   statsBar;
    @FXML private VBox   tachesContainer;
    @FXML private ToggleButton btnTous;
    @FXML private ToggleButton btnEnAttente;
    @FXML private ToggleButton btnEnCours;
    @FXML private ToggleButton btnTerminees;

    private user currentUser;
    private List<Tache> allTaches;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isLoggedIn()) return;

        currentUser = sm.getCurrentUser();
        welcomeLabel.setText("Bienvenue, " + currentUser.getName() + " 👋");

        // Sélectionner "Toutes" par défaut
        btnTous.setSelected(true);
        styleToggleButtons();

        loadTaches();
    }

    // ───────────────────────────────────────────────
    // CHARGEMENT
    // ───────────────────────────────────────────────
    private void loadTaches() {
        try {
            allTaches = new TacheService().getTachesParOuvrier(currentUser.getId());
            buildStatsBar();
            renderTaches(allTaches);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ───────────────────────────────────────────────
    // BARRE DE STATS
    // ───────────────────────────────────────────────
    private void buildStatsBar() {
        statsBar.getChildren().clear();
        long total     = allTaches.size();
        long attente   = allTaches.stream().filter(t -> t.getStatut() == Statut.EN_ATTENTE).count();
        long enCours   = allTaches.stream().filter(t -> t.getStatut() == Statut.EN_COURS).count();
        long terminees = allTaches.stream().filter(t -> t.getStatut() == Statut.TERMINEE).count();

        statsBar.getChildren().addAll(
                statCard("Total",       String.valueOf(total),     "#2d7a3a", "📋"),
                statCard("En attente",  String.valueOf(attente),   "#f39c12", "⏳"),
                statCard("En cours",    String.valueOf(enCours),   "#2980b9", "🔄"),
                statCard("Terminées",   String.valueOf(terminees), "#27ae60", "✅")
        );
    }

    private VBox statCard(String label, String value, String color, String icon) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 24, 14, 24));
        card.setStyle("-fx-background-color:white;-fx-background-radius:14px;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);" +
                "-fx-border-color:rgba(43,59,31,0.08);-fx-border-width:1;-fx-border-radius:14px;");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size:22px;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(34,48,27,0.6);");
        card.getChildren().addAll(ico, val, lbl);
        return card;
    }

    // ───────────────────────────────────────────────
    // FILTRE
    // ───────────────────────────────────────────────
    @FXML
    private void handleFilter() {
        styleToggleButtons();
        if (allTaches == null) return;

        List<Tache> filtered;
        if (btnEnAttente.isSelected())
            filtered = allTaches.stream().filter(t -> t.getStatut() == Statut.EN_ATTENTE).collect(Collectors.toList());
        else if (btnEnCours.isSelected())
            filtered = allTaches.stream().filter(t -> t.getStatut() == Statut.EN_COURS).collect(Collectors.toList());
        else if (btnTerminees.isSelected())
            filtered = allTaches.stream().filter(t -> t.getStatut() == Statut.TERMINEE).collect(Collectors.toList());
        else
            filtered = allTaches;

        renderTaches(filtered);
    }

    private void styleToggleButtons() {
        for (ToggleButton btn : new ToggleButton[]{btnTous, btnEnAttente, btnEnCours, btnTerminees}) {
            if (btn.isSelected()) {
                btn.setStyle("-fx-background-color:#2d6a1f;-fx-text-fill:white;" +
                        "-fx-background-radius:20px;-fx-cursor:hand;-fx-font-size:12px;" +
                        "-fx-font-weight:bold;-fx-padding:7 16;");
            } else {
                btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#555;" +
                        "-fx-border-color:rgba(43,59,31,0.25);-fx-border-width:1;" +
                        "-fx-background-radius:20px;-fx-border-radius:20px;" +
                        "-fx-cursor:hand;-fx-font-size:12px;-fx-padding:7 16;");
            }
        }
    }

    // ───────────────────────────────────────────────
    // RENDU DES CARTES
    // ───────────────────────────────────────────────
    private void renderTaches(List<Tache> taches) {
        tachesContainer.getChildren().clear();
        if (taches == null || taches.isEmpty()) {
            Label empty = new Label("🎉 Aucune tâche dans cette catégorie !");
            empty.setStyle("-fx-font-size:16px;-fx-text-fill:rgba(34,48,27,0.5);-fx-padding:30;");
            tachesContainer.getChildren().add(empty);
            return;
        }
        for (Tache t : taches) tachesContainer.getChildren().add(createTacheCard(t));
    }

    private VBox createTacheCard(Tache t) {
        boolean estTerminee = t.getStatut() == Statut.TERMINEE;

        VBox card = new VBox(10);
        card.setPadding(new Insets(18, 20, 18, 18));
        card.setStyle("-fx-background-color:" + (estTerminee ? "#f9fff9" : "white") + ";" +
                "-fx-background-radius:16px;" +
                "-fx-border-color:" + (estTerminee ? "rgba(39,174,96,0.3)" : "rgba(43,59,31,0.08)") + ";" +
                "-fx-border-width:1;-fx-border-radius:16px;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");

        // ── Ligne titre ──
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // Checkbox pour marquer terminé/en attente
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(estTerminee);
        checkBox.setStyle("-fx-cursor:hand;");
        checkBox.setOnAction(e -> handleToggleStatut(t, checkBox.isSelected()));

        Label titre = new Label(t.getTitre());
        titre.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" +
                (estTerminee ? "rgba(34,48,27,0.4)" : "#22301b") + ";" +
                (estTerminee ? "-fx-strikethrough:true;" : ""));
        HBox.setHgrow(titre, Priority.ALWAYS);

        // Badges
        Label prioriteBadge = new Label(t.getPrioriteLabel());
        prioriteBadge.setStyle("-fx-padding:3 12;-fx-background-radius:10px;-fx-font-size:11px;" +
                "-fx-font-weight:bold;" + t.getPrioriteStyle());

        Label statutBadge = new Label(t.getStatutLabel());
        statutBadge.setStyle("-fx-padding:3 12;-fx-background-radius:10px;-fx-font-size:11px;" +
                "-fx-font-weight:bold;" + t.getStatutStyle());

        titleRow.getChildren().addAll(checkBox, titre, prioriteBadge, statutBadge);

        // ── Ligne description + date ──
        HBox metaRow = new HBox(20);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.setPadding(new Insets(0, 0, 0, 28));

        if (t.getDescription() != null && !t.getDescription().isBlank()) {
            Label desc = new Label(t.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-font-size:13px;-fx-text-fill:rgba(34,48,27,0.65);");
            HBox.setHgrow(desc, Priority.ALWAYS);
            metaRow.getChildren().add(desc);
        }

        if (t.getDateEcheance() != null) {
            boolean enRetard = !estTerminee && t.getDateEcheance().isBefore(java.time.LocalDate.now());
            Label date = new Label("📅 Échéance : " + t.getDateEcheance().format(DATE_FMT));
            date.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" +
                    (enRetard ? "#e74c3c" : "rgba(34,48,27,0.55)") + ";");
            metaRow.getChildren().add(date);
        }

        // ── Bouton "En cours" (uniquement si EN_ATTENTE) ──
        if (t.getStatut() == Statut.EN_ATTENTE) {
            HBox actionRow = new HBox();
            actionRow.setPadding(new Insets(4, 0, 0, 28));
            Button startBtn = new Button("▶ Démarrer");
            startBtn.setStyle("-fx-background-color:rgba(41,128,185,0.12);-fx-text-fill:#2980b9;" +
                    "-fx-font-weight:bold;-fx-font-size:12px;-fx-padding:6 16;" +
                    "-fx-background-radius:10px;-fx-cursor:hand;-fx-border-width:0;");
            startBtn.setOnAction(e -> {
                updateStatut(t, Statut.EN_COURS);
            });
            actionRow.getChildren().add(startBtn);
            card.getChildren().addAll(titleRow, metaRow, actionRow);
        } else {
            card.getChildren().addAll(titleRow, metaRow);
        }

        return card;
    }

    // ───────────────────────────────────────────────
    // MISE À JOUR STATUT
    // ───────────────────────────────────────────────
    private void handleToggleStatut(Tache t, boolean checked) {
        Statut newStatut = checked ? Statut.TERMINEE : Statut.EN_ATTENTE;
        updateStatut(t, newStatut);
    }

    private void updateStatut(Tache t, Statut newStatut) {
        try {
            new TacheService().updateStatut(t.getId(), newStatut);
            t.setStatut(newStatut);
            loadTaches(); // Refresh complet pour recalculer les stats
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de mettre à jour le statut : " + e.getMessage());
        }
    }

    // ───────────────────────────────────────────────
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}