package controllers;

import entity.Animal;
import entity.AnimalHealthRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Comparator;
import javafx.util.StringConverter;
import services.ServiceAnimal;
import services.ServiceAnimalHealthRecord;
import utils.AnimalListRefresh;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class HealthRecordController implements Initializable {

    @FXML private ComboBox<Animal> animalCombo;
    @FXML private DatePicker recordDatePicker;
    @FXML private TextField weightField;
    @FXML private ComboBox<String> appetiteCombo;
    @FXML private ComboBox<String> conditionCombo;
    @FXML private Label productionLabel;
    @FXML private TextField productionField;
    @FXML private TextField notesField;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Label recordsTitleLabel;
    @FXML private VBox recordTableContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> searchFieldCombo;

    private final ServiceAnimal serviceAnimal = new ServiceAnimal();
    private final ServiceAnimalHealthRecord serviceRecord = new ServiceAnimalHealthRecord();
    private final ObservableList<AnimalHealthRecord> recordList = FXCollections.observableArrayList();
    private FilteredList<AnimalHealthRecord> filteredRecords;
    private AnimalHealthRecord selectedRecord;
    private String currentSortColumn = null;
    private boolean sortAscending = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        appetiteCombo.setItems(FXCollections.observableArrayList("LOW", "NORMAL", "HIGH", "NONE"));
        conditionCombo.setItems(FXCollections.observableArrayList("HEALTHY", "SICK", "INJURED", "CRITICAL"));

        filteredRecords = new FilteredList<>(recordList, p -> true);

        if (searchFieldCombo != null) {
            searchFieldCombo.setItems(FXCollections.observableArrayList(
                "All Fields", "Date", "Weight", "Appetite", "Condition", "Production", "Notes"
            ));
            searchFieldCombo.getSelectionModel().selectFirst();
        }


        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterRecords(newValue);
                refreshRecordTable();
            });
        }
        if (searchFieldCombo != null) {
            searchFieldCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                filterRecords(searchField.getText());
                refreshRecordTable();
            });
        }

        recordDatePicker.setValue(LocalDate.now());

        loadAnimals();
        AnimalListRefresh.addListener(this::loadAnimals);
    }

    private void refreshRecordTable() {
        if (recordTableContainer == null) return;
        recordTableContainer.getChildren().clear();
        

        HBox headerRow = createRecordHeaderRow();
        recordTableContainer.getChildren().add(headerRow);
        

        List<AnimalHealthRecord> sortedList = new java.util.ArrayList<>(filteredRecords);
        if (currentSortColumn != null) {
            sortedList.sort(getRecordComparator(currentSortColumn, sortAscending));
        }
        
        int rowIdx = 0;
        for (AnimalHealthRecord r : sortedList) {
            HBox row = createRecordDataRow(r, rowIdx++);
            recordTableContainer.getChildren().add(row);
        }
    }

    private HBox createRecordHeaderRow() {
        HBox header = new HBox();
        header.getStyleClass().add("mgmt-table-header-row");
        header.setAlignment(Pos.CENTER_LEFT);
        
        String[] headers = {"ID", "Date", "Weight", "Appetite", "Condition", "Production", "Notes"};
        double[] widths = {50, 110, 80, 90, 90, 90, 180};
        
        for (int i = 0; i < headers.length; i++) {
            Label headerLabel = new Label(headers[i]);
            headerLabel.getStyleClass().add("mgmt-table-header-cell");
            headerLabel.setPrefWidth(widths[i]);
            headerLabel.setMinWidth(widths[i]);
            headerLabel.setAlignment(Pos.CENTER_LEFT);
            
            final String column = headers[i];
            headerLabel.setOnMouseClicked(e -> {
                if (currentSortColumn != null && currentSortColumn.equals(column)) {
                    sortAscending = !sortAscending;
                } else {
                    currentSortColumn = column;
                    sortAscending = true;
                }
                refreshRecordTable();
            });
            
            if (currentSortColumn != null && currentSortColumn.equals(column)) {
                headerLabel.setText(headers[i] + (sortAscending ? " ▲" : " ▼"));
            }
            
            header.getChildren().add(headerLabel);
        }
        return header;
    }

    private HBox createRecordDataRow(AnimalHealthRecord r, int rowIndex) {
        HBox row = new HBox();
        row.getStyleClass().add(rowIndex % 2 == 0 ? "mgmt-table-row" : "mgmt-table-row-alt");
        if (selectedRecord != null && selectedRecord.getId() != null && selectedRecord.getId().equals(r.getId())) {
            row.getStyleClass().add("mgmt-table-row-selected");
        }
        row.setAlignment(Pos.CENTER_LEFT);

        double[] widths = {50, 110, 80, 90, 90, 90, 180};
        String notes = r.getNotes();
        if (notes != null && notes.length() > 30) {
            notes = notes.substring(0, 27) + "...";
        }
        String[] values = {
            r.getId() != null ? r.getId().toString() : "-",
            r.getRecordDate() != null ? r.getRecordDate().toString() : "-",
            r.getWeight() != null ? r.getWeight() + " kg" : "-",
            r.getAppetite() != null ? r.getAppetite().name() : "-",
            r.getConditionStatus() != null ? r.getConditionStatus().name() : "-",
            formatProduction(r),
            notes != null ? notes : "-"
        };

        for (int i = 0; i < values.length; i++) {
            Label cell = new Label(values[i]);
            cell.setPrefWidth(widths[i]);
            cell.setMinWidth(widths[i]);
            cell.setAlignment(Pos.CENTER_LEFT);
            if (i == 4) {

                String status = values[i].toLowerCase();
                String badge = switch (status) {
                    case "healthy"  -> "badge-healthy";
                    case "sick"     -> "badge-sick";
                    case "injured"  -> "badge-injured";
                    case "critical" -> "badge-critical";
                    default         -> "badge-neutral";
                };
                cell.getStyleClass().addAll("mgmt-table-cell", badge);
            } else {
                cell.getStyleClass().add("mgmt-table-cell");
            }
            row.getChildren().add(cell);
        }
        
        row.setOnMouseClicked(e -> {
            selectedRecord = r;
            refreshRecordTable();
            updateBtn.setDisable(false);
            deleteBtn.setDisable(false);
            onTableRowSelected();
        });
        
        return row;
    }

    private Comparator<AnimalHealthRecord> getRecordComparator(String column, boolean ascending) {
        Comparator<AnimalHealthRecord> comp = switch (column) {
            case "ID" -> Comparator.comparing(r -> r.getId() != null ? r.getId() : 0);
            case "Date" -> Comparator.comparing(r -> r.getRecordDate() != null ? r.getRecordDate() : LocalDate.MIN);
            case "Weight" -> Comparator.comparing(r -> r.getWeight() != null ? r.getWeight() : 0.0);
            case "Appetite" -> Comparator.comparing(r -> r.getAppetite() != null ? r.getAppetite().name() : "");
            case "Condition" -> Comparator.comparing(r -> r.getConditionStatus() != null ? r.getConditionStatus().name() : "");
            case "Production" -> Comparator.comparing(r -> {
                String prod = formatProduction(r);
                try {
                    return prod != null && !prod.isEmpty() ? Double.parseDouble(prod) : 0.0;
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            });
            case "Notes" -> Comparator.comparing(r -> r.getNotes() != null ? r.getNotes() : "");
            default -> Comparator.comparing(r -> r.getId() != null ? r.getId() : 0);
        };
        return ascending ? comp : comp.reversed();
    }

    private void filterRecords(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredRecords.setPredicate(record -> true);
            return;
        }

        String selectedField = searchFieldCombo != null && searchFieldCombo.getSelectionModel().getSelectedItem() != null
                ? searchFieldCombo.getSelectionModel().getSelectedItem() : "All Fields";
        String lowerSearch = searchText.toLowerCase();

        filteredRecords.setPredicate(record -> {
            switch (selectedField) {
                case "Date":
                    return record.getRecordDate() != null && record.getRecordDate().toString().contains(lowerSearch);
                case "Weight":
                    return record.getWeight() != null && String.valueOf(record.getWeight()).contains(lowerSearch);
                case "Appetite":
                    return record.getAppetite() != null && record.getAppetite().name().toLowerCase().contains(lowerSearch);
                case "Condition":
                    return record.getConditionStatus() != null && record.getConditionStatus().name().toLowerCase().contains(lowerSearch);
                case "Production":
                    String prod = formatProduction(record);
                    return prod != null && prod.toLowerCase().contains(lowerSearch);
                case "Notes":
                    return record.getNotes() != null && record.getNotes().toLowerCase().contains(lowerSearch);
                case "All Fields":
                default:

                    if (record.getRecordDate() != null && record.getRecordDate().toString().contains(lowerSearch)) {
                        return true;
                    }
                    if (record.getWeight() != null && String.valueOf(record.getWeight()).contains(lowerSearch)) {
                        return true;
                    }
                    if (record.getAppetite() != null && record.getAppetite().name().toLowerCase().contains(lowerSearch)) {
                        return true;
                    }
                    if (record.getConditionStatus() != null && record.getConditionStatus().name().toLowerCase().contains(lowerSearch)) {
                        return true;
                    }
                    String production = formatProduction(record);
                    if (production != null && production.toLowerCase().contains(lowerSearch)) {
                        return true;
                    }
                    if (record.getNotes() != null && record.getNotes().toLowerCase().contains(lowerSearch)) {
                        return true;
                    }
                    return false;
            }
        });
    }

    private String formatProduction(AnimalHealthRecord r) {
        if (r.getMilkYield() != null) return r.getMilkYield().toString();
        if (r.getEggCount() != null) return r.getEggCount().toString();
        if (r.getWoolLength() != null) return r.getWoolLength().toString();
        return "";
    }

    private void loadAnimals() {
        animalCombo.getItems().clear();
        try {
            List<Animal> animals = serviceAnimal.getAll();
            animalCombo.getItems().addAll(animals);
            animalCombo.setConverter(new StringConverter<Animal>() {
                @Override
                public String toString(Animal a) {
                    return a != null ? "ID " + a.getId() + " - Ear " + a.getEarTag() + " (" + a.getType() + ")" : "";
                }
                @Override
                public Animal fromString(String s) { return null; }
            });
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onAnimalSelected() {
        Animal a = animalCombo.getSelectionModel().getSelectedItem();
        if (a == null) {
            recordList.clear();
            if (recordsTitleLabel != null) recordsTitleLabel.setText("Health Records for Selected Animal");
            return;
        }
        updateProductionLabel(a.getType());
        recordsTitleLabel.setText("All Health Records for: " + a.getType() + " #" + a.getEarTag());
        loadRecordsForAnimal(a.getId());
    }

    @FXML
    private void onRefreshRecords() {
        Animal a = animalCombo.getSelectionModel().getSelectedItem();
        if (a == null) {
            showError("Select an animal first.");
            return;
        }
        loadRecordsForAnimal(a.getId());
    }

    private void updateProductionLabel(String type) {
        if (type == null) {
            productionLabel.setText("Production:");
            return;
        }
        switch (type.toLowerCase()) {
            case "cow":
            case "goat":
                productionLabel.setText("Milk Yield (L):");
                break;
            case "chicken":
                productionLabel.setText("Egg Count:");
                break;
            case "sheep":
                productionLabel.setText("Wool Length (cm):");
                break;
            default:
                productionLabel.setText("Production:");
        }
    }

    private void loadRecordsForAnimal(int animalId) {
        recordList.clear();
        try {
            recordList.addAll(serviceRecord.getRecordsByAnimalId(animalId));
            refreshRecordTable();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onAddRecord() {
        Animal a = animalCombo.getSelectionModel().getSelectedItem();
        if (a == null) {
            showError("Please select an animal.");
            return;
        }
        try {
            LocalDate recordDate = recordDatePicker.getValue();
            if (recordDate == null) {
                showError("Please select record date.");
                return;
            }
            Double weight = null;
            if (!weightField.getText().trim().isEmpty()) {
                weight = Double.parseDouble(weightField.getText().trim());
                if (weight < 0) {
                    showError("Weight can't be negative.");
                    return;
                }
            }
            AnimalHealthRecord.Appetite appetite = appetiteCombo.getSelectionModel().getSelectedItem() != null
                    ? AnimalHealthRecord.Appetite.valueOf(appetiteCombo.getSelectionModel().getSelectedItem()) : null;
            AnimalHealthRecord.ConditionStatus condition = conditionCombo.getSelectionModel().getSelectedItem() != null
                    ? AnimalHealthRecord.ConditionStatus.valueOf(conditionCombo.getSelectionModel().getSelectedItem()) : null;
            if (condition == null) {
                showError("Please select condition status.");
                return;
            }

            Double milkYield = null;
            Integer eggCount = null;
            Double woolLength = null;
            String prod = productionField.getText().trim();
            if (!prod.isEmpty() && a.getType() != null) {
                switch (a.getType().toLowerCase()) {
                    case "cow":
                    case "goat":
                        milkYield = Double.parseDouble(prod);
                        if (milkYield < 0) {
                            showError("Production can't be negative.");
                            return;
                        }
                        break;
                    case "chicken":
                        eggCount = Integer.parseInt(prod);
                        if (eggCount < 0) {
                            showError("Production can't be negative.");
                            return;
                        }
                        break;
                    case "sheep":
                        woolLength = Double.parseDouble(prod);
                        if (woolLength < 0) {
                            showError("Production can't be negative.");
                            return;
                        }
                        break;
                    default:
                        break;
                }
            }

            AnimalHealthRecord r = new AnimalHealthRecord(a.getId(), recordDate, weight, appetite, condition, milkYield, eggCount, woolLength, notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());
            serviceRecord.add(r);
            loadRecordsForAnimal(a.getId());
            clearForm();
            showInfo("Health record added.");
        } catch (NumberFormatException e) {
            showError("Invalid number");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void onTableRowSelected() {
        if (selectedRecord == null) {
            updateBtn.setDisable(true);
            deleteBtn.setDisable(true);
            return;
        }
        updateBtn.setDisable(false);
        deleteBtn.setDisable(false);
        recordDatePicker.setValue(selectedRecord.getRecordDate());
        weightField.setText(selectedRecord.getWeight() != null ? selectedRecord.getWeight().toString() : "");
        appetiteCombo.getSelectionModel().select(selectedRecord.getAppetite() != null ? selectedRecord.getAppetite().name() : null);
        conditionCombo.getSelectionModel().select(selectedRecord.getConditionStatus() != null ? selectedRecord.getConditionStatus().name() : null);
        String prod = formatProduction(selectedRecord);
        productionField.setText(prod);
        notesField.setText(selectedRecord.getNotes() != null ? selectedRecord.getNotes() : "");
    }

    @FXML
    private void onUpdateRecord() {
        if (selectedRecord == null) return;
        Animal a = animalCombo.getSelectionModel().getSelectedItem();
        if (a == null) {
            showError("Please select an animal.");
            return;
        }
        try {
            LocalDate recordDate = recordDatePicker.getValue();
            if (recordDate == null) {
                showError("Please select record date.");
                return;
            }
            Double weight = null;
            if (!weightField.getText().trim().isEmpty()) {
                weight = Double.parseDouble(weightField.getText().trim());
                if (weight < 0) {
                    showError("Weight can't be negative");
                    return;
                }
            }
            AnimalHealthRecord.Appetite appetite = appetiteCombo.getSelectionModel().getSelectedItem() != null
                    ? AnimalHealthRecord.Appetite.valueOf(appetiteCombo.getSelectionModel().getSelectedItem()) : null;
            AnimalHealthRecord.ConditionStatus condition = conditionCombo.getSelectionModel().getSelectedItem() != null
                    ? AnimalHealthRecord.ConditionStatus.valueOf(conditionCombo.getSelectionModel().getSelectedItem()) : null;
            if (condition == null) {
                showError("Please select condition status.");
                return;
            }

            Double milkYield = null;
            Integer eggCount = null;
            Double woolLength = null;
            String prod = productionField.getText().trim();
            if (!prod.isEmpty() && a.getType() != null) {
                switch (a.getType().toLowerCase()) {
                    case "cow":
                    case "goat":
                        milkYield = Double.parseDouble(prod);
                        if (milkYield < 0) {
                            showError("Production can't be negative");
                            return;
                        }
                        break;
                    case "chicken":
                        eggCount = Integer.parseInt(prod);
                        if (eggCount < 0) {
                            showError("Production can't be negative");
                            return;
                        }
                        break;
                    case "sheep":
                        woolLength = Double.parseDouble(prod);
                        if (woolLength < 0) {
                            showError("Production can't be negative");
                            return;
                        }
                        break;
                    default:
                        break;
                }
            }

            selectedRecord.setRecordDate(recordDate);
            selectedRecord.setWeight(weight);
            selectedRecord.setAppetite(appetite);
            selectedRecord.setConditionStatus(condition);
            selectedRecord.setMilkYield(milkYield);
            selectedRecord.setEggCount(eggCount);
            selectedRecord.setWoolLength(woolLength);
            selectedRecord.setNotes(notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());
            serviceRecord.update(selectedRecord);
            loadRecordsForAnimal(a.getId());
            clearForm();
            selectedRecord = null;
            updateBtn.setDisable(true);
            deleteBtn.setDisable(true);
            showInfo("Health record updated.");
        } catch (NumberFormatException e) {
            showError("Invalid number");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteRecord() {
        if (selectedRecord == null) return;
        Animal a = animalCombo.getSelectionModel().getSelectedItem();
        try {
            serviceRecord.delete(selectedRecord.getId());
            if (a != null) loadRecordsForAnimal(a.getId());
            clearForm();
            selectedRecord = null;
            updateBtn.setDisable(true);
            deleteBtn.setDisable(true);
            showInfo("Health record deleted.");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void clearForm() {
        recordDatePicker.setValue(LocalDate.now());
        weightField.clear();
        appetiteCombo.getSelectionModel().clearSelection();
        conditionCombo.getSelectionModel().clearSelection();
        productionField.clear();
        notesField.clear();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
