package com.example.agrisense360.controllers;

import com.example.agrisense360.entity.Camera;
import com.example.agrisense360.entity.MotionEvent;
import com.example.agrisense360.utils.SessionCameraManager;
import com.example.agrisense360.utils.StreamLoader;
import com.example.agrisense360.utils.MotionDetectionEngine;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CameraMonitoringController {
    private static final int NULL_FRAME_SLEEP_MS = 35;
    private static final int LOOP_YIELD_MS = 8;

    @FXML
    private Label cameraNameLabel;
    @FXML
    private Label streamUrlLabel;
    @FXML
    private ImageView streamImageView;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private Label fpsLabel;
    @FXML
    private Label sensitivityLabel;
    @FXML
    private Circle motionIndicatorCircle;
    @FXML
    private Label motionStatusLabel;
    @FXML
    private Label frameCountLabel;
    @FXML
    private Label motionCountdownLabel;
    @FXML
    private Label severityLabel;
    @FXML
    private ListView<String> motionEventsList;
    @FXML
    private Label statusLabel;
    @FXML
    private Label alertLabel;

    private Camera camera;
    private SessionCameraManager sessionManager;
    private MotionDetectionEngine detectionEngine;
    private boolean isMonitoring = false;
    private Thread streamThread;
    private List<MotionEvent> detectedEvents = new ArrayList<>();
    private long frameCount = 0;
    private long startTime = 0;
    private StreamLoader streamLoader;

    @FXML
    public void initialize() {
        sessionManager = SessionCameraManager.getInstance();
        motionEventsList.setStyle("-fx-control-inner-background: #f9f9f9;");
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
        cameraNameLabel.setText(camera.getCameraName());
        streamUrlLabel.setText(camera.getStreamUrl());
        sensitivityLabel.setText(camera.getSensitivityLevel() + " Mode");

        // Initialize motion detection engine with appropriate sensitivity
        MotionDetectionEngine.SensitivityLevel sensitivity =
                "NIGHT".equalsIgnoreCase(camera.getSensitivityLevel()) ?
                        MotionDetectionEngine.SensitivityLevel.NIGHT :
                        MotionDetectionEngine.SensitivityLevel.DAY;
        detectionEngine = new MotionDetectionEngine(sensitivity);
        streamLoader = new StreamLoader(camera.getStreamUrl());
        streamLoader.warmUpAsync();
        statusLabel.setText("Camera ready. Stream preloading...");
    }

    @FXML
    private void handleStartMonitoring() {
        if (camera == null) {
            showError("Camera not set");
            return;
        }

        isMonitoring = true;
        startButton.setDisable(true);
        stopButton.setDisable(false);
        statusLabel.setText("Starting camera feed...");
        detectedEvents.clear();
        frameCount = 0;
        startTime = System.currentTimeMillis();

        // Start capture thread
        streamThread = new Thread(this::captureAndDetect);
        streamThread.setDaemon(true);
        streamThread.start();
    }

    @FXML
    private void handleStopMonitoring() {
        isMonitoring = false;
        stopButton.setDisable(true);

        if (streamThread != null) {
            try {
                streamThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (streamLoader != null) {
            streamLoader.stop();
        }
        detectionEngine.reset();

        Platform.runLater(() -> {
            startButton.setDisable(false);
            statusLabel.setText("Monitoring stopped");
            alertLabel.setText("Monitoring stopped");
            motionStatusLabel.setText("No Motion Detected");
            motionIndicatorCircle.setStyle("-fx-fill: #95a5a6;");
        });
    }

    @FXML
    private void handleBackToList() {
        handleStopMonitoring();
        streamImageView.getScene().getWindow().hide();
    }

    @FXML
    private void handleExportEvents() {
        if (detectedEvents.isEmpty()) {
            showWarning("No motion events to export");
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Save Motion Events As");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("motion_events_" + camera.getCameraName() + "_" + System.currentTimeMillis() + ".csv");

        File file = fileChooser.showSaveDialog(streamImageView.getScene().getWindow());
        if (file != null) {
            try {
                exportEventsToCsv(file);
                showInfo("Success", "Motion events exported to:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showError("Error exporting events: " + e.getMessage());
            }
        }
    }

    private void captureAndDetect() {
        try {
            streamLoader.start();
            boolean lastMotionState = false;

            Platform.runLater(() -> statusLabel.setText("Monitoring active..."));

            while (isMonitoring) {
                Image frame = streamLoader.getNextFrame();
                if (frame == null) {
                    // Wait before retry
                    try {
                        Thread.sleep(NULL_FRAME_SLEEP_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }

                // Detect motion
                boolean motionDetected = detectionEngine.detectMotion(frame);
                String severity = MotionDetectionEngine.determineSeverity(
                        (double) detectionEngine.getMotionCountdown() / 10
                );

                frameCount++;
                final int countdown = detectionEngine.getMotionCountdown();

                // Update UI
                Platform.runLater(() -> updateUI(frame, motionDetected, severity, countdown));

                // Log motion event on state change
                if (motionDetected && !lastMotionState) {
                    logMotionEvent(severity);
                }

                lastMotionState = motionDetected;

                // Frame rate control
                Thread.sleep(LOOP_YIELD_MS);
            }

        } catch (Exception e) {
            Platform.runLater(() -> {
                showError("Error during motion detection: " + e.getMessage());
                handleStopMonitoring();
            });
        } finally {
            streamLoader.stop();
        }
    }

    private void updateUI(Image frame, boolean motionDetected, String severity, int countdown) {
        // Display frame
        streamImageView.setImage(frame);

        // Update motion status
        if (motionDetected) {
            motionStatusLabel.setText("🚨 MOTION DETECTED!");
            motionStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            motionIndicatorCircle.setStyle("-fx-fill: #e74c3c;");
            alertLabel.setText("⚠️ Motion detected - " + severity + " severity");
        } else {
            motionStatusLabel.setText("✓ No Motion");
            motionStatusLabel.setStyle("-fx-text-fill: #27ae60;");
            motionIndicatorCircle.setStyle("-fx-fill: #95a5a6;");
            alertLabel.setText("Monitoring active");
        }

        // Update stats
        frameCountLabel.setText(String.valueOf(frameCount));
        motionCountdownLabel.setText(String.valueOf(countdown));
        severityLabel.setText(severity);

        // Update FPS
        if (frameCount > 0 && frameCount % 10 == 0) {
            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            if (elapsedSeconds > 0) {
                double fps = (double) frameCount / elapsedSeconds;
                fpsLabel.setText(String.format("FPS: %.1f", fps));
            }
        }
    }

    private void logMotionEvent(String severity) {
        try {
            // Calculate elapsed time since monitoring started
            long elapsedMillis = System.currentTimeMillis() - startTime;
            long elapsedSeconds = elapsedMillis / 1000;
            long elapsedMinutes = elapsedSeconds / 60;
            long remainingSeconds = elapsedSeconds % 60;
            
            // Format as MM:SS
            String elapsedTime = String.format("%02d:%02d", elapsedMinutes, remainingSeconds);
            
            MotionEvent event = new MotionEvent(
                    camera.getId(),
                    LocalDateTime.now(),
                    detectionEngine.getMotionCountdown(),
                    severity,
                    elapsedSeconds  // Store elapsed seconds from monitoring start
            );
            sessionManager.addMotionEvent(event);
            detectedEvents.add(event);

            // Update event list UI with elapsed time instead of system clock time
            String eventText = String.format("[%s] %s - Motion Detected",
                    elapsedTime,
                    severity
            );

            Platform.runLater(() -> {
                motionEventsList.getItems().add(0, eventText);
                if (motionEventsList.getItems().size() > 20) {
                    motionEventsList.getItems().remove(motionEventsList.getItems().size() - 1);
                }
            });

        } catch (Exception e) {
            System.err.println("Error logging motion event: " + e.getMessage());
        }
    }

    private void exportEventsToCsv(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Camera,Elapsed Time (MM:SS),Severity,Motion Frames,System Detection Time\n");

            for (MotionEvent event : detectedEvents) {
                // Convert elapsed seconds to MM:SS format
                long minutes = event.getElapsedSeconds() / 60;
                long seconds = event.getElapsedSeconds() % 60;
                String elapsedTime = String.format("%02d:%02d", minutes, seconds);
                
                DateTimeFormatter systemTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                String systemTime = event.getDetectionTime().format(systemTimeFormatter);
                
                writer.write(String.format("\"%s\",%s,%s,%d,%s\n",
                        camera.getCameraName(),
                        elapsedTime,
                        event.getSeverity(),
                        event.getMotionFrameCount(),
                        systemTime
                ));
            }
            
            System.out.println("✓ Motion events exported to: " + file.getAbsolutePath());
        }
    }

    public void stopMonitoring() {
        if (isMonitoring) {
            handleStopMonitoring();
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
}
