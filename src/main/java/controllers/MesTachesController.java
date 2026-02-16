package controllers;

import entity.user;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import services.SessionManager;

public class MesTachesController {

    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        loadUserInfo();
    }

    private void loadUserInfo() {
        SessionManager sessionManager = SessionManager.getInstance();

        if (sessionManager.isLoggedIn()) {
            user currentUser = sessionManager.getCurrentUser();
            welcomeLabel.setText("Bienvenue, " + currentUser.getName() + " 👋");
        }
    }
}