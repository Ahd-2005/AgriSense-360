package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import entity.AffectationTravail;
import services.AffectationTravailService;
import services.AIReportService;
import services.DiscordWebhookService;
import services.WeatherService;

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
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbSort;
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
    @FXML private Button btnCheckMeteo;
    @FXML private Button btnAIPlan;
    @FXML private Label lblMeteoResult;
    @FXML private Label lblAIPlanResult;

    private final AffectationTravailService service = new AffectationTravailService();
    private final ObservableList<AffectationTravail> affectationList = FXCollections.observableArrayList();
    private FilteredList<AffectationTravail> filteredList;
    private AffectationTravail selectedAffectation;

    private static final String[] STATUT_OPTIONS = {"En cours", "Terminée", "Annulée", "En attente"};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbStatut.getItems().setAll(STATUT_OPTIONS);
        cbSort.getItems().addAll("Récent au plus ancien", "Ancien au plus récent");
        cbSort.setValue("Récent au plus ancien");

        colId.setCellValueFactory(new PropertyValueFactory<>("idAffectation"));
        colTypeTravail.setCellValueFactory(new PropertyValueFactory<>("typeTravail"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colZoneTravail.setCellValueFactory(new PropertyValueFactory<>("zoneTravail"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Setup FilteredList
        filteredList = new FilteredList<>(affectationList, p -> true);
        
        // Setup SortedList (custom sorting via applyFilters, not TableView column sorting)
        SortedList<AffectationTravail> sortedList = new SortedList<>(filteredList,
                (a1, a2) -> a2.getDateDebut().compareTo(a1.getDateDebut()));
        tvAffectations.setItems(sortedList);
        
        tvAffectations.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedAffectation = newVal;
            if (newVal != null) {
                loadAffectationIntoForm(newVal);
            }
        });

        // Setup search listener
        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        
        // Setup sort listener
        cbSort.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

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
            applyFilters();
        } catch (SQLException e) {
            System.err.println("Error loading affectations: " + e.getMessage());
            showError("Erreur", "Impossible de charger les affectations: " + e.getMessage());
        }
    }

    private void applyFilters() {
        String searchText = tfSearch.getText().toLowerCase().trim();
        String sortOption = cbSort.getValue();

        // Apply filter
        filteredList.setPredicate(affectation -> {
            if (searchText.isEmpty()) {
                return true;
            }
            return affectation.getTypeTravail().toLowerCase().contains(searchText);
        });

        // Apply sort
        ObservableList<AffectationTravail> items = tvAffectations.getItems();
        if (items instanceof SortedList) {
            SortedList<AffectationTravail> sortedList = (SortedList<AffectationTravail>) items;
            if ("Récent au plus ancien".equals(sortOption)) {
                sortedList.setComparator((a1, a2) -> a2.getDateDebut().compareTo(a1.getDateDebut()));
            } else if ("Ancien au plus récent".equals(sortOption)) {
                sortedList.setComparator((a1, a2) -> a1.getDateDebut().compareTo(a2.getDateDebut()));
            }
        }
    }

    @FXML
    private void onResetFilters() {
        tfSearch.clear();
        cbSort.setValue("Récent au plus ancien");
        applyFilters();
    }

    @FXML
    private void onAdd() {
        if (!validate()) return;
        AffectationTravail a = formToModel(null);
        try {
            service.add(a);
            showInfo("Succès", "Affectation ajoutée.");
            DiscordWebhookService.sendAffectationNotification("created", a.getTypeTravail(), a.getZoneTravail(), a.getStatut(), "Nouvelle affectation ajoutée");
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
            DiscordWebhookService.sendAffectationNotification("updated", a.getTypeTravail(), a.getZoneTravail(), a.getStatut(), "Affectation modifiée avec succès");
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
            DiscordWebhookService.sendAffectationNotification("deleted", selectedAffectation.getTypeTravail(), selectedAffectation.getZoneTravail(), selectedAffectation.getStatut(), "Affectation supprimée");
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

    @FXML
    private void onCheckMeteo() {
        String zone = tfZoneTravail.getText() != null ? tfZoneTravail.getText().trim() : "";
        if (zone.isEmpty()) {
            lblMeteoResult.setText("⚠️ Renseignez d'abord la zone de travail.");
            lblMeteoResult.setStyle("-fx-text-fill: #d68910; -fx-font-weight: bold;");
            return;
        }

        lblMeteoResult.setText("⏳ Chargement de la météo pour « " + zone + " »...");
        lblMeteoResult.setStyle("-fx-text-fill: #555;");
        btnCheckMeteo.setDisable(true);

        Task<WeatherService.WeatherInfo> task = new Task<>() {
            @Override
            protected WeatherService.WeatherInfo call() {
                return WeatherService.fetchWeather(zone);
            }
        };

        task.setOnSucceeded(e -> {
            WeatherService.WeatherInfo info = task.getValue();
            btnCheckMeteo.setDisable(false);
            if (info.success) {
                String text = info.getSummary() + "\n" + info.getSuitabilityLabel();
                lblMeteoResult.setText(text);
                lblMeteoResult.setStyle("-fx-text-fill: " + info.getSuitabilityColor()
                        + "; -fx-font-weight: bold; -fx-font-size: 12px;");

                if ("DECONSEILLE".equals(info.suitability)) {
                    showInfo("Alerte Météo ☁️",
                            "Les conditions météo à « " + info.cityName + " » sont défavorables.\n"
                            + info.getSummary() + "\n\nIl est déconseillé de planifier un travail agricole.");
                }
            } else {
                lblMeteoResult.setText("❌ " + info.errorMessage);
                lblMeteoResult.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
            }
        });

        task.setOnFailed(e -> {
            btnCheckMeteo.setDisable(false);
            lblMeteoResult.setText("❌ Erreur inattendue lors de la vérification météo.");
            lblMeteoResult.setStyle("-fx-text-fill: #c0392b;");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void clearForm() {
        tfTypeTravail.clear();
        dpDateDebut.setValue(null);
        dpDateFin.setValue(null);
        tfZoneTravail.clear();
        cbStatut.setValue(null);
        lblMeteoResult.setText("");
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

    // ── AI Planning ────────────────────────────────────────────────

    @FXML
    private void onAIPlan() {
        if (selectedAffectation == null) {
            showError("Sélection requise", "Veuillez sélectionner une affectation pour la planification IA.");
            return;
        }
        lblAIPlanResult.setText("⏳ Génération du plan IA en cours...");
        btnAIPlan.setDisable(true);

        AffectationTravail copy = selectedAffectation;
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return AIReportService.generatePerformanceReport(copy, java.util.Collections.emptyList());
            }
        };
        task.setOnSucceeded(e -> {
            lblAIPlanResult.setText(task.getValue());
            btnAIPlan.setDisable(false);
        });
        task.setOnFailed(e -> {
            lblAIPlanResult.setText("❌ Erreur: " + task.getException().getMessage());
            btnAIPlan.setDisable(false);
        });
        new Thread(task).start();
    }

    // ── Navigation ─────────────────────────────────────────────────

    @FXML
    private void onSwitchToEvaluation() {
        MainLayoutController.getInstance().navigateToEvaluation();
    }

    @FXML
    private void onSwitchToDashboard() {
        MainLayoutController.getInstance().navigateToDashboardWorkers();
    }

    @FXML
    private void onSwitchToCalendar() {
        MainLayoutController.getInstance().navigateToCalendar();
    }
}
