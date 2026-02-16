package com.example.agrisens360.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.example.agrisens360.entity.Produit;
import com.example.agrisens360.entity.Stock;
import com.example.agrisens360.services.ServiceProduit;
import com.example.agrisens360.services.ServiceStock;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;

public class AddStockController {

    @FXML private ComboBox<Produit> comboProduits;
    @FXML private TextField txtQuantite;
    @FXML private TextField txtSeuil;
    @FXML private ComboBox<String> comboUnite;
    @FXML private DatePicker dateReception;
    @FXML private DatePicker dateExpiration;
    @FXML private TextField txtEmplacement;
    @FXML private Button btnEnregistrer;

    private ServiceProduit serviceProduit = new ServiceProduit();
    private ServiceStock serviceStock = new ServiceStock();
    private ObservableList<Produit> produitsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        comboUnite.getItems().addAll("kg", "L", "sac", "pièce");
        loadProduits();
    }

    private void loadProduits() {
        try {
            produitsList.clear();
            produitsList.addAll(serviceProduit.afficher());
            comboProduits.setItems(produitsList);
            comboProduits.setConverter(new javafx.util.StringConverter<Produit>() {
                @Override
                public String toString(Produit produit) {
                    return produit != null ? produit.getNom() + " (ID: " + produit.getId() + ")" : "";
                }

                @Override
                public Produit fromString(String string) {
                    return null; // Non utilisé
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement des produits : " + e.getMessage());
        }
    }

    @FXML
    private void enregistrer() {
        // Contrôles de saisie
        Produit selectedProduit = comboProduits.getValue();
        if (selectedProduit == null) {
            showAlert("Erreur", "Veuillez sélectionner un produit.");
            return;
        }
        if (txtQuantite.getText().trim().isEmpty()) {
            showAlert("Erreur", "La quantité est obligatoire.");
            return;
        }
        try {
            BigDecimal quantite = new BigDecimal(txtQuantite.getText().trim());
            if (quantite.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert("Erreur", "La quantité doit être positive.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "La quantité doit être un nombre valide.");
            return;
        }
        if (!txtSeuil.getText().trim().isEmpty()) {
            try {
                BigDecimal seuil = new BigDecimal(txtSeuil.getText().trim());
                if (seuil.compareTo(BigDecimal.ZERO) < 0) {
                    showAlert("Erreur", "Le seuil ne peut pas être négatif.");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Erreur", "Le seuil doit être un nombre valide.");
                return;
            }
        }

        Stock stock = new Stock();
        stock.setProduitId(selectedProduit.getId());
        stock.setQuantiteActuelle(new BigDecimal(txtQuantite.getText().trim()));
        stock.setSeuilAlerte(!txtSeuil.getText().trim().isEmpty() ? new BigDecimal(txtSeuil.getText().trim()) : null);
        stock.setUniteMesure(comboUnite.getValue());
        stock.setDateReception(dateReception.getValue() != null ? Date.valueOf(dateReception.getValue()) : null);
        stock.setDateExpiration(dateExpiration.getValue() != null ? Date.valueOf(dateExpiration.getValue()) : null);
        stock.setEmplacement(txtEmplacement.getText().trim());

        try {
            serviceStock.ajouter(stock);
            showAlert("Succès", "Stock ajouté avec succès.");
            annuler(); // Retour à la liste
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de l'ajout : " + e.getMessage());
        }
    }


    @FXML
    private void annuler() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToStockList();
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