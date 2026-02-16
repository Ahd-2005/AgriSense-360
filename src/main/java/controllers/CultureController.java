package controllers;

import entity.Culture;
import entity.Parcelle;
import entity.user;
import entity.user.Role;
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
import services.CultureService;
import services.ParcelleService;
import services.SessionManager;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class CultureController {

    private final CultureService service = new CultureService();
    private final ParcelleService parcelleService = new ParcelleService();
    private final Map<String, String[]> cultureMap = new HashMap<>();
    private final Map<String, String> imageMap = new HashMap<>();

    @FXML private GridPane cultureGrid;
    @FXML private TextField searchField;
    @FXML private Button addCultureBtn;

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
            allCultures = service.getAllCultures();

            // Filter by user if not admin
            if (currentUser != null && currentUser.getRole() != Role.ROLE_ADMIN) {
                // If you have user_id field in Culture entity, filter here
                // For now, showing all cultures
            }

            filteredCultures = new ArrayList<>(allCultures);
            displayCultures(filteredCultures);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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
        filteredCultures.sort(Comparator.comparing(Culture::getTypeCulture));
        displayCultures(filteredCultures);
    }

    @FXML
    private void sortByEtat() {
        filteredCultures.sort(Comparator.comparing(Culture::getEtat));
        displayCultures(filteredCultures);
    }

    @FXML
    private void sortBySurface() {
        filteredCultures.sort(Comparator.comparing(Culture::getSurface).reversed());
        displayCultures(filteredCultures);
    }

    @FXML
    private void resetSort() {
        filteredCultures = new ArrayList<>(allCultures);
        displayCultures(filteredCultures);
    }

    private void displayCultures(List<Culture> cultures) {
        cultureGrid.getChildren().clear();
        int col = 0;
        int row = 0;

        for (Culture culture : cultures) {
            VBox card = createCultureCard(culture);
            cultureGrid.add(card, col, row);

            col++;
            if (col == 5) {
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
        String etatClass = "etat-" + culture.getEtat()
                .toLowerCase()
                .replace("é", "e")
                .replace("è", "e")
                .replace("à", "a");
        etatLabel.getStyleClass().addAll("culture-etat", etatClass);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button editBtn = new Button("✏");
        editBtn.setStyle("-fx-font-size: 16px;");
        editBtn.getStyleClass().addAll("card-button", "edit-button");
        editBtn.setOnAction(e -> showUpdatePopup(culture));

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle("-fx-font-size: 16px;");
        deleteBtn.getStyleClass().addAll("card-button", "delete-button");
        deleteBtn.setOnAction(e -> handleDelete(culture));

        // Disable edit/delete for workers
        if (currentUser != null && currentUser.getRole() == Role.ROLE_OUVRIER) {
            editBtn.setDisable(true);
            deleteBtn.setDisable(true);
        }

        buttonBox.getChildren().addAll(editBtn, deleteBtn);
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
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("close-button");
        closeBtn.setOnAction(e -> popup.close());
        header.getChildren().addAll(spacer, closeBtn);

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

        VBox nomBox = new VBox(5);
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

        VBox dpBox = new VBox(5);
        Label dpLabel = new Label("Date de plantation:");
        dpLabel.getStyleClass().add("form-label");
        DatePicker datePlantationPicker = new DatePicker();
        datePlantationPicker.getStyleClass().add("form-field");
        dpBox.getChildren().addAll(dpLabel, datePlantationPicker);

        VBox drBox = new VBox(5);
        Label drLabel = new Label("Date de récolte:");
        drLabel.getStyleClass().add("form-label");
        DatePicker dateRecoltePicker = new DatePicker();
        dateRecoltePicker.getStyleClass().add("form-field");
        drBox.getChildren().addAll(drLabel, dateRecoltePicker);

        VBox etatBox = new VBox(5);
        Label etatLabel = new Label("État:");
        etatLabel.getStyleClass().add("form-label");
        ComboBox<String> etatComboBox = new ComboBox<>();
        etatComboBox.getStyleClass().add("form-field");
        etatComboBox.getItems().addAll("Semis", "Croissance", "Maturité", "Récolte");
        etatComboBox.setValue("Semis");
        etatBox.getChildren().addAll(etatLabel, etatComboBox);

        VBox surfaceBox = new VBox(5);
        Label surfaceLabel = new Label("Surface (m²):");
        surfaceLabel.getStyleClass().add("form-label");
        TextField surfaceField = new TextField();
        surfaceField.setPromptText("Ex: 100.5");
        surfaceField.getStyleClass().add("form-field");
        surfaceBox.getChildren().addAll(surfaceLabel, surfaceField);

        VBox parcelleBox = new VBox(5);
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
                    dateRecoltePicker, etatComboBox, surfaceField,
                    parcelleComboBox, messageLabel)) {
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

        VBox content = new VBox(12);
        content.getStyleClass().add("popup-content");
        content.setPrefWidth(480);
        content.setMaxWidth(480);
        content.setPadding(new Insets(20));

        HBox header = new HBox();
        Label title = new Label("✏️ Modifier la Culture");
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
        TextField idField = new TextField(String.valueOf(culture.getId()));
        idField.setEditable(false);
        idField.getStyleClass().add("form-field");
        idBox.getChildren().addAll(idLabel, idField);

        VBox typeBox = new VBox(5);
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

        VBox etatBox = new VBox(5);
        Label etatLabel = new Label("État:");
        etatLabel.getStyleClass().add("form-label");
        ComboBox<String> etatComboBox = new ComboBox<>();
        etatComboBox.getStyleClass().add("form-field");
        etatComboBox.getItems().addAll("Semis", "Croissance", "Maturité", "Récolte");
        etatComboBox.setValue(culture.getEtat());
        etatBox.getChildren().addAll(etatLabel, etatComboBox);

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
        loadLibreParcelles(parcelleComboBox);

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

        content.getChildren().addAll(header, idBox, typeBox, nomBox, dpBox, drBox,
                etatBox, surfaceBox, parcelleBox,
                messageLabel, saveBtn);

        root.getChildren().add(content);
        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/cards.css").toExternalForm());
        popup.setScene(scene);
        popup.show();
    }

    private boolean handleAddCulture(ComboBox<String> typeComboBox, ComboBox<String> nomComboBox,
                                     DatePicker datePlantationPicker, DatePicker dateRecoltePicker,
                                     ComboBox<String> etatComboBox, TextField surfaceField,
                                     ComboBox<String> parcelleComboBox, Label messageLabel) {
        try {
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
            c.setEtat(etat.trim());
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
}