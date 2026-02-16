package controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import entity.Equipment;
import entity.Maintenance;
import services.ServiceEquipment;
import services.ServiceMaintenance;
import utils.EquipmentListRefresh;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class MaintenanceController implements Initializable {

    @FXML private ComboBox<Equipment> equipmentCombo;
    @FXML private DatePicker maintenanceDatePicker;
    @FXML private TextField maintenanceTypeField;
    @FXML private TextField costField;
    @FXML private TextField maintenanceSearchField;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Label recordsTitleLabel;
    @FXML private TableView<Maintenance> maintenanceTable;
    @FXML private TableColumn<Maintenance, Integer> colId;
    @FXML private TableColumn<Maintenance, String> colEquipment;
    @FXML private TableColumn<Maintenance, LocalDate> colDate;
    @FXML private TableColumn<Maintenance, String> colType;
    @FXML private TableColumn<Maintenance, BigDecimal> colCost;

    private final ServiceEquipment serviceEquipment = new ServiceEquipment();
    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();
    private final ObservableList<Maintenance> maintenanceList = FXCollections.observableArrayList();
    private final FilteredList<Maintenance> filteredMaintenance = new FilteredList<>(maintenanceList, item -> true);
    private final Map<Integer, Equipment> equipmentMap = new HashMap<>();
    private Maintenance selectedMaintenance;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        maintenanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        applyLengthLimit(maintenanceTypeField, 80);
        applyLengthLimit(costField, 12);
        if (maintenanceSearchField != null) {
            applyLengthLimit(maintenanceSearchField, 80);
        }
        applyCostFormatter(costField);
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colEquipment.setCellValueFactory(c -> new SimpleStringProperty(resolveEquipmentName(c.getValue().getEquipmentId())));
        colDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getMaintenanceDate()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMaintenanceType()));
        colCost.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCost()));

        SortedList<Maintenance> sortedMaintenance = new SortedList<>(filteredMaintenance);
        sortedMaintenance.comparatorProperty().bind(maintenanceTable.comparatorProperty());
        maintenanceTable.setItems(sortedMaintenance);
        maintenanceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> onTableRowSelected());

        if (maintenanceSearchField != null) {
            maintenanceSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyMaintenanceFilter(newVal));
        }

        loadEquipment();
        EquipmentListRefresh.addListener(this::loadEquipment);
    }

    private String resolveEquipmentName(int equipmentId) {
        Equipment equipment = equipmentMap.get(equipmentId);
        if (equipment == null) return "#" + equipmentId;
        return "#" + equipment.getId() + " - " + equipment.getName();
    }

    private void loadEquipment() {
        equipmentCombo.getItems().clear();
        equipmentMap.clear();
        try {
            List<Equipment> equipment = serviceEquipment.getAll();
            equipmentCombo.getItems().addAll(equipment);
            for (Equipment e : equipment) {
                equipmentMap.put(e.getId(), e);
            }
        } catch (SQLException e) {
            showError("Could not load equipment: " + e.getMessage());
        }
    }

    @FXML
    private void onEquipmentSelected() {
        Equipment equipment = equipmentCombo.getSelectionModel().getSelectedItem();
        if (equipment == null) {
            maintenanceList.clear();
            if (recordsTitleLabel != null) recordsTitleLabel.setText("Maintenance Records for Selected Equipment");
            return;
        }
        if (recordsTitleLabel != null) {
            recordsTitleLabel.setText("Maintenance Records for: " + equipment.getName());
        }
        loadRecordsForEquipment(equipment.getId());
    }

    @FXML
    private void onRefreshRecords() {
        Equipment equipment = equipmentCombo.getSelectionModel().getSelectedItem();
        if (equipment == null) {
            showError("Select equipment first.");
            return;
        }
        loadRecordsForEquipment(equipment.getId());
    }

    private void loadRecordsForEquipment(int equipmentId) {
        maintenanceList.clear();
        try {
            maintenanceList.addAll(serviceMaintenance.getByEquipmentId(equipmentId));
        } catch (SQLException e) {
            showError("Could not load maintenance records: " + e.getMessage());
        }
        applyMaintenanceFilter(maintenanceSearchField != null ? maintenanceSearchField.getText() : "");
    }

    private void applyMaintenanceFilter(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        if (normalized.isEmpty()) {
            filteredMaintenance.setPredicate(item -> true);
            return;
        }
        filteredMaintenance.setPredicate(item -> {
            String type = item.getMaintenanceType() != null ? item.getMaintenanceType().toLowerCase() : "";
            return type.contains(normalized);
        });
    }

    @FXML
    private void onAddRecord() {
        Equipment equipment = equipmentCombo.getSelectionModel().getSelectedItem();
        if (equipment == null) {
            showError("Please select equipment.");
            return;
        }
        try {
            LocalDate date = maintenanceDatePicker.getValue();
            String type = maintenanceTypeField.getText().trim();
            String costText = costField.getText().trim();
            BigDecimal cost = validateMaintenanceInput(date, type, costText);
            if (cost == null) {
                return;
            }
            Maintenance record = new Maintenance(equipment.getId(), date, type, cost);
            serviceMaintenance.add(record);
            loadRecordsForEquipment(equipment.getId());
            clearForm();
            showInfo("Maintenance record added.");
        } catch (NumberFormatException e) {
            showError("Invalid cost.");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void onTableRowSelected() {
        selectedMaintenance = maintenanceTable.getSelectionModel().getSelectedItem();
        if (selectedMaintenance == null) {
            updateBtn.setDisable(true);
            deleteBtn.setDisable(true);
            return;
        }
        updateBtn.setDisable(false);
        deleteBtn.setDisable(false);
        maintenanceDatePicker.setValue(selectedMaintenance.getMaintenanceDate());
        maintenanceTypeField.setText(selectedMaintenance.getMaintenanceType());
        costField.setText(selectedMaintenance.getCost() != null ? selectedMaintenance.getCost().toString() : "");
    }

    @FXML
    private void onUpdateRecord() {
        if (selectedMaintenance == null) return;
        Equipment equipment = equipmentCombo.getSelectionModel().getSelectedItem();
        if (equipment == null) {
            showError("Please select equipment.");
            return;
        }
        try {
            LocalDate date = maintenanceDatePicker.getValue();
            String type = maintenanceTypeField.getText().trim();
            String costText = costField.getText().trim();
            BigDecimal cost = validateMaintenanceInput(date, type, costText);
            if (cost == null) {
                return;
            }
            selectedMaintenance.setEquipmentId(equipment.getId());
            selectedMaintenance.setMaintenanceDate(date);
            selectedMaintenance.setMaintenanceType(type);
            selectedMaintenance.setCost(cost);
            serviceMaintenance.update(selectedMaintenance);
            loadRecordsForEquipment(equipment.getId());
            clearForm();
            selectedMaintenance = null;
            updateBtn.setDisable(true);
            deleteBtn.setDisable(true);
            showInfo("Maintenance record updated.");
        } catch (NumberFormatException e) {
            showError("Invalid cost.");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteRecord() {
        if (selectedMaintenance == null) return;
        Equipment equipment = equipmentCombo.getSelectionModel().getSelectedItem();
        try {
            serviceMaintenance.delete(selectedMaintenance.getId());
            if (equipment != null) loadRecordsForEquipment(equipment.getId());
            clearForm();
            selectedMaintenance = null;
            updateBtn.setDisable(true);
            deleteBtn.setDisable(true);
            showInfo("Maintenance record deleted.");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void clearForm() {
        maintenanceDatePicker.setValue(null);
        maintenanceTypeField.clear();
        costField.clear();
    }

    private BigDecimal validateMaintenanceInput(LocalDate date, String type, String costText) {
        if (date == null) {
            showError("Please select a maintenance date.");
            return null;
        }
        if (type.isEmpty()) {
            showError("Maintenance type is required.");
            return null;
        }
        if (type.length() > 80) {
            showError("Maintenance type must be 80 characters or less.");
            return null;
        }
        if (costText.isEmpty()) {
            showError("Cost is required.");
            return null;
        }
        String normalizedCost = costText.replace(',', '.');
        BigDecimal cost;
        try {
            cost = new BigDecimal(normalizedCost);
        } catch (NumberFormatException e) {
            showError("Cost must be a valid number.");
            return null;
        }
        if (cost.scale() > 2) {
            showError("Cost can have at most 2 decimal places.");
            return null;
        }
        if (cost.compareTo(BigDecimal.ZERO) < 0) {
            showError("Cost must be positive.");
            return null;
        }
        return cost;
    }

    private void applyCostFormatter(TextField field) {
        if (field == null) {
            return;
        }
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            if (text == null || text.isEmpty()) {
                return change;
            }
            String normalized = text.replace(',', '.');
            if (!normalized.matches("\\d{0,10}(\\.\\d{0,2})?")) {
                return null;
            }
            return change;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    private void applyLengthLimit(TextField field, int maxLength) {
        if (field == null) {
            return;
        }
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            if (text != null && text.length() > maxLength) {
                return null;
            }
            return change;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
