package controllers;

import entity.AnimalTypeOption;
import entity.LocationOption;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import services.ServiceAnimalTypeOption;
import services.ServiceLocationOption;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class OptionsController implements Initializable {

    @FXML private TextField locationNameField;
    @FXML private TableView<LocationOption> locationTable;
    @FXML private TableColumn<LocationOption, Integer> colLocationId;
    @FXML private TableColumn<LocationOption, String> colLocationName;
    @FXML private Button deleteLocationBtn;

    @FXML private TextField typeNameField;
    @FXML private TableView<AnimalTypeOption> typeTable;
    @FXML private TableColumn<AnimalTypeOption, Integer> colTypeId;
    @FXML private TableColumn<AnimalTypeOption, String> colTypeName;
    @FXML private Button deleteTypeBtn;

    private final ServiceLocationOption serviceLocationOption = new ServiceLocationOption();
    private final ServiceAnimalTypeOption serviceAnimalTypeOption = new ServiceAnimalTypeOption();
    private final ObservableList<LocationOption> locationList = FXCollections.observableArrayList();
    private final ObservableList<AnimalTypeOption> typeList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colLocationId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colLocationName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName() != null ? c.getValue().getName() : ""));
        locationTable.setItems(locationList);
        locationTable.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> deleteLocationBtn.setDisable(nv == null));

        colTypeId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colTypeName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName() != null ? c.getValue().getName() : ""));
        typeTable.setItems(typeList);
        typeTable.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> deleteTypeBtn.setDisable(nv == null));

        refreshAll();
    }

    private void refreshAll() {
        try {
            locationList.setAll(serviceLocationOption.getAll());
            typeList.setAll(serviceAnimalTypeOption.getAll());
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Could not load options: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onAddLocation() {
        String name = locationNameField.getText();
        if (name == null || name.trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Enter a location name.").showAndWait();
            return;
        }
        try {
            serviceLocationOption.add(new LocationOption(name.trim()));
            locationNameField.clear();
            refreshAll();
            new Alert(Alert.AlertType.INFORMATION, "Location added.").showAndWait();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                new Alert(Alert.AlertType.WARNING, "A location with this name already exists.").showAndWait();
            } else {
                new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
            }
        }
    }

    @FXML
    private void onDeleteLocation() {
        LocationOption sel = locationTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            if (serviceLocationOption.isUsedByAnimal(sel.getName())) {
                new Alert(Alert.AlertType.WARNING, "Cannot delete: at least one animal uses this location. Reassign or remove them first.").showAndWait();
                return;
            }
            serviceLocationOption.delete(sel.getId());
            refreshAll();
            deleteLocationBtn.setDisable(true);
            new Alert(Alert.AlertType.INFORMATION, "Location deleted.").showAndWait();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onAddType() {
        String name = typeNameField.getText();
        if (name == null || name.trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Enter an animal type name.").showAndWait();
            return;
        }
        try {
            serviceAnimalTypeOption.add(new AnimalTypeOption(name.trim()));
            typeNameField.clear();
            refreshAll();
            new Alert(Alert.AlertType.INFORMATION, "Animal type added.").showAndWait();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                new Alert(Alert.AlertType.WARNING, "This animal type already exists.").showAndWait();
            } else {
                new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
            }
        }
    }

    @FXML
    private void onDeleteType() {
        AnimalTypeOption sel = typeTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            if (serviceAnimalTypeOption.isUsedByAnimal(sel.getName())) {
                new Alert(Alert.AlertType.WARNING, "Cannot delete: at least one animal uses this type. Change their type or remove them first.").showAndWait();
                return;
            }
            serviceAnimalTypeOption.delete(sel.getId());
            refreshAll();
            deleteTypeBtn.setDisable(true);
            new Alert(Alert.AlertType.INFORMATION, "Animal type deleted.").showAndWait();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
        }
    }
}
