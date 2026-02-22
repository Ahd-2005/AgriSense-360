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
            // Connexion à la base de données au démarrage
            utils.MyDataBase.getInstance();

            SessionManager sessionManager = SessionManager.getInstance();

            if (sessionManager.loadSavedSession()) {
                // ✅ Session valide trouvée → Dashboard directement
                user currentUser = sessionManager.getCurrentUser();
                System.out.println("✅ Session restaurée pour: " + currentUser.getName()
                        + " (" + currentUser.getRole() + ")");

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
                Parent root = loader.load();

                // NE PAS appeler configureForUserRole ici —
                // initialize() dans MainLayoutController le fait déjà via SessionManager
                // On appelle juste setCurrentUser pour mettre à jour l'affichage nom/rôle
                MainLayoutController controller = loader.getController();
                controller.setCurrentUser(currentUser);

                Scene scene = new Scene(root, 1400, 800);
                stage.setScene(scene);
                stage.setTitle("AgriSense 360 - Dashboard");

            } else {
                // ❌ Pas de session → Landing page
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
            System.err.println("❌ Erreur démarrage: " + e.getMessage());

            // Fallback → landing page
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/fxml/Landingpage.fxml"));
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
            SessionManager.getInstance().cleanupExpiredSessions();
            System.out.println("Application fermée. Sessions nettoyées.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}