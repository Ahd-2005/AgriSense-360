package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import entity.user;
import services.CameraService;
import services.FaceRecognitionService;
import services.SessionManager;
import services.userservice;

public class FaceLoginController {

    @FXML private ImageView cameraPreview;
    @FXML private ImageView capturedPreview;
    @FXML private Label     placeholderLabel;
    @FXML private Label     statusLabel;
    @FXML private Button    startCameraBtn;
    @FXML private Button    captureBtn;
    @FXML private Button    loginBtn;

    private CameraService cameraService;
    private String        capturedImagePath;

    // ── 1. Start camera ───────────────────────────────────
    @FXML
    private void handleStartCamera() {
        showStatus("⏳ Ouverture de la caméra...", true);
        startCameraBtn.setDisable(true);

        new Thread(() -> {
            cameraService = new CameraService();
            boolean opened = cameraService.open();

            Platform.runLater(() -> {
                if (!opened) {
                    showStatus("❌ Impossible d'accéder à la caméra.", false);
                    startCameraBtn.setDisable(false);
                    return;
                }
                if (placeholderLabel != null) placeholderLabel.setVisible(false);
                cameraPreview.setVisible(true);
                capturedPreview.setVisible(false);
                captureBtn.setDisable(false);
                startCameraBtn.setDisable(false);
                startCameraBtn.setText("🔄  Redémarrer");
                showStatus("✅ Caméra prête. Placez votre visage et prenez la photo.", true);

                cameraService.startPreview(img ->
                        Platform.runLater(() -> cameraPreview.setImage(img))
                );
            });
        }).start();
    }

    // ── 2. Capture snapshot ───────────────────────────────
    @FXML
    private void handleCapture() {
        if (cameraService == null || !cameraService.isOpen()) {
            showStatus("❌ La caméra n'est pas démarrée.", false);
            return;
        }
        showStatus("📸 Capture en cours...", true);
        captureBtn.setDisable(true);

        new Thread(() -> {
            String path = cameraService.captureAndSave();
            Platform.runLater(() -> {
                if (path == null) {
                    showStatus("❌ Impossible de capturer. Réessayez.", false);
                    captureBtn.setDisable(false);
                    return;
                }
                capturedImagePath = path;
                Image snapshot = new Image("file:///" + path.replace("\\", "/"),
                        320, 240, false, true);
                capturedPreview.setImage(snapshot);
                capturedPreview.setVisible(true);
                cameraPreview.setVisible(false);
                loginBtn.setDisable(false);
                captureBtn.setDisable(false);
                captureBtn.setText("🔁  Reprendre");
                captureBtn.setOnAction(e -> handleRetake());
                showStatus("📸 Photo prise. Cliquez sur Se connecter.", true);
            });
        }).start();
    }

    private void handleRetake() {
        capturedImagePath = null;
        loginBtn.setDisable(true);
        capturedPreview.setVisible(false);
        captureBtn.setText("📸  Prendre la photo");
        captureBtn.setOnAction(e -> handleCapture());
        handleStartCamera();
    }

    // ── 3. Face login ─────────────────────────────────────
    @FXML
    private void handleFaceLogin() {
        if (capturedImagePath == null) {
            showStatus("❌ Veuillez d'abord capturer votre visage.", false);
            return;
        }
        showStatus("🔍 Reconnaissance en cours...", true);
        loginBtn.setDisable(true);

        String pathToCheck = capturedImagePath;
        new Thread(() -> {
            FaceRecognitionService faceService = FaceRecognitionService.getInstance();
            int matchedUserId = faceService.loginByFace(pathToCheck);

            Platform.runLater(() -> {
                if (matchedUserId == -1) {
                    showStatus("❌ Visage non reconnu. Essayez une autre capture.", false);
                    loginBtn.setDisable(false);
                    return;
                }
                try {
                    userservice service = new userservice();
                    user loggedInUser = service.findById(matchedUserId);

                    if (loggedInUser == null) {
                        showStatus("❌ Utilisateur introuvable.", false);
                        loginBtn.setDisable(false);
                        return;
                    }
                    if ("BLOCKED".equals(loggedInUser.getStatus())) {
                        showStatus("❌ Ce compte est bloqué.", false);
                        loginBtn.setDisable(false);
                        return;
                    }
                    if (!FaceVerificationController.userHasFaceRegistered(matchedUserId)) {
                        showStatus("❌ Face ID non configuré pour ce compte.\nConnectez-vous normalement puis activez Face ID dans votre profil.", false);
                        loginBtn.setDisable(false);
                        return;
                    }

                    if (cameraService != null) cameraService.stop();

                    SessionManager.getInstance().createSession(loggedInUser);
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
                    Parent root = loader.load();
                    MainLayoutController controller = loader.getController();
                    controller.configureForUserRole(loggedInUser.getRole());
                    controller.setCurrentUser(loggedInUser);

                    Stage stage = (Stage) startCameraBtn.getScene().getWindow();
                    stage.setScene(new Scene(root, 1400, 800));
                    stage.setTitle("AgriSense 360 - Dashboard");
                    stage.setMaximized(true);
                    stage.centerOnScreen();

                } catch (Exception e) {
                    e.printStackTrace();
                    showStatus("❌ Erreur : " + e.getMessage(), false);
                    loginBtn.setDisable(false);
                }
            });
        }).start();
    }

    // ── Back ──────────────────────────────────────────────
    @FXML
    private void handleBack() {
        if (cameraService != null) cameraService.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) startCameraBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Connexion - AgriSense 360");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String message, boolean isSuccess) {
        statusLabel.setText(message);
        statusLabel.setStyle(isSuccess
                ? "-fx-text-fill: #388e3c; -fx-font-size: 13px; -fx-font-weight: bold;"
                : "-fx-text-fill: #d32f2f; -fx-font-size: 13px; -fx-font-weight: bold;");
    }
}