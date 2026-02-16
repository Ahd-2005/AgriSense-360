package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charger main-layout.fxml comme scène principale (contient sidebar et header fixes)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
        Parent root = loader.load();

        // Créer la scène
        Scene scene = new Scene(root, 1530, 800);

        // Configurer la fenêtre
        primaryStage.setTitle("AgriSense 360");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
/*package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Charger le FXML du backoffice
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BackOfficeStock.fxml"));
            Parent root = loader.load();

            // Créer la scène
            Scene scene = new Scene(root, 1200, 800);  // Taille ajustable

            // Configurer la fenêtre
            primaryStage.setTitle("Test Backoffice - Gestion Produits et Stocks");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement du backoffice : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}*/