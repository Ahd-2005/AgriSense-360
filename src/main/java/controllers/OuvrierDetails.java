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

public class OuvrierDetails {

    @FXML private Label idLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;
    @FXML private Label tasksLabel;
    @FXML private Button deleteBtn;

    private user currentOuvrier;
    private user loggedInUser;
    private OuvrierManagement ouvrierManagement;

    @FXML
    public void initialize() {
        // Get logged in user from session
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            this.loggedInUser = sessionManager.getCurrentUser();
        }
    }

    public void setOuvrier(user ouvrier) {
        this.currentOuvrier = ouvrier;
        displayInfo();
        configurePermissions();
    }

    public void setOuvrierManagement(OuvrierManagement management) {
        this.ouvrierManagement = management;
    }

    private void configurePermissions() {
        // Prevent deleting yourself
        if (loggedInUser != null && currentOuvrier != null
                && loggedInUser.getId() == currentOuvrier.getId() && deleteBtn != null) {
            deleteBtn.setDisable(true);
        }
    }

    private void displayInfo() {
        idLabel.setText(String.valueOf(currentOuvrier.getId()));
        nameLabel.setText(currentOuvrier.getName());
        emailLabel.setText(currentOuvrier.getEmail());
        phoneLabel.setText(currentOuvrier.getPhone());
        roleLabel.setText(getRoleFriendlyName(currentOuvrier.getRole()));
        tasksLabel.setText("Aucune tâche"); // TODO: implement tasks

        String status = currentOuvrier.getStatus();
        statusLabel.setText(status);

        // Color-code status badge
        if ("ACTIVE".equals(status)) {
            statusLabel.setStyle("-fx-padding: 8 20; -fx-background-radius: 15px; " +
                    "-fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-background-color: rgba(90, 152, 20, 0.15); -fx-text-fill: #5a9814;");
        } else if ("BLOCKED".equals(status)) {
            statusLabel.setStyle("-fx-padding: 8 20; -fx-background-radius: 15px; " +
                    "-fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-background-color: rgba(231, 76, 60, 0.15); -fx-text-fill: #e74c3c;");
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
        loadInMainLayout("/fxml/OuvrierEdit.fxml", controller -> {
            OuvrierEdit editController = (OuvrierEdit) controller;
            editController.setOuvrier(currentOuvrier);
            editController.setOuvrierManagement(ouvrierManagement);
        });
    }

    @FXML
    private void handleDelete() {
        // Prevent deleting yourself
        if (loggedInUser != null && currentOuvrier.getId() == loggedInUser.getId()) {
            showAlert(Alert.AlertType.WARNING, "Impossible",
                    "Vous ne pouvez pas supprimer votre propre compte!");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmer");
        confirmAlert.setHeaderText("Supprimer Ouvrier");
        confirmAlert.setContentText("Voulez-vous vraiment supprimer " + currentOuvrier.getName() + "?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userservice service = new userservice();
                    service.deleteUser(currentOuvrier.getId());

                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Ouvrier supprimé avec succès!");

                    if (ouvrierManagement != null) {
                        ouvrierManagement.refreshTable();
                    }

                    handleBack();

                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de suppression: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBack() {
        loadInMainLayout("/fxml/OuvrierManagement.fxml", controller -> {
            if (controller instanceof OuvrierManagement) {
                ((OuvrierManagement) controller).refreshTable();
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de chargement de la page: " + e.getMessage());
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