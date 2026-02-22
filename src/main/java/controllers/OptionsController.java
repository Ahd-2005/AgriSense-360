package controllers;

import entity.Animal;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import services.ServiceAnimal;
import services.ServiceEnumManagement;
import utils.AnimalListRefresh;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OptionsController implements Initializable {

    private static final String BREVO_API_KEY = "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";
    private static final String FROM_EMAIL     = "rinmo7620@gmail.com";

    @FXML private ListView<String> typesList;
    @FXML private ListView<String> locationsList;
    @FXML private TextField newTypeField;
    @FXML private TextField newLocationField;
    @FXML private Button deleteTypeBtn;
    @FXML private Button deleteLocationBtn;

    @FXML private TextField vetEmailField;
    @FXML private TextArea notesArea;
    @FXML private Label emailStatusLabel;

    private final ServiceEnumManagement serviceEnum = new ServiceEnumManagement();
    private final ServiceAnimal serviceAnimal = new ServiceAnimal();
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
    }

    private void refreshLists() {
        try {
            typesItems.setAll(serviceEnum.getEnumValues("Animal", "type"));
            locationsItems.setAll(serviceEnum.getEnumValues("Animal", "location"));
        } catch (SQLException e) {
            showError("Could not load lists: " + e.getMessage());
        }
    }

    @FXML
    private void onAddType() {
        String v = newTypeField.getText();
        if (v == null || v.trim().isEmpty()) { showError("Enter a type name."); return; }
        try {
            serviceEnum.addEnumValue("Animal", "type", v.trim());
            newTypeField.clear();
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

    private void showInfo(String msg)  { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }
}
