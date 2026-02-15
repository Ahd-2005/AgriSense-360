package controllers;

import entity.Animal;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import services.ServiceAnimal;
import utils.AnimalListRefresh;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
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
    @FXML private TableColumn<Animal, String> colOrigin;
    @FXML private Button deleteAnimalBtn;
    @FXML private Button updateAnimalBtn;

    private final ServiceAnimal serviceAnimal = new ServiceAnimal();
    private final ObservableList<Animal> animalList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeCombo.setItems(FXCollections.observableArrayList("SHEEP", "COW", "GOAT", "CHICKEN"));
        genderCombo.setItems(FXCollections.observableArrayList("MALE", "FEMALE"));
        originCombo.setItems(FXCollections.observableArrayList("BORN_IN_FARM", "OUTSIDE"));

        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colEarTag.setCellValueFactory(c -> c.getValue().getEarTag() != null ? new SimpleIntegerProperty(c.getValue().getEarTag()).asObject() : new SimpleObjectProperty<>());
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType() != null ? c.getValue().getType().name() : ""));
        colGender.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGender() != null ? c.getValue().getGender().name() : ""));
        colWeight.setCellValueFactory(c -> c.getValue().getWeight() != null ? new SimpleDoubleProperty(c.getValue().getWeight()).asObject() : new SimpleObjectProperty<>());
        colHealthStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHealthStatus() != null ? c.getValue().getHealthStatus() : ""));
        colBirthDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getBirthDate()));
        colOrigin.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrigin() != null ? c.getValue().getOrigin().name() : ""));

        animalTable.setItems(animalList);
        animalTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            deleteAnimalBtn.setDisable(!hasSelection);
            updateAnimalBtn.setDisable(!hasSelection);
            if (hasSelection) populateForm(newVal);
        });
        refreshTable();
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
            Animal.AnimalType type = Animal.AnimalType.valueOf(typeCombo.getSelectionModel().getSelectedItem());
            Animal.Gender gender = Animal.Gender.valueOf(genderCombo.getSelectionModel().getSelectedItem());
            Double weight = weightField.getText().trim().isEmpty() ? null : Double.parseDouble(weightField.getText().trim());
            LocalDate birthDate = birthDatePicker.getValue();
            LocalDate entryDate = entryDatePicker.getValue();
            Animal.Origin origin = Animal.Origin.valueOf(originCombo.getSelectionModel().getSelectedItem());
            boolean vaccinated = vaccinatedCheck.isSelected();

            selected.setEarTag(earTag);
            selected.setType(type);
            selected.setGender(gender);
            selected.setWeight(weight);
            selected.setBirthDate(birthDate);
            selected.setEntryDate(entryDate);
            selected.setOrigin(origin);
            selected.setVaccinated(vaccinated);
            serviceAnimal.update(selected);
            refreshTable();
            clearForm();
            updateAnimalBtn.setDisable(true);
            AnimalListRefresh.notifyAnimalChanged();
            deleteAnimalBtn.setDisable(true);
            showInfo("Animal updated successfully.");
        } catch (NumberFormatException e) {
            showError("Invalid number for Ear Tag or Weight.");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        } catch (Exception e) {
            showError("Please fill all required fields: Ear Tag, Type, Gender, Origin.");
        }
    }

    @FXML
    private void onAddAnimal() {
        try {
            int earTag = Integer.parseInt(earTagField.getText().trim());
            Animal.AnimalType type = Animal.AnimalType.valueOf(typeCombo.getSelectionModel().getSelectedItem());
            Animal.Gender gender = Animal.Gender.valueOf(genderCombo.getSelectionModel().getSelectedItem());
            Double weight = weightField.getText().trim().isEmpty() ? null : Double.parseDouble(weightField.getText().trim());
            LocalDate birthDate = birthDatePicker.getValue();
            LocalDate entryDate = entryDatePicker.getValue();
            Animal.Origin origin = Animal.Origin.valueOf(originCombo.getSelectionModel().getSelectedItem());
            boolean vaccinated = vaccinatedCheck.isSelected();

            Animal a = new Animal(earTag, type, gender, weight, null, birthDate, entryDate, origin, vaccinated);
            serviceAnimal.add(a);
            refreshTable();
            clearForm();
            AnimalListRefresh.notifyAnimalChanged();
            showInfo("Animal added successfully.");
        } catch (NumberFormatException e) {
            showError("Invalid number for Ear Tag or Weight.");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        } catch (Exception e) {
            showError("Please fill all required fields: Ear Tag, Type, Gender, Origin.");
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
        typeCombo.getSelectionModel().select(a.getType() != null ? a.getType().name() : null);
        genderCombo.getSelectionModel().select(a.getGender() != null ? a.getGender().name() : null);
        weightField.setText(a.getWeight() != null ? String.valueOf(a.getWeight()) : "");
        birthDatePicker.setValue(a.getBirthDate());
        entryDatePicker.setValue(a.getEntryDate());
        originCombo.getSelectionModel().select(a.getOrigin() != null ? a.getOrigin().name() : null);
        vaccinatedCheck.setSelected(a.getVaccinated() != null && a.getVaccinated());
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
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
