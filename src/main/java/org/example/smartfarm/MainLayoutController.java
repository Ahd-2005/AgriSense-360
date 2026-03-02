package org.example.smartfarm;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainLayoutController {

    @FXML private StackPane contentArea;
    @FXML private Button navHome;
    @FXML private Button navAffectations;
    @FXML private Button navEvaluations;

    @FXML
    public void initialize() {
        loadView("/fxml/home_content.fxml");
        setActiveNav(navHome);
    }

    @FXML
    private void onHome() {
        loadView("/fxml/home_content.fxml");
        setActiveNav(navHome);
    }

    @FXML
    private void onAffectations() {
        loadView("/fxml/affectation_view.fxml");
        setActiveNav(navAffectations);
    }

    @FXML
    private void onEvaluations() {
        loadView("/fxml/evaluation_view.fxml");
        setActiveNav(navEvaluations);
    }

    void showAffectations() {
        onAffectations();
    }

    void showEvaluations() {
        onEvaluations();
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);
            if (fxmlPath.contains("home_content") && loader.getController() instanceof HomeController hc) {
                hc.setMainLayout(this);
            }
        } catch (IOException e) {
            System.err.println("Error loading view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setActiveNav(Button active) {
        navHome.getStyleClass().removeAll("active");
        navAffectations.getStyleClass().removeAll("active");
        navEvaluations.getStyleClass().removeAll("active");
        if (active != null) {
            active.getStyleClass().add("active");
        }
    }
}
