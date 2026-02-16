package controllers;

import entity.Parcelle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import services.ParcelleService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ParcelleController {

    private final ParcelleService service = new ParcelleService();

    @FXML private GridPane parcelleGrid;
    @FXML private TextField searchField;

    private List<Parcelle> allParcelles = new ArrayList<>();
    private List<Parcelle> filteredParcelles = new ArrayList<>();

    @FXML
    public void initialize() {
        if (parcelleGrid != null) {
            loadParcelleCards();
        }

        // Add search listener
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterParcelles(newValue);
            });
        }
    }

    private void loadParcelleCards() {
        try {
            allParcelles = service.getAllParcelles();
            filteredParcelles = new ArrayList<>(allParcelles);
            displayParcelles(filteredParcelles);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filterParcelles(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredParcelles = new ArrayList<>(allParcelles);
        } else {
            String lowerSearch = searchText.toLowerCase();
            filteredParcelles = allParcelles.stream()
                    .filter(p -> p.getNom().toLowerCase().contains(lowerSearch))
                    .collect(Collectors.toList());
        }
        displayParcelles(filteredParcelles);
    }

    @FXML
    private void sortByStatut() {
        filteredParcelles.sort(Comparator.comparing(Parcelle::getStatut));
        displayParcelles(filteredParcelles);
    }

    @FXML
    private void sortBySurface() {
        filteredParcelles.sort(Comparator.comparing(Parcelle::getSurface).reversed());
        displayParcelles(filteredParcelles);
    }

    @FXML
    private void resetSort() {
        filteredParcelles = new ArrayList<>(allParcelles);
        displayParcelles(filteredParcelles);
    }

    private void displayParcelles(List<Parcelle> parcelles) {
        parcelleGrid.getChildren().clear();
        int col = 0;
        int row = 0;

        for (Parcelle parcelle : parcelles) {
            VBox card = createParcelleCard(parcelle);
            parcelleGrid.add(card, col, row);

            col++;
            if (col == 3) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createParcelleCard(Parcelle parcelle) {
        VBox card = new VBox(12);
        card.getStyleClass().add("parcelle-card");
        card.setPrefWidth(350);
        card.setPrefHeight(280);

        Label nameLabel = new Label(parcelle.getNom());
        nameLabel.getStyleClass().add("parcelle-name");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(8);

        Label surfaceLabel = new Label("Surface:");
        surfaceLabel.getStyleClass().add("parcelle-info-label");
        Label surfaceValue = new Label(parcelle.getSurface() + " m²");
        surfaceValue.getStyleClass().add("parcelle-info");
        infoGrid.add(surfaceLabel, 0, 0);
        infoGrid.add(surfaceValue, 1, 0);

        double remaining = 0 ;
        try {
            remaining = service.getRemainingParcelleSize(parcelle.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Label remainingLabel = new Label("Restant:");
        remainingLabel.getStyleClass().add("parcelle-info-label");
        Label remainingValue = new Label(String.format("%.2f", remaining) + " m²");
        remainingValue.getStyleClass().add("parcelle-info");
        infoGrid.add(remainingLabel, 0, 1);
        infoGrid.add(remainingValue, 1, 1);

        Label localisationLabel = new Label("Localisation:");
        localisationLabel.getStyleClass().add("parcelle-info-label");
        Label localisationValue = new Label(parcelle.getLocalisation());
        localisationValue.getStyleClass().add("parcelle-info");
        infoGrid.add(localisationLabel, 0, 2);
        infoGrid.add(localisationValue, 1, 2);

        Label typeSolLabel = new Label("Type de sol:");
        typeSolLabel.getStyleClass().add("parcelle-info-label");
        Label typeSolValue = new Label(parcelle.getTypeSol());
        typeSolValue.getStyleClass().add("parcelle-info");
        infoGrid.add(typeSolLabel, 0, 3);
        infoGrid.add(typeSolValue, 1, 3);

        Label statutLabel = new Label(parcelle.getStatut());
        if ("Libre".equalsIgnoreCase(parcelle.getStatut())) {
            statutLabel.getStyleClass().add("statut-libre");
        } else {
            // Handle both spellings
            statutLabel.getStyleClass().add("statut-occupee");
            statutLabel.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; " +
                    "-fx-padding: 5 10; -fx-background-radius: 12px; " +
                    "-fx-font-size: 12px; -fx-font-weight: bold;");
        }

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button editBtn = new Button("✏ Modifier");
        editBtn.setStyle("-fx-font-size: 14px;");
        editBtn.getStyleClass().addAll("card-button", "edit-button");
        editBtn.setOnAction(e -> showUpdatePopup(parcelle));

        Button deleteBtn = new Button("🗑 Supprimer");
        deleteBtn.setStyle("-fx-font-size: 14px;");
        deleteBtn.getStyleClass().addAll("card-button", "delete-button");
        deleteBtn.setOnAction(e -> handleDelete(parcelle));

        buttonBox.getChildren().addAll(editBtn, deleteBtn);
        card.getChildren().addAll(nameLabel, infoGrid, statutLabel, buttonBox);

        return card;
    }

    @FXML
    private void showAddPopup() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");

        VBox content = new VBox(12);
        content.getStyleClass().add("popup-content");
        content.setPrefWidth(450);
        content.setMaxWidth(450);
        content.setPadding(new Insets(20));

        HBox header = new HBox();
        Label title = new Label("➕ Ajouter une Parcelle");
        title.getStyleClass().add("popup-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("close-button");
        closeBtn.setOnAction(e -> popup.close());
        header.getChildren().addAll(title, spacer, closeBtn);

        VBox nomBox = new VBox(5);
        Label nomLabel = new Label("Nom de la parcelle:");
        nomLabel.getStyleClass().add("form-label");
        TextField nomField = new TextField();
        nomField.setPromptText("Ex: Parcelle Nord");
        nomField.setPrefWidth(400);
        nomBox.getChildren().addAll(nomLabel, nomField);

        VBox surfaceBox = new VBox(5);
        Label surfaceLabel = new Label("Surface (m²):");
        surfaceLabel.getStyleClass().add("form-label");
        TextField surfaceField = new TextField();
        surfaceField.setPromptText("Ex: 500");
        surfaceField.setPrefWidth(400);
        surfaceBox.getChildren().addAll(surfaceLabel, surfaceField);

        VBox localisationBox = new VBox(5);
        Label localisationLabel = new Label("Localisation:");
        localisationLabel.getStyleClass().add("form-label");
        TextField localisationField = new TextField();
        localisationField.setPromptText("Ex: Zone A, Secteur 3");
        localisationField.setPrefWidth(400);
        localisationBox.getChildren().addAll(localisationLabel, localisationField);

        VBox typeSolBox = new VBox(5);
        Label typeSolLabel = new Label("Type de sol:");
        typeSolLabel.getStyleClass().add("form-label");
        TextField typeSolField = new TextField();
        typeSolField.setPromptText("Ex: Argileux, Sableux");
        typeSolField.setPrefWidth(400);
        typeSolBox.getChildren().addAll(typeSolLabel, typeSolField);

        VBox statutBox = new VBox(5);
        Label statutLabel = new Label("Statut:");
        statutLabel.getStyleClass().add("form-label");
        ComboBox<String> statutComboBox = new ComboBox<>();
        statutComboBox.setPrefWidth(400);
        statutComboBox.getItems().addAll("Libre", "Occupée");
        statutComboBox.setValue("Libre");
        statutBox.getChildren().addAll(statutLabel, statutComboBox);

        Label messageLabel = new Label();

        Button saveBtn = new Button("✅ Enregistrer");
        saveBtn.getStyleClass().addAll("card-button", "edit-button");
        saveBtn.setPrefWidth(150);
        saveBtn.setOnAction(e -> {
            if (handleAddParcelle(nomField, surfaceField, localisationField,
                    typeSolField, statutComboBox, messageLabel)) {
                popup.close();
                loadParcelleCards();
            }
        });

        HBox buttonContainer = new HBox(saveBtn);
        buttonContainer.setAlignment(Pos.CENTER);

        content.getChildren().addAll(header, nomBox, surfaceBox, localisationBox,
                typeSolBox, statutBox, messageLabel, buttonContainer);

        root.getChildren().add(content);
        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/cards.css").toExternalForm());
        popup.setScene(scene);
        popup.show();
    }

    private void showUpdatePopup(Parcelle parcelle) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");

        VBox content = new VBox(12);
        content.getStyleClass().add("popup-content");
        content.setPrefWidth(450);
        content.setMaxWidth(450);
        content.setPadding(new Insets(20));

        HBox header = new HBox();
        Label title = new Label("✏️ Modifier la Parcelle");
        title.getStyleClass().add("popup-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("close-button");
        closeBtn.setOnAction(e -> popup.close());
        header.getChildren().addAll(title, spacer, closeBtn);

        VBox idBox = new VBox(5);
        Label idLabel = new Label("ID:");
        idLabel.getStyleClass().add("form-label");
        TextField idField = new TextField(String.valueOf(parcelle.getId()));
        idField.setEditable(false);
        idField.setPrefWidth(400);
        idBox.getChildren().addAll(idLabel, idField);

        VBox nomBox = new VBox(5);
        Label nomLabel = new Label("Nom de la parcelle:");
        nomLabel.getStyleClass().add("form-label");
        TextField nomField = new TextField(parcelle.getNom());
        nomField.setPrefWidth(400);
        nomBox.getChildren().addAll(nomLabel, nomField);

        VBox surfaceBox = new VBox(5);
        Label surfaceLabel = new Label("Surface (m²):");
        surfaceLabel.getStyleClass().add("form-label");
        TextField surfaceField = new TextField(String.valueOf(parcelle.getSurface()));
        surfaceField.setPrefWidth(400);
        surfaceBox.getChildren().addAll(surfaceLabel, surfaceField);

        VBox localisationBox = new VBox(5);
        Label localisationLabel = new Label("Localisation:");
        localisationLabel.getStyleClass().add("form-label");
        TextField localisationField = new TextField(parcelle.getLocalisation());
        localisationField.setPrefWidth(400);
        localisationBox.getChildren().addAll(localisationLabel, localisationField);

        VBox typeSolBox = new VBox(5);
        Label typeSolLabel = new Label("Type de sol:");
        typeSolLabel.getStyleClass().add("form-label");
        TextField typeSolField = new TextField(parcelle.getTypeSol());
        typeSolField.setPrefWidth(400);
        typeSolBox.getChildren().addAll(typeSolLabel, typeSolField);

        VBox statutBox = new VBox(5);
        Label statutLabel = new Label("Statut:");
        statutLabel.getStyleClass().add("form-label");
        ComboBox<String> statutComboBox = new ComboBox<>();
        statutComboBox.setPrefWidth(400);
        statutComboBox.getItems().addAll("Libre", "Occupée");
        statutComboBox.setValue(parcelle.getStatut());
        statutBox.getChildren().addAll(statutLabel, statutComboBox);

        Label messageLabel = new Label();

        Button saveBtn = new Button("💾 Enregistrer");
        saveBtn.getStyleClass().addAll("card-button", "edit-button");
        saveBtn.setPrefWidth(150);
        saveBtn.setOnAction(e -> {
            if (handleUpdateParcelle(idField, nomField, surfaceField, localisationField,
                    typeSolField, statutComboBox, messageLabel)) {
                popup.close();
                loadParcelleCards();
            }
        });

        HBox buttonContainer = new HBox(saveBtn);
        buttonContainer.setAlignment(Pos.CENTER);

        content.getChildren().addAll(header, idBox, nomBox, surfaceBox,
                localisationBox, typeSolBox, statutBox,
                messageLabel, buttonContainer);

        root.getChildren().add(content);
        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/cards.css").toExternalForm());
        popup.setScene(scene);
        popup.show();
    }

    private boolean handleAddParcelle(TextField nomField, TextField surfaceField,
                                      TextField localisationField, TextField typeSolField,
                                      ComboBox<String> statutComboBox, Label messageLabel) {
        try {
            String nom = nomField.getText();
            String surfaceText = surfaceField.getText();
            String localisation = localisationField.getText();
            String typeSol = typeSolField.getText();
            String statut = statutComboBox.getValue();

            if (nom == null || nom.trim().isEmpty() || !nom.matches("[A-Za-zÀ-ÿ ]{3,}")) {
                showError(messageLabel, "❌ Nom invalide (min 3 caractères)");
                return false;
            }

            if (surfaceText == null || surfaceText.trim().isEmpty()) {
                showError(messageLabel, "❌ Surface requise");
                return false;
            }

            double surface;
            try {
                surface = Double.parseDouble(surfaceText);
                if (surface <= 0) {
                    showError(messageLabel, "❌ Surface doit être positive");
                    return false;
                }
            } catch (NumberFormatException e) {
                showError(messageLabel, "❌ Surface doit être un nombre");
                return false;
            }

            if (localisation == null || localisation.trim().isEmpty() || !localisation.matches("[A-Za-zÀ-ÿ0-9 ,]{3,}")) {
                showError(messageLabel, "❌ Localisation invalide (min 3 caractères)");
                return false;
            }

            if (typeSol == null || typeSol.trim().isEmpty() || !typeSol.matches("[A-Za-zÀ-ÿ ]{3,}")) {
                showError(messageLabel, "❌ Type de sol invalide (min 3 lettres)");
                return false;
            }

            if (statut == null || statut.trim().isEmpty()) {
                showError(messageLabel, "❌ Veuillez sélectionner un statut");
                return false;
            }

            Parcelle p = new Parcelle();
            p.setNom(nom.trim());
            p.setSurface(surface);
            p.setLocalisation(localisation.trim());
            p.setTypeSol(typeSol.trim());
            p.setStatut(statut.trim());

            service.addParcelle(p);
            return true;

        } catch (Exception e) {
            showError(messageLabel, "❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean handleUpdateParcelle(TextField idField, TextField nomField, TextField surfaceField,
                                         TextField localisationField, TextField typeSolField,
                                         ComboBox<String> statutComboBox, Label messageLabel) {
        try {
            int id = Integer.parseInt(idField.getText());
            String nom = nomField.getText();
            String surfaceText = surfaceField.getText();
            String localisation = localisationField.getText();
            String typeSol = typeSolField.getText();
            String statut = statutComboBox.getValue();

            if (nom == null || nom.trim().isEmpty() || !nom.matches("[A-Za-zÀ-ÿ ]{3,}")) {
                showError(messageLabel, "❌ Nom invalide (min 3 caractères)");
                return false;
            }

            if (surfaceText == null || surfaceText.trim().isEmpty()) {
                showError(messageLabel, "❌ Surface requise");
                return false;
            }

            double surface;
            try {
                surface = Double.parseDouble(surfaceText);
                if (surface <= 0) {
                    showError(messageLabel, "❌ Surface doit être positive");
                    return false;
                }
            } catch (NumberFormatException e) {
                showError(messageLabel, "❌ Surface doit être un nombre");
                return false;
            }

            if (localisation == null || localisation.trim().isEmpty() || !localisation.matches("[A-Za-zÀ-ÿ0-9 ,]{3,}")) {
                showError(messageLabel, "❌ Localisation invalide (min 3 caractères)");
                return false;
            }

            if (typeSol == null || typeSol.trim().isEmpty() || !typeSol.matches("[A-Za-zÀ-ÿ ]{3,}")) {
                showError(messageLabel, "❌ Type de sol invalide (min 3 lettres)");
                return false;
            }

            if (statut == null || statut.trim().isEmpty()) {
                showError(messageLabel, "❌ Veuillez sélectionner un statut");
                return false;
            }

            Parcelle p = new Parcelle();
            p.setId(id);
            p.setNom(nom.trim());
            p.setSurface(surface);
            p.setLocalisation(localisation.trim());
            p.setTypeSol(typeSol.trim());
            p.setStatut(statut.trim());

            service.updateParcelle(p);
            return true;

        } catch (Exception e) {
            showError(messageLabel, "❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void handleDelete(Parcelle parcelle) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Êtes-vous sûr de vouloir supprimer cette parcelle ?");
        confirmAlert.setContentText("Parcelle: " + parcelle.getNom() + " (ID: " + parcelle.getId() + ")");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.deleteParcelle(parcelle.getId());
                    loadParcelleCards();

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Succès");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("✅ Parcelle supprimée avec succès!");
                    successAlert.showAndWait();

                } catch (SQLException e) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Erreur");
                    errorAlert.setHeaderText("Erreur lors de la suppression");
                    errorAlert.setContentText("❌ " + e.getMessage());
                    errorAlert.showAndWait();
                    e.printStackTrace();
                }
            }
        });
    }

    private void showError(Label messageLabel, String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

    @FXML
    void goBackToCulture() {
        // Get the MainLayoutController instance and navigate back to culture
        MainLayoutController controller = MainLayoutController.getInstance();
        if (controller != null) {
            controller.navigateToCulture();
        }
    }
}