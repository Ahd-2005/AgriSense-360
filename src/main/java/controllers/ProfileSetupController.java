package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import entity.user;
import services.userservice;

import java.io.File;

public class ProfileSetupController {

    @FXML private ImageView avatarPreview;
    @FXML private Label statusLabel;
    @FXML private HBox styleBox;
    @FXML private ProgressIndicator loadingSpinner;

    private user currentUser;
    private String selectedAvatarUrl;

    private static final String[] DICEBEAR_STYLES = {
            "avataaars",
            "bottts",
            "fun-emoji",
            "pixel-art",
            "lorelei",
            "micah",
            "adventurer"
    };
    private int currentStyleIndex = 0;
    private String currentSeed;

    public void initData(user user) {
        this.currentUser = user;
        this.currentSeed = sanitizeSeed(user.getName());
        String dicebearUrl = buildDicebearUrl(DICEBEAR_STYLES[currentStyleIndex], currentSeed);
        this.selectedAvatarUrl = dicebearUrl;
        loadImageAsync(dicebearUrl);
    }

    private String buildDicebearUrl(String style, String seed) {
        return "https://api.dicebear.com/7.x/" + style + "/png?seed=" + seed + "&size=200&backgroundColor=b6e3f4,c0aede,d1d4f9,ffd5dc,ffdfbf";
    }

    private String sanitizeSeed(String name) {
        return name.trim().toLowerCase().replaceAll("[^a-z0-9]", "-");
    }

    // =============================================
    // PICK PHOTO FROM PC — opens file explorer
    // =============================================
    @FXML
    private void handleChooseFromPC() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
        );

        Stage stage = (Stage) avatarPreview.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            // Store the absolute file path — this gets saved to DB
            selectedAvatarUrl = selectedFile.getAbsolutePath();

            // Preview immediately
            try {
                Image img = new Image(selectedFile.toURI().toString(), 200, 200, true, true);
                avatarPreview.setImage(img);
                showStatus("✅ Photo sélectionnée: " + selectedFile.getName(), true);
            } catch (Exception e) {
                showStatus("❌ Impossible de charger cette image", false);
            }
        }
    }

    @FXML
    private void handleNextStyle() {
        currentStyleIndex = (currentStyleIndex + 1) % DICEBEAR_STYLES.length;
        String url = buildDicebearUrl(DICEBEAR_STYLES[currentStyleIndex], currentSeed);
        selectedAvatarUrl = url;
        loadImageAsync(url);
        showStatus("🎨 Style: " + DICEBEAR_STYLES[currentStyleIndex], true);
    }

    @FXML
    private void handlePrevStyle() {
        currentStyleIndex = (currentStyleIndex - 1 + DICEBEAR_STYLES.length) % DICEBEAR_STYLES.length;
        String url = buildDicebearUrl(DICEBEAR_STYLES[currentStyleIndex], currentSeed);
        selectedAvatarUrl = url;
        loadImageAsync(url);
        showStatus("🎨 Style: " + DICEBEAR_STYLES[currentStyleIndex], true);
    }

    @FXML
    private void handleRandomAvatar() {
        currentSeed = "user-" + System.currentTimeMillis();
        String url = buildDicebearUrl(DICEBEAR_STYLES[currentStyleIndex], currentSeed);
        selectedAvatarUrl = url;
        loadImageAsync(url);
        showStatus("🎲 Nouvel avatar généré!", true);
    }

    @FXML
    private void handleConfirm() {
        saveAndGoToLogin(selectedAvatarUrl);
    }

    @FXML
    private void handleSkip() {
        String url = buildDicebearUrl("avataaars", sanitizeSeed(currentUser.getName()));
        saveAndGoToLogin(url);
    }

    private void loadImageAsync(String url) {
        if (loadingSpinner != null) loadingSpinner.setVisible(true);

        Image image = new Image(url, 200, 200, true, true, true);
        image.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() == 1.0) {
                Platform.runLater(() -> {
                    if (image.isError()) {
                        showStatus("❌ Impossible de charger l'image", false);
                    } else {
                        avatarPreview.setImage(image);
                        showStatus("✅ Avatar prêt", true);
                    }
                    if (loadingSpinner != null) loadingSpinner.setVisible(false);
                });
            }
        });
        avatarPreview.setImage(image);
    }

    private void saveAndGoToLogin(String photoUrl) {
        try {
            userservice service = new userservice();
            service.updateProfilePicture(currentUser.getId(), photoUrl);
            currentUser.setProfilePicture(photoUrl);
            showStatus("✅ Profil sauvegardé! Redirection...", true);

            new Thread(() -> {
                try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                Platform.runLater(this::navigateToLogin);
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            showStatus("❌ Erreur lors de la sauvegarde: " + e.getMessage(), false);
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) avatarPreview.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Connexion - AgriSense 360");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("❌ Erreur de navigation: " + e.getMessage(), false);
        }
    }

    private void showStatus(String message, boolean isSuccess) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle(isSuccess
                    ? "-fx-text-fill: #388e3c; -fx-font-size: 13px; -fx-font-weight: bold;"
                    : "-fx-text-fill: #d32f2f; -fx-font-size: 13px; -fx-font-weight: bold;"
            );
        }
    }
}