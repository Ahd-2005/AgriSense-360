package com.example.agrisense360.controllers;

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
    private Node cameraView;
    private Node equipmentStoreView;
    private WeatherController weatherController;

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
            if (weatherView != null) {
                weatherView.setVisible(false);
            }
            if (cameraView != null) {
                cameraView.setVisible(false);
            }
            if (equipmentStoreView != null) {
                equipmentStoreView.setVisible(false);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load Equipment Management view", e);
        }
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

    public void showWeather() {
        try {
            if (weatherView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/weather-view.fxml"));
                weatherView = loader.load();
                weatherController = loader.getController();
            }
            if (!contentStack.getChildren().contains(weatherView)) {
                contentStack.getChildren().add(weatherView);
            }
            contentStack.getChildren().get(0).setVisible(false);
            weatherView.setVisible(true);
            if (weatherController != null) {
                weatherController.setAiDashboardMode(false);
            }
            if (animalsView != null) {
                animalsView.setVisible(false);
            }
            if (equipmentView != null) {
                equipmentView.setVisible(false);
            }
            if (cameraView != null) {
                cameraView.setVisible(false);
            }
            if (equipmentStoreView != null) {
                equipmentStoreView.setVisible(false);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load Weather view", e);
        }
    }

    public void showAiDashboard() {
        showWeather();
        if (weatherController != null) {
            weatherController.setAiDashboardMode(true);
        }
    }

    public void showCameras() {
        try {
            if (cameraView == null) {
                cameraView = FXMLLoader.load(getClass().getResource("/fxml/camera-management-view.fxml"));
            }
            if (!contentStack.getChildren().contains(cameraView)) {
                contentStack.getChildren().add(cameraView);
            }
            contentStack.getChildren().get(0).setVisible(false);
            cameraView.setVisible(true);
            if (animalsView != null) {
                animalsView.setVisible(false);
            }
            if (equipmentView != null) {
                equipmentView.setVisible(false);
            }
            if (weatherView != null) {
                weatherView.setVisible(false);
            }
            if (equipmentStoreView != null) {
                equipmentStoreView.setVisible(false);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load Camera Management view", e);
        }
    }

    public void showEquipmentStore() {
        try {
            if (equipmentStoreView == null) {
                equipmentStoreView = FXMLLoader.load(getClass().getResource("/fxml/equipment-store-view.fxml"));
            }
            if (!contentStack.getChildren().contains(equipmentStoreView)) {
                contentStack.getChildren().add(equipmentStoreView);
            }
            contentStack.getChildren().get(0).setVisible(false);
            equipmentStoreView.setVisible(true);
            if (animalsView != null) {
                animalsView.setVisible(false);
            }
            if (equipmentView != null) {
                equipmentView.setVisible(false);
            }
            if (weatherView != null) {
                weatherView.setVisible(false);
            }
            if (cameraView != null) {
                cameraView.setVisible(false);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load Equipment Store view", e);
        }
    }

    public void showEquipment() {
        navigateEquipment();
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
        if (weatherView != null) {
            weatherView.setVisible(false);
        }
        if (cameraView != null) {
            cameraView.setVisible(false);
        }
        if (equipmentStoreView != null) {
            equipmentStoreView.setVisible(false);
        }
    }

    @FXML
    private void toggleSidebar() {

    }

}
