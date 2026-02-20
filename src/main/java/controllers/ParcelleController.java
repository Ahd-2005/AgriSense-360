package controllers;

import entity.Parcelle;
import entity.Culture;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import services.ParcelleService;
import services.CultureService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ParcelleController {

    private final ParcelleService service = new ParcelleService();
    private final CultureService cultureService = new CultureService();

    @FXML private GridPane parcelleGrid;
    @FXML private TextField searchField;

    // Sort buttons
    @FXML private Button statutButton;
    @FXML private Button surfaceButton;
    @FXML private Button resetButton;

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
        // Custom order: Libre first, then Occupée
        Map<String, Integer> statutOrder = new HashMap<>();
        statutOrder.put("Libre", 1);
        statutOrder.put("Occupée", 2);

        filteredParcelles.sort(Comparator.comparing(p -> statutOrder.getOrDefault(p.getStatut(), 999)));
        displayParcelles(filteredParcelles);
        setActiveSortButton(statutButton);
    }

    @FXML
    private void sortBySurface() {
        filteredParcelles.sort(Comparator.comparing(Parcelle::getSurface).reversed());
        displayParcelles(filteredParcelles);
        setActiveSortButton(surfaceButton);
    }

    @FXML
    private void resetSort() {
        filteredParcelles = new ArrayList<>(allParcelles);
        displayParcelles(filteredParcelles);
        setActiveSortButton(null); // Remove active from all
    }

    // Helper method to manage active sort button styling
    private void setActiveSortButton(Button activeButton) {
        // Remove active class from all buttons
        if (statutButton != null) statutButton.getStyleClass().remove("sort-button-active");
        if (surfaceButton != null) surfaceButton.getStyleClass().remove("sort-button-active");

        // Add active class to selected button
        if (activeButton != null && !activeButton.getStyleClass().contains("sort-button-active")) {
            activeButton.getStyleClass().add("sort-button-active");
        }
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

        // Add double-click event to show cultures in this parcelle
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                showCulturesInParcelle(parcelle);
            }
        });

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

            // NEW VALIDATION: Check if trying to set to "Libre" but there are still cultures
            if ("Libre".equalsIgnoreCase(statut)) {
                try {
                    double remainingSurface = service.getRemainingParcelleSize(id);
                    double usedSurface = surface - remainingSurface;

                    if (usedSurface > 0.01) { // If there are still cultures
                        showError(messageLabel, "❌ Impossible de passer à 'Libre'! Il y a encore " +
                                String.format("%.2f", usedSurface) + " m² de cultures dans cette parcelle.");
                        return false;
                    }
                } catch (SQLException e) {
                    showError(messageLabel, "❌ Erreur de vérification de la surface");
                    return false;
                }
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

            // Show success message
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Succès");
            success.setHeaderText(null);
            success.setContentText("✅ Parcelle modifiée avec succès!");
            success.showAndWait();

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

    // Show all cultures in this parcelle in a popup
    private void showCulturesInParcelle(Parcelle parcelle) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.setTitle("Cultures de " + parcelle.getNom());

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");

        VBox content = new VBox(15);
        content.getStyleClass().add("popup-content");
        content.setPrefWidth(900);
        content.setMaxWidth(900);
        content.setMaxHeight(600);
        content.setPadding(new Insets(20));

        // Header with close button
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🌱 Cultures de la parcelle: " + parcelle.getNom());
        title.getStyleClass().add("popup-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("close-button");
        closeBtn.setOnAction(e -> popup.close());
        header.getChildren().addAll(title, spacer, closeBtn);

        // Parcelle info summary
        VBox parcelleInfo = new VBox(8);
        parcelleInfo.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 12; -fx-background-radius: 8;");

        try {
            double remaining = service.getRemainingParcelleSize(parcelle.getId());
            Label surfaceLabel = new Label("📏 Surface totale: " + parcelle.getSurface() + " m²");
            Label restantLabel = new Label("📦 Surface restante: " + String.format("%.2f", remaining) + " m²");
            Label usedLabel = new Label("🌾 Surface utilisée: " + String.format("%.2f", (parcelle.getSurface() - remaining)) + " m²");

            surfaceLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
            restantLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            usedLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #FF9800; -fx-font-weight: bold;");

            parcelleInfo.getChildren().addAll(surfaceLabel, usedLabel, restantLabel);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Scrollable culture cards
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        GridPane cultureGrid = new GridPane();
        cultureGrid.setHgap(15);
        cultureGrid.setVgap(15);
        cultureGrid.setPadding(new Insets(10));

        try {
            List<Culture> allCultures = cultureService.getAllCultures();
            List<Culture> parcelleCultures = allCultures.stream()
                    .filter(c -> c.getParcelleId() == parcelle.getId())
                    .collect(Collectors.toList());

            if (parcelleCultures.isEmpty()) {
                Label noCultures = new Label("🌾 Aucune culture dans cette parcelle");
                noCultures.setStyle("-fx-font-size: 16px; -fx-text-fill: #888; -fx-padding: 30;");
                cultureGrid.add(noCultures, 0, 0);
            } else {
                int col = 0;
                int row = 0;
                for (Culture culture : parcelleCultures) {
                    VBox cultureCard = createCultureCardForPopup(culture);
                    cultureGrid.add(cultureCard, col, row);
                    col++;
                    if (col == 3) { // 3 cards per row
                        col = 0;
                        row++;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        scrollPane.setContent(cultureGrid);
        content.getChildren().addAll(header, parcelleInfo, scrollPane);

        root.getChildren().add(content);
        root.setOnMouseClicked(e -> {
            if (e.getTarget() == root) popup.close();
        });

        Scene scene = new Scene(root, 950, 650);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/cards.css").toExternalForm());

        popup.setScene(scene);
        popup.show();
    }

    // Create a culture card for the popup
    // Create a culture card for the popup
    private VBox createCultureCardForPopup(Culture culture) {
        VBox card = new VBox(10);
        card.getStyleClass().add("culture-card");
        card.setPrefWidth(260);
        card.setMaxWidth(260);
        card.setPrefHeight(300); // Increased height
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(10));

        // Culture image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(220);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("culture-image");

        if (culture.getImg() != null && !culture.getImg().isEmpty()) {
            try {
                Image img = new Image(getClass().getResourceAsStream("/images/cultures/" + culture.getImg()));
                imageView.setImage(img);
            } catch (Exception e) {
                try {
                    imageView.setImage(new Image(getClass().getResourceAsStream("/images/cultures/default.png")));
                } catch (Exception ex) {
                    imageView.setImage(null);
                }
            }
        }

        // Culture type
        Label typeLabel = new Label(culture.getTypeCulture());
        typeLabel.getStyleClass().add("culture-type");
        typeLabel.setWrapText(true);
        typeLabel.setAlignment(Pos.CENTER);

        // Culture name
        Label nameLabel = new Label(culture.getNom());
        nameLabel.getStyleClass().add("culture-name");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);

        // État badge - FIXED to handle harvest states
        Label etatLabel = new Label(culture.getEtat());
        String etatClass;
        String etat = culture.getEtat().toLowerCase();

        // Handle special cases for harvest states
        if (etat.contains("récolte prévue") || etat.contains("recolte prevue")) {
            etatClass = "etat-recolte-prevue";
        } else if (etat.contains("récolte en retard") || etat.contains("recolte en retard")) {
            etatClass = "etat-recolte-en-retard";
        } else {
            etatClass = "etat-" + etat
                    .replace("é", "e")
                    .replace("è", "e")
                    .replace("à", "a");
        }
        etatLabel.getStyleClass().addAll("culture-etat", etatClass);
        etatLabel.setWrapText(true);
        etatLabel.setMaxWidth(200);

        // Surface info
        Label surfaceLabel = new Label("📏 " + culture.getSurface() + " m²");
        surfaceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666; -fx-font-weight: bold;");

        // Dates info (optional but helpful)
        Label datesLabel = new Label("📅 " + culture.getDatePlantation() + " → " + culture.getDateRecolte());
        datesLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
        datesLabel.setWrapText(true);
        datesLabel.setAlignment(Pos.CENTER);

        card.getChildren().addAll(imageView, typeLabel, nameLabel, etatLabel, surfaceLabel, datesLabel);

        // Add double-click to view full details
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                showCultureDetailFromParcelle(culture);
            }
        });

        return card;
    }

    // Helper method to show culture details from parcelle popup
    private void showCultureDetailFromParcelle(Culture culture) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/culture_detail.fxml"));
            if (loader.getLocation() == null) {
                // If no detail view exists, show a simple alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Détails de la culture");
                alert.setHeaderText(culture.getNom());
                alert.setContentText(
                        "Type: " + culture.getTypeCulture() + "\n" +
                                "État: " + culture.getEtat() + "\n" +
                                "Date plantation: " + culture.getDatePlantation() + "\n" +
                                "Date récolte: " + culture.getDateRecolte() + "\n" +
                                "Surface: " + culture.getSurface() + " m²"
                );
                alert.showAndWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}