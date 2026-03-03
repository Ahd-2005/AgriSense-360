package controllers;

import entity.Tache;
import entity.Tache.Priorite;
import entity.user;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import services.TacheService;

import java.sql.SQLException;
import java.time.LocalDate;

public class TacheAdd {

    @FXML private Label    ouvrierNameLabel;
    @FXML private TextField titreField;
    @FXML private TextArea  descriptionField;
    @FXML private ComboBox<Priorite> prioriteCombo;
    @FXML private DatePicker dateEcheancePicker;
    @FXML private Label titreError;
    @FXML private Label prioriteError;

    private user currentOuvrier;
    private OuvrierDetails ouvrierDetailsController;

    @FXML
    public void initialize() {
        prioriteCombo.setItems(FXCollections.observableArrayList(Priorite.values()));
        prioriteCombo.setValue(Priorite.NORMALE);
        prioriteCombo.setConverter(new StringConverter<Priorite>() {
            @Override public String toString(Priorite p) {
                if (p == null) return "";
                switch (p) {
                    case BASSE:   return "🟢 Basse";
                    case NORMALE: return "🔵 Normale";
                    case HAUTE:   return "🔴 Haute";
                    default:      return p.name();
                }
            }
            @Override public Priorite fromString(String s) { return null; }
        });
        dateEcheancePicker.setValue(LocalDate.now().plusDays(3));
    }

    public void setOuvrier(user ouvrier) {
        this.currentOuvrier = ouvrier;
        ouvrierNameLabel.setText("Affecter à : " + ouvrier.getName());
    }

    public void setOuvrierDetailsController(OuvrierDetails controller) {
        this.ouvrierDetailsController = controller;
    }

    // ───────────────────────────────────────────────
    // SAUVEGARDE
    // ───────────────────────────────────────────────
    @FXML
    private void handleSave() {
        if (!validate()) return;

        try {
            Tache t = new Tache();
            t.setTitre(titreField.getText().trim());
            t.setDescription(descriptionField.getText().trim());
            t.setOuvrierId(currentOuvrier.getId());
            t.setPriorite(prioriteCombo.getValue());
            t.setStatut(Tache.Statut.EN_ATTENTE);
            if (dateEcheancePicker.getValue() != null)
                t.setDateEcheance(dateEcheancePicker.getValue());

            new TacheService().ajouterTache(t);

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "✅ Tâche affectée à " + currentOuvrier.getName() + " avec succès !");
            goBack();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec : " + e.getMessage());
        }
    }

    private boolean validate() {
        titreError.setVisible(false);
        prioriteError.setVisible(false);
        boolean ok = true;

        if (titreField.getText().trim().isEmpty()) {
            titreError.setText("❌ Le titre est requis");
            titreError.setVisible(true);
            ok = false;
        }
        if (prioriteCombo.getValue() == null) {
            prioriteError.setText("❌ Veuillez choisir une priorité");
            prioriteError.setVisible(true);
            ok = false;
        }
        return ok;
    }

    @FXML
    private void handleCancel() { goBack(); }

    private void goBack() {
        try {
            Parent root = titreField.getScene().getRoot();
            if (root instanceof BorderPane) {
                StackPane contentArea = (StackPane) ((BorderPane) root).getCenter();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OuvrierDetails.fxml"));
                Parent content = loader.load();
                OuvrierDetails ctrl = loader.getController();
                ctrl.setOuvrier(currentOuvrier);
                // Récupérer OuvrierManagement du contexte parent si disponible
                if (ouvrierDetailsController != null)
                    ctrl.setOuvrierManagement(ouvrierDetailsController.getOuvrierManagement());
                contentArea.getChildren().clear();
                contentArea.getChildren().add(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}
