package controllers;

import entity.Culture;
import entity.Parcelle;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import services.CultureService;
import services.ParcelleService;

import java.sql.SQLException;
import java.util.List;

/**
 * HomeContentController — AgriSense 360 Redesigned Home
 * ═══════════════════════════════════════════════════════
 * CHANGES vs previous version:
 *
 *  1. Added field:
 *       @FXML private ChatBotController chatbotController;
 *     This is auto-injected by JavaFX because home_content.fxml
 *     uses <fx:include fx:id="chatbot" .../> — JavaFX automatically
 *     creates a field named "<fx:id>Controller" in the parent
 *     controller. No other code change needed; the chatbot is
 *     fully self-contained in ChatBotController.
 *
 *  2. All existing @FXML injections and navigation methods are
 *     COMPLETELY UNCHANGED.
 */
public class HomeContentController {

    // ── Hero stat chips ─────────────────────────────────────────────
    @FXML private Label totalParcellesLabel;
    @FXML private Label totalCulturesLabel;
    @FXML private Label tauxOccupationLabel;
    @FXML private Label culturesRetardLabel;

    // ── Glass panel rows ────────────────────────────────────────────
    @FXML private Label glassParcellesLabel;
    @FXML private Label glassCulturesLabel;
    @FXML private Label glassCulturesRetesLabel;
    @FXML private Label glassCulturesRetardLabel;
    @FXML private Label glassTauxLabel;

    // ── Stats band ───────────────────────────────────────────────────
    @FXML private Label bandParcellesLabel;
    @FXML private Label bandCulturesLabel;
    @FXML private Label bandRetesLabel;
    @FXML private Label bandSurfaceLabel;

    // ── Chatbot (auto-injected from <fx:include fx:id="chatbot">) ────
    // JavaFX naming convention: fx:id="chatbot" → field "chatbotController"
    @FXML private ChatBotController chatbotController;

    // ── Services ────────────────────────────────────────────────────
    private final ParcelleService parcelleService = new ParcelleService();
    private final CultureService  cultureService  = new CultureService();

    private static final String M2 = "m\u00B2";

    /**
     * Called automatically by JavaFX after FXML injection.
     * Loads live stats from DB and populates all bound labels.
     * ChatBotController.initialize() is called automatically
     * by JavaFX for the included FXML — nothing extra needed here.
     */
    @FXML
    public void initialize() {
        try {
            List<Parcelle> parcelles = parcelleService.getAllParcelles();
            List<Culture>  cultures  = cultureService.getAllCultures();

            int    nbParcelles     = parcelles.size();
            int    nbCultures      = cultures.size();
            double totalSurface    = parcelles.stream().mapToDouble(Parcelle::getSurface).sum();
            double surfaceUtilisee = cultures.stream().mapToDouble(Culture::getSurface).sum();
            double taux            = totalSurface > 0 ? (surfaceUtilisee / totalSurface) * 100 : 0;

            long culturesPretes = cultures.stream()
                    .filter(c -> "Maturit\u00e9".equals(c.getEtat())
                            || "R\u00e9colte Pr\u00e9vue".equals(c.getEtat()))
                    .count();

            long culturesRetard = cultures.stream()
                    .filter(c -> "R\u00e9colte en Retard".equals(c.getEtat()))
                    .count();

            String surfaceText = String.format("%.0f", totalSurface) + " " + M2;
            String tauxText    = String.format("%.1f%%", taux);

            // Hero chips
            safeSet(totalParcellesLabel,  String.valueOf(nbParcelles));
            safeSet(totalCulturesLabel,   String.valueOf(nbCultures));
            safeSet(tauxOccupationLabel,  tauxText);
            safeSet(culturesRetardLabel,  String.valueOf(culturesRetard));

            // Glass panel
            safeSet(glassParcellesLabel,      String.valueOf(nbParcelles));
            safeSet(glassCulturesLabel,       String.valueOf(nbCultures));
            safeSet(glassCulturesRetesLabel,  String.valueOf(culturesPretes));
            safeSet(glassCulturesRetardLabel, String.valueOf(culturesRetard));
            safeSet(glassTauxLabel,           tauxText);

            // Stats band
            safeSet(bandParcellesLabel, String.valueOf(nbParcelles));
            safeSet(bandCulturesLabel,  String.valueOf(nbCultures));
            safeSet(bandRetesLabel,     String.valueOf(culturesPretes));
            safeSet(bandSurfaceLabel,   surfaceText);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void safeSet(Label label, String value) {
        if (label != null) label.setText(value);
    }

    // ═══════════════════════════════════════════════════════════════
    // NAVIGATION — completely unchanged
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void navigateToAnimals() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) controller.navigateToAnimals();
    }

    @FXML
    private void navigateToEquipment() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) controller.navigateToEquipment();
    }

    @FXML
    private void navigateToStock() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) controller.navigateToStock();
    }

    @FXML
    private void navigateToCulture() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) controller.navigateToCulture();
    }

    @FXML
    private void navigateToUsers() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) controller.navigateToUsers();
    }

    @FXML
    private void navigateToWorkers() {
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) controller.navigateToWorkers();
    }
}