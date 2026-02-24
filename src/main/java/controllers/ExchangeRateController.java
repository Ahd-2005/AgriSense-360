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
    private static final String API_KEY = "c8b45130c7af97fe2b679215";
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";

    @FXML private ComboBox<String> comboBaseDevise;
    @FXML private ComboBox<String> comboSource;
    @FXML private ComboBox<String> comboCible;
    @FXML private ComboBox<String> comboProduitDevise;
    @FXML private FlowPane flowTaux;
    @FXML private FlowPane flowProduits;
    @FXML private TextField txtMontant;
    @FXML private Label lblResultatConversion;
    @FXML private Label lblTauxUtilise;
    @FXML private Label lblDerniereMAJ;
    @FXML private Button btnActualiser;

    private Map<String, Double> currentRates = new HashMap<>();
    private ServiceStockProduit serviceProduit = new ServiceStockProduit();

    // Toutes les devises supportées par ExchangeRate-API
    private static final List<String> ALL_CURRENCIES = Arrays.asList(
            "AED","AFN","ALL","AMD","ANG","AOA","ARS","AUD","AWG","AZN",
            "BAM","BBD","BDT","BGN","BHD","BIF","BMD","BND","BOB","BRL",
            "BSD","BTN","BWP","BYN","BZD","CAD","CDF","CHF","CLP","CNY",
            "COP","CRC","CUP","CVE","CZK","DJF","DKK","DOP","DZD","EGP",
            "ERN","ETB","EUR","FJD","FKP","FOK","GBP","GEL","GGP","GHS",
            "GIP","GMD","GNF","GTQ","GYD","HKD","HNL","HRK","HTG","HUF",
            "IDR","ILS","IMP","INR","IQD","IRR","ISK","JEP","JMD","JOD",
            "JPY","KES","KGS","KHR","KID","KMF","KRW","KWD","KYD","KZT",
            "LAK","LBP","LKR","LRD","LSL","LYD","MAD","MDL","MGA","MKD",
            "MMK","MNT","MOP","MRU","MUR","MVR","MWK","MXN","MYR","MZN",
            "NAD","NGN","NIO","NOK","NPR","NZD","OMR","PAB","PEN","PGK",
            "PHP","PKR","PLN","PYG","QAR","RON","RSD","RUB","RWF","SAR",
            "SBD","SCR","SDG","SEK","SGD","SHP","SLE","SLL","SOS","SRD",
            "SSP","STN","SYP","SZL","THB","TJS","TMT","TND","TOP","TRY",
            "TTD","TVD","TWD","TZS","UAH","UGX","USD","UYU","UZS","VES",
            "VND","VUV","WST","XAF","XCD","XDR","XOF","XPF","YER","ZAR",
            "ZMW","ZWL"
    );

    @FXML
    public void initialize() {
        comboBaseDevise.getItems().addAll(ALL_CURRENCIES);
        comboSource.getItems().addAll(ALL_CURRENCIES);
        comboCible.getItems().addAll(ALL_CURRENCIES);
        comboProduitDevise.getItems().addAll(ALL_CURRENCIES);

        // Valeurs par défaut
        comboBaseDevise.setValue("TND");
        comboSource.setValue("TND");
        comboCible.setValue("EUR");
        comboProduitDevise.setValue("EUR");

        // Charger les taux au démarrage
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

        // Appel API dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                String urlStr = BASE_URL + API_KEY + "/latest/" + baseDevise;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                // Parse JSON avec json-simple
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(sb.toString());
                String result = (String) json.get("result");

                if ("success".equals(result)) {
                    JSONObject rates = (JSONObject) json.get("conversion_rates");
                    Map<String, Double> newRates = new HashMap<>();
                    for (Object key : rates.keySet()) {
                        Object val = rates.get(key);
                        double rate = val instanceof Long ? ((Long) val).doubleValue() : (Double) val;
                        newRates.put((String) key, rate);
                    }

                    String updateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

                    Platform.runLater(() -> {
                        currentRates = newRates;
                        afficherTaux(baseDevise, newRates);
                        lblDerniereMAJ.setText("Dernière MAJ : " + updateTime);
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
                    showError("Erreur de connexion : " + e.getMessage());
                    btnActualiser.setDisable(false);
                    btnActualiser.setText("🔄 Actualiser");
                });
            }
        }).start();
    }

    private void afficherTaux(String base, Map<String, Double> rates) {
        flowTaux.getChildren().clear();

        // Trier par nom de devise
        List<String> sortedKeys = new ArrayList<>(rates.keySet());
        Collections.sort(sortedKeys);

        for (String devise : sortedKeys) {
            double rate = rates.get(devise);

            VBox card = new VBox(4);
            card.setAlignment(javafx.geometry.Pos.CENTER);
            card.setPrefWidth(140);
            card.setMinWidth(140);
            card.setStyle(
                    "-fx-background-color: #f8faf5;" +
                            "-fx-background-radius: 10px;" +
                            "-fx-border-color: #c8d8b0;" +
                            "-fx-border-radius: 10px;" +
                            "-fx-padding: 10px 8px;"
            );

            Label deviseLabel = new Label(devise);
            deviseLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #22301b;");

            Label rateLabel = new Label(String.format("%.4f", rate));
            rateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5a9814;");

            Label baseLabel = new Label("1 " + base + " =");
            baseLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaa;");

            card.getChildren().addAll(deviseLabel, baseLabel, rateLabel);
            flowTaux.getChildren().add(card);
        }
    }

    @FXML
    private void convertir() {
        String montantStr = txtMontant.getText().trim();
        String source = comboSource.getValue();
        String cible = comboCible.getValue();

        if (montantStr.isEmpty()) {
            lblResultatConversion.setText("⚠️ Entrez un montant.");
            lblResultatConversion.setStyle("-fx-font-size: 18px; -fx-text-fill: #d32f2f;");
            return;
        }
        if (source == null || cible == null) {
            lblResultatConversion.setText("⚠️ Sélectionnez les devises.");
            lblResultatConversion.setStyle("-fx-font-size: 18px; -fx-text-fill: #d32f2f;");
            return;
        }

        double montant;
        try {
            montant = Double.parseDouble(montantStr.replace(",", "."));
        } catch (NumberFormatException e) {
            lblResultatConversion.setText("⚠️ Montant invalide.");
            lblResultatConversion.setStyle("-fx-font-size: 18px; -fx-text-fill: #d32f2f;");
            return;
        }

        // Si les taux sont chargés, convertir localement
        if (!currentRates.isEmpty()) {
            double resultat = convertLocallement(montant, source, cible);
            if (resultat >= 0) {
                lblResultatConversion.setText(String.format("%.2f %s = %.4f %s", montant, source, resultat, cible));
                lblResultatConversion.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #5a9814;");

                double tauxDirect = resultat / montant;
                lblTauxUtilise.setText(String.format("Taux : 1 %s = %.6f %s", source, tauxDirect, cible));
                return;
            }
        }

        // Sinon, appel API direct
        lblResultatConversion.setText("⏳ Conversion en cours...");
        new Thread(() -> {
            try {
                String urlStr = BASE_URL + API_KEY + "/pair/" + source + "/" + cible + "/" + montant;
                URL url = new URL(urlStr);
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
                    Object convObj = json.get("conversion_result");
                    double converted = convObj instanceof Long ? ((Long) convObj).doubleValue() : (Double) convObj;
                    Object rateObj = json.get("conversion_rate");
                    double rate = rateObj instanceof Long ? ((Long) rateObj).doubleValue() : (Double) rateObj;

                    final double finalConverted = converted;
                    final double finalRate = rate;
                    Platform.runLater(() -> {
                        lblResultatConversion.setText(String.format("%.2f %s = %.4f %s", montant, source, finalConverted, cible));
                        lblResultatConversion.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #5a9814;");
                        lblTauxUtilise.setText(String.format("Taux : 1 %s = %.6f %s", source, finalRate, cible));
                    });
                } else {
                    Platform.runLater(() -> showError("Erreur API conversion."));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showError("Erreur : " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void convertirProduits() {
        String cible = comboProduitDevise.getValue();
        if (cible == null) {
            showError("Sélectionnez une devise cible.");
            return;
        }

        flowProduits.getChildren().clear();
        Label loading = new Label("Chargement des produits...");
        loading.setStyle("-fx-font-size: 14px; -fx-text-fill: #888;");
        flowProduits.getChildren().add(loading);

        new Thread(() -> {
            try {
                List<Produit> produits = serviceProduit.afficher();

                // Obtenir le taux TND -> cible
                double tauxTNDversCible = 1.0;
                if (!currentRates.isEmpty() && currentRates.containsKey(cible)) {
                    // Les taux actuels sont basés sur comboBaseDevise
                    String base = comboBaseDevise.getValue() != null ? comboBaseDevise.getValue() : "TND";
                    tauxTNDversCible = convertLocallement(1.0, "TND", cible);
                } else {
                    // Appel API
                    tauxTNDversCible = getTauxAPI("TND", cible);
                }

                final double taux = tauxTNDversCible;
                final String deviseCible = cible;

                Platform.runLater(() -> {
                    flowProduits.getChildren().clear();

                    if (produits.isEmpty()) {
                        flowProduits.getChildren().add(new Label("Aucun produit trouvé."));
                        return;
                    }

                    for (Produit p : produits) {
                        if (p.getPrixUnitaire() == null) continue;

                        double prixConverti = p.getPrixUnitaire().doubleValue() * taux;

                        VBox card = new VBox(6);
                        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        card.setPrefWidth(280);
                        card.setStyle(
                                "-fx-background-color: white;" +
                                        "-fx-background-radius: 12px;" +
                                        "-fx-border-color: #c8d8b0;" +
                                        "-fx-border-radius: 12px;" +
                                        "-fx-padding: 14px 16px;" +
                                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 3);"
                        );

                        Label nomLabel = new Label(p.getNom());
                        nomLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #22301b;");

                        Label categorieLabel = new Label(p.getCategorie() != null ? p.getCategorie() : "—");
                        categorieLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

                        Label prixOriginal = new Label(String.format("%.2f TND", p.getPrixUnitaire().doubleValue()));
                        prixOriginal.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaa; -fx-strikethrough: true;");

                        Label prixConvertiLabel = new Label(String.format("%.4f %s", prixConverti, deviseCible));
                        prixConvertiLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #5a9814;");

                        card.getChildren().addAll(nomLabel, categorieLabel, prixOriginal, prixConvertiLabel);
                        flowProduits.getChildren().add(card);
                    }

                    Label tauxInfo = new Label(String.format("Taux utilisé : 1 TND = %.6f %s", taux, deviseCible));
                    tauxInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaa; -fx-font-style: italic;");
                    flowProduits.getChildren().add(tauxInfo);
                });

            } catch (Exception e) {
                Platform.runLater(() -> showError("Erreur : " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Conversion locale en utilisant les taux déjà chargés.
     * Passe par la devise de base du ComboBox.
     */
    private double convertLocallement(double montant, String source, String cible) {
        if (currentRates.isEmpty()) return -1;
        String base = comboBaseDevise.getValue() != null ? comboBaseDevise.getValue() : "TND";

        if (source.equals(base) && currentRates.containsKey(cible)) {
            return montant * currentRates.get(cible);
        } else if (cible.equals(base) && currentRates.containsKey(source)) {
            return montant / currentRates.get(source);
        } else if (currentRates.containsKey(source) && currentRates.containsKey(cible)) {
            // Convertir source → base → cible
            double enBase = montant / currentRates.get(source);
            return enBase * currentRates.get(cible);
        }
        return -1;
    }

    /**
     * Appel API direct pour obtenir un taux de change.
     */
    private double getTauxAPI(String source, String cible) {
        try {
            String urlStr = BASE_URL + API_KEY + "/pair/" + source + "/" + cible;
            URL url = new URL(urlStr);
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
                Object rateObj = json.get("conversion_rate");
                return rateObj instanceof Long ? ((Long) rateObj).doubleValue() : (Double) rateObj;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1.0;
    }

    private void showError(String msg) {
        flowTaux.getChildren().clear();
        Label err = new Label("❌ " + msg);
        err.setStyle("-fx-font-size: 14px; -fx-text-fill: #d32f2f;");
        flowTaux.getChildren().add(err);
    }

    @FXML private void goToHome() {
        if (MainLayoutController.getInstance() != null)
            MainLayoutController.getInstance().navigateToHome();
    }

    @FXML private void goToProductList() {
        if (MainLayoutController.getInstance() != null)
            MainLayoutController.getInstance().navigateToProductList();
    }

    @FXML private void goToStockList() {
        if (MainLayoutController.getInstance() != null)
            MainLayoutController.getInstance().navigateToStockList();
    }
}
