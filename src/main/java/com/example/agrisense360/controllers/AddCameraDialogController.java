package com.example.agrisense360.controllers;

import com.example.agrisense360.entity.Camera;
import com.example.agrisense360.utils.SessionCameraManager;
import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;

public class AddCameraDialogController {
    @FXML private TextField cameraNameField;
    @FXML private TextField streamUrlField;
    @FXML private ComboBox<String> locationCombo;
    @FXML private RadioButton dayModeRadio;
    @FXML private RadioButton nightModeRadio;
    @FXML private CheckBox alertsEnabledCheckbox;

    private SessionCameraManager sessionManager;
    private CameraManagementController parentController;
    private Camera currentCamera;

    @FXML
    public void initialize() {
        sessionManager = SessionCameraManager.getInstance();
        
        // Set up ToggleGroup for sensitivity radio buttons
        ToggleGroup sensitivityGroup = new ToggleGroup();
        dayModeRadio.setToggleGroup(sensitivityGroup);
        nightModeRadio.setToggleGroup(sensitivityGroup);
        dayModeRadio.setSelected(true);  // Day mode by default
        
        // Populate location combo box
        locationCombo.setItems(FXCollections.observableArrayList(
                "Field", "Barn", "Storage", "Equipment Shed", "Other"
        ));
        locationCombo.setValue("Field");
    }

    public void setParentController(CameraManagementController parent) {
        this.parentController = parent;
    }

    public void setCamera(Camera camera) {
        this.currentCamera = camera;
        cameraNameField.setText(camera.getCameraName());
        streamUrlField.setText(camera.getStreamUrl());
        locationCombo.setValue(camera.getLocation());
        
        if ("NIGHT".equals(camera.getSensitivityLevel())) {
            nightModeRadio.setSelected(true);
        } else {
            dayModeRadio.setSelected(true);
        }
        alertsEnabledCheckbox.setSelected(camera.isAlertsEnabled());
    }

    @FXML
    private void handleSaveCamera() {
        if (!validateInputs()) return;

        try {
            if (currentCamera != null) {
                // Update existing
                currentCamera.setCameraName(cameraNameField.getText().trim());
                currentCamera.setStreamUrl(streamUrlField.getText().trim());
                currentCamera.setLocation(locationCombo.getValue());
                currentCamera.setSensitivityLevel(nightModeRadio.isSelected() ? "NIGHT" : "DAY");
                currentCamera.setAlertsEnabled(alertsEnabledCheckbox.isSelected());
                sessionManager.updateCamera(currentCamera);
            } else {
                // Add new
                Camera newCamera = new Camera(
                        cameraNameField.getText().trim(),
                        streamUrlField.getText().trim(),
                        locationCombo.getValue(),
                        nightModeRadio.isSelected() ? "NIGHT" : "DAY"
                );
                newCamera.setAlertsEnabled(alertsEnabledCheckbox.isSelected());
                newCamera.setCreatedDate(LocalDateTime.now());
                sessionManager.addCamera(newCamera);
            }

            parentController.loadCameras();
            closeDialog();
        } catch (Exception e) {
            showError("Error saving camera: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private boolean validateInputs() {
        if (cameraNameField.getText().trim().isEmpty()) {
            showWarning("Camera name required");
            return false;
        }
        if (streamUrlField.getText().trim().isEmpty()) {
            showWarning("Stream URL required");
            return false;
        }
        if (locationCombo.getValue() == null) {
            showWarning("Select a location");
            return false;
        }
        return true;
    }

    private void closeDialog() {
        Stage stage = (Stage) cameraNameField.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.showAndWait();
    }
}
