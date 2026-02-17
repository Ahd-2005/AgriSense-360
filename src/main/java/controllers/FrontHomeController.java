package controllers;

import controllers.MainLayoutController;
import entity.Stock;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import services.ServiceStockProduit;
import services.ServiceStockStock;

import java.sql.SQLException;
import java.util.List;

public class FrontHomeController {

    @FXML private Label statProduits;
    @FXML private Label statStocks;
    @FXML private Label statAlertes;
    @FXML private Label alertesText;

    private ServiceStockProduit serviceProduit = new ServiceStockProduit();
    private ServiceStockStock serviceStock = new ServiceStockStock();

    @FXML
    public void initialize() {
        // Vérifier que les champs FXML sont injectés
        if (statProduits == null || statStocks == null || statAlertes == null || alertesText == null) {
            System.err.println("Erreur : Un ou plusieurs champs FXML ne sont pas injectés. Vérifiez les fx:id dans le FXML.");
            return;
        }
        loadStatistics();
        loadAlertes();
    }

    private void loadStatistics() {
        try {
            List<?> produits = serviceProduit.afficher();
            statProduits.setText(String.valueOf(produits.size()));

            List<?> stocks = serviceStock.afficher();
            statStocks.setText(String.valueOf(stocks.size()));

            long alertes = stocks.stream()
                    .map(s -> (Stock) s)
                    .filter(s -> s.getQuantiteActuelle() != null && s.getSeuilAlerte() != null && s.getQuantiteActuelle().compareTo(s.getSeuilAlerte()) <= 0)
                    .count();
            statAlertes.setText(String.valueOf(alertes));
        } catch (SQLException e) {
            e.printStackTrace();
            statProduits.setText("Erreur");
            statStocks.setText("Erreur");
            statAlertes.setText("Erreur");
        }
    }

    private void loadAlertes() {
        try {
            List<?> stocks = serviceStock.afficher();
            StringBuilder alertes = new StringBuilder();
            for (Object obj : stocks) {
                Stock s = (Stock) obj;
                if (s.getQuantiteActuelle() != null && s.getSeuilAlerte() != null && s.getQuantiteActuelle().compareTo(s.getSeuilAlerte()) <= 0) {
                    alertes.append("Stock ID ").append(s.getId()).append(" en alerte (Produit ID ").append(s.getProduitId()).append(").\n");
                }
            }
            if (alertes.length() > 0) {
                alertesText.setText(alertes.toString());
            } else {
                alertesText.setText("Aucune alerte pour le moment.");
            }
        } catch (SQLException e) {
            alertesText.setText("Erreur lors du chargement des alertes.");
        }
    }

    @FXML
    private void goToProductList() {
        if (controllers.MainLayoutController.getInstance() != null) {
            controllers.MainLayoutController.getInstance().navigateToProductList();
        } else {
            showAlert("Erreur", "Impossible de naviguer vers la liste des produits.");
        }
    }

    @FXML
    private void goToStockList() {
        if (controllers.MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToStockList();
        } else {
            showAlert("Erreur", "Impossible de naviguer vers la liste des stocks.");
        }
    }

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}