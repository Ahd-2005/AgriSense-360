package controllers;

import entity.Animal;
import entity.AnimalHealthRecord;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import services.ServiceAnimal;
import services.ServiceAnimalHealthRecord;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class StatsController implements Initializable {

    @FXML private Label totalAnimalsLabel;
    @FXML private Label totalRecordsLabel;
    @FXML private Label vacRateLabel;
    @FXML private Label atRiskLabel;

    @FXML private PieChart conditionPie;
    @FXML private BarChart<String, Number> typeBarChart;
    @FXML private FlowPane locationCardsPane;

    private final ServiceAnimal serviceAnimal = new ServiceAnimal();
    private final ServiceAnimalHealthRecord serviceRecord = new ServiceAnimalHealthRecord();

    private boolean loaded = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        totalAnimalsLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && !loaded) {
                loadData();
            }
        });
    }

    private void loadData() {
        if (loaded) return;
        loaded = true;
        try {
            List<Animal> animals = serviceAnimal.getAll();
            List<AnimalHealthRecord> records = serviceRecord.getAll();
            buildKPIs(animals, records);
            buildConditionPie(records);
            buildTypeBar(animals);
            buildLocationCards(animals);
        } catch (SQLException e) {
            totalAnimalsLabel.setText("!");
            totalRecordsLabel.setText("!");
            vacRateLabel.setText("!");
            atRiskLabel.setText("!");
        }
    }

    private void buildKPIs(List<Animal> animals, List<AnimalHealthRecord> records) {
        totalAnimalsLabel.setText(String.valueOf(animals.size()));
        totalRecordsLabel.setText(String.valueOf(records.size()));

        long vaccinated = animals.stream()
                .filter(a -> Boolean.TRUE.equals(a.getVaccinated())).count();
        double vacRate = animals.isEmpty() ? 0.0 : (double) vaccinated / animals.size() * 100.0;
        vacRateLabel.setText(String.format("%.0f%%", vacRate));

        long atRisk = animals.stream()
                .filter(a -> a.getHealthStatus() != null
                        && !a.getHealthStatus().equalsIgnoreCase("healthy"))
                .count();
        atRiskLabel.setText(String.valueOf(atRisk));
    }

    private void buildConditionPie(List<AnimalHealthRecord> records) {
        Map<String, Long> counts = records.stream()
                .filter(r -> r.getConditionStatus() != null)
                .collect(Collectors.groupingBy(
                        r -> capitalize(r.getConditionStatus().name()), Collectors.counting()));

        if (counts.isEmpty()) return;

        List<PieChart.Data> data = new ArrayList<>();
        data.add(new PieChart.Data("Healthy  " + counts.getOrDefault("Healthy", 0L),  counts.getOrDefault("Healthy",  0L)));
        data.add(new PieChart.Data("Sick  "    + counts.getOrDefault("Sick",    0L),  counts.getOrDefault("Sick",     0L)));
        data.add(new PieChart.Data("Injured  " + counts.getOrDefault("Injured", 0L),  counts.getOrDefault("Injured",  0L)));
        data.add(new PieChart.Data("Critical  "+ counts.getOrDefault("Critical",0L),  counts.getOrDefault("Critical", 0L)));
        data.removeIf(d -> d.getPieValue() == 0);

        conditionPie.setData(FXCollections.observableArrayList(data));

        Map<String, String> colors = Map.of(
                "Healthy",  "#43a047",
                "Sick",     "#f57c00",
                "Injured",  "#1e88e5",
                "Critical", "#e53935"
        );

        Platform.runLater(() -> conditionPie.getData().forEach(d -> {
            String label = d.getName().split(" {2}")[0];
            if (d.getNode() != null)
                d.getNode().setStyle("-fx-pie-color: " + colors.getOrDefault(label, "#888888") + ";");
        }));
    }

    private void buildTypeBar(List<Animal> animals) {
        Map<String, Long> counts = animals.stream()
                .filter(a -> a.getType() != null)
                .collect(Collectors.groupingBy(a -> capitalize(a.getType()), Collectors.counting()));

        if (counts.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));

        typeBarChart.getData().add(series);
        Platform.runLater(() ->
                typeBarChart.getData().get(0).getData()
                        .forEach(d -> d.getNode().setStyle("-fx-bar-fill: #5a9814;")));
    }

    private void buildLocationCards(List<Animal> animals) {
        Map<String, Long> counts = animals.stream()
                .filter(a -> a.getLocation() != null && !a.getLocation().isEmpty())
                .collect(Collectors.groupingBy(a -> capitalize(a.getLocation()), Collectors.counting()));

        if (counts.isEmpty()) return;

        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> {
                    VBox card = new VBox(6);
                    card.setAlignment(Pos.CENTER);
                    card.setPrefSize(130, 90);
                    card.getStyleClass().add("stats-kpi-card");

                    Label count = new Label(String.valueOf(e.getValue()));
                    count.getStyleClass().add("stats-kpi-value");

                    Label name = new Label(e.getKey());
                    name.getStyleClass().add("stats-kpi-label");
                    name.setWrapText(true);
                    name.setMaxWidth(118);

                    card.getChildren().addAll(count, name);
                    locationCardsPane.getChildren().add(card);
                });
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
