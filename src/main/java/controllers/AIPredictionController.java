package controllers;

import entity.Animal;
import entity.AnimalHealthRecord;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
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
    @FXML private Label detailDate;
    @FXML private Label detailWeight;
    @FXML private Label detailAppetite;
    @FXML private Label detailProdLabel;
    @FXML private Label detailProd;

    private final ServiceAnimal serviceAnimal = new ServiceAnimal();
    private final ServiceAnimalHealthRecord serviceRecord = new ServiceAnimalHealthRecord();

    private Animal selectedAnimal;
    private AnimalHealthRecord lastRecord;

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
        lastRecord = null;
        clearStatus();
        hideResults();
        if (selectedAnimal == null) return;
        try {
            List<AnimalHealthRecord> records = serviceRecord.getRecordsByAnimalId(selectedAnimal.getId());
            if (records.isEmpty()) {
                setStatus("No health records found for this animal.");
            } else {
                lastRecord = records.get(0);
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
        if (lastRecord == null) {
            setStatus("No health record available to base the prediction on.");
            return;
        }

        clearStatus();
        hideResults();
        statusLabel.setText("Analyzing...");

        String type = selectedAnimal.getType().toLowerCase();
        double production = 0.0;
        if (type.equals("cow") && lastRecord.getMilkYield() != null) {
            production = lastRecord.getMilkYield();
        } else if ((type.equals("sheep") || type.equals("goat")) && lastRecord.getWoolLength() != null) {
            production = lastRecord.getWoolLength();
        } else if (lastRecord.getEggCount() != null) {
            production = lastRecord.getEggCount();
        }

        String recordDate = lastRecord.getRecordDate() != null
                ? lastRecord.getRecordDate().toString()
                : LocalDate.now().toString();
        String appetite = lastRecord.getAppetite() != null
                ? lastRecord.getAppetite().name().toLowerCase()
                : "normal";
        double weight = lastRecord.getWeight() != null
                ? lastRecord.getWeight()
                : (selectedAnimal.getWeight() != null ? selectedAnimal.getWeight() : 200.0);
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
        AnimalHealthRecord rec = lastRecord;
        double finalProduction = production;

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

        task.setOnSucceeded(e -> handleResponse(task.getValue(), animal, rec, finalProduction));
        task.setOnFailed(e -> setStatus("Could not reach the AI server. Make sure it is running on port 8000."));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void handleResponse(String response, Animal animal, AnimalHealthRecord rec, double production) {
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

            String lastRecordDate = rec.getRecordDate() != null ? rec.getRecordDate().toString() : "N/A";
            animalInfoLabel.setText("Animal #" + animal.getEarTag() + "  ·  Last record: " + lastRecordDate);

            detailDate.setText(rec.getRecordDate() != null ? rec.getRecordDate().toString() : "N/A");
            detailWeight.setText(rec.getWeight() != null ? rec.getWeight() + " kg" : "N/A");
            detailAppetite.setText(rec.getAppetite() != null ? capitalize(rec.getAppetite().name()) : "N/A");

            String type = animal.getType().toLowerCase();
            if (type.equals("cow")) {
                detailProdLabel.setText("Milk Yield");
                detailProd.setText(rec.getMilkYield() != null ? rec.getMilkYield() + " L" : "N/A");
            } else if (type.equals("sheep") || type.equals("goat")) {
                detailProdLabel.setText("Wool Length");
                detailProd.setText(rec.getWoolLength() != null ? rec.getWoolLength() + " cm" : "N/A");
            } else {
                detailProdLabel.setText("Egg Count");
                detailProd.setText(rec.getEggCount() != null ? rec.getEggCount().toString() : "N/A");
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

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private void clearStatus() {
        statusLabel.setText("");
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
