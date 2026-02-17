package controllers;

import controllers.MainLayoutController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import entity.Produit;
import entity.Stock;
import services.ServiceStockProduit;
import services.ServiceStockStock;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;

public class AjoutProduitController {

    @FXML private ComboBox<String> comboCategorie;
    @FXML private TextField txtNom;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtFournisseur;
    @FXML private TextField txtPrix;
    @FXML private Button btnChoisirPhoto;
    @FXML private Label lblPhotoStatus;
    @FXML private ImageView imgPreview;
    @FXML private HBox imagePreviewContainer;  // Nouveau conteneur pour l'image
    @FXML private TextField txtQuantite;
    @FXML private TextField txtSeuil;
    @FXML private ComboBox<String> comboUnite;
    @FXML private DatePicker dateReception;
    @FXML private DatePicker dateExpiration;
    @FXML private TextField txtEmplacement;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnAnnuler;

    private ServiceStockProduit serviceProduit = new ServiceStockProduit();
    private ServiceStockStock serviceStock = new ServiceStockStock();
    private Produit produitToEdit = null;
    private String selectedPhotoUrl = null;
    private boolean isModification = false;

    @FXML
    public void initialize() {
        comboCategorie.getItems().addAll("Fertilisants", "Pesticides", "Semences", "Outils", "Autres");
        comboUnite.getItems().addAll("kg", "L", "sac", "pièce", "unité");

        Produit produit = MainLayoutController.getProduitToEdit();
        if (produit != null) {
            isModification = true;
            setProduitToEdit(produit);
        } else {
            isModification = false;
            btnEnregistrer.setText("✓ Enregistrer Produit");
        }
    }

    public void setProduitToEdit(Produit produit) {
        this.produitToEdit = produit;
        btnEnregistrer.setText("✓ Enregistrer Modifications");

        comboCategorie.setValue(produit.getCategorie());
        txtNom.setText(produit.getNom());
        txtDescription.setText(produit.getDescription());
        // Gérer le fournisseur si le champ existe dans l'entité
        // txtFournisseur.setText(produit.getFournisseur() != null ? produit.getFournisseur() : "");
        txtPrix.setText(produit.getPrixUnitaire() != null ? produit.getPrixUnitaire().toString() : "");

        selectedPhotoUrl = produit.getPhotoUrl();
        if (selectedPhotoUrl != null && !selectedPhotoUrl.trim().isEmpty()) {
            lblPhotoStatus.setText("Photo actuelle");
            lblPhotoStatus.setStyle("-fx-text-fill: #5a9814; -fx-font-style: normal; -fx-font-weight: bold;");
            try {
                imgPreview.setImage(new Image(selectedPhotoUrl));
                imgPreview.setVisible(true);
                if (imagePreviewContainer != null) {
                    imagePreviewContainer.setVisible(true);
                }
            } catch (Exception e) {
                System.err.println("Erreur chargement image: " + e.getMessage());
                imgPreview.setImage(null);
                imgPreview.setVisible(false);
                if (imagePreviewContainer != null) {
                    imagePreviewContainer.setVisible(false);
                }
            }
        } else {
            lblPhotoStatus.setText("Aucune photo");
            lblPhotoStatus.setStyle("-fx-text-fill: #6b7b6e; -fx-font-style: italic;");
            imgPreview.setVisible(false);
            if (imagePreviewContainer != null) {
                imagePreviewContainer.setVisible(false);
            }
        }

        try {
            Stock stock = serviceStock.recupererParProduitId(produit.getId());
            if (stock != null) {
                txtQuantite.setText(stock.getQuantiteActuelle() != null ? stock.getQuantiteActuelle().toString() : "");
                txtSeuil.setText(stock.getSeuilAlerte() != null ? stock.getSeuilAlerte().toString() : "");
                comboUnite.setValue(stock.getUniteMesure());
                dateReception.setValue(stock.getDateReception() != null ? stock.getDateReception().toLocalDate() : null);
                dateExpiration.setValue(stock.getDateExpiration() != null ? stock.getDateExpiration().toLocalDate() : null);
                txtEmplacement.setText(stock.getEmplacement());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Avertissement", "Impossible de charger les informations de stock.");
        }
    }

    @FXML
    private void choisirPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            lblPhotoStatus.setText("Photo sélectionnée : " + selectedFile.getName());
            lblPhotoStatus.setStyle("-fx-text-fill: #5a9814; -fx-font-style: normal; -fx-font-weight: bold;");

            try {
                Image image = new Image(selectedFile.toURI().toString());
                imgPreview.setImage(image);
                imgPreview.setVisible(true);
                if (imagePreviewContainer != null) {
                    imagePreviewContainer.setVisible(true);
                }
                selectedPhotoUrl = selectedFile.toURI().toString();
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de charger l'image sélectionnée.");
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void enregistrer() {
        // Validation commune
        if (txtNom.getText().trim().isEmpty()) {
            showAlert("Erreur de validation", "Le nom du produit est obligatoire.");
            return;
        }
        if (txtNom.getText().trim().length() < 3) {
            showAlert("Erreur de validation", "Le nom du produit doit contenir au moins 3 caractères.");
            return;
        }
        if (comboCategorie.getValue() == null || comboCategorie.getValue().trim().isEmpty()) {
            showAlert("Erreur de validation", "La catégorie est obligatoire.");
            return;
        }
        if (txtPrix.getText().trim().isEmpty()) {
            showAlert("Erreur de validation", "Le prix est obligatoire.");
            return;
        }

        String description = txtDescription.getText().trim();
        if (!description.isEmpty() && description.length() > 500) {
            showAlert("Erreur de validation", "La description ne doit pas dépasser 500 caractères.");
            return;
        }

        BigDecimal prix = null;
        BigDecimal quantite = null;
        BigDecimal seuil = null;

        try {
            prix = new BigDecimal(txtPrix.getText().trim());
            if (prix.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert("Erreur de validation", "Le prix doit être positif.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur de validation", "Le prix doit être un nombre valide (ex: 125.50).");
            return;
        }

        if (!txtQuantite.getText().trim().isEmpty()) {
            try {
                quantite = new BigDecimal(txtQuantite.getText().trim());
                if (quantite.compareTo(BigDecimal.ZERO) < 0) {
                    showAlert("Erreur de validation", "La quantité ne peut pas être négative.");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Erreur de validation", "La quantité doit être un nombre valide.");
                return;
            }
        }

        if (!txtSeuil.getText().trim().isEmpty()) {
            try {
                seuil = new BigDecimal(txtSeuil.getText().trim());
                if (seuil.compareTo(BigDecimal.ZERO) < 0) {
                    showAlert("Erreur de validation", "Le seuil d'alerte ne peut pas être négatif.");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Erreur de validation", "Le seuil d'alerte doit être un nombre valide.");
                return;
            }
        }

        if (dateReception.getValue() != null && dateExpiration.getValue() != null) {
            if (dateExpiration.getValue().isBefore(dateReception.getValue())) {
                showAlert("Erreur de validation", "La date d'expiration doit être après la date de réception.");
                return;
            }
        }

        if (quantite != null && (comboUnite.getValue() == null || comboUnite.getValue().trim().isEmpty())) {
            showAlert("Erreur de validation", "L'unité de mesure est obligatoire si une quantité est saisie.");
            return;
        }

        try {
            Produit produit;
            int produitId;

            if (isModification && produitToEdit != null) {
                produit = produitToEdit;
                produit.setCategorie(comboCategorie.getValue());
                produit.setNom(txtNom.getText().trim());
                produit.setDescription(txtDescription.getText().trim());
                // Si vous avez un champ fournisseur :
                // produit.setFournisseur(txtFournisseur.getText().trim());
                produit.setPrixUnitaire(prix);
                produit.setPhotoUrl(selectedPhotoUrl != null ? selectedPhotoUrl : produit.getPhotoUrl());

                serviceProduit.modifier(produit);
                produitId = produit.getId();
                showAlert("Succès", "Produit modifié avec succès !");
            } else {
                produit = new Produit();
                produit.setAgriculteurId(3);  // TODO: Récupérer l'ID de l'utilisateur connecté
                produit.setCategorie(comboCategorie.getValue());
                produit.setNom(txtNom.getText().trim());
                produit.setDescription(txtDescription.getText().trim());
                // Si vous avez un champ fournisseur :
                // produit.setFournisseur(txtFournisseur.getText().trim());
                produit.setPrixUnitaire(prix);
                produit.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                produit.setPhotoUrl(selectedPhotoUrl);

                produitId = serviceProduit.ajouter(produit);
                if (produitId == -1) {
                    showAlert("Erreur", "Échec de l'ajout du produit.");
                    return;
                }
                produit.setId(produitId);
                showAlert("Succès", "Produit ajouté avec succès !");
            }

            // Gestion du stock
            try {
                Stock stockExistant = serviceStock.recupererParProduitId(produitId);

                Stock stock;
                if (stockExistant != null) {
                    stock = stockExistant;
                } else {
                    stock = new Stock();
                    stock.setProduitId(produitId);
                }

                stock.setQuantiteActuelle(quantite);
                stock.setSeuilAlerte(seuil);
                stock.setUniteMesure(comboUnite.getValue());
                stock.setDateReception(dateReception.getValue() != null ? Date.valueOf(dateReception.getValue()) : null);
                stock.setDateExpiration(dateExpiration.getValue() != null ? Date.valueOf(dateExpiration.getValue()) : null);
                stock.setEmplacement(txtEmplacement.getText().trim());

                if (stockExistant != null) {
                    serviceStock.modifier(stock);
                } else {
                    serviceStock.ajouter(stock);
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Erreur Stock", "Erreur lors de l'enregistrement du stock : " + e.getMessage());
                return;
            }

            // Réinitialiser
            MainLayoutController.setProduitToEdit(null);
            this.produitToEdit = null;
            this.isModification = false;

            annuler();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Échec de l'enregistrement : " + e.getMessage());
        }
    }

    @FXML
    private void annuler() {
        // Réinitialiser
        MainLayoutController.setProduitToEdit(null);

        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToProductList();
        } else {
            showAlert("Erreur", "Impossible de revenir à la liste.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
