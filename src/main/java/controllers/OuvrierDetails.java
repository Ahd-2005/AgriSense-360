package controllers;

import entity.Tache;
import entity.user;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import services.SessionManager;
import services.TacheService;
import services.userservice;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class OuvrierDetails {

    @FXML private StackPane avatarPane;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label statusLabel;
    @FXML private Label tacheCountLabel;
    @FXML private Button deleteBtn;
    @FXML private VBox   tachesContainer;
    @FXML private ComboBox<String> tacheFilterCombo;

    private user currentOuvrier;
    private user loggedInUser;
    private OuvrierManagement ouvrierManagement;
    private List<Tache> allTaches;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();
        if (sm.isLoggedIn()) loggedInUser = sm.getCurrentUser();

        tacheFilterCombo.setItems(FXCollections.observableArrayList(
                "Toutes", "En attente", "En cours", "Terminées"
        ));
        tacheFilterCombo.setValue("Toutes");
    }

    public void setOuvrier(user ouvrier) {
        this.currentOuvrier = ouvrier;
        displayInfo();
        configurePermissions();
        loadTaches();
    }

    public void setOuvrierManagement(OuvrierManagement management) {
        this.ouvrierManagement = management;
    }

    public OuvrierManagement getOuvrierManagement() {
        return ouvrierManagement;
    }

    // ───────────────────────────────────────────────
    // AFFICHAGE INFOS OUVRIER
    // ───────────────────────────────────────────────
    private void displayInfo() {
        nameLabel.setText(currentOuvrier.getName());
        emailLabel.setText(currentOuvrier.getEmail());
        phoneLabel.setText(currentOuvrier.getPhone() != null ? currentOuvrier.getPhone() : "—");
        buildAvatar();

        if ("ACTIVE".equals(currentOuvrier.getStatus())) {
            statusLabel.setText("✔ Actif");
            statusLabel.setStyle("-fx-padding:5 16;-fx-background-radius:12px;-fx-font-size:12px;" +
                    "-fx-font-weight:bold;-fx-background-color:rgba(255,255,255,0.25);-fx-text-fill:#e8f5e9;");
        } else {
            statusLabel.setText("✖ Bloqué");
            statusLabel.setStyle("-fx-padding:5 16;-fx-background-radius:12px;-fx-font-size:12px;" +
                    "-fx-font-weight:bold;-fx-background-color:rgba(231,76,60,0.3);-fx-text-fill:#ffcccc;");
        }
    }

    private void buildAvatar() {
        avatarPane.getChildren().clear();
        String picUrl = currentOuvrier.getProfilePicture();
        if (picUrl != null && !picUrl.isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(picUrl, 80, 80, true, true));
                iv.setFitWidth(80); iv.setFitHeight(80);
                iv.setClip(new Circle(40, 40, 40));
                avatarPane.getChildren().add(iv);
                return;
            } catch (Exception ignored) {}
        }
        Label lbl = new Label(getInitials(currentOuvrier.getName()));
        lbl.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:white;");
        StackPane bg = new StackPane(lbl);
        bg.setMinSize(80, 80); bg.setMaxSize(80, 80);
        bg.setStyle("-fx-background-color:" + getAvatarColor(currentOuvrier.getName()) +
                ";-fx-background-radius:40;");
        avatarPane.getChildren().add(bg);
    }

    private void configurePermissions() {
        if (loggedInUser != null && currentOuvrier != null
                && loggedInUser.getId() == currentOuvrier.getId() && deleteBtn != null)
            deleteBtn.setDisable(true);
    }

    // ───────────────────────────────────────────────
    // TÂCHES
    // ───────────────────────────────────────────────
    private void loadTaches() {
        try {
            allTaches = new TacheService().getTachesParOuvrier(currentOuvrier.getId());
            long actives = allTaches.stream()
                    .filter(t -> t.getStatut() != Tache.Statut.TERMINEE).count();
            tacheCountLabel.setText("📋 " + actives + " tâche" + (actives > 1 ? "s" : "") + " active" + (actives > 1 ? "s" : ""));
            renderTaches(allTaches);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFilterTaches() {
        if (allTaches == null) return;
        String filter = tacheFilterCombo.getValue();
        List<Tache> filtered;
        switch (filter) {
            case "En attente": filtered = allTaches.stream().filter(t -> t.getStatut() == Tache.Statut.EN_ATTENTE).collect(Collectors.toList()); break;
            case "En cours":   filtered = allTaches.stream().filter(t -> t.getStatut() == Tache.Statut.EN_COURS).collect(Collectors.toList());   break;
            case "Terminées":  filtered = allTaches.stream().filter(t -> t.getStatut() == Tache.Statut.TERMINEE).collect(Collectors.toList());   break;
            default:           filtered = allTaches;
        }
        renderTaches(filtered);
    }

    private void renderTaches(List<Tache> taches) {
        tachesContainer.getChildren().clear();
        if (taches == null || taches.isEmpty()) {
            Label empty = new Label("Aucune tâche pour le moment.");
            empty.setStyle("-fx-font-size:14px;-fx-text-fill:rgba(34,48,27,0.5);-fx-padding:20;");
            tachesContainer.getChildren().add(empty);
            return;
        }
        for (Tache t : taches) tachesContainer.getChildren().add(createTacheCard(t));
    }

    private HBox createTacheCard(Tache t) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 18));
        card.setStyle("-fx-background-color:white;-fx-background-radius:14px;" +
                "-fx-border-color:rgba(43,59,31,0.08);-fx-border-width:1;" +
                "-fx-border-radius:14px;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");

        // Couleur bande gauche selon priorité
        String barColor;
        switch (t.getPriorite()) {
            case HAUTE:   barColor = "#e74c3c"; break;
            case NORMALE: barColor = "#3498db"; break;
            default:      barColor = "#95a5a6";
        }
        Region bar = new Region();
        bar.setMinWidth(5); bar.setMaxWidth(5);
        bar.setStyle("-fx-background-color:" + barColor + ";-fx-background-radius:3;");

        // Infos
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleLine = new HBox(10);
        titleLine.setAlignment(Pos.CENTER_LEFT);
        Label titre = new Label(t.getTitre());
        titre.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#22301b;");
        Label prioriteBadge = new Label(t.getPrioriteLabel());
        prioriteBadge.setStyle("-fx-padding:2 10;-fx-background-radius:8px;-fx-font-size:11px;" +
                "-fx-font-weight:bold;" + t.getPrioriteStyle());
        Label statutBadge = new Label(t.getStatutLabel());
        statutBadge.setStyle("-fx-padding:2 10;-fx-background-radius:8px;-fx-font-size:11px;" +
                "-fx-font-weight:bold;" + t.getStatutStyle());
        titleLine.getChildren().addAll(titre, prioriteBadge, statutBadge);

        HBox metaLine = new HBox(18);
        metaLine.setAlignment(Pos.CENTER_LEFT);
        if (t.getDescription() != null && !t.getDescription().isBlank()) {
            Label desc = new Label(t.getDescription().length() > 60
                    ? t.getDescription().substring(0, 60) + "..." : t.getDescription());
            desc.setStyle("-fx-font-size:13px;-fx-text-fill:rgba(34,48,27,0.6);");
            metaLine.getChildren().add(desc);
        }
        if (t.getDateEcheance() != null) {
            Label date = new Label("📅 " + t.getDateEcheance().format(DATE_FMT));
            date.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(34,48,27,0.55);");
            metaLine.getChildren().add(date);
        }
        info.getChildren().addAll(titleLine, metaLine);

        // Bouton supprimer (gérant)
        Button delBtn = new Button("🗑");
        delBtn.setStyle("-fx-background-color:rgba(231,76,60,0.1);-fx-text-fill:#e74c3c;" +
                "-fx-font-size:14px;-fx-background-radius:8px;-fx-cursor:hand;" +
                "-fx-padding:6 10;-fx-border-width:0;");
        delBtn.setOnAction(e -> handleDeleteTache(t));

        card.getChildren().addAll(bar, info, delBtn);
        return card;
    }

    private void handleDeleteTache(Tache t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la tâche");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer la tâche \"" + t.getTitre() + "\" ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    new TacheService().supprimerTache(t.getId());
                    loadTaches();
                    if (ouvrierManagement != null) ouvrierManagement.refreshTable();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer : " + e.getMessage());
                }
            }
        });
    }

    // ───────────────────────────────────────────────
    // ACTIONS NAVIGATION
    // ───────────────────────────────────────────────
    @FXML
    private void handleAffecterTache() {
        try {
            Parent root = nameLabel.getScene().getRoot();
            if (root instanceof BorderPane) {
                StackPane contentArea = (StackPane) ((BorderPane) root).getCenter();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TacheAdd.fxml"));
                Parent content = loader.load();
                TacheAdd ctrl = loader.getController();
                ctrl.setOuvrier(currentOuvrier);
                ctrl.setOuvrierDetailsController(this);
                contentArea.getChildren().clear();
                contentArea.getChildren().add(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    @FXML
    private void handleEdit() {
        loadInMainLayout("/fxml/OuvrierEdit.fxml", controller -> {
            OuvrierEdit c = (OuvrierEdit) controller;
            c.setOuvrier(currentOuvrier);
            c.setOuvrierManagement(ouvrierManagement);
        });
    }

    @FXML
    private void handleDelete() {
        if (loggedInUser != null && currentOuvrier.getId() == loggedInUser.getId()) {
            showAlert(Alert.AlertType.WARNING, "Action impossible", "Vous ne pouvez pas supprimer votre propre compte !");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer définitivement " + currentOuvrier.getName() + " ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    new userservice().deleteUser(currentOuvrier.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Ouvrier supprimé avec succès !");
                    if (ouvrierManagement != null) ouvrierManagement.refreshTable();
                    handleBack();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBack() {
        loadInMainLayout("/fxml/OuvrierManagement.fxml", controller -> {
            if (controller instanceof OuvrierManagement)
                ((OuvrierManagement) controller).refreshTable();
        });
    }

    // ───────────────────────────────────────────────
    // UTILITAIRES
    // ───────────────────────────────────────────────
    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
        return (p[0].charAt(0) + "" + p[1].charAt(0)).toUpperCase();
    }

    private String getAvatarColor(String name) {
        String[] colors = {"#2d7a3a","#2980b9","#8e44ad","#c0392b","#d35400","#16a085","#2c3e50"};
        return colors[Math.abs(name.hashCode()) % colors.length];
    }

    private void loadInMainLayout(String fxmlPath, ControllerCallback callback) {
        try {
            Parent root = nameLabel.getScene().getRoot();
            if (root instanceof BorderPane) {
                StackPane contentArea = (StackPane) ((BorderPane) root).getCenter();
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent content = loader.load();
                if (callback != null) callback.onControllerLoaded(loader.getController());
                contentArea.getChildren().clear();
                contentArea.getChildren().add(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger : " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ControllerCallback { void onControllerLoaded(Object controller); }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}