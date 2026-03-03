package controllers;

import entity.Tache;
import entity.Tache.Statut;
import entity.user;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import services.SessionManager;
import services.TacheService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OuvrierHome {

    @FXML private StackPane avatarPane;
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label dateLabel;
    @FXML private Label tachesTotalLabel;
    @FXML private Label tachesActivesLabel;
    @FXML private Label tachesTermineesLabel;
    @FXML private VBox  recentTachesContainer;

    private user currentUser;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isLoggedIn()) { redirectToLogin(); return; }

        currentUser = sm.getCurrentUser();
        loadUserInfo();
        loadTacheStats();
        loadRecentTaches();
    }

    // ───────────────────────────────────────────────
    // INFOS UTILISATEUR
    // ───────────────────────────────────────────────
    private void loadUserInfo() {
        welcomeLabel.setText("Bonjour, " + currentUser.getName() + " 👋");
        roleLabel.setText("Rôle : Ouvrier");
        dateLabel.setText("Aujourd'hui : " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy",
                java.util.Locale.FRENCH)));
        buildAvatar();
    }

    private void buildAvatar() {
        avatarPane.getChildren().clear();
        String picUrl = currentUser.getProfilePicture();
        if (picUrl != null && !picUrl.isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(picUrl, 72, 72, true, true));
                iv.setFitWidth(72); iv.setFitHeight(72);
                iv.setClip(new Circle(36, 36, 36));
                avatarPane.getChildren().add(iv);
                return;
            } catch (Exception ignored) {}
        }
        Label lbl = new Label(getInitials(currentUser.getName()));
        lbl.setStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:white;");
        StackPane bg = new StackPane(lbl);
        bg.setMinSize(72, 72); bg.setMaxSize(72, 72);
        bg.setStyle("-fx-background-color:" + getAvatarColor(currentUser.getName()) +
                ";-fx-background-radius:36;" +
                "-fx-border-color:rgba(255,255,255,0.4);-fx-border-width:3;-fx-border-radius:36;");
        avatarPane.getChildren().add(bg);
    }

    // ───────────────────────────────────────────────
    // STATS TÂCHES
    // ───────────────────────────────────────────────
    private void loadTacheStats() {
        try {
            List<Tache> taches = new TacheService().getTachesParOuvrier(currentUser.getId());
            long total     = taches.size();
            long actives   = taches.stream().filter(t -> t.getStatut() != Statut.TERMINEE).count();
            long terminees = taches.stream().filter(t -> t.getStatut() == Statut.TERMINEE).count();

            tachesTotalLabel.setText(String.valueOf(total));
            tachesActivesLabel.setText(String.valueOf(actives));
            tachesTermineesLabel.setText(String.valueOf(terminees));

        } catch (SQLException e) {
            tachesTotalLabel.setText("—");
            tachesActivesLabel.setText("—");
            tachesTermineesLabel.setText("—");
        }
    }

    // ───────────────────────────────────────────────
    // TÂCHES RÉCENTES (3 dernières)
    // ───────────────────────────────────────────────
    private void loadRecentTaches() {
        recentTachesContainer.getChildren().clear();
        try {
            List<Tache> taches = new TacheService().getTachesParOuvrier(currentUser.getId());

            if (taches.isEmpty()) {
                Label empty = new Label("🎉 Aucune tâche assignée pour le moment.");
                empty.setStyle("-fx-font-size:14px;-fx-text-fill:rgba(34,48,27,0.5);-fx-padding:16;");
                recentTachesContainer.getChildren().add(empty);
                return;
            }

            // Prendre les 3 premières (déjà triées par date DESC dans le service)
            taches.stream().limit(3).forEach(t -> recentTachesContainer.getChildren().add(createMiniCard(t)));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox createMiniCard(Tache t) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 18, 14, 16));
        card.setStyle("-fx-background-color:white;-fx-background-radius:14px;" +
                "-fx-border-color:rgba(43,59,31,0.08);-fx-border-width:1;" +
                "-fx-border-radius:14px;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");

        // Bande couleur gauche selon statut
        String barColor;
        switch (t.getStatut()) {
            case TERMINEE:   barColor = "#27ae60"; break;
            case EN_COURS:   barColor = "#2980b9"; break;
            default:         barColor = "#f39c12";
        }
        Region bar = new Region();
        bar.setMinWidth(5); bar.setMaxWidth(5);
        bar.setStyle("-fx-background-color:" + barColor + ";-fx-background-radius:3;");

        // Infos
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox topLine = new HBox(8);
        topLine.setAlignment(Pos.CENTER_LEFT);

        Label titre = new Label(t.getTitre());
        titre.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#22301b;" +
                (t.getStatut() == Statut.TERMINEE ? "-fx-strikethrough:true;-fx-opacity:0.6;" : ""));
        HBox.setHgrow(titre, Priority.ALWAYS);

        Label prioriteBadge = new Label(t.getPrioriteLabel());
        prioriteBadge.setStyle("-fx-padding:2 8;-fx-background-radius:8px;-fx-font-size:10px;" +
                "-fx-font-weight:bold;" + t.getPrioriteStyle());
        topLine.getChildren().addAll(titre, prioriteBadge);

        HBox bottomLine = new HBox(12);
        bottomLine.setAlignment(Pos.CENTER_LEFT);
        Label statutLbl = new Label(t.getStatutLabel());
        statutLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;" + t.getStatutStyle() +
                "-fx-padding:2 8;-fx-background-radius:8px;");
        bottomLine.getChildren().add(statutLbl);

        if (t.getDateEcheance() != null) {
            boolean enRetard = t.getStatut() != Statut.TERMINEE && t.getDateEcheance().isBefore(LocalDate.now());
            Label date = new Label("📅 " + t.getDateEcheance().format(DATE_FMT));
            date.setStyle("-fx-font-size:11px;-fx-text-fill:" + (enRetard ? "#e74c3c" : "rgba(34,48,27,0.5)") + ";");
            bottomLine.getChildren().add(date);
        }

        info.getChildren().addAll(topLine, bottomLine);
        card.getChildren().addAll(bar, info);
        return card;
    }

    // ───────────────────────────────────────────────
    // NAVIGATION
    // ───────────────────────────────────────────────
    @FXML
    private void handleMyTasks() {
        navigateInLayout("/fxml/mes_taches.fxml");
    }

    @FXML
    private void handleMyProfile() {
        MainLayoutController ctrl = MainLayoutController.getInstance();
        if (ctrl != null) {
            ctrl.navigateToProfile();
        } else {
            showAlert("Info", "Navigation vers le profil non disponible.");
        }
    }

    @FXML
    private void handleLogout() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Déconnexion");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Êtes-vous sûr de vouloir vous déconnecter ?");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                SessionManager.getInstance().logout();
                redirectToLogin();
            }
        });
    }

    // ───────────────────────────────────────────────
    // UTILITAIRES
    // ───────────────────────────────────────────────
    private void navigateInLayout(String fxmlPath) {
        try {
            Parent root = welcomeLabel.getScene().getRoot();
            if (root instanceof BorderPane) {
                StackPane contentArea = (StackPane) ((BorderPane) root).getCenter();
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent content = loader.load();
                contentArea.getChildren().clear();
                contentArea.getChildren().add(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page : " + e.getMessage());
        }
    }

    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Landingpage.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("AgriSense 360");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}