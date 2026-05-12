
package controllers;

import entity.application;
import entity.application.DesiredRole;
import entity.farm;
import entity.user;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.applicationservice;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;

public class FarmApplyController {

    @FXML private Label farmNameLabel;
    @FXML private Label farmLocationLabel;
    @FXML private Label farmSurfaceLabel;
    @FXML private Label farmDescLabel;

    @FXML private ToggleGroup roleGroup;
    @FXML private RadioButton gerantRadio;
    @FXML private RadioButton ouvrierRadio;

    @FXML private Label cvFileLabel;
    @FXML private Label roleError;
    @FXML private Label cvError;
    @FXML private Label successMsg;

    private user currentUser;
    private farm selectedFarm;
    private File selectedCvFile;

    // Directory where CVs are saved (relative to project root)
    private static final String CV_UPLOAD_DIR = "uploads/cvs/";

    public void initData(user user, farm farm) {
        this.currentUser = user;
        this.selectedFarm = farm;

        farmNameLabel.setText(farm.getName());
        farmLocationLabel.setText("📍 " + (farm.getLocation() != null ? farm.getLocation() : "—"));
        farmSurfaceLabel.setText("📐 " + farm.getSurface() + " ha");
        farmDescLabel.setText(farm.getDescription() != null ? farm.getDescription() : "Aucune description.");
    }

    @FXML
    private void handleChooseCv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir votre CV");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("Word Files", "*.doc", "*.docx")
        );
        Stage stage = (Stage) cvFileLabel.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedCvFile = file;
            cvFileLabel.setText("📎 " + file.getName());
            cvFileLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 13px;");
            cvError.setVisible(false);
        }
    }

    @FXML
    private void handleSubmit() {
        hideErrors();
        boolean valid = true;

        // Validate role selection
        if (roleGroup.getSelectedToggle() == null) {
            roleError.setVisible(true);
            valid = false;
        }

        // Validate CV
        if (selectedCvFile == null) {
            cvError.setVisible(true);
            valid = false;
        }

        if (!valid) return;

        try {
            // Check duplicate application
            applicationservice appService = new applicationservice();
            if (appService.hasApplied(currentUser.getId(), selectedFarm.getId())) {
                showAlert(Alert.AlertType.WARNING, "Déjà postulé",
                        "Vous avez déjà postulé à cette ferme.");
                return;
            }

            // Save CV file to uploads directory
            String savedCvPath = saveCvFile(selectedCvFile, currentUser.getId(), selectedFarm.getId());

            // Determine desired role
            DesiredRole desiredRole = (roleGroup.getSelectedToggle() == gerantRadio)
                    ? DesiredRole.ROLE_GERANT
                    : DesiredRole.ROLE_OUVRIER;

            // Create and submit application
            application app = new application(
                    currentUser.getId(),
                    selectedFarm.getId(),
                    desiredRole,
                    savedCvPath
            );
            appService.submit(app);

            // Show success → navigate to waiting screen
            goToWaiting();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + e.getMessage());
        }
    }

    private String saveCvFile(File file, int userId, int farmId) throws IOException {
        Path dir = Paths.get(CV_UPLOAD_DIR);
        Files.createDirectories(dir);

        String ext = file.getName().substring(file.getName().lastIndexOf('.'));
        String newName = "cv_user" + userId + "_farm" + farmId + "_" + System.currentTimeMillis() + ext;
        Path dest = dir.resolve(newName);
        Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toString();
    }

    private void goToWaiting() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PendingWaiting.fxml"));
            Parent root = loader.load();

            PendingWaitingController controller = loader.getController();
            controller.initData(currentUser);

            Stage stage = (Stage) farmNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Candidature envoyée - AgriSense 360");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FarmList.fxml"));
            Parent root = loader.load();

            FarmListController controller = loader.getController();
            controller.initData(currentUser);

            Stage stage = (Stage) farmNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Choisir une ferme - AgriSense 360");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideErrors() {
        roleError.setVisible(false);
        cvError.setVisible(false);
        successMsg.setVisible(false);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
