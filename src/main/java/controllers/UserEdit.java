package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import entity.user;
import entity.user.Role;
import services.userservice;
import services.SessionManager;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class UserEdit {

    @FXML private StackPane avatarPane;
    @FXML private TextField profilePictureField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<Role> roleCombo;

    @FXML private Label nameError;
    @FXML private Label phoneError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label roleError;

    private user currentUser;
    private user loggedInUser;
    private AdminDashboard adminDashboard;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{8}$");
    private static final Pattern NAME_PATTERN  = Pattern.compile("^[A-Za-zÀ-ÿ\\s]{2,50}$");

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList(Role.values()));
        // French display names in ComboBox
        roleCombo.setConverter(new javafx.util.StringConverter<Role>() {
            @Override public String toString(Role r) {
                if (r == null) return "";
                switch (r) {
                    case ROLE_ADMIN:   return "Administrateur";
                    case ROLE_GERANT:  return "Gérant";
                    case ROLE_OUVRIER: return "Ouvrier";
                    default: return r.name();
                }
            }
            @Override public Role fromString(String s) { return null; }
        });

        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) this.loggedInUser = sessionManager.getCurrentUser();

        // Update avatar preview live as URL changes
        profilePictureField.textProperty().addListener((obs, old, nv) -> refreshAvatarPreview(nv));

        hideAllErrors();
    }

    public void setUser(user user) {
        this.currentUser = user;
        fillFields();
        configurePermissions();
    }

    public void setAdminDashboard(AdminDashboard dashboard) {
        this.adminDashboard = dashboard;
    }

    // ───────────────────────────────────────────────
    // REMPLISSAGE
    // ───────────────────────────────────────────────
    private void fillFields() {
        nameField.setText(currentUser.getName());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
        roleCombo.setValue(currentUser.getRole());
        String pic = currentUser.getProfilePicture();
        profilePictureField.setText(pic != null ? pic : "");
        refreshAvatarPreview(pic);
    }

    private void configurePermissions() {
        // L'email ne peut pas être modifié
        emailField.setEditable(false);
        // L'admin ne peut pas changer son propre rôle
        if (loggedInUser != null && currentUser != null
                && loggedInUser.getId() == currentUser.getId()) {
            roleCombo.setDisable(true);
        }
    }

    // ───────────────────────────────────────────────
    // AVATAR PREVIEW
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
        // Initiales
        String name = nameField.getText();
        if (name == null || name.isBlank()) name = currentUser != null ? currentUser.getName() : "?";
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
        if (name.isEmpty() && currentUser != null) name = currentUser.getName();
        // DiceBear avataaars style (même que votre implémentation existante)
        String seed = name.toLowerCase().replace(" ", "-")
                + "-" + System.currentTimeMillis() % 1000;
        String url = "https://api.dicebear.com/7.x/avataaars/png?seed=" + seed + "&size=128";
        profilePictureField.setText(url);
        // Le listener sur textProperty met à jour l'aperçu automatiquement
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
        if (!validateRole())  valid = false;

        if (!passwordField.getText().trim().isEmpty()) {
            if (!validatePassword())        valid = false;
            if (!validateConfirmPassword()) valid = false;
        }

        if (!valid) return;

        try {
            currentUser.setName(nameField.getText().trim());
            currentUser.setPhone(phoneField.getText().trim());
            currentUser.setRole(roleCombo.getValue());

            String pic = profilePictureField.getText().trim();
            currentUser.setProfilePicture(pic.isEmpty() ? null : pic);

            if (!passwordField.getText().trim().isEmpty())
                currentUser.setPassword(passwordField.getText().trim());

            new userservice().updateUser(currentUser);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur mis à jour avec succès !");
            if (adminDashboard != null) adminDashboard.refreshTable();
            goBackToDashboard();

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
        if (v.isEmpty()) { showError(nameError, "❌ Le nom est requis"); return false; }
        if (!NAME_PATTERN.matcher(v).matches()) {
            showError(nameError, "❌ Nom invalide (lettres uniquement, 2-50 caractères)"); return false;
        }
        return true;
    }

    private boolean validatePhone() {
        String v = phoneField.getText().trim();
        if (v.isEmpty()) { showError(phoneError, "❌ Le téléphone est requis"); return false; }
        if (!PHONE_PATTERN.matcher(v).matches()) {
            showError(phoneError, "❌ Numéro invalide (8 chiffres requis)"); return false;
        }
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

    private boolean validateRole() {
        if (roleCombo.getValue() == null) {
            showError(roleError, "❌ Veuillez sélectionner un rôle"); return false;
        }
        return true;
    }

    // ───────────────────────────────────────────────
    // UTILITAIRES
    // ───────────────────────────────────────────────
    private void showError(Label lbl, String msg) { lbl.setText(msg); lbl.setVisible(true); }

    private void hideAllErrors() {
        for (Label l : new Label[]{nameError, phoneError, passwordError, confirmPasswordError, roleError})
            if (l != null) l.setVisible(false);
    }

    @FXML private void handleCancel() { goBackToDashboard(); }

    private void goBackToDashboard() {
        try {
            Parent root = nameField.getScene().getRoot();
            if (root instanceof BorderPane) {
                StackPane contentArea = (StackPane) ((BorderPane) root).getCenter();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboard.fxml"));
                Parent content = loader.load();
                contentArea.getChildren().clear();
                contentArea.getChildren().add(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner au tableau de bord.");
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