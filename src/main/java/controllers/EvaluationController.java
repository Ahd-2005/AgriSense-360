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
import javafx.scene.input.KeyEvent;
import entity.AffectationTravail;
import entity.EvaluationPerformance;
import services.AffectationTravailService;
import services.AIReportService;
import services.EvaluationPerformanceService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class EvaluationController implements Initializable {

    @FXML private ComboBox<AffectationTravail> cbAffectation;
    @FXML private TextField tfNote;
    @FXML private ComboBox<String> cbQualite;
    @FXML private TextField tfCommentaire;
    @FXML private DatePicker dpDateEvaluation;
    @FXML private ComboBox<String> cbSearchQualite;
    @FXML private ComboBox<String> cbSortDate;
    @FXML private TableView<EvaluationPerformance> tvEvaluations;
    @FXML private TableColumn<EvaluationPerformance, Integer> colIdEval;
    @FXML private TableColumn<EvaluationPerformance, Integer> colIdAffect;
    @FXML private TableColumn<EvaluationPerformance, Integer> colNote;
    @FXML private TableColumn<EvaluationPerformance, String> colQualite;
    @FXML private TableColumn<EvaluationPerformance, String> colCommentaire;
    @FXML private TableColumn<EvaluationPerformance, LocalDate> colDateEval;
    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnClear;
    @FXML private Button btnRapportAI;
    @FXML private Label lblAverageNote;

    private final AffectationTravailService affectationService = new AffectationTravailService();
    private final EvaluationPerformanceService evaluationService = new EvaluationPerformanceService();
    private final ObservableList<EvaluationPerformance> evaluationList = FXCollections.observableArrayList();

    @FXML
    private void onSwitchToAffectation() {
        MainLayoutController.getInstance().navigateToWorkers();
    }
    private FilteredList<EvaluationPerformance> filteredList;
    private EvaluationPerformance selectedEvaluation;

    private static final String[] QUALITE_OPTIONS = {"", "Faible", "Moyenne", "Bonne"};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbQualite.getItems().setAll(QUALITE_OPTIONS);
        cbSearchQualite.getItems().addAll("", "Faible", "Moyenne", "Bonne");
        cbSearchQualite.setValue("");
        cbSortDate.getItems().addAll("Récent au plus ancien", "Ancien au plus récent");
        cbSortDate.setValue("Récent au plus ancien");

        tfNote.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (!e.getCharacter().matches("[0-9]")) {
                e.consume();
            }
        });

        colIdEval.setCellValueFactory(new PropertyValueFactory<>("idEvaluation"));
        colIdAffect.setCellValueFactory(new PropertyValueFactory<>("idAffectation"));
        colNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        colQualite.setCellValueFactory(new PropertyValueFactory<>("qualite"));
        colCommentaire.setCellValueFactory(new PropertyValueFactory<>("commentaire"));
        colDateEval.setCellValueFactory(new PropertyValueFactory<>("dateEvaluation"));

        cbAffectation.setConverter(new javafx.util.StringConverter<AffectationTravail>() {
            @Override
            public String toString(AffectationTravail a) {
                return a == null ? "" : "ID " + a.getIdAffectation() + " - " + a.getTypeTravail() + " (" + a.getDateDebut() + ")";
            }
            @Override
            public AffectationTravail fromString(String s) { return null; }
        });

        // Setup FilteredList
        filteredList = new FilteredList<>(evaluationList, p -> true);
        
        // Setup SortedList
        SortedList<EvaluationPerformance> sortedList = new SortedList<>(filteredList);
        tvEvaluations.setItems(sortedList);
        
        tvEvaluations.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedEvaluation = newVal;
            if (newVal != null) {
                loadEvaluationIntoForm(newVal);
                updateAverageLabel(newVal.getIdAffectation());
            } else {
                lblAverageNote.setText("");
            }
        });

        // Setup quality filter listener
        cbSearchQualite.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        
        // Setup sort listener
        cbSortDate.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        loadAffectations();
        loadAll();
    }

    private void loadAffectations() {
        try {
            List<AffectationTravail> list = affectationService.getAll();
            cbAffectation.getItems().clear();
            cbAffectation.getItems().addAll(list);
        } catch (SQLException e) {
            System.err.println("Error loading affectations: " + e.getMessage());
            showError("Erreur", "Impossible de charger les affectations: " + e.getMessage());
        }
    }

    private void loadEvaluationIntoForm(EvaluationPerformance e) {
        try {
            AffectationTravail a = affectationService.getById(e.getIdAffectation());
            cbAffectation.setValue(a);
        } catch (SQLException ex) {
            cbAffectation.setValue(null);
        }
        tfNote.setText(String.valueOf(e.getNote()));
        cbQualite.setValue(e.getQualite() != null && !e.getQualite().isEmpty() ? e.getQualite() : "");
        tfCommentaire.setText(e.getCommentaire());
        dpDateEvaluation.setValue(e.getDateEvaluation());
    }

    private void updateAverageLabel(int idAffectation) {
        try {
            double avg = evaluationService.averageNoteByAffectation(idAffectation);
            lblAverageNote.setText("Moyenne des notes pour cette affectation: " + String.format("%.2f", avg));
        } catch (SQLException e) {
            lblAverageNote.setText("");
        }
    }

    private void loadAll() {
        try {
            List<EvaluationPerformance> list = evaluationService.getAll();
            evaluationList.clear();
            evaluationList.addAll(list);
            applyFilters();
        } catch (SQLException e) {
            System.err.println("Error loading evaluations: " + e.getMessage());
            showError("Erreur", "Impossible de charger les évaluations: " + e.getMessage());
        }
    }

    private void applyFilters() {
        String selectedQualite = cbSearchQualite.getValue();
        String sortOption = cbSortDate.getValue();

        // Apply quality filter
        filteredList.setPredicate(evaluation -> {
            if (selectedQualite == null || selectedQualite.isEmpty()) {
                return true;
            }
            return evaluation.getQualite() != null && evaluation.getQualite().equals(selectedQualite);
        });

        // Apply sort by date
        ObservableList<EvaluationPerformance> items = tvEvaluations.getItems();
        if (items instanceof SortedList) {
            SortedList<EvaluationPerformance> sortedList = (SortedList<EvaluationPerformance>) items;
            if ("Récent au plus ancien".equals(sortOption)) {
                sortedList.setComparator((e1, e2) -> e2.getDateEvaluation().compareTo(e1.getDateEvaluation()));
            } else if ("Ancien au plus récent".equals(sortOption)) {
                sortedList.setComparator((e1, e2) -> e1.getDateEvaluation().compareTo(e2.getDateEvaluation()));
            }
        }
    }

    @FXML
    private void onResetFilters() {
        cbSearchQualite.setValue("");
        cbSortDate.setValue("Récent au plus ancien");
        applyFilters();
    }

    @FXML
    private void onAdd() {
        if (!validate()) return;
        EvaluationPerformance e = formToModel(null);
        try {
            evaluationService.add(e);
            showInfo("Succès", "Évaluation ajoutée.");
            loadAll();
            clearForm();
        } catch (SQLException ex) {
            System.err.println("Error adding: " + ex.getMessage());
            showError("Erreur", "Impossible d'ajouter: " + ex.getMessage());
        }
    }

    @FXML
    private void onUpdate() {
        if (selectedEvaluation == null) {
            showError("Erreur", "Veuillez sélectionner une évaluation à modifier.");
            return;
        }
        if (!validate()) return;
        EvaluationPerformance e = formToModel(selectedEvaluation.getIdEvaluation());
        try {
            evaluationService.update(e);
            showInfo("Succès", "Évaluation modifiée.");
            loadAll();
            clearForm();
            selectedEvaluation = null;
        } catch (SQLException ex) {
            System.err.println("Error updating: " + ex.getMessage());
            showError("Erreur", "Impossible de modifier: " + ex.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        if (selectedEvaluation == null) {
            showError("Erreur", "Veuillez sélectionner une évaluation à supprimer.");
            return;
        }
        try {
            evaluationService.delete(selectedEvaluation.getIdEvaluation());
            showInfo("Succès", "Évaluation supprimée.");
            loadAll();
            clearForm();
            selectedEvaluation = null;
        } catch (SQLException ex) {
            System.err.println("Error deleting: " + ex.getMessage());
            showError("Erreur", "Impossible de supprimer: " + ex.getMessage());
        }
    }

    @FXML
    private void onClear() {
        clearForm();
        selectedEvaluation = null;
        lblAverageNote.setText("");
    }

    @FXML
    private void onGenerateRapport() {
        AffectationTravail affectation = cbAffectation.getValue();
        if (affectation == null) {
            showError("Rapport IA", "Sélectionnez d'abord une affectation dans le formulaire.");
            return;
        }

        btnRapportAI.setDisable(true);
        btnRapportAI.setText("⏳ Génération...");

        final int idAffectation = affectation.getIdAffectation();
        final AffectationTravail affectationCopy = affectation;

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                List<EvaluationPerformance> evals = evaluationService.getByAffectation(idAffectation);
                return AIReportService.generatePerformanceReport(affectationCopy, evals);
            }
        };

        task.setOnSucceeded(e -> {
            btnRapportAI.setDisable(false);
            btnRapportAI.setText("🤖 Rapport IA");
            showRapportDialog(affectationCopy, task.getValue());
        });

        task.setOnFailed(e -> {
            btnRapportAI.setDisable(false);
            btnRapportAI.setText("🤖 Rapport IA");
            showError("Erreur", "Impossible de générer le rapport IA.");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showRapportDialog(AffectationTravail affectation, String rapport) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Rapport IA — " + affectation.getTypeTravail());
        dialog.setHeaderText("📊 Analyse de performance IA\nAffectation : "
                + affectation.getTypeTravail() + "  |  Zone : " + affectation.getZoneTravail());

        TextArea ta = new TextArea(rapport);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefSize(580, 360);
        ta.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px;"
                + " -fx-background-color: #fafafa; -fx-border-color: #ddd;");

        dialog.getDialogPane().setContent(ta);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(620);
        dialog.showAndWait();
    }

    private void clearForm() {
        cbAffectation.setValue(null);
        tfNote.clear();
        cbQualite.setValue("");
        tfCommentaire.clear();
        dpDateEvaluation.setValue(null);
    }

    private EvaluationPerformance formToModel(Integer id) {
        EvaluationPerformance e = new EvaluationPerformance();
        if (id != null) e.setIdEvaluation(id);
        AffectationTravail a = cbAffectation.getValue();
        if (a != null) e.setIdAffectation(a.getIdAffectation());
        String noteStr = tfNote.getText() != null ? tfNote.getText().trim() : "";
        e.setNote(noteStr.isEmpty() ? 0 : Integer.parseInt(noteStr));
        e.setQualite(cbQualite.getValue() != null ? cbQualite.getValue() : "");
        e.setCommentaire(tfCommentaire.getText() != null ? tfCommentaire.getText().trim() : "");
        e.setDateEvaluation(dpDateEvaluation.getValue());
        return e;
    }

    private boolean validate() {
        AffectationTravail a = cbAffectation.getValue();
        if (a == null) {
            showError("Validation", "Veuillez sélectionner une affectation.");
            cbAffectation.requestFocus();
            return false;
        }
        String noteStr = tfNote.getText() != null ? tfNote.getText().trim() : "";
        if (noteStr.isEmpty()) {
            showError("Validation", "La note est requise.");
            tfNote.requestFocus();
            return false;
        }
        int note;
        try {
            note = Integer.parseInt(noteStr);
        } catch (NumberFormatException ex) {
            showError("Validation", "La note doit être un nombre entre 1 et 5.");
            return false;
        }
        if (note < 1 || note > 5) {
            showError("Validation", "La note doit être entre 1 et 5.");
            tfNote.requestFocus();
            return false;
        }
        String qualite = cbQualite.getValue() != null ? cbQualite.getValue().trim() : "";
        if (!qualite.isEmpty() && !qualite.equals("Faible") && !qualite.equals("Moyenne") && !qualite.equals("Bonne")) {
            showError("Validation", "La qualité doit être Faible, Moyenne ou Bonne (ou vide).");
            cbQualite.requestFocus();
            return false;
        }
        return true;
    }

    private void showError(String title, String message) {
        Alert al = new Alert(Alert.AlertType.ERROR);
        al.setTitle(title);
        al.setHeaderText(null);
        al.setContentText(message);
        al.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert al = new Alert(Alert.AlertType.INFORMATION);
        al.setTitle(title);
        al.setHeaderText(null);
        al.setContentText(message);
        al.showAndWait();
    }
}
