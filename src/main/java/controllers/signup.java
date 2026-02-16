package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import entity.user;
import entity.user.Role;
import services.userservice;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class signup {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField phoneField;
    @FXML private ChoiceBox<Role> roleChoice;

    // Error labels
    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label phoneError;
    @FXML private Label roleError;

    // Regex patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[0-9]{8}$" // 8 digits for Tunisia
    );

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[A-Za-zÀ-ÿ\\s]{2,50}$" // Letters and spaces, 2-50 chars
    );

    @FXML
    public void initialize() {
        // Only allow GERANT and OUVRIER (NOT ADMIN)
        roleChoice.getItems().addAll(Role.ROLE_GERANT, Role.ROLE_OUVRIER);
        roleChoice.setValue(Role.ROLE_OUVRIER); // default

        // Hide all error labels initially
        hideAllErrors();
    }

    @FXML
    public void handleSignup() {
        // Reset all error messages
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

        // 3. Validate Password
        if (!validatePassword()) {
            isValid = false;
        }

        // 4. Validate Confirm Password
        if (!validateConfirmPassword()) {
            isValid = false;
        }

        // 5. Validate Phone
        if (!validatePhone()) {
            isValid = false;
        }

        // 6. Validate Role
        if (!validateRole()) {
            isValid = false;
        }

        // If any validation fails, stop here
        if (!isValid) {
            return;
        }

        // Check if email already exists
        try {
            userservice service = new userservice();
            user existingUser = service.findByEmail(emailField.getText().trim());

            if (existingUser != null) {
                showError(emailError, "❌ Cet email existe déjà!");
                return;
            }

            // Create new user
            user newUser = new user();
            newUser.setName(nameField.getText().trim());
            newUser.setEmail(emailField.getText().trim().toLowerCase());
            newUser.setPassword(passwordField.getText().trim());
            newUser.setPhone(phoneField.getText().trim());
            newUser.setRole(roleChoice.getValue());

            service.ajouter(newUser);

            showAlert("Succès", "✅ Compte créé avec succès! Veuillez vous connecter.");
            goToLogin();

        } catch (SQLException e) {
            showAlert("Erreur", "❌ Échec de création du compte: " + e.getMessage());
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

    private boolean validatePassword() {
        String password = passwordField.getText().trim();

        if (password.isEmpty()) {
            showError(passwordError, "❌ Le mot de passe est obligatoire");
            return false;
        }

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
            showError(passwordError, "❌ Mot de passe faible (min 8 car, majuscule, minuscule, chiffre, caractère spécial)");
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

    private boolean validateRole() {
        if (roleChoice.getValue() == null) {
            showError(roleError, "❌ Veuillez sélectionner un rôle");
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
        nameError.setVisible(false);
        emailError.setVisible(false);
        passwordError.setVisible(false);
        confirmPasswordError.setVisible(false);
        phoneError.setVisible(false);
        roleError.setVisible(false);
    }

    @FXML
    public void goBack() {
        goToLogin();
    }

    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Login");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Échec du chargement de la page de connexion!");
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