package controllers;

import controllers.MainLayoutController;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import entity.Produit;
import services.ServiceStockProduit;

import java.sql.SQLException;
import java.util.List;

public class ProductListController {

    @FXML
    private GridPane gridProduits;

    @FXML
    private Button sidebarToggle;

    private ServiceStockProduit produitService = new ServiceStockProduit();

    @FXML
    public void initialize() {
        gridProduits.getColumnConstraints().clear();
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        gridProduits.getColumnConstraints().addAll(col1, col2);

        refreshProductList();
    }

    private void refreshProductList() {
        gridProduits.getChildren().clear();

        gridProduits.getRowConstraints().clear();

        List<Produit> produits = produitService.getAllProduits();

        int col = 0;
        int row = 0;
        for (Produit produit : produits) {
            VBox card = createProductCard(produit);
            gridProduits.add(card, col, row);

            col++;
            if (col >= 2) { // 2 colonnes par ligne
                col = 0;
                row++;
            }
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

        // Boutons
        Button editBtn = new Button("✏️ Modifier");
        editBtn.getStyleClass().add("primary");
        editBtn.setOnAction(e -> modifierProduit(produit));

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.getStyleClass().add("ghost");
        deleteBtn.setOnAction(e -> supprimerProduit(produit));

        HBox buttons = new HBox(20, editBtn, deleteBtn);
        buttons.setAlignment(Pos.CENTER);

        card.getChildren().addAll(imageView, titleLabel, categorieLabel, prixLabel, descriptionLabel, buttons);

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
                    produitService.supprimer(produit.getId());
                    showAlert("Succès", "Produit supprimé avec succès.");
                    refreshProductList();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Erreur lors de la suppression : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void toggleSidebar() {
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
}