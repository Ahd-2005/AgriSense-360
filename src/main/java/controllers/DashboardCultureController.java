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
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import services.CultureService;
import services.ParcelleService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardCultureController {

    private final CultureService cultureService = new CultureService();
    private final ParcelleService parcelleService = new ParcelleService();

    // Store data for popups
    private List<Culture> allCultures;
    private List<Parcelle> allParcelles;

    // Navigation cards
    @FXML private VBox gererCultureCard;
    @FXML private VBox gererParcelleCard;
    @FXML private ImageView cultureImage;
    @FXML private ImageView parcelleImage;

    // Charts
    @FXML private PieChart surfaceChart;
    @FXML private BarChart<String, Number> cultureTypeChart;
    @FXML private PieChart etatChart;

    // Summary statistics labels
    @FXML private Label totalCulturesLabel;
    @FXML private Label totalParcellesLabel;
    @FXML private Label surfaceTotaleLabel;
    @FXML private Label tauxOccupationLabel;
    @FXML private Label culturesRetesLabel;
    @FXML private Label culturesRetardLabel;

    // Top parcelles
    @FXML private VBox topParcellesBox;

    // Récolte sections
    @FXML private Label recoltePrevueCountBadge;
    @FXML private HBox recoltePrevueBox;
    @FXML private Label recolteRetardCountBadge;
    @FXML private HBox recolteRetardBox;

    @FXML
    public void initialize() {
        try {
            allCultures = cultureService.getAllCultures();
            allParcelles = parcelleService.getAllParcelles();
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
    }

    // ========== POPUP METHODS FOR CHARTS ========== //

    @FXML
    private void showSurfaceChartDetails(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initStyle(StageStyle.UTILITY);
            popup.setTitle("Détails - Répartition des Surfaces");

            VBox content = new VBox(20);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(30));
            content.setStyle("-fx-background-color: white;");

            Label title = new Label("📊 Répartition des Surfaces - Détails");
            title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2a5a3a;");

            double totalSurface = allParcelles.stream().mapToDouble(Parcelle::getSurface).sum();
            double surfaceUtilisee = allCultures.stream().mapToDouble(Culture::getSurface).sum();
            double surfaceDisponible = totalSurface - surfaceUtilisee;
            double pourcentageUtilisee = totalSurface > 0 ? (surfaceUtilisee / totalSurface) * 100 : 0;
            double pourcentageDisponible = totalSurface > 0 ? (surfaceDisponible / totalSurface) * 100 : 0;

            VBox detailsBox = new VBox(15);
            detailsBox.setAlignment(Pos.CENTER_LEFT);
            detailsBox.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 20; -fx-background-radius: 10px;");

            Label totalLabel = new Label(String.format("📏 Surface Totale: %.0f m²", totalSurface));
            totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            Label utiliseeLabel = new Label(String.format("🟢 Surface Utilisée: %.0f m² (%.1f%%)",
                    surfaceUtilisee, pourcentageUtilisee));
            utiliseeLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #4CAF50;");

            Label disponibleLabel = new Label(String.format("🔵 Surface Disponible: %.0f m² (%.1f%%)",
                    surfaceDisponible, pourcentageDisponible));
            disponibleLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #2196F3;");

            Label culturesLabel = new Label(String.format("🌱 Nombre de cultures: %d", allCultures.size()));
            culturesLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

            Label parcellesLabel = new Label(String.format("🌍 Nombre de parcelles: %d", allParcelles.size()));
            parcellesLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

            detailsBox.getChildren().addAll(totalLabel, utiliseeLabel, disponibleLabel, culturesLabel, parcellesLabel);

            content.getChildren().addAll(title, detailsBox);

            Scene scene = new Scene(content, 500, 350);
            popup.setScene(scene);
            popup.show();
        }
    }

    @FXML
    private void showCultureTypeChartDetails(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initStyle(StageStyle.UTILITY);
            popup.setTitle("Détails - Cultures par Type");

            VBox content = new VBox(20);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(30));
            content.setStyle("-fx-background-color: white;");

            Label title = new Label("🌱 Cultures par Type - Détails");
            title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2a5a3a;");

            Map<String, Long> typeCounts = allCultures.stream()
                    .collect(Collectors.groupingBy(Culture::getTypeCulture, Collectors.counting()));

            Map<String, Double> typeSurfaces = new HashMap<>();
            for (Culture c : allCultures) {
                typeSurfaces.merge(c.getTypeCulture(), c.getSurface(), Double::sum);
            }

            VBox detailsBox = new VBox(12);
            detailsBox.setAlignment(Pos.CENTER_LEFT);
            detailsBox.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 20; -fx-background-radius: 10px;");

            Map<String, String> typeColors = new HashMap<>();
            typeColors.put("Céréales", "#4CAF50");
            typeColors.put("Légumes", "#FF9800");
            typeColors.put("Fruits", "#2196F3");
            typeColors.put("Ornementales", "#9C27B0");

            for (Map.Entry<String, Long> entry : typeCounts.entrySet()) {
                String type = entry.getKey();
                long count = entry.getValue();
                double surface = typeSurfaces.getOrDefault(type, 0.0);
                String color = typeColors.getOrDefault(type, "#888");

                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);

                Label colorBox = new Label("  ");
                colorBox.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5px; -fx-padding: 10;");

                Label info = new Label(String.format("%s: %d cultures (%.0f m²)", type, count, surface));
                info.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

                row.getChildren().addAll(colorBox, info);
                detailsBox.getChildren().add(row);
            }

            Label totalLabel = new Label(String.format("Total: %d cultures", allCultures.size()));
            totalLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2a5a3a; -fx-padding: 10 0 0 0;");

            content.getChildren().addAll(title, detailsBox, totalLabel);

            Scene scene = new Scene(content, 450, 400);
            popup.setScene(scene);
            popup.show();
        }
    }

    @FXML
    private void showEtatChartDetails(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initStyle(StageStyle.UTILITY);
            popup.setTitle("Détails - Répartition par État");

            VBox content = new VBox(20);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(30));
            content.setStyle("-fx-background-color: white;");

            Label title = new Label("📈 Répartition par État - Détails");
            title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2a5a3a;");

            Map<String, Long> etatCounts = allCultures.stream()
                    .collect(Collectors.groupingBy(Culture::getEtat, Collectors.counting()));

            VBox detailsBox = new VBox(12);
            detailsBox.setAlignment(Pos.CENTER_LEFT);
            detailsBox.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 20; -fx-background-radius: 10px;");

            Map<String, String> etatColors = new HashMap<>();
            etatColors.put("Semis", "#4CAF50");
            etatColors.put("Croissance", "#FF9800");
            etatColors.put("Maturité", "#2196F3");
            etatColors.put("Récolte Prévue", "#FFB300");
            etatColors.put("Récolte en Retard", "#f44336");

            String[] etatOrder = {"Semis", "Croissance", "Maturité", "Récolte Prévue", "Récolte en Retard"};

            for (String etat : etatOrder) {
                long count = etatCounts.getOrDefault(etat, 0L);
                String color = etatColors.getOrDefault(etat, "#888");
                double percent = allCultures.size() > 0 ? (count * 100.0 / allCultures.size()) : 0;

                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);

                Label colorBox = new Label("  ");
                colorBox.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5px; -fx-padding: 10;");

                Label info = new Label(String.format("%s: %d cultures (%.1f%%)", etat, count, percent));
                info.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

                row.getChildren().addAll(colorBox, info);
                detailsBox.getChildren().add(row);
            }

            Label totalLabel = new Label(String.format("Total: %d cultures", allCultures.size()));
            totalLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2a5a3a; -fx-padding: 10 0 0 0;");

            content.getChildren().addAll(title, detailsBox, totalLabel);

            Scene scene = new Scene(content, 450, 450);
            popup.setScene(scene);
            popup.show();
        }
    }

    // ========== END POPUP METHODS ========== //

    private void loadSummaryStatistics() {
        totalCulturesLabel.setText(String.valueOf(allCultures.size()));
        totalParcellesLabel.setText(String.valueOf(allParcelles.size()));

        double totalSurface = allParcelles.stream().mapToDouble(Parcelle::getSurface).sum();
        double surfaceUtilisee = allCultures.stream().mapToDouble(Culture::getSurface).sum();

        surfaceTotaleLabel.setText(String.format("%.0f m²", totalSurface));

        double tauxOccupation = totalSurface > 0 ? (surfaceUtilisee / totalSurface) * 100 : 0;
        tauxOccupationLabel.setText(String.format("%.1f%%", tauxOccupation));

        long culturesPretes = allCultures.stream()
                .filter(c -> "Maturité".equals(c.getEtat()) || "Récolte Prévue".equals(c.getEtat()))
                .count();
        culturesRetesLabel.setText(String.valueOf(culturesPretes));

        long culturesRetard = allCultures.stream()
                .filter(c -> "Récolte en Retard".equals(c.getEtat()))
                .count();
        culturesRetardLabel.setText(String.valueOf(culturesRetard));
    }

    private void loadSurfaceChart() {
        double totalSurface = allParcelles.stream().mapToDouble(Parcelle::getSurface).sum();
        double surfaceUtilisee = allCultures.stream().mapToDouble(Culture::getSurface).sum();
        double surfaceDisponible = totalSurface - surfaceUtilisee;

        ObservableList<PieChart.Data> surfaceData = FXCollections.observableArrayList(
                new PieChart.Data(String.format("Utilisée (%.0f m²)", surfaceUtilisee), surfaceUtilisee),
                new PieChart.Data(String.format("Disponible (%.0f m²)", surfaceDisponible), surfaceDisponible)
        );

        surfaceChart.setData(surfaceData);
        applySurfaceChartColors(surfaceData);
    }

    private void applySurfaceChartColors(ObservableList<PieChart.Data> data) {
        String[] colors = {"#2196F3", "#4CAF50", "#FF9800", "#F44336", "#9C27B0"};

        for (int i = 0; i < data.size(); i++) {
            PieChart.Data item = data.get(i);
            if (item.getNode() != null) {
                String color = colors[i % colors.length];           // cycle colors if more slices
                item.getNode().setStyle("-fx-pie-color: " + color + ";");
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
        applyCultureTypeChartColors(series);
    }

    private void applyCultureTypeChartColors(XYChart.Series<String, Number> series) {
        Map<String, String> typeColors = new HashMap<>();
        typeColors.put("Céréales", "#4CAF50");
        typeColors.put("Légumes", "#FF9800");
        typeColors.put("Fruits", "#2196F3");
        typeColors.put("Ornementales", "#9C27B0");

        for (XYChart.Data<String, Number> data : series.getData()) {
            String color = typeColors.getOrDefault(data.getXValue(), "#888888");
            data.getNode().setStyle("-fx-bar-fill: " + color + ";");
        }
    }

    private void loadEtatChart() {
        Map<String, Long> etatCounts = allCultures.stream()
                .collect(Collectors.groupingBy(Culture::getEtat, Collectors.counting()));

        ObservableList<PieChart.Data> etatData = FXCollections.observableArrayList();
        etatCounts.forEach((etat, count) -> etatData.add(new PieChart.Data(etat + " (" + count + ")", count)));

        etatChart.setData(etatData);
        applyEtatChartColors(etatData, etatCounts);
    }

    private void applyEtatChartColors(ObservableList<PieChart.Data> data, Map<String, Long> etatCounts) {
        Map<String, String> etatColors = new HashMap<>();
        etatColors.put("Semis", "#4CAF50");
        etatColors.put("Croissance", "#FF9800");
        etatColors.put("Maturité", "#2196F3");
        etatColors.put("Récolte Prévue", "#FFB300");
        etatColors.put("Récolte en Retard", "#f44336");

        for (PieChart.Data chartData : data) {
            String label = chartData.getName();
            String etat = label.substring(0, label.lastIndexOf(" ("));
            String color = etatColors.getOrDefault(etat, "#888888");
            chartData.getNode().setStyle("-fx-pie-color: " + color + ";");
        }
    }

    private void loadTopParcelles() {
        Map<Integer, Double> occupationMap = new HashMap<>();
        Map<Integer, Long> cultureCountMap = new HashMap<>();

        for (Parcelle parcelle : allParcelles) {
            List<Culture> parcelCultures = allCultures.stream()
                    .filter(c -> c.getParcelleId() == parcelle.getId())
                    .collect(Collectors.toList());

            double used = parcelCultures.stream().mapToDouble(Culture::getSurface).sum();
            double occupationPercent = (used / parcelle.getSurface()) * 100;

            occupationMap.put(parcelle.getId(), occupationPercent);
            cultureCountMap.put(parcelle.getId(), (long) parcelCultures.size());
        }

        List<Parcelle> topParcelles = allParcelles.stream()
                .sorted((p1, p2) -> Double.compare(
                        occupationMap.getOrDefault(p2.getId(), 0.0),
                        occupationMap.getOrDefault(p1.getId(), 0.0)
                ))
                .limit(3)
                .collect(Collectors.toList());

        topParcellesBox.getChildren().clear();
        String[] medals = {"🥇", "🥈", "🥉"};
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

        Label surfaceLabel = new Label(String.format("📏 %.0f m² total • %.0f%% occupé",
                parcelle.getSurface(), occupationPercent));
        surfaceLabel.getStyleClass().add("parcelle-stats");

        Label culturesLabel = new Label(String.format("🌱 %d cultures actives", cultureCount));
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

    private void loadRecoltePrevue() {
        List<Culture> recoltePrevue = allCultures.stream()
                .filter(c -> "Récolte Prévue".equals(c.getEtat()))
                .collect(Collectors.toList());

        recoltePrevueCountBadge.setText(String.valueOf(recoltePrevue.size()));
        recoltePrevueBox.getChildren().clear();

        if (recoltePrevue.isEmpty()) {
            Label emptyLabel = new Label("✅ Aucune culture à récolter bientôt");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #888; -fx-padding: 20;");
            recoltePrevueBox.getChildren().add(emptyLabel);
        } else {
            for (Culture culture : recoltePrevue) {
                recoltePrevueBox.getChildren().add(createDashboardCultureCard(culture, false));
            }
        }
    }

    private void loadRecolteRetard() {
        List<Culture> recolteRetard = allCultures.stream()
                .filter(c -> "Récolte en Retard".equals(c.getEtat()))
                .collect(Collectors.toList());

        recolteRetardCountBadge.setText(String.valueOf(recolteRetard.size()));
        recolteRetardBox.getChildren().clear();

        if (recolteRetard.isEmpty()) {
            Label emptyLabel = new Label("✅ Aucune culture en retard");
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
                } catch (Exception ex) {
                    // Leave empty
                }
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

        String dateText = isRetard ?
                "⚠️ Retard: " + Math.abs(daysRemaining) + " jours" :
                "⏰ Dans " + daysRemaining + " jours";

        Label dateLabel = new Label(dateText);
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                (isRetard ? "#f44336" : "#FF9800") + "; -fx-font-weight: bold;");

        card.getChildren().addAll(imageView, nameLabel, typeLabel, etatLabel, dateLabel);
        return card;
    }

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