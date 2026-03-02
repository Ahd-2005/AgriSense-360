package controllers;

import entity.Animal;
import entity.AnimalHealthRecord;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import services.ServiceAnimal;
import services.ServiceAnimalHealthRecord;
import services.ServiceEnumManagement;
import services.PdfReportService;
import utils.AnimalListRefresh;

import java.io.BufferedWriter;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OptionsController implements Initializable {

    private static final String BREVO_API_KEY = "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";
    private static final String FROM_EMAIL     = "rinmo7620@gmail.com";
    private static final int    TRAIN_THRESHOLD = 1000;

    @FXML private ListView<String> typesList;
    @FXML private ListView<String> locationsList;
    @FXML private ComboBox<String> animalTypeCombo;
    @FXML private TextField newLocationField;
    @FXML private Button deleteTypeBtn;
    @FXML private Button deleteLocationBtn;

    @FXML private TextField vetEmailField;
    @FXML private TextArea notesArea;
    @FXML private Label emailStatusLabel;

    @FXML private Label recordCountLabel;
    @FXML private Label trainInfoLabel;
    @FXML private Button trainModelBtn;
    @FXML private Label trainStatusLabel;

    @FXML private CheckBox pdfSummaryCheck;
    @FXML private CheckBox pdfAllAnimalsCheck;
    @FXML private CheckBox pdfAtRiskCheck;
    @FXML private CheckBox pdfRecentRecordsCheck;
    @FXML private Label pdfStatusLabel;

    private final ServiceEnumManagement serviceEnum = new ServiceEnumManagement();
    private final ServiceAnimal serviceAnimal = new ServiceAnimal();
    private final ServiceAnimalHealthRecord serviceAnimalHealthRecord = new ServiceAnimalHealthRecord();
    private final ObservableList<String> typesItems = FXCollections.observableArrayList();
    private final ObservableList<String> locationsItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typesList.setItems(typesItems);
        locationsList.setItems(locationsItems);
        typesList.getSelectionModel().selectedItemProperty()
                .addListener((o, old, val) -> deleteTypeBtn.setDisable(val == null));
        locationsList.getSelectionModel().selectedItemProperty()
                .addListener((o, old, val) -> deleteLocationBtn.setDisable(val == null));
        refreshLists();
        loadRecordCount();
        List<String> sorted = defaultFarmAnimals().stream().sorted().collect(Collectors.toList());
        animalTypeCombo.getItems().setAll(sorted);
    }

    private void loadRecordCount() {
        try {
            int count = serviceAnimalHealthRecord.getAll().size();
            recordCountLabel.setText(count + " record" + (count == 1 ? "" : "s"));
            if (count >= TRAIN_THRESHOLD) {
                trainModelBtn.setDisable(false);
                trainInfoLabel.setText("You have enough records to train a custom model.");
                trainInfoLabel.setStyle("-fx-text-fill: #2e7d32;");
            } else {
                trainModelBtn.setDisable(true);
                trainInfoLabel.setText("You need at least " + TRAIN_THRESHOLD + " health records to train a custom model. "
                        +  (TRAIN_THRESHOLD - count) + " more needed.");
                trainInfoLabel.setStyle("-fx-text-fill: #888888;");
            }
        } catch (SQLException e) {
            recordCountLabel.setText("Error loading count");
        }
    }

    private void refreshLists() {
        try {
            typesItems.setAll(serviceEnum.getEnumValues("Animal", "type"));
            locationsItems.setAll(serviceEnum.getEnumValues("Animal", "location"));
        } catch (SQLException e) {
            showError("Could not load lists: " + e.getMessage());
        }
    }

    private List<String> defaultFarmAnimals() {
        return new ArrayList<>(Arrays.asList(
            "Alpaca", "Buffalo", "Camel", "Cattle", "Chicken", "Cow",
            "Donkey", "Duck", "Emu", "Goat", "Goose", "Guinea Fowl",
            "Horse", "Llama", "Mule", "Ostrich", "Pig", "Pigeon",
            "Quail", "Rabbit", "Sheep", "Turkey", "Yak"
        ));
    }

    @FXML
    private void onAddType() {
        String v = animalTypeCombo.getEditor().getText();
        if (v == null || v.trim().isEmpty()) { showError("Select or type an animal type first."); return; }
        try {
            serviceEnum.addEnumValue("Animal", "type", v.trim());
            animalTypeCombo.getEditor().clear();
            animalTypeCombo.setValue(null);
            refreshLists();
            AnimalListRefresh.notifyAnimalChanged();
            showInfo("Type added.");
        } catch (SQLException e) { showError(e.getMessage()); }
    }

    @FXML
    private void onDeleteType() {
        String selected = typesList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            serviceEnum.removeEnumValue("Animal", "type", selected);
            refreshLists();
            AnimalListRefresh.notifyAnimalChanged();
            showInfo("Type removed.");
        } catch (SQLException e) { showError(e.getMessage()); }
    }

    @FXML
    private void onAddLocation() {
        String v = newLocationField.getText();
        if (v == null || v.trim().isEmpty()) { showError("Enter a location name."); return; }
        try {
            serviceEnum.addEnumValue("Animal", "location", v.trim());
            newLocationField.clear();
            refreshLists();
            AnimalListRefresh.notifyAnimalChanged();
            showInfo("Location added.");
        } catch (SQLException e) { showError(e.getMessage()); }
    }

    @FXML
    private void onDeleteLocation() {
        String selected = locationsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            serviceEnum.removeEnumValue("Animal", "location", selected);
            refreshLists();
            AnimalListRefresh.notifyAnimalChanged();
            showInfo("Location removed.");
        } catch (SQLException e) { showError(e.getMessage()); }
    }

    @FXML
    private void onSendVetReport() {
        String vetEmail = vetEmailField.getText().trim();
        String notes    = notesArea.getText().trim();

        if (vetEmail.isEmpty()) {
            setEmailStatus("Please enter the vet email.", false);
            return;
        }

        setEmailStatus("Sending...", null);

        Thread t = new Thread(() -> {
            try {
                List<Animal> atRisk = serviceAnimal.getAll().stream()
                        .filter(a -> {
                            String s = a.getHealthStatus();
                            return s != null && (s.equalsIgnoreCase("sick")
                                    || s.equalsIgnoreCase("injured")
                                    || s.equalsIgnoreCase("critical"));
                        })
                        .collect(Collectors.toList());

                String subject = "Animal Health Report — " + LocalDate.now();
                String body    = buildEmailBody(atRisk, notes);
                sendViaBrevo(BREVO_API_KEY, FROM_EMAIL, vetEmail, subject, body);

                Platform.runLater(() -> setEmailStatus("Report sent successfully.", true));
            } catch (Exception ex) {
                Platform.runLater(() -> setEmailStatus("Failed: " + ex.getMessage(), false));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onTrainModel() {
        trainModelBtn.setDisable(true);
        setTrainStatus("Exporting data...", null);

        Thread t = new Thread(() -> {
            try {
                Path pythonDir = Paths.get(System.getProperty("user.dir"), "src", "main", "python");

                List<Animal> animals = serviceAnimal.getAll();
                List<AnimalHealthRecord> records = serviceAnimalHealthRecord.getAll();

                Map<Integer, Integer> idMap = new HashMap<>();
                for (int i = 0; i < animals.size(); i++) {
                    idMap.put(animals.get(i).getId(), i + 1);
                }

                exportAnimalCsv(pythonDir, animals);
                exportHealthRecordCsv(pythonDir, records, idMap);

                Platform.runLater(() -> setTrainStatus("Data exported. Training model...", null));

                ProcessBuilder pb = new ProcessBuilder("python", "train.py", "--custom");
                pb.directory(pythonDir.toFile());
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = new String(process.getInputStream().readAllBytes());
                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    Platform.runLater(() -> setTrainStatus("Training failed. Check that Python and dependencies are installed.", false));
                } else {
                    Platform.runLater(() -> {
                        setTrainStatus("Model trained and saved. Restart the AI server (api.py) to apply it.", true);
                        trainModelBtn.setDisable(false);
                        AIPredictionController.notifyModelTrained();
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setTrainStatus("Error: " + ex.getMessage(), false);
                    trainModelBtn.setDisable(false);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void exportAnimalCsv(Path pythonDir, List<Animal> animals) throws Exception {
        Path csv = pythonDir.resolve("animal.csv");
        try (BufferedWriter w = Files.newBufferedWriter(csv)) {
            w.write("id,type,vaccinated,weight\n");
            for (int i = 0; i < animals.size(); i++) {
                Animal a = animals.get(i);
                w.write((i + 1) + ","
                        + (a.getType() != null ? a.getType().toLowerCase() : "") + ","
                        + (Boolean.TRUE.equals(a.getVaccinated()) ? 1 : 0) + ","
                        + (a.getWeight() != null ? a.getWeight() : 200.0) + "\n");
            }
        }
    }

    private void exportHealthRecordCsv(Path pythonDir, List<AnimalHealthRecord> records,
                                        Map<Integer, Integer> idMap) throws Exception {
        Path csv = pythonDir.resolve("healthRecord.csv");
        try (BufferedWriter w = Files.newBufferedWriter(csv)) {
            w.write("id,animal,recordDate,weight,appetite,conditionStatus,milkYield,eggCount,woolLength\n");
            for (AnimalHealthRecord r : records) {
                Integer seqId = idMap.get(r.getAnimalId());
                if (seqId == null) continue;
                w.write(r.getId() + ","
                        + seqId + ","
                        + (r.getRecordDate() != null ? r.getRecordDate() : "") + ","
                        + (r.getWeight() != null ? r.getWeight() : "") + ","
                        + (r.getAppetite() != null ? r.getAppetite().name().toLowerCase() : "") + ","
                        + (r.getConditionStatus() != null ? r.getConditionStatus().name().toLowerCase() : "") + ","
                        + (r.getMilkYield() != null ? r.getMilkYield() : "") + ","
                        + (r.getEggCount() != null ? r.getEggCount() : "") + ","
                        + (r.getWoolLength() != null ? r.getWoolLength() : "") + "\n");
            }
        }
    }

    private String buildEmailBody(List<Animal> atRisk, String notes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Animal Health Report\n");
        sb.append("Date: ").append(LocalDate.now()).append("\n\n");

        if (atRisk.isEmpty()) {
            sb.append("Good news — all monitored animals are currently healthy.\n");
        } else {
            sb.append("Animals requiring attention (").append(atRisk.size()).append("):\n");
            sb.append("----------------------------------------\n");
            for (Animal a : atRisk) {
                sb.append("  * Ear Tag #").append(a.getEarTag())
                  .append("  [").append(a.getType() != null ? a.getType() : "unknown").append("]")
                  .append("  Status: ").append(a.getHealthStatus())
                  .append("  Location: ").append(a.getLocation() != null ? a.getLocation() : "-")
                  .append("\n");
            }
        }

        if (!notes.isEmpty()) {
            sb.append("\nAdditional Notes:\n");
            sb.append("----------------------------------------\n");
            sb.append(notes).append("\n");
        }

        sb.append("\n-- Sent from AgriSense 360");
        return sb.toString();
    }

    private void sendViaBrevo(String apiKey, String fromEmail, String toEmail,
                               String subject, String body) throws Exception {
        String safeBody = body
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");

        String json = "{"
                + "\"sender\":{\"name\":\"AgriSense 360\",\"email\":\"" + fromEmail + "\"},"
                + "\"to\":[{\"email\":\"" + toEmail + "\"}],"
                + "\"subject\":\"" + subject + "\","
                + "\"textContent\":\"" + safeBody + "\""
                + "}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode() + " — " + response.body());
        }
    }

    private void setEmailStatus(String msg, Boolean success) {
        emailStatusLabel.setText(msg);
        if (success == null) {
            emailStatusLabel.setStyle("-fx-text-fill: #888888;");
        } else if (success) {
            emailStatusLabel.setStyle("-fx-text-fill: #2e7d32;");
        } else {
            emailStatusLabel.setStyle("-fx-text-fill: #e53935;");
        }
    }

    private void setTrainStatus(String msg, Boolean success) {
        trainStatusLabel.setText(msg);
        if (success == null) {
            trainStatusLabel.setStyle("-fx-text-fill: #888888;");
        } else if (success) {
            trainStatusLabel.setStyle("-fx-text-fill: #2e7d32;");
        } else {
            trainStatusLabel.setStyle("-fx-text-fill: #e53935;");
        }
    }

    @FXML
    private void onExportPdf() {
        boolean summary  = pdfSummaryCheck.isSelected();
        boolean all      = pdfAllAnimalsCheck.isSelected();
        boolean atRisk   = pdfAtRiskCheck.isSelected();
        boolean recent   = pdfRecentRecordsCheck.isSelected();

        if (!summary && !all && !atRisk && !recent) {
            pdfStatusLabel.setText("Selectionnez au moins une section.");
            pdfStatusLabel.setStyle("-fx-text-fill: #e53935;");
            return;
        }

        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Enregistrer le Rapport PDF");
        chooser.setInitialFileName("rapport_ferme_" + LocalDate.now() + ".pdf");
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = chooser.showSaveDialog(pdfSummaryCheck.getScene().getWindow());
        if (file == null) return;

        pdfStatusLabel.setText("Generation en cours...");
        pdfStatusLabel.setStyle("-fx-text-fill: #888888;");

        Thread t = new Thread(() -> {
            try {
                List<Animal> animals = serviceAnimal.getAll();
                List<AnimalHealthRecord> records = recent
                        ? serviceAnimalHealthRecord.getAll().stream()
                                .filter(r -> r.getRecordDate() != null)
                                .sorted((a, b) -> b.getRecordDate().compareTo(a.getRecordDate()))
                                .limit(20)
                                .collect(Collectors.toList())
                        : new ArrayList<>();

                new PdfReportService().generateFarmReport(
                        summary, all, atRisk, recent, animals, records, file);

                Platform.runLater(() -> {
                    pdfStatusLabel.setText("Rapport genere : " + file.getName());
                    pdfStatusLabel.setStyle("-fx-text-fill: #2e7d32;");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    pdfStatusLabel.setText("Erreur : " + ex.getMessage());
                    pdfStatusLabel.setStyle("-fx-text-fill: #e53935;");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void showInfo(String msg)  { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }
}
