package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import entity.user;
import entity.user.Role;
import services.userservice;
import services.SessionManager;

import java.sql.SQLException;
import java.util.List;

public class AdminDashboard {

    @FXML private VBox userCardsContainer;

    private user currentUser;

    @FXML
    public void initialize() {
        // Get current user from session
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            this.currentUser = sessionManager.getCurrentUser();

            // Check if user has permission to access admin dashboard
            if (currentUser.getRole() != Role.ROLE_ADMIN) {
                showAlert(Alert.AlertType.ERROR, "Access Denied",
                        "You don't have permission to access this page.");
                return;
            }
        }

        loadUsers();
    }

    private void loadUsers() {
        try {
            userservice service = new userservice();
            List<user> usersFromDB = service.getAllUsers();

            userCardsContainer.getChildren().clear();

            if (usersFromDB.isEmpty()) {
                Label emptyLabel = new Label("No users found");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray; -fx-padding: 50;");
                userCardsContainer.getChildren().add(emptyLabel);
            } else {
                for (user u : usersFromDB) {
                    userCardsContainer.getChildren().add(createUserCard(u));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load users: " + e.getMessage());
        }
    }

    private VBox createUserCard(user u) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: #fff; -fx-padding: 25; " +
                "-fx-background-radius: 16px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 12, 0, 0, 4); " +
                "-fx-border-color: rgba(43, 59, 31, 0.08); -fx-border-width: 1; " +
                "-fx-border-radius: 16px;");

        // Header Row: Name and Status Badge
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(u.getName());
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #22301b;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label statusBadge = new Label(u.getStatus());
        statusBadge.setStyle(
                "-fx-padding: 6 18; -fx-background-radius: 15px; -fx-font-size: 12px; -fx-font-weight: bold; " +
                        ("ACTIVE".equals(u.getStatus())
                                ? "-fx-background-color: rgba(90, 152, 20, 0.15); -fx-text-fill: #5a9814;"
                                : "-fx-background-color: rgba(231, 76, 60, 0.15); -fx-text-fill: #e74c3c;")
        );

        header.getChildren().addAll(nameLabel, statusBadge);

        // Info Grid
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(20);
        infoGrid.setVgap(12);
        infoGrid.setPadding(new Insets(10, 0, 10, 0));

        addInfoRow(infoGrid, 0, "📧 Email:", u.getEmail());
        addInfoRow(infoGrid, 1, "📱 Phone:", u.getPhone());
        addInfoRow(infoGrid, 2, "👤 Role:", getRoleFriendlyName(u.getRole()));
        addInfoRow(infoGrid, 3, "🆔 ID:", String.valueOf(u.getId()));

        // Action Buttons
        HBox actionButtons = new HBox(12);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);
        actionButtons.setPadding(new Insets(10, 0, 0, 0));

        Button viewBtn = createActionButton("👁 View", "#3498db");
        viewBtn.setOnAction(e -> openUserDetails(u));

        Button editBtn = createActionButton("✏ Edit", "#f39c12");
        editBtn.setOnAction(e -> openUserEdit(u));

        Button deleteBtn = createActionButton("🗑 Delete", "#e74c3c");
        deleteBtn.setOnAction(e -> handleDeleteUser(u));

        // Prevent deleting yourself
        if (currentUser != null && u.getId() == currentUser.getId()) {
            deleteBtn.setDisable(true);
            deleteBtn.setStyle(deleteBtn.getStyle() + "; -fx-opacity: 0.5;");
        }

        actionButtons.getChildren().addAll(viewBtn, editBtn, deleteBtn);

        card.getChildren().addAll(header, new Separator(), infoGrid, actionButtons);

        return card;
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

    private void addInfoRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold; -fx-text-fill: rgba(43, 59, 31, 0.7); -fx-font-size: 13px;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #22301b; -fx-font-size: 14px;");

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; " +
                "-fx-background-radius: 8px; -fx-font-weight: bold;");
        return btn;
    }

    private void openUserDetails(user selectedUser) {
        loadInMainLayout("/fxml/UserDetails.fxml", controller -> {
            UserDetails detailsController = (UserDetails) controller;
            detailsController.setUser(selectedUser);
            detailsController.setAdminDashboard(this);
        });
    }

    private void openUserEdit(user selectedUser) {
        loadInMainLayout("/fxml/UserEdit.fxml", controller -> {
            UserEdit editController = (UserEdit) controller;
            editController.setUser(selectedUser);
            editController.setAdminDashboard(this);
        });
    }

    private void handleDeleteUser(user selectedUser) {
        // Prevent deleting yourself
        if (currentUser != null && selectedUser.getId() == currentUser.getId()) {
            showAlert(Alert.AlertType.WARNING, "Cannot Delete",
                    "You cannot delete your own account!");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete User");
        confirmAlert.setContentText("Are you sure you want to delete " + selectedUser.getName() + "?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userservice service = new userservice();
                    service.deleteUser(selectedUser.getId());

                    showAlert(Alert.AlertType.INFORMATION, "Success", "User deleted successfully!");
                    refreshTable();

                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete user: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleAddUser() {
        loadInMainLayout("/fxml/UserAdd.fxml", controller -> {
            UserAdd addController = (UserAdd) controller;
            addController.setAdminDashboard(this);
        });
    }

    @FXML
    private void handleRefresh() {
        refreshTable();
    }

    public void refreshTable() {
        loadUsers();
    }

    private void loadInMainLayout(String fxmlPath, ControllerCallback callback) {
        try {
            // Find the MainLayout's content area and load the page there
            Parent root = userCardsContainer.getScene().getRoot();
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