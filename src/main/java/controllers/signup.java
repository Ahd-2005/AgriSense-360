package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import entity.user;
import services.userservice;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class signup {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField phoneField;

    // Error labels
    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label phoneError;

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
        hideAllErrors();
    }

    @FXML
    public void handleSignup() {
        hideAllErrors();

        boolean isValid = true;
        if (!validateName())            isValid = false;
        if (!validateEmail())           isValid = false;
        if (!validatePassword())        isValid = false;
        if (!validateConfirmPassword()) isValid = false;
        if (!validatePhone())           isValid = false;

        if (!isValid) return;

        try {
            userservice service = new userservice();

            // Check email duplicate
            user existingUser = service.findByEmail(emailField.getText().trim());
            if (existingUser != null) {
                showError(emailError, "❌ Cet email existe déjà!");
                return;
            }

            // ✅ Create new user as ROLE_PENDING — no role choice for new signups
            user newUser = new user();
            newUser.setName(nameField.getText().trim());
            newUser.setEmail(emailField.getText().trim().toLowerCase());
            newUser.setPassword(passwordField.getText().trim()); // plain — will be hashed in service
            newUser.setPhone(phoneField.getText().trim());
            newUser.setRole(user.Role.ROLE_PENDING);  // always pending at signup

            service.ajouter(newUser);

            // Fetch the created user with its DB-generated ID
            user createdUser = service.findByEmail(newUser.getEmail());

            if (createdUser == null) {
                showAlert("Erreur", "❌ Impossible de récupérer le compte créé.");
                return;
            }

            // ✅ Go to profile picture setup first, then farm list
            goToProfileSetup(createdUser);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "❌ Échec de création du compte: " + e.getMessage());
        }
    }

    // ============= NAVIGATION =============

    private void goToProfileSetup(user createdUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProfileSetup.fxml"));
            Parent root = loader.load();

            ProfileSetupController controller = loader.getController();
            controller.initData(createdUser);

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Photo de profil - AgriSense 360");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            // If ProfileSetup doesn't exist yet, go straight to farm list
            goToFarmList(createdUser);
        }
    }

    // ✅ Called from ProfileSetupController after picture is set (or skipped)
    public static void navigateToFarmList(user createdUser, Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(signup.class.getResource("/fxml/FarmList.fxml"));
            Parent root = loader.load();

            FarmListController controller = loader.getController();
            controller.initData(createdUser);

            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Choisir une ferme - AgriSense 360");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goToFarmList(user createdUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FarmList.fxml"));
            Parent root = loader.load();

            FarmListController controller = loader.getController();
            controller.initData(createdUser);

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Choisir une ferme - AgriSense 360");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "❌ Impossible d'ouvrir la liste des fermes: " + e.getMessage());
        }
    }

    @FXML
    public void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Login - AgriSense 360");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============= VALIDATION =============

    private boolean validateName() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) { showError(nameError, "❌ Le nom est obligatoire"); return false; }
        if (!NAME_PATTERN.matcher(name).matches()) { showError(nameError, "❌ Nom invalide (lettres uniquement, 2-50 caractères)"); return false; }
        return true;
    }

    private boolean validateEmail() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) { showError(emailError, "❌ L'email est obligatoire"); return false; }
        if (!EMAIL_PATTERN.matcher(email).matches()) { showError(emailError, "❌ Format d'email invalide (ex: user@example.com)"); return false; }
        return true;
    }

    private boolean validatePassword() {
        String password = passwordField.getText().trim();
        if (password.isEmpty()) { showError(passwordError, "❌ Le mot de passe est obligatoire"); return false; }
        if (password.length() < 8) { showError(passwordError, "❌ Minimum 8 caractères"); return false; }
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit     = password.matches(".*[0-9].*");
        boolean hasSpecial   = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
        if (!hasUpperCase || !hasLowerCase || !hasDigit || !hasSpecial) {
            showError(passwordError, "❌ Mot de passe faible (majuscule, minuscule, chiffre, caractère spécial)");
            return false;
        }
        return true;
    }

    private boolean validateConfirmPassword() {
        String password        = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();
        if (confirmPassword.isEmpty()) { showError(confirmPasswordError, "❌ Veuillez confirmer le mot de passe"); return false; }
        if (!password.equals(confirmPassword)) { showError(confirmPasswordError, "❌ Les mots de passe ne correspondent pas"); return false; }
        return true;
    }

    private boolean validatePhone() {
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) { showError(phoneError, "❌ Le numéro de téléphone est obligatoire"); return false; }
        if (!PHONE_PATTERN.matcher(phone).matches()) { showError(phoneError, "❌ Numéro invalide (8 chiffres requis)"); return false; }
        return true;
    }

    // ============= HELPERS =============

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
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}