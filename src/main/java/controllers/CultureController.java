package controllers;



import entity.Culture;

import javafx.application.Platform;

import javafx.concurrent.Task;

import javafx.scene.control.*;

import services.HarvestAIService;



import java.util.Optional;

import javafx.stage.Window;

import utils.CultureDurations;

import entity.Parcelle;

import entity.user;

import entity.user.Role;

import javafx.fxml.FXML;

import javafx.geometry.Insets;

import javafx.geometry.Pos;

import javafx.scene.Scene;

import javafx.scene.control.*;

import javafx.scene.image.Image;

import javafx.scene.image.ImageView;

import javafx.scene.layout.*;

import javafx.stage.Modality;

import javafx.stage.Stage;

import javafx.stage.StageStyle;

import services.CultureService;

import services.ParcelleService;

import services.SessionManager;



import java.sql.Date;

import java.sql.SQLException;

import java.time.LocalDate;

import java.util.*;

import java.util.concurrent.atomic.AtomicReference;

import java.util.stream.Collectors;



public class CultureController {



    private final CultureService service = new CultureService();

    private final HarvestAIService harvestAIService = new HarvestAIService();

    private final ParcelleService parcelleService = new ParcelleService();

    private final Map<String, String[]> cultureMap = new HashMap<>();

    private final Map<String, String> imageMap = new HashMap<>();



    @FXML private GridPane cultureGrid;

    @FXML private TextField searchField;

    @FXML private Button addCultureBtn;

    @FXML private Button agendaBtn;



    // Sort buttons

    @FXML private Button typeButton;

    @FXML private Button etatButton;

    @FXML private Button surfaceButton;

    @FXML private Button resetButton;



    private List<Culture> allCultures = new ArrayList<>();

    private List<Culture> filteredCultures = new ArrayList<>();

    private user currentUser;



    @FXML

    public void initialize() {

        // Get current user from session

        SessionManager sessionManager = SessionManager.getInstance();

        if (sessionManager.isLoggedIn()) {

            this.currentUser = sessionManager.getCurrentUser();

            configurePermissions();

        }



        initializeCultureMap();

        initializeImageMap();



        if (cultureGrid != null) {

            loadCultureCards();

        }



        // Add search listener

        if (searchField != null) {

            searchField.textProperty().addListener((observable, oldValue, newValue) -> {

                filterCultures(newValue);

            });

        }

    }



    private void configurePermissions() {

        if (currentUser != null) {

            Role userRole = currentUser.getRole();



            // Workers (Ouvriers) have read-only access

            if (userRole == Role.ROLE_OUVRIER && addCultureBtn != null) {

                addCultureBtn.setDisable(true);

            }

        }

    }



    private void initializeCultureMap() {

        cultureMap.put("Céréales", new String[]{"Blé", "Maïs", "Riz", "Avoine"});

        cultureMap.put("Légumes", new String[]{"Tomates", "Salades", "Pomme de terre", "Carottes", "Oignon", "Lentille"});

        cultureMap.put("Fruits", new String[]{"Pomme", "Pêche", "Orange", "Fraise", "Framboise", "Banane"});

        cultureMap.put("Ornementales", new String[]{"Rosier", "Tulipe", "Jasmin", "Laurier-rose"});

    }



    private void initializeImageMap() {

        imageMap.put("Blé", "ble.png");

        imageMap.put("Maïs", "mais.png");

        imageMap.put("Riz", "riz.png");

        imageMap.put("Avoine", "avoine.png");

        imageMap.put("Tomates", "tomates.png");

        imageMap.put("Salades", "salades.png");

        imageMap.put("Pomme de terre", "pomme_de_terre.png");

        imageMap.put("Carottes", "carottes.png");

        imageMap.put("Oignon", "oignon.png");

        imageMap.put("Lentille", "lentille.png");

        imageMap.put("Pomme", "pomme.png");

        imageMap.put("Pêche", "peche.png");

        imageMap.put("Orange", "orange.png");

        imageMap.put("Fraise", "fraise.png");

        imageMap.put("Framboise", "framboise.png");

        imageMap.put("Banane", "banane.png");

        imageMap.put("Rosier", "rosier.png");

        imageMap.put("Tulipe", "tulipe.png");

        imageMap.put("Jasmin", "jasmin.png");

        imageMap.put("Laurier-rose", "laurier_rose.png");

    }



    private void loadCultureCards() {
        try {
            if (currentUser != null && currentUser.getFarmId() != null) {
                allCultures = service.getCulturesByFarm(currentUser.getFarmId());
            } else {
                allCultures = service.getAllCultures();
            }
            filteredCultures = new ArrayList<>(allCultures);
            displayCultures(filteredCultures);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*

    private boolean updateAllCulturesStates() {

        boolean anyUpdated = false;



        for (Culture culture : allCultures) {

            LocalDate plantationDate = culture.getDatePlantation().toLocalDate();

            LocalDate recolteDate = culture.getDateRecolte().toLocalDate();



            String calculatedState = CultureDurations.calculateCurrentState(

                    plantationDate, recolteDate, culture.getNom()

            );



            // Update if state has changed

            if (!calculatedState.equals(culture.getEtat())) {

                culture.setEtat(calculatedState);

                try {

                    service.updateCulture(culture);

                    anyUpdated = true;

                    System.out.println("✅ État mis à jour: " + culture.getNom() + " -> " + calculatedState);

                } catch (SQLException e) {

                    System.err.println("❌ Error updating culture state: " + e.getMessage());

                    e.printStackTrace();

                }

            }

        }



        return anyUpdated;

    }

    */

    private void filterCultures(String searchText) {

        if (searchText == null || searchText.trim().isEmpty()) {

            filteredCultures = new ArrayList<>(allCultures);

        } else {

            String lowerSearch = searchText.toLowerCase();

            filteredCultures = allCultures.stream()

                    .filter(c -> c.getNom().toLowerCase().contains(lowerSearch))

                    .collect(Collectors.toList());

        }

        displayCultures(filteredCultures);

    }



    @FXML

    private void sortByType() {

        // Custom order: Céréales, Légumes, Fruits, Ornementales

        Map<String, Integer> typeOrder = new HashMap<>();

        typeOrder.put("Céréales", 1);

        typeOrder.put("Légumes", 2);

        typeOrder.put("Fruits", 3);

        typeOrder.put("Ornementales", 4);



        filteredCultures.sort(Comparator.comparing(c -> typeOrder.getOrDefault(c.getTypeCulture(), 999)));

        displayCultures(filteredCultures);

        setActiveSortButton(typeButton);

    }



    @FXML

    private void sortByEtat() {

        // Custom order: Semis, Croissance, Maturité, Récolte, Récolte prévue, Récolte en retard

        Map<String, Integer> etatOrder = new HashMap<>();

        etatOrder.put("Semis", 1);

        etatOrder.put("Croissance", 2);

        etatOrder.put("Maturité", 3);

        etatOrder.put("Récolte", 4);

        etatOrder.put("Récolte prévue", 5);

        etatOrder.put("Récolte en retard", 6);



        filteredCultures.sort(Comparator.comparing(c -> etatOrder.getOrDefault(c.getEtat(), 999)));

        displayCultures(filteredCultures);

        setActiveSortButton(etatButton);

    }



    @FXML

    private void sortBySurface() {

        filteredCultures.sort(Comparator.comparing(Culture::getSurface).reversed());

        displayCultures(filteredCultures);

        setActiveSortButton(surfaceButton);

    }



    @FXML

    private void resetSort() {

        filteredCultures = new ArrayList<>(allCultures);

        displayCultures(filteredCultures);

        setActiveSortButton(null); // Remove active from all

    }



    // Helper method to manage active sort button styling

    private void setActiveSortButton(Button activeButton) {

        // Remove active class from all buttons

        if (typeButton != null) typeButton.getStyleClass().remove("sort-button-active");

        if (etatButton != null) etatButton.getStyleClass().remove("sort-button-active");

        if (surfaceButton != null) surfaceButton.getStyleClass().remove("sort-button-active");



        // Add active class to selected button

        if (activeButton != null && !activeButton.getStyleClass().contains("sort-button-active")) {

            activeButton.getStyleClass().add("sort-button-active");

        }

    }



    private void displayCultures(List<Culture> cultures) {

        cultureGrid.getChildren().clear();

        int col = 0;

        int row = 0;



        for (Culture culture : cultures) {

            VBox card = createCultureCard(culture);

            cultureGrid.add(card, col, row);



            col++;

            if (col == 4) {

                col = 0;

                row++;

            }

        }

    }



    private VBox createCultureCard(Culture culture) {

        VBox card = new VBox(10);

        card.getStyleClass().add("culture-card");

        card.setAlignment(Pos.CENTER);

        card.setPrefWidth(250);

        card.setPrefHeight(300);



        ImageView imageView = new ImageView();

        imageView.setFitWidth(150);

        imageView.setFitHeight(150);

        imageView.setPreserveRatio(true);

        imageView.getStyleClass().add("culture-image");



        try {

            Image image = new Image(getClass().getResourceAsStream("/images/cultures/" + culture.getImg()));

            imageView.setImage(image);

        } catch (Exception e) {

            imageView.setImage(null);

        }



        Label typeLabel = new Label(culture.getTypeCulture());

        typeLabel.getStyleClass().add("culture-type");



        Label nameLabel = new Label(culture.getNom());

        nameLabel.getStyleClass().add("culture-name");



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



        HBox buttonBox = new HBox(8);

        buttonBox.setAlignment(Pos.CENTER);



        Button editBtn = new Button("✏");

        editBtn.setStyle("-fx-font-size: 14px;");

        editBtn.getStyleClass().addAll("card-button", "edit-button");

        editBtn.setOnAction(e -> showUpdatePopup(culture));



        Button deleteBtn = new Button("🗑");

        deleteBtn.setStyle("-fx-font-size: 14px;");

        deleteBtn.getStyleClass().addAll("card-button", "delete-button");

        deleteBtn.setOnAction(e -> handleDelete(culture));



        // Harvest button

        Button harvestBtn = new Button("🌾 Récolter");

        harvestBtn.setStyle("-fx-font-size: 14px;");

        harvestBtn.getStyleClass().addAll("card-button", "harvest-button");

        harvestBtn.setOnAction(e -> handleHarvest(culture));



        buttonBox.getChildren().addAll(editBtn, deleteBtn, harvestBtn);

        // Disable edit/delete for workers

        if (currentUser != null && currentUser.getRole() == Role.ROLE_OUVRIER) {

            editBtn.setDisable(true);

            deleteBtn.setDisable(true);

        }



        card.getChildren().addAll(imageView, typeLabel, nameLabel, etatLabel, buttonBox);

        card.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {

                showDetailPopup(culture);

            }

        });



        return card;

    }



    private void showDetailPopup(Culture culture) {

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

        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("Fermer");

        closeBtn.getStyleClass().addAll("card-button", "edit-button");

        closeBtn.setOnAction(e -> popup.close());



        // Create harvest button FIRST before using it

        Button harvestDetailBtn = new Button("🌾 Récolter cette culture");

        harvestDetailBtn.getStyleClass().addAll("card-button", "harvest-button");

        harvestDetailBtn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 30;");

        harvestDetailBtn.setOnAction(e -> {

            popup.close();

            handleHarvest(culture);

        });



        HBox buttonBox = new HBox(10);

        buttonBox.setAlignment(Pos.CENTER);

        buttonBox.getChildren().addAll(closeBtn, harvestDetailBtn);

        header.getChildren().addAll(spacer, buttonBox);



        Label title = new Label("📋 Détails de la Culture");

        title.getStyleClass().add("popup-title");



        ImageView imageView = new ImageView();

        imageView.setFitWidth(200);

        imageView.setFitHeight(200);

        imageView.setPreserveRatio(true);

        try {

            Image image = new Image(getClass().getResourceAsStream("/images/cultures/" + culture.getImg()));

            imageView.setImage(image);

        } catch (Exception e) {

            imageView.setImage(null);

        }



        VBox details = new VBox(10);

        details.getChildren().addAll(

                createDetailRow("Type:", culture.getTypeCulture()),

                createDetailRow("Nom:", culture.getNom()),

                createDetailRow("État:", culture.getEtat()),

                createDetailRow("Date Plantation:", culture.getDatePlantation().toString()),

                createDetailRow("Date Récolte:", culture.getDateRecolte().toString()),

                createDetailRow("Surface:", culture.getSurface() + " m²"),

                createDetailRow("Parcelle ID:", String.valueOf(culture.getParcelleId()))

        );



        try {

            List<Parcelle> parcelles = parcelleService.getAllParcelles();

            for (Parcelle p : parcelles) {

                if (p.getId() == culture.getParcelleId()) {

                    details.getChildren().add(createDetailRow("Nom Parcelle:", p.getNom()));

                    break;

                }

            }

        } catch (SQLException e) {

            e.printStackTrace();

        }



        content.getChildren().addAll(header, title, imageView, details);

        root.getChildren().add(content);



        Scene scene = new Scene(root);

        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        scene.getStylesheets().add(getClass().getResource("/css/cards.css").toExternalForm());

        popup.setScene(scene);

        popup.show();

    }



    private HBox createDetailRow(String label, String value) {

        HBox row = new HBox(10);

        row.setAlignment(Pos.CENTER_LEFT);



        Label labelNode = new Label(label);

        labelNode.getStyleClass().add("detail-label");

        labelNode.setPrefWidth(150);



        Label valueNode = new Label(value);

        valueNode.getStyleClass().add("detail-value");



        row.getChildren().addAll(labelNode, valueNode);

        return row;

    }



    @FXML

    private void showAddPopup() {

        // Check permissions

        if (currentUser != null && currentUser.getRole() == Role.ROLE_OUVRIER) {

            Alert alert = new Alert(Alert.AlertType.WARNING);

            alert.setTitle("Permission Denied");

            alert.setContentText("You don't have permission to add cultures.");

            alert.showAndWait();

            return;

        }



        Stage popup = new Stage();

        popup.initModality(Modality.APPLICATION_MODAL);

        popup.initStyle(StageStyle.TRANSPARENT);



        StackPane root = new StackPane();

        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");



        VBox content = new VBox(12);

        content.getStyleClass().add("popup-content");

        content.setPrefWidth(480);

        content.setMaxWidth(480);

        content.setPadding(new Insets(20));



        HBox header = new HBox();

        Label title = new Label("➕ Ajouter une Culture");

        title.getStyleClass().add("popup-title");

        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");

        closeBtn.getStyleClass().add("close-button");

        closeBtn.setOnAction(e -> popup.close());

        header.getChildren().addAll(title, spacer, closeBtn);



        VBox typeBox = new VBox(5);

        Label typeLabel = new Label("Type de culture:");

        typeLabel.getStyleClass().add("form-label");

        ComboBox<String> typeComboBox = new ComboBox<>();

        typeComboBox.setPromptText("Sélectionner le type");

        typeComboBox.getStyleClass().add("form-field");

        typeComboBox.getItems().addAll("Céréales", "Légumes", "Fruits", "Ornementales");

        typeBox.getChildren().addAll(typeLabel, typeComboBox);



        VBox nomBox = new VBox(4);

        Label nomLabel = new Label("Nom:");

        nomLabel.getStyleClass().add("form-label");

        ComboBox<String> nomComboBox = new ComboBox<>();

        nomComboBox.setPromptText("Sélectionner d'abord le type");

        nomComboBox.getStyleClass().add("form-field");

        nomComboBox.setDisable(true);

        nomBox.getChildren().addAll(nomLabel, nomComboBox);



        typeComboBox.setOnAction(e -> {

            String selectedType = typeComboBox.getValue();

            if (selectedType != null) {

                nomComboBox.setDisable(false);

                nomComboBox.getItems().clear();

                nomComboBox.getItems().addAll(cultureMap.get(selectedType));

            }

        });



        VBox dpBox = new VBox(4);

        Label dpLabel = new Label("Date de plantation:");

        dpLabel.getStyleClass().add("form-label");

        DatePicker datePlantationPicker = new DatePicker();

        datePlantationPicker.getStyleClass().add("form-field");

        dpBox.getChildren().addAll(dpLabel, datePlantationPicker);



        VBox drBox = new VBox(4);

        Label drLabel = new Label("Date de récolte:");

        drLabel.getStyleClass().add("form-label");

        DatePicker dateRecoltePicker = new DatePicker();

        dateRecoltePicker.getStyleClass().add("form-field");

        drBox.getChildren().addAll(drLabel, dateRecoltePicker);



        // Auto-calculate harvest date when culture and plantation date are selected

        Label durationInfoLabel = new Label("");

        durationInfoLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px; -fx-font-weight: bold;");

        drBox.getChildren().add(durationInfoLabel);



        // Update harvest date when culture name changes

        nomComboBox.setOnAction(e -> {

            String selectedNom = nomComboBox.getValue();

            LocalDate plantation = datePlantationPicker.getValue();

            if (selectedNom != null && plantation != null) {

                LocalDate harvestDate = CultureDurations.calculateHarvestDate(plantation, selectedNom);

                dateRecoltePicker.setValue(harvestDate);

                int totalDays = CultureDurations.getTotalDuration(selectedNom);

                durationInfoLabel.setText("📅 Durée estimée: " + totalDays + " jours");

            }

            // Also handle image selection

            if (selectedNom != null) {

                String img = imageMap.get(selectedNom);

                // ... existing image code ...

            }

        });



        // Update harvest date when plantation date changes

        datePlantationPicker.setOnAction(e -> {

            String selectedNom = nomComboBox.getValue();

            LocalDate plantation = datePlantationPicker.getValue();

            if (selectedNom != null && plantation != null) {

                LocalDate harvestDate = CultureDurations.calculateHarvestDate(plantation, selectedNom);

                dateRecoltePicker.setValue(harvestDate);

                int totalDays = CultureDurations.getTotalDuration(selectedNom);

                durationInfoLabel.setText("📅 Durée estimée: " + totalDays + " jours");

            }

        });



        VBox surfaceBox = new VBox(4);

        Label surfaceLabel = new Label("Surface (m²):");

        surfaceLabel.getStyleClass().add("form-label");

        TextField surfaceField = new TextField();

        surfaceField.setPromptText("Ex: 100.5");

        surfaceField.getStyleClass().add("form-field");

        surfaceBox.getChildren().addAll(surfaceLabel, surfaceField);



        VBox parcelleBox = new VBox(4);

        Label parcelleLabel = new Label("Parcelle:");

        parcelleLabel.getStyleClass().add("form-label");

        ComboBox<String> parcelleComboBox = new ComboBox<>();

        parcelleComboBox.getStyleClass().add("form-field");

        loadLibreParcelles(parcelleComboBox);

        parcelleBox.getChildren().addAll(parcelleLabel, parcelleComboBox);



        Label messageLabel = new Label();



        Button saveBtn = new Button("✅ Enregistrer");

        saveBtn.getStyleClass().addAll("card-button", "edit-button");

        saveBtn.setOnAction(e -> {

            if (handleAddCulture(typeComboBox, nomComboBox, datePlantationPicker,

                    dateRecoltePicker, surfaceField,

                    parcelleComboBox, messageLabel)) {

                popup.close();

                loadCultureCards();

            }

        });



        content.getChildren().addAll(header, typeBox, nomBox, dpBox, drBox,

                surfaceBox, parcelleBox,

                messageLabel, saveBtn);



        root.getChildren().add(content);

        Scene scene = new Scene(root);

        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        scene.getStylesheets().add(getClass().getResource("/css/cards.css").toExternalForm());

        popup.setScene(scene);

        popup.show();

    }



    private void showUpdatePopup(Culture culture) {

        // Check permissions

        if (currentUser != null && currentUser.getRole() == Role.ROLE_OUVRIER) {

            Alert alert = new Alert(Alert.AlertType.WARNING);

            alert.setTitle("Permission Denied");

            alert.setContentText("You don't have permission to update cultures.");

            alert.showAndWait();

            return;

        }



        Stage popup = new Stage();

        popup.initModality(Modality.APPLICATION_MODAL);

        popup.initStyle(StageStyle.TRANSPARENT);



        StackPane root = new StackPane();

        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");



        VBox content = new VBox(8);

        content.getStyleClass().add("popup-content");

        content.setPrefWidth(400);

        content.setMaxWidth(400);

        content.setPadding(new Insets(15));



        HBox header = new HBox();

        Label title = new Label("✏️ Modifier la Culture");

        title.getStyleClass().add("popup-title");

        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");

        closeBtn.getStyleClass().add("close-button");

        closeBtn.setOnAction(e -> popup.close());

        header.getChildren().addAll(title, spacer, closeBtn);



        // ID hidden — stored in variable, not shown to keep form compact

        TextField idField = new TextField(String.valueOf(culture.getId()));

        idField.setVisible(false);

        idField.setManaged(false);



        VBox typeBox = new VBox(4);

        Label typeLabel = new Label("Type de culture:");

        typeLabel.getStyleClass().add("form-label");

        ComboBox<String> typeComboBox = new ComboBox<>();

        typeComboBox.getStyleClass().add("form-field");

        typeComboBox.getItems().addAll("Céréales", "Légumes", "Fruits", "Ornementales");

        typeComboBox.setValue(culture.getTypeCulture());

        typeBox.getChildren().addAll(typeLabel, typeComboBox);



        VBox nomBox = new VBox(5);

        Label nomLabel = new Label("Nom:");

        nomLabel.getStyleClass().add("form-label");

        ComboBox<String> nomComboBox = new ComboBox<>();

        nomComboBox.getStyleClass().add("form-field");

        if (culture.getTypeCulture() != null && cultureMap.containsKey(culture.getTypeCulture())) {

            nomComboBox.getItems().addAll(cultureMap.get(culture.getTypeCulture()));

            nomComboBox.setValue(culture.getNom());

        }

        nomBox.getChildren().addAll(nomLabel, nomComboBox);



        typeComboBox.setOnAction(e -> {

            String selectedType = typeComboBox.getValue();

            if (selectedType != null) {

                nomComboBox.getItems().clear();

                nomComboBox.getItems().addAll(cultureMap.get(selectedType));

            }

        });



        VBox dpBox = new VBox(5);

        Label dpLabel = new Label("Date de plantation:");

        dpLabel.getStyleClass().add("form-label");

        DatePicker datePlantationPicker = new DatePicker(culture.getDatePlantation().toLocalDate());

        datePlantationPicker.getStyleClass().add("form-field");

        dpBox.getChildren().addAll(dpLabel, datePlantationPicker);



        VBox drBox = new VBox(5);

        Label drLabel = new Label("Date de récolte:");

        drLabel.getStyleClass().add("form-label");

        DatePicker dateRecoltePicker = new DatePicker(culture.getDateRecolte().toLocalDate());

        dateRecoltePicker.getStyleClass().add("form-field");

        drBox.getChildren().addAll(drLabel, dateRecoltePicker);



        VBox etatBox = new VBox(4);

        Label etatLabel = new Label("État:");

        etatLabel.getStyleClass().add("form-label");

        ComboBox<String> etatComboBox = new ComboBox<>();

        etatComboBox.getStyleClass().add("form-field");

        etatComboBox.getItems().addAll("Semis", "Croissance", "Maturité", "Récolte", "Récolte prévue", "Récolte en retard");

        etatComboBox.setValue(culture.getEtat());

        etatComboBox.setDisable(true); // ✅ DÉSACTIVER LE COMBOBOX

        etatComboBox.setStyle("-fx-opacity: 0.7;"); // Visual feedback qu'il est désactivé

        etatBox.getChildren().addAll(etatLabel, etatComboBox);



        // Info message

        // Info message

        Label etatInfoLabel = new Label("ℹ️ L'état est calculé automatiquement selon la date");

        etatInfoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888; -fx-padding: 5 0 0 0;");

        etatBox.getChildren().add(etatInfoLabel);



        // ✅ NOUVEAU: Label pour afficher la durée estimée

        Label durationInfoLabel = new Label("");

        durationInfoLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 0 0 0;");

        drBox.getChildren().add(durationInfoLabel); // Ajouter sous dateRecoltePicker



        // ✅ NOUVEAU: Auto-update harvest date and state when plantation date changes

        datePlantationPicker.setOnAction(e -> {

            LocalDate newPlantationDate = datePlantationPicker.getValue();

            String cultureName = nomComboBox.getValue();



            if (newPlantationDate != null && cultureName != null) {

                // Calculate new harvest date

                LocalDate newHarvestDate = CultureDurations.calculateHarvestDate(newPlantationDate, cultureName);

                dateRecoltePicker.setValue(newHarvestDate);



                // Calculate new state

                String newState = CultureDurations.calculateCurrentState(

                        newPlantationDate, newHarvestDate, cultureName

                );

                etatComboBox.setValue(newState);



                // Show duration info

                int totalDays = CultureDurations.getTotalDuration(cultureName);

                durationInfoLabel.setText("📅 Durée estimée: " + totalDays + " jours");



                System.out.println("✅ Dates mises à jour automatiquement:");

                System.out.println("   - Plantation: " + newPlantationDate);

                System.out.println("   - Récolte: " + newHarvestDate);

                System.out.println("   - État: " + newState);

            }

        });



        // ✅ NOUVEAU: Also update when culture name changes (if user changes type/nom)

        nomComboBox.setOnAction(e -> {

            LocalDate plantationDate = datePlantationPicker.getValue();

            String cultureName = nomComboBox.getValue();



            if (plantationDate != null && cultureName != null) {

                LocalDate newHarvestDate = CultureDurations.calculateHarvestDate(plantationDate, cultureName);

                dateRecoltePicker.setValue(newHarvestDate);



                String newState = CultureDurations.calculateCurrentState(

                        plantationDate, newHarvestDate, cultureName

                );

                etatComboBox.setValue(newState);



                int totalDays = CultureDurations.getTotalDuration(cultureName);

                durationInfoLabel.setText("📅 Durée estimée: " + totalDays + " jours");

            }

        });



        VBox surfaceBox = new VBox(5);

        Label surfaceLabel = new Label("Surface (m²):");

        surfaceLabel.getStyleClass().add("form-label");

        TextField surfaceField = new TextField(String.valueOf(culture.getSurface()));

        surfaceField.getStyleClass().add("form-field");

        surfaceBox.getChildren().addAll(surfaceLabel, surfaceField);



        VBox parcelleBox = new VBox(5);

        Label parcelleLabel = new Label("Parcelle:");

        parcelleLabel.getStyleClass().add("form-label");

        ComboBox<String> parcelleComboBox = new ComboBox<>();

        parcelleComboBox.getStyleClass().add("form-field");

        loadLibreParcellesForEdit(parcelleComboBox, culture.getParcelleId()); // Include current parcelle



        for (String item : parcelleComboBox.getItems()) {

            if (item.startsWith(culture.getParcelleId() + " - ")) {

                parcelleComboBox.setValue(item);

                break;

            }

        }

        parcelleBox.getChildren().addAll(parcelleLabel, parcelleComboBox);



        Label messageLabel = new Label();



        Button saveBtn = new Button("💾 Enregistrer");

        saveBtn.getStyleClass().addAll("card-button", "edit-button");

        saveBtn.setOnAction(e -> {

            if (handleUpdateCulture(culture, idField, typeComboBox, nomComboBox,

                    datePlantationPicker, dateRecoltePicker, etatComboBox,

                    surfaceField, parcelleComboBox, messageLabel)) {

                popup.close();

                loadCultureCards();

            }

        });



        content.getChildren().addAll(header, typeBox, nomBox, dpBox, drBox,

                etatBox, surfaceBox, parcelleBox,

                messageLabel, saveBtn);



        root.getChildren().add(content);

        Scene scene = new Scene(root);

        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        scene.getStylesheets().add(getClass().getResource("/css/cards.css").toExternalForm());

        popup.setScene(scene);

        popup.show();

    }



    private void showEtatChangeWarning(Culture culture, String newEtat,

                                       DatePicker datePlantationPicker,

                                       DatePicker dateRecoltePicker,

                                       ComboBox<String> nomComboBox) {

        Alert warning = new Alert(Alert.AlertType.WARNING);

        warning.setTitle("⚠️ Attention - Changement d'état");

        warning.setHeaderText("Vous êtes sur le point de changer l'état de la culture");

        warning.setContentText(

                "État actuel: " + culture.getEtat() + "\n" +

                        "Nouvel état: " + newEtat + "\n\n" +

                        "La date de récolte sera recalculée en fonction du nouvel état.\n" +

                        "Voulez-vous continuer?"

        );



        ButtonType btnConfirm = new ButtonType("Oui, continuer", ButtonBar.ButtonData.OK_DONE);

        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        warning.getButtonTypes().setAll(btnConfirm, btnCancel);



        Optional<ButtonType> result = warning.showAndWait();

        if (result.isPresent() && result.get() == btnConfirm) {

            // Recalculate dates based on new state

            recalculateDatesFromEtat(newEtat, datePlantationPicker, dateRecoltePicker, nomComboBox);

        }

    }



    private void recalculateDatesFromEtat(String etat, DatePicker datePlantationPicker,

                                          DatePicker dateRecoltePicker,

                                          ComboBox<String> nomComboBox) {

        String selectedNom = nomComboBox.getValue();

        LocalDate currentPlantation = datePlantationPicker.getValue();



        if (selectedNom != null && currentPlantation != null) {

            int totalDays = CultureDurations.getTotalDuration(selectedNom);

            int daysPerState = totalDays / 4; // Approximate: Semis, Croissance, Maturité, Récolte



            LocalDate newHarvestDate;



            switch (etat) {

                case "Semis":

                    // Keep original dates

                    newHarvestDate = currentPlantation.plusDays(totalDays);

                    break;

                case "Croissance":

                    // Move plantation date forward to start of Croissance

                    LocalDate croissanceStart = currentPlantation.plusDays(daysPerState);

                    datePlantationPicker.setValue(croissanceStart);

                    newHarvestDate = croissanceStart.plusDays(totalDays - daysPerState);

                    break;

                case "Maturité":

                    // Move plantation date forward to start of Maturité

                    LocalDate maturiteStart = currentPlantation.plusDays(daysPerState * 2);

                    datePlantationPicker.setValue(maturiteStart);

                    newHarvestDate = maturiteStart.plusDays(totalDays - (daysPerState * 2));

                    break;

                case "Récolte":

                case "Récolte prévue":

                case "Récolte en retard":

                    // Move plantation date forward to start of Récolte

                    LocalDate recolteStart = currentPlantation.plusDays(daysPerState * 3);

                    datePlantationPicker.setValue(recolteStart);

                    newHarvestDate = recolteStart;

                    break;

                default:

                    newHarvestDate = CultureDurations.calculateHarvestDate(currentPlantation, selectedNom);

            }



            dateRecoltePicker.setValue(newHarvestDate);

        }

    }



    private boolean handleAddCulture(ComboBox<String> typeComboBox, ComboBox<String> nomComboBox,

                                     DatePicker datePlantationPicker, DatePicker dateRecoltePicker,

                                     TextField surfaceField,

                                     ComboBox<String> parcelleComboBox, Label messageLabel) {

        try {

            String type = typeComboBox.getValue();

            String nom = nomComboBox.getValue();

            LocalDate dp = datePlantationPicker.getValue();

            LocalDate dr = dateRecoltePicker.getValue();

            String parcelleSelection = parcelleComboBox.getValue();



            if (type == null || type.trim().isEmpty()) {

                showError(messageLabel, "❌ Veuillez sélectionner un type de culture");

                return false;

            }



            if (nom == null || nom.trim().isEmpty()) {

                showError(messageLabel, "❌ Veuillez sélectionner un nom de culture");

                return false;

            }



            if (dp == null) {

                showError(messageLabel, "❌ Date de plantation requise");

                return false;

            }



            if (dr == null) {

                showError(messageLabel, "❌ Date de récolte requise");

                return false;

            }



            if (dr.isBefore(dp)) {

                showError(messageLabel, "❌ Date récolte doit être après plantation");

                return false;

            }



            if (parcelleSelection == null || parcelleSelection.trim().isEmpty()) {

                showError(messageLabel, "❌ Veuillez sélectionner une parcelle");

                return false;

            }



            int parcelleId;

            try {

                parcelleId = Integer.parseInt(parcelleSelection.split(" - ")[0]);

            } catch (Exception e) {

                showError(messageLabel, "❌ Erreur de sélection de parcelle");

                return false;

            }



            String surfaceText = surfaceField.getText();

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



            try {

                double remainingSurface = parcelleService.getRemainingParcelleSize(parcelleId);

                if (surface > remainingSurface) {

                    showError(messageLabel, "❌ Surface trop grande! Restant: " +

                            String.format("%.2f", remainingSurface) + " m²");

                    return false;

                }

            } catch (SQLException e) {

                showError(messageLabel, "❌ Erreur de vérification de surface");

                return false;

            }



            Culture c = new Culture();

            c.setNom(nom.trim());

            c.setTypeCulture(type.trim());

            c.setDatePlantation(Date.valueOf(dp));

            c.setDateRecolte(Date.valueOf(dr));

            c.setSurface(surface);



            // Calculate initial state based on dates

            String calculatedEtat = CultureDurations.calculateCurrentState(

                    dp, dr, nom.trim()

            );

            c.setEtat(calculatedEtat);



            c.setParcelleId(parcelleId);



            String imagePath = imageMap.get(nom);

            if (imagePath == null) {

                imagePath = "default.png";

            }

            c.setImg(imagePath);



            service.addCulture(c);

            return true;



        } catch (Exception e) {

            showError(messageLabel, "❌ Erreur: " + e.getMessage());

            e.printStackTrace();

            return false;

        }

    }



    private boolean handleUpdateCulture(Culture oldCulture, TextField idField,

                                        ComboBox<String> typeComboBox, ComboBox<String> nomComboBox,

                                        DatePicker datePlantationPicker, DatePicker dateRecoltePicker,

                                        ComboBox<String> etatComboBox, TextField surfaceField,

                                        ComboBox<String> parcelleComboBox, Label messageLabel) {

        try {

            int id = Integer.parseInt(idField.getText());

            String type = typeComboBox.getValue();

            String nom = nomComboBox.getValue();

            LocalDate dp = datePlantationPicker.getValue();

            LocalDate dr = dateRecoltePicker.getValue();

            String etat = etatComboBox.getValue();

            String parcelleSelection = parcelleComboBox.getValue();



            if (type == null || type.trim().isEmpty()) {

                showError(messageLabel, "❌ Veuillez sélectionner un type de culture");

                return false;

            }



            if (nom == null || nom.trim().isEmpty()) {

                showError(messageLabel, "❌ Veuillez sélectionner un nom de culture");

                return false;

            }



            if (etat == null || etat.trim().isEmpty()) {

                showError(messageLabel, "❌ Veuillez sélectionner un état");

                return false;

            }



            if (dp == null) {

                showError(messageLabel, "❌ Date de plantation requise");

                return false;

            }



            if (dr == null) {

                showError(messageLabel, "❌ Date de récolte requise");

                return false;

            }



            if (dr.isBefore(dp)) {

                showError(messageLabel, "❌ Date récolte doit être après plantation");

                return false;

            }



            if (parcelleSelection == null || parcelleSelection.trim().isEmpty()) {

                showError(messageLabel, "❌ Veuillez sélectionner une parcelle");

                return false;

            }



            int parcelleId;

            try {

                parcelleId = Integer.parseInt(parcelleSelection.split(" - ")[0]);

            } catch (Exception e) {

                showError(messageLabel, "❌ Erreur de sélection de parcelle");

                return false;

            }



            String surfaceText = surfaceField.getText();

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



            try {

                double remainingSurface = parcelleService.getRemainingParcelleSize(parcelleId);

                remainingSurface += oldCulture.getSurface();



                if (surface > remainingSurface) {

                    showError(messageLabel, "❌ Surface trop grande! Restant: " +

                            String.format("%.2f", remainingSurface) + " m²");

                    return false;

                }

            } catch (SQLException e) {

                showError(messageLabel, "❌ Erreur de vérification de surface");

                return false;

            }



            Culture c = new Culture();

            c.setId(id);

            c.setNom(nom.trim());

            c.setTypeCulture(type.trim());

            c.setDatePlantation(Date.valueOf(dp));

            c.setDateRecolte(Date.valueOf(dr));

            c.setEtat(etat.trim());

            c.setParcelleId(parcelleId);

            c.setSurface(surface);



            String imagePath = imageMap.get(nom);

            if (imagePath == null) {

                imagePath = oldCulture.getImg();

            }

            c.setImg(imagePath);



            service.updateCulture(c);

            return true;



        } catch (Exception e) {

            showError(messageLabel, "❌ Erreur: " + e.getMessage());

            e.printStackTrace();

            return false;

        }

    }



    private void handleDelete(Culture culture) {

        // Check permissions

        if (currentUser != null && currentUser.getRole() == Role.ROLE_OUVRIER) {

            Alert alert = new Alert(Alert.AlertType.WARNING);

            alert.setTitle("Permission Denied");

            alert.setContentText("You don't have permission to delete cultures.");

            alert.showAndWait();

            return;

        }



        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);

        confirmAlert.setTitle("Confirmation de suppression");

        confirmAlert.setHeaderText("Êtes-vous sûr de vouloir supprimer cette culture ?");

        confirmAlert.setContentText("Culture: " + culture.getNom() + " (ID: " + culture.getId() + ")");



        confirmAlert.showAndWait().ifPresent(response -> {

            if (response == ButtonType.OK) {

                try {

                    service.deleteCulture(culture.getId());

                    loadCultureCards();



                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);

                    successAlert.setTitle("Succès");

                    successAlert.setHeaderText(null);

                    successAlert.setContentText("✅ Culture supprimée avec succès!");

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

    /**

     * Handle harvest action

     */

    private void handleHarvest(Culture culture) {

        String etat = culture.getEtat();

        boolean isReady = CultureDurations.isReadyToHarvest(etat);



        if (!isReady) {

            // Show warning for early harvest

            Alert warning = new Alert(Alert.AlertType.WARNING);

            warning.setTitle("⚠️ Attention - Récolte Prématurée");

            warning.setHeaderText("Cette culture n'est pas encore prête!");

            warning.setContentText(

                    "État actuel: " + etat + "\n\n" +

                            CultureDurations.getEarlyHarvestWarning(etat) + "\n\n" +

                            "Voulez-vous quand même continuer la récolte?"

            );



            ButtonType btnConfirm = new ButtonType("Oui, récolter quand même", ButtonBar.ButtonData.OK_DONE);

            ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            warning.getButtonTypes().setAll(btnConfirm, btnCancel);



            Optional<ButtonType> result = warning.showAndWait();

            if (result.isEmpty() || result.get() != btnConfirm) {

                return; // User cancelled

            }

        }



        // Confirm harvest

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);

        confirm.setTitle("Confirmer la récolte");

        confirm.setHeaderText("Récolter: " + culture.getNom());

        confirm.setContentText(

                "Type: " + culture.getTypeCulture() + "\n" +

                        "Surface: " + culture.getSurface() + " m²\n" +

                        "État: " + culture.getEtat() + "\n\n" +

                        "La parcelle sera libérée et cette culture sera supprimée.\n" +

                        "Cette action est irréversible."

        );



        Optional<ButtonType> confirmResult = confirm.showAndWait();

        if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {

            performHarvest(culture);

        }

    }



    /**

     * Perform the actual harvest - delete culture and free parcelle space

     */

    private void performHarvest(Culture culture) {

        // 1. Demander l'emplacement de stockage

        javafx.scene.control.TextInputDialog emplacementDialog = new javafx.scene.control.TextInputDialog("Entrepot principal");

        emplacementDialog.setTitle("Recolte - Emplacement");

        emplacementDialog.setHeaderText("Culture : " + culture.getNom());

        emplacementDialog.setContentText("Emplacement de stockage :");

        Optional<String> emplacementOpt = emplacementDialog.showAndWait();

        if (emplacementOpt.isEmpty()) return;

        String emplacement = emplacementOpt.get();



        // 2. Afficher un label "calcul en cours..." (non bloquant)

        // Le calcul se fait dans un thread background

        Task<HarvestAIService.HarvestResult> task = new Task<>() {

            @Override

            protected HarvestAIService.HarvestResult call() {

                return harvestAIService.recolterCulture(culture, emplacement);

            }

        };



        task.setOnSucceeded(ev -> javafx.application.Platform.runLater(() -> {

            HarvestAIService.HarvestResult result = task.getValue();



            if (!result.success) {

                Alert error = new Alert(Alert.AlertType.ERROR);

                error.setTitle("Erreur IA");

                error.setHeaderText(null);

                error.setContentText(result.errorMessage);

                error.showAndWait();

                return;

            }



            if (result.quantiteKg <= 0) {

                // Trop tot pour recolter

                Alert info = new Alert(Alert.AlertType.INFORMATION);

                info.setTitle("Recolte impossible");

                info.setHeaderText(null);

                info.setContentText(

                        "Rendement : " + result.rendementPct + "%\n\n" +

                                result.explication + "\n\nAucune quantite ajoutee au stock."

                );

                info.showAndWait();

            } else {

                // ✅ Recolte officielle : supprime + libere + log RECOLTE avec quantite ML

                try {

                    service.recolterEtSupprimerCulture(culture, result.quantiteKg);

                } catch (SQLException ex) {

                    ex.printStackTrace();

                }



                Alert success = new Alert(Alert.AlertType.INFORMATION);

                success.setTitle("Recolte reussie !");

                success.setHeaderText(null);

                success.setContentText(

                        "Culture : " + culture.getNom() + "\n" +

                                "Quantite recoltee : " + String.format("%.1f", result.quantiteKg) + " kg\n" +

                                "Rendement : " + result.rendementPct + "%\n\n" +

                                result.explication + "\n\n" +

                                "Stock mis a jour (emplacement: " + emplacement + ")\n" +

                                "Surface liberee : " + culture.getSurface() + " m2"

                );

                success.showAndWait();



                loadCultureCards();

            }

        }));



        task.setOnFailed(ev -> javafx.application.Platform.runLater(() -> {

            Alert error = new Alert(Alert.AlertType.ERROR);

            error.setTitle("Erreur");

            error.setHeaderText(null);

            error.setContentText("Erreur inattendue: " +

                    (task.getException() != null ? task.getException().getMessage() : "inconnue"));

            error.showAndWait();

        }));



        Thread t = new Thread(task);

        t.setDaemon(true);

        t.start();

    }



    private void loadLibreParcelles(ComboBox<String> comboBox) {

        try {

            List<Parcelle> allParcelles = parcelleService.getAllParcelles();

            comboBox.getItems().clear();



            for (Parcelle p : allParcelles) {

                if ("Libre".equalsIgnoreCase(p.getStatut())) {

                    double remaining = parcelleService.getRemainingParcelleSize(p.getId());

                    if (remaining > 0.01) {

                        comboBox.getItems().add(

                                p.getId() + " - " + p.getNom() +

                                        " (Restant: " + String.format("%.2f", remaining) + " m² / " + p.getSurface() + " m²)"

                        );

                    }

                }

            }

        } catch (SQLException e) {

            e.printStackTrace();

        }

    }



    // Load parcelles for edit - includes current parcelle even if full

    private void loadLibreParcellesForEdit(ComboBox<String> comboBox, int currentParcelleId) {

        try {

            List<Parcelle> allParcelles = parcelleService.getAllParcelles();

            comboBox.getItems().clear();



            for (Parcelle p : allParcelles) {

                double remaining = parcelleService.getRemainingParcelleSize(p.getId());



                // Include if: (1) it's the current parcelle OR (2) it's libre with space

                if (p.getId() == currentParcelleId ||

                        ("Libre".equalsIgnoreCase(p.getStatut()) && remaining > 0.01)) {

                    comboBox.getItems().add(

                            p.getId() + " - " + p.getNom() +

                                    " (Restant: " + String.format("%.2f", remaining) + " m² / " + p.getSurface() + " m²)"

                    );

                }

            }

        } catch (SQLException e) {

            e.printStackTrace();

        }

    }



    private void showError(Label messageLabel, String message) {

        messageLabel.setText(message);

        messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

    }



    @FXML

    void goToParcelles() {

        // Get the MainLayoutController instance and navigate to parcelle

        MainLayoutController controller = MainLayoutController.getInstance();

        if (controller != null) {

            controller.navigateToParcelle();

        }

    }

    @FXML

    private void openAgenda() {

        // Récupérer la fenêtre parente pour centrer la popup

        Window ownerWindow = null;

        if (cultureGrid != null && cultureGrid.getScene() != null) {

            ownerWindow = cultureGrid.getScene().getWindow();

        }

        AgendaController.open((javafx.stage.Stage) ownerWindow);

    }



}