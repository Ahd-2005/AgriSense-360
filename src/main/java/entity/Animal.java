package controllers;

import entity.AnimalTypeOption;
import entity.LocationOption;
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
    @FXML private TableColumn<LocationOption, String> colLocationName;
    @FXML private Button addLocationBtn;
    @FXML private Button deleteLocationBtn;

    @FXML private TextField typeNameField;
    @FXML private TableView<AnimalTypeOption> typeTable;
    @FXML private TableColumn<AnimalTypeOption, String> colTypeName;
    @FXML private Button addTypeBtn;
    @FXML private Button deleteTypeBtn;

    private final ServiceLocationOption serviceLocationOption = new ServiceLocationOption();
    private final ServiceAnimalTypeOption serviceAnimalTypeOption = new ServiceAnimalTypeOption();
    private final ObservableList<LocationOption> locationList = FXCollections.observableArrayList();
    private final ObservableList<AnimalTypeOption> typeList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup columns
        colLocationName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        locationTable.setItems(locationList);

        colTypeName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        typeTable.setItems(typeList);

        // Setup button actions
        addLocationBtn.setOnAction(e -> addLocation());
        deleteLocationBtn.setOnAction(e -> deleteLocation());
        addTypeBtn.setOnAction(e -> addType());
        deleteTypeBtn.setOnAction(e -> deleteType());

        refreshAll();
    }

    @FXML
    private void addLocation() {
        String name = locationNameField.getText();
        if (name == null || name.trim().isEmpty()) {
            showError("Please enter a location name");
            return;
        }

        try {
            serviceLocationOption.add(new LocationOption(name));
            locationNameField.clear();
            refreshLocations();
            showInfo("Location '" + name + "' added successfully");
        } catch (SQLException e) {
            showError("Error adding location: " + e.getMessage());
        }
    }

    @FXML
    private void deleteLocation() {
        LocationOption selected = locationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a location to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Location");
        confirm.setContentText("Are you sure you want to delete '" + selected.getName() + "'?");

        if (confirm.showAndWait().orElse(Alert.AlertType.CANCEL) == Alert.AlertType.OK) {
            try {
                serviceLocationOption.delete(selected.getName());
                refreshLocations();
                showInfo("Location '" + selected.getName() + "' deleted successfully");
            } catch (SQLException e) {
                showError("Error deleting location: " + e.getMessage());
            }
        }
    }

    @FXML
    private void addType() {
        String name = typeNameField.getText();
        if (name == null || name.trim().isEmpty()) {
            showError("Please enter an animal type name");
            return;
        }

        try {
            serviceAnimalTypeOption.add(new AnimalTypeOption(name));
            typeNameField.clear();
            refreshTypes();
            showInfo("Animal type '" + name + "' added successfully");
        } catch (SQLException e) {
            showError("Error adding animal type: " + e.getMessage());
        }
    }

    @FXML
    private void deleteType() {
        AnimalTypeOption selected = typeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select an animal type to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Animal Type");
        confirm.setContentText("Are you sure you want to delete '" + selected.getName() + "'?");

        if (confirm.showAndWait().orElse(Alert.AlertType.CANCEL) == Alert.AlertType.OK) {
            try {
                serviceAnimalTypeOption.delete(selected.getName());
                refreshTypes();
                showInfo("Animal type '" + selected.getName() + "' deleted successfully");
            } catch (SQLException e) {
                showError("Error deleting animal type: " + e.getMessage());
            }
        }
    }

    private void refreshAll() {
        refreshLocations();
        refreshTypes();
    }

    private void refreshLocations() {
        try {
            locationList.setAll(serviceLocationOption.getAll());
        } catch (SQLException e) {
            showError("Could not load locations: " + e.getMessage());
        }
    }

    private void refreshTypes() {
        try {
            typeList.setAll(serviceAnimalTypeOption.getAll());
        } catch (SQLException e) {
            showError("Could not load animal types: " + e.getMessage());
        }
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }
}

package services;

import entity.LocationOption;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceLocationOption {

    private final Connection connection = MyDataBase.getInstance().getCnx();

    /**
     * Reads available locations from the ENUM definition in the database.
     */
    public List<LocationOption> getAll() throws SQLException {
        List<LocationOption> list = new ArrayList<>();
        String sql = "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_NAME='Animal' AND COLUMN_NAME='location' AND TABLE_SCHEMA=DATABASE()";
        
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                String columnType = rs.getString("COLUMN_TYPE");
                String enumValues = columnType.substring(columnType.indexOf("(") + 1, columnType.lastIndexOf(")"));
                String[] locations = enumValues.split(",");
                for (String location : locations) {
                    list.add(new LocationOption(location.replace("'", "").trim()));
                }
            }
        }
        return list;
    }

    /**
     * Adds a new location to the ENUM
     */
    public void add(LocationOption opt) throws SQLException {
        if (opt.getName() == null || opt.getName().trim().isEmpty()) {
            throw new SQLException("Location name cannot be empty");
        }
        
        String locationName = opt.getName().trim().toLowerCase();
        
        // Check if location already exists
        List<LocationOption> existing = getAll();
        for (LocationOption e : existing) {
            if (e.getName().equalsIgnoreCase(locationName)) {
                throw new SQLException("Location '" + locationName + "' already exists");
            }
        }
        
        // Build new ENUM values
        StringBuilder enumValues = new StringBuilder("ENUM(");
        for (int i = 0; i < existing.size(); i++) {
            enumValues.append("'").append(existing.get(i).getName()).append("'");
            if (i < existing.size() - 1) enumValues.append(",");
        }
        enumValues.append(",'").append(locationName).append("')");
        
        // Execute ALTER TABLE
        String sql = "ALTER TABLE Animal MODIFY COLUMN location " + enumValues.toString();
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    /**
     * Removes a location from the ENUM
     */
    public void delete(String locationName) throws SQLException {
        if (locationName == null || locationName.trim().isEmpty()) {
            throw new SQLException("Location name cannot be empty");
        }
        
        // Check if location is used by any animals
        if (isUsedByAnimal(locationName)) {
            throw new SQLException("Cannot delete location '" + locationName + "'. It is currently assigned to one or more animals.");
        }
        
        // Get all existing locations
        List<LocationOption> existing = getAll();
        
        // Remove the location to delete
        existing.removeIf(e -> e.getName().equalsIgnoreCase(locationName));
        
        if (existing.isEmpty()) {
            throw new SQLException("Cannot delete the last location");
        }
        
        // Build new ENUM values
        StringBuilder enumValues = new StringBuilder("ENUM(");
        for (int i = 0; i < existing.size(); i++) {
            enumValues.append("'").append(existing.get(i).getName()).append("'");
            if (i < existing.size() - 1) enumValues.append(",");
        }
        enumValues.append(")");
        
        // Execute ALTER TABLE
        String sql = "ALTER TABLE Animal MODIFY COLUMN location " + enumValues.toString();
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    public boolean isUsedByAnimal(String locationName) throws SQLException {
        if (locationName == null) return false;
        String sql = "SELECT COUNT(*) as count FROM Animal WHERE location = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, locationName);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt("count") > 0;
    }
}
