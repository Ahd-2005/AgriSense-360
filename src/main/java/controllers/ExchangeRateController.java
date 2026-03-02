package controllers;

import entity.Produit;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import services.ServiceStockProduit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExchangeRateController {
    private static final String API_KEY  = "c8b45130c7af97fe2b679215";
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";

    @FXML private ComboBox<String> comboBaseDevise;
    @FXML private ComboBox<String> comboSource;
    @FXML private ComboBox<String> comboCible;
    @FXML private ComboBox<String> comboProduitDevise;
    @FXML private FlowPane  flowTaux;
    @FXML private FlowPane  flowProduits;
    @FXML private TextField txtMontant;
    @FXML private Label     lblResultatConversion;
    @FXML private Label     lblTauxUtilise;
    @FXML private Label     lblDerniereMAJ;
    @FXML private Button    btnActualiser;

    private Map<String, Double> currentRates = new HashMap<>();
    private ServiceStockProduit serviceProduit = new ServiceStockProduit();

    // ── Devises sélectionnées (contexte agricole/commercial tunisien) ──────────
    private static final List<String> ALL_CURRENCIES = Arrays.asList(
            "TND","USD","EUR","GBP","SAR","AED","MAD","DZD","EGP","LYD",
            "JPY","CNY","CAD","AUD","CHF","INR","BRL","TRY","RUB","MXN",
            "NGN","ZAR","KWD","QAR","OMR","BHD","JOD","IQD","PKR","IDR"
    );

    private static final Map<String, String> FLAGS = new HashMap<>() {{
        put("TND","🇹🇳"); put("USD","🇺🇸"); put("EUR","🇪🇺"); put("GBP","🇬🇧");
        put("SAR","🇸🇦"); put("AED","🇦🇪"); put("MAD","🇲🇦"); put("DZD","🇩🇿");
        put("EGP","🇪🇬"); put("LYD","🇱🇾"); put("JPY","🇯🇵"); put("CNY","🇨🇳");
        put("CAD","🇨🇦"); put("AUD","🇦🇺"); put("CHF","🇨🇭"); put("INR","🇮🇳");
        put("BRL","🇧🇷"); put("TRY","🇹🇷"); put("RUB","🇷🇺"); put("MXN","🇲🇽");
        put("NGN","🇳🇬"); put("ZAR","🇿🇦"); put("KWD","🇰🇼"); put("QAR","🇶🇦");
        put("OMR","🇴🇲"); put("BHD","🇧🇭"); put("JOD","🇯🇴"); put("IQD","🇮🇶");
        put("PKR","🇵🇰"); put("IDR","🇮🇩");
    }};

    private static final Map<String, String> NAMES = new HashMap<>() {{
        put("TND","Dinar tunisien");     put("USD","Dollar US");        put("EUR","Euro");
        put("GBP","Livre sterling");     put("SAR","Riyal saoudien");   put("AED","Dirham EAU");
        put("MAD","Dirham marocain");    put("DZD","Dinar algérien");   put("EGP","Livre égyptienne");
        put("LYD","Dinar libyen");       put("JPY","Yen japonais");     put("CNY","Yuan chinois");
        put("CAD","Dollar canadien");    put("AUD","Dollar australien"); put("CHF","Franc suisse");
        put("INR","Roupie indienne");    put("BRL","Real brésilien");   put("TRY","Livre turque");
        put("RUB","Rouble russe");       put("MXN","Peso mexicain");    put("NGN","Naira nigérian");
        put("ZAR","Rand sud-africain"); put("KWD","Dinar koweïtien");  put("QAR","Riyal qatari");
        put("OMR","Rial omanais");       put("BHD","Dinar bahreïni");   put("JOD","Dinar jordanien");
        put("IQD","Dinar irakien");      put("PKR","Roupie pakistanaise"); put("IDR","Roupie indonésienne");
    }};

    @FXML
    public void initialize() {
        comboBaseDevise.getItems().addAll(ALL_CURRENCIES);
        comboSource.getItems().addAll(ALL_CURRENCIES);
        comboCible.getItems().addAll(ALL_CURRENCIES);
        comboProduitDevise.getItems().addAll(ALL_CURRENCIES);

        comboBaseDevise.setValue("TND");
        comboSource.setValue("TND");
        comboCible.setValue("EUR");
        comboProduitDevise.setValue("EUR");

        actualiserTaux();
    }

    @FXML
    private void actualiserTaux() {
        String base = comboBaseDevise.getValue();
        if (base == null) base = "TND";
        btnActualiser.setDisable(true);
        btnActualiser.setText("⏳ Chargement...");
        flowTaux.getChildren().clear();
        Label loading = new Label("Chargement des taux en cours...");
        loading.setStyle("-fx-font-size: 14px; -fx-text-fill: #888;");
        flowTaux.getChildren().add(loading);

        final String baseDevise = base;
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + API_KEY + "/latest/" + baseDevise);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(sb.toString());

                if ("success".equals(json.get("result"))) {
                    JSONObject rates = (JSONObject) json.get("conversion_rates");
                    Map<String, Double> newRates = new HashMap<>();
                    for (Object key : rates.keySet()) {
                        Object val = rates.get(key);
                        newRates.put((String) key, val instanceof Long ? ((Long) val).doubleValue() : (Double) val);
                    }
                    String updateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
                    Platform.runLater(() -> {
                        currentRates = newRates;
                        afficherTaux(baseDevise, newRates);
                        lblDerniereMAJ.setText("Mis à jour : " + updateTime);
                        btnActualiser.setDisable(false);
                        btnActualiser.setText("🔄 Actualiser");
                    });
                } else {
                    Platform.runLater(() -> {
                        showError("Erreur API : " + json.get("error-type"));
                        btnActualiser.setDisable(false);
                        btnActualiser.setText("🔄 Actualiser");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Erreur connexion : " + e.getMessage());
                    btnActualiser.setDisable(false);
                    btnActualiser.setText("🔄 Actualiser");
                });
            }
        }).start();
    }

    // ── Affichage des taux avec drapeaux ──────────────────────────────────────

    private void afficherTaux(String base, Map<String, Double> rates) {
        flowTaux.getChildren().clear();

        for (String devise : ALL_CURRENCIES) {
            Double rate = rates.get(devise);
            if (rate == null) continue;

            String flag = FLAGS.getOrDefault(devise, "🏳");
            String name = NAMES.getOrDefault(devise, devise);

            VBox card = new VBox(3);
            card.setAlignment(javafx.geometry.Pos.CENTER);
            card.setPrefWidth(155);
            card.setMinWidth(140);
            card.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 12px;" +
                            "-fx-border-color: #c8d8b0;" +
                            "-fx-border-radius: 12px;" +
                            "-fx-padding: 12px 8px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);"
            );

            Label flagLabel = new Label(flag + "  " + devise);
            flagLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #22301b;");

            Label nameLabel = new Label(name);
            nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa;");
            nameLabel.setWrapText(true);
            nameLabel.setMaxWidth(145);
            nameLabel.setAlignment(javafx.geometry.Pos.CENTER);

            Label baseLabel = new Label("1 " + base + " =");
            baseLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #bbb;");

            Label rateLabel = new Label(String.format("%.4f", rate));
            rateLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #5a9814;");

            card.getChildren().addAll(flagLabel, nameLabel, baseLabel, rateLabel);
            flowTaux.getChildren().add(card);
        }
    }

    // ── Conversion ────────────────────────────────────────────────────────────

    @FXML
    private void convertir() {
        String montantStr = txtMontant.getText().trim();
        String source = comboSource.getValue();
        String cible  = comboCible.getValue();

        if (montantStr.isEmpty()) { lblResultatConversion.setText("⚠️ Entrez un montant."); return; }
        if (source == null || cible == null) { lblResultatConversion.setText("⚠️ Sélectionnez les devises."); return; }

        double montant;
        try { montant = Double.parseDouble(montantStr.replace(",",".")); }
        catch (NumberFormatException e) { lblResultatConversion.setText("⚠️ Montant invalide."); return; }

        if (!currentRates.isEmpty()) {
            double res = convertLocallement(montant, source, cible);
            if (res >= 0) {
                lblResultatConversion.setText(String.format("%.2f %s = %.4f %s", montant, source, res, cible));
                lblResultatConversion.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #5a9814;");
                lblTauxUtilise.setText(String.format("Taux : 1 %s = %.6f %s", source, res/montant, cible));
                return;
            }
        }

        lblResultatConversion.setText("⏳ Conversion...");
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + API_KEY + "/pair/" + source + "/" + cible + "/" + montant);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET"); conn.setConnectTimeout(10000); conn.setReadTimeout(10000);
                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder(); String l;
                while ((l = r.readLine()) != null) sb.append(l);
                r.close();
                JSONObject json = (JSONObject) new JSONParser().parse(sb.toString());
                if ("success".equals(json.get("result"))) {
                    Object co = json.get("conversion_result");
                    Object ro = json.get("conversion_rate");
                    double converted = co instanceof Long ? ((Long)co).doubleValue() : (Double)co;
                    double rate      = ro instanceof Long ? ((Long)ro).doubleValue() : (Double)ro;
                    Platform.runLater(() -> {
                        lblResultatConversion.setText(String.format("%.2f %s = %.4f %s", montant, source, converted, cible));
                        lblResultatConversion.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #5a9814;");
                        lblTauxUtilise.setText(String.format("Taux : 1 %s = %.6f %s", source, rate, cible));
                    });
                }
            } catch (Exception e) { Platform.runLater(() -> showError("Erreur : " + e.getMessage())); }
        }).start();
    }

    @FXML
    private void convertirProduits() {
        String cible = comboProduitDevise.getValue();
        if (cible == null) { showError("Sélectionnez une devise cible."); return; }

        flowProduits.getChildren().clear();
        Label loading = new Label("Chargement des produits...");
        loading.setStyle("-fx-font-size: 14px; -fx-text-fill: #888;");
        flowProduits.getChildren().add(loading);

        new Thread(() -> {
            try {
                List<Produit> produits = serviceProduit.afficher();
                double taux = !currentRates.isEmpty() ? convertLocallement(1.0, "TND", cible) : getTauxAPI("TND", cible);
                final double t = taux; final String dc = cible;

                Platform.runLater(() -> {
                    flowProduits.getChildren().clear();
                    if (produits.isEmpty()) { flowProduits.getChildren().add(new Label("Aucun produit.")); return; }

                    String flagCible = FLAGS.getOrDefault(dc, "");
                    for (Produit p : produits) {
                        if (p.getPrixUnitaire() == null) continue;
                        double pc = p.getPrixUnitaire().doubleValue() * t;
                        VBox card = new VBox(6);
                        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        card.setPrefWidth(280);
                        card.setStyle(
                                "-fx-background-color: white; -fx-background-radius: 12px;" +
                                        "-fx-border-color: #c8d8b0; -fx-border-radius: 12px;" +
                                        "-fx-padding: 14px 16px;" +
                                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 3);");
                        Label nom = new Label(p.getNom());
                        nom.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #22301b;");
                        Label cat = new Label(p.getCategorie() != null ? p.getCategorie() : "—");
                        cat.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
                        Label orig = new Label(String.format("%.2f TND", p.getPrixUnitaire().doubleValue()));
                        orig.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaa; -fx-strikethrough: true;");
                        Label conv = new Label(String.format("%s %.4f %s", flagCible, pc, dc));
                        conv.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #5a9814;");
                        card.getChildren().addAll(nom, cat, orig, conv);
                        flowProduits.getChildren().add(card);
                    }
                    String f = FLAGS.getOrDefault(dc, "");
                    Label info = new Label(String.format("Taux : 1 TND = %.6f %s %s", t, f, dc));
                    info.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaa; -fx-font-style: italic;");
                    flowProduits.getChildren().add(info);
                });
            } catch (Exception e) { Platform.runLater(() -> showError("Erreur : " + e.getMessage())); }
        }).start();
    }

    private double convertLocallement(double montant, String source, String cible) {
        if (currentRates.isEmpty()) return -1;
        String base = comboBaseDevise.getValue() != null ? comboBaseDevise.getValue() : "TND";
        if (source.equals(base) && currentRates.containsKey(cible)) return montant * currentRates.get(cible);
        if (cible.equals(base)  && currentRates.containsKey(source)) return montant / currentRates.get(source);
        if (currentRates.containsKey(source) && currentRates.containsKey(cible))
            return (montant / currentRates.get(source)) * currentRates.get(cible);
        return -1;
    }

    private double getTauxAPI(String source, String cible) {
        try {
            URL url = new URL(BASE_URL + API_KEY + "/pair/" + source + "/" + cible);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET"); conn.setConnectTimeout(10000); conn.setReadTimeout(10000);
            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder(); String l;
            while ((l = r.readLine()) != null) sb.append(l);
            r.close();
            JSONObject json = (JSONObject) new JSONParser().parse(sb.toString());
            if ("success".equals(json.get("result"))) {
                Object ro = json.get("conversion_rate");
                return ro instanceof Long ? ((Long)ro).doubleValue() : (Double)ro;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 1.0;
    }

    private void showError(String msg) {
        flowTaux.getChildren().clear();
        Label err = new Label("❌ " + msg);
        err.setStyle("-fx-font-size: 14px; -fx-text-fill: #d32f2f;");
        flowTaux.getChildren().add(err);
    }

    @FXML private void goToHome()        { if (MainLayoutController.getInstance() != null) MainLayoutController.getInstance().navigateToStock(); }
    @FXML private void goToProductList() { if (MainLayoutController.getInstance() != null) MainLayoutController.getInstance().navigateToProductList(); }
    @FXML private void goToStockList()   { if (MainLayoutController.getInstance() != null) MainLayoutController.getInstance().navigateToStockList(); }
}
