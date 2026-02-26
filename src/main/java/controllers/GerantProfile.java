package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import entity.user;
import entity.user.Role;
import services.userservice;
import services.SessionManager;

import java.io.File;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class GerantProfile {

    // ── View Mode ──────────────────────────────────────────
    @FXML private VBox viewModeContainer;
    @FXML private ImageView profileImageView;   // shows photo or avatar
    @FXML private Label displayNameLabel;
    @FXML private Label emailDisplayLabel;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label roleDetailLabel;

    // ── Edit Mode ──────────────────────────────────────────
    @FXML private VBox editModeContainer;
    @FXML private ImageView editProfileImageView;  // preview while editing
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label editPhotoStatus;

    // ── Error Labels ───────────────────────────────────────
    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;

    private user currentUser;
    private String pendingPhotoUrl; // holds new photo path during edit

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{8}$");
    private static final Pattern NAME_PATTERN  = Pattern.compile("^[A-Za-zÀ-ÿ\\s]{2,50}$");

    // ══════════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            this.currentUser = sessionManager.getCurrentUser();
            displayUserInfo();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Session expirée. Veuillez vous reconnecter.");
        }
        hideAllErrors();
    }

    // ══════════════════════════════════════════════════════
    // DISPLAY USER INFO (View Mode)
    // ══════════════════════════════════════════════════════
    private void displayUserInfo() {
        if (currentUser == null) return;

        // ── Load profile photo or DiceBear avatar ──────────
        loadProfileImage(profileImageView, currentUser);

        // ── Header ─────────────────────────────────────────
        displayNameLabel.setText(currentUser.getName());
        emailDisplayLabel.setText(currentUser.getEmail());

        String roleName = getRoleFriendlyName(currentUser.getRole());
        roleLabel.setText(roleName);

        String status = currentUser.getStatus();
        statusLabel.setText(status);
        if ("ACTIVE".equals(status)) {
            statusLabel.setStyle("-fx-padding: 6 18; -fx-background-radius: 15px; -fx-font-size: 13px; " +
                    "-fx-font-weight: bold; -fx-background-color: rgba(90,152,20,0.15); -fx-text-fill: #5a9814;");
        } else {
            statusLabel.setStyle("-fx-padding: 6 18; -fx-background-radius: 15px; -fx-font-size: 13px; " +
                    "-fx-font-weight: bold; -fx-background-color: rgba(231,76,60,0.15); -fx-text-fill: #e74c3c;");
        }

        // ── Detail fields ──────────────────────────────────
        nameLabel.setText(currentUser.getName());
        emailLabel.setText(currentUser.getEmail());
        phoneLabel.setText(currentUser.getPhone());
        roleDetailLabel.setText(roleName);
    }

    // ══════════════════════════════════════════════════════
    // LOAD PROFILE IMAGE — photo URL or DiceBear fallback
    // ══════════════════════════════════════════════════════
    private void loadProfileImage(ImageView imageView, user u) {
        String photoUrl = u.getProfilePicture();

        if (photoUrl != null && !photoUrl.isBlank()) {
            // Could be a file:// path or http:// URL
            try {
                String url = photoUrl.startsWith("http") ? photoUrl : new File(photoUrl).toURI().toString();
                Image img = new Image(url, 150, 150, true, true, true);
                img.progressProperty().addListener((obs, o, n) -> {
                    if (n.doubleValue() == 1.0) {
                        Platform.runLater(() -> {
                            if (!img.isError()) {
                                imageView.setImage(img);
                            } else {
                                loadDicebear(imageView, u.getName());
                            }
                        });
                    }
                });
                imageView.setImage(img);
                return;
            } catch (Exception ignored) {}
        }

        // Fallback: DiceBear avatar
        loadDicebear(imageView, u.getName());
    }

    private void loadDicebear(ImageView imageView, String name) {
        String seed = name.trim().toLowerCase().replaceAll("[^a-z0-9]", "-");
        String url  = "https://api.dicebear.com/7.x/avataaars/png?seed=" + seed + "&size=150&backgroundColor=b6e3f4,c0aede";
        try {
            Image img = new Image(url, 150, 150, true, true, true);
            imageView.setImage(img);
        } catch (Exception e) {
            System.err.println("DiceBear load failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // EDIT MODE — switch + populate
    // ══════════════════════════════════════════════════════
    @FXML
    private void handleEdit() {
        viewModeContainer.setVisible(false);
        viewModeContainer.setManaged(false);
        editModeContainer.setVisible(true);
        editModeContainer.setManaged(true);

        nameField.setText(currentUser.getName());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhone());
        passwordField.clear();
        confirmPasswordField.clear();
        pendingPhotoUrl = currentUser.getProfilePicture();

        // Show current photo in edit preview
        loadProfileImage(editProfileImageView, currentUser);
        if (editPhotoStatus != null) editPhotoStatus.setText("");
        hideAllErrors();
    }

    // ══════════════════════════════════════════════════════
    // FILE CHOOSER — pick photo from PC
    // ══════════════════════════════════════════════════════
    @FXML
    private void handleChoosePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
        );

        Stage stage = (Stage) editProfileImageView.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            // Store the absolute path — saved to DB
            pendingPhotoUrl = selectedFile.getAbsolutePath();

            // Preview it immediately
            try {
                Image img = new Image(selectedFile.toURI().toString(), 150, 150, true, true);
                editProfileImageView.setImage(img);
                if (editPhotoStatus != null) {
                    editPhotoStatus.setText("✅ Photo sélectionnée: " + selectedFile.getName());
                    editPhotoStatus.setStyle("-fx-text-fill: #388e3c; -fx-font-size: 12px;");
                }
            } catch (Exception e) {
                if (editPhotoStatus != null) {
                    editPhotoStatus.setText("❌ Impossible de charger cette image");
                    editPhotoStatus.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12px;");
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════
    // SAVE
    // ══════════════════════════════════════════════════════
    @FXML
    private void handleSave() {
        hideAllErrors();
        boolean isValid = true;

        if (!validateName())  isValid = false;
        if (!validateEmail()) isValid = false;
        if (!validatePhone()) isValid = false;
        if (!passwordField.getText().trim().isEmpty()) {
            if (!validatePassword())        isValid = false;
            if (!validateConfirmPassword()) isValid = false;
        }
        if (!isValid) return;

        // Check email uniqueness if changed
        if (!emailField.getText().trim().equalsIgnoreCase(currentUser.getEmail())) {
            try {
                userservice service = new userservice();
                user existing = service.findByEmail(emailField.getText().trim());
                if (existing != null) {
                    showError(emailError, "❌ Cet email existe déjà!");
                    return;
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de vérification: " + e.getMessage());
                return;
            }
        }

        try {
            currentUser.setName(nameField.getText().trim());
            currentUser.setEmail(emailField.getText().trim().toLowerCase());
            currentUser.setPhone(phoneField.getText().trim());
            if (!passwordField.getText().trim().isEmpty()) {
                currentUser.setPassword(passwordField.getText().trim());
            }

            // ── Save new photo if changed ──────────────────
            if (pendingPhotoUrl != null && !pendingPhotoUrl.equals(currentUser.getProfilePicture())) {
                currentUser.setProfilePicture(pendingPhotoUrl);
            }

            userservice service = new userservice();
            service.updateUser(currentUser);

            // Also update profile picture in DB separately (safety)
            if (pendingPhotoUrl != null) {
                service.updateProfilePicture(currentUser.getId(), pendingPhotoUrl);
            }

            // Update session
            SessionManager.getInstance().createSession(currentUser);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Profil mis à jour avec succès!");
            displayUserInfo();
            handleCancel();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec de mise à jour: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        editModeContainer.setVisible(false);
        editModeContainer.setManaged(false);
        viewModeContainer.setVisible(true);
        viewModeContainer.setManaged(true);
        passwordField.clear();
        confirmPasswordField.clear();
        pendingPhotoUrl = null;
        hideAllErrors();
    }

    // ══════════════════════════════════════════════════════
    // VALIDATION
    // ══════════════════════════════════════════════════════
    private boolean validateName() {
        String v = nameField.getText().trim();
        if (v.isEmpty())                          { showError(nameError, "❌ Le nom est obligatoire"); return false; }
        if (!NAME_PATTERN.matcher(v).matches())   { showError(nameError, "❌ Lettres uniquement, 2-50 caractères"); return false; }
        return true;
    }
    private boolean validateEmail() {
        String v = emailField.getText().trim();
        if (v.isEmpty())                           { showError(emailError, "❌ L'email est obligatoire"); return false; }
        if (!EMAIL_PATTERN.matcher(v).matches())   { showError(emailError, "❌ Format invalide (ex: user@email.com)"); return false; }
        return true;
    }
    private boolean validatePhone() {
        String v = phoneField.getText().trim();
        if (v.isEmpty())                           { showError(phoneError, "❌ Téléphone obligatoire"); return false; }
        if (!PHONE_PATTERN.matcher(v).matches())   { showError(phoneError, "❌ 8 chiffres requis"); return false; }
        return true;
    }
    private boolean validatePassword() {
        String v = passwordField.getText().trim();
        if (v.length() < 8) { showError(passwordError, "❌ Minimum 8 caractères"); return false; }
        if (!v.matches(".*[A-Z].*") || !v.matches(".*[a-z].*") ||
                !v.matches(".*[0-9].*") || !v.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            showError(passwordError, "❌ Majuscule, minuscule, chiffre et caractère spécial requis");
            return false;
        }
        return true;
    }
    private boolean validateConfirmPassword() {
        if (confirmPasswordField.getText().trim().isEmpty()) { showError(confirmPasswordError, "❌ Confirmez le mot de passe"); return false; }
        if (!passwordField.getText().trim().equals(confirmPasswordField.getText().trim())) {
            showError(confirmPasswordError, "❌ Les mots de passe ne correspondent pas"); return false;
        }
        return true;
    }

    // ══════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════
    private String getRoleFriendlyName(Role role) {
        switch (role) {
            case ROLE_ADMIN:   return "Administrateur";
            case ROLE_GERANT:  return "Gérant";
            case ROLE_OUVRIER: return "Ouvrier";
            default:           return role.name();
        }
    }
    private void showError(Label lbl, String msg) { lbl.setText(msg); lbl.setVisible(true); }
    private void hideAllErrors() {
        if (nameError != null)            nameError.setVisible(false);
        if (emailError != null)           emailError.setVisible(false);
        if (phoneError != null)           phoneError.setVisible(false);
        if (passwordError != null)        passwordError.setVisible(false);
        if (confirmPasswordError != null) confirmPasswordError.setVisible(false);
    }
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}