package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import services.ServiceEnumManagement;
import utils.AnimalListRefresh;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class EnumManagementController implements Initializable {

    @FXML private ListView<String> typesList;
    @FXML private ListView<String> locationsList;
    @FXML private TextField newTypeField;
    @FXML private TextField newLocationField;
    @FXML private Button deleteTypeBtn;
    @FXML private Button deleteLocationBtn;

    private final ServiceEnumManagement serviceEnum = new ServiceEnumManagement();
    private final ObservableList<String> typesItems = FXCollections.observableArrayList();
    private final ObservableList<String> locationsItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typesList.setItems(typesItems);
        locationsList.setItems(locationsItems);
        typesList.getSelectionModel().selectedItemProperty().addListener((o, old, val) -> deleteTypeBtn.setDisable(val == null));
        locationsList.getSelectionModel().selectedItemProperty().addListener((o, old, val) -> deleteLocationBtn.setDisable(val == null));
        refreshLists();
    }

    private void refreshLists() {
        try {
            List<String> types = serviceEnum.getEnumValues("Animal", "type");
            typesItems.clear();
            typesItems.addAll(types);
            List<String> locs = serviceEnum.getEnumValues("Animal", "location");
            locationsItems.clear();
            locationsItems.addAll(locs);
        } catch (SQLException e) {
            showError("Could not load lists: " + e.getMessage());
        }
    }

    @FXML
    private void onAddType() {
        String v = newTypeField.getText();
        if (v == null || v.trim().isEmpty()) {
            showError("Enter a type name.");
            return;
        }
        try {
            serviceEnum.addEnumValue("Animal", "type", v.trim());
            newTypeField.clear();
            refreshLists();
            AnimalListRefresh.notifyAnimalChanged();
            showInfo("Type added. Animals tab combos updated.");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onDeleteType() {
        String selected = typesList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            serviceEnum.removeEnumValue("Animal", "type", selected);
            refreshLists();
            AnimalListRefresh.notifyAnimalChanged();
            showInfo("Type removed.");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onAddLocation() {
        String v = newLocationField.getText();
        if (v == null || v.trim().isEmpty()) {
            showError("Enter a location name.");
            return;
        }
        try {
            serviceEnum.addEnumValue("Animal", "location", v.trim());
            newLocationField.clear();
            refreshLists();
            AnimalListRefresh.notifyAnimalChanged();
            showInfo("Location added. Animals tab combos updated.");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onDeleteLocation() {
        String selected = locationsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            serviceEnum.removeEnumValue("Animal", "location", selected);
            refreshLists();
            AnimalListRefresh.notifyAnimalChanged();
            showInfo("Location removed.");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
