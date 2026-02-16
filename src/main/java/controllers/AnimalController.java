package controllers;

import entity.Animal;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
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
    @FXML private TableView<Animal> animalTable;
    @FXML private TableColumn<Animal, Integer> colId;
    @FXML private TableColumn<Animal, Integer> colEarTag;
    @FXML private TableColumn<Animal, String> colType;
    @FXML private TableColumn<Animal, String> colGender;
    @FXML private TableColumn<Animal, Double> colWeight;
    @FXML private TableColumn<Animal, String> colHealthStatus;
    @FXML private TableColumn<Animal, LocalDate> colBirthDate;
    @FXML private TableColumn<Animal, Integer> colAge;
    @FXML private TableColumn<Animal, String> colOrigin;
    @FXML private TableColumn<Animal, String> colLocation;
    @FXML private ComboBox<String> locationCombo;
    @FXML private Button deleteAnimalBtn;
    @FXML private Button updateAnimalBtn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> searchFieldCombo;

    private final ServiceAnimal serviceAnimal = new ServiceAnimal();
    private final ServiceEnumManagement serviceEnum = new ServiceEnumManagement();
    private final ObservableList<Animal> animalList = FXCollections.observableArrayList();
    private FilteredList<Animal> filteredAnimals;
    private SortedList<Animal> sortedAnimals;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        genderCombo.setItems(FXCollections.observableArrayList("MALE", "FEMALE"));
        originCombo.setItems(FXCollections.observableArrayList("BORN_IN_FARM", "OUTSIDE"));
        refreshTypeAndLocationCombos();

        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colEarTag.setCellValueFactory(c -> c.getValue().getEarTag() != null ? new SimpleIntegerProperty(c.getValue().getEarTag()).asObject() : new SimpleObjectProperty<>());
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType() != null ? c.getValue().getType() : ""));
        colGender.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGender() != null ? c.getValue().getGender().name() : ""));
        colWeight.setCellValueFactory(c -> c.getValue().getWeight() != null ? new SimpleDoubleProperty(c.getValue().getWeight()).asObject() : new SimpleObjectProperty<>());
        colHealthStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHealthStatus() != null ? c.getValue().getHealthStatus() : ""));
        colBirthDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getBirthDate()));
        colAge.setCellValueFactory(c -> c.getValue().getAge() != null ? new SimpleIntegerProperty(c.getValue().getAge()).asObject() : new SimpleObjectProperty<>());
        colOrigin.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrigin() != null ? c.getValue().getOrigin().name() : ""));
        colLocation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLocation() != null ? c.getValue().getLocation() : ""));


        filteredAnimals = new FilteredList<>(animalList, p -> true);
        sortedAnimals = new SortedList<>(filteredAnimals);
        sortedAnimals.comparatorProperty().bind(animalTable.comparatorProperty());
        animalTable.setItems(sortedAnimals);


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
            });
        }
        if (searchFieldCombo != null) {
            searchFieldCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                filterAnimals(searchField.getText());
            });
        }


        animalTable.getColumns().forEach(col -> col.setSortable(true));

        animalTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            deleteAnimalBtn.setDisable(!hasSelection);
            updateAnimalBtn.setDisable(!hasSelection);
            if (hasSelection) populateForm(newVal);
        });
        AnimalListRefresh.addListener(this::refreshTypeAndLocationCombos);
        refreshTable();
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
        refreshTable();
    }

    @FXML
    private void onDeleteAnimal() {
        Animal selected = animalTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            serviceAnimal.delete(selected.getId());
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
        Animal selected = animalTable.getSelectionModel().getSelectedItem();
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
            refreshTable();
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
            refreshTable();
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

    private void refreshTable() {
        animalList.clear();
        try {
            animalList.addAll(serviceAnimal.getAll());
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
