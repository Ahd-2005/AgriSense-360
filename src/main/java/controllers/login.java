package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import entity.user;
import services.userservice;
import services.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class login {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Hyperlink signupLink;

    // Error Labels
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label loginError;

    // Regex pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @FXML
    public void initialize() {
        hideAllErrors();
    }

    @FXML
    private void handleLogin() {
        hideAllErrors();
        boolean isValid = true;

        // Validate email
        if (!validateEmail()) {
            isValid = false;
        }

        // Validate password
        if (!validatePassword()) {
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        try {
            userservice service = new userservice();
            user loggedInUser = service.login(email, password);

            if (loggedInUser != null) {
                // Check if user is blocked
                if ("BLOCKED".equals(loggedInUser.getStatus())) {
                    showError(loginError, "❌ Votre compte a été bloqué. Contactez l'administrateur.");
                    return;
                }

                handleSuccessfulLogin(loggedInUser);
            } else {
                showError(loginError, "❌ Email ou mot de passe incorrect!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showError(loginError, "❌ Erreur de connexion à la base de données");
        }
    }

    // ============= VALIDATION METHODS =============

    private boolean validateEmail() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError(emailError, "❌ L'email est obligatoire");
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError(emailError, "❌ Format d'email invalide");
            return false;
        }

        return true;
    }

    private boolean validatePassword() {
        String password = passwordField.getText().trim();

        if (password.isEmpty()) {
            showError(passwordError, "❌ Le mot de passe est obligatoire");
            return false;
        }

        return true;
    }

    // ============= HELPER METHODS =============

    private void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideAllErrors() {
        if (emailError != null) emailError.setVisible(false);
        if (passwordError != null) passwordError.setVisible(false);
        if (loginError != null) loginError.setVisible(false);
    }

    private void handleSuccessfulLogin(user loggedInUser) {
        try {
            // Create session for the user
            SessionManager sessionManager = SessionManager.getInstance();
            sessionManager.createSession(loggedInUser);

            // Load the main layout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
            Parent root = loader.load();

            // Get the controller and configure it for the user's role
            MainLayoutController mainController = loader.getController();
            mainController.configureForUserRole(loggedInUser.getRole());
            mainController.setCurrentUser(loggedInUser);

            // Switch to main layout scene
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 800);
            stage.setScene(scene);
            stage.setTitle("AgriSense 360 - Dashboard");
            stage.centerOnScreen();
            stage.show();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showError(loginError, "❌ Impossible de créer la session");
        }
    }

    @FXML
    private void goToSignup(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/signup.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Créer un compte - AgriSense 360");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            showError(loginError, "❌ Impossible d'ouvrir la page d'inscription");
        }
    }

    @FXML
    private void handleForgotPassword() {
        showMessage(Alert.AlertType.INFORMATION, "Info",
                "Fonctionnalité de récupération de mot de passe - À venir!");
    }

    private void showMessage(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}