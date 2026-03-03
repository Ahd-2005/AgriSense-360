package com.example.agrisense360.controllers;

import javafx.fxml.FXML;

public class EquipmentManagementController {

    @FXML
    private void openWeather() {
        MainController controller = MainController.getInstance();
        if (controller != null) {
            controller.showWeather();
        }
    }

    @FXML
    private void openCameras() {
        MainController controller = MainController.getInstance();
        if (controller != null) {
            controller.showCameras();
        }
    }

    @FXML
    private void openAiDashboard() {
        MainController controller = MainController.getInstance();
        if (controller != null) {
            controller.showAiDashboard();
        }
    }

    @FXML
    private void openEquipmentStore() {
        MainController controller = MainController.getInstance();
        if (controller != null) {
            controller.showEquipmentStore();
        }
    }
}
