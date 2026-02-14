package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainController {

    @FXML private StackPane contentStack;

    private Node animalsView;

    @FXML
    private void initialize() {

    }

    @FXML
    private void navigateAnimals() {
        try {
            if (animalsView == null) {
                animalsView = FXMLLoader.load(getClass().getResource("/animals-management-view.fxml"));
            }
            if (!contentStack.getChildren().contains(animalsView)) {
                contentStack.getChildren().add(animalsView);
            }
            contentStack.getChildren().get(0).setVisible(false);
            animalsView.setVisible(true);
        } catch (IOException e) {
            throw new RuntimeException("Could not load Animals Management view", e);
        }
    }

    @FXML
    private void navigateEquipment() {
        showHome();
    }

    @FXML
    private void navigateStock() {
        showHome();
    }

    @FXML
    private void navigateCulture() {
        showHome();
    }

    @FXML
    private void navigateUsers() {
        showHome();
    }

    @FXML
    private void navigateWorkers() {
        showHome();
    }

    private void showHome() {
        if (contentStack.getChildren().isEmpty()) return;
        contentStack.getChildren().get(0).setVisible(true);
        if (contentStack.getChildren().size() > 1) {
            contentStack.getChildren().get(1).setVisible(false);
        }
    }

    @FXML
    private void toggleSidebar() {

    }
}
