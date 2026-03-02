package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import entity.user;
import services.CameraService;
import services.FaceRecognitionService;
import services.SessionManager;
import utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * FaceVerificationController — live webcam face registration.
 * Loads INSIDE MainLayout's contentArea — sidebar stays visible.
 *
 * The "Back" button simply calls MainLayoutController to navigate to profile.
 */
public class FaceVerificationController {

    @FXML private ImageView cameraPreview;
    @FXML private ImageView capturedPreview;
    @FXML private VBox      cameraOffPlaceholder;
    @FXML private Circle    faceGuide;
    @FXML private Label     statusIcon;
    @FXML private Label     statusTitle;
    @FXML private Label     statusDesc;
    @FXML private Label     resultLabel;
    @FXML private HBox      statusCard;
    @FXML private Button    startCameraBtn;
    @FXML private Button    captureBtn;
    @FXML private Button    registerBtn;
    @FXML private Button    deleteBtn;

    private CameraService cameraService;
    private String        capturedImagePath;
    private user          currentUser;

    // ── Init ──────────────────────────────────────────────
    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        refreshFaceStatus();
    }

    // ── 1. Start camera ───────────────────────────────────
    @FXML
    private void handleStartCamera() {
        setResult("⏳ Ouverture de la caméra...", true);
        startCameraBtn.setDisable(true);

        new Thread(() -> {
            cameraService = new CameraService();
            boolean opened = cameraService.open();

            Platform.runLater(() -> {
                if (!opened) {
                    setResult("❌ Impossible d'accéder à la caméra. Vérifiez votre webcam.", false);
                    startCameraBtn.setDisable(false);
                    return;
                }
                cameraOffPlaceholder.setVisible(false);
                cameraPreview.setVisible(true);
                capturedPreview.setVisible(false);
                faceGuide.setVisible(true);
                captureBtn.setDisable(false);
                startCameraBtn.setText("🔄  Redémarrer");
                startCameraBtn.setDisable(false);
                setResult("✅ Caméra prête. Placez votre visage dans le cercle vert.", true);

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
            setResult("❌ La caméra n'est pas démarrée.", false);
            return;
        }
        setResult("📸 Capture en cours...", true);
        captureBtn.setDisable(true);

        new Thread(() -> {
            String path = cameraService.captureAndSave();
            Platform.runLater(() -> {
                if (path == null) {
                    setResult("❌ Impossible de capturer l'image. Réessayez.", false);
                    captureBtn.setDisable(false);
                    return;
                }
                capturedImagePath = path;
                Image snapshot = new Image("file:///" + path.replace("\\", "/"),
                        320, 240, false, true);
                capturedPreview.setImage(snapshot);
                capturedPreview.setVisible(true);
                cameraPreview.setVisible(false);
                faceGuide.setVisible(false);

                registerBtn.setDisable(false);
                captureBtn.setText("🔁  Reprendre");
                captureBtn.setDisable(false);
                captureBtn.setOnAction(e -> handleRetake());
                setResult("📸 Photo capturée ! Cliquez sur Enregistrer.", true);
            });
        }).start();
    }

    private void handleRetake() {
        capturedImagePath = null;
        registerBtn.setDisable(true);
        capturedPreview.setVisible(false);
        captureBtn.setText("📸  Prendre la photo");
        captureBtn.setOnAction(e -> handleCapture());
        handleStartCamera();
    }

    // ── 3. Register face ──────────────────────────────────
    @FXML
    private void handleRegisterFace() {
        if (capturedImagePath == null) {
            setResult("❌ Veuillez d'abord capturer votre visage.", false);
            return;
        }
        setResult("⏳ Enregistrement en cours...", true);
        registerBtn.setDisable(true);

        String pathToRegister = capturedImagePath;
        new Thread(() -> {
            try {
                FaceRecognitionService faceService = FaceRecognitionService.getInstance();
                FaceRecognitionService.FaceRecognitionResult result =
                        faceService.registerFace(currentUser.getId(), pathToRegister);

                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        setResult("✅ Visage enregistré ! Vous pouvez maintenant vous connecter via Face ID.", true);
                        capturedImagePath = null;
                        refreshFaceStatus();
                    } else {
                        setResult("❌ Échec : " + result.getMessage(), false);
                        registerBtn.setDisable(false);
                    }
                });
            } catch (FaceRecognitionService.FaceRecognitionException e) {
                Platform.runLater(() -> {
                    setResult("❌ " + e.getMessage(), false);
                    registerBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setResult("❌ Erreur : " + e.getMessage(), false);
                    registerBtn.setDisable(false);
                });
            }
        }).start();
    }

    // ── 4. Delete face ────────────────────────────────────
    @FXML
    private void handleDeleteFace() {
        try {
            deleteFaceFromDB(currentUser.getId());
            setResult("🗑️ Face ID supprimé.", true);
            capturedImagePath = null;
            capturedPreview.setVisible(false);
            cameraOffPlaceholder.setVisible(true);
            registerBtn.setDisable(true);
            captureBtn.setDisable(true);
            refreshFaceStatus();
        } catch (SQLException e) {
            setResult("❌ Erreur suppression : " + e.getMessage(), false);
        }
    }

    // ── Back — navigate inside MainLayout (sidebar stays!) ──
    @FXML
    private void handleBack() {
        stopCameraIfRunning();
        // Use MainLayoutController to navigate — sidebar never touched
        MainLayoutController main = MainLayoutController.getInstance();
        if (main != null) {
            main.navigateToProfile();
        }
    }

    // ── DB helpers ─────────────────────────────────────────
    public static boolean userHasFaceRegistered(int userId) {
        try {
            Connection cnx = MyDataBase.getInstance().getCnx();
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT COUNT(*) FROM user_faces WHERE user_id = ?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void deleteFaceFromDB(int userId) throws SQLException {
        Connection cnx = MyDataBase.getInstance().getCnx();
        PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM user_faces WHERE user_id = ?");
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    // ── Status card refresh ───────────────────────────────
    private void refreshFaceStatus() {
        boolean hasFace = userHasFaceRegistered(currentUser.getId());
        if (hasFace) {
            statusIcon.setText("✅");
            statusTitle.setText("Face ID configuré");
            statusTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
            statusDesc.setText("Votre visage est enregistré. Vous pouvez vous connecter via Face ID.");
            statusDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #388e3c; -fx-wrap-text: true;");
            statusCard.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 12; -fx-padding: 16;"
                    + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
            deleteBtn.setVisible(true);
            deleteBtn.setManaged(true);
        } else {
            statusIcon.setText("⚠️");
            statusTitle.setText("Face ID non configuré");
            statusTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e65100;");
            statusDesc.setText("Vous n'avez pas encore enregistré votre visage.");
            statusDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #bf360c; -fx-wrap-text: true;");
            statusCard.setStyle("-fx-background-color: #fff3e0; -fx-background-radius: 12; -fx-padding: 16;"
                    + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
            deleteBtn.setVisible(false);
            deleteBtn.setManaged(false);
        }
    }

    private void stopCameraIfRunning() {
        if (cameraService != null) cameraService.stop();
    }

    private void setResult(String message, boolean success) {
        resultLabel.setText(message);
        resultLabel.setStyle(success
                ? "-fx-text-fill: #388e3c; -fx-font-size: 13px; -fx-font-weight: bold;"
                : "-fx-text-fill: #d32f2f; -fx-font-size: 13px; -fx-font-weight: bold;");
    }
}