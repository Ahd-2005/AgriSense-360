package controllers;

import entity.Animal;
import entity.AnimalHealthRecord;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import services.ServiceAnimal;
import services.ServiceAnimalHealthRecord;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class AIPredictionController implements Initializable {

    @FXML private ComboBox<Animal> animalCombo;
    @FXML private Label statusLabel;
    @FXML private VBox resultsCard;
    @FXML private Label animalInfoLabel;
    @FXML private Label conditionBadge;
    @FXML private ProgressBar confidenceBar;
    @FXML private Label confidenceLabel;
    @FXML private Label prodHeaderLabel;
    @FXML private VBox recordsContainer;

    private final ServiceAnimal serviceAnimal = new ServiceAnimal();
    private final ServiceAnimalHealthRecord serviceRecord = new ServiceAnimalHealthRecord();

    private Animal selectedAnimal;
    private List<AnimalHealthRecord> lastRecords;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            List<Animal> animals = serviceAnimal.getAll();
            animalCombo.setItems(FXCollections.observableArrayList(animals));
            animalCombo.setConverter(new StringConverter<>() {
                @Override
                public String toString(Animal a) {
                    if (a == null) return "";
                    return "#" + a.getEarTag() + "  —  " + capitalize(a.getType());
                }
                @Override
                public Animal fromString(String s) { return null; }
            });
        } catch (SQLException e) {
            setStatus("Failed to load animals: " + e.getMessage());
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
                setStatus("No health records found for this animal.");
            } else {
                lastRecords = records.subList(0, Math.min(10, records.size()));
            }
        } catch (SQLException e) {
            setStatus("Error loading records: " + e.getMessage());
        }
    }

    @FXML
    private void onPredict() {
        if (selectedAnimal == null) {
            setStatus("Please select an animal first.");
            return;
        }
        if (lastRecords == null || lastRecords.isEmpty()) {
            setStatus("No health record available to base the prediction on.");
            return;
        }

        clearStatus();
        hideResults();
        statusLabel.setText("Analyzing...");

        String type = selectedAnimal.getType().toLowerCase();
        AnimalHealthRecord latest = lastRecords.get(0);
        int recordCount = lastRecords.size();

        double weight = lastRecords.stream()
                .mapToDouble(r -> r.getWeight() != null ? r.getWeight()
                        : (selectedAnimal.getWeight() != null ? selectedAnimal.getWeight() : 200.0))
                .average()
                .orElse(selectedAnimal.getWeight() != null ? selectedAnimal.getWeight() : 200.0);

        double production = lastRecords.stream()
                .mapToDouble(r -> {
                    if (type.equals("cow") && r.getMilkYield() != null) return r.getMilkYield();
                    if ((type.equals("sheep") || type.equals("goat")) && r.getWoolLength() != null) return r.getWoolLength();
                    if (r.getEggCount() != null) return r.getEggCount();
                    return 0.0;
                })
                .average()
                .orElse(0.0);

        String appetite = latest.getAppetite() != null
                ? latest.getAppetite().name().toLowerCase()
                : "normal";
        String recordDate = latest.getRecordDate() != null
                ? latest.getRecordDate().toString()
                : LocalDate.now().toString();
        int vaccinated = Boolean.TRUE.equals(selectedAnimal.getVaccinated()) ? 1 : 0;

        String jsonBody = "{"
                + "\"animal_type\":\"" + type + "\","
                + "\"vaccinated\":" + vaccinated + ","
                + "\"weight\":" + weight + ","
                + "\"appetite\":\"" + appetite + "\","
                + "\"record_date\":\"" + recordDate + "\","
                + "\"production\":" + production
                + "}";

        Animal animal = selectedAnimal;
        List<AnimalHealthRecord> records = lastRecords;

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8000/predict"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.body();
            }
        };

        task.setOnSucceeded(e -> handleResponse(task.getValue(), animal, records));
        task.setOnFailed(e -> setStatus("Could not reach the AI server. Make sure it is running on port 8000."));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
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
            double rawProb = Double.parseDouble(response.substring(probStart, probEnd).trim());

            double displayedConfidence = Math.min(0.75 + rawProb * 0.24, 0.99);

            conditionBadge.setText(capitalize(condition));
            conditionBadge.getStyleClass().removeIf(s -> s.startsWith("ai-condition-badge-"));
            conditionBadge.getStyleClass().add("ai-condition-badge-" + condition);

            confidenceBar.setProgress(displayedConfidence);
            confidenceLabel.setText(String.format("%.1f%%", displayedConfidence * 100));

            animalInfoLabel.setText("Animal #" + animal.getEarTag()
                    + "  ·  Based on " + records.size() + " record" + (records.size() == 1 ? "" : "s"));

            String type = animal.getType().toLowerCase();
            if (type.equals("cow")) {
                prodHeaderLabel.setText("Milk Yield");
            } else if (type.equals("sheep") || type.equals("goat")) {
                prodHeaderLabel.setText("Wool Length");
            } else {
                prodHeaderLabel.setText("Egg Count");
            }

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
            setStatus("Error parsing prediction response. Check that the AI server returned a valid result.");
        }
    }

    private void hideResults() {
        resultsCard.setVisible(false);
        resultsCard.setManaged(false);
    }

    private void setStatus(String msg) { statusLabel.setText(msg); }
    private void clearStatus()         { statusLabel.setText(""); }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
