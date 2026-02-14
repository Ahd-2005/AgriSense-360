package controllers;

import javafx.fxml.FXML;

public class HomeContentController {

    @FXML
    private void navigateToAnimals() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) {
            controller.navigateToAnimals();
        }
    }

    @FXML
    private void navigateToEquipment() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) {
            controller.navigateToEquipment();
        }
    }

    @FXML
    private void navigateToStock() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) {
            controller.navigateToStock();
        }
    }

    @FXML
    private void navigateToCulture() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) {
            controller.navigateToCulture();
        }
    }

    @FXML
    private void navigateToUsers() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) {
            controller.navigateToUsers();
        }
    }

    @FXML
    private void navigateToWorkers() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) {
            controller.navigateToWorkers();
        }
    }
}