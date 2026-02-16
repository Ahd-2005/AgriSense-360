package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import tn.esprit.services.ServiceProduit;
import tn.esprit.services.ServiceStock;

import java.sql.SQLException;
import java.util.List;

public class FrontHomeController {

    @FXML private Label statProduits;
    @FXML private Label statStocks;
    @FXML private Label statAlertes;
    @FXML private Label alertesText;

    private ServiceProduit serviceProduit = new ServiceProduit();
    private ServiceStock serviceStock = new ServiceStock();

    @FXML
    public void initialize() {
        // Vérifier que les champs FXML sont injectés
        if (statProduits == null || statStocks == null || statAlertes == null || alertesText == null) {
            System.err.println("Erreur : Un ou plusieurs champs FXML ne sont pas injectés. Vérifiez les fx:id dans le FXML.");
            return;
        }
        // Charger les statistiques au démarrage
        loadStatistics();
        // Charger les alertes
        loadAlertes();
    }

    // Méthode pour charger les statistiques dynamiquement
    private void loadStatistics() {
        try {
            // Nombre de produits
            List<?> produits = serviceProduit.afficher();
            statProduits.setText(String.valueOf(produits.size()));

            // Nombre de stocks
            List<?> stocks = serviceStock.afficher();
            statStocks.setText(String.valueOf(stocks.size()));

            // Nombre d'alertes (stocks avec quantité <= seuil)
            long alertes = stocks.stream()
                    .map(s -> (tn.esprit.entities.Stock) s)
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

    // Méthode pour charger les alertes récentes
    private void loadAlertes() {
        try {
            List<?> stocks = serviceStock.afficher();
            StringBuilder alertes = new StringBuilder();
            for (Object obj : stocks) {
                tn.esprit.entities.Stock s = (tn.esprit.entities.Stock) obj;
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

    // Navigation vers la liste des produits (utilise MainLayoutController pour préserver le layout)
    @FXML
    private void goToProductList() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToProductList();
        } else {
            showAlert("Erreur", "Impossible de naviguer vers la liste des produits.");
        }
    }

    // Navigation vers la liste des stocks (utilise MainLayoutController pour préserver le layout)
    @FXML
    private void goToStockList() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToStockList();
        } else {
            showAlert("Erreur", "Impossible de naviguer vers la liste des stocks.");
        }
    }

    // Méthode utilitaire pour afficher des alertes
    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}