package controllers;

import entity.Culture;
import entity.Parcelle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.CultureService;
import services.ParcelleService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

public class DashboardCultureController {

    private final services.AgromonitoringAdviceService adviceService = new services.AgromonitoringAdviceService();

    private final CultureService cultureService = new CultureService();
    private final ParcelleService parcelleService = new ParcelleService();

    private List<Culture> allCultures;
    private List<Parcelle> allParcelles;

    @FXML private VBox gererCultureCard;
    @FXML private VBox gererParcelleCard;
    @FXML private ImageView cultureImage;
    @FXML private ImageView parcelleImage;

    // Advice card (Agromonitoring API)
    @FXML private HBox adviceCard;
    @FXML private Label adviceText;
    @FXML private Label adviceAuthor;
    @FXML private Button adviceRefreshBtn;
    @FXML private javafx.scene.layout.FlowPane adviceTags;

    @FXML private PieChart surfaceChart;
    @FXML private BarChart<String, Number> cultureTypeChart;
    @FXML private PieChart etatChart;

    @FXML private Label totalCulturesLabel;
    @FXML private Label totalParcellesLabel;
    @FXML private Label surfaceTotaleLabel;
    @FXML private Label tauxOccupationLabel;
    @FXML private Label culturesRetesLabel;
    @FXML private Label culturesRetardLabel;

    @FXML private VBox topParcellesBox;

    @FXML private Label recoltePrevueCountBadge;
    @FXML private HBox recoltePrevueBox;
    @FXML private Label recolteRetardCountBadge;
    @FXML private HBox recolteRetardBox;

    // Unicode constant for m² — avoids any file-encoding parse error
    private static final String M2 = "m\u00B2";

    @FXML
    public void initialize() {
        entity.user currentUser = services.SessionManager.getInstance().getCurrentUser();
        try {
            if (currentUser != null && currentUser.getFarmId() != null) {
                allCultures = cultureService.getCulturesByFarm(currentUser.getFarmId());
                allParcelles = parcelleService.getParcellesByFarm(currentUser.getFarmId());
            } else {
                allCultures = cultureService.getAllCultures();
                allParcelles = parcelleService.getAllParcelles();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            allCultures = new ArrayList<>();
            allParcelles = new ArrayList<>();
        }

        loadSummaryStatistics();
        loadSurfaceChart();
        loadCultureTypeChart();
        loadEtatChart();
        loadTopParcelles();
        loadRecoltePrevue();
        loadRecolteRetard();

        // Load advice (OpenFarm) after data fetched
        loadAdvice();
    }

    // ============================================================
    // CONSEIL (Général - Offline Random)
    // ============================================================
    private void loadAdvice() {
        if (adviceText == null) return;
        adviceText.setText("⏳ Chargement du conseil...");
        adviceAuthor.setText("");
        if (adviceTags != null) adviceTags.getChildren().clear();

        CompletableFuture.supplyAsync(() -> adviceService.fetchAdvice().orElse(null))
                .thenAccept(advice -> javafx.application.Platform.runLater(() -> {
                    if (advice == null) {
                        adviceText.setText("Impossible de récupérer un conseil pour le moment. Réessayez plus tard.");
                        adviceAuthor.setText("");
                        return;
                    }

                    // Split raw text into advice sentences and metrics JSON
                    String raw = advice.text;
                    String mainText = raw;
                    org.json.JSONObject metrics = new org.json.JSONObject();

                    int metaIdx = raw.indexOf("__METRICS__=");
                    if (metaIdx >= 0) {
                        mainText = raw.substring(0, metaIdx).trim();
                        try {
                            metrics = new org.json.JSONObject(raw.substring(metaIdx + "__METRICS__=".length()).trim());
                        } catch (Exception ignored) {}
                    }

                    // Split advice into individual sentences and show them as bullet chips
                    adviceText.setText("");
                    if (adviceTags != null) {
                        adviceTags.getChildren().clear();

                        // --- Weather / advice sentence chips (one per sentence) ---
                        String[] sentences = mainText.split("(?<=\\.) ");
                        for (String sentence : sentences) {
                            sentence = sentence.trim();
                            if (sentence.isEmpty()) continue;
                            Label chip = buildAdviceChip(sentence);
                            adviceTags.getChildren().add(chip);
                        }

                        // --- Metric chips with emoji + colored background ---
                        if (metrics.has("tempC")) {
                            double t = metrics.optDouble("tempC");
                            String emoji = t >= 30 ? "🔥" : t <= 5 ? "🥶" : "🌡️";
                            String color = t >= 30 ? "#ff5722" : t <= 5 ? "#2196F3" : "#4CAF50";
                            adviceTags.getChildren().add(buildMetricChip(emoji + " Temp", String.format("%.1f°C", t), color));
                        }
                        if (metrics.has("humidity")) {
                            int h = metrics.optInt("humidity");
                            String emoji = h >= 85 ? "💧" : h <= 35 ? "🏜️" : "💦";
                            String color = h >= 85 ? "#1976D2" : h <= 35 ? "#FF9800" : "#26C6DA";
                            adviceTags.getChildren().add(buildMetricChip(emoji + " Humidité", h + "%", color));
                        }
                        if (metrics.has("wind")) {
                            double w = metrics.optDouble("wind");
                            String emoji = w >= 8 ? "💨" : "🌬️";
                            String color = w >= 8 ? "#9C27B0" : "#78909C";
                            adviceTags.getChildren().add(buildMetricChip(emoji + " Vent", String.format("%.1f m/s", w), color));
                        }
                        if (metrics.has("rain1h")) {
                            double r = metrics.optDouble("rain1h");
                            adviceTags.getChildren().add(buildMetricChip("🌧️ Pluie", String.format("%.1f mm", r), "#1565C0"));
                        }
                        if (metrics.has("soilMoist")) {
                            double sm = metrics.optDouble("soilMoist");
                            String emoji = sm < 0.15 ? "🌵" : sm > 0.40 ? "🌊" : "🌱";
                            String color = sm < 0.15 ? "#FF6F00" : sm > 0.40 ? "#0288D1" : "#388E3C";
                            adviceTags.getChildren().add(buildMetricChip(emoji + " Sol", String.format("%.2f m³/m³", sm), color));
                        }
                        if (metrics.has("soilT")) {
                            double st = metrics.optDouble("soilT");
                            String emoji = st < 10 ? "❄️" : st > 25 ? "☀️" : "🌿";
                            adviceTags.getChildren().add(buildMetricChip(emoji + " Sol T°", String.format("%.1f°C", st), "#5D4037"));
                        }
                        if (metrics.has("desc")) {
                            adviceTags.getChildren().add(buildMetricChip("🌤️", metrics.optString("desc"), "#607D8B"));
                        }
                    }

                    adviceAuthor.setText("— " + advice.source);
                }));
    }

    /** A chip for advice sentence text */
    private Label buildAdviceChip(String text) {
        Label lbl = new Label("• " + text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(860);
        lbl.setStyle(
                "-fx-background-color: #e8f5e9;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 6 12;" +
                        "-fx-text-fill: #2a5a3a;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;"
        );
        return lbl;
    }

    /** A chip for a metric key+value with a colored badge */
    private HBox buildMetricChip(String label, String value, String color) {
        Label keyLbl = new Label(label);
        keyLbl.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 8px 0 0 8px;" +
                        "-fx-padding: 5 10;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;"
        );

        Label valLbl = new Label(value);
        valLbl.setStyle(
                "-fx-background-color: " + color + "22;" +
                        "-fx-background-radius: 0 8px 8px 0;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 0 8px 8px 0;" +
                        "-fx-padding: 5 10;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;"
        );

        HBox chip = new HBox(0, keyLbl, valLbl);
        chip.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        chip.setStyle("-fx-cursor: default;");
        return chip;
    }


    @FXML
    private void refreshAdvice() {
        loadAdvice();
    }

    // ============================================================
    // POPUP: Surface
    // ============================================================
    @FXML
    private void showSurfaceChartDetails(MouseEvent event) {
        if (event.getClickCount() == 2) {
            double totalSurface = allParcelles.stream().mapToDouble(Parcelle::getSurface).sum();
            double surfaceUtilisee = allCultures.stream().mapToDouble(Culture::getSurface).sum();
            double surfaceDisponible = totalSurface - surfaceUtilisee;
            double pctUtilisee = totalSurface > 0 ? (surfaceUtilisee / totalSurface) * 100 : 0;

            Stage popup = createStyledStage("R\u00e9partition des Surfaces");

            ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                    new PieChart.Data("Utilis\u00e9e (" + String.format("%.0f", surfaceUtilisee) + " " + M2 + ")", surfaceUtilisee),
                    new PieChart.Data("Disponible (" + String.format("%.0f", surfaceDisponible) + " " + M2 + ")", surfaceDisponible)
            );
            PieChart chart = new PieChart(data);
            chart.setLegendVisible(false);
            chart.setLabelsVisible(true);
            chart.setAnimated(true);
            chart.setStartAngle(90);
            chart.setPrefSize(380, 340);
            chart.setStyle("-fx-background-color: transparent;");

            String labelUsed = "Utilisee (" + (int) surfaceUtilisee + " m2)";
            String labelDispo = "Disponible (" + (int) surfaceDisponible + " m2)";

            data = FXCollections.observableArrayList(
                    new PieChart.Data(labelUsed, surfaceUtilisee),
                    new PieChart.Data(labelDispo, surfaceDisponible)
            );
            chart = new PieChart(data);
            chart.setLegendVisible(false);
            chart.setLabelsVisible(true);
            chart.setAnimated(true);
            chart.setStartAngle(90);
            chart.setPrefSize(380, 340);
            chart.setStyle("-fx-background-color: transparent;");

            final ObservableList<PieChart.Data> dataFinal = data;
            javafx.application.Platform.runLater(() -> {
                if (dataFinal.get(0).getNode() != null)
                    dataFinal.get(0).getNode().setStyle("-fx-pie-color: #4CAF50;");
                if (dataFinal.get(1).getNode() != null)
                    dataFinal.get(1).getNode().setStyle("-fx-pie-color: #2196F3;");
            });

            String[] statValues = {
                    String.format("%.0f", surfaceUtilisee) + " " + M2,
                    String.format("%.0f", surfaceDisponible) + " " + M2,
                    String.format("%.0f", totalSurface) + " " + M2,
                    String.format("%.1f%%", pctUtilisee)
            };
            // FIX: stat box minWidth 100, font 14px so values never truncate to "2..."
            String[] statNames = {"Utilis\u00e9e", "Disponible", "Totale", "Occupation"};
            String[] statColors = {"#4CAF50", "#2196F3", "#9C27B0", "#FF9800"};

            VBox legendBox = buildLegendBox(
                    "R\u00e9partition des Surfaces",
                    statNames, statValues, statColors,
                    new String[]{"Utilis\u00e9e", "Disponible"},
                    new String[]{"#4CAF50", "#2196F3"}
            );

            HBox content = buildPopupLayout(chart, legendBox);
            showPopup(popup, content, 860, 460);
        }
    }

    // ============================================================
    // POPUP: Cultures par Type
    // FIX: color map keys use exact accented strings from DB:
    //   "C\u00e9r\u00e9ales", "L\u00e9gumes", "Fruits", "Ornementales"
    // Previously they were "Cereales", "Legumes" — no match, gray bars
    // ============================================================
    @FXML
    private void showCultureTypeChartDetails(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Map<String, Long> typeCounts = allCultures.stream()
                    .collect(Collectors.groupingBy(Culture::getTypeCulture, Collectors.counting()));
            Map<String, Double> typeSurfaces = new HashMap<>();
            for (Culture c : allCultures) {
                typeSurfaces.merge(c.getTypeCulture(), c.getSurface(), Double::sum);
            }

            // FIX: keys match exact DB values (accented)
            Map<String, String> typeColors = new LinkedHashMap<>();
            typeColors.put("C\u00e9r\u00e9ales", "#4CAF50");
            typeColors.put("L\u00e9gumes", "#FF9800");
            typeColors.put("Fruits", "#2196F3");
            typeColors.put("Ornementales", "#9C27B0");

            Stage popup = createStyledStage("Cultures par Type");

            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Nombre");
            BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
            chart.setLegendVisible(false);
            chart.setAnimated(true);
            chart.setStyle("-fx-background-color: transparent;");
            chart.setPrefSize(380, 310);
            chart.setCategoryGap(25);
            chart.setBarGap(5);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            typeCounts.forEach((type, count) -> series.getData().add(new XYChart.Data<>(type, count)));
            chart.getData().add(series);

            javafx.application.Platform.runLater(() -> {
                for (XYChart.Data<String, Number> d : series.getData()) {
                    String color = typeColors.getOrDefault(d.getXValue(), "#888");
                    if (d.getNode() != null) {
                        d.getNode().setStyle("-fx-bar-fill: " + color + "; -fx-background-radius: 6px;");
                    }
                }
            });

            String[] names = typeCounts.keySet().toArray(new String[0]);
            String[] colors = Arrays.stream(names).map(n -> typeColors.getOrDefault(n, "#888")).toArray(String[]::new);

            String[] statValues = {String.valueOf(allCultures.size()), String.valueOf(typeCounts.size())};
            String[] statNames = {"Total Cultures", "Types"};
            String[] statColors = {"#4CAF50", "#9C27B0"};

            VBox legendBox = buildLegendBox("Cultures par Type", statNames, statValues, statColors, names, colors);

            HBox content = buildPopupLayout(chart, legendBox);
            showPopup(popup, content, 860, 460);
        }
    }

    // ============================================================
    // POPUP: Etat
    // FIX: etatColors keys use exact accented DB strings:
    //   "Semis", "Croissance", "Maturit\u00e9", "R\u00e9colte Pr\u00e9vue",
    //   "R\u00e9colte en Retard"
    // Previously "Maturite", "Recolte Prevue" — never matched DB values
    // ============================================================
    @FXML
    private void showEtatChartDetails(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Map<String, Long> etatCounts = allCultures.stream()
                    .collect(Collectors.groupingBy(Culture::getEtat, Collectors.counting()));

            // FIX: exact accented keys to match DB values
            Map<String, String> etatColors = new LinkedHashMap<>();
            etatColors.put("Semis", "#4CAF50");
            etatColors.put("Croissance", "#FF9800");
            etatColors.put("Maturit\u00e9", "#2196F3");
            etatColors.put("R\u00e9colte Pr\u00e9vue", "#FFB300");
            etatColors.put("R\u00e9colte en Retard", "#f44336");

            Stage popup = createStyledStage("R\u00e9partition par \u00c9tat");

            ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
            for (String etat : etatColors.keySet()) {
                long count = etatCounts.getOrDefault(etat, 0L);
                if (count > 0) {
                    data.add(new PieChart.Data(etat + " (" + count + ")", count));
                }
            }

            PieChart chart = new PieChart(data);
            chart.setLegendVisible(false);
            chart.setLabelsVisible(true);
            chart.setAnimated(true);
            chart.setStartAngle(90);
            chart.setPrefSize(380, 340);
            chart.setStyle("-fx-background-color: transparent;");

            javafx.application.Platform.runLater(() -> {
                for (PieChart.Data d : data) {
                    String rawEtat = d.getName().replaceAll(" \\(\\d+\\)$", "");
                    String color = etatColors.getOrDefault(rawEtat, "#888");
                    if (d.getNode() != null) {
                        d.getNode().setStyle("-fx-pie-color: " + color + ";");
                    }
                }
            });

            String[] statValues = {
                    String.valueOf(allCultures.size()),
                    String.valueOf(etatCounts.getOrDefault("R\u00e9colte Pr\u00e9vue", 0L) + etatCounts.getOrDefault("Maturit\u00e9", 0L)),
                    String.valueOf(etatCounts.getOrDefault("R\u00e9colte en Retard", 0L))
            };
            String[] statNames = {"Total", "Pr\u00eats", "En Retard"};
            String[] statColors = {"#2a5a3a", "#FFB300", "#f44336"};

            List<String> legNames = new ArrayList<>();
            List<String> legColors = new ArrayList<>();
            for (String etat : etatColors.keySet()) {
                if (etatCounts.getOrDefault(etat, 0L) > 0) {
                    legNames.add(etat + " (" + etatCounts.get(etat) + ")");
                    legColors.add(etatColors.get(etat));
                }
            }

            VBox legendBox = buildLegendBox("\u00c9tats des Cultures",
                    statNames, statValues, statColors,
                    legNames.toArray(new String[0]),
                    legColors.toArray(new String[0]));

            HBox content = buildPopupLayout(chart, legendBox);
            showPopup(popup, content, 860, 480);
        }
    }

    // ============================================================
    // POPUP BUILDER HELPERS
    // ============================================================
    private Stage createStyledStage(String title) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle(title);
        popup.setResizable(false);
        return popup;
    }

    private VBox buildLegendBox(String title,
                                String[] statNames, String[] statValues, String[] statColors,
                                String[] legendLabels, String[] legendColors) {
        VBox box = new VBox(16);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(24, 24, 24, 20));
        box.setPrefWidth(380);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2a5a3a;");
        box.getChildren().add(titleLabel);

        HBox statsRow = new HBox(8);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < statNames.length; i++) {
            VBox stat = new VBox(4);
            stat.setAlignment(Pos.CENTER);
            stat.setMinWidth(100);
            stat.setPrefWidth(100);
            stat.setStyle(
                    "-fx-background-color: " + statColors[i] + "20;" +
                            "-fx-background-radius: 10px;" +
                            "-fx-padding: 10 8;"
            );
            Label val = new Label(statValues[i]);
            val.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + statColors[i] + ";");
            val.setWrapText(false);
            Label nm = new Label(statNames[i]);
            nm.setStyle("-fx-font-size: 10px; -fx-text-fill: #666; -fx-font-weight: bold;");
            nm.setWrapText(false);
            stat.getChildren().addAll(val, nm);
            statsRow.getChildren().add(stat);
        }
        box.getChildren().add(statsRow);

        Region sep = new Region();
        sep.setStyle("-fx-background-color: #e0e0e0;");
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().add(sep);

        Label legTitle = new Label("L\u00e9gende");
        legTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #444;");
        box.getChildren().add(legTitle);

        for (int i = 0; i < legendLabels.length; i++) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 10, 6, 10));
            row.setStyle(
                    "-fx-background-color: " + legendColors[i] + "15;" +
                            "-fx-background-radius: 8px;"
            );
            Circle dot = new Circle(9);
            dot.setFill(Color.web(legendColors[i]));
            Label lbl = new Label(legendLabels[i]);
            lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #222; -fx-font-weight: bold;");
            row.getChildren().addAll(dot, lbl);
            box.getChildren().add(row);
        }

        return box;
    }

    private HBox buildPopupLayout(javafx.scene.Node chart, VBox legendBox) {
        HBox root = new HBox(0);
        root.setStyle("-fx-background-color: white;");
        VBox chartArea = new VBox();
        chartArea.setAlignment(Pos.CENTER);
        chartArea.setPadding(new Insets(20));
        chartArea.setStyle("-fx-background-color: #f7faf8;");
        chartArea.getChildren().add(chart);
        chartArea.setPrefWidth(420);
        root.getChildren().addAll(chartArea, legendBox);
        return root;
    }

    private void showPopup(Stage popup, HBox content, double width, double height) {
        Scene scene = new Scene(content, width, height);
        popup.setScene(scene);
        popup.show();
    }

    // ============================================================
    // DATA LOADING
    // ============================================================
    private void loadSummaryStatistics() {
        totalCulturesLabel.setText(String.valueOf(allCultures.size()));
        totalParcellesLabel.setText(String.valueOf(allParcelles.size()));

        double totalSurface = allParcelles.stream().mapToDouble(Parcelle::getSurface).sum();
        double surfaceUtilisee = allCultures.stream().mapToDouble(Culture::getSurface).sum();

        surfaceTotaleLabel.setText(String.format("%.0f", totalSurface) + " " + M2);

        double tauxOccupation = totalSurface > 0 ? (surfaceUtilisee / totalSurface) * 100 : 0;
        tauxOccupationLabel.setText(String.format("%.1f%%", tauxOccupation));

        // FIX: restored accented strings to match actual DB values
        long culturesPretes = allCultures.stream()
                .filter(c -> "Maturit\u00e9".equals(c.getEtat()) || "R\u00e9colte Pr\u00e9vue".equals(c.getEtat()))
                .count();
        culturesRetesLabel.setText(String.valueOf(culturesPretes));

        long culturesRetard = allCultures.stream()
                .filter(c -> "R\u00e9colte en Retard".equals(c.getEtat()))
                .count();
        culturesRetardLabel.setText(String.valueOf(culturesRetard));
    }

    private void loadSurfaceChart() {
        double totalSurface = allParcelles.stream().mapToDouble(Parcelle::getSurface).sum();
        double surfaceUtilisee = allCultures.stream().mapToDouble(Culture::getSurface).sum();
        double surfaceDisponible = totalSurface - surfaceUtilisee;

        ObservableList<PieChart.Data> surfaceData = FXCollections.observableArrayList(
                new PieChart.Data("Utilis\u00e9e (" + String.format("%.0f", surfaceUtilisee) + " " + M2 + ")", surfaceUtilisee),
                new PieChart.Data("Disponible (" + String.format("%.0f", surfaceDisponible) + " " + M2 + ")", surfaceDisponible)
        );
        surfaceChart.setData(surfaceData);
        javafx.application.Platform.runLater(() -> applySurfaceChartColors(surfaceData));
    }

    private void applySurfaceChartColors(ObservableList<PieChart.Data> data) {
        String[] pieColors = {"#4CAF50", "#2196F3"};
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getNode() != null) {
                data.get(i).getNode().setStyle("-fx-pie-color: " + pieColors[i] + ";");
            }
        }
    }

    private void loadCultureTypeChart() {
        Map<String, Long> typeCounts = allCultures.stream()
                .collect(Collectors.groupingBy(Culture::getTypeCulture, Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        typeCounts.forEach((type, count) -> series.getData().add(new XYChart.Data<>(type, count)));

        cultureTypeChart.getData().clear();
        cultureTypeChart.getData().add(series);
        javafx.application.Platform.runLater(() -> applyCultureTypeChartColors(series));
    }

    private void applyCultureTypeChartColors(XYChart.Series<String, Number> series) {
        // FIX: restored accented keys to match DB values
        Map<String, String> typeColors = new HashMap<>();
        typeColors.put("C\u00e9r\u00e9ales", "#4CAF50");
        typeColors.put("L\u00e9gumes", "#FF9800");
        typeColors.put("Fruits", "#2196F3");
        typeColors.put("Ornementales", "#9C27B0");

        for (XYChart.Data<String, Number> data : series.getData()) {
            String color = typeColors.getOrDefault(data.getXValue(), "#888888");
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-bar-fill: " + color + "; -fx-background-radius: 6px;");
            }
        }
    }

    private void loadEtatChart() {
        Map<String, Long> etatCounts = allCultures.stream()
                .collect(Collectors.groupingBy(Culture::getEtat, Collectors.counting()));

        ObservableList<PieChart.Data> etatData = FXCollections.observableArrayList();
        etatCounts.forEach((etat, count) -> etatData.add(new PieChart.Data(etat + " (" + count + ")", count)));

        etatChart.setData(etatData);
        javafx.application.Platform.runLater(() -> applyEtatChartColors(etatData, etatCounts));
    }

    private void applyEtatChartColors(ObservableList<PieChart.Data> data, Map<String, Long> etatCounts) {
        // FIX: restored accented keys to match DB values
        Map<String, String> etatColors = new HashMap<>();
        etatColors.put("Semis", "#4CAF50");
        etatColors.put("Croissance", "#FF9800");
        etatColors.put("Maturit\u00e9", "#2196F3");
        etatColors.put("R\u00e9colte Pr\u00e9vue", "#FFB300");
        etatColors.put("R\u00e9colte en Retard", "#f44336");

        for (PieChart.Data chartData : data) {
            String label = chartData.getName();
            String etat = label.contains(" (") ? label.substring(0, label.lastIndexOf(" (")) : label;
            String color = etatColors.getOrDefault(etat, "#888888");
            if (chartData.getNode() != null) {
                chartData.getNode().setStyle("-fx-pie-color: " + color + ";");
            }
        }
    }

    // ============================================================
    // TOP PARCELLES — PRIMARY: culture count DESC, SECONDARY: surface DESC
    // ============================================================
    private void loadTopParcelles() {
        Map<Integer, Double> occupationMap = new HashMap<>();
        Map<Integer, Long> cultureCountMap = new HashMap<>();

        for (Parcelle parcelle : allParcelles) {
            List<Culture> parcelCultures = allCultures.stream()
                    .filter(c -> c.getParcelleId() == parcelle.getId())
                    .collect(Collectors.toList());

            double used = parcelCultures.stream().mapToDouble(Culture::getSurface).sum();
            double occupationPercent = parcelle.getSurface() > 0 ? (used / parcelle.getSurface()) * 100 : 0;

            occupationMap.put(parcelle.getId(), occupationPercent);
            cultureCountMap.put(parcelle.getId(), (long) parcelCultures.size());
        }

        List<Parcelle> topParcelles = allParcelles.stream()
                .sorted((p1, p2) -> {
                    long count1 = cultureCountMap.getOrDefault(p1.getId(), 0L);
                    long count2 = cultureCountMap.getOrDefault(p2.getId(), 0L);
                    if (count1 != count2) {
                        return Long.compare(count2, count1);
                    }
                    return Double.compare(p2.getSurface(), p1.getSurface());
                })
                .limit(3)
                .collect(Collectors.toList());

        topParcellesBox.getChildren().clear();
        String[] medals = {"\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49"};
        for (int i = 0; i < topParcelles.size(); i++) {
            Parcelle p = topParcelles.get(i);
            double occupation = occupationMap.getOrDefault(p.getId(), 0.0);
            long cultureCount = cultureCountMap.getOrDefault(p.getId(), 0L);
            topParcellesBox.getChildren().add(createTopParcelleCard(p, occupation, cultureCount, medals[i]));
        }
    }

    private HBox createTopParcelleCard(Parcelle parcelle, double occupationPercent, long cultureCount, String medal) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("top-parcelle-card");
        card.setPrefHeight(100);

        Label medalLabel = new Label(medal);
        medalLabel.getStyleClass().add("medal-icon");

        VBox infoBox = new VBox(5);
        Label nameLabel = new Label(parcelle.getNom());
        nameLabel.getStyleClass().add("parcelle-name-top");

        Label surfaceLabel = new Label(
                String.format("%.0f", parcelle.getSurface()) + " " + M2
                        + "  \u2022  " + String.format("%.0f%%", occupationPercent) + " occup\u00e9"
        );
        surfaceLabel.getStyleClass().add("parcelle-stats");

        Label culturesLabel = new Label(cultureCount + " cultures actives");
        culturesLabel.getStyleClass().add("parcelle-culture-count");

        infoBox.getChildren().addAll(nameLabel, surfaceLabel, culturesLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        VBox progressBox = new VBox(5);
        progressBox.setAlignment(Pos.CENTER_RIGHT);
        ProgressBar progressBar = new ProgressBar(occupationPercent / 100.0);
        progressBar.setPrefWidth(150);
        progressBar.getStyleClass().add("occupation-progress");

        Label percentLabel = new Label(String.format("%.0f%%", occupationPercent));
        percentLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4CAF50; -fx-font-size: 14px;");

        progressBox.getChildren().addAll(progressBar, percentLabel);
        card.getChildren().addAll(medalLabel, infoBox, progressBox);
        return card;
    }

    // ============================================================
    // RECOLTE SECTIONS
    // FIX: restored accented filter strings to match DB values
    // ============================================================
    private void loadRecoltePrevue() {
        // FIX: "R\u00e9colte Pr\u00e9vue" not "Recolte Prevue"
        List<Culture> recoltePrevue = allCultures.stream()
                .filter(c -> "R\u00e9colte Pr\u00e9vue".equals(c.getEtat()))
                .collect(Collectors.toList());

        recoltePrevueCountBadge.setText(String.valueOf(recoltePrevue.size()));
        recoltePrevueBox.getChildren().clear();

        if (recoltePrevue.isEmpty()) {
            Label emptyLabel = new Label("Aucune culture \u00e0 r\u00e9colter bient\u00f4t");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #888; -fx-padding: 20;");
            recoltePrevueBox.getChildren().add(emptyLabel);
        } else {
            for (Culture culture : recoltePrevue) {
                recoltePrevueBox.getChildren().add(createDashboardCultureCard(culture, false));
            }
        }
    }

    private void loadRecolteRetard() {
        // FIX: "R\u00e9colte en Retard" not "Recolte en Retard"
        List<Culture> recolteRetard = allCultures.stream()
                .filter(c -> "R\u00e9colte en Retard".equals(c.getEtat()))
                .collect(Collectors.toList());

        recolteRetardCountBadge.setText(String.valueOf(recolteRetard.size()));
        recolteRetardBox.getChildren().clear();

        if (recolteRetard.isEmpty()) {
            Label emptyLabel = new Label("Aucune culture en retard");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #888; -fx-padding: 20;");
            recolteRetardBox.getChildren().add(emptyLabel);
        } else {
            for (Culture culture : recolteRetard) {
                recolteRetardBox.getChildren().add(createDashboardCultureCard(culture, true));
            }
        }
    }

    private VBox createDashboardCultureCard(Culture culture, boolean isRetard) {
        VBox card = new VBox(8);
        card.getStyleClass().add("dashboard-culture-card");
        card.setAlignment(Pos.TOP_CENTER);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("culture-image");

        if (culture.getImg() != null && !culture.getImg().isEmpty()) {
            try {
                Image img = new Image(getClass().getResourceAsStream("/images/cultures/" + culture.getImg()));
                imageView.setImage(img);
            } catch (Exception e) {
                try {
                    imageView.setImage(new Image(getClass().getResourceAsStream("/images/cultures/default.png")));
                } catch (Exception ex) { /* Leave empty */ }
            }
        }

        Label nameLabel = new Label(culture.getNom());
        nameLabel.getStyleClass().add("culture-name");

        Label typeLabel = new Label(culture.getTypeCulture());
        typeLabel.getStyleClass().add("culture-info");

        Label etatLabel = new Label(culture.getEtat());
        etatLabel.getStyleClass().addAll("etat-badge",
                isRetard ? "etat-recolte-en-retard" : "etat-recolte-prevue");

        LocalDate recolteDate = culture.getDateRecolte().toLocalDate();
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), recolteDate);

        String dateText = isRetard
                ? "Retard: " + Math.abs(daysRemaining) + " jours"
                : "Dans " + daysRemaining + " jours";

        Label dateLabel = new Label(dateText);
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                + (isRetard ? "#f44336" : "#FF9800") + "; -fx-font-weight: bold;");

        card.getChildren().addAll(imageView, nameLabel, typeLabel, etatLabel, dateLabel);
        return card;
    }

    // ============================================================
    // NAVIGATION
    // ============================================================
    @FXML
    private void goToGererCulture() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) {
            controller.navigateToAfficherCulture();
        }
    }

    @FXML
    private void goToGererParcelle() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) {
            controller.navigateToParcelle();
        }
    }
}