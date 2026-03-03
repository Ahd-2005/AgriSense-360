package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import entity.user;
import services.userservice;
import services.SessionManager;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class OuvrierEdit {

    @FXML private StackPane avatarPane;
    @FXML private TextField profilePictureField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label nameError;
    @FXML private Label phoneError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;

    private user currentOuvrier;
    private user loggedInUser;
    private OuvrierManagement ouvrierManagement;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{8}$");
    private static final Pattern NAME_PATTERN  = Pattern.compile("^[A-Za-zÀ-ÿ\\s]{2,50}$");

    @FXML
    public void initialize() {
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) this.loggedInUser = sessionManager.getCurrentUser();

        profilePictureField.textProperty().addListener((obs, old, nv) -> refreshAvatarPreview(nv));
        hideAllErrors();
    }

    public void setOuvrier(user ouvrier) {
        this.currentOuvrier = ouvrier;
        fillFields();
    }

    public void setOuvrierManagement(OuvrierManagement management) {
        this.ouvrierManagement = management;
    }

    // ───────────────────────────────────────────────
    // REMPLISSAGE
    // ───────────────────────────────────────────────
    private void fillFields() {
        nameField.setText(currentOuvrier.getName());
        emailField.setText(currentOuvrier.getEmail());
        emailField.setEditable(false);
        phoneField.setText(currentOuvrier.getPhone() != null ? currentOuvrier.getPhone() : "");
        String pic = currentOuvrier.getProfilePicture();
        profilePictureField.setText(pic != null ? pic : "");
        refreshAvatarPreview(pic);
    }

    // ───────────────────────────────────────────────
    // AVATAR
    // ───────────────────────────────────────────────
    private void refreshAvatarPreview(String url) {
        avatarPane.getChildren().clear();
        if (url != null && !url.isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(url, 64, 64, true, true));
                iv.setFitWidth(64); iv.setFitHeight(64);
                Circle clip = new Circle(32, 32, 32);
                iv.setClip(clip);
                avatarPane.getChildren().add(iv);
                return;
            } catch (Exception ignored) {}
        }
        String name = nameField.getText();
        if (name == null || name.isBlank()) name = currentOuvrier != null ? currentOuvrier.getName() : "?";
        Label lbl = new Label(getInitials(name));
        lbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane bg = new StackPane(lbl);
        bg.setMinSize(64, 64); bg.setMaxSize(64, 64);
        bg.setStyle("-fx-background-color:" + getAvatarColor(name) + "; -fx-background-radius:32;");
        avatarPane.getChildren().add(bg);
    }

    @FXML
    private void handleGenerateAvatar() {
        String name = nameField.getText().trim();
        if (name.isEmpty() && currentOuvrier != null) name = currentOuvrier.getName();
        String seed = name.toLowerCase().replace(" ", "-") + "-" + System.currentTimeMillis() % 1000;
        profilePictureField.setText("https://api.dicebear.com/7.x/avataaars/png?seed=" + seed + "&size=128");
    }

    @FXML
    private void handlePreviewPicture() {
        refreshAvatarPreview(profilePictureField.getText());
    }

    // ───────────────────────────────────────────────
    // SAUVEGARDE
    // ───────────────────────────────────────────────
    @FXML
    private void handleSave() {
        hideAllErrors();
        boolean valid = true;

        if (!validateName())  valid = false;
        if (!validatePhone()) valid = false;

        if (!passwordField.getText().trim().isEmpty()) {
            if (!validatePassword())        valid = false;
            if (!validateConfirmPassword()) valid = false;
        }

        if (!valid) return;

        try {
            currentOuvrier.setName(nameField.getText().trim());
            currentOuvrier.setPhone(phoneField.getText().trim());

            String pic = profilePictureField.getText().trim();
            currentOuvrier.setProfilePicture(pic.isEmpty() ? null : pic);

            if (!passwordField.getText().trim().isEmpty())
                currentOuvrier.setPassword(passwordField.getText().trim());

            new userservice().updateUser(currentOuvrier);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Ouvrier mis à jour avec succès !");
            if (ouvrierManagement != null) ouvrierManagement.refreshTable();
            goBack();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la mise à jour : " + e.getMessage());
        }
    }

    // ───────────────────────────────────────────────
    // VALIDATION
    // ───────────────────────────────────────────────
    private boolean validateName() {
        String v = nameField.getText().trim();
        if (v.isEmpty())                        { showError(nameError, "❌ Le nom est requis"); return false; }
        if (!NAME_PATTERN.matcher(v).matches()) { showError(nameError, "❌ Nom invalide (lettres uniquement, 2-50 caractères)"); return false; }
        return true;
    }

    private boolean validatePhone() {
        String v = phoneField.getText().trim();
        if (v.isEmpty())                         { showError(phoneError, "❌ Le téléphone est requis"); return false; }
        if (!PHONE_PATTERN.matcher(v).matches()) { showError(phoneError, "❌ Numéro invalide (8 chiffres requis)"); return false; }
        return true;
    }

    private boolean validatePassword() {
        String p = passwordField.getText().trim();
        if (p.length() < 8) { showError(passwordError, "❌ Minimum 8 caractères"); return false; }
        if (!p.matches(".*[A-Z].*") || !p.matches(".*[a-z].*") ||
                !p.matches(".*[0-9].*") || !p.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            showError(passwordError, "❌ Mot de passe faible (maj, min, chiffre, caractère spécial)");
            return false;
        }
        return true;
    }

    private boolean validateConfirmPassword() {
        if (!passwordField.getText().trim().equals(confirmPasswordField.getText().trim())) {
            showError(confirmPasswordError, "❌ Les mots de passe ne correspondent pas"); return false;
        }
        return true;
    }

    // ───────────────────────────────────────────────
    // UTILITAIRES
    // ───────────────────────────────────────────────
    private void showError(Label lbl, String msg) { lbl.setText(msg); lbl.setVisible(true); }

    private void hideAllErrors() {
        for (Label l : new Label[]{nameError, phoneError, passwordError, confirmPasswordError})
            if (l != null) l.setVisible(false);
    }

    @FXML private void handleCancel() { goBack(); }

    private void goBack() {
        try {
            Parent root = nameField.getScene().getRoot();
            if (root instanceof BorderPane) {
                StackPane contentArea = (StackPane) ((BorderPane) root).getCenter();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OuvrierManagement.fxml"));
                Parent content = loader.load();
                OuvrierManagement ctrl = loader.getController();
                ctrl.refreshTable();
                contentArea.getChildren().clear();
                contentArea.getChildren().add(content);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner à la liste.");
        }
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
    }

    private String getAvatarColor(String name) {
        String[] colors = {"#2d7a3a","#2980b9","#8e44ad","#c0392b","#d35400","#16a085","#2c3e50"};
        return colors[Math.abs(name.hashCode()) % colors.length];
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}