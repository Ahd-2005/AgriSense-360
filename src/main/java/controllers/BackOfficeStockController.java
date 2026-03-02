package controllers;

import controllers.MainLayoutController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import entity.Produit;
import entity.Stock;
import services.ServiceStockProduit;
import services.ServiceStockStock;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class BackOfficeStockController {

    @FXML private TextField txtRecherche;
    @FXML private Label statTotalProduits;
    @FXML private Label statStocksDisponibles;
    @FXML private Label statAlertesCritique;
    @FXML private Label statValeurTotale;
    @FXML private Label lblTotalProduits;
    @FXML private Label lblTotalStocks;
    @FXML private Label lblTotalAlertes;
    @FXML private BarChart<String, Number> chartCategories;
    @FXML private GridPane gridProduits;
    @FXML private GridPane gridStocks;
    @FXML private TableView<AlerteData> tableAlertes;

    private final ServiceStockProduit serviceProduit = new ServiceStockProduit();
    private final ServiceStockStock serviceStock = new ServiceStockStock();

    @FXML
    public void initialize() {
        setupGridPanes();
        rafraichir();
    }

    private void setupGridPanes() {
        // Configuration des GridPanes pour 2 colonnes
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHgrow(Priority.ALWAYS);

        gridProduits.getColumnConstraints().clear();
        gridProduits.getColumnConstraints().addAll(col1, col2);
        gridProduits.setHgap(20);
        gridProduits.setVgap(20);

        gridStocks.getColumnConstraints().clear();
        gridStocks.getColumnConstraints().addAll(col1, col2);
        gridStocks.setHgap(20);
        gridStocks.setVgap(20);
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

            long totalProduits = produits.size();
            long totalStocks = stocks.size();
            long alertes = stocks.stream()
                    .filter(s -> s.getQuantiteActuelle() != null && s.getSeuilAlerte() != null &&
                            s.getQuantiteActuelle().compareTo(s.getSeuilAlerte()) <= 0)
                    .count();
            BigDecimal valeurTotale = produits.stream()
                    .map(Produit::getPrixUnitaire)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            statTotalProduits.setText(String.valueOf(totalProduits));
            statStocksDisponibles.setText(String.valueOf(totalStocks));
            statAlertesCritique.setText(String.valueOf(alertes));
            statValeurTotale.setText(valeurTotale + " DT");

            if (lblTotalProduits != null) lblTotalProduits.setText(totalProduits + " produit(s)");
            if (lblTotalStocks != null) lblTotalStocks.setText(totalStocks + " stock(s)");
            if (lblTotalAlertes != null) lblTotalAlertes.setText(alertes + " alerte(s)");
        } catch (SQLException e) {
            System.err.println("Erreur chargement stats: " + e.getMessage());
        }
    }

    private void loadChart() {
        try {
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
        } catch (Exception e) {
            System.err.println("Erreur chargement graphique: " + e.getMessage());
        }
    }

    private void loadAlertes() {
        try {
            List<Stock> stocks = serviceStock.afficher();
            ObservableList<AlerteData> alertes = FXCollections.observableArrayList();
            for (Stock s : stocks) {
                if (s.getQuantiteActuelle() != null && s.getSeuilAlerte() != null &&
                        s.getQuantiteActuelle().compareTo(s.getSeuilAlerte()) <= 0) {
                    String nomProduit = getNomProduit(s.getProduitId());
                    String statut = s.getQuantiteActuelle().compareTo(s.getSeuilAlerte()) <= 0 ?
                            "🔴 Critique" : "⚠️ Attention";
                    alertes.add(new AlerteData(nomProduit, s.getQuantiteActuelle(),
                            s.getSeuilAlerte(), s.getUniteMesure(), statut));
                }
            }
            tableAlertes.setItems(alertes);
        } catch (SQLException e) {
            System.err.println("Erreur chargement alertes: " + e.getMessage());
        }
    }

    private String getNomProduit(int produitId) {
        try {
            Produit p = serviceProduit.recupererParId(produitId);
            return p != null ? p.getNom() : "Produit #" + produitId;
        } catch (SQLException e) {
            return "Produit #" + produitId;
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

                col++;
                if (col >= 2) {
                    col = 0;
                    row++;
                }
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

                col++;
                if (col >= 2) {
                    col = 0;
                    row++;
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement stocks: " + e.getMessage());
        }
    }

    private VBox createProductCard(Produit p) {
        VBox card = new VBox(8);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setPrefHeight(220);
        card.setMaxHeight(220);

        // Image
        ImageView img = new ImageView();
        img.setFitHeight(80);
        img.setFitWidth(280);
        img.setPreserveRatio(true);
        String photoUrl = p.getPhotoUrl();
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            try {
                Image image = new Image(
                        photoUrl.startsWith("file:") || photoUrl.startsWith("http")
                                ? photoUrl
                                : getClass().getResource(photoUrl).toExternalForm(), true);
                img.setImage(image);
            } catch (Exception e) {
                loadDefaultImage(img);
            }
        } else {
            loadDefaultImage(img);
        }

        // Nom
        Label name = new Label(p.getNom());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        name.setMaxWidth(280);

        // Catégorie
        Label cat = new Label("Catégorie: " + (p.getCategorie() != null ? p.getCategorie() : "Non catégorisé"));
        cat.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Prix
        Label price = new Label("Prix: " + (p.getPrixUnitaire() != null ? p.getPrixUnitaire() + " DT" : "Non défini"));
        price.setStyle("-fx-font-size: 12px; -fx-text-fill: #4caf50; -fx-font-weight: bold;");

        // Boutons
        HBox actions = new HBox(8);
        actions.setAlignment(javafx.geometry.Pos.CENTER);

        Button edit = new Button("✏️ Modifier");
        edit.setPrefWidth(130);
        edit.setPrefHeight(32);
        edit.getStyleClass().add("small-primary");
        edit.setOnAction(e -> editProduct(p));

        Button delete = new Button("🗑️ Supprimer");
        delete.setPrefWidth(130);
        delete.setPrefHeight(32);
        delete.getStyleClass().add("small-danger");
        delete.setOnAction(e -> deleteProduct(p));

        actions.getChildren().addAll(edit, delete);

        card.getChildren().addAll(img, name, cat, price, actions);
        return card;
    }
    private void loadDefaultImage(ImageView imageView) {
        try {
            String defaultUrl = getClass().getResource("/images/default_product.png").toExternalForm();
            imageView.setImage(new Image(defaultUrl, 320, 120, true, true));
        } catch (Exception e) {
            imageView.setImage(null);
        }
    }
    private VBox createStockCard(Stock s) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stock-card");
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setPrefHeight(150);
        card.setMaxHeight(150);

        String nomProduit = getNomProduitCourt(s.getProduitId());

        // Déterminer le statut
        String statut = "";
        String statutStyle = "";
        if (s.getQuantiteActuelle() != null && s.getSeuilAlerte() != null) {
            if (s.getQuantiteActuelle().compareTo(s.getSeuilAlerte()) <= 0) {
                statut = "⚠️ ALERTE";
                statutStyle = "-fx-text-fill: #f44336; -fx-font-weight: bold;";
            } else if (s.getQuantiteActuelle().compareTo(s.getSeuilAlerte().multiply(new BigDecimal("1.5"))) <= 0) {
                statut = "⚠️ Stock bas";
                statutStyle = "-fx-text-fill: #ff9800; -fx-font-weight: bold;";
            }
        }

        // En-tête
        Label nameLabel = new Label(nomProduit);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
        nameLabel.setMaxWidth(280);

        Label statutLabel = new Label(statut);
        statutLabel.setStyle(statutStyle);

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.getChildren().addAll(nameLabel, statutLabel);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Détails
        GridPane details = new GridPane();
        details.setHgap(10);
        details.setVgap(4);

        Label qtyLabel = new Label("Quantité:");
        qtyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        String qtyValue = (s.getQuantiteActuelle() != null ? s.getQuantiteActuelle().toString() : "0") +
                (s.getUniteMesure() != null ? " " + s.getUniteMesure() : "");
        Label qtyValueLabel = new Label(qtyValue);
        qtyValueLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        Label seuilLabel = new Label("Seuil:");
        seuilLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        String seuilValue = (s.getSeuilAlerte() != null ? s.getSeuilAlerte().toString() : "0") +
                (s.getUniteMesure() != null ? " " + s.getUniteMesure() : "");
        Label seuilValueLabel = new Label(seuilValue);
        seuilValueLabel.setStyle("-fx-font-size: 13px;");

        Label emplacementLabel = new Label("📍 " + (s.getEmplacement() != null && !s.getEmplacement().isEmpty() ?
                s.getEmplacement() : "Emplacement non spécifié"));
        emplacementLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        details.add(qtyLabel, 0, 0);
        details.add(qtyValueLabel, 1, 0);
        details.add(seuilLabel, 0, 1);
        details.add(seuilValueLabel, 1, 1);
        details.add(emplacementLabel, 0, 2, 2, 1);

        // Boutons
        HBox actions = new HBox(8);
        actions.setAlignment(javafx.geometry.Pos.CENTER);

        Button edit = new Button("✏️ Modifier");
        edit.setPrefWidth(130);
        edit.setPrefHeight(30);
        edit.getStyleClass().add("small-primary");
        edit.setOnAction(e -> editStock(s));

        Button delete = new Button("🗑️ Supprimer");
        delete.setPrefWidth(130);
        delete.setPrefHeight(30);
        delete.getStyleClass().add("small-danger");
        delete.setOnAction(e -> deleteStock(s));

        actions.getChildren().addAll(edit, delete);

        card.getChildren().addAll(header, details, actions);
        return card;
    }

    private String getNomProduitCourt(int produitId) {
        String nom = getNomProduit(produitId);
        if (nom.length() > 25) {
            return nom.substring(0, 22) + "...";
        }
        return nom;
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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le produit ?");
        alert.setContentText("Voulez-vous vraiment supprimer " + p.getNom() + " ?");

        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                serviceProduit.supprimer(p.getId());
                rafraichir();
                showAlert("Succès", "Produit supprimé avec succès");
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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le stock ?");
        alert.setContentText("Voulez-vous vraiment supprimer ce stock ?");

        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                serviceStock.supprimer(s.getId());
                rafraichir();
                showAlert("Succès", "Stock supprimé avec succès");
            } catch (SQLException e) {
                showAlert("Erreur", "Suppression échouée: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class AlerteData {
        private final String nomProduit;
        private final BigDecimal quantiteActuelle;
        private final BigDecimal seuilAlerte;
        private final String unite;
        private final String statut;

        public AlerteData(String nomProduit, BigDecimal quantiteActuelle, BigDecimal seuilAlerte, String unite, String statut) {
            this.nomProduit = nomProduit;
            this.quantiteActuelle = quantiteActuelle;
            this.seuilAlerte = seuilAlerte;
            this.unite = unite;
            this.statut = statut;
        }

        public String getNomProduit() { return nomProduit; }
        public BigDecimal getQuantiteActuelle() { return quantiteActuelle; }
        public BigDecimal getSeuilAlerte() { return seuilAlerte; }
        public String getUnite() { return unite; }
        public String getStatut() { return statut; }
    }
}