package org.example.smartfarm;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {

    @FXML private Button btnAffectations;
    @FXML private Button btnEvaluations;

    @FXML
    private void onAffectations() {
        loadView("/fxml/affectation_view.fxml", "Affectations");
    }

    @FXML
    private void onEvaluations() {
        loadView("/fxml/evaluation_view.fxml", "Évaluations");
    }

    private void loadView(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 820, 620));
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading view: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
