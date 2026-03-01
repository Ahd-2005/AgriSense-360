package controllers;

import entity.Stock;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import services.ServiceStockProduit;
import services.ServiceStockStock;
import services.StockAlertService;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public class FrontHomeController {

    @FXML private Label statProduits;
    @FXML private Label statStocks;
    @FXML private Label statAlertes;
    @FXML private VBox  statAlertesCard;   // carte alertes — fond change selon état
    @FXML private VBox  alertesContainer;  // liste des alertes détaillées

    private final ServiceStockProduit serviceProduit = new ServiceStockProduit();
    private final ServiceStockStock   serviceStock   = new ServiceStockStock();

    @FXML
    public void initialize() {
        chargerStatistiques();
        chargerAlertes();
    }

    // ── Statistiques ──────────────────────────────────────────────────────────

    private void chargerStatistiques() {
        try {
            int nbProduits = serviceProduit.afficher().size();
            List<Stock> stocks = serviceStock.afficher();
            long nbAlertes = stocks.stream()
                    .filter(s -> s.getQuantiteActuelle() != null && s.getSeuilAlerte() != null
                            && s.getQuantiteActuelle().compareTo(s.getSeuilAlerte()) < 0)
                    .count();

            safeSetText(statProduits, String.valueOf(nbProduits));
            safeSetText(statStocks,   String.valueOf(stocks.size()));
            safeSetText(statAlertes,  String.valueOf(nbAlertes));

            // Couleur dynamique du chiffre alertes
            if (statAlertes != null) {
                statAlertes.setStyle(nbAlertes > 0
                        ? "-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #e53935;"
                        : "-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
            }
            // Fond de la carte alertes : rouge si alertes, vert sinon
            if (statAlertesCard != null) {
                statAlertesCard.setStyle(nbAlertes > 0
                        ? "-fx-background-color: #ffebee; -fx-background-radius: 14px; -fx-padding: 24px 32px;" +
                        "-fx-border-color: #ef9a9a; -fx-border-radius: 14px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 3);"
                        : "-fx-background-color: #e8f5e9; -fx-background-radius: 14px; -fx-padding: 24px 32px;" +
                        "-fx-border-color: #a5d6a7; -fx-border-radius: 14px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 3);");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            safeSetText(statProduits, "—");
            safeSetText(statStocks,   "—");
            safeSetText(statAlertes,  "—");
        }
    }

    // ── Alertes détaillées ────────────────────────────────────────────────────

    private void chargerAlertes() {
        if (alertesContainer == null) return;
        alertesContainer.getChildren().clear();

        List<StockAlertService.StockAlert> alertes =
                StockAlertService.getInstance().getStocksEnAlerte();

        if (alertes.isEmpty()) {
            Label ok = new Label("✅  Tous les stocks sont au-dessus du seuil d'alerte.");
            ok.setStyle("-fx-font-size: 15px; -fx-text-fill: #2e7d32; -fx-font-style: italic;");
            alertesContainer.getChildren().add(ok);
            return;
        }

        for (StockAlertService.StockAlert a : alertes) {
            HBox card = new HBox(16);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(12, 16, 12, 16));
            card.setStyle(
                    "-fx-background-color: #fff3e0; -fx-background-radius: 10px;" +
                            "-fx-border-color: #ff9800; -fx-border-radius: 10px;"
            );

            Label icone = new Label("⚠️");
            icone.setStyle("-fx-font-size: 22px;");

            VBox info = new VBox(4);
            String cat = (a.categorie != null && !a.categorie.isEmpty()) ? "  (" + a.categorie + ")" : "";
            Label nom = new Label("📦 " + a.nomProduit + cat);
            nom.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #22301b;");

            String u = a.stock.getUniteMesure() != null ? " " + a.stock.getUniteMesure() : "";
            Label qte = new Label(
                    "Actuel : " + a.stock.getQuantiteActuelle() + u +
                            "   |   Seuil : " + a.stock.getSeuilAlerte() + u
            );
            qte.setStyle("-fx-font-size: 13px; -fx-text-fill: #d32f2f; -fx-font-weight: bold;");
            info.getChildren().addAll(nom, qte);

            if (a.stock.getEmplacement() != null && !a.stock.getEmplacement().isEmpty()) {
                Label loc = new Label("📍 " + a.stock.getEmplacement());
                loc.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
                info.getChildren().add(loc);
            }

            card.getChildren().addAll(icone, info);
            alertesContainer.getChildren().add(card);
        }
    }

    // ── Actions & Navigation ──────────────────────────────────────────────────

    @FXML private void actualiserStats()    { chargerStatistiques(); chargerAlertes(); }

    @FXML private void goToProductList()    { nav(MainLayoutController::navigateToProductList); }
    @FXML private void goToStockList()      { nav(MainLayoutController::navigateToStockList); }
    @FXML private void goToCommodityPrice() { nav(MainLayoutController::navigateToCommodityPrice); }
    @FXML private void goToAddProduct()     { nav(MainLayoutController::navigateToAddProduct); }
    @FXML private void goToAddStock()       { nav(MainLayoutController::navigateToAddStock); }
    @FXML private void goToExchangeRate()   { nav(MainLayoutController::navigateToExchangeRate); }

    private void nav(Consumer<MainLayoutController> action) {
        MainLayoutController c = MainLayoutController.getInstance();
        if (c != null) action.accept(c);
    }

    private void safeSetText(Label label, String text) {
        if (label != null) label.setText(text);
    }
}
