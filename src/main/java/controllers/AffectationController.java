package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import entity.AffectationTravail;
import services.AffectationTravailService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class AffectationController implements Initializable {

    @FXML private TextField tfTypeTravail;
    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private TextField tfZoneTravail;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TableView<AffectationTravail> tvAffectations;
    @FXML private TableColumn<AffectationTravail, Integer> colId;
    @FXML private TableColumn<AffectationTravail, String> colTypeTravail;
    @FXML private TableColumn<AffectationTravail, LocalDate> colDateDebut;
    @FXML private TableColumn<AffectationTravail, LocalDate> colDateFin;
    @FXML private TableColumn<AffectationTravail, String> colZoneTravail;
    @FXML private TableColumn<AffectationTravail, String> colStatut;
    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnClear;
    @FXML
    private Button btnGoEvaluation;


    private final AffectationTravailService service = new AffectationTravailService();
    private final ObservableList<AffectationTravail> affectationList = FXCollections.observableArrayList();
    private AffectationTravail selectedAffectation;

    private static final String[] STATUT_OPTIONS = {"En cours", "Terminée", "Annulée", "En attente"};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbStatut.getItems().setAll(STATUT_OPTIONS);

        colId.setCellValueFactory(new PropertyValueFactory<>("idAffectation"));
        colTypeTravail.setCellValueFactory(new PropertyValueFactory<>("typeTravail"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colZoneTravail.setCellValueFactory(new PropertyValueFactory<>("zoneTravail"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        tvAffectations.setItems(affectationList);
        tvAffectations.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedAffectation = newVal;
            if (newVal != null) {
                loadAffectationIntoForm(newVal);
            }
        });

        loadAll();
    }

    private void loadAffectationIntoForm(AffectationTravail a) {
        tfTypeTravail.setText(a.getTypeTravail());
        dpDateDebut.setValue(a.getDateDebut());
        dpDateFin.setValue(a.getDateFin());
        tfZoneTravail.setText(a.getZoneTravail());
        cbStatut.setValue(a.getStatut());
    }

    private void loadAll() {
        try {
            List<AffectationTravail> list = service.getAll();
            affectationList.clear();
            affectationList.addAll(list);
        } catch (SQLException e) {
            System.err.println("Error loading affectations: " + e.getMessage());
            showError("Erreur", "Impossible de charger les affectations: " + e.getMessage());
        }
    }

    @FXML
    private void onAdd() {
        if (!validate()) return;
        AffectationTravail a = formToModel(null);
        try {
            service.add(a);
            showInfo("Succès", "Affectation ajoutée.");
            loadAll();
            clearForm();
        } catch (SQLException e) {
            System.err.println("Error adding: " + e.getMessage());
            showError("Erreur", "Impossible d'ajouter: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdate() {
        if (selectedAffectation == null) {
            showError("Erreur", "Veuillez sélectionner une affectation à modifier.");
            return;
        }
        if (!validate()) return;
        AffectationTravail a = formToModel(selectedAffectation.getIdAffectation());
        try {
            service.update(a);
            showInfo("Succès", "Affectation modifiée.");
            loadAll();
            clearForm();
            selectedAffectation = null;
        } catch (SQLException e) {
            System.err.println("Error updating: " + e.getMessage());
            showError("Erreur", "Impossible de modifier: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        if (selectedAffectation == null) {
            showError("Erreur", "Veuillez sélectionner une affectation à supprimer.");
            return;
        }
        try {
            service.delete(selectedAffectation.getIdAffectation());
            showInfo("Succès", "Affectation supprimée.");
            loadAll();
            clearForm();
            selectedAffectation = null;
        } catch (SQLException e) {
            System.err.println("Error deleting: " + e.getMessage());
            showError("Erreur", "Impossible de supprimer: " + e.getMessage());
        }
    }

    @FXML
    private void onClear() {
        clearForm();
        selectedAffectation = null;
    }

    private void clearForm() {
        tfTypeTravail.clear();
        dpDateDebut.setValue(null);
        dpDateFin.setValue(null);
        tfZoneTravail.clear();
        cbStatut.setValue(null);
    }

    private AffectationTravail formToModel(Integer id) {
        AffectationTravail a = new AffectationTravail();
        if (id != null) a.setIdAffectation(id);
        a.setTypeTravail(tfTypeTravail.getText() != null ? tfTypeTravail.getText().trim() : "");
        a.setDateDebut(dpDateDebut.getValue());
        a.setDateFin(dpDateFin.getValue());
        a.setZoneTravail(tfZoneTravail.getText() != null ? tfZoneTravail.getText().trim() : "");
        a.setStatut(cbStatut.getValue());
        return a;
    }

    private boolean validate() {
        String type = tfTypeTravail.getText() != null ? tfTypeTravail.getText().trim() : "";
        if (type.length() < 3) {
            showError("Validation", "Le type de travail est requis et doit contenir au moins 3 caractères.");
            tfTypeTravail.requestFocus();
            return false;
        }
        LocalDate dateDebut = dpDateDebut.getValue();
        LocalDate dateFin = dpDateFin.getValue();
        if (dateDebut == null || dateFin == null) {
            showError("Validation", "Les dates de début et de fin sont requises.");
            return false;
        }
        if (!dateFin.isAfter(dateDebut) && !dateFin.isEqual(dateDebut)) {
            showError("Validation", "La date de fin doit être >= à la date de début.");
            return false;
        }
        return true;
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
    @FXML
    private void goToEvaluation() {
        MainLayoutController mainLayout =
                MainLayoutController.getInstance();

        if (mainLayout != null) {
            mainLayout.navigateToEvaluation();
        } else {
            showError("Navigation", "Main layout non disponible.");
        }



    }

}
