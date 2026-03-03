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
import services.userservice;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class UserAdd {

    @FXML private StackPane avatarPane;
    @FXML private TextField profilePictureField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<user.Role> roleCombo;

    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label roleError;

    private AdminDashboard adminDashboard;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{8}$");
    private static final Pattern NAME_PATTERN  = Pattern.compile("^[A-Za-zÀ-ÿ\\s]{2,50}$");

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList(user.Role.values()));
        roleCombo.setConverter(new javafx.util.StringConverter<user.Role>() {
            @Override public String toString(user.Role r) {
                if (r == null) return "";
                switch (r) {
                    case ROLE_ADMIN:   return "Administrateur";
                    case ROLE_GERANT:  return "Gérant";
                    case ROLE_OUVRIER: return "Ouvrier";
                    default: return r.name();
                }
            }
            @Override public user.Role fromString(String s) { return null; }
        });

        // Mettre à jour l'aperçu en live quand l'URL change
        profilePictureField.textProperty().addListener((obs, old, nv) -> refreshAvatarPreview(nv));

        // Mettre à jour l'avatar quand le nom change (pour les initiales)
        nameField.textProperty().addListener((obs, old, nv) -> {
            if (profilePictureField.getText().isBlank()) refreshAvatarPreview(null);
        });

        hideAllErrors();
        refreshAvatarPreview(null); // affiche un placeholder initial
    }

    public void setAdminDashboard(AdminDashboard dashboard) {
        this.adminDashboard = dashboard;
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
        // Initiales ou icône neutre
        String name = nameField != null ? nameField.getText() : "";
        Label lbl = new Label(name.isBlank() ? "👤" : getInitials(name));
        lbl.setStyle("-fx-font-size: " + (name.isBlank() ? "26px" : "22px") +
                "; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane bg = new StackPane(lbl);
        bg.setMinSize(64, 64); bg.setMaxSize(64, 64);
        String color = name.isBlank() ? "#888" : getAvatarColor(name);
        bg.setStyle("-fx-background-color:" + color + "; -fx-background-radius:32;");
        avatarPane.getChildren().add(bg);
    }

    @FXML
    private void handleGenerateAvatar() {
        String name = nameField.getText().trim();
        String seed = (name.isEmpty() ? "user" : name.toLowerCase().replace(" ", "-"))
                + "-" + System.currentTimeMillis() % 9999;
        String url = "https://api.dicebear.com/7.x/avataaars/png?seed=" + seed + "&size=128";
        profilePictureField.setText(url);
        // Le listener met à jour l'aperçu automatiquement
    }

    @FXML
    private void handlePreviewPicture() {
        refreshAvatarPreview(profilePictureField.getText());
    }

    // ───────────────────────────────────────────────
    // AJOUT
    // ───────────────────────────────────────────────
    @FXML
    private void handleAddUser() {
        hideAllErrors();
        boolean valid = true;

        if (!validateName())            valid = false;
        if (!validateEmail())           valid = false;
        if (!validatePhone())           valid = false;
        if (!validatePassword())        valid = false;
        if (!validateConfirmPassword()) valid = false;
        if (!validateRole())            valid = false;

        if (!valid) return;

        try {
            userservice service = new userservice();

            if (service.findByEmail(emailField.getText().trim()) != null) {
                showError(emailError, "❌ Cet email existe déjà !");
                return;
            }

            user newUser = new user();
            newUser.setName(nameField.getText().trim());
            newUser.setEmail(emailField.getText().trim().toLowerCase());
            newUser.setPhone(phoneField.getText().trim());
            newUser.setPassword(passwordField.getText().trim());
            newUser.setRole(roleCombo.getValue());
            newUser.setStatus("ACTIVE");

            String pic = profilePictureField.getText().trim();
            // Si aucune URL fournie, générer automatiquement un avatar DiceBear
            if (pic.isEmpty()) {
                String seed = newUser.getName().toLowerCase().replace(" ", "-");
                pic = "https://api.dicebear.com/7.x/avataaars/png?seed=" + seed + "&size=128";
            }
            newUser.setProfilePicture(pic);

            service.ajouter(newUser);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Utilisateur créé avec succès !");
            if (adminDashboard != null) adminDashboard.refreshTable();
            goBackToDashboard();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec de la création : " + e.getMessage());
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

    private boolean validateEmail() {
        String v = emailField.getText().trim();
        if (v.isEmpty())                         { showError(emailError, "❌ L'email est requis"); return false; }
        if (!EMAIL_PATTERN.matcher(v).matches()) { showError(emailError, "❌ Format invalide (ex: utilisateur@exemple.com)"); return false; }
        return true;
    }

    private boolean validatePhone() {
        String v = phoneField.getText().trim();
        if (v.isEmpty())                          { showError(phoneError, "❌ Le téléphone est requis"); return false; }
        if (!PHONE_PATTERN.matcher(v).matches())  { showError(phoneError, "❌ Numéro invalide (8 chiffres requis)"); return false; }
        return true;
    }

    private boolean validatePassword() {
        String p = passwordField.getText().trim();
        if (p.isEmpty()) { showError(passwordError, "❌ Le mot de passe est requis"); return false; }
        if (p.length() < 8) { showError(passwordError, "❌ Minimum 8 caractères"); return false; }
        if (!p.matches(".*[A-Z].*") || !p.matches(".*[a-z].*") ||
                !p.matches(".*[0-9].*") || !p.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            showError(passwordError, "❌ Mot de passe faible (maj, min, chiffre, caractère spécial)");
            return false;
        }
        return true;
    }

    private boolean validateConfirmPassword() {
        if (confirmPasswordField.getText().trim().isEmpty()) {
            showError(confirmPasswordError, "❌ Veuillez confirmer le mot de passe"); return false;
        }
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
        for (Label l : new Label[]{nameError, emailError, phoneError,
                passwordError, confirmPasswordError, roleError})
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
                AdminDashboard ctrl = loader.getController();
                ctrl.refreshTable();
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