package controllers;

import entity.user;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class PendingWaitingController {

    @FXML private Label nameLabel;
    @FXML private Label messageLabel;

    private user currentUser;

    public void initData(user user) {
        this.currentUser = user;
        nameLabel.setText(user.getName());
    }

    @FXML
    private void handleLogout() {
        try {
            services.SessionManager.getInstance().logout();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) nameLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Login - AgriSense 360");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBrowseMoreFarms() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FarmList.fxml"));
            Parent root = loader.load();
            FarmListController controller = loader.getController();
            controller.initData(currentUser);
            Stage stage = (Stage) nameLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Choisir une ferme - AgriSense 360");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
