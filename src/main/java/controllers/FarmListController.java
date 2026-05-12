package controllers;

import entity.farm;
import entity.user;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import services.SessionManager;
import services.farmservice;

import java.sql.SQLException;
import java.util.List;

public class FarmListController {

    @FXML private GridPane farmsGrid;
    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    @FXML private ScrollPane scrollPane;

    private user currentUser;

    // ── Called automatically when loaded via loadContent() (no initData) ──
    @FXML
    public void initialize() {
        user sessionUser = SessionManager.getInstance().getCurrentUser();
        if (sessionUser != null) {
            initData(sessionUser);
        }
    }

    // ── Called manually when navigating from FarmApply / PendingWaiting ──
    public void initData(user user) {
        this.currentUser = user;
        welcomeLabel.setText("Bonjour " + user.getName() + " ! Choisissez une ferme où postuler.");
        loadFarms();
    }

    private void loadFarms() {
        farmsGrid.getChildren().clear();
        try {
            farmservice service = new farmservice();
            List<farm> farms = service.getAllFarms();

            if (farms.isEmpty()) {
                statusLabel.setText("Aucune ferme disponible pour le moment.");
                statusLabel.setVisible(true);
                return;
            }

            int col = 0, row = 0;
            for (farm f : farms) {
                VBox card = buildFarmCard(f);
                farmsGrid.add(card, col, row);
                col++;
                if (col == 3) { col = 0; row++; }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Erreur lors du chargement des fermes.");
            statusLabel.setVisible(true);
        }
    }

    private VBox buildFarmCard(farm f) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(320);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #c8e6c9;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        );

        // Farm image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(290);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(false);
        if (f.getImage() != null && !f.getImage().isEmpty()) {
            try {
                imageView.setImage(new Image(f.getImage(), true));
            } catch (Exception e) {
                imageView.setImage(null);
            }
        }

        // Name
        Label nameLabel = new Label(f.getName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
        nameLabel.setWrapText(true);

        // Location
        Label locationLabel = new Label("📍 " + (f.getLocation() != null ? f.getLocation() : "Non précisée"));
        locationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #546e7a;");

        // Surface
        Label surfaceLabel = new Label("📐 " + f.getSurface() + " ha");
        surfaceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #546e7a;");

        // Description
        if (f.getDescription() != null && !f.getDescription().isEmpty()) {
            Label descLabel = new Label(f.getDescription());
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(290);
            descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");
            card.getChildren().addAll(imageView, nameLabel, locationLabel, surfaceLabel, descLabel);
        } else {
            card.getChildren().addAll(imageView, nameLabel, locationLabel, surfaceLabel);
        }

        // Apply button
        Button applyBtn = new Button("Postuler à cette ferme →");
        applyBtn.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-cursor: hand;" +
                        "-fx-pref-width: 290; -fx-pref-height: 38;"
        );
        applyBtn.setOnAction(e -> openApplyForm(f));
        card.getChildren().add(applyBtn);

        return card;
    }

    private void openApplyForm(farm selectedFarm) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FarmApply.fxml"));
            Parent root = loader.load();

            FarmApplyController controller = loader.getController();
            controller.initData(currentUser, selectedFarm);

            Stage stage = (Stage) farmsGrid.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Postuler - " + selectedFarm.getName());
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSkip() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PendingWaiting.fxml"));
            Parent root = loader.load();

            PendingWaitingController controller = loader.getController();
            controller.initData(currentUser);

            Stage stage = (Stage) farmsGrid.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("En attente - AgriSense 360");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}