package controllers;

import controllers.MainLayoutController;
import entity.Culture;
import entity.Parcelle;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import services.CultureService;
import services.ParcelleService;

import java.sql.SQLException;
import java.util.List;

public class HomeContentController {

    // ── Singleton ────────────────────────────────────────────────────
    private static HomeContentController instance;
    public static HomeContentController getInstance() { return instance; }

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

    // ── Stats band ──────────────────────────────────────────────────
    @FXML private Label bandParcellesLabel;
    @FXML private Label bandCulturesLabel;
    @FXML private Label bandRetesLabel;
    @FXML private Label bandSurfaceLabel;

    // ── AgriBot button + notification badge ─────────────────────────
    @FXML private Button agriBotBtn;
    @FXML private Label  agriBotBadge;

    // ── Services ────────────────────────────────────────────────────
    private final ParcelleService parcelleService = new ParcelleService();
    private final CultureService  cultureService  = new CultureService();

    private static final String M2 = "m\u00B2";

    @FXML
    public void initialize() {
        instance = this;

        // Hover effect on AgriBot button
        if (agriBotBtn != null) {
            String normal = "-fx-background-color:rgba(255,255,255,0.18);" +
                    "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                    "-fx-background-radius:50;-fx-border-radius:50;" +
                    "-fx-border-color:rgba(255,255,255,0.45);-fx-border-width:1.5;" +
                    "-fx-cursor:hand;-fx-padding:10 22 10 16;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.25),8,0,0,2);";
            String hover  = "-fx-background-color:rgba(255,255,255,0.30);" +
                    "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                    "-fx-background-radius:50;-fx-border-radius:50;" +
                    "-fx-border-color:rgba(255,255,255,0.65);-fx-border-width:1.5;" +
                    "-fx-cursor:hand;-fx-padding:10 22 10 16;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.35),12,0,0,3);";
            agriBotBtn.setOnMouseEntered(e -> agriBotBtn.setStyle(hover));
            agriBotBtn.setOnMouseExited(e  -> agriBotBtn.setStyle(normal));
        }

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

            safeSet(totalParcellesLabel,  String.valueOf(nbParcelles));
            safeSet(totalCulturesLabel,   String.valueOf(nbCultures));
            safeSet(tauxOccupationLabel,  tauxText);
            safeSet(culturesRetardLabel,  String.valueOf(culturesRetard));

            safeSet(glassParcellesLabel,      String.valueOf(nbParcelles));
            safeSet(glassCulturesLabel,       String.valueOf(nbCultures));
            safeSet(glassCulturesRetesLabel,  String.valueOf(culturesPretes));
            safeSet(glassCulturesRetardLabel, String.valueOf(culturesRetard));
            safeSet(glassTauxLabel,           tauxText);

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

    // ── AgriBot button handler ───────────────────────────────────────
    @FXML
    private void openAgriBot() {
        MainLayoutController ctrl = MainLayoutController.getInstance();
        if (ctrl != null) ctrl.toggleChat();
    }

    /** Called by MainLayoutController when a bot reply arrives while chat is hidden */
    public void showAgriBotBadge(int count) {
        if (agriBotBadge == null) return;
        agriBotBadge.setText(count > 9 ? "9+" : String.valueOf(count));
        agriBotBadge.setVisible(true);
        agriBotBadge.setManaged(true);
    }

    /** Called by MainLayoutController when chat is opened/cleared */
    public void clearAgriBotBadge() {
        if (agriBotBadge == null) return;
        agriBotBadge.setVisible(false);
        agriBotBadge.setManaged(false);
    }

    // ── Navigation ───────────────────────────────────────────────────
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