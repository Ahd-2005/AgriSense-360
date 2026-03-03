package com.example.agrisense360.controllers;

import com.example.agrisense360.entity.Camera;
import com.example.agrisense360.entity.MotionEvent;
import com.example.agrisense360.utils.SessionCameraManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CameraManagementController {
    @FXML
    private TableView<Camera> cameraTable;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sensitivityFilterCombo;
    @FXML
    private Label statusLabel;
    @FXML
    private Label cameraCountLabel;

    private SessionCameraManager sessionManager;
    private ObservableList<Camera> cameraList;
    private ObservableList<Camera> filteredList;

    @FXML
    public void initialize() {
        sessionManager = SessionCameraManager.getInstance();

        // Setup sensitivity filter
        sensitivityFilterCombo.setItems(FXCollections.observableArrayList("All", "NIGHT", "DAY"));
        sensitivityFilterCombo.setValue("All");
        sensitivityFilterCombo.setOnAction(e -> filterCameras());

        // Setup search field
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterCameras());

        // Setup row double-click for monitoring
        cameraTable.setRowFactory(tv -> {
            TableRow<Camera> row = new TableRow<Camera>() {
                @Override
                protected void updateItem(Camera item, boolean empty) {
                    super.updateItem(item, empty);
                    setStyle("");
                }
            };
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    Camera camera = row.getItem();
                    openMonitoringView(camera);
                }
            });
            return row;
        });

        // Load cameras
        loadCameras();
    }

    public void loadCameras() {
        try {
            ObservableList<Camera> cameras = sessionManager.getAllCameras();
            cameraList = FXCollections.observableArrayList(cameras);
            filteredList = FXCollections.observableArrayList(cameras);
            cameraTable.setItems(filteredList);
            updateCameraCount();
            statusLabel.setText("Loaded " + cameras.size() + " camera(s)");
        } catch (Exception e) {
            showError("Error loading cameras: " + e.getMessage());
            statusLabel.setText("Error loading cameras");
        }
    }

    private void filterCameras() {
        String searchText = searchField.getText().toLowerCase();
        String sensitivityFilter = sensitivityFilterCombo.getValue();

        filteredList = cameraList.stream()
                .filter(c -> c.getCameraName().toLowerCase().contains(searchText))
                .filter(c -> sensitivityFilter.equals("All") || c.getSensitivityLevel().equals(sensitivityFilter))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        cameraTable.setItems(filteredList);
        updateCameraCount();
    }

    private void updateCameraCount() {
        cameraCountLabel.setText("Total: " + filteredList.size() + " camera(s)");
    }

    @FXML
    private void handleAddCamera() {
        openCameraDialog(null);
    }

    @FXML
    private void handleEditCamera() {
        Camera selectedCamera = cameraTable.getSelectionModel().getSelectedItem();
        if (selectedCamera == null) {
            showWarning("Please select a camera to edit");
            return;
        }
        openCameraDialog(selectedCamera);
    }

    @FXML
    private void handleDeleteCamera() {
        Camera selectedCamera = cameraTable.getSelectionModel().getSelectedItem();
        if (selectedCamera == null) {
            showWarning("Please select a camera to delete");
            return;
        }

        Optional<ButtonType> result = showConfirmation(
                "Delete Camera",
                "Are you sure you want to delete '" + selectedCamera.getCameraName() + "'?",
                "This action cannot be undone."
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                sessionManager.deleteCamera(selectedCamera.getId());
                loadCameras();
                statusLabel.setText("Camera deleted successfully");
            } catch (Exception e) {
                showError("Error deleting camera: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleMonitorCamera() {
        Camera selectedCamera = cameraTable.getSelectionModel().getSelectedItem();
        if (selectedCamera == null) {
            showWarning("Please select a camera to monitor");
            return;
        }
        openMonitoringView(selectedCamera);
    }

    private void openCameraDialog(Camera cameraToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/camera-add-view.fxml"));
            Parent root = loader.load();

            AddCameraDialogController controller = loader.getController();
            controller.setParentController(this);

            if (cameraToEdit != null) {
                controller.setCamera(cameraToEdit);
            }

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 600, 650));
            stage.setTitle(cameraToEdit != null ? "Edit Camera" : "Add Camera");
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException e) {
            showError("Error opening camera dialog: " + e.getMessage());
        }
    }

    private void openMonitoringView(Camera camera) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/camera-monitoring-view.fxml"));
            Parent root = loader.load();

            CameraMonitoringController controller = loader.getController();
            controller.setCamera(camera);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setTitle("Monitoring: " + camera.getCameraName());
            stage.setOnCloseRequest(e -> controller.stopMonitoring());
            stage.show();

        } catch (IOException e) {
            showError("Error opening monitoring view: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportReport() {
        if (cameraList == null || cameraList.isEmpty()) {
            showWarning("No cameras to export");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report As");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("camera_report_" + System.currentTimeMillis() + ".csv");

        File file = fileChooser.showSaveDialog(cameraTable.getScene().getWindow());
        if (file != null) {
            try {
                exportToCSV(file);
                statusLabel.setText("Report exported successfully");
                showInfo("Success", "Report exported to: " + file.getAbsolutePath());
            } catch (Exception e) {
                showError("Error exporting report: " + e.getMessage());
            }
        }
    }

    private void exportToCSV(File file) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (FileWriter writer = new FileWriter(file)) {
            // Write header
            writer.write("Camera ID,Camera Name,Location,Sensitivity,Alerts Enabled,Status,Created Date,Total Motion Events,Last Event\n");

            // Write camera data
            for (Camera camera : cameraList) {
                ObservableList<MotionEvent> events = sessionManager.getMotionEventsForCamera(camera.getId());
                String lastEvent = events.isEmpty() ? "N/A" : events.get(0).getDetectionTime().format(formatter);

                writer.write(String.format("%d,\"%s\",\"%s\",%s,%s,%s,%s,%d,%s\n",
                        camera.getId(),
                        camera.getCameraName(),
                        camera.getLocation() != null ? camera.getLocation() : "N/A",
                        camera.getSensitivityLevel(),
                        camera.isAlertsEnabled() ? "Yes" : "No",
                        camera.isActive() ? "Active" : "Inactive",
                        camera.getCreatedDate() != null ? camera.getCreatedDate().format(formatter) : "N/A",
                        events.size(),
                        lastEvent
                ));
            }
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Optional<ButtonType> showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return Optional.of(alert.showAndWait()).filter(Optional::isPresent).map(Optional::get);
    }
}
