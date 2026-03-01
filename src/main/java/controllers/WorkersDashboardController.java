package controllers;

import entity.AffectationTravail;
import entity.EvaluationPerformance;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import services.AffectationTravailService;
import services.EvaluationPerformanceService;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class WorkersDashboardController implements Initializable {

    @FXML private Label lblTotalAffect;
    @FXML private Label lblEnCours;
    @FXML private Label lblTerminees;
    @FXML private Label lblAvgNote;
    @FXML private Label lblCompletionRate;

    @FXML private PieChart pieStatut;
    @FXML private BarChart<String, Number> barQualite;
    @FXML private LineChart<String, Number> lineNotes;
    @FXML private BarChart<String, Number> barZones;

    private final AffectationTravailService affectationService = new AffectationTravailService();
    private final EvaluationPerformanceService evaluationService = new EvaluationPerformanceService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadDashboard();
    }

    private void loadDashboard() {
        try {
            List<AffectationTravail> affectations = affectationService.getAll();
            List<EvaluationPerformance> evaluations = evaluationService.getAll();

            updateKPIs(affectations, evaluations);
            updatePieChart(affectations);
            updateBarQualite(evaluations);
            updateLineNotes(evaluations);
            updateBarZones(affectations);

        } catch (SQLException e) {
            System.err.println("[WorkersDashboard] Error loading data: " + e.getMessage());
        }
    }

    // ── KPI Cards ──────────────────────────────────────────────────

    private void updateKPIs(List<AffectationTravail> affectations, List<EvaluationPerformance> evaluations) {
        int total = affectations.size();
        long enCours = affectations.stream().filter(a -> "En cours".equals(a.getStatut())).count();
        long terminees = affectations.stream().filter(a -> "Terminée".equals(a.getStatut())).count();
        double avgNote = evaluations.stream().mapToInt(EvaluationPerformance::getNote).average().orElse(0.0);
        double completionRate = total > 0 ? (terminees * 100.0 / total) : 0;

        lblTotalAffect.setText(String.valueOf(total));
        lblEnCours.setText(String.valueOf(enCours));
        lblTerminees.setText(String.valueOf(terminees));
        lblAvgNote.setText(String.format("%.1f", avgNote));
        lblCompletionRate.setText(String.format("%.0f%%", completionRate));
    }

    // ── Pie Chart: statuts ─────────────────────────────────────────

    private void updatePieChart(List<AffectationTravail> affectations) {
        Map<String, Long> byStatut = affectations.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStatut() != null ? a.getStatut() : "Inconnu",
                        Collectors.counting()));

        pieStatut.setData(FXCollections.observableArrayList(
                byStatut.entrySet().stream()
                        .map(e -> new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()))
                        .toList()
        ));
    }

    // ── Bar Chart: qualité évaluations ─────────────────────────────

    private void updateBarQualite(List<EvaluationPerformance> evaluations) {
        Map<String, Long> byQualite = evaluations.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getQualite() != null ? e.getQualite() : "N/A",
                        Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Qualité");

        // Ensure consistent order
        for (String q : new String[]{"Bonne", "Moyenne", "Faible"}) {
            series.getData().add(new XYChart.Data<>(q, byQualite.getOrDefault(q, 0L)));
        }
        // Add any unknown quality
        byQualite.forEach((k, v) -> {
            if (!"Bonne".equals(k) && !"Moyenne".equals(k) && !"Faible".equals(k)) {
                series.getData().add(new XYChart.Data<>(k, v));
            }
        });

        barQualite.getData().clear();
        barQualite.getData().add(series);
    }

    // ── Line Chart: notes over time ────────────────────────────────

    private void updateLineNotes(List<EvaluationPerformance> evaluations) {
        // Sort by date
        List<EvaluationPerformance> sorted = evaluations.stream()
                .filter(e -> e.getDateEvaluation() != null)
                .sorted(Comparator.comparing(EvaluationPerformance::getDateEvaluation))
                .toList();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Note");

        for (EvaluationPerformance e : sorted) {
            series.getData().add(new XYChart.Data<>(e.getDateEvaluation().toString(), e.getNote()));
        }

        lineNotes.getData().clear();
        lineNotes.getData().add(series);
    }

    // ── Bar Chart: zones ───────────────────────────────────────────

    private void updateBarZones(List<AffectationTravail> affectations) {
        Map<String, Long> byZone = affectations.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getZoneTravail() != null ? a.getZoneTravail() : "N/A",
                        Collectors.counting()));

        // Sort by count descending, take top 8
        List<Map.Entry<String, Long>> topZones = byZone.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .toList();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Zones");
        for (Map.Entry<String, Long> entry : topZones) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        barZones.getData().clear();
        barZones.getData().add(series);
    }

    // ── Navigation ─────────────────────────────────────────────────

    @FXML
    private void onSwitchToAffectation() {
        MainLayoutController.getInstance().navigateToWorkers();
    }

    @FXML
    private void onSwitchToEvaluation() {
        MainLayoutController.getInstance().navigateToEvaluation();
    }

    @FXML
    private void onSwitchToCalendar() {
        MainLayoutController.getInstance().navigateToCalendar();
    }
}
