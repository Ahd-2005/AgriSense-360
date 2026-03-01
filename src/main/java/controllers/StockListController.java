package controllers;

import controllers.MainLayoutController;
import entity.Produit;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import entity.Stock;
import services.ServiceStockProduit;
import services.ServiceStockStock;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class StockListController {

    @FXML private FlowPane flowStocks;
    @FXML private Button btnAjouterStock;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> comboTri;
    @FXML private Label lblResultats;

    private ServiceStockStock serviceStock = new ServiceStockStock();
    private ServiceStockProduit serviceProduit = new ServiceStockProduit();
    private List<Stock> allStocks = new ArrayList<>();
    private Set<Integer> stocksEnAlerte = Set.of();


    @FXML
    public void initialize() {
        // Vérifier que les champs FXML sont injectés
        if (flowStocks == null || btnAjouterStock == null) {
            System.err.println("Erreur : Un ou plusieurs champs FXML ne sont pas injectés. Vérifiez les fx:id dans le FXML.");
            return;
        }
        // Initialiser le ComboBox de tri
        comboTri.getItems().addAll(
                "Nom produit (A → Z)",
                "Nom produit (Z → A)",
                "Date réception (récent)",
                "Quantité"
        );

        // Écouter les changements en temps réel
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> applyFilterAndSort());
        comboTri.valueProperty().addListener((obs, oldVal, newVal) -> applyFilterAndSort());
        // Charger les stocks
        refreshStockList();
    }

    private void refreshStockList() {
        try {
            allStocks = serviceStock.afficher();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement des stocks : " + e.getMessage());
            allStocks = new ArrayList<>();
        }
        applyFilterAndSort();
    }

    private void applyFilterAndSort() {
        String search = txtRecherche.getText() != null ? txtRecherche.getText().toLowerCase().trim() : "";
        String tri = comboTri.getValue();

        // Filtrage : on charge le nom du produit pour chaque stock
        List<Stock> filtered = allStocks.stream()
                .filter(s -> {
                    if (search.isEmpty()) return true;
                    // Recherche par nom de produit
                    try {
                        Produit p = serviceProduit.recupererParId(s.getProduitId());
                        boolean nomMatch = p != null && p.getNom() != null && p.getNom().toLowerCase().contains(search);
                        return nomMatch;
                    } catch (SQLException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        // Tri
        if (tri != null) {
            switch (tri) {
                case "Nom produit (A → Z)":
                    filtered.sort((a, b) -> {
                        String na = getNomProduit(a.getProduitId()).toLowerCase();
                        String nb = getNomProduit(b.getProduitId()).toLowerCase();
                        return na.compareTo(nb);
                    });
                    break;
                case "Nom produit (Z → A)":
                    filtered.sort((a, b) -> {
                        String na = getNomProduit(a.getProduitId()).toLowerCase();
                        String nb = getNomProduit(b.getProduitId()).toLowerCase();
                        return nb.compareTo(na);
                    });
                    break;
                case "Quantité":
                    filtered.sort(Comparator.comparing(s -> s.getQuantiteActuelle()));
                    break;
                case "Date réception (récent)":
                    filtered.sort((a, b) -> {
                        if (a.getDateReception() == null && b.getDateReception() == null) return 0;
                        if (a.getDateReception() == null) return 1;
                        if (b.getDateReception() == null) return -1;
                        return b.getDateReception().compareTo(a.getDateReception());
                    });
                    break;
            }
        }

        // Mise à jour label résultats
        if (!search.isEmpty()) {
            lblResultats.setText(filtered.size() + " résultat(s) pour \"" + txtRecherche.getText().trim() + "\"");
        } else {
            lblResultats.setText(filtered.size() + " stock(s) au total");
        }

        displayStocks(filtered);
    }
    private String getNomProduit(int produitId) {
        try {
            Produit p = serviceProduit.recupererParId(produitId);
            return p != null && p.getNom() != null ? p.getNom() : "";
        } catch (SQLException e) {
            return "";
        }
    }


    private void displayStocks(List<Stock> stocks) {
        flowStocks.getChildren().clear();

        if (stocks.isEmpty()) {
            Label videLabel = new Label("Aucun stock trouvé");
            videLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #888; -fx-padding: 20px;");
            flowStocks.getChildren().add(videLabel);
            return;
        }

        for (Stock stock : stocks) {
            VBox card = createStockCard(stock);
            flowStocks.getChildren().add(card);
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
        String nomProduit = getNomProduit(stock.getProduitId());
        Label nomProduitLabel = new Label("Produit : " + (nomProduit.isEmpty() ? "ID " + stock.getProduitId() : nomProduit));

        nomProduitLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #22301b;");

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
        card.getChildren().addAll(nomProduitLabel, idLabel, produitIdLabel, quantiteLabel, seuilLabel, uniteLabel, dateReceptionLabel, dateExpirationLabel, emplacementLabel, buttonsBox);

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
    private void reinitialiserFiltres() {
        txtRecherche.clear();
        comboTri.setValue(null);
    }
    @FXML
    private void goToHome() {
        if (controllers.MainLayoutController.getInstance() != null) {
            controllers.MainLayoutController.getInstance().navigateToStock();
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