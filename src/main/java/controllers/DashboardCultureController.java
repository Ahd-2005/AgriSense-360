package controllers;

import entity.Culture;
import entity.Parcelle;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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

    // Navigation cards
    @FXML private VBox gererCultureCard;
    @FXML private VBox gererParcelleCard;

    // Statistics labels
    @FXML private Label totalCulturesLabel;
    @FXML private Label totalParcellesLabel;
    @FXML private Label surfaceUtiliseeLabel;
    @FXML private Label surfaceDisponibleLabel;

    // État counts
    @FXML private Label semisCountLabel;
    @FXML private Label croissanceCountLabel;
    @FXML private Label maturiteCountLabel;
    @FXML private Label recoltePrevueCountLabel;
    @FXML private Label recolteRetardCountLabel;

    // Top parcelles
    @FXML private VBox topParcellesBox;

    // Récolte sections
    @FXML private Label recoltePrevueCountBadge;
    @FXML private HBox recoltePrevueBox;
    @FXML private Label recolteRetardCountBadge;
    @FXML private HBox recolteRetardBox;

    @FXML
    public void initialize() {
        loadStatistics();
        loadTopParcelles();
        loadRecoltePrevue();
        loadRecolteRetard();
    }

    /**
     * Load all statistics
     */
    private void loadStatistics() {
        try {
            List<Culture> allCultures = cultureService.getAllCultures();
            List<Parcelle> allParcelles = parcelleService.getAllParcelles();

            // Total counts
            totalCulturesLabel.setText(String.valueOf(allCultures.size()));
            totalParcellesLabel.setText(String.valueOf(allParcelles.size()));

            // Surface calculations
            double totalSurface = allParcelles.stream()
                    .mapToDouble(Parcelle::getSurface)
                    .sum();

            double surfaceUtilisee = allCultures.stream()
                    .mapToDouble(Culture::getSurface)
                    .sum();

            double surfaceDisponible = totalSurface - surfaceUtilisee;

            surfaceUtiliseeLabel.setText(String.format("%.0f m²", surfaceUtilisee));
            surfaceDisponibleLabel.setText(String.format("%.0f m²", surfaceDisponible));

            // Count by état
            Map<String, Long> etatCounts = allCultures.stream()
                    .collect(Collectors.groupingBy(Culture::getEtat, Collectors.counting()));

            semisCountLabel.setText(String.valueOf(etatCounts.getOrDefault("Semis", 0L)));
            croissanceCountLabel.setText(String.valueOf(etatCounts.getOrDefault("Croissance", 0L)));
            maturiteCountLabel.setText(String.valueOf(etatCounts.getOrDefault("Maturité", 0L)));
            recoltePrevueCountLabel.setText(String.valueOf(etatCounts.getOrDefault("Récolte Prévue", 0L)));
            recolteRetardCountLabel.setText(String.valueOf(etatCounts.getOrDefault("Récolte en Retard", 0L)));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load top 3 parcelles by occupation
     */
    private void loadTopParcelles() {
        try {
            List<Parcelle> allParcelles = parcelleService.getAllParcelles();
            List<Culture> allCultures = cultureService.getAllCultures();

            // Calculate occupation for each parcelle
            Map<Integer, Double> occupationMap = new HashMap<>();
            for (Parcelle parcelle : allParcelles) {
                double used = allCultures.stream()
                        .filter(c -> c.getParcelleId() == parcelle.getId())
                        .mapToDouble(Culture::getSurface)
                        .sum();
                double occupationPercent = (used / parcelle.getSurface()) * 100;
                occupationMap.put(parcelle.getId(), occupationPercent);
            }

            // Sort and get top 3
            List<Parcelle> topParcelles = allParcelles.stream()
                    .sorted((p1, p2) -> Double.compare(
                            occupationMap.getOrDefault(p2.getId(), 0.0),
                            occupationMap.getOrDefault(p1.getId(), 0.0)
                    ))
                    .limit(3)
                    .collect(Collectors.toList());

            // Display top 3
            topParcellesBox.getChildren().clear();
            String[] medals = {"🥇", "🥈", "🥉"};
            for (int i = 0; i < topParcelles.size(); i++) {
                Parcelle p = topParcelles.get(i);
                double occupation = occupationMap.getOrDefault(p.getId(), 0.0);
                topParcellesBox.getChildren().add(createTopParcelleCard(p, occupation, medals[i]));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create card for top parcelle
     */
    private HBox createTopParcelleCard(Parcelle parcelle, double occupationPercent, String medal) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("top-parcelle-card");
        card.setPrefHeight(80);

        // Medal
        Label medalLabel = new Label(medal);
        medalLabel.getStyleClass().add("medal-icon");

        // Info
        VBox infoBox = new VBox(5);
        Label nameLabel = new Label(parcelle.getNom());
        nameLabel.getStyleClass().add("parcelle-name-top");

        Label statsLabel = new Label(String.format("%.0f m² total • %.0f%% occupé",
                parcelle.getSurface(), occupationPercent));
        statsLabel.getStyleClass().add("parcelle-stats");

        infoBox.getChildren().addAll(nameLabel, statsLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Progress bar
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

    /**
     * Load cultures with "Récolte Prévue"
     */
    private void loadRecoltePrevue() {
        try {
            List<Culture> cultures = cultureService.getAllCultures();

            List<Culture> recoltePrevue = cultures.stream()
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

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load cultures with "Récolte en Retard"
     */
    private void loadRecolteRetard() {
        try {
            List<Culture> cultures = cultureService.getAllCultures();

            List<Culture> recolteRetard = cultures.stream()
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

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create compact culture card for dashboard
     */
    private VBox createDashboardCultureCard(Culture culture, boolean isRetard) {
        VBox card = new VBox(8);
        card.getStyleClass().add("dashboard-culture-card");
        card.setAlignment(Pos.TOP_CENTER);

        // Image
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
                imageView.setImage(new Image(getClass().getResourceAsStream("/images/cultures/default.png")));
            }
        }

        // Name
        Label nameLabel = new Label(culture.getNom());
        nameLabel.getStyleClass().add("culture-name");

        // Type
        Label typeLabel = new Label(culture.getTypeCulture());
        typeLabel.getStyleClass().add("culture-info");

        // État badge
        Label etatLabel = new Label(culture.getEtat());
        etatLabel.getStyleClass().addAll("etat-badge",
                isRetard ? "etat-recolte-en-retard" : "etat-recolte-prevue");

        // Date info
        LocalDate recolteDate = culture.getDateRecolte().toLocalDate();
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), recolteDate);

        String dateText;
        if (isRetard) {
            dateText = "⚠️ Retard: " + Math.abs(daysRemaining) + " jours";
        } else {
            dateText = "⏰ Dans " + daysRemaining + " jours";
        }

        Label dateLabel = new Label(dateText);
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isRetard ? "#f44336" : "#FF9800") + "; -fx-font-weight: bold;");

        card.getChildren().addAll(imageView, nameLabel, typeLabel, etatLabel, dateLabel);
        return card;
    }

    /**
     * Navigate to Gérer Culture page
     */
    @FXML
    private void goToGererCulture() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) {
            controller.navigateToAfficherCulture();
        }
    }

    /**
     * Navigate to Gérer Parcelle page
     */
    @FXML
    private void goToGererParcelle() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) {
            controller.navigateToParcelle();
        }
    }
}