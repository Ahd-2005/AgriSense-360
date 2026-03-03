package controllers;

import javafx.fxml.FXML;

public class EquipmentManagementController {

    @FXML
    private void openWeather() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) {
            controller.navigateToWeather();
        }
    }
}
