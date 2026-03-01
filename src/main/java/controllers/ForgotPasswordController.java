package controllers;

import entity.user;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.EmailService;
import services.PasswordResetService;
import services.userservice;

public class ForgotPasswordController {

    // Step 1
    @FXML private VBox      step1Box;
    @FXML private TextField emailField;
    @FXML private Label     emailError;
    @FXML private Label     sendingLabel;

    // Step 2
    @FXML private VBox          step2Box;
    @FXML private TextField     codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         codeError;
    @FXML private Label         passwordError;
    @FXML private Label         confirmError;
    @FXML private Label         successLabel;

    // Stocké entre les deux étapes
    private user foundUser;
    private String sentCode;

    // ============================================================
    // ÉTAPE 1 — Envoyer le code
    // ============================================================
    @FXML
    private void handleSendCode() {
        emailError.setVisible(false);
        sendingLabel.setVisible(false);

        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError(emailError, "❌ Veuillez entrer votre email");
            return;
        }

        sendingLabel.setVisible(true);

        new Thread(() -> {
            try {
                userservice service = new userservice();
                user u = service.findByEmail(email);

                Platform.runLater(() -> {
                    sendingLabel.setVisible(false);

                    if (u == null) {
                        showError(emailError, "❌ Aucun compte trouvé avec cet email");
                        return;
                    }

                    if ("BLOCKED".equals(u.getStatus())) {
                        showError(emailError, "❌ Ce compte est bloqué. Contactez l'administrateur.");
                        return;
                    }

                    foundUser = u;

                    // Générer et envoyer le code dans un thread
                    new Thread(() -> {
                        try {
                            String code = PasswordResetService.generateAndSaveCode(foundUser.getId());
                            sentCode = code;
                            EmailService.sendResetCode(foundUser.getEmail(), foundUser.getName(), code);

                            Platform.runLater(() -> {
                                // Passer à l'étape 2
                                step1Box.setVisible(false);
                                step1Box.setManaged(false);
                                step2Box.setVisible(true);
                                step2Box.setManaged(true);
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                            Platform.runLater(() ->
                                showError(emailError, "❌ Erreur envoi email: " + e.getMessage())
                            );
                        }
                    }).start();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                    showError(emailError, "❌ Erreur base de données: " + e.getMessage())
                );
            }
        }).start();
    }

    // ============================================================
    // ÉTAPE 2 — Vérifier le code et reset le mot de passe
    // ============================================================
    @FXML
    private void handleResetPassword() {
        codeError.setVisible(false);
        passwordError.setVisible(false);
        confirmError.setVisible(false);
        successLabel.setVisible(false);

        String code       = codeField.getText().trim();
        String newPass    = newPasswordField.getText().trim();
        String confirmPass= confirmPasswordField.getText().trim();

        // Validation code
        if (code.isEmpty() || code.length() != 6) {
            showError(codeError, "❌ Entrez le code à 6 chiffres reçu par email");
            return;
        }

        // Validation mot de passe
        if (newPass.length() < 8) {
            showError(passwordError, "❌ Minimum 8 caractères");
            return;
        }

        boolean hasUpper   = newPass.matches(".*[A-Z].*");
        boolean hasLower   = newPass.matches(".*[a-z].*");
        boolean hasDigit   = newPass.matches(".*[0-9].*");
        boolean hasSpecial = newPass.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            showError(passwordError, "❌ Mot de passe faible (majuscule, minuscule, chiffre, caractère spécial)");
            return;
        }

        // Confirmation
        if (!newPass.equals(confirmPass)) {
            showError(confirmError, "❌ Les mots de passe ne correspondent pas");
            return;
        }

        // Vérifier le code en DB
        new Thread(() -> {
            try {
                boolean valid = PasswordResetService.verifyCode(foundUser.getId(), code);

                Platform.runLater(() -> {
                    if (!valid) {
                        showError(codeError, "❌ Code incorrect ou expiré (15 min max)");
                        return;
                    }

                    // Mettre à jour le mot de passe
                    try {
                        userservice service = new userservice();
                        foundUser.setPassword(newPass);
                        service.updateUser(foundUser);

                        // Marquer le code comme utilisé
                        PasswordResetService.markCodeAsUsed(foundUser.getId(), code);

                        // Succès
                        successLabel.setText("✅ Mot de passe réinitialisé ! Redirection...");
                        successLabel.setVisible(true);

                        // Rediriger vers login après 2 secondes
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                Platform.runLater(this::goToLogin);
                            } catch (InterruptedException ignored) {}
                        }).start();

                    } catch (Exception e) {
                        e.printStackTrace();
                        showError(codeError, "❌ Erreur mise à jour: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                    showError(codeError, "❌ Erreur vérification: " + e.getMessage())
                );
            }
        }).start();
    }

    // ============================================================
    // NAVIGATION
    // ============================================================
    @FXML
    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Login - AgriSense 360");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
    }
}
