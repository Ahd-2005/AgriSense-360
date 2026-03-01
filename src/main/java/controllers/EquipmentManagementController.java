package controllers;

import javafx.fxml.FXML;

public class EquipmentManagementController {

    @FXML
    private void openWeather() {
        MainController controller = MainController.getInstance();
        if (controller != null) {
            controller.showWeather();
        }
    }
}
