package controllers;

import entity.Animal;
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
import services.ServiceAnimal;
import services.ServiceEnumManagement;
import utils.AnimalListRefresh;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class AnimalController implements Initializable {

    @FXML private TextField earTagField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> genderCombo;
    @FXML private TextField weightField;
    @FXML private DatePicker birthDatePicker;
    @FXML private DatePicker entryDatePicker;
    @FXML private ComboBox<String> originCombo;
    @FXML private CheckBox vaccinatedCheck;
    @FXML private VBox animalTableContainer;
    @FXML private ComboBox<String> locationCombo;
    @FXML private Button deleteAnimalBtn;
    @FXML private Button updateAnimalBtn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> searchFieldCombo;

    private final ServiceAnimal serviceAnimal = new ServiceAnimal();
    private final ServiceEnumManagement serviceEnum = new ServiceEnumManagement();
    private final ObservableList<Animal> animalList = FXCollections.observableArrayList();
    private FilteredList<Animal> filteredAnimals;
    private Animal selectedAnimal;
    private String currentSortColumn = null;
    private boolean sortAscending = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        genderCombo.setItems(FXCollections.observableArrayList("MALE", "FEMALE"));
        originCombo.setItems(FXCollections.observableArrayList("BORN_IN_FARM", "OUTSIDE"));
        refreshTypeAndLocationCombos();

        filteredAnimals = new FilteredList<>(animalList, p -> true);

        if (searchFieldCombo != null) {
            searchFieldCombo.setItems(FXCollections.observableArrayList(
                "All Fields", "Ear Tag", "Type", "Gender", "Weight", "Health Status", 
                "Birth Date", "Age", "Origin", "Location"
            ));
            searchFieldCombo.getSelectionModel().selectFirst();
        }


        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterAnimals(newValue);
                refreshTable();
            });
        }
        if (searchFieldCombo != null) {
            searchFieldCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                filterAnimals(searchField.getText());
                refreshTable();
            });
        }

        AnimalListRefresh.addListener(this::refreshTypeAndLocationCombos);
        refreshData();
    }

    private void refreshTable() {
        if (animalTableContainer == null) return;
        animalTableContainer.getChildren().clear();

        HBox headerRow = createHeaderRow();
        animalTableContainer.getChildren().add(headerRow);

        List<Animal> sortedList = new java.util.ArrayList<>(filteredAnimals);
        if (currentSortColumn != null) {
            sortedList.sort(getComparator(currentSortColumn, sortAscending));
        }
        
        for (Animal a : sortedList) {
            HBox row = createDataRow(a);
            animalTableContainer.getChildren().add(row);
        }
    }

    private HBox createHeaderRow() {
        HBox header = new HBox();
        header.getStyleClass().add("mgmt-table-header-row");
        header.setAlignment(Pos.CENTER_LEFT);
        
        String[] headers = {"ID", "Ear Tag", "Type", "Gender", "Weight", "Health", "Birth Date", "Age", "Origin", "Location"};
        double[] widths = {50, 80, 90, 80, 80, 90, 110, 70, 110, 120};
        
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
                refreshTable();
            });
            
            if (currentSortColumn != null && currentSortColumn.equals(column)) {
                headerLabel.setText(headers[i] + (sortAscending ? " ▲" : " ▼"));
            }
            
            header.getChildren().add(headerLabel);
        }
        return header;
    }

    private HBox createDataRow(Animal a) {
        HBox row = new HBox();
        row.getStyleClass().add("mgmt-table-row");
        if (selectedAnimal != null && selectedAnimal.getId() != null && selectedAnimal.getId().equals(a.getId())) {
            row.getStyleClass().add("mgmt-table-row-selected");
        }
        row.setAlignment(Pos.CENTER_LEFT);
        
        double[] widths = {50, 80, 90, 80, 80, 90, 110, 70, 110, 120};
        String[] values = {
            a.getId() != null ? a.getId().toString() : "-",
            a.getEarTag() != null ? a.getEarTag().toString() : "-",
            a.getType() != null ? a.getType() : "-",
            a.getGender() != null ? a.getGender().name() : "-",
            a.getWeight() != null ? a.getWeight() + " kg" : "-",
            a.getHealthStatus() != null ? a.getHealthStatus() : "-",
            a.getBirthDate() != null ? a.getBirthDate().toString() : "-",
            a.getAge() != null ? a.getAge() + " yr" : "-",
            a.getOrigin() != null ? a.getOrigin().name() : "-",
            a.getLocation() != null ? a.getLocation() : "-"
        };
        
        for (int i = 0; i < values.length; i++) {
            Label cell = new Label(values[i]);
            cell.getStyleClass().add("mgmt-table-cell");
            cell.setPrefWidth(widths[i]);
            cell.setMinWidth(widths[i]);
            cell.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().add(cell);
        }
        
        row.setOnMouseClicked(e -> {
            selectedAnimal = a;
            refreshTable();
            deleteAnimalBtn.setDisable(false);
            updateAnimalBtn.setDisable(false);
            populateForm(a);
        });
        
        return row;
    }

    private Comparator<Animal> getComparator(String column, boolean ascending) {
        Comparator<Animal> comp = switch (column) {
            case "ID" -> Comparator.comparing(a -> a.getId() != null ? a.getId() : 0);
            case "Ear Tag" -> Comparator.comparing(a -> a.getEarTag() != null ? a.getEarTag() : 0);
            case "Type" -> Comparator.comparing(a -> a.getType() != null ? a.getType() : "");
            case "Gender" -> Comparator.comparing(a -> a.getGender() != null ? a.getGender().name() : "");
            case "Weight" -> Comparator.comparing(a -> a.getWeight() != null ? a.getWeight() : 0.0);
            case "Health" -> Comparator.comparing(a -> a.getHealthStatus() != null ? a.getHealthStatus() : "");
            case "Birth Date" -> Comparator.comparing(a -> a.getBirthDate() != null ? a.getBirthDate() : LocalDate.MIN);
            case "Age" -> Comparator.comparing(a -> a.getAge() != null ? a.getAge() : 0);
            case "Origin" -> Comparator.comparing(a -> a.getOrigin() != null ? a.getOrigin().name() : "");
            case "Location" -> Comparator.comparing(a -> a.getLocation() != null ? a.getLocation() : "");
            default -> Comparator.comparing(a -> a.getId() != null ? a.getId() : 0);
        };
        return ascending ? comp : comp.reversed();
    }

    private void filterAnimals(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredAnimals.setPredicate(animal -> true);
            return;
        }

        String selectedField = searchFieldCombo != null && searchFieldCombo.getSelectionModel().getSelectedItem() != null
                ? searchFieldCombo.getSelectionModel().getSelectedItem() : "All Fields";
        String lowerSearch = searchText.toLowerCase();

        filteredAnimals.setPredicate(animal -> {
            switch (selectedField) {
                case "Ear Tag":
                    return animal.getEarTag() != null && String.valueOf(animal.getEarTag()).contains(lowerSearch);
                case "Type":
                    return animal.getType() != null && animal.getType().toLowerCase().contains(lowerSearch);
                case "Gender":
                    return animal.getGender() != null && animal.getGender().name().toLowerCase().contains(lowerSearch);
                case "Weight":
                    return animal.getWeight() != null && String.valueOf(animal.getWeight()).contains(lowerSearch);
                case "Health Status":
                    return animal.getHealthStatus() != null && animal.getHealthStatus().toLowerCase().contains(lowerSearch);
                case "Birth Date":
                    return animal.getBirthDate() != null && animal.getBirthDate().toString().contains(lowerSearch);
                case "Age":
                    return animal.getAge() != null && String.valueOf(animal.getAge()).contains(lowerSearch);
                case "Origin":
                    return animal.getOrigin() != null && animal.getOrigin().name().toLowerCase().contains(lowerSearch);
                case "Location":
                    return animal.getLocation() != null && animal.getLocation().toLowerCase().contains(lowerSearch);
                case "All Fields":
                default:

                    if (animal.getEarTag() != null && String.valueOf(animal.getEarTag()).contains(lowerSearch)) {
                        return true;
                    }
                    if (animal.getType() != null && animal.getType().toLowerCase().contains(lowerSearch)) {
                        return true;
                    }
                    if (animal.getGender() != null && animal.getGender().name().toLowerCase().contains(lowerSearch)) {
                        return true;
                    }
                    if (animal.getWeight() != null && String.valueOf(animal.getWeight()).contains(lowerSearch)) {
                        return true;
                    }
                    if (animal.getLocation() != null && animal.getLocation().toLowerCase().contains(lowerSearch)) {
                        return true;
                    }
                    if (animal.getOrigin() != null && animal.getOrigin().name().toLowerCase().contains(lowerSearch)) {
                        return true;
                    }
                    if (animal.getHealthStatus() != null && animal.getHealthStatus().toLowerCase().contains(lowerSearch)) {
                        return true;
                    }
                    if (animal.getBirthDate() != null && animal.getBirthDate().toString().contains(lowerSearch)) {
                        return true;
                    }
                    if (animal.getAge() != null && String.valueOf(animal.getAge()).contains(lowerSearch)) {
                        return true;
                    }
                    return false;
            }
        });
    }

    private void refreshTypeAndLocationCombos() {
        try {
            List<String> types = serviceEnum.getEnumValues("Animal", "type");
            typeCombo.setItems(FXCollections.observableArrayList(types));
            List<String> locations = serviceEnum.getEnumValues("Animal", "location");
            locationCombo.setItems(FXCollections.observableArrayList(locations));
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onRefreshAnimals() {
        refreshData();
    }

    @FXML
    private void onDeleteAnimal() {
        Animal selected = selectedAnimal;
        if (selected == null) return;
        try {
            serviceAnimal.delete(selected.getId());
            selectedAnimal = null;
            refreshTable();
            clearForm();
            deleteAnimalBtn.setDisable(true);
            updateAnimalBtn.setDisable(true);
            AnimalListRefresh.notifyAnimalChanged();
            showInfo("Animal deleted.");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdateAnimal() {
        Animal selected = selectedAnimal;
        if (selected == null) return;
        try {
            int earTag = Integer.parseInt(earTagField.getText().trim());
            String type = typeCombo.getSelectionModel().getSelectedItem();
            Animal.Gender gender = Animal.Gender.valueOf(genderCombo.getSelectionModel().getSelectedItem());
            Double weight = null;
            if (!weightField.getText().trim().isEmpty()) {
                weight = Double.parseDouble(weightField.getText().trim());
                if (weight < 0) {
                    showError("Weight can't be negative.");
                    return;
                }
            }
            LocalDate birthDate = birthDatePicker.getValue();
            LocalDate entryDate = entryDatePicker.getValue();
            Animal.Origin origin = Animal.Origin.valueOf(originCombo.getSelectionModel().getSelectedItem());
            boolean vaccinated = vaccinatedCheck.isSelected();
            String location = locationCombo.getSelectionModel().getSelectedItem();

            selected.setEarTag(earTag);
            selected.setType(type);
            selected.setGender(gender);
            selected.setWeight(weight);
            selected.setBirthDate(birthDate);
            selected.setEntryDate(entryDate);
            selected.setOrigin(origin);
            selected.setVaccinated(vaccinated);
            selected.setLocation(location);
            serviceAnimal.update(selected);
            selectedAnimal = null;
            refreshData();
            clearForm();
            updateAnimalBtn.setDisable(true);
            AnimalListRefresh.notifyAnimalChanged();
            deleteAnimalBtn.setDisable(true);
            showInfo("Animal updated.");
        } catch (NumberFormatException e) {
            showError("Check animal ear tag and weight");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        } catch (Exception e) {
            showError("Please fill all required fields");
        }
    }

    @FXML
    private void onAddAnimal() {
        try {
            int earTag = Integer.parseInt(earTagField.getText().trim());
            String type = typeCombo.getSelectionModel().getSelectedItem();
            Animal.Gender gender = Animal.Gender.valueOf(genderCombo.getSelectionModel().getSelectedItem());
            Double weight = null;
            if (!weightField.getText().trim().isEmpty()) {
                weight = Double.parseDouble(weightField.getText().trim());
                if (weight < 0) {
                    showError("Weight Can't be negative.");
                    return;
                }
            }
            LocalDate birthDate = birthDatePicker.getValue();
            LocalDate entryDate = entryDatePicker.getValue();
            Animal.Origin origin = Animal.Origin.valueOf(originCombo.getSelectionModel().getSelectedItem());
            boolean vaccinated = vaccinatedCheck.isSelected();
            String location = locationCombo.getSelectionModel().getSelectedItem();

            Animal a = new Animal(earTag, type, gender, weight, null, birthDate, entryDate, origin, vaccinated, location);
            serviceAnimal.add(a);
            refreshData();
            clearForm();
            AnimalListRefresh.notifyAnimalChanged();
            showInfo("Animal added successfully.");
        } catch (NumberFormatException e) {
            showError("Check ear tag and weight again");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        } catch (Exception e) {
            showError("Please fill all required fields");
        }
    }

    private void refreshData() {
        animalList.clear();
        try {
            animalList.addAll(serviceAnimal.getAll());
            refreshTable();
        } catch (SQLException e) {
            showError("Could not load animals: " + e.getMessage());
        }
    }

    private void populateForm(Animal a) {
        earTagField.setText(a.getEarTag() != null ? String.valueOf(a.getEarTag()) : "");
        typeCombo.getSelectionModel().select(a.getType());
        genderCombo.getSelectionModel().select(a.getGender() != null ? a.getGender().name() : null);
        weightField.setText(a.getWeight() != null ? String.valueOf(a.getWeight()) : "");
        birthDatePicker.setValue(a.getBirthDate());
        entryDatePicker.setValue(a.getEntryDate());
        originCombo.getSelectionModel().select(a.getOrigin() != null ? a.getOrigin().name() : null);
        vaccinatedCheck.setSelected(a.getVaccinated() != null && a.getVaccinated());
        locationCombo.getSelectionModel().select(a.getLocation());
    }

    private void clearForm() {
        earTagField.clear();
        typeCombo.getSelectionModel().clearSelection();
        genderCombo.getSelectionModel().clearSelection();
        weightField.clear();
        birthDatePicker.setValue(null);
        entryDatePicker.setValue(null);
        originCombo.getSelectionModel().clearSelection();
        vaccinatedCheck.setSelected(false);
        locationCombo.getSelectionModel().clearSelection();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
