package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import entity.user;
import services.userservice;

import java.sql.SQLException;
import java.util.List;

public class OuvrierManagement {

    @FXML private VBox cardContainer;

    @FXML
    public void initialize() {
        loadOuvriers();
    }

    private void loadOuvriers() {
        try {
            userservice service = new userservice();
            List<user> ouvriersFromDB = service.getAllOuvriers();

            cardContainer.getChildren().clear();

            if (ouvriersFromDB.isEmpty()) {
                Label emptyLabel = new Label("Aucun ouvrier trouvé");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray; -fx-padding: 50;");
                cardContainer.getChildren().add(emptyLabel);
            } else {
                for (user ouvrier : ouvriersFromDB) {
                    cardContainer.getChildren().add(createOuvrierCard(ouvrier));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec du chargement des ouvriers: " + e.getMessage());
        }
    }

    private VBox createOuvrierCard(user ouvrier) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: #fff; -fx-padding: 25; " +
                "-fx-background-radius: 16px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 12, 0, 0, 4); " +
                "-fx-border-color: rgba(43, 59, 31, 0.08); -fx-border-width: 1; " +
                "-fx-border-radius: 16px;");

        // Header Row: Name and Status Badge
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(ouvrier.getName());
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #22301b;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label statusBadge = new Label(ouvrier.getStatus());
        statusBadge.setStyle(
                "-fx-padding: 6 18; -fx-background-radius: 15px; -fx-font-size: 12px; -fx-font-weight: bold; " +
                        ("ACTIVE".equals(ouvrier.getStatus())
                                ? "-fx-background-color: rgba(90, 152, 20, 0.15); -fx-text-fill: #5a9814;"
                                : "-fx-background-color: rgba(231, 76, 60, 0.15); -fx-text-fill: #e74c3c;")
        );

        header.getChildren().addAll(nameLabel, statusBadge);

        // Info Grid
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(20);
        infoGrid.setVgap(12);
        infoGrid.setPadding(new Insets(10, 0, 10, 0));

        addInfoRow(infoGrid, 0, "📧 Email:", ouvrier.getEmail());
        addInfoRow(infoGrid, 1, "📱 Téléphone:", ouvrier.getPhone());
        addInfoRow(infoGrid, 2, "🆔 ID:", String.valueOf(ouvrier.getId()));
        addInfoRow(infoGrid, 3, "📋 Tâches:", "Aucune tâche");

        // Action Buttons
        HBox actionButtons = new HBox(12);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);
        actionButtons.setPadding(new Insets(10, 0, 0, 0));

        Button viewBtn = createActionButton("👁 Voir", "#3498db");
        viewBtn.setOnAction(e -> handleViewOuvrier(ouvrier));

        Button editBtn = createActionButton("✏ Modifier", "#f39c12");
        editBtn.setOnAction(e -> handleModifyOuvrier(ouvrier));

        Button deleteBtn = createActionButton("🗑 Supprimer", "#e74c3c");
        deleteBtn.setOnAction(e -> handleDeleteOuvrier(ouvrier));

        actionButtons.getChildren().addAll(viewBtn, editBtn, deleteBtn);

        card.getChildren().addAll(header, new Separator(), infoGrid, actionButtons);

        return card;
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

    private void handleViewOuvrier(user ouvrier) {
        loadInMainLayout("/fxml/OuvrierDetails.fxml", controller -> {
            OuvrierDetails detailsController = (OuvrierDetails) controller;
            detailsController.setOuvrier(ouvrier);
            detailsController.setOuvrierManagement(this);
        });
    }

    private void handleModifyOuvrier(user ouvrier) {
        loadInMainLayout("/fxml/OuvrierEdit.fxml", controller -> {
            OuvrierEdit editController = (OuvrierEdit) controller;
            editController.setOuvrier(ouvrier);
            editController.setOuvrierManagement(this);
        });
    }

    private void handleDeleteOuvrier(user ouvrier) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmer");
        confirmAlert.setHeaderText("Supprimer Ouvrier");
        confirmAlert.setContentText("Voulez-vous vraiment supprimer " + ouvrier.getName() + "?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userservice service = new userservice();
                    service.deleteUser(ouvrier.getId());

                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Ouvrier supprimé!");
                    refreshTable();

                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de suppression: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleAddOuvrier() {
        loadInMainLayout("/fxml/OuvrierAdd.fxml", controller -> {
            OuvrierAdd addController = (OuvrierAdd) controller;
            addController.setOuvrierManagement(this);
        });
    }

    @FXML
    private void handleRefresh() {
        refreshTable();
    }

    public void refreshTable() {
        loadOuvriers();
    }

    private void loadInMainLayout(String fxmlPath, ControllerCallback callback) {
        try {
            Parent root = cardContainer.getScene().getRoot();
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