package com.example.agrisens360;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import services.SessionManager;
import controllers.MainLayoutController;
import entity.user;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        try {
            utils.MyDataBase.getInstance();

            SessionManager sessionManager = SessionManager.getInstance();

            if (sessionManager.loadSavedSession()) {
                user currentUser = sessionManager.getCurrentUser();
                System.out.println("✅ Session restaurée pour: " + currentUser.getName()
                        + " (" + currentUser.getRole() + ")");

                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(HelloApplication.class.getResource("/fxml/MainLayout.fxml"));
                Parent root = loader.load();

                MainLayoutController controller = loader.getController();
                controller.setCurrentUser(currentUser);

                Scene scene = new Scene(root, 1400, 800);
                stage.setScene(scene);
                stage.setTitle("AgriSense 360 - Dashboard");

            } else {
                System.out.println("No active session. Loading landing page...");

                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(HelloApplication.class.getResource("/fxml/LandingPage.fxml"));
                Parent root = loader.load();

                Scene scene = new Scene(root, 1400, 800);
                stage.setScene(scene);
                stage.setTitle("AgriSense 360 - Smart Farm Management");
            }

            stage.centerOnScreen();
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Erreur démarrage: " + e.getMessage());

            try {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(HelloApplication.class.getResource("/fxml/LandingPage.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 1400, 800);
                stage.setScene(scene);
                stage.setTitle("AgriSense 360");
                stage.centerOnScreen();
                stage.show();
            } catch (Exception fallback) {
                fallback.printStackTrace();
                throw fallback;
            }
        }
    }

    @Override
    public void stop() {
        try {
            SessionManager sessionManager = SessionManager.getInstance();
            if (sessionManager.isLoggedIn()) {
                sessionManager.saveCloseTimestamp();
            }
            sessionManager.cleanupExpiredSessions();
            System.out.println("Application fermée. Close timestamp saved.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}