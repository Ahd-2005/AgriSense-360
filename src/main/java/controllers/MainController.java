package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainController {

    private static MainController instance;

    @FXML private StackPane contentStack;

    private Node animalsView;
    private Node equipmentView;
    private Node weatherView;

    @FXML
    private void initialize() {
        instance = this;
    }

    public static MainController getInstance() {
        return instance;
    }

    @FXML
    private void navigateHome() {
        showHome();
    }

    @FXML
    private void navigateAnimals() {
        try {
            if (animalsView == null) {
                animalsView = FXMLLoader.load(getClass().getResource("/fxml/animals-management-view.fxml"));
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
        try {
            if (equipmentView == null) {
                equipmentView = FXMLLoader.load(getClass().getResource("/fxml/equipment-management-view.fxml"));
            }
            if (!contentStack.getChildren().contains(equipmentView)) {
                contentStack.getChildren().add(equipmentView);
            }
            contentStack.getChildren().get(0).setVisible(false);
            equipmentView.setVisible(true);
            if (animalsView != null) {
                animalsView.setVisible(false);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load Equipment Management view", e);
        }
    }


    public void showWeather() {
        try {
            if (weatherView == null) {
                weatherView = FXMLLoader.load(getClass().getResource("/fxml/weather-view.fxml"));
            }
            if (!contentStack.getChildren().contains(weatherView)) {
                contentStack.getChildren().add(weatherView);
            }
            contentStack.getChildren().get(0).setVisible(false);
            weatherView.setVisible(true);
            if (animalsView != null) {
                animalsView.setVisible(false);
            }
            if (equipmentView != null) {
                equipmentView.setVisible(false);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load Weather view", e);
        }
    }

    private void showHome() {
        if (contentStack.getChildren().isEmpty()) return;
        contentStack.getChildren().get(0).setVisible(true);
        if (animalsView != null) {
            animalsView.setVisible(false);
        }
        if (equipmentView != null) {
            equipmentView.setVisible(false);
        }
    }

    @FXML
    private void toggleSidebar() {

    }

}
