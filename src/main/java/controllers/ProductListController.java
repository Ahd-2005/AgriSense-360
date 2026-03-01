package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import entity.Produit;
import entity.Stock;
import services.RecommandationService;
import services.ServiceStockProduit;
import services.ServiceStockStock;
import utils.BarcodeGenerator;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ProductListController {

    @FXML private GridPane gridProduits;
    @FXML private Button sidebarToggle;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> comboTri;
    @FXML private Label lblResultats;

    private ServiceStockProduit serviceProduit = new ServiceStockProduit();
    private ServiceStockStock   serviceStock   = new ServiceStockStock();

    private ObservableList<Produit>  allProduits = FXCollections.observableArrayList();
    private Map<Integer, Stock>      stocksMap   = new HashMap<>();

    // ── Initialisation ────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        gridProduits.getColumnConstraints().clear();
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        gridProduits.getColumnConstraints().addAll(col1, col2);

        comboTri.getItems().addAll(
                "Nom (A → Z)",
                "Nom (Z → A)",
                "Prix (croissant)",
                "Prix (décroissant)",
                "Date d'ajout (récent)"
        );

        txtRecherche.textProperty().addListener((obs, o, n) -> applyFilterAndSort());
        comboTri.valueProperty().addListener((obs, o, n) -> applyFilterAndSort());

        refreshProductList();
    }

    // ── Chargement des données ────────────────────────────────────────────────

    private void refreshProductList() {
        gridProduits.getChildren().clear();
        try {
            List<Produit> produits = serviceProduit.afficher();
            allProduits.setAll(produits);

            // Charger les stocks pour le moteur de recommandation
            stocksMap.clear();
            for (Stock s : serviceStock.afficher()) {
                stocksMap.put(s.getProduitId(), s);
            }

            applyFilterAndSort();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les produits : " + e.getMessage());
            Label errorLabel = new Label("Erreur de chargement");
            errorLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #d32f2f;");
            gridProduits.add(errorLabel, 0, 0);
            GridPane.setColumnSpan(errorLabel, 2);
        }
    }

    // ── Filtrage et tri ───────────────────────────────────────────────────────

    private void applyFilterAndSort() {
        String search = txtRecherche.getText() != null ? txtRecherche.getText().toLowerCase().trim() : "";
        String tri    = comboTri.getValue();

        List<Produit> filtered = allProduits.stream()
                .filter(p -> {
                    if (search.isEmpty()) return true;
                    boolean nomMatch  = p.getNom() != null && p.getNom().toLowerCase().contains(search);
                    boolean prixMatch = p.getPrixUnitaire() != null && p.getPrixUnitaire().toString().contains(search);
                    return nomMatch || prixMatch;
                })
                .collect(Collectors.toList());

        if (tri != null) {
            switch (tri) {
                case "Nom (A → Z)":
                    filtered.sort(Comparator.comparing(p -> p.getNom() != null ? p.getNom().toLowerCase() : ""));
                    break;
                case "Nom (Z → A)":
                    filtered.sort((a, b) -> {
                        String na = a.getNom() != null ? a.getNom().toLowerCase() : "";
                        String nb = b.getNom() != null ? b.getNom().toLowerCase() : "";
                        return nb.compareTo(na);
                    });
                    break;
                case "Prix (croissant)":
                    filtered.sort(Comparator.comparing(p -> p.getPrixUnitaire() != null ? p.getPrixUnitaire() : java.math.BigDecimal.ZERO));
                    break;
                case "Prix (décroissant)":
                    filtered.sort((a, b) -> {
                        java.math.BigDecimal pa = a.getPrixUnitaire() != null ? a.getPrixUnitaire() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal pb = b.getPrixUnitaire() != null ? b.getPrixUnitaire() : java.math.BigDecimal.ZERO;
                        return pb.compareTo(pa);
                    });
                    break;
                case "Date d'ajout (récent)":
                    filtered.sort((a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    break;
            }
        }

        lblResultats.setText(search.isEmpty()
                ? filtered.size() + " produit(s) au total"
                : filtered.size() + " résultat(s) pour \"" + txtRecherche.getText().trim() + "\"");

        displayProduits(filtered);
    }

    private void displayProduits(List<Produit> produits) {
        gridProduits.getChildren().clear();
        gridProduits.getRowConstraints().clear();

        if (produits.isEmpty()) {
            Label vide = new Label("Aucun produit trouvé");
            vide.setStyle("-fx-font-size: 18px; -fx-text-fill: #888; -fx-padding: 20px;");
            gridProduits.add(vide, 0, 0);
            GridPane.setColumnSpan(vide, 2);
            return;
        }

        int col = 0, row = 0;
        for (Produit produit : produits) {
            gridProduits.add(createProductCard(produit), col, row);
            col++;
            if (col >= 2) { col = 0; row++; }
        }

        for (int i = 0; i <= row; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setMinHeight(600); // plus grand pour accueillir les reco
            gridProduits.getRowConstraints().add(rc);
        }
    }

    // ── Carte produit ─────────────────────────────────────────────────────────

    private VBox createProductCard(Produit produit) {
        VBox card = new VBox(16);
        card.getStyleClass().add("product-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(450);
        card.setMinWidth(400);

        // Image produit
        ImageView imageView = new ImageView();
        imageView.setFitWidth(400);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        String photoUrl = produit.getPhotoUrl();
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            try {
                Image image = new Image(
                        photoUrl.startsWith("file:") || photoUrl.startsWith("http")
                                ? photoUrl
                                : getClass().getResource(photoUrl).toExternalForm(), true);
                imageView.setImage(image);
            } catch (Exception e) {
                loadDefaultImage(imageView);
            }
        } else {
            loadDefaultImage(imageView);
        }

        // Infos de base
        Label titleLabel = new Label(produit.getNom());
        titleLabel.getStyleClass().add("card-title");

        Label categorieLabel = new Label("Catégorie : " + (produit.getCategorie() != null ? produit.getCategorie() : "N/A"));
        categorieLabel.getStyleClass().add("product-info");

        Label prixLabel = new Label("Prix : " + (produit.getPrixUnitaire() != null ? produit.getPrixUnitaire() + " DT" : "N/A"));
        prixLabel.getStyleClass().add("product-info");

        Label descriptionLabel = new Label("Description : " + (produit.getDescription() != null ? produit.getDescription() : "Aucune"));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(380);
        descriptionLabel.getStyleClass().add("product-description");

        // Barcode
        Label barcodeLabel = new Label("Code-barres :");
        ImageView barcodeView = new ImageView();
        barcodeView.setFitWidth(300);
        barcodeView.setFitHeight(100);
        barcodeView.setPreserveRatio(true);
        barcodeView.setSmooth(true);

        String barcodePath = produit.getBarcodeUrl();
        if (barcodePath != null && !barcodePath.trim().isEmpty()) {
            try {
                String projectDir = System.getProperty("user.dir").replace("\\", "/");
                String fullPath = "file:///" + projectDir + "/src/main/resources/" + barcodePath;
                Image barcodeImage = new Image(fullPath, true);
                barcodeImage.errorProperty().addListener((obs, wasError, isError) -> {
                    if (isError) {
                        try {
                            URL resourceUrl = getClass().getResource("/" + barcodePath);
                            if (resourceUrl != null) {
                                barcodeView.setImage(new Image(resourceUrl.toExternalForm()));
                                barcodeView.setVisible(true);
                                barcodeLabel.setText("Code-barres généré");
                            } else {
                                barcodeLabel.setText("Code-barres : Fichier introuvable");
                                barcodeView.setVisible(false);
                            }
                        } catch (Exception ex) {
                            barcodeView.setVisible(false);
                        }
                    }
                });
                barcodeView.setImage(barcodeImage);
                barcodeView.setVisible(true);
                barcodeLabel.setText("Code-barres généré");
            } catch (Exception e) {
                barcodeLabel.setText("Code-barres : Erreur chargement");
                barcodeView.setVisible(false);
            }
        } else {
            barcodeLabel.setText("Code-barres : Non généré");
            barcodeView.setVisible(false);
        }

        // ── Section recommandations ───────────────────────────────────────────
        VBox recBox = creerSectionRecommandations(produit);

        // Boutons
        Button editBtn = new Button("✏️ Modifier");
        editBtn.getStyleClass().add("primary");
        editBtn.setOnAction(e -> modifierProduit(produit));

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.getStyleClass().add("ghost");
        deleteBtn.setOnAction(e -> supprimerProduit(produit));

        HBox buttons = new HBox(20, editBtn, deleteBtn);
        buttons.setAlignment(Pos.CENTER);

        card.getChildren().addAll(
                imageView, titleLabel, categorieLabel, prixLabel, descriptionLabel,
                barcodeLabel, barcodeView, recBox, buttons
        );

        return card;
    }

    // ── Section recommandations ───────────────────────────────────────────────

    private VBox creerSectionRecommandations(Produit produit) {
        VBox section = new VBox(6);
        section.setMaxWidth(420);
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

        chargerRecommandations(produit, contenu);
        refreshBtn.setOnAction(e -> {
            contenu.getChildren().clear();
            chargerRecommandations(produit, contenu);
        });

        return section;
    }

    private void chargerRecommandations(Produit produit, VBox contenu) {
        if (allProduits.size() <= 1) {
            contenu.getChildren().add(infoLabel("Pas assez de produits pour recommander."));
            return;
        }

        Stock stockDuProduit = stocksMap.get(produit.getId());

        List<RecommandationService.Recommandation> recs = RecommandationService.getInstance()
                .recommander(produit, stockDuProduit, new ArrayList<>(allProduits), stocksMap, 3);

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
                        "-fx-padding: 6px 10px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);"
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
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 4px; -fx-padding: 2px 6px; -fx-alignment: center;"
        );

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);

        String prixStr = rec.produit.getPrixUnitaire() != null ? rec.produit.getPrixUnitaire() + " TND" : "N/A";
        String cat     = rec.produit.getCategorie() != null ? rec.produit.getCategorie() : "—";

        Label nomRec = new Label(rec.produit.getNom() + "  ·  " + cat + "  ·  " + prixStr);
        nomRec.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #22301b;");
        nomRec.setWrapText(true);
        nomRec.setMaxWidth(240);

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

    private void loadDefaultImage(ImageView imageView) {
        try {
            String defaultUrl = getClass().getResource("/images/default_product.png").toExternalForm();
            imageView.setImage(new Image(defaultUrl, 320, 120, true, true));
        } catch (Exception e) {
            imageView.setImage(null);
        }
    }

    private void modifierProduit(Produit produit) {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.setProduitToEdit(produit);
            MainLayoutController.getInstance().navigateToEditProduct();
        } else {
            showAlert("Erreur", "Impossible de naviguer vers la modification.");
        }
    }

    private void supprimerProduit(Produit produit) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le produit : " + produit.getNom());
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce produit ?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    serviceProduit.supprimer(produit.getId());
                    showAlert("Succès", "Produit supprimé avec succès.");
                    refreshProductList();
                } catch (SQLException e) {
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

    @FXML private void reinitialiserFiltres() { txtRecherche.clear(); comboTri.setValue(null); }
    @FXML private void toggleSidebar() {}

    @FXML private void goToHome() {
        if (MainLayoutController.getInstance() != null) MainLayoutController.getInstance().navigateToHome();
    }

    @FXML private void goToStockList() {
        if (MainLayoutController.getInstance() != null) MainLayoutController.getInstance().navigateToStockList();
    }

    @FXML private void ajouterProduit() {
        if (MainLayoutController.getInstance() != null) MainLayoutController.getInstance().navigateToAddProduct();
    }

    @FXML private void goToCommodityPrice() {
        if (MainLayoutController.getInstance() != null) MainLayoutController.getInstance().navigateToCommodityPrice();
    }

    @FXML private void genererBarcodesPourTous() {
        try {
            List<Produit> produits = serviceProduit.afficher();
            int count = 0;
            for (Produit p : produits) {
                if (p.getBarcodeUrl() == null || p.getBarcodeUrl().isEmpty()) {
                    String code = "PROD-" + p.getId();
                    String barcodeUrl = BarcodeGenerator.generateAndSave(code, 400, 150);
                    if (barcodeUrl != null) {
                        p.setBarcodeUrl(barcodeUrl);
                        serviceProduit.updateBarcode(p.getId(), barcodeUrl);
                        count++;
                    }
                }
            }
            showAlert("Succès", count + " codes-barres générés !");
            refreshProductList();
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }
}
