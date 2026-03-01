package controllers;

import entity.Produit;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.ServiceStockProduit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommodityPriceController {

    private static final String API_KEY   = "af8a718b-1bee-4615-a820-893c531f85e9";
    private static final String BASE_URL  = "https://api.commoditypriceapi.com/v2/rates/latest";
    private static final double USD_TO_TND = 3.10;

    private static final List<CommodityInfo> COMMODITIES = List.of(
            // centimes=true  → API retourne US Cents/boisseau  (÷100 → USD, ÷27.2155 → USD/kg)
            new CommodityInfo("WHEAT",    "🌾 Blé",            "Semences",     true,  "US¢/Bu"),
            new CommodityInfo("CORN",     "🌽 Maïs",           "Semences",     true,  "US¢/Bu"),
            new CommodityInfo("SOYBEAN",  "🫘 Soja",           "Semences",     true,  "US¢/Bu"),
            new CommodityInfo("OATS",     "🌿 Avoine",         "Semences",     true,  "US¢/Bu"),
            new CommodityInfo("RICE",     "🍚 Riz",            "Semences",     true,  "US¢/cwt"),
            // centimes=false → API retourne USD/tonne          (÷1000 → USD/kg)
            new CommodityInfo("UREA",     "🧪 Urée",           "Fertilisants", false, "USD/T"),
            new CommodityInfo("PALM-OIL", "🌴 Huile de palme", "Fertilisants", false, "USD/T")
    );

    @FXML private FlowPane flowCommodities;
    @FXML private FlowPane flowComparaison;
    @FXML private Button   btnActualiser;
    @FXML private Label    lblMaj;
    @FXML private Label    lblStatut;
    @FXML private ComboBox<String> comboCategorie;

    private final ServiceStockProduit serviceProduit = new ServiceStockProduit();

    private Map<String, Double> rawPrices  = new HashMap<>();
    // Prix marché convertis en TND/kg — même unité que prixUnitaire des produits
    private Map<String, Double> prixTNDKg  = new HashMap<>();
    private List<Produit>       allProduits = new ArrayList<>();

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        comboCategorie.getItems().addAll("Toutes", "Semences", "Fertilisants");
        comboCategorie.setValue("Toutes");
        comboCategorie.valueProperty().addListener((obs, o, n) -> afficherCommodites());

        try {
            allProduits = serviceProduit.afficher();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        actualiser();
    }

    // ── Appel API ─────────────────────────────────────────────────────────────

    @FXML
    private void actualiser() {
        btnActualiser.setDisable(true);
        btnActualiser.setText("⏳ Chargement...");
        lblStatut.setText("Connexion à l'API...");
        lblStatut.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
        flowCommodities.getChildren().clear();
        flowComparaison.getChildren().clear();

        String symbols = COMMODITIES.stream()
                .map(c -> c.symbol)
                .reduce((a, b) -> a + "," + b).orElse("");

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "?symbols=" + symbols);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("x-api-key", API_KEY);

                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(code == 200 ? conn.getInputStream() : conn.getErrorStream())
                );
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String json = sb.toString();
                System.out.println("API Response: " + json);

                Map<String, Double> prices = parseJsonRates(json);
                String updateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

                Platform.runLater(() -> {
                    btnActualiser.setDisable(false);
                    btnActualiser.setText("🔄 Actualiser");
                    if (prices.isEmpty()) {
                        lblStatut.setText("❌ Réponse invalide");
                        lblStatut.setStyle("-fx-text-fill: #e53935;");
                        afficherErreur("Réponse API invalide :\n" + json.substring(0, Math.min(300, json.length())));
                    } else {
                        rawPrices = prices;
                        prixTNDKg = convertirEnTNDKg(prices);
                        lblMaj.setText("Mis à jour : " + updateTime + "  |  1 USD = " + USD_TO_TND + " TND");
                        lblStatut.setText("✅ " + prices.size() + " commodités chargées");
                        lblStatut.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                        afficherCommodites();
                        afficherComparaison();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnActualiser.setDisable(false);
                    btnActualiser.setText("🔄 Actualiser");
                    lblStatut.setText("❌ Erreur : " + e.getMessage());
                    lblStatut.setStyle("-fx-text-fill: #e53935;");
                });
            }
        }, "CommodityAPIThread").start();
    }

    // ── Conversion prix marché → TND/kg ──────────────────────────────────────

    /**
     * Tous les prix de l'API sont convertis en TND/kg pour être comparables
     * directement au prixUnitaire (TND/kg) des produits en DB.
     *
     * Semences (US¢/Bu) : val ÷ 100 → USD/Bu  ÷ 27.2155 → USD/kg  × 3.10 → TND/kg
     * Riz      (US¢/cwt): val ÷ 100 → USD/cwt ÷ 45.3592 → USD/kg  × 3.10 → TND/kg
     * Fertilisants (USD/T): val ÷ 1000 → USD/kg × 3.10 → TND/kg
     */
    private Map<String, Double> convertirEnTNDKg(Map<String, Double> raw) {
        Map<String, Double> result = new HashMap<>();
        for (CommodityInfo info : COMMODITIES) {
            Double val = raw.get(info.symbol);
            if (val == null) continue;
            double tndKg;
            if (info.centimes) {
                double usdParBu = val / 100.0;
                tndKg = "RICE".equals(info.symbol)
                        ? (usdParBu / 45.3592) * USD_TO_TND
                        : (usdParBu / 27.2155) * USD_TO_TND;
            } else {
                tndKg = (val / 1000.0) * USD_TO_TND;
            }
            result.put(info.symbol, tndKg);
        }
        return result;
    }

    // ── Matching catégorie → commodité ───────────────────────────────────────

    /**
     * Associe la catégorie d'un produit à la commodité de référence.
     * Gère singulier/pluriel, majuscules/minuscules et variantes courantes.
     */
    private CommodityInfo trouverCommodite(String categorie) {
        if (categorie == null) return null;
        String c = categorie.toLowerCase().trim();

        // Fertilisants
        if (c.contains("fertilisant") || c.contains("fertilisants") ||
                c.contains("engrais") || c.contains("urée") ||
                c.contains("uree")    || c.contains("npk"))
            return getCommodity("UREA");
        if (c.contains("huile") || c.contains("palme"))
            return getCommodity("PALM-OIL");

        // Semences spécifiques
        if (c.contains("maïs") || c.contains("mais") || c.contains("corn"))
            return getCommodity("CORN");
        if (c.contains("blé") || c.contains("ble") || c.contains("wheat"))
            return getCommodity("CORN"); // WHEAT absent de l'API → CORN comme référence
        if (c.contains("soja") || c.contains("soy"))
            return getCommodity("CORN");
        if (c.contains("riz")  || c.contains("rice"))
            return getCommodity("CORN");
        if (c.contains("avoine") || c.contains("oat"))
            return getCommodity("CORN");

        // Semences génériques (incluant "Semences" exact)
        if (c.contains("semence") || c.contains("graine") || c.contains("seed"))
            return getCommodity("CORN");

        return null;
    }

    private CommodityInfo getCommodity(String symbol) {
        return COMMODITIES.stream().filter(c -> c.symbol.equals(symbol)).findFirst().orElse(null);
    }

    // ── Affichage commodités ──────────────────────────────────────────────────

    private void afficherCommodites() {
        flowCommodities.getChildren().clear();
        String filtre = comboCategorie.getValue();

        for (CommodityInfo info : COMMODITIES) {
            if (!"Toutes".equals(filtre) && !info.categorie.equals(filtre)) continue;
            Double raw = rawPrices.get(info.symbol);
            Double tnd = prixTNDKg.get(info.symbol);
            flowCommodities.getChildren().add(creerCarteCommodite(info, raw, tnd));
        }

        if (flowCommodities.getChildren().isEmpty()) {
            Label vide = new Label("Aucune donnée disponible.");
            vide.setStyle("-fx-font-size: 14px; -fx-text-fill: #888;");
            flowCommodities.getChildren().add(vide);
        }
    }

    private VBox creerCarteCommodite(CommodityInfo info, Double raw, Double tnd) {
        boolean hasData = raw != null && raw > 0;
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(190);
        card.setMinWidth(170);
        card.setPadding(new Insets(14, 12, 14, 12));
        card.setStyle(
                "-fx-background-color: " + (hasData ? "white" : "#f5f5f5") + ";" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-color: " + (hasData ? "#c8d8b0" : "#ddd") + ";" +
                        "-fx-border-radius: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 3);"
        );

        Label nomLabel = new Label(info.displayName);
        nomLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #22301b;");
        nomLabel.setWrapText(true);
        nomLabel.setMaxWidth(170);
        nomLabel.setAlignment(Pos.CENTER);

        Label catLabel = new Label(info.categorie);
        catLabel.setStyle(
                "-fx-font-size: 10px; -fx-text-fill: white;" +
                        "-fx-background-color: " + (info.categorie.equals("Semences") ? "#5a9814" : "#1565c0") + ";" +
                        "-fx-background-radius: 4px; -fx-padding: 2px 6px;"
        );

        if (hasData) {
            Label rawLabel = new Label(String.format("%.2f", raw));
            rawLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
            Label uniteLabel = new Label(info.unite);
            uniteLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
            Label tndLabel = new Label(tnd != null ? String.format("≈ %.4f TND/kg", tnd) : "");
            tndLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5a9814; -fx-font-weight: bold;");
            card.getChildren().addAll(nomLabel, catLabel, rawLabel, uniteLabel, tndLabel);
        } else {
            Label naLabel = new Label("N/A");
            naLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #aaa;");
            card.getChildren().addAll(nomLabel, catLabel, naLabel);
        }
        return card;
    }

    // ── Comparaison produits (Semences + Fertilisants uniquement) ─────────────

    private void afficherComparaison() {
        flowComparaison.getChildren().clear();

        // prixUnitaire est en TND/kg — comparaison directe sans conversion
        List<Produit> comparables = allProduits.stream()
                .filter(p -> p.getPrixUnitaire() != null
                        && p.getCategorie() != null
                        && trouverCommodite(p.getCategorie()) != null)
                .toList();

        if (comparables.isEmpty()) {
            Label vide = new Label(
                    "Aucun produit de type Semences ou Fertilisants trouvé.\n" +
                            "La catégorie doit contenir : semence, engrais, fertilisant, blé, maïs, soja, urée..."
            );
            vide.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-font-style: italic;");
            vide.setWrapText(true);
            flowComparaison.getChildren().add(vide);
            return;
        }

        for (Produit p : comparables) {
            CommodityInfo ref    = trouverCommodite(p.getCategorie());
            Double marcheTNDKg  = ref != null ? prixTNDKg.get(ref.symbol) : null;
            double prixProduit  = p.getPrixUnitaire().doubleValue(); // déjà TND/kg
            flowComparaison.getChildren().add(creerCarteComparaison(p, ref, prixProduit, marcheTNDKg));
        }
    }

    private VBox creerCarteComparaison(Produit produit, CommodityInfo ref,
                                       double prixProduitTNDKg, Double marcheTNDKg) {
        VBox card = new VBox(10);
        card.setPrefWidth(380);
        card.setMinWidth(320);
        card.setPadding(new Insets(16, 18, 16, 18));

        String statut = "INCONNU", couleur = "#888", emoji = "❓";
        String conseil = "Pas de référence marché disponible pour cette catégorie.";
        String detail  = "";

        if (marcheTNDKg != null && marcheTNDKg > 0) {
            double ecart = (prixProduitTNDKg - marcheTNDKg) / marcheTNDKg * 100;
            if (ecart > 20) {
                statut = "SURÉVALUÉ";   couleur = "#e53935"; emoji = "📈";
                conseil = String.format("Votre prix est %.1f%% au-dessus du marché mondial.", ecart);
                detail  = "Envisagez de renégocier avec votre fournisseur.";
            } else if (ecart < -20) {
                statut = "SOUS-ÉVALUÉ"; couleur = "#1565c0"; emoji = "📉";
                conseil = String.format("Votre prix est %.1f%% en-dessous du marché mondial.", Math.abs(ecart));
                detail  = "Opportunité d'achat — le marché est plus cher.";
            } else {
                statut = "CORRECT";     couleur = "#2e7d32"; emoji = "✅";
                conseil = String.format("Prix dans la norme du marché mondial (écart %.1f%%).", ecart);
                detail  = "Pas d'action requise.";
            }
        }

        final String c = couleur;
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12px;" +
                        "-fx-border-color: " + c + "; -fx-border-width: 1.5px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 3);"
        );

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label nom = new Label("📦 " + produit.getNom());
        nom.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #22301b;");
        HBox.setHgrow(nom, Priority.ALWAYS);
        Label badge = new Label(emoji + " " + statut);
        badge.setStyle(
                "-fx-background-color: " + c + "; -fx-text-fill: white;" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 6px; -fx-padding: 3px 8px;"
        );
        header.getChildren().addAll(nom, badge);

        // Catégorie
        Label catLabel = new Label(produit.getCategorie());
        catLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

        // Ligne des prix — comparaison directe TND/kg ↔ TND/kg
        HBox prixRow = new HBox(16);
        prixRow.setAlignment(Pos.CENTER_LEFT);
        prixRow.setPadding(new Insets(8, 0, 8, 0));

        VBox prixVotreBox = new VBox(2);
        Label titrePV = new Label("Votre prix (TND/kg)");
        titrePV.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        Label valPV = new Label(String.format("%.4f TND/kg", prixProduitTNDKg));
        valPV.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #22301b;");
        prixVotreBox.getChildren().addAll(titrePV, valPV);
        prixRow.getChildren().add(prixVotreBox);

        if (marcheTNDKg != null && ref != null) {
            Label sep = new Label("↔");
            sep.setStyle("-fx-font-size: 18px; -fx-text-fill: #ccc;");

            VBox prixMarcheBox = new VBox(2);
            Label titreM = new Label("Marché " + ref.displayName + " (TND/kg)");
            titreM.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
            Label valM = new Label(String.format("%.4f TND/kg", marcheTNDKg));
            valM.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + c + ";");

            // Note de conversion transparente
            Double raw = rawPrices.get(ref.symbol);
            if (raw != null) {
                Label note = new Label(String.format(
                        "(%s %.2f → %.4f TND/kg  |  1 USD = %.2f TND)", ref.unite, raw, marcheTNDKg, USD_TO_TND));
                note.setStyle("-fx-font-size: 10px; -fx-text-fill: #bbb;");
                prixMarcheBox.getChildren().addAll(titreM, valM, note);
            } else {
                prixMarcheBox.getChildren().addAll(titreM, valM);
            }
            prixRow.getChildren().addAll(sep, prixMarcheBox);
        }

        // Conseil
        VBox conseilBox = new VBox(2);
        conseilBox.setStyle(
                "-fx-background-color: #f9f9f9; -fx-background-radius: 6px; -fx-padding: 8px 10px;"
        );
        Label conseilLabel = new Label("💡 " + conseil);
        conseilLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-font-weight: bold;");
        conseilLabel.setWrapText(true);
        conseilLabel.setMaxWidth(340);
        Label detailLabel = new Label(detail);
        detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888; -fx-font-style: italic;");
        conseilBox.getChildren().addAll(conseilLabel, detailLabel);

        // Note indicative
        Label noteLabel = new Label(
                "ℹ️ Comparaison indicative : prix marché = matière première brute (import mondial)." +
                        " Un écart est normal si votre produit est transformé ou conditionné."
        );
        noteLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa; -fx-font-style: italic;");
        noteLabel.setWrapText(true);
        noteLabel.setMaxWidth(340);

        card.getChildren().addAll(header, catLabel, prixRow, conseilBox, noteLabel);
        return card;
    }

    // ── Parsing JSON ──────────────────────────────────────────────────────────

    private Map<String, Double> parseJsonRates(String json) {
        Map<String, Double> prices = new HashMap<>();
        try {
            String block = null;
            for (String key : new String[]{"\"rates\"", "\"data\""}) {
                if (json.contains(key)) {
                    int start = json.indexOf(key) + key.length();
                    start = json.indexOf("{", start);
                    int end = findMatchingBrace(json, start);
                    block = json.substring(start + 1, end);
                    break;
                }
            }
            if (block == null) return prices;

            for (String part : block.split(",")) {
                part = part.trim();
                if (!part.contains(":")) continue;
                String[] kv = part.split(":", 2);
                String sym = kv[0].trim().replace("\"", "").replace("{", "").replace("}", "").trim();
                String val = kv[1].trim().replace("\"", "").replace("}", "").trim();
                try {
                    double d = Double.parseDouble(val);
                    if (!sym.isEmpty() && d > 0) prices.put(sym.toUpperCase(), d);
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing : " + e.getMessage());
        }
        return prices;
    }

    private int findMatchingBrace(String s, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < s.length(); i++) {
            if (s.charAt(i) == '{') depth++;
            else if (s.charAt(i) == '}') { depth--; if (depth == 0) return i; }
        }
        return s.length() - 1;
    }

    private void afficherErreur(String msg) {
        flowCommodities.getChildren().clear();
        VBox errBox = new VBox(10);
        errBox.setAlignment(Pos.CENTER);
        errBox.setPadding(new Insets(30));
        Label icon = new Label("❌");
        icon.setStyle("-fx-font-size: 40px;");
        Label txt = new Label(msg);
        txt.setStyle("-fx-font-size: 14px; -fx-text-fill: #e53935;");
        txt.setWrapText(true);
        txt.setMaxWidth(500);
        errBox.getChildren().addAll(icon, txt);
        flowCommodities.getChildren().add(errBox);
    }

    @FXML private void goToHome() {
        if (MainLayoutController.getInstance() != null) MainLayoutController.getInstance().navigateToHome();
    }
    @FXML private void goToProductList() {
        if (MainLayoutController.getInstance() != null) MainLayoutController.getInstance().navigateToProductList();
    }
    @FXML private void goToStockList() {
        if (MainLayoutController.getInstance() != null) MainLayoutController.getInstance().navigateToStockList();
    }

    private static class CommodityInfo {
        final String  symbol;
        final String  displayName;
        final String  categorie;
        final boolean centimes;
        final String  unite;

        CommodityInfo(String symbol, String displayName, String categorie, boolean centimes, String unite) {
            this.symbol      = symbol;
            this.displayName = displayName;
            this.categorie   = categorie;
            this.centimes    = centimes;
            this.unite       = unite;
        }
    }
}