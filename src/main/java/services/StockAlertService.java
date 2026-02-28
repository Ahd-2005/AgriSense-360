package services;

import entity.Produit;
import entity.Stock;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import services.NotificationService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StockAlertService {

    private static StockAlertService instance;
    private final ServiceStockStock serviceStock = new ServiceStockStock();
    private final ServiceStockProduit serviceProduit = new ServiceStockProduit();

    private ScheduledExecutorService scheduler;
    private static final int VERIFICATION_INTERVAL_MINUTES = 5;

    private AlertCallback alertCallback;

    public interface AlertCallback {
        void onAlertsUpdated(List<StockAlert> alerts);
    }

    public static class StockAlert {
        public final Stock stock;
        public final String nomProduit;
        public final String categorie;

        public StockAlert(Stock stock, String nomProduit, String categorie) {
            this.stock = stock;
            this.nomProduit = nomProduit;
            this.categorie = categorie;
        }
    }

    private StockAlertService() {}

    public static StockAlertService getInstance() {
        if (instance == null) instance = new StockAlertService();
        return instance;
    }

    public void setAlertCallback(AlertCallback callback) {
        this.alertCallback = callback;
    }

    public void demarrer() {
        // Vérification immédiate avec popup + notifications
        verifierEtNotifier(true);

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "StockAlertThread");
            t.setDaemon(true);
            return t;
        });

        // Vérification périodique : notifications sans popup
        scheduler.scheduleAtFixedRate(
                () -> verifierEtNotifier(false),
                VERIFICATION_INTERVAL_MINUTES,
                VERIFICATION_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public void arreter() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    public void verifierEtNotifier(boolean afficherPopup) {
        List<StockAlert> alertes = getStocksEnAlerte();

        Platform.runLater(() -> {
            // Badge sidebar
            if (alertCallback != null) {
                alertCallback.onAlertsUpdated(alertes);
            }

            if (!alertes.isEmpty()) {
                // Toast Windows + barre in-app (toujours, pas seulement au démarrage)
                NotificationService.getInstance().notifierStocksEnAlerte(alertes);

                // Popup détaillée seulement au démarrage
                if (afficherPopup) {
                    afficherPopupAlerte(alertes);
                }
            }
        });
    }

    public List<StockAlert> getStocksEnAlerte() {
        List<StockAlert> alertes = new ArrayList<>();
        try {
            List<Stock> stocks = serviceStock.afficher();
            for (Stock s : stocks) {
                if (s.getQuantiteActuelle() != null && s.getSeuilAlerte() != null) {
                    if (s.getQuantiteActuelle().compareTo(s.getSeuilAlerte()) < 0) {
                        String nomProduit = "Produit #" + s.getProduitId();
                        String categorie = "";
                        try {
                            Produit p = serviceProduit.recupererParId(s.getProduitId());
                            if (p != null) {
                                nomProduit = p.getNom();
                                categorie = p.getCategorie() != null ? p.getCategorie() : "";
                            }
                        } catch (SQLException ignored) {}
                        alertes.add(new StockAlert(s, nomProduit, categorie));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alertes;
    }

    private void afficherPopupAlerte(List<StockAlert> alertes) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("⚠️ Alertes Stock");
        alert.setHeaderText("⚠️ " + alertes.size() + " produit(s) en dessous du seuil d'alerte !");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        for (StockAlert a : alertes) {
            VBox row = new VBox(3);
            row.setStyle(
                    "-fx-background-color: #fff3e0;" +
                            "-fx-background-radius: 8px;" +
                            "-fx-border-color: #ff9800;" +
                            "-fx-border-radius: 8px;" +
                            "-fx-padding: 10px 14px;"
            );

            Label nomLabel = new Label("📦 " + a.nomProduit
                    + (a.categorie.isEmpty() ? "" : "  (" + a.categorie + ")"));
            nomLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #22301b;");

            Label quantiteLabel = new Label("Quantité actuelle : "
                    + a.stock.getQuantiteActuelle()
                    + " " + (a.stock.getUniteMesure() != null ? a.stock.getUniteMesure() : ""));
            quantiteLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #d32f2f; -fx-font-weight: bold;");

            Label seuilLabel = new Label("Seuil d'alerte : "
                    + a.stock.getSeuilAlerte()
                    + " " + (a.stock.getUniteMesure() != null ? a.stock.getUniteMesure() : ""));
            seuilLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");

            Label emplacementLabel = new Label("📍 " + (a.stock.getEmplacement() != null ? a.stock.getEmplacement() : "N/A"));
            emplacementLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

            row.getChildren().addAll(nomLabel, quantiteLabel, seuilLabel, emplacementLabel);
            content.getChildren().add(row);
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(Math.min(alertes.size() * 110, 400));
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        alert.getDialogPane().setContent(scrollPane);
        alert.getDialogPane().setPrefWidth(520);
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }
}
