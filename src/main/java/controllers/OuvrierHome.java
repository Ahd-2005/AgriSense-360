package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import entity.user;
import entity.user.Role;
import services.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OuvrierHome {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label loginTimeLabel;

    private user currentUser;

    @FXML
    public void initialize() {
        loadUserInfo();
    }

    private void loadUserInfo() {
        SessionManager sessionManager = SessionManager.getInstance();

        if (sessionManager.isLoggedIn()) {
            currentUser = sessionManager.getCurrentUser();

            // Set welcome message
            welcomeLabel.setText("Bonjour, " + currentUser.getName() + " 👋");

            // Set role
            roleLabel.setText("Rôle: " + getRoleFriendlyName(currentUser.getRole()));

            // Set login time (you can add timestamp to UserSession if needed)
            loginTimeLabel.setText("Connecté aujourd'hui");

        } else {
            // If not logged in, redirect to login
            redirectToLogin();
        }
    }

    private String getRoleFriendlyName(Role role) {
        switch (role) {
            case ROLE_ADMIN:
                return "Administrateur";
            case ROLE_GERANT:
                return "Gérant";
            case ROLE_OUVRIER:
                return "Ouvrier";
            default:
                return role.name();
        }
    }

    @FXML
    private void handleMyTasks() {
        showAlert("Info", "Fonctionnalité 'Mes Tâches' à venir!");
        // TODO: Navigate to tasks page
    }

    @FXML
    private void handleTimeSheet() {
        showAlert("Info", "Fonctionnalité 'Feuille de Temps' à venir!");
        // TODO: Navigate to time sheet page
    }

    @FXML
    private void handleMyProjects() {
        showAlert("Info", "Fonctionnalité 'Mes Projets' à venir!");
        // TODO: Navigate to projects page
    }

    @FXML
    private void handleMyProfile() {
        showAlert("Info", "Fonctionnalité 'Mon Profil' à venir!");
        // TODO: Navigate to profile page using MainLayoutController
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) {
            controller.navigateToProfile();
        }
    }

    @FXML
    private void handleLogout() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Déconnexion");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir vous déconnecter?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Clear session
                SessionManager.getInstance().logout();

                // Redirect to login
                redirectToLogin();
            }
        });
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
            showAlert("Erreur", "Échec de retour à la page de connexion: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}