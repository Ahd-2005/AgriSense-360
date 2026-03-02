package org.example.smartfarm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.DbInit;
import utils.MyConnection;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        MyConnection.getInstance().getCnx();
        DbInit.initTables();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
        Parent root = loader.load();
        root.getStylesheets().addAll(
            getClass().getResource("/css/sidebar.css").toExternalForm(),
            getClass().getResource("/css/home.css").toExternalForm(),
            getClass().getResource("/css/cards.css").toExternalForm()
        );
        primaryStage.setTitle("SmartFarm - Gestion agricole");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
