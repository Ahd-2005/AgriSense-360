package com.example.agrisens360;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import services.SessionManager;
import controllers.MainLayoutController;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        try {
            // Connect to database on startup
            utils.MyDataBase.getInstance();

            SessionManager sessionManager = SessionManager.getInstance();

            // Check if there's a saved session
            if (sessionManager.loadSavedSession()) {
                // User has active session - go directly to main layout
                System.out.println("Active session found. Loading dashboard...");

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
                Parent root = loader.load();

                // Configure sidebar based on user role
                MainLayoutController controller = loader.getController();
                controller.configureForUserRole(sessionManager.getCurrentUser().getRole());
                controller.setCurrentUser(sessionManager.getCurrentUser());

                Scene scene = new Scene(root, 1400, 800);
                stage.setScene(scene);
                stage.setTitle("AgriSense 360 - Dashboard");

            } else {
                // No active session - show landing page
                System.out.println("No active session. Loading landing page...");

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Landingpage.fxml"));
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
            System.err.println("Error starting application: " + e.getMessage());

            // Fallback: Load landing page if anything fails
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/fxml/Landingpage.fxml"));
                Scene scene = new Scene(root, 1400, 800);
                stage.setScene(scene);
                stage.setTitle("AgriSense 360");
                stage.centerOnScreen();
                stage.show();
            } catch (Exception fallbackError) {
                fallbackError.printStackTrace();
                throw fallbackError;
            }
        }
    }

    @Override
    public void stop() {
        // Clean up expired sessions on app close
        try {
            SessionManager.getInstance().cleanupExpiredSessions();
            System.out.println("Application closed. Sessions cleaned up.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}