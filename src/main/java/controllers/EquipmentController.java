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
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import entity.Equipment;
import services.ServiceEquipment;
import services.ServiceMaintenance;
import utils.EquipmentListRefresh;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class EquipmentController implements Initializable {

    @FXML private TextField nameField;
    @FXML private TextField typeField;
    @FXML private TextField statusField;
    @FXML private DatePicker purchaseDatePicker;
    @FXML private TextField equipmentSearchField;
    @FXML private TableView<Equipment> equipmentTable;
    @FXML private TableColumn<Equipment, Integer> colId;
    @FXML private TableColumn<Equipment, String> colName;
    @FXML private TableColumn<Equipment, String> colType;
    @FXML private TableColumn<Equipment, String> colStatus;
    @FXML private TableColumn<Equipment, LocalDate> colPurchaseDate;
    @FXML private Button deleteEquipmentBtn;
    @FXML private Button updateEquipmentBtn;

    private final ServiceEquipment serviceEquipment = new ServiceEquipment();
    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();
    private final ObservableList<Equipment> equipmentList = FXCollections.observableArrayList();
    private final FilteredList<Equipment> filteredEquipment = new FilteredList<>(equipmentList, item -> true);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        purchaseDatePicker.setEditable(false);
        equipmentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        applyLengthLimit(nameField, 120);
        applyLengthLimit(typeField, 80);
        applyLengthLimit(statusField, 30);
        if (equipmentSearchField != null) {
            applyLengthLimit(equipmentSearchField, 120);
        }
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colPurchaseDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPurchaseDate()));

        SortedList<Equipment> sortedEquipment = new SortedList<>(filteredEquipment);
        sortedEquipment.comparatorProperty().bind(equipmentTable.comparatorProperty());
        equipmentTable.setItems(sortedEquipment);
        if (equipmentSearchField != null) {
            equipmentSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyEquipmentFilter(newVal));
        }
        equipmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            deleteEquipmentBtn.setDisable(!hasSelection);
            updateEquipmentBtn.setDisable(!hasSelection);
            if (hasSelection) populateForm(newVal);
        });
        refreshTable();
    }

    private void applyEquipmentFilter(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        if (normalized.isEmpty()) {
            filteredEquipment.setPredicate(item -> true);
            return;
        }
        filteredEquipment.setPredicate(item -> {
            String name = item.getName() != null ? item.getName().toLowerCase() : "";
            String type = item.getType() != null ? item.getType().toLowerCase() : "";
            String status = item.getStatus() != null ? item.getStatus().toLowerCase() : "";
            return name.contains(normalized) || type.contains(normalized) || status.contains(normalized);
        });
    }

    @FXML
    private void onRefreshEquipments() {
        refreshTable();
    }

    @FXML
    private void onAddEquipment() {
        try {
            String name = nameField.getText().trim();
            String type = typeField.getText().trim();
            String status = statusField.getText().trim();
            LocalDate purchaseDate = purchaseDatePicker.getValue();
            if (!validateEquipmentInput(name, type, status)) {
                return;
            }
            Equipment equipment = new Equipment(name, type, status, purchaseDate);
            serviceEquipment.add(equipment);
            refreshTable();
            clearForm();
            EquipmentListRefresh.notifyEquipmentChanged();
            showInfo("Equipment added successfully.");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdateEquipment() {
        Equipment selected = equipmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            String name = nameField.getText().trim();
            String type = typeField.getText().trim();
            String status = statusField.getText().trim();
            LocalDate purchaseDate = purchaseDatePicker.getValue();
            if (!validateEquipmentInput(name, type, status)) {
                return;
            }
            selected.setName(name);
            selected.setType(type);
            selected.setStatus(status);
            selected.setPurchaseDate(purchaseDate);
            serviceEquipment.update(selected);
            refreshTable();
            clearForm();
            updateEquipmentBtn.setDisable(true);
            deleteEquipmentBtn.setDisable(true);
            EquipmentListRefresh.notifyEquipmentChanged();
            showInfo("Equipment updated successfully.");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteEquipment() {
        Equipment selected = equipmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            serviceMaintenance.deleteByEquipmentId(selected.getId());
            serviceEquipment.delete(selected.getId());
            refreshTable();
            clearForm();
            deleteEquipmentBtn.setDisable(true);
            updateEquipmentBtn.setDisable(true);
            EquipmentListRefresh.notifyEquipmentChanged();
            showInfo("Equipment deleted.");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void refreshTable() {
        equipmentList.clear();
        try {
            equipmentList.addAll(serviceEquipment.getAll());
        } catch (SQLException e) {
            showError("Could not load equipment: " + e.getMessage());
        }
        applyEquipmentFilter(equipmentSearchField != null ? equipmentSearchField.getText() : "");
    }

    private boolean validateEquipmentInput(String name, String type, String status) {
        if (name.isEmpty()) {
            showError("Name is required.");
            return false;
        }
        if (type.isEmpty()) {
            showError("Type is required.");
            return false;
        }
        if (status.isEmpty()) {
            showError("Status is required.");
            return false;
        }
        if (name.length() > 120) {
            showError("Name must be 120 characters or less.");
            return false;
        }
        if (type.length() > 80) {
            showError("Type must be 80 characters or less.");
            return false;
        }
        if (status.length() > 30) {
            showError("Status must be 30 characters or less.");
            return false;
        }
        return true;
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

    private void populateForm(Equipment e) {
        nameField.setText(e.getName());
        typeField.setText(e.getType());
        statusField.setText(e.getStatus());
        purchaseDatePicker.setValue(e.getPurchaseDate());
    }

    private void clearForm() {
        nameField.clear();
        typeField.clear();
        statusField.clear();
        purchaseDatePicker.setValue(null);
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
