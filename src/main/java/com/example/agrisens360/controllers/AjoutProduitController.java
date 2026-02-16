package com.example.agrisens360.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import com.example.agrisens360.entity.Produit;
import com.example.agrisens360.entity.Stock;
import com.example.agrisens360.services.ServiceProduit;
import com.example.agrisens360.services.ServiceStock;

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
    @FXML private TextField txtQuantite;
    @FXML private TextField txtSeuil;
    @FXML private ComboBox<String> comboUnite;
    @FXML private DatePicker dateReception;
    @FXML private DatePicker dateExpiration;
    @FXML private TextField txtEmplacement;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnAnnuler;

    private ServiceProduit serviceProduit = new ServiceProduit();
    private ServiceStock serviceStock = new ServiceStock();
    private Produit produitToEdit = null; // Pour la modification
    private String selectedPhotoUrl = null;

    @FXML
    public void initialize() {
        comboCategorie.getItems().addAll("Fertilisants", "Pesticides", "Semences", "Outils", "Autres");
        comboUnite.getItems().addAll("kg", "L", "sac", "pièce");

        Produit produit = MainLayoutController.getProduitToEdit();
        if (produit != null) {
            setProduitToEdit(produit);
        }
    }

    public void setProduitToEdit(Produit produit) {
        this.produitToEdit = produit;

        comboCategorie.setValue(produit.getCategorie());
        txtNom.setText(produit.getNom());
        txtDescription.setText(produit.getDescription());
        txtPrix.setText(produit.getPrixUnitaire() != null ? produit.getPrixUnitaire().toString() : "");
        lblPhotoStatus.setText(produit.getPhotoUrl() != null ? "Photo sélectionnée" : "Aucune photo");
        selectedPhotoUrl = produit.getPhotoUrl();
        if (selectedPhotoUrl != null) {
            try {
                imgPreview.setImage(new Image(selectedPhotoUrl));
            } catch (Exception e) {
                imgPreview.setImage(null);
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
        }

        btnEnregistrer.setText("Modifier Produit");
    }

    @FXML
    private void choisirPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            lblPhotoStatus.setText("Photo sélectionnée : " + selectedFile.getName());
            imgPreview.setImage(new Image(selectedFile.toURI().toString()));
            selectedPhotoUrl = "file:/" + selectedFile.getAbsolutePath().replace("\\", "/");
            // produit.setPhotoUrl(selectedFile.toURI().toString());
        }
    }

    @FXML
    private void enregistrer() {

        if (txtNom.getText().trim().isEmpty()) {
            showAlert("Erreur", "Le nom du produit est obligatoire.");
            return;
        }
        if (txtNom.getText().trim().length() < 3) {
            showAlert("Erreur", "Le nom du produit doit contenir au moins 3 caractères.");
            return;
        }
        if (comboCategorie.getValue() == null || comboCategorie.getValue().trim().isEmpty()) {
            showAlert("Erreur", "La catégorie est obligatoire.");
            return;
        }
        if (txtPrix.getText().trim().isEmpty()) {
            showAlert("Erreur", "Le prix est obligatoire.");
            return;
        }
        String description = txtDescription.getText().trim();
        if (!description.isEmpty() && description.length() > 500) {
            showAlert("Erreur", "La description ne doit pas dépasser 500 caractères.");
            return;
        }

        BigDecimal prix = null;
        BigDecimal quantite = null;
        BigDecimal seuil = null;
        try {
            prix = new BigDecimal(txtPrix.getText().trim());
            if (prix.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert("Erreur", "Le prix doit être positif.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le prix doit être un nombre valide.");
            return;
        }
        if (!txtQuantite.getText().trim().isEmpty()) {
            try {
                quantite = new BigDecimal(txtQuantite.getText().trim());
                if (quantite.compareTo(BigDecimal.ZERO) < 0) {
                    showAlert("Erreur", "La quantité ne peut pas être négative.");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Erreur", "La quantité doit être un nombre valide.");
                return;
            }
        }
        if (!txtSeuil.getText().trim().isEmpty()) {
            try {
                seuil = new BigDecimal(txtSeuil.getText().trim());
                if (seuil.compareTo(BigDecimal.ZERO) < 0) {
                    showAlert("Erreur", "Le seuil d'alerte ne peut pas être négatif.");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Erreur", "Le seuil d'alerte doit être un nombre valide.");
                return;
            }
        }

        if (dateReception.getValue() != null && dateExpiration.getValue() != null) {
            if (dateExpiration.getValue().isBefore(dateReception.getValue())) {
                showAlert("Erreur", "La date d'expiration doit être après la date de réception.");
                return;
            }
        }

        if (quantite != null && (comboUnite.getValue() == null || comboUnite.getValue().trim().isEmpty())) {
            showAlert("Erreur", "L'unité de mesure est obligatoire si une quantité est saisie.");
            return;
        }

        String photoUrl = null;
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            try {
                new URL(photoUrl);
            } catch (Exception e) {
                showAlert("Erreur", "L'URL de la photo n'est pas valide.");
                return;
            }
        }

        try {
            Produit produit;
            int produitId;

            if (produitToEdit == null) {
                produit = new Produit();
                produit.setAgriculteurId(3); // à ajuster
                produit.setCategorie(comboCategorie.getValue());
                produit.setNom(txtNom.getText());
                produit.setDescription(txtDescription.getText());
                produit.setPrixUnitaire(prix);
                produit.setCreatedAt(new Timestamp(System.currentTimeMillis()));  // Pour éviter l'erreur de paramètre
                // produit.setPhotoUrl(photoUrl);
                produit.setPhotoUrl(selectedPhotoUrl);

                produitId = serviceProduit.ajouter(produit);
                if (produitId == -1) {
                    showAlert("Erreur", "Échec de l'ajout du produit.");
                    return;
                }
                produit.setId(produitId);
                showAlert("Succès", "Produit ajouté avec succès.");
            } else {
                produit = produitToEdit;
                produit.setCategorie(comboCategorie.getValue());
                produit.setNom(txtNom.getText());
                produit.setDescription(txtDescription.getText());
                produit.setPrixUnitaire(prix);
                produit.setPhotoUrl(selectedPhotoUrl != null ? selectedPhotoUrl : produit.getPhotoUrl());

                serviceProduit.modifier(produit);
                produitId = produit.getId();
                showAlert("Succès", "Produit modifié avec succès.");
            }

            try {
                Stock stockExistant = serviceStock.recupererParProduitId(produitId);

                Stock stock;
                if (stockExistant != null) {
                    stock = stockExistant;
                    System.out.println("Stock existant trouvé avec ID: " + stock.getId());
                } else {
                    stock = new Stock();
                    stock.setProduitId(produitId);
                    System.out.println("Création d'un nouveau stock");
                }

                stock.setQuantiteActuelle(quantite);
                stock.setSeuilAlerte(seuil);
                stock.setUniteMesure(comboUnite.getValue());
                stock.setDateReception(dateReception.getValue() != null ? Date.valueOf(dateReception.getValue()) : null);
                stock.setDateExpiration(dateExpiration.getValue() != null ? Date.valueOf(dateExpiration.getValue()) : null);
                stock.setEmplacement(txtEmplacement.getText());

                if (stockExistant != null) {
                    serviceStock.modifier(stock);
                    System.out.println("Stock modifié avec succès");
                } else {
                    serviceStock.ajouter(stock);
                    System.out.println("Stock ajouté avec succès");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Erreur Stock", "Erreur: " + e.getMessage());
                return;
            }
            annuler();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Échec de l'enregistrement : " + e.getMessage());
        }
    }


    @FXML
    private void annuler() {
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