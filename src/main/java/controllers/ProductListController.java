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
import services.ServiceStockProduit;
import utils.BarcodeGenerator;

import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class ProductListController {

    @FXML
    private GridPane gridProduits;

    @FXML
    private Button sidebarToggle;

    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> comboTri;
    @FXML private Label lblResultats;

    private ServiceStockProduit ServiceProduit = new ServiceStockProduit();
    private ObservableList<Produit> allProduits = FXCollections.observableArrayList();




    @FXML
    public void initialize() {
        gridProduits.getColumnConstraints().clear();
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        gridProduits.getColumnConstraints().addAll(col1, col2);
// Initialiser le ComboBox de tri
        comboTri.getItems().addAll(
                "Nom (A → Z)",
                "Nom (Z → A)",
                "Prix (croissant)",
                "Prix (décroissant)",
                "Date d'ajout (récent)"
        );

        // Écouter les changements en temps réel
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> applyFilterAndSort());
        comboTri.valueProperty().addListener((obs, oldVal, newVal) -> applyFilterAndSort());

        refreshProductList();
    }
    private void applyFilterAndSort() {
        String search = txtRecherche.getText() != null ? txtRecherche.getText().toLowerCase().trim() : "";
        String tri = comboTri.getValue();

        // Filtrage par nom ou prix
        List<Produit> filtered = allProduits.stream()
                .filter(p -> {
                    if (search.isEmpty()) return true;
                    boolean nomMatch = p.getNom() != null && p.getNom().toLowerCase().contains(search);
                    boolean prixMatch = p.getPrixUnitaire() != null && p.getPrixUnitaire().toString().contains(search);
                    return nomMatch || prixMatch;
                })
                .collect(Collectors.toList());

        // Tri
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
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    break;
            }
        }

        // Mise à jour du label résultats
        if (!search.isEmpty()) {
            lblResultats.setText(filtered.size() + " résultat(s) pour \"" + txtRecherche.getText().trim() + "\"");
        } else {
            lblResultats.setText(filtered.size() + " produit(s) au total");
        }

        displayProduits(filtered);
    }

    private void displayProduits(List<Produit> produits) {
        gridProduits.getChildren().clear();
        gridProduits.getRowConstraints().clear();

        if (produits.isEmpty()) {
            Label videLabel = new Label("Aucun produit trouvé");
            videLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #888; -fx-padding: 20px;");
            gridProduits.add(videLabel, 0, 0);
            GridPane.setColumnSpan(videLabel, 2);
            return;
        }

        int col = 0;
        int row = 0;

        for (Produit produit : produits) {
            VBox card = createProductCard(produit);
            gridProduits.add(card, col, row);
            col++;
            if (col >= 2) {
                col = 0;
                row++;
            }
        }

        for (int i = 0; i < row + 1; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setMinHeight(450);
            gridProduits.getRowConstraints().add(rc);
        }
    }


    private void refreshProductList() {
        gridProduits.getChildren().clear();

        try {
            // Récupère tous les produits
            List<Produit> produits = ServiceProduit.afficher();  // ou getAllProduits()

            // Met à jour la liste complète pour le filtre/tri
            allProduits.setAll(produits);

            // Applique filtre + tri immédiatement
            applyFilterAndSort();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les produits : " + e.getMessage());

            Label errorLabel = new Label("Erreur de chargement");
            errorLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #d32f2f;");
            gridProduits.add(errorLabel, 0, 0);
            GridPane.setColumnSpan(errorLabel, 2);
        }
    }

    private VBox createProductCard(Produit produit) {
        VBox card = new VBox(20);
        card.getStyleClass().add("product-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(450);
        card.setMinWidth(400);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(400);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        String photoUrl = produit.getPhotoUrl();
        System.out.println("URL de l'image pour " + produit.getNom() + " : '" + photoUrl + "'");

        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            try {
                Image image = new Image(photoUrl.startsWith("file:") || photoUrl.startsWith("http") ? photoUrl : getClass().getResource(photoUrl).toExternalForm(), true);
                imageView.setImage(image);
                System.out.println("Image chargée avec succès pour " + produit.getNom());
            } catch (Exception e) {
                System.out.println("Erreur chargement image : " + e.getMessage());
                loadDefaultImage(imageView);
            }
        } else {
            loadDefaultImage(imageView);
        }

        // Infos
        Label titleLabel = new Label(produit.getNom());
        titleLabel.getStyleClass().add("card-title");

        Label categorieLabel = new Label("Catégorie : " + (produit.getCategorie() != null ? produit.getCategorie() : "N/A"));
        categorieLabel.getStyleClass().add("product-info");

        Label prixLabel = new Label("Prix : " + (produit.getPrixUnitaire() != null ? produit.getPrixUnitaire() + " DT" : "N/A"));
        prixLabel.getStyleClass().add("product-info");

        Label descriptionLabel = new Label("Description : " + (produit.getDescription() != null ? produit.getDescription() : "Aucune"));
        descriptionLabel.setWrapText(true);  // ← Texte long passe à la ligne
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
        System.out.println("=== BARCODE DEBUG ===");
        System.out.println("Produit: " + produit.getNom());
        System.out.println("barcodePath: '" + barcodePath + "'");
        String projectDir = System.getProperty("user.dir").replace("\\", "/");
        //String fullPath = projectDir + "/src/main/resources/" + barcodePath;
        String fullPath = "file:" + projectDir + "/src/main/resources/" + barcodePath;
        System.out.println("Full path: " + fullPath);
        System.out.println("File exists: " + new java.io.File(fullPath).exists());

        if (barcodePath != null && !barcodePath.trim().isEmpty()) {
            try {
                // Priorité : chemin absolu depuis le dossier du projet (le fichier est là)
                //String projectDir = System.getProperty("user.dir").replace("\\", "/");
                //String fullPath = "file:" + projectDir + "/src/main/resources/" + barcodePath;

                Image barcodeImage = new Image(fullPath, true);

                // Vérifier si l'image s'est chargée correctement
                barcodeImage.errorProperty().addListener((obs, wasError, isError) -> {
                    if (isError) {
                        // Fallback : essayer via resources classpath
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
                            barcodeLabel.setText("Code-barres : Erreur chargement");
                            barcodeView.setVisible(false);
                        }
                    }
                });

                barcodeView.setImage(barcodeImage);
                barcodeView.setVisible(true);
                barcodeLabel.setText("Code-barres généré");
                System.out.println("Barcode chargé : " + fullPath);

            } catch (Exception e) {
                System.err.println("Erreur chargement barcode : " + e.getMessage());
                barcodeLabel.setText("Code-barres : Erreur chargement");
                barcodeView.setVisible(false);
            }
        } else {
            barcodeLabel.setText("Code-barres : Non généré");
            barcodeView.setVisible(false);
        }
        // Boutons
        Button editBtn = new Button("✏️ Modifier");
        editBtn.getStyleClass().add("primary");
        editBtn.setOnAction(e -> modifierProduit(produit));

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.getStyleClass().add("ghost");
        deleteBtn.setOnAction(e -> supprimerProduit(produit));

        HBox buttons = new HBox(20, editBtn, deleteBtn);
        buttons.setAlignment(Pos.CENTER);

        card.getChildren().addAll(imageView, titleLabel, categorieLabel, prixLabel, descriptionLabel, barcodeLabel, barcodeView, buttons);


        return card;
    }
    private void loadDefaultImage(ImageView imageView) {
        try {
            String defaultUrl = getClass().getResource("/images/default_product.png").toExternalForm();
            Image defaultImage = new Image(defaultUrl, 320, 120, true, true);
            imageView.setImage(defaultImage);
            System.out.println("Image par défaut chargée");
        } catch (Exception e) {
            System.out.println("Erreur de chargement de l'image par défaut: " + e.getMessage());
            imageView.setImage(null);
        }
    }


    private void modifierProduit(Produit produit) {
        if (controllers.MainLayoutController.getInstance() != null) {
            controllers.MainLayoutController.setProduitToEdit(produit);  // Passer le produit
            controllers.MainLayoutController.getInstance().navigateToEditProduct();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers la modification.");
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
                    ServiceProduit.supprimer(produit.getId());
                    showAlert(Alert.AlertType.ERROR, "Succès", "Produit supprimé avec succès.");
                    refreshProductList();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void reinitialiserFiltres() {
        txtRecherche.clear();
        comboTri.setValue(null);
    }

    @FXML
    private void toggleSidebar() {
    }

    private void showAlert(Alert.AlertType error, String title, String message) {
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
    private void goToStockList() {
        if (controllers.MainLayoutController.getInstance() != null) {
            controllers.MainLayoutController.getInstance().navigateToStockList();
        }
    }

    @FXML
    private void ajouterProduit() {
        if (controllers.MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToAddProduct();
        }
    }
    @FXML
    private void genererBarcodesPourTous() {
        try {
            List<Produit> produits = ServiceProduit.afficher();
            int count = 0;
            for (Produit p : produits) {
                if (p.getBarcodeUrl() == null || p.getBarcodeUrl().isEmpty()) {
                    String code = "PROD-" + p.getId();
                    String barcodeUrl = BarcodeGenerator.generateAndSave(code, 400, 150);
                    if (barcodeUrl != null) {
                        p.setBarcodeUrl(barcodeUrl);
                        ServiceProduit.updateBarcode(p.getId(), barcodeUrl);
                        count++;
                    }
                }
            }
            showAlert(Alert.AlertType.ERROR, "Succès", count + " codes-barres générés !");
            refreshProductList();  // Recharge la liste
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }
}