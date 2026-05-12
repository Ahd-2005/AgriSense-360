package controllers;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import entity.user;
import services.GoogleAuthService;
import services.userservice;
import services.SessionManager;
import services.FaceRecognitionService;
import javafx.stage.FileChooser;
import java.io.File;

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

        if (!validateEmail()) {
            isValid = false;
        }

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
                if ("blocked".equals(loggedInUser.getStatus())) {
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
            SessionManager sessionManager = SessionManager.getInstance();
            sessionManager.createSession(loggedInUser);

            String fxmlPath;
            String title;
            Object controller = null;

            if ("pending".equals(loggedInUser.getStatus()) && loggedInUser.getFarmId() != null) {
                // Application submitted, waiting for owner
                fxmlPath = "/fxml/PendingWaiting.fxml";
                title = "En attente d'approbation - AgriSense 360";
            } else if (loggedInUser.getRole() == user.Role.ROLE_PENDING) {
                // No application submitted yet
                fxmlPath = "/fxml/FarmList.fxml";
                title = "Choisir une ferme - AgriSense 360";
            } else {
                // Fully active user (Owner or Approved Gerant/Ouvrier)
                fxmlPath = "/fxml/MainLayout.fxml";
                title = "AgriSense 360 - Dashboard";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Init data for non-main layouts
            if (fxmlPath.contains("PendingWaiting")) {
                PendingWaitingController pwc = loader.getController();
                pwc.initData(loggedInUser);
            } else if (fxmlPath.contains("FarmList")) {
                FarmListController flc = loader.getController();
                flc.initData(loggedInUser);
            } else if (fxmlPath.contains("MainLayout")) {
                MainLayoutController mainController = loader.getController();
                mainController.configureForUserRole(loggedInUser.getRole());
                mainController.setCurrentUser(loggedInUser);
            }

            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 800);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showError(loginError, "❌ Impossible de créer la session: " + e.getMessage());
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

    // ✅ MODIFIÉ : navigue vers ForgotPassword.fxml au lieu d'afficher une alerte
    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ForgotPassword.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Mot de passe oublié - AgriSense 360");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            showError(loginError, "❌ Impossible d'ouvrir la page de réinitialisation");
        }
    }

    @FXML
    private void handleGoogleLogin() {
        new Thread(() -> {
            try {
                GoogleAuthService googleAuth = new GoogleAuthService();
                boolean success = googleAuth.authenticate();

                if (success) {
                    String email = googleAuth.getUserEmail();
                    String name  = googleAuth.getUserName();

                    userservice service = new userservice();
                    user existingUser = service.findByEmail(email);

                    if (existingUser != null) {
                        if ("BLOCKED".equals(existingUser.getStatus())) {
                            javafx.application.Platform.runLater(() ->
                                    showError(loginError, "❌ Compte bloqué.")
                            );
                            return;
                        }
                        javafx.application.Platform.runLater(() -> {
                            try { handleSuccessfulLogin(existingUser); }
                            catch (Exception e) { showError(loginError, "❌ Erreur connexion"); }
                        });

                    } else {
                        javafx.application.Platform.runLater(() -> {
                            try {
                                FXMLLoader loader = new FXMLLoader(
                                        getClass().getResource("/fxml/RoleChoice.fxml")
                                );
                                Parent root = loader.load();

                                RoleChoiceController controller = loader.getController();
                                controller.initData(email, name);

                                Stage stage = (Stage) emailField.getScene().getWindow();
                                stage.setScene(new Scene(root, 1400, 800));
                                stage.setTitle("Choisir votre rôle - AgriSense 360");
                                stage.centerOnScreen();

                            } catch (Exception e) {
                                e.printStackTrace();
                                showError(loginError, "❌ Erreur: " + e.getMessage());
                            }
                        });
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        showError(loginError, "❌ Erreur Google Auth: " + e.getMessage())
                );
            }
        }).start();
    }

    private void showMessage(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ============= FACE LOGIN =============

    @FXML
    private void handleFaceLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FaceLogin.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Connexion Faciale - AgriSense 360");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showError(loginError, "❌ Impossible d'ouvrir la reconnaissance faciale");
        }
    }

}