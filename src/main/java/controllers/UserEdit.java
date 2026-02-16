package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import entity.user;
import entity.user.Role;
import services.userservice;
import services.SessionManager;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class UserEdit {

    @FXML private Label idLabel;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<Role> roleCombo;

    // Error Labels
    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label roleError;

    private user currentUser;
    private user loggedInUser;
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
        roleCombo.setItems(FXCollections.observableArrayList(Role.values()));

        // Get logged in user from session
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            this.loggedInUser = sessionManager.getCurrentUser();
        }

        hideAllErrors();
    }

    public void setUser(user user) {
        this.currentUser = user;
        fillFields();
        configurePermissions();
    }

    public void setAdminDashboard(AdminDashboard dashboard) {
        this.adminDashboard = dashboard;
    }

    private void configurePermissions() {
        // Prevent editing yourself from changing your own role
        if (loggedInUser != null && currentUser != null
                && loggedInUser.getId() == currentUser.getId()) {
            roleCombo.setDisable(true);
        }
    }

    private void fillFields() {
        idLabel.setText(String.valueOf(currentUser.getId()));
        nameField.setText(currentUser.getName());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhone());
        roleCombo.setValue(currentUser.getRole());
    }

    @FXML
    private void handleSave() {
        hideAllErrors();
        boolean isValid = true;

        if (!validateName()) isValid = false;
        if (!validateEmail()) isValid = false;
        if (!validatePhone()) isValid = false;
        if (!validateRole()) isValid = false;

        // Validate password only if changing
        if (!passwordField.getText().trim().isEmpty()) {
            if (!validatePassword()) isValid = false;
            if (!validateConfirmPassword()) isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Check if email changed and exists
        if (!emailField.getText().trim().equals(currentUser.getEmail())) {
            try {
                userservice service = new userservice();
                user existingUser = service.findByEmail(emailField.getText().trim());

                if (existingUser != null) {
                    showError(emailError, "❌ This email already exists!");
                    return;
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Verification error: " + e.getMessage());
                return;
            }
        }

        try {
            currentUser.setName(nameField.getText().trim());
            currentUser.setEmail(emailField.getText().trim().toLowerCase());
            currentUser.setPhone(phoneField.getText().trim());
            currentUser.setRole(roleCombo.getValue());

            if (!passwordField.getText().trim().isEmpty()) {
                currentUser.setPassword(passwordField.getText().trim());
            }

            userservice service = new userservice();
            service.updateUser(currentUser);

            showAlert(Alert.AlertType.INFORMATION, "Success", "✅ User updated successfully!");

            if (adminDashboard != null) {
                adminDashboard.refreshTable();
            }

            goBackToDashboard();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "❌ Failed to update user: " + e.getMessage());
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