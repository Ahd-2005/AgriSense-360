package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import entity.user;
import services.userservice;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class UserAdd {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<user.Role> roleCombo;

    // Error Labels
    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label roleError;

    private AdminDashboard adminDashboard;

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
        roleCombo.setItems(FXCollections.observableArrayList(user.Role.values()));
        hideAllErrors();
    }

    public void setAdminDashboard(AdminDashboard dashboard) {
        this.adminDashboard = dashboard;
    }

    @FXML
    private void handleAddUser() {
        hideAllErrors();
        boolean isValid = true;

        if (!validateName()) isValid = false;
        if (!validateEmail()) isValid = false;
        if (!validatePhone()) isValid = false;
        if (!validatePassword()) isValid = false;
        if (!validateConfirmPassword()) isValid = false;
        if (!validateRole()) isValid = false;

        if (!isValid) {
            return;
        }

        try {
            userservice service = new userservice();

            // Check if email already exists
            user existingUser = service.findByEmail(emailField.getText().trim());
            if (existingUser != null) {
                showError(emailError, "❌ This email already exists!");
                return;
            }

            user newUser = new user();
            newUser.setName(nameField.getText().trim());
            newUser.setEmail(emailField.getText().trim().toLowerCase());
            newUser.setPhone(phoneField.getText().trim());
            newUser.setPassword(passwordField.getText().trim());
            newUser.setRole(roleCombo.getValue());
            newUser.setStatus("ACTIVE");

            service.ajouter(newUser);

            showAlert(Alert.AlertType.INFORMATION, "Success", "✅ User added successfully!");

            if (adminDashboard != null) {
                adminDashboard.refreshTable();
            }

            goBackToDashboard();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "❌ Failed to add user: " + e.getMessage());
        }
    }

    // ============= VALIDATION METHODS =============

    private boolean validateName() {
        String name = nameField.getText().trim();

        if (name.isEmpty()) {
            showError(nameError, "❌ Name is required");
            return false;
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            showError(nameError, "❌ Invalid name (letters only, 2-50 characters)");
            return false;
        }

        return true;
    }

    private boolean validateEmail() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError(emailError, "❌ Email is required");
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError(emailError, "❌ Invalid email format (e.g., user@example.com)");
            return false;
        }

        return true;
    }

    private boolean validatePhone() {
        String phone = phoneField.getText().trim();

        if (phone.isEmpty()) {
            showError(phoneError, "❌ Phone number is required");
            return false;
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            showError(phoneError, "❌ Invalid phone number (8 digits required)");
            return false;
        }

        return true;
    }

    private boolean validatePassword() {
        String password = passwordField.getText().trim();

        if (password.isEmpty()) {
            showError(passwordError, "❌ Password is required");
            return false;
        }

        if (password.length() < 8) {
            showError(passwordError, "❌ Password must be at least 8 characters");
            return false;
        }

        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        if (!hasUpperCase || !hasLowerCase || !hasDigit || !hasSpecial) {
            showError(passwordError, "❌ Weak password (uppercase, lowercase, digit, special char)");
            return false;
        }

        return true;
    }

    private boolean validateConfirmPassword() {
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (confirmPassword.isEmpty()) {
            showError(confirmPasswordError, "❌ Please confirm password");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            showError(confirmPasswordError, "❌ Passwords do not match");
            return false;
        }

        return true;
    }

    private boolean validateRole() {
        if (roleCombo.getValue() == null) {
            showError(roleError, "❌ Please select a role");
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
        if (roleError != null) roleError.setVisible(false);
    }

    @FXML
    private void handleCancel() {
        goBackToDashboard();
    }

    private void goBackToDashboard() {
        try {
            Parent root = nameField.getScene().getRoot();
            if (root instanceof BorderPane) {
                BorderPane mainLayout = (BorderPane) root;
                StackPane contentArea = (StackPane) mainLayout.getCenter();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboard.fxml"));
                Parent content = loader.load();

                AdminDashboard controller = loader.getController();
                controller.refreshTable();

                contentArea.getChildren().clear();
                contentArea.getChildren().add(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "❌ Failed to return to dashboard: " + e.getMessage());
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