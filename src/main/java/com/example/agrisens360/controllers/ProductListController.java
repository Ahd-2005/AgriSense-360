package com.example.agrisens360.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.example.agrisens360.entity.Produit;
import com.example.agrisens360.services.ServiceProduit;

import java.sql.SQLException;
import java.util.List;

public class ProductListController {

    @FXML
    private GridPane gridProduits;

    @FXML
    private Button sidebarToggle;

    private ServiceProduit produitService = new ServiceProduit();

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
        VBox card = new VBox();
        card.getStyleClass().add("card");
        card.setSpacing(8.0);
        card.setPrefWidth(350);
        card.setPrefHeight(300);
        card.setMaxWidth(350);
        card.setMaxHeight(300);
        card.setStyle("-fx-padding: 15px; -fx-background-color: white; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        ImageView imageView = new ImageView();
        imageView.setFitHeight(120.0);
        imageView.setFitWidth(320.0);
        imageView.setPreserveRatio(true);

        String photoUrl = produit.getPhotoUrl();
        System.out.println("URL de l'image pour " + produit.getNom() + " : '" + photoUrl + "'");  // LOG POUR DÉBOGUER

        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            try {
                String finalUrl = photoUrl.startsWith("@") ? getClass().getResource(photoUrl.substring(1)).toExternalForm() : photoUrl;
                Image image = new Image(finalUrl, 320, 120, true, true);
                imageView.setImage(image);
                System.out.println("Image chargée avec succès pour " + produit.getNom());
            } catch (Exception e) {
                System.out.println("Erreur de chargement d'image pour " + produit.getNom() + " (URL: " + photoUrl + ") : " + e.getMessage());
                loadDefaultImage(imageView);
            }
        } else {
            System.out.println("URL de l'image null ou vide pour " + produit.getNom() + ", chargement image par défaut");
            loadDefaultImage(imageView);
        }

        Label titleLabel = new Label(produit.getNom());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #22301b;");

        Label categorieLabel = new Label("Catégorie: " + (produit.getCategorie() != null ? produit.getCategorie() : "N/A"));
        categorieLabel.setStyle("-fx-font-size: 14px;");

        Label prixLabel = new Label("Prix: " + (produit.getPrixUnitaire() != null ? produit.getPrixUnitaire() + " DT" : "N/A"));
        prixLabel.setStyle("-fx-font-size: 14px;");

        Label descriptionLabel = new Label("Description: " + (produit.getDescription() != null && produit.getDescription().length() > 50 ?
                produit.getDescription().substring(0, 47) + "..." : produit.getDescription()));
        descriptionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        Button editButton = new Button("✏️ Modifier");
        editButton.getStyleClass().add("primary");
        editButton.setPrefWidth(150.0);
        editButton.setPrefHeight(40.0);
        editButton.setOnAction(e -> modifierProduit(produit));

        Button deleteButton = new Button("🗑️ Supprimer");
        deleteButton.getStyleClass().add("ghost");
        deleteButton.setPrefWidth(150.0);
        deleteButton.setPrefHeight(40.0);
        deleteButton.setOnAction(e -> supprimerProduit(produit));

        HBox buttonsBox = new HBox(10.0, editButton, deleteButton);
        buttonsBox.setAlignment(javafx.geometry.Pos.CENTER);

        card.getChildren().addAll(imageView, titleLabel, categorieLabel, prixLabel, descriptionLabel, buttonsBox);

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
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.setProduitToEdit(produit);  // Passer le produit
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
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToHome();
        }
    }

    @FXML
    private void goToStockList() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToStockList();
        }
    }

    @FXML
    private void ajouterProduit() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToAddProduct();
        }
    }
}