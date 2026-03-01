package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import entity.user;
import entity.user.Role;
import services.userservice;
import services.SessionManager;

import java.sql.SQLException;

public class UserDetails {

    @FXML private Label idLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;
    @FXML private Button activateBtn;
    @FXML private Button blockBtn;
    @FXML private Button deleteBtn;

    private user currentUser;
    private user loggedInUser; // The user who is logged in
    private AdminDashboard adminDashboard;

    @FXML
    public void initialize() {
        // Get logged in user from session
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            this.loggedInUser = sessionManager.getCurrentUser();
        }
    }

    public void setUser(user user) {
        this.currentUser = user;
        displayUserInfo();
        configurePermissions();
    }

    public void setAdminDashboard(AdminDashboard dashboard) {
        this.adminDashboard = dashboard;
    }

    private void configurePermissions() {
        // Prevent deleting or blocking yourself
        if (loggedInUser != null && currentUser != null
                && loggedInUser.getId() == currentUser.getId()) {
            if (deleteBtn != null) deleteBtn.setDisable(true);
            if (blockBtn != null) blockBtn.setDisable(true);
        }
    }

    private void displayUserInfo() {
        idLabel.setText(String.valueOf(currentUser.getId()));
        nameLabel.setText(currentUser.getName());
        emailLabel.setText(currentUser.getEmail());
        phoneLabel.setText(currentUser.getPhone());
        roleLabel.setText(getRoleFriendlyName(currentUser.getRole()));

        String status = currentUser.getStatus();
        statusLabel.setText(status);

        // Color-code status badge
        if ("ACTIVE".equals(status)) {
            statusLabel.setStyle("-fx-padding: 8 20; -fx-background-radius: 15px; " +
                    "-fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-background-color: rgba(90, 152, 20, 0.15); -fx-text-fill: #5a9814;");
            activateBtn.setDisable(true);
            blockBtn.setDisable(false);
        } else if ("BLOCKED".equals(status)) {
            statusLabel.setStyle("-fx-padding: 8 20; -fx-background-radius: 15px; " +
                    "-fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-background-color: rgba(231, 76, 60, 0.15); -fx-text-fill: #e74c3c;");
            activateBtn.setDisable(false);
            blockBtn.setDisable(true);
        }

        // Prevent blocking yourself
        if (loggedInUser != null && currentUser.getId() == loggedInUser.getId()) {
            blockBtn.setDisable(true);
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
    private void handleActivate() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Activate");
        confirmAlert.setHeaderText("Activate User");
        confirmAlert.setContentText("Are you sure you want to activate " + currentUser.getName() + "?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userservice service = new userservice();
                    service.toggleUserStatus(currentUser.getId(), "ACTIVE");

                    currentUser.setStatus("ACTIVE");
                    displayUserInfo();

                    showAlert(Alert.AlertType.INFORMATION, "Success", "User activated successfully!");

                    if (adminDashboard != null) {
                        adminDashboard.refreshTable();
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to activate user: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBlock() {
        // Prevent blocking yourself
        if (loggedInUser != null && currentUser.getId() == loggedInUser.getId()) {
            showAlert(Alert.AlertType.WARNING, "Cannot Block",
                    "You cannot block your own account!");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Block");
        confirmAlert.setHeaderText("Block User");
        confirmAlert.setContentText("Are you sure you want to block " + currentUser.getName() + "?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userservice service = new userservice();
                    service.toggleUserStatus(currentUser.getId(), "BLOCKED");

                    currentUser.setStatus("BLOCKED");
                    displayUserInfo();

                    showAlert(Alert.AlertType.INFORMATION, "Success", "User blocked successfully!");

                    if (adminDashboard != null) {
                        adminDashboard.refreshTable();
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to block user: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleModifier() {
        loadInMainLayout("/fxml/UserEdit.fxml", controller -> {
            UserEdit editController = (UserEdit) controller;
            editController.setUser(currentUser);
            editController.setAdminDashboard(adminDashboard);
        });
    }

    @FXML
    private void handleDelete() {
        // Prevent deleting yourself
        if (loggedInUser != null && currentUser.getId() == loggedInUser.getId()) {
            showAlert(Alert.AlertType.WARNING, "Cannot Delete",
                    "You cannot delete your own account!");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete User");
        confirmAlert.setContentText("Are you sure you want to delete user: " + currentUser.getName() + "?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteUser();
            }
        });
    }

    private void deleteUser() {
        try {
            userservice service = new userservice();
            service.deleteUser(currentUser.getId());

            showAlert(Alert.AlertType.INFORMATION, "Success", "User deleted successfully!");

            if (adminDashboard != null) {
                adminDashboard.refreshTable();
            }

            handleBack();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete user: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        loadInMainLayout("/fxml/AdminDashboard.fxml", controller -> {
            if (controller instanceof AdminDashboard) {
                ((AdminDashboard) controller).refreshTable();
            }
        });
    }

    private void loadInMainLayout(String fxmlPath, ControllerCallback callback) {
        try {
            Parent root = idLabel.getScene().getRoot();
            if (root instanceof BorderPane) {
                BorderPane mainLayout = (BorderPane) root;
                StackPane contentArea = (StackPane) mainLayout.getCenter();

                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent content = loader.load();

                if (callback != null) {
                    callback.onControllerLoaded(loader.getController());
                }

                contentArea.getChildren().clear();
                contentArea.getChildren().add(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load page: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ControllerCallback {
        void onControllerLoaded(Object controller);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}