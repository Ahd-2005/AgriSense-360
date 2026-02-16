package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import entity.user;
import entity.user.Role;
import services.userservice;
import services.SessionManager;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class OuvrierEdit {

    @FXML private Label idLabel;
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

    private user currentOuvrier;
    private user loggedInUser;
    private OuvrierManagement ouvrierManagement;

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
        // Get logged in user from session
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            this.loggedInUser = sessionManager.getCurrentUser();
        }
        hideAllErrors();
    }

    public void setOuvrier(user ouvrier) {
        this.currentOuvrier = ouvrier;
        fillFields();
    }

    public void setOuvrierManagement(OuvrierManagement management) {
        this.ouvrierManagement = management;
    }

    private void fillFields() {
        idLabel.setText(String.valueOf(currentOuvrier.getId()));
        nameField.setText(currentOuvrier.getName());
        emailField.setText(currentOuvrier.getEmail());
        phoneField.setText(currentOuvrier.getPhone());
    }

    @FXML
    private void handleSave() {
        hideAllErrors();
        boolean isValid = true;

        // Validate fields
        if (!validateName()) isValid = false;
        if (!validateEmail()) isValid = false;
        if (!validatePhone()) isValid = false;

        // Validate password only if changing
        if (!passwordField.getText().trim().isEmpty()) {
            if (!validatePassword()) isValid = false;
            if (!validateConfirmPassword()) isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Check if email changed and exists
        if (!emailField.getText().trim().equals(currentOuvrier.getEmail())) {
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
            // Update ouvrier object
            currentOuvrier.setName(nameField.getText().trim());
            currentOuvrier.setEmail(emailField.getText().trim().toLowerCase());
            currentOuvrier.setPhone(phoneField.getText().trim());

            // Update password only if provided
            if (!passwordField.getText().trim().isEmpty()) {
                currentOuvrier.setPassword(passwordField.getText().trim());
            }

            // Save to database
            userservice service = new userservice();
            service.updateUser(currentOuvrier);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Ouvrier mis à jour avec succès!");

            if (ouvrierManagement != null) {
                ouvrierManagement.refreshTable();
            }

            goBack();

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
        goBack();
    }

    private void goBack() {
        try {
            Parent root = nameField.getScene().getRoot();
            if (root instanceof BorderPane) {
                BorderPane mainLayout = (BorderPane) root;
                StackPane contentArea = (StackPane) mainLayout.getCenter();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OuvrierManagement.fxml"));
                Parent content = loader.load();

                OuvrierManagement controller = loader.getController();
                controller.refreshTable();

                contentArea.getChildren().clear();
                contentArea.getChildren().add(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec de retour: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}