package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import entity.user;
import entity.user.Role;
import services.userservice;
import services.SessionManager;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class GerantProfile {

    // View Mode Elements
    @FXML private VBox viewModeContainer;
    @FXML private Label avatarLabel;
    @FXML private Label displayNameLabel;
    @FXML private Label emailDisplayLabel;
    @FXML private Label idLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label roleLabel;
    @FXML private Label roleDetailLabel;
    @FXML private Label statusLabel;

    // Edit Mode Elements
    @FXML private VBox editModeContainer;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    // Error Labels
    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;

    private user currentUser;

    // Regex patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[0-9]{8}$"
    );

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[A-Za-zÀ-ÿ\\s]{2,50}$"
    );

    @FXML
    public void initialize() {
        // Get current user from session
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            this.currentUser = sessionManager.getCurrentUser();
            displayUserInfo();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Session expirée. Veuillez vous reconnecter.");
        }

        // Hide error labels initially
        hideAllErrors();
    }

    private void displayUserInfo() {
        if (currentUser != null) {
            // Header info
            displayNameLabel.setText(currentUser.getName());
            emailDisplayLabel.setText(currentUser.getEmail());

            // Role badge
            String roleName = getRoleFriendlyName(currentUser.getRole());
            roleLabel.setText(roleName);

            // Status badge
            String status = currentUser.getStatus();
            statusLabel.setText(status);
            if ("ACTIVE".equals(status)) {
                statusLabel.setStyle("-fx-padding: 6 18; -fx-background-radius: 15px; " +
                        "-fx-font-size: 13px; -fx-font-weight: bold; " +
                        "-fx-background-color: rgba(90, 152, 20, 0.15); -fx-text-fill: #5a9814;");
            } else {
                statusLabel.setStyle("-fx-padding: 6 18; -fx-background-radius: 15px; " +
                        "-fx-font-size: 13px; -fx-font-weight: bold; " +
                        "-fx-background-color: rgba(231, 76, 60, 0.15); -fx-text-fill: #e74c3c;");
            }

            // Detail fields
            idLabel.setText(String.valueOf(currentUser.getId()));
            nameLabel.setText(currentUser.getName());
            emailLabel.setText(currentUser.getEmail());
            phoneLabel.setText(currentUser.getPhone());
            roleDetailLabel.setText(roleName);
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
    private void handleEdit() {
        // Switch to edit mode
        viewModeContainer.setVisible(false);
        viewModeContainer.setManaged(false);
        editModeContainer.setVisible(true);
        editModeContainer.setManaged(true);

        // Fill form with current data
        fillEditForm();
        hideAllErrors();
    }

    private void fillEditForm() {
        nameField.setText(currentUser.getName());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhone());
        passwordField.clear();
        confirmPasswordField.clear();
    }

    @FXML
    private void handleSave() {
        hideAllErrors();
        boolean isValid = true;

        // 1. Validate Name
        if (!validateName()) {
            isValid = false;
        }

        // 2. Validate Email
        if (!validateEmail()) {
            isValid = false;
        }

        // 3. Validate Phone
        if (!validatePhone()) {
            isValid = false;
        }

        // 4. Validate Password (only if changing)
        if (!passwordField.getText().trim().isEmpty()) {
            if (!validatePassword()) {
                isValid = false;
            }
            if (!validateConfirmPassword()) {
                isValid = false;
            }
        }

        if (!isValid) {
            return;
        }

        // Check if email changed and if new email exists
        if (!emailField.getText().trim().equals(currentUser.getEmail())) {
            try {
                userservice service = new userservice();
                user existingUser = service.findByEmail(emailField.getText().trim());

                if (existingUser != null) {
                    showError(emailError, "❌ Cet email existe déjà!");
                    return;
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de vérification: " + e.getMessage());
                return;
            }
        }

        try {
            // Update user object
            currentUser.setName(nameField.getText().trim());
            currentUser.setEmail(emailField.getText().trim().toLowerCase());
            currentUser.setPhone(phoneField.getText().trim());

            // Update password only if provided
            if (!passwordField.getText().trim().isEmpty()) {
                currentUser.setPassword(passwordField.getText().trim());
            }

            // Save to database
            userservice service = new userservice();
            service.updateUser(currentUser);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Profil mis à jour avec succès!");

            // Switch back to view mode
            displayUserInfo();
            handleCancel();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec de mise à jour: " + e.getMessage());
        }
    }

    // ============= VALIDATION METHODS =============

    private boolean validateName() {
        String name = nameField.getText().trim();

        if (name.isEmpty()) {
            showError(nameError, "❌ Le nom est obligatoire");
            return false;
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            showError(nameError, "❌ Nom invalide (lettres uniquement, 2-50 caractères)");
            return false;
        }

        return true;
    }

    private boolean validateEmail() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError(emailError, "❌ L'email est obligatoire");
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError(emailError, "❌ Format d'email invalide (ex: user@example.com)");
            return false;
        }

        return true;
    }

    private boolean validatePhone() {
        String phone = phoneField.getText().trim();

        if (phone.isEmpty()) {
            showError(phoneError, "❌ Le numéro de téléphone est obligatoire");
            return false;
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            showError(phoneError, "❌ Numéro invalide (8 chiffres requis)");
            return false;
        }

        return true;
    }

    private boolean validatePassword() {
        String password = passwordField.getText().trim();

        if (password.length() < 8) {
            showError(passwordError, "❌ Le mot de passe doit contenir au moins 8 caractères");
            return false;
        }

        // Strong password check
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        if (!hasUpperCase || !hasLowerCase || !hasDigit || !hasSpecial) {
            showError(passwordError, "❌ Mot de passe faible (majuscule, minuscule, chiffre, spécial)");
            return false;
        }

        return true;
    }

    private boolean validateConfirmPassword() {
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (confirmPassword.isEmpty()) {
            showError(confirmPasswordError, "❌ Veuillez confirmer le mot de passe");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            showError(confirmPasswordError, "❌ Les mots de passe ne correspondent pas");
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
        if (nameError != null) nameError.setVisible(false);
        if (emailError != null) emailError.setVisible(false);
        if (phoneError != null) phoneError.setVisible(false);
        if (passwordError != null) passwordError.setVisible(false);
        if (confirmPasswordError != null) confirmPasswordError.setVisible(false);
    }

    @FXML
    private void handleCancel() {
        // Switch back to view mode without saving
        editModeContainer.setVisible(false);
        editModeContainer.setManaged(false);
        viewModeContainer.setVisible(true);
        viewModeContainer.setManaged(true);

        // Clear password fields
        passwordField.clear();
        confirmPasswordField.clear();
        hideAllErrors();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}