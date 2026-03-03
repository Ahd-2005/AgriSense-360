package controllers;

import entity.user;
import entity.user.Role;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import services.SessionManager;

public class GerantHome {

    @FXML private Label welcomeLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label userEmailLabel;




    @FXML
    public void initialize() {
        loadUserData();
    }

    private void loadUserData() {
        SessionManager sessionManager = SessionManager.getInstance();

        if (sessionManager.isLoggedIn()) {
            user currentUser = sessionManager.getCurrentUser();

            if (currentUser != null) {
                welcomeLabel.setText("Bienvenue, " + currentUser.getName() + "!");
                userRoleLabel.setText("Rôle: " + getRoleFriendlyName(currentUser.getRole()));
                userEmailLabel.setText("Email: " + currentUser.getEmail());
            }
        } else {
            welcomeLabel.setText("Bienvenue!");
            userRoleLabel.setText("Rôle: Non connecté");
            userEmailLabel.setText("");
        }
    }

    private String getRoleFriendlyName(Role role) {
        switch (role) {
            case ROLE_ADMIN:
                return "Administrateur";
            case ROLE_GERANT:
                return "Gérant";
            case ROLE_OUVRIER:
                return "Ouvrier";
            default:
                return "Unknown";
        }
    }
}