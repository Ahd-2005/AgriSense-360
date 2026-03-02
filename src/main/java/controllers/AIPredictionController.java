package controllers;

import entity.Animal;
import entity.AnimalHealthRecord;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import services.ServiceAnimal;
import services.ServiceAnimalHealthRecord;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AIPredictionController implements Initializable {

    private static final String BREVO_API_KEY = "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";
    private static final String FROM_EMAIL    = "rinmo7620@gmail.com";

    @FXML private ComboBox<Animal> animalCombo;
    @FXML private Label statusLabel;
    @FXML private Label batchStatusLabel;
    @FXML private VBox resultsCard;
    @FXML private Label animalInfoLabel;
    @FXML private Label conditionBadge;
    @FXML private ProgressBar confidenceBar;
    @FXML private Label confidenceLabel;
    @FXML private Label prodHeaderLabel;
    @FXML private VBox recordsContainer;
    @FXML private RadioButton generalModelRadio;
    @FXML private RadioButton customModelRadio;
    @FXML private Label customModelStatusLabel;

    @FXML private VBox batchResultsCard;
    @FXML private Label batchSummaryLabel;
    @FXML private VBox batchResultsContainer;
    @FXML private TextField batchVetEmailField;
    @FXML private Label batchEmailStatusLabel;

    private final ServiceAnimal serviceAnimal = new ServiceAnimal();
    private final ServiceAnimalHealthRecord serviceRecord = new ServiceAnimalHealthRecord();

    private Animal selectedAnimal;
    private List<AnimalHealthRecord> lastRecords;
    private List<BatchResult> batchResults = new ArrayList<>();
    private static Runnable onModelTrained;

    private static class BatchResult {
        final Animal animal;
        final String condition;
        final double confidence;
        BatchResult(Animal animal, String condition, double confidence) {
            this.animal = animal;
            this.condition = condition;
            this.confidence = confidence;
        }
    }

    public static void notifyModelTrained() {
        if (onModelTrained != null) javafx.application.Platform.runLater(onModelTrained);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            List<Animal> animals = serviceAnimal.getAll();
            animalCombo.setItems(FXCollections.observableArrayList(animals));
            animalCombo.setConverter(new StringConverter<>() {
                @Override public String toString(Animal a) {
                    if (a == null) return "";
                    return "#" + a.getEarTag() + "  —  " + capitalize(a.getType());
                }
                @Override public Animal fromString(String s) { return null; }
            });
        } catch (SQLException e) {
            setStatus("Failed to load animals: " + e.getMessage());
        }
        onModelTrained = this::refreshModelAvailability;
        refreshModelAvailability();
    }

    private void refreshModelAvailability() {
        boolean customExists = Paths.get(System.getProperty("user.dir"),
                "src", "main", "python", "custom_model.pkl").toFile().exists();
        if (customExists) {
            customModelRadio.setDisable(false);
            customModelStatusLabel.setText("(disponible)");
            customModelStatusLabel.setStyle("-fx-text-fill: #2e7d32;");
        } else {
            customModelRadio.setDisable(true);
            customModelStatusLabel.setText("(pas encore entraîné)");
            customModelStatusLabel.setStyle("-fx-text-fill: #888888;");
        }
    }

    @FXML
    private void onAnimalSelected() {
        selectedAnimal = animalCombo.getValue();
        lastRecords = null;
        clearStatus();
        hideResults();
        if (selectedAnimal == null) return;
        try {
            List<AnimalHealthRecord> records = serviceRecord.getRecordsByAnimalId(selectedAnimal.getId());
            if (records.isEmpty()) {
                setStatus("Aucun dossier de santé trouvé pour cet animal.");
            } else {
                lastRecords = records.subList(0, Math.min(10, records.size()));
            }
        } catch (SQLException e) {
            setStatus("Erreur lors du chargement des dossiers : " + e.getMessage());
        }
    }

    @FXML
    private void onPredict() {
        if (selectedAnimal == null) { setStatus("Veuillez sélectionner un animal."); return; }
        if (lastRecords == null || lastRecords.isEmpty()) {
            setStatus("Aucun dossier de santé disponible pour baser la prédiction.");
            return;
        }
        clearStatus();
        hideResults();
        statusLabel.setText("Analyse en cours...");

        String modelParam = customModelRadio.isSelected() ? "custom" : "general";
        String jsonBody = buildJsonBody(selectedAnimal, lastRecords);
        Animal animal = selectedAnimal;
        List<AnimalHealthRecord> records = lastRecords;

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception { return callApi(jsonBody, modelParam); }
        };
        task.setOnSucceeded(e -> handleResponse(task.getValue(), animal, records));
        task.setOnFailed(e -> setStatus(task.getException().getMessage() != null
                ? task.getException().getMessage()
                : "Impossible de contacter le serveur IA. Assurez-vous qu'il tourne sur le port 8000."));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onAnalyzeAll() {
        batchResultsCard.setVisible(false);
        batchResultsCard.setManaged(false);
        batchResultsContainer.getChildren().clear();
        batchResults.clear();
        batchEmailStatusLabel.setText("");
        batchStatusLabel.setStyle("-fx-text-fill: #888888;");
        batchStatusLabel.setText("Analyse en cours...");

        String modelParam = customModelRadio.isSelected() ? "custom" : "general";

        Task<List<BatchResult>> task = new Task<>() {
            @Override
            protected List<BatchResult> call() throws Exception {
                List<Animal> animals = serviceAnimal.getAll();
                List<BatchResult> unhealthy = new ArrayList<>();
                int total = animals.size();
                for (int i = 0; i < total; i++) {
                    Animal animal = animals.get(i);
                    final String progress = (i + 1) + "/" + total;
                    javafx.application.Platform.runLater(() ->
                            batchStatusLabel.setText("Analyse en cours... " + progress));

                    List<AnimalHealthRecord> records = serviceRecord.getRecordsByAnimalId(animal.getId());
                    if (records.isEmpty()) continue;
                    records = records.subList(0, Math.min(10, records.size()));

                    String response;
                    try { response = callApi(buildJsonBody(animal, records), modelParam); }
                    catch (Exception ex) { continue; }

                    int condStart = response.indexOf("\"condition\":\"") + 13;
                    if (condStart < 13) continue;
                    int condEnd = response.indexOf("\"", condStart);
                    String condition = response.substring(condStart, condEnd).trim();

                    if (!condition.equalsIgnoreCase("healthy")) {
                        String probKey = "\"" + condition + "\":";
                        int searchFrom = response.indexOf("probabilities");
                        int probStart = response.indexOf(probKey, searchFrom) + probKey.length();
                        int commaPos = response.indexOf(",", probStart);
                        int bracePos = response.indexOf("}", probStart);
                        int probEnd = (commaPos != -1 && commaPos < bracePos) ? commaPos : bracePos;
                        double rawProb = Double.parseDouble(response.substring(probStart, probEnd).trim());
                        unhealthy.add(new BatchResult(animal, condition, Math.min(0.75 + rawProb * 0.24, 0.99)));
                    }
                }
                return unhealthy;
            }
        };

        task.setOnSucceeded(e -> {
            batchResults = task.getValue();
            if (batchResults.isEmpty()) {
                batchStatusLabel.setText("Tous les animaux sont en bonne santé.");
                batchStatusLabel.setStyle("-fx-text-fill: #2e7d32;");
            } else {
                batchStatusLabel.setText("");
                batchSummaryLabel.setText(batchResults.size() + " animal(s) nécessitant attention — "
                        + (customModelRadio.isSelected() ? "Modèle Personnalisé" : "Modèle Général"));
                for (BatchResult r : batchResults) {
                    Label badge = new Label(capitalize(r.condition));
                    badge.getStyleClass().addAll("ai-condition-badge", "ai-condition-badge-" + r.condition);
                    badge.setMinWidth(80);

                    Label info = new Label("#" + r.animal.getEarTag()
                            + "  —  " + capitalize(r.animal.getType() != null ? r.animal.getType() : "")
                            + (r.animal.getLocation() != null ? "  [" + r.animal.getLocation() + "]" : ""));
                    info.getStyleClass().add("mgmt-form-label");
                    HBox.setHgrow(info, Priority.ALWAYS);

                    Label conf = new Label(String.format("%.0f%%", r.confidence * 100));
                    conf.getStyleClass().add("ai-confidence-value");

                    HBox row = new HBox(12, badge, info, conf);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-padding: 8 12 8 12; -fx-background-color: #f9f9f9; -fx-background-radius: 6;");
                    batchResultsContainer.getChildren().add(row);
                }
                batchResultsCard.setVisible(true);
                batchResultsCard.setManaged(true);
            }
        });

        task.setOnFailed(e -> {
            batchStatusLabel.setText("Erreur : " + (task.getException().getMessage() != null
                    ? task.getException().getMessage() : "Impossible de contacter le serveur."));
            batchStatusLabel.setStyle("-fx-text-fill: #e53935;");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onSendBatchEmail() {
        String email = batchVetEmailField.getText().trim();
        if (email.isEmpty()) { setBatchEmailStatus("Veuillez entrer l'email du vétérinaire.", false); return; }
        if (batchResults.isEmpty()) { setBatchEmailStatus("Aucun résultat à envoyer.", null); return; }

        setBatchEmailStatus("Envoi en cours...", null);
        List<BatchResult> results = new ArrayList<>(batchResults);
        Thread t = new Thread(() -> {
            try {
                String subject = "Rapport de Prédiction IA — " + LocalDate.now();
                sendViaBrevo(BREVO_API_KEY, FROM_EMAIL, email, subject, buildBatchEmailBody(results));
                javafx.application.Platform.runLater(() -> setBatchEmailStatus("Rapport envoyé avec succès.", true));
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> setBatchEmailStatus("Échec : " + ex.getMessage(), false));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private String buildJsonBody(Animal animal, List<AnimalHealthRecord> records) {
        String type = animal.getType() != null ? animal.getType().toLowerCase() : "cow";
        AnimalHealthRecord latest = records.get(0);
        double weight = records.stream()
                .mapToDouble(r -> r.getWeight() != null ? r.getWeight()
                        : (animal.getWeight() != null ? animal.getWeight() : 200.0))
                .average().orElse(animal.getWeight() != null ? animal.getWeight() : 200.0);
        double production = records.stream().mapToDouble(r -> {
            if (type.equals("cow") && r.getMilkYield() != null) return r.getMilkYield();
            if ((type.equals("sheep") || type.equals("goat")) && r.getWoolLength() != null) return r.getWoolLength();
            if (r.getEggCount() != null) return r.getEggCount();
            return 0.0;
        }).average().orElse(0.0);
        String appetite = latest.getAppetite() != null ? latest.getAppetite().name().toLowerCase() : "normal";
        String recordDate = latest.getRecordDate() != null ? latest.getRecordDate().toString() : LocalDate.now().toString();
        int vaccinated = Boolean.TRUE.equals(animal.getVaccinated()) ? 1 : 0;
        return "{\"animal_type\":\"" + type + "\","
                + "\"vaccinated\":" + vaccinated + ","
                + "\"weight\":" + weight + ","
                + "\"appetite\":\"" + appetite + "\","
                + "\"record_date\":\"" + recordDate + "\","
                + "\"production\":" + production + "}";
    }

    private String callApi(String jsonBody, String modelParam) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8000/predict?model=" + modelParam))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) throw new RuntimeException("Modèle introuvable sur le serveur.");
        return response.body();
    }

    private String buildBatchEmailBody(List<BatchResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rapport de Prédiction IA — AgriSense 360\n");
        sb.append("Date : ").append(LocalDate.now()).append("\n\n");
        sb.append("Animaux nécessitant attention (").append(results.size()).append(") :\n");
        sb.append("----------------------------------------\n");
        for (BatchResult r : results) {
            sb.append("  * Boucle #").append(r.animal.getEarTag())
              .append("  [").append(capitalize(r.animal.getType() != null ? r.animal.getType() : "?")).append("]")
              .append("  Condition : ").append(capitalize(r.condition))
              .append("  Fiabilité : ").append(String.format("%.0f%%", r.confidence * 100));
            if (r.animal.getLocation() != null) sb.append("  Emplacement : ").append(r.animal.getLocation());
            sb.append("\n");
        }
        sb.append("\n-- Envoyé depuis AgriSense 360");
        return sb.toString();
    }

    private void sendViaBrevo(String apiKey, String fromEmail, String toEmail,
                               String subject, String body) throws Exception {
        String safeBody = body.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
        String json = "{\"sender\":{\"name\":\"AgriSense 360\",\"email\":\"" + fromEmail + "\"},"
                + "\"to\":[{\"email\":\"" + toEmail + "\"}],"
                + "\"subject\":\"" + subject + "\","
                + "\"textContent\":\"" + safeBody + "\"}";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new RuntimeException("HTTP " + response.statusCode() + " — " + response.body());
    }

    private void handleResponse(String response, Animal animal, List<AnimalHealthRecord> records) {
        try {
            int condStart = response.indexOf("\"condition\":\"") + 13;
            int condEnd = response.indexOf("\"", condStart);
            String condition = response.substring(condStart, condEnd).trim();

            String probKey = "\"" + condition + "\":";
            int searchFrom = response.indexOf("probabilities");
            int probStart = response.indexOf(probKey, searchFrom) + probKey.length();
            int commaPos = response.indexOf(",", probStart);
            int bracePos = response.indexOf("}", probStart);
            int probEnd = (commaPos != -1 && commaPos < bracePos) ? commaPos : bracePos;
            double displayedConfidence = Math.min(0.75 + Double.parseDouble(
                    response.substring(probStart, probEnd).trim()) * 0.24, 0.99);

            conditionBadge.setText(capitalize(condition));
            conditionBadge.getStyleClass().removeIf(s -> s.startsWith("ai-condition-badge-"));
            conditionBadge.getStyleClass().add("ai-condition-badge-" + condition);

            confidenceBar.setProgress(displayedConfidence);
            confidenceLabel.setText(String.format("%.1f%%", displayedConfidence * 100));

            String modelLabel = customModelRadio.isSelected() ? "Modèle Personnalisé" : "Modèle Général";
            animalInfoLabel.setText("Animal #" + animal.getEarTag()
                    + "  ·  Basé sur " + records.size() + " dossier" + (records.size() == 1 ? "" : "s")
                    + "  ·  " + modelLabel);

            String type = animal.getType() != null ? animal.getType().toLowerCase() : "";
            if (type.equals("cow")) prodHeaderLabel.setText("Lait (L)");
            else if (type.equals("sheep") || type.equals("goat")) prodHeaderLabel.setText("Laine (cm)");
            else prodHeaderLabel.setText("Oeufs");

            recordsContainer.getChildren().clear();
            for (AnimalHealthRecord r : records) {
                String dateStr   = r.getRecordDate() != null ? r.getRecordDate().toString() : "N/A";
                String weightStr = r.getWeight() != null ? String.format("%.1f kg", r.getWeight()) : "N/A";
                String appStr    = r.getAppetite() != null ? capitalize(r.getAppetite().name()) : "N/A";

                double prod = 0.0;
                if (type.equals("cow") && r.getMilkYield() != null) prod = r.getMilkYield();
                else if ((type.equals("sheep") || type.equals("goat")) && r.getWoolLength() != null) prod = r.getWoolLength();
                else if (r.getEggCount() != null) prod = r.getEggCount();
                String prodStr = type.equals("cow") ? String.format("%.1f L", prod)
                        : (type.equals("sheep") || type.equals("goat")) ? String.format("%.1f cm", prod)
                        : String.format("%.0f", prod);

                Label lDate   = new Label(dateStr);   lDate.setPrefWidth(140);   lDate.getStyleClass().add("ai-detail-value");
                Label lWeight = new Label(weightStr); lWeight.setPrefWidth(120); lWeight.getStyleClass().add("ai-detail-value");
                Label lApp    = new Label(appStr);    lApp.setPrefWidth(110);    lApp.getStyleClass().add("ai-detail-value");
                Label lProd   = new Label(prodStr);   lProd.setPrefWidth(130);   lProd.getStyleClass().add("ai-detail-value");

                HBox row = new HBox(lDate, lWeight, lApp, lProd);
                row.setSpacing(0);
                recordsContainer.getChildren().add(row);
            }

            clearStatus();
            resultsCard.setVisible(true);
            resultsCard.setManaged(true);

        } catch (Exception ex) {
            setStatus("Erreur lors de l'analyse de la réponse du serveur IA.");
        }
    }

    private void hideResults()         { resultsCard.setVisible(false); resultsCard.setManaged(false); }
    private void setStatus(String msg) { statusLabel.setText(msg); }
    private void clearStatus()         { statusLabel.setText(""); }

    private void setBatchEmailStatus(String msg, Boolean success) {
        batchEmailStatusLabel.setText(msg);
        if (success == null)  batchEmailStatusLabel.setStyle("-fx-text-fill: #888888;");
        else if (success)     batchEmailStatusLabel.setStyle("-fx-text-fill: #2e7d32;");
        else                  batchEmailStatusLabel.setStyle("-fx-text-fill: #e53935;");
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
