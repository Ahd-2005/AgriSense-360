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

    // Inline validation labels
    @FXML private Label earTagError;
    @FXML private Label typeError;
    @FXML private Label genderError;
    @FXML private Label weightError;
    @FXML private Label birthDateError;
    @FXML private Label entryDateError;
    @FXML private Label originError;
    @FXML private Label locationError;

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


        birthDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isAfter(LocalDate.now())) {
                setError(birthDateError, "Are you living in the future?");
            } else {
                setError(birthDateError, "");

                LocalDate entry = entryDatePicker.getValue();
                if (entry != null && newVal != null && entry.isBefore(newVal)) {
                    setError(entryDateError, "Entry date cannot be before birth date.");
                } else if (entry != null) {
                    setError(entryDateError, "");
                }
            }
        });
        entryDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) { setError(entryDateError, ""); return; }
            if (newVal.isAfter(LocalDate.now())) {
                setError(entryDateError, "Entry date cannot be in the future.");
            } else {
                LocalDate birth = birthDatePicker.getValue();
                if (birth != null && newVal.isBefore(birth)) {
                    setError(entryDateError, "Entry date cannot be before birth date.");
                } else {
                    setError(entryDateError, "");
                }
            }
        });

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
        
        int rowIdx = 0;
        for (Animal a : sortedList) {
            HBox row = createDataRow(a, rowIdx++);
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

    private HBox createDataRow(Animal a, int rowIndex) {
        HBox row = new HBox();
        row.getStyleClass().add(rowIndex % 2 == 0 ? "mgmt-table-row" : "mgmt-table-row-alt");
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
            cell.setPrefWidth(widths[i]);
            cell.setMinWidth(widths[i]);
            cell.setAlignment(Pos.CENTER_LEFT);
            if (i == 5) {

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



    private void setError(Label label, String msg) {
        label.setText(msg);
        boolean hasError = msg != null && !msg.isEmpty();
        label.setVisible(hasError);
        label.setManaged(hasError);
    }

    private void clearErrors() {
        setError(earTagError,   "");
        setError(typeError,     "");
        setError(genderError,   "");
        setError(weightError,   "");
        setError(birthDateError,"");
        setError(entryDateError,"");
        setError(originError,   "");
        setError(locationError, "");
    }

    private boolean validate() {
        clearErrors();
        boolean valid = true;


        String earTagText = earTagField.getText().trim();
        if (earTagText.isEmpty()) {
            setError(earTagError, "Ear tag is required.");
            valid = false;
        } else {
            try {
                int tag = Integer.parseInt(earTagText);
                if (tag <= 0) {
                    setError(earTagError, "Ear tag must be a positive number.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                setError(earTagError, "Ear tag must be a whole number.");
                valid = false;
            }
        }

        // Type: required
        if (typeCombo.getSelectionModel().isEmpty()) {
            setError(typeError, "Please select a type.");
            valid = false;
        }

        // Gender: required
        if (genderCombo.getSelectionModel().isEmpty()) {
            setError(genderError, "Please select a gender.");
            valid = false;
        }

        // Weight: optional, must be non-negative if provided
        String weightText = weightField.getText().trim();
        if (!weightText.isEmpty()) {
            try {
                double w = Double.parseDouble(weightText);
                if (w < 0) {
                    setError(weightError, "Weight cannot be negative.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                setError(weightError, "Weight must be a valid number (e.g. 320.5).");
                valid = false;
            }
        }

        // Birth date: must not be in the future
        LocalDate birthDate = birthDatePicker.getValue();
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            setError(birthDateError, "Birth date cannot be in the future.");
            valid = false;
        }

        // Entry date: must not be before birth date; must not be in the future
        LocalDate entryDate = entryDatePicker.getValue();
        if (entryDate != null) {
            if (entryDate.isAfter(LocalDate.now())) {
                setError(entryDateError, "Entry date cannot be in the future.");
                valid = false;
            } else if (birthDate != null && entryDate.isBefore(birthDate)) {
                setError(entryDateError, "Entry date cannot be before birth date.");
                valid = false;
            }
        }

        // Origin: required
        if (originCombo.getSelectionModel().isEmpty()) {
            setError(originError, "Please select an origin.");
            valid = false;
        }

        // Location: required
        if (locationCombo.getSelectionModel().isEmpty()) {
            setError(locationError, "Please select a location.");
            valid = false;
        }

        return valid;
    }

    // ───────────────────────────────────────────────────────────

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
        if (!validate()) return;
        try {
            int earTag = Integer.parseInt(earTagField.getText().trim());
            String type = typeCombo.getSelectionModel().getSelectedItem();
            Animal.Gender gender = Animal.Gender.valueOf(genderCombo.getSelectionModel().getSelectedItem());
            Double weight = weightField.getText().trim().isEmpty() ? null
                    : Double.parseDouble(weightField.getText().trim());
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
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void onAddAnimal() {
        if (!validate()) return;
        try {
            int earTag = Integer.parseInt(earTagField.getText().trim());
            String type = typeCombo.getSelectionModel().getSelectedItem();
            Animal.Gender gender = Animal.Gender.valueOf(genderCombo.getSelectionModel().getSelectedItem());
            Double weight = weightField.getText().trim().isEmpty() ? null
                    : Double.parseDouble(weightField.getText().trim());
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
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
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
        clearErrors();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
