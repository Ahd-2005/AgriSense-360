package controllers;

import controllers.MainLayoutController;
import entity.Produit;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import entity.Stock;
import services.RecommandationService;
import services.ServiceStockProduit;
import services.ServiceStockStock;
import services.StockAlertService;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class StockListController {

    @FXML private FlowPane flowStocks;
    @FXML private Button btnAjouterStock;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> comboTri;
    @FXML private Label lblResultats;

    private ServiceStockStock serviceStock   = new ServiceStockStock();
    private ServiceStockProduit serviceProduit = new ServiceStockProduit();

    private List<Stock>   allStocks   = new ArrayList<>();
    private List<Produit> allProduits = new ArrayList<>();
    // Map produitId → Stock pour le moteur de recommandation
    private Map<Integer, Stock> stocksMap = new HashMap<>();
    // IDs des stocks en alerte (cartes rouges)
    private Set<Integer> stocksEnAlerte = new HashSet<>();

    // ── Initialisation ────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        if (flowStocks == null || btnAjouterStock == null) {
            System.err.println("Erreur : champs FXML non injectés.");
            return;
        }

        comboTri.getItems().addAll(
                "Nom produit (A → Z)",
                "Nom produit (Z → A)",
                "Date réception (récent)",
                "Quantité",
                "⚠️ Alertes en premier"
        );

        txtRecherche.textProperty().addListener((obs, o, n) -> applyFilterAndSort());
        comboTri.valueProperty().addListener((obs, o, n) -> applyFilterAndSort());

        refreshStockList();
    }

    private void refreshStockList() {
        // Stocks en alerte (pour cartes rouges)
        stocksEnAlerte = StockAlertService.getInstance()
                .getStocksEnAlerte()
                .stream()
                .map(a -> a.stock.getId())
                .collect(Collectors.toSet());

        try {
            allStocks   = serviceStock.afficher();
            allProduits = serviceProduit.afficher();

            // Construire la map produitId → Stock
            stocksMap.clear();
            for (Stock s : allStocks) {
                stocksMap.put(s.getProduitId(), s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement : " + e.getMessage());
        }

        applyFilterAndSort();
    }

    // ── Filtrage et tri ───────────────────────────────────────────────────────

    private void applyFilterAndSort() {
        String search = txtRecherche.getText() != null ? txtRecherche.getText().toLowerCase().trim() : "";
        String tri    = comboTri.getValue();

        List<Stock> filtered = allStocks.stream()
                .filter(s -> {
                    if (search.isEmpty()) return true;
                    String nom = getNomProduit(s.getProduitId()).toLowerCase();
                    return nom.contains(search);
                })
                .collect(Collectors.toList());

        if (tri != null) {
            switch (tri) {
                case "Nom produit (A → Z)":
                    filtered.sort((a, b) -> getNomProduit(a.getProduitId()).compareToIgnoreCase(getNomProduit(b.getProduitId())));
                    break;
                case "Nom produit (Z → A)":
                    filtered.sort((a, b) -> getNomProduit(b.getProduitId()).compareToIgnoreCase(getNomProduit(a.getProduitId())));
                    break;
                case "Quantité":
                    filtered.sort(Comparator.comparing(s -> s.getQuantiteActuelle() != null ? s.getQuantiteActuelle() : java.math.BigDecimal.ZERO));
                    break;
                case "Date réception (récent)":
                    filtered.sort((a, b) -> {
                        if (a.getDateReception() == null) return 1;
                        if (b.getDateReception() == null) return -1;
                        return b.getDateReception().compareTo(a.getDateReception());
                    });
                    break;
                case "⚠️ Alertes en premier":
                    filtered.sort((a, b) -> Boolean.compare(!stocksEnAlerte.contains(a.getId()), !stocksEnAlerte.contains(b.getId())));
                    break;
            }
        }

        long nbAlertes = filtered.stream().filter(s -> stocksEnAlerte.contains(s.getId())).count();
        String suffix  = nbAlertes > 0 ? "  —  ⚠️ " + nbAlertes + " en alerte" : "";

        lblResultats.setText(search.isEmpty()
                ? filtered.size() + " stock(s) au total" + suffix
                : filtered.size() + " résultat(s) pour \"" + txtRecherche.getText().trim() + "\"" + suffix);

        displayStocks(filtered);
    }

    private void displayStocks(List<Stock> stocks) {
        flowStocks.getChildren().clear();
        if (stocks.isEmpty()) {
            Label vide = new Label("Aucun stock trouvé");
            vide.setStyle("-fx-font-size: 18px; -fx-text-fill: #888; -fx-padding: 20px;");
            flowStocks.getChildren().add(vide);
            return;
        }
        for (Stock s : stocks) flowStocks.getChildren().add(createStockCard(s));
    }

    // ── Carte stock ───────────────────────────────────────────────────────────

    private VBox createStockCard(Stock stock) {
        boolean enAlerte = stocksEnAlerte.contains(stock.getId());

        // Récupérer le Produit correspondant
        Produit produit = allProduits.stream()
                .filter(p -> p.getId() == stock.getProduitId())
                .findFirst()
                .orElse(null);

        VBox card = new VBox(8);
        card.setPrefWidth(420);
        card.setMinWidth(400);

        if (enAlerte) {
            card.setStyle(
                    "-fx-padding: 15px; -fx-background-color: #fff8f8;" +
                            "-fx-background-radius: 10px; -fx-border-radius: 10px;" +
                            "-fx-border-color: #e53935; -fx-border-width: 2px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(229,57,53,0.2), 12, 0, 0, 4);"
            );
        } else {
            card.setStyle(
                    "-fx-padding: 15px; -fx-background-color: white;" +
                            "-fx-background-radius: 10px; -fx-border-radius: 10px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);"
            );
        }

        // Header : nom + badge alerte
        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        String nomProduit = produit != null && produit.getNom() != null ? produit.getNom() : "Produit #" + stock.getProduitId();
        Label nomLabel = new Label("📦 " + nomProduit);
        nomLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #22301b;");
        headerBox.getChildren().add(nomLabel);

        if (enAlerte) {
            Label badge = new Label("⚠️ STOCK BAS");
            badge.setStyle(
                    "-fx-background-color: #e53935; -fx-text-fill: white;" +
                            "-fx-font-size: 11px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 6px; -fx-padding: 3px 8px;"
            );
            headerBox.getChildren().add(badge);
        }

        Label idLabel       = new Label("Stock ID: " + stock.getId());
        Label produitIdLbl  = new Label("Produit ID: " + stock.getProduitId());
        Label quantiteLabel = new Label("Quantité : "
                + (stock.getQuantiteActuelle() != null ? stock.getQuantiteActuelle() : "N/A")
                + " " + (stock.getUniteMesure() != null ? stock.getUniteMesure() : ""));
        Label seuilLabel    = new Label("Seuil alerte : " + (stock.getSeuilAlerte() != null ? stock.getSeuilAlerte() : "N/A"));
        Label uniteLabel    = new Label("Unité : " + (stock.getUniteMesure() != null ? stock.getUniteMesure() : "N/A"));
        Label dateRecLabel  = new Label("Date Réception : " + (stock.getDateReception() != null ? stock.getDateReception() : "N/A"));
        Label dateExpLabel  = new Label("Date Expiration : " + (stock.getDateExpiration() != null ? stock.getDateExpiration() : "N/A"));
        Label empLabel      = new Label("📍 " + (stock.getEmplacement() != null ? stock.getEmplacement() : "N/A"));

        String styleBase = "-fx-font-size: 14px;";
        idLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #888;");
        produitIdLbl.setStyle(styleBase);
        quantiteLabel.setStyle(styleBase + (enAlerte ? " -fx-text-fill: #e53935; -fx-font-weight: bold;" : ""));
        seuilLabel.setStyle(styleBase);
        uniteLabel.setStyle(styleBase);
        dateRecLabel.setStyle(styleBase);
        dateExpLabel.setStyle(styleBase);
        empLabel.setStyle(styleBase);

        // Section recommandations
        VBox recBox = creerSectionRecommandations(produit, stock);

        // Boutons
        Button editButton = new Button("✏️ Modifier");
        editButton.getStyleClass().add("primary");
        editButton.setPrefWidth(150);
        editButton.setPrefHeight(40);
        editButton.setOnAction(e -> modifierStock(stock));

        Button deleteButton = new Button("🗑️ Supprimer");
        deleteButton.getStyleClass().add("ghost");
        deleteButton.setPrefWidth(150);
        deleteButton.setPrefHeight(40);
        deleteButton.setOnAction(e -> supprimerStock(stock));

        HBox buttonsBox = new HBox(10, editButton, deleteButton);
        buttonsBox.setAlignment(Pos.CENTER);

        card.getChildren().addAll(
                headerBox, idLabel, produitIdLbl, quantiteLabel, seuilLabel,
                uniteLabel, dateRecLabel, dateExpLabel, empLabel, recBox, buttonsBox
        );
        return card;
    }

    // ── Section recommandations ───────────────────────────────────────────────

    private VBox creerSectionRecommandations(Produit produit, Stock stock) {
        VBox section = new VBox(6);
        section.setStyle(
                "-fx-background-color: #f0f7e6; -fx-background-radius: 8px;" +
                        "-fx-border-color: #c8d8b0; -fx-border-radius: 8px; -fx-padding: 10px 12px;"
        );

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("🤖 Produits similaires recommandés");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #22301b;");

        Button refreshBtn = new Button("↻");
        refreshBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #5a9814;" +
                        "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4px;"
        );
        refreshBtn.setTooltip(new Tooltip("Recalculer"));

        titleRow.getChildren().addAll(title, refreshBtn);
        section.getChildren().add(titleRow);

        VBox contenu = new VBox(4);
        section.getChildren().add(contenu);

        chargerRecommandations(produit, stock, contenu);
        refreshBtn.setOnAction(e -> {
            contenu.getChildren().clear();
            chargerRecommandations(produit, stock, contenu);
        });

        return section;
    }

    private void chargerRecommandations(Produit produit, Stock stock, VBox contenu) {
        if (produit == null) {
            contenu.getChildren().add(infoLabel("Produit introuvable."));
            return;
        }
        if (allProduits.size() <= 1) {
            contenu.getChildren().add(infoLabel("Pas assez de produits pour recommander."));
            return;
        }

        List<RecommandationService.Recommandation> recs = RecommandationService.getInstance()
                .recommander(produit, stock, allProduits, stocksMap, 3);

        if (recs.isEmpty()) {
            contenu.getChildren().add(infoLabel("Aucune recommandation disponible."));
            return;
        }

        for (RecommandationService.Recommandation rec : recs) {
            contenu.getChildren().add(carteRec(rec));
        }
    }

    private HBox carteRec(RecommandationService.Recommandation rec) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-background-color: white; -fx-background-radius: 6px;" +
                        "-fx-padding: 6px 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);"
        );

        String couleur = switch (rec.niveau) {
            case "ÉLEVÉ" -> "#2e7d32";
            case "MOYEN" -> "#f57c00";
            default      -> "#757575";
        };

        Label niveauBadge = new Label(rec.niveau);
        niveauBadge.setMinWidth(52);
        niveauBadge.setStyle(
                "-fx-background-color: " + couleur + "; -fx-text-fill: white;" +
                        "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4px;" +
                        "-fx-padding: 2px 6px; -fx-alignment: center;"
        );

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);

        String prixStr = rec.produit.getPrixUnitaire() != null ? rec.produit.getPrixUnitaire() + " TND" : "N/A";
        String cat     = rec.produit.getCategorie() != null ? rec.produit.getCategorie() : "—";

        Label nomRec = new Label(rec.produit.getNom() + "  ·  " + cat + "  ·  " + prixStr);
        nomRec.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #22301b;");

        Label raison = new Label(rec.raison);
        raison.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        info.getChildren().addAll(nomRec, raison);

        Label score = new Label(String.format("%.0f%%", rec.score * 100));
        score.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + couleur + ";");

        row.getChildren().addAll(niveauBadge, info, score);
        return row;
    }

    private Label infoLabel(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #888; -fx-font-style: italic;");
        return l;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getNomProduit(int produitId) {
        return allProduits.stream()
                .filter(p -> p.getId() == produitId)
                .map(Produit::getNom)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
    }

    private void modifierStock(Stock stock) {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.setStockToEdit(stock);
            MainLayoutController.getInstance().navigateToEditStock();
        } else {
            showAlert("Erreur", "Impossible de naviguer.");
        }
    }

    private void supprimerStock(Stock stock) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le stock ID: " + stock.getId());
        alert.setContentText("Cette action est irréversible.");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    serviceStock.supprimer(stock.getId());
                    showAlert("Succès", "Stock supprimé.");
                    StockAlertService.getInstance().notifierSiNouvelleAlerte();
                    refreshStockList();
                } catch (SQLException e) {
                    showAlert("Erreur", e.getMessage());
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

    @FXML private void reinitialiserFiltres() { txtRecherche.clear(); comboTri.setValue(null); }

    @FXML private void goToHome() {
        if (MainLayoutController.getInstance() != null) MainLayoutController.getInstance().navigateToHome();
    }

    @FXML private void goToProductList() {
        if (MainLayoutController.getInstance() != null) MainLayoutController.getInstance().navigateToProductList();
    }

    @FXML private void ajouterStock() {
        if (MainLayoutController.getInstance() != null) MainLayoutController.getInstance().navigateToAddStock();
    }
}
