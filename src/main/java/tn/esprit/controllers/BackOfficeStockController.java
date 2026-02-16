package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Produit;
import tn.esprit.entities.Stock;
import tn.esprit.services.ServiceProduit;
import tn.esprit.services.ServiceStock;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BackOfficeStockController {

    @FXML private TextField txtRecherche;
    @FXML private Label statTotalProduits;
    @FXML private Label statStocksDisponibles;
    @FXML private Label statAlertesCritique;
    @FXML private Label statValeurTotale;
    @FXML private BarChart<String, Number> chartCategories;
    @FXML private GridPane gridProduits;
    @FXML private GridPane gridStocks;
    @FXML private TableView<AlerteData> tableAlertes;

    private ServiceProduit serviceProduit = new ServiceProduit();
    private ServiceStock serviceStock = new ServiceStock();

    @FXML
    public void initialize() {
        // Charger les données
        rafraichir();
    }

    @FXML
    private void rafraichir() {
        loadStatistics();
        loadChart();
        loadAlertes();
        refreshProductList();
        refreshStockList();
    }

    private void loadStatistics() {
        try {
            List<Produit> produits = serviceProduit.getAllProduits();
            List<Stock> stocks = serviceStock.afficher();

            statTotalProduits.setText(String.valueOf(produits.size()));
            statStocksDisponibles.setText(String.valueOf(stocks.size()));

            long alertes = stocks.stream()
                    .filter(s -> s.getQuantiteActuelle() != null && s.getSeuilAlerte() != null && s.getQuantiteActuelle().compareTo(s.getSeuilAlerte()) <= 0)
                    .count();
            statAlertesCritique.setText(String.valueOf(alertes));

            BigDecimal valeurTotale = produits.stream()
                    .filter(p -> p.getPrixUnitaire() != null)
                    .map(p -> p.getPrixUnitaire())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            statValeurTotale.setText(valeurTotale + " DT");
        } catch (SQLException e) {
            System.err.println("Erreur chargement stats: " + e.getMessage());
        }
    }

    private void loadChart() {
        List<Produit> produits = serviceProduit.getAllProduits();
        Map<String, Long> repartition = produits.stream()
                .collect(Collectors.groupingBy(Produit::getCategorie, Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Produits par catégorie");
        for (Map.Entry<String, Long> entry : repartition.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        chartCategories.getData().clear();
        chartCategories.getData().add(series);
    }

    private void loadAlertes() {
        try {
            List<Stock> stocks = serviceStock.afficher();
            ObservableList<AlerteData> alertes = FXCollections.observableArrayList();
            for (Stock s : stocks) {
                if (s.getQuantiteActuelle() != null && s.getSeuilAlerte() != null && s.getQuantiteActuelle().compareTo(s.getSeuilAlerte()) <= 0) {
                    String nomProduit = "Produit ID " + s.getProduitId(); // Ajustez pour récupérer le nom réel
                    alertes.add(new AlerteData(nomProduit, s.getQuantiteActuelle(), s.getSeuilAlerte(), s.getUniteMesure()));
                }
            }
            tableAlertes.setItems(alertes);
        } catch (SQLException e) {
            System.err.println("Erreur chargement alertes: " + e.getMessage());
        }
    }

    private void refreshProductList() {
        gridProduits.getChildren().clear();
        try {
            List<Produit> produits = serviceProduit.getAllProduits();
            int col = 0, row = 0;
            for (Produit p : produits) {
                VBox card = createProductCard(p);
                gridProduits.add(card, col, row);
                col = (col + 1) % 2;
                if (col == 0) row++;
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement produits: " + e.getMessage());
        }
    }

    private void refreshStockList() {
        gridStocks.getChildren().clear();
        try {
            List<Stock> stocks = serviceStock.afficher();
            int col = 0, row = 0;
            for (Stock s : stocks) {
                VBox card = createStockCard(s);
                gridStocks.add(card, col, row);
                col = (col + 1) % 2;
                if (col == 0) row++;
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement stocks: " + e.getMessage());
        }
    }

    private VBox createProductCard(Produit p) {
        VBox card = new VBox(8);
        card.setStyle("-fx-padding: 15px; -fx-background-color: white; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefWidth(350);
        card.setPrefHeight(250);

        ImageView img = new ImageView();
        img.setFitHeight(100);
        img.setFitWidth(300);
        try {
            img.setImage(new Image(p.getPhotoUrl() != null ? p.getPhotoUrl() : "/images/default_product.png"));
        } catch (Exception e) {
            img.setImage(null);
        }

        Label name = new Label(p.getNom());
        name.setStyle("-fx-font-weight: bold;");
        Label cat = new Label("Catégorie: " + p.getCategorie());
        Label price = new Label("Prix: " + p.getPrixUnitaire() + " DT");

        Button edit = new Button("✏️ Modifier");
        edit.setOnAction(e -> editProduct(p));
        Button delete = new Button("🗑️ Supprimer");
        delete.setOnAction(e -> deleteProduct(p));
        HBox actions = new HBox(10, edit, delete);
        actions.setAlignment(javafx.geometry.Pos.CENTER);

        card.getChildren().addAll(img, name, cat, price, actions);
        return card;
    }

    private VBox createStockCard(Stock s) {
        VBox card = new VBox(8);
        card.setStyle("-fx-padding: 15px; -fx-background-color: white; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefWidth(350);
        card.setPrefHeight(200);

        Label id = new Label("ID: " + s.getId());
        Label prodId = new Label("Produit ID: " + s.getProduitId());
        Label qty = new Label("Quantité: " + s.getQuantiteActuelle());
        Label seuil = new Label("Seuil: " + s.getSeuilAlerte());
        Label unit = new Label("Unité: " + s.getUniteMesure());

        Button edit = new Button("✏️ Modifier");
        edit.setOnAction(e -> editStock(s));
        Button delete = new Button("🗑️ Supprimer");
        delete.setOnAction(e -> deleteStock(s));
        HBox actions = new HBox(10, edit, delete);
        actions.setAlignment(javafx.geometry.Pos.CENTER);

        card.getChildren().addAll(id, prodId, qty, seuil, unit, actions);
        return card;
    }

    @FXML
    private void goToAddProduct() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToAddProduct();
        }
    }

    @FXML
    private void goToAddStock() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToAddStock();
        }
    }

    private void editProduct(Produit p) {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.setProduitToEdit(p);
            MainLayoutController.getInstance().navigateToEditProduct();
        }
    }

    private void deleteProduct(Produit p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer " + p.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                serviceProduit.supprimer(p.getId());
                rafraichir(); // Recharger après suppression
            } catch (SQLException e) {
                showAlert("Erreur", "Suppression échouée: " + e.getMessage());
            }
        }
    }

    private void editStock(Stock s) {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.setStockToEdit(s);
            MainLayoutController.getInstance().navigateToEditStock();
        }
    }

    private void deleteStock(Stock s) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer stock ID " + s.getId() + " ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                serviceStock.supprimer(s.getId());
                rafraichir(); // Recharger après suppression
            } catch (SQLException e) {
                showAlert("Erreur", "Suppression échouée: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).show();
    }

    // Classe interne pour les données du tableau
    public static class AlerteData {
        private String nomProduit;
        private BigDecimal quantiteActuelle;
        private BigDecimal seuilAlerte;
        private String unite;

        public AlerteData(String nomProduit, BigDecimal quantiteActuelle, BigDecimal seuilAlerte, String unite) {
            this.nomProduit = nomProduit;
            this.quantiteActuelle = quantiteActuelle;
            this.seuilAlerte = seuilAlerte;
            this.unite = unite;
        }

        // Getters
        public String getNomProduit() { return nomProduit; }
        public BigDecimal getQuantiteActuelle() { return quantiteActuelle; }
        public BigDecimal getSeuilAlerte() { return seuilAlerte; }
        public String getUnite() { return unite; }
    }
}