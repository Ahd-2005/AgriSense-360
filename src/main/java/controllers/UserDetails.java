package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import entity.user;
import entity.user.Role;
import services.userservice;
import services.SessionManager;

import java.sql.SQLException;

public class UserDetails {

    @FXML private StackPane avatarPane;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;
    @FXML private Label roleBadgeLabel;
    @FXML private Button activateBtn;
    @FXML private Button blockBtn;
    @FXML private Button deleteBtn;

    private user currentUser;
    private user loggedInUser;
    private AdminDashboard adminDashboard;

    @FXML
    public void initialize() {
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            this.loggedInUser = sessionManager.getCurrentUser();
        }
    }

    public void setUser(user user) {
        this.currentUser = user;
        displayUserInfo();
        configurePermissions();
    }

    public void setAdminDashboard(AdminDashboard dashboard) {
        this.adminDashboard = dashboard;
    }

    // ───────────────────────────────────────────────
    // AFFICHAGE
    // ───────────────────────────────────────────────
    private void displayUserInfo() {
        nameLabel.setText(currentUser.getName());
        emailLabel.setText(currentUser.getEmail());
        phoneLabel.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "—");
        roleLabel.setText(getRoleFriendlyName(currentUser.getRole()));
        roleBadgeLabel.setText(getRoleFriendlyName(currentUser.getRole()));

        // Avatar
        buildAvatar();

        // Statut
        String status = currentUser.getStatus();
        if ("ACTIVE".equals(status)) {
            statusLabel.setText("✔ Actif");
            statusLabel.setStyle(
                    "-fx-padding: 5 16; -fx-background-radius: 12px; -fx-font-size: 12px; -fx-font-weight: bold;" +
                            "-fx-background-color: rgba(255,255,255,0.25); -fx-text-fill: #e8f5e9;");
            activateBtn.setDisable(true);
            blockBtn.setDisable(false);
        } else {
            statusLabel.setText("✖ Bloqué");
            statusLabel.setStyle(
                    "-fx-padding: 5 16; -fx-background-radius: 12px; -fx-font-size: 12px; -fx-font-weight: bold;" +
                            "-fx-background-color: rgba(231,76,60,0.3); -fx-text-fill: #ffcccc;");
            activateBtn.setDisable(false);
            blockBtn.setDisable(true);
        }

        if (loggedInUser != null && currentUser.getId() == loggedInUser.getId()) {
            blockBtn.setDisable(true);
        }
    }

    private void buildAvatar() {
        avatarPane.getChildren().clear();
        String picUrl = currentUser.getProfilePicture();

        if (picUrl != null && !picUrl.isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(picUrl, 80, 80, true, true));
                iv.setFitWidth(80); iv.setFitHeight(80);
                Circle clip = new Circle(40, 40, 40);
                iv.setClip(clip);
                iv.setStyle("-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.25),8,0,0,3);");
                avatarPane.getChildren().add(iv);
                return;
            } catch (Exception ignored) {}
        }

        // Initiales colorées (même style que AdminDashboard)
        String initials = getInitials(currentUser.getName());
        Label lbl = new Label(initials);
        lbl.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane bg = new StackPane(lbl);
        bg.setMinSize(80, 80); bg.setMaxSize(80, 80);
        bg.setStyle("-fx-background-color:" + getAvatarColor(currentUser.getName()) + ";" +
                "-fx-background-radius: 40;");
        avatarPane.getChildren().add(bg);
    }

    private void configurePermissions() {
        if (loggedInUser != null && currentUser != null
                && loggedInUser.getId() == currentUser.getId()) {
            if (deleteBtn != null) deleteBtn.setDisable(true);
            if (blockBtn  != null) blockBtn.setDisable(true);
        }
    }

    // ───────────────────────────────────────────────
    // ACTIONS STATUT
    // ───────────────────────────────────────────────
    @FXML
    private void handleActivate() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer l'activation");
        confirm.setHeaderText("Activer l'utilisateur");
        confirm.setContentText("Activer le compte de " + currentUser.getName() + " ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    new userservice().toggleUserStatus(currentUser.getId(), "ACTIVE");
                    currentUser.setStatus("ACTIVE");
                    displayUserInfo();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur activé avec succès !");
                    if (adminDashboard != null) adminDashboard.refreshTable();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'activation : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBlock() {
        if (loggedInUser != null && currentUser.getId() == loggedInUser.getId()) {
            showAlert(Alert.AlertType.WARNING, "Action impossible",
                    "Vous ne pouvez pas bloquer votre propre compte !");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer le blocage");
        confirm.setHeaderText("Bloquer l'utilisateur");
        confirm.setContentText("Bloquer le compte de " + currentUser.getName() + " ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    new userservice().toggleUserStatus(currentUser.getId(), "BLOCKED");
                    currentUser.setStatus("BLOCKED");
                    displayUserInfo();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur bloqué avec succès !");
                    if (adminDashboard != null) adminDashboard.refreshTable();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec du blocage : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleModifier() {
        loadInMainLayout("/fxml/UserEdit.fxml", controller -> {
            UserEdit editController = (UserEdit) controller;
            editController.setUser(currentUser);
            editController.setAdminDashboard(adminDashboard);
        });
    }

    @FXML
    private void handleDelete() {
        if (loggedInUser != null && currentUser.getId() == loggedInUser.getId()) {
            showAlert(Alert.AlertType.WARNING, "Action impossible",
                    "Vous ne pouvez pas supprimer votre propre compte !");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer l'utilisateur");
        confirm.setContentText("Supprimer définitivement " + currentUser.getName() + " ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    new userservice().deleteUser(currentUser.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur supprimé avec succès !");
                    if (adminDashboard != null) adminDashboard.refreshTable();
                    handleBack();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la suppression : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBack() {
        loadInMainLayout("/fxml/AdminDashboard.fxml", controller -> {
            if (controller instanceof AdminDashboard)
                ((AdminDashboard) controller).refreshTable();
        });
    }

    // ───────────────────────────────────────────────
    // UTILITAIRES
    // ───────────────────────────────────────────────
    private String getRoleFriendlyName(Role role) {
        switch (role) {
            case ROLE_ADMIN:   return "Administrateur";
            case ROLE_GERANT:  return "Gérant";
            case ROLE_OUVRIER: return "Ouvrier";
            default:           return role.name();
        }
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la page : " + e.getMessage());
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
