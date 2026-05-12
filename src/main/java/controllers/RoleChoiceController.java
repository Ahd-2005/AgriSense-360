package controllers;

import entity.user;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import services.SessionManager;
import services.userservice;

import java.sql.SQLException;

public class RoleChoiceController {

    @FXML private Label userNameLabel;
    @FXML private Label errorLabel;

    // Données passées depuis login.java
    private String googleEmail;
    private String googleName;

    // Appelé depuis login.java après navigation vers cette page
    public void initData(String email, String name) {
        this.googleEmail = email;
        this.googleName  = name;
        userNameLabel.setText("👋 " + name);
    }

    @FXML
    private void chooseOwner() {
        createAndLogin(user.Role.ROLE_OWNER);
    }

    @FXML
    private void chooseGerant() {
        createAndLogin(user.Role.ROLE_GERANT);
    }

    @FXML
    private void chooseOuvrier() {
        createAndLogin(user.Role.ROLE_OUVRIER);
    }

    private void createAndLogin(user.Role role) {
        try {
            userservice service = new userservice();

            // Créer le nouvel utilisateur
            user newUser = new user();
            newUser.setName(googleName);
            newUser.setEmail(googleEmail);
            newUser.setPassword("GOOGLE_" + System.currentTimeMillis());
            newUser.setPhone("00000000");
            newUser.setRole(role);
            
            // Owner is active immediately, others are pending
            if (role == user.Role.ROLE_OWNER) {
                newUser.setStatus("active");
            } else {
                newUser.setStatus("pending");
            }

            service.ajouter(newUser);

            // Récupérer l'user avec son vrai ID en DB
            user created = service.findByEmail(googleEmail);

            if (created == null) {
                showError("❌ Erreur lors de la création du compte");
                return;
            }

            // Créer la session
            SessionManager.getInstance().createSession(created);

            // Navigation based on role
            if (role == user.Role.ROLE_OWNER) {
                goToMainLayout(created);
            } else {
                goToFarmList(created);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("❌ Erreur: " + e.getMessage());
        }
    }

    private void goToMainLayout(user created) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
        Parent root = loader.load();

        MainLayoutController mainController = loader.getController();
        mainController.configureForUserRole(created.getRole());
        mainController.setCurrentUser(created);

        Stage stage = (Stage) userNameLabel.getScene().getWindow();
        Scene scene = new Scene(root, 1400, 800);
        stage.setScene(scene);
        stage.setTitle("AgriSense 360 - Dashboard Propriétaire");
        stage.centerOnScreen();
        stage.show();
    }

    private void goToFarmList(user created) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FarmList.fxml"));
        Parent root = loader.load();

        FarmListController controller = loader.getController();
        controller.initData(created);

        Stage stage = (Stage) userNameLabel.getScene().getWindow();
        Scene scene = new Scene(root, 1400, 800);
        stage.setScene(scene);
        stage.setTitle("AgriSense 360 - Choisir une ferme");
        stage.centerOnScreen();
        stage.show();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
