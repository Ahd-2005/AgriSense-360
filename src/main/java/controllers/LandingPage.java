package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import services.SessionManager;

public class LandingPage {

    @FXML
    public void initialize() {
        // Check if user is already logged in
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            // User has active session, redirect to main layout
            redirectToMainLayout();
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Connexion - AgriSense 360");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page de connexion: " + e.getMessage());
        }
    }

    @FXML
    private void goToSignup(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/signup.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Créer un compte - AgriSense 360");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page d'inscription: " + e.getMessage());
        }
    }

    /**
     * Redirect to main layout if user is already logged in
     */
    private void redirectToMainLayout() {
        try {
            SessionManager sessionManager = SessionManager.getInstance();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
            Parent root = loader.load();

            // Configure the main layout for the user's role
            MainLayoutController mainController = loader.getController();
            mainController.configureForUserRole(sessionManager.getCurrentUser().getRole());
            mainController.setCurrentUser(sessionManager.getCurrentUser());

            // Get the current stage (need a node reference, but we're in initialize)
            // This will be handled by Main.java on startup instead

        } catch (Exception e) {
            e.printStackTrace();
            // If redirect fails, stay on landing page
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}