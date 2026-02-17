package controllers;

import controllers.MainLayoutController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import entity.Stock;
import services.ServiceStockStock;

import java.sql.SQLException;
import java.util.List;

public class StockListController {

    @FXML private FlowPane flowStocks;
    @FXML private Button btnAjouterStock;

    private ServiceStockStock serviceStock = new ServiceStockStock();

    @FXML
    public void initialize() {
        // Vérifier que les champs FXML sont injectés
        if (flowStocks == null || btnAjouterStock == null) {
            System.err.println("Erreur : Un ou plusieurs champs FXML ne sont pas injectés. Vérifiez les fx:id dans le FXML.");
            return;
        }
        // Charger les stocks
        refreshStockList();
    }

    private void refreshStockList() {
        // Effacer le FlowPane
        flowStocks.getChildren().clear();

        // Charger les stocks depuis la base de données
        try {
            List<Stock> stocks = serviceStock.afficher();

            // Peupler le FlowPane avec des cartes
            for (Stock stock : stocks) {
                VBox card = createStockCard(stock);
                flowStocks.getChildren().add(card);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement des stocks : " + e.getMessage());
        }
    }

    // Méthode pour créer une carte stock
    private VBox createStockCard(Stock stock) {
        VBox card = new VBox();
        card.getStyleClass().add("stock-card");
        card.setSpacing(8.0);
        card.setPrefWidth(400);
        card.setMinWidth(400);
        card.setStyle("-fx-padding: 15px; -fx-background-color: white; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // Informations du stock
        Label idLabel = new Label("ID: " + stock.getId());
        idLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #22301b;");

        Label produitIdLabel = new Label("Produit ID: " + stock.getProduitId());
        produitIdLabel.setStyle("-fx-font-size: 14px;");

        Label quantiteLabel = new Label("Quantité: " + (stock.getQuantiteActuelle() != null ? stock.getQuantiteActuelle() : "N/A"));
        quantiteLabel.setStyle("-fx-font-size: 14px;");

        Label seuilLabel = new Label("Seuil Alerte: " + (stock.getSeuilAlerte() != null ? stock.getSeuilAlerte() : "N/A"));
        seuilLabel.setStyle("-fx-font-size: 14px;");

        Label uniteLabel = new Label("Unité: " + (stock.getUniteMesure() != null ? stock.getUniteMesure() : "N/A"));
        uniteLabel.setStyle("-fx-font-size: 14px;");

        Label dateReceptionLabel = new Label("Date Réception: " + (stock.getDateReception() != null ? stock.getDateReception() : "N/A"));
        dateReceptionLabel.setStyle("-fx-font-size: 14px;");

        Label dateExpirationLabel = new Label("Date Expiration: " + (stock.getDateExpiration() != null ? stock.getDateExpiration() : "N/A"));
        dateExpirationLabel.setStyle("-fx-font-size: 14px;");

        Label emplacementLabel = new Label("Emplacement: " + (stock.getEmplacement() != null ? stock.getEmplacement() : "N/A"));
        emplacementLabel.setStyle("-fx-font-size: 14px;");

        // Boutons d'actions
        Button editButton = new Button("✏️ Modifier");
        editButton.getStyleClass().add("primary");
        editButton.setPrefWidth(150.0);
        editButton.setPrefHeight(40.0);
        editButton.setOnAction(e -> modifierStock(stock));

        Button deleteButton = new Button("🗑️ Supprimer");
        deleteButton.getStyleClass().add("ghost");
        deleteButton.setPrefWidth(150.0);
        deleteButton.setPrefHeight(40.0);
        deleteButton.setOnAction(e -> supprimerStock(stock));

        HBox buttonsBox = new HBox(10.0, editButton, deleteButton);
        buttonsBox.setAlignment(javafx.geometry.Pos.CENTER);

        // Ajouter tous les éléments à la carte
        card.getChildren().addAll(idLabel, produitIdLabel, quantiteLabel, seuilLabel, uniteLabel, dateReceptionLabel, dateExpirationLabel, emplacementLabel, buttonsBox);

        return card;
    }

    /*private void modifierStock(Stock stock) {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToEditStock();
            // Si nécessaire, passez le stock au contrôleur chargé (nécessite une référence)
        } else {
            showAlert("Erreur", "Impossible de naviguer vers la modification.");
        }
    }*/
    private void modifierStock(Stock stock) {
        if (controllers.MainLayoutController.getInstance() != null) {
            controllers.MainLayoutController.setStockToEdit(stock);  // Passer le stock
            controllers.MainLayoutController.getInstance().navigateToEditStock();
        } else {
            showAlert("Erreur", "Impossible de naviguer vers la modification.");
        }
    }

    private void supprimerStock(Stock stock) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le stock ID: " + stock.getId());
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce stock ? Cette action est irréversible.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    serviceStock.supprimer(stock.getId());
                    showAlert("Succès", "Stock supprimé avec succès.");
                    refreshStockList();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Erreur lors de la suppression : " + e.getMessage());
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void goToHome() {
        if (controllers.MainLayoutController.getInstance() != null) {
            controllers.MainLayoutController.getInstance().navigateToHome();
        }
    }

    @FXML
    private void goToProductList() {
        if (controllers.MainLayoutController.getInstance() != null) {
            controllers.MainLayoutController.getInstance().navigateToProductList();
        }
    }

    @FXML
    private void ajouterStock() {
        if (controllers.MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToAddStock();
        }
    }
}