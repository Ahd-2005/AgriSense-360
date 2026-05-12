package controllers;

import entity.application;
import entity.user;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.applicationservice;
import services.SessionManager;

import java.awt.Desktop;
import java.io.File;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PendingApplicationsController {

    @FXML private VBox applicationsContainer;
    @FXML private Label titleLabel;
    @FXML private Label emptyLabel;

    private user currentOwner;
    private applicationservice appService;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        try {
            appService = new applicationservice();
            currentOwner = SessionManager.getInstance().getCurrentUser();
            if (currentOwner != null) loadApplications();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadApplications() {
        applicationsContainer.getChildren().clear();
        try {
            List<application> apps = appService.getPendingByOwner(currentOwner.getId());

            if (apps.isEmpty()) {
                emptyLabel.setVisible(true);
                emptyLabel.setManaged(true);
                return;
            }

            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);

            for (application app : apps) {
                applicationsContainer.getChildren().add(buildCard(app));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox buildCard(application app) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: #c8e6c9;" +
                "-fx-border-radius: 10;" +
                "-fx-border-width: 1.5;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);"
        );

        // Header row
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 28px;");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(app.getUserName() != null ? app.getUserName() : "Utilisateur #" + app.getUserId());
        nameLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1b3a1f;");

        Label emailLabel = new Label(app.getUserEmail() != null ? app.getUserEmail() : "");
        emailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

        info.getChildren().addAll(nameLabel, emailLabel);
        header.getChildren().addAll(avatar, info);

        // Details row
        HBox details = new HBox(20);
        details.setAlignment(Pos.CENTER_LEFT);

        Label farmLabel = new Label("🌾 " + (app.getFarmName() != null ? app.getFarmName() : "Ferme #" + app.getFarmId()));
        farmLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");

        String roleDisplay = app.getDesiredRole() == application.DesiredRole.ROLE_GERANT ? "🧑‍💼 Gérant" : "👷 Ouvrier";
        Label roleLabel = new Label("Poste souhaité: " + roleDisplay);
        roleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #546e7a;");

        String dateStr = app.getAppliedAt() != null ? app.getAppliedAt().format(DATE_FMT) : "—";
        Label dateLabel = new Label("📅 " + dateStr);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaa;");

        details.getChildren().addAll(farmLabel, roleLabel, dateLabel);

        // Action buttons
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);

        // View CV button
        if (app.getCvPath() != null && !app.getCvPath().isEmpty()) {
            Button cvBtn = new Button("📄 Voir le CV");
            cvBtn.setStyle(
                    "-fx-background-color: #e3f2fd; -fx-text-fill: #1565c0;" +
                    "-fx-border-color: #90caf9; -fx-border-width: 1.5;" +
                    "-fx-background-radius: 8; -fx-border-radius: 8;" +
                    "-fx-padding: 7 16; -fx-cursor: hand; -fx-font-size: 13px;"
            );
            cvBtn.setOnAction(e -> openCv(app.getCvPath()));
            actions.getChildren().add(cvBtn);
        }

        // Accept as Gérant
        Button acceptGerantBtn = new Button("✅ Accepter comme Gérant");
        acceptGerantBtn.setStyle(
                "-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32;" +
                "-fx-border-color: #4CAF50; -fx-border-width: 1.5;" +
                "-fx-background-radius: 8; -fx-border-radius: 8;" +
                "-fx-padding: 7 16; -fx-cursor: hand; -fx-font-size: 13px;"
        );
        acceptGerantBtn.setOnAction(e -> acceptApplication(app, user.Role.ROLE_GERANT));

        // Accept as Ouvrier
        Button acceptOuvrierBtn = new Button("✅ Accepter comme Ouvrier");
        acceptOuvrierBtn.setStyle(
                "-fx-background-color: #e8f5e9; -fx-text-fill: #388e3c;" +
                "-fx-border-color: #81c784; -fx-border-width: 1.5;" +
                "-fx-background-radius: 8; -fx-border-radius: 8;" +
                "-fx-padding: 7 16; -fx-cursor: hand; -fx-font-size: 13px;"
        );
        acceptOuvrierBtn.setOnAction(e -> acceptApplication(app, user.Role.ROLE_OUVRIER));

        // Reject
        Button rejectBtn = new Button("❌ Refuser");
        rejectBtn.setStyle(
                "-fx-background-color: #ffebee; -fx-text-fill: #c62828;" +
                "-fx-border-color: #ef9a9a; -fx-border-width: 1.5;" +
                "-fx-background-radius: 8; -fx-border-radius: 8;" +
                "-fx-padding: 7 16; -fx-cursor: hand; -fx-font-size: 13px;"
        );
        rejectBtn.setOnAction(e -> rejectApplication(app));

        actions.getChildren().addAll(acceptGerantBtn, acceptOuvrierBtn, rejectBtn);
        card.getChildren().addAll(header, details, actions);
        return card;
    }

    private void acceptApplication(application app, user.Role assignedRole) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer l'acceptation");
        confirm.setHeaderText(null);
        confirm.setContentText("Accepter " + app.getUserName() + " comme " +
                (assignedRole == user.Role.ROLE_GERANT ? "Gérant" : "Ouvrier") + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    appService.accept(app.getId(), assignedRole);
                    showAlert(Alert.AlertType.INFORMATION, "Accepté",
                            app.getUserName() + " a été accepté comme " +
                            (assignedRole == user.Role.ROLE_GERANT ? "Gérant" : "Ouvrier") + ".");
                    loadApplications(); // refresh
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + e.getMessage());
                }
            }
        });
    }

    private void rejectApplication(application app) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer le refus");
        confirm.setHeaderText(null);
        confirm.setContentText("Refuser la candidature de " + app.getUserName() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    appService.reject(app.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Refusé",
                            "La candidature de " + app.getUserName() + " a été refusée.");
                    loadApplications();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + e.getMessage());
                }
            }
        });
    }

    private void openCv(String cvPath) {
        try {
            File file = new File(cvPath);
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                showAlert(Alert.AlertType.WARNING, "Fichier introuvable",
                        "Le CV est introuvable à: " + cvPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le CV: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadApplications();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
