package controllers;

import controllers.MainLayoutController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import entity.Stock;
import services.ServiceStockStock;

import java.math.BigDecimal;
import java.sql.Date;

public class EditStockController {

    @FXML private TextField txtProduitId;
    @FXML private TextField txtQuantite;
    @FXML private TextField txtSeuil;
    @FXML private ComboBox<String> comboUnite;
    @FXML private DatePicker dateReception;
    @FXML private DatePicker dateExpiration;
    @FXML private TextField txtEmplacement;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnAnnuler;

    private ServiceStockStock serviceStock = new ServiceStockStock();
    private Stock stockToEdit = null;

    @FXML
    public void initialize() {
        comboUnite.getItems().addAll("kg", "L", "sac", "pièce");
        Stock stock = controllers.MainLayoutController.getStockToEdit();
        if (stock != null) {
            setStockToEdit(stock);
        }
    }

    // Méthode pour pré-remplir le formulaire
    public void setStockToEdit(Stock stock) {
        this.stockToEdit = stock;

        // Pré-remplir (produitId est int, donc pas de vérification null)
        txtProduitId.setText(String.valueOf(stock.getProduitId()));
        txtQuantite.setText(stock.getQuantiteActuelle() != null ? stock.getQuantiteActuelle().toString() : "");
        txtSeuil.setText(stock.getSeuilAlerte() != null ? stock.getSeuilAlerte().toString() : "");
        comboUnite.setValue(stock.getUniteMesure());
        dateReception.setValue(stock.getDateReception() != null ? stock.getDateReception().toLocalDate() : null);
        dateExpiration.setValue(stock.getDateExpiration() != null ? stock.getDateExpiration().toLocalDate() : null);
        txtEmplacement.setText(stock.getEmplacement() != null ? stock.getEmplacement() : "");

        btnEnregistrer.setText("Modifier Stock");
    }

    @FXML
    private void enregistrer() {
        // Contrôles de saisie renforcés
        String produitIdText = txtProduitId.getText().trim();
        if (produitIdText.isEmpty()) {
            showAlert("Erreur", "Produit ID est obligatoire.");
            return;
        }
        int produitId;
        try {
            produitId = Integer.parseInt(produitIdText);
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Produit ID doit être un nombre valide.");
            return;
        }
        if (!txtQuantite.getText().trim().isEmpty()) {
            try {
                BigDecimal quantite = new BigDecimal(txtQuantite.getText().trim());
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

        // Mettre à jour le stock
        stockToEdit.setProduitId(produitId);
        stockToEdit.setQuantiteActuelle(!txtQuantite.getText().trim().isEmpty() ? new BigDecimal(txtQuantite.getText().trim()) : null);
        stockToEdit.setSeuilAlerte(!txtSeuil.getText().trim().isEmpty() ? new BigDecimal(txtSeuil.getText().trim()) : null);
        stockToEdit.setUniteMesure(comboUnite.getValue());
        stockToEdit.setDateReception(dateReception.getValue() != null ? Date.valueOf(dateReception.getValue()) : null);
        stockToEdit.setDateExpiration(dateExpiration.getValue() != null ? Date.valueOf(dateExpiration.getValue()) : null);
        stockToEdit.setEmplacement(txtEmplacement.getText().trim());

        try {
            serviceStock.modifier(stockToEdit);
            showAlert("Succès", "Stock modifié avec succès.");
            annuler(); // Redirection après succès
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la modification : " + e.getMessage());
        }
    }

    @FXML
    private void annuler() {
        // Retour à la liste des stocks (charge dans le centre, sidebar reste)
        if (controllers.MainLayoutController.getInstance() != null) {
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