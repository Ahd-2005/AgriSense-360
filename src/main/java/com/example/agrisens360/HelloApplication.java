package com.example.agrisens360;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Load the main layout with sidebar
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainLayout.fxml"));

        Scene scene = new Scene(root, 1400, 800);

        stage.setTitle("AgriSense 360 - Smart Farm Management");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }


    public static void main(String[] args) {
        // Connect to database on startup
        utils.MyDataBase.getInstance();
        launch();
    }
}