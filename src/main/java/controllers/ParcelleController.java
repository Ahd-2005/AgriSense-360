package controllers;

import entity.Culture;
import entity.Parcelle;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import services.CultureService;
import services.ParcelleService;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ParcelleController {

    private final ParcelleService service = new ParcelleService();

    private final CultureService cultureService = new CultureService();

    @FXML private GridPane parcelleGrid;

    @FXML private TextField searchField;

    @FXML private Button statutButton;

    @FXML private Button surfaceButton;

    @FXML private Button resetButton;

    @FXML private Button carteBtn;

    private List<Parcelle> allParcelles = new ArrayList<>();

    private List<Parcelle> filteredParcelles = new ArrayList<>();

    // ── List of all 24 Tunisian governorates ──────────────────────────────────

    private static final List<String> GOUVERNORATS = Arrays.asList(

            "Ariana", "Béja", "Ben Arous", "Bizerte", "Gabès",

            "Gafsa", "Jendouba", "Kairouan", "Kasserine", "Kébili",

            "Le Kef", "Mahdia", "Manouba", "Médenine", "Monastir",

            "Nabeul", "Sfax", "Sidi Bouzid", "Siliana", "Sousse",

            "Tataouine", "Tozeur", "Tunis", "Zaghouan"

    );

    @FXML

    public void initialize() {

        if (parcelleGrid != null) {

            loadParcelleCards();

        }

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

        setActiveSortButton(null);

    }

    private void setActiveSortButton(Button activeButton) {

        if (statutButton != null)  statutButton.getStyleClass().remove("sort-button-active");

        if (surfaceButton != null) surfaceButton.getStyleClass().remove("sort-button-active");

        if (activeButton != null && !activeButton.getStyleClass().contains("sort-button-active")) {

            activeButton.getStyleClass().add("sort-button-active");

        }

    }

    private void displayParcelles(List<Parcelle> parcelles) {

        parcelleGrid.getChildren().clear();

        int col = 0, row = 0;

        for (Parcelle parcelle : parcelles) {

            VBox card = createParcelleCard(parcelle);

            parcelleGrid.add(card, col, row);

            col++;

            if (col == 3) { col = 0; row++; }

        }

    }

    // ══════════════════════════════════════════════════════════════════════════

    // CARD — with "📍 Carte" button that zooms directly to THIS parcelle

    // ══════════════════════════════════════════════════════════════════════════

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

        double remaining = 0;

        try { remaining = service.getRemainingParcelleSize(parcelle.getId()); }

        catch (SQLException e) { e.printStackTrace(); }

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

        Label typeSolBadge = new Label(parcelle.getTypeSol());

        typeSolBadge.setStyle(getTypeSolStyle(parcelle.getTypeSol()));

        infoGrid.add(typeSolLabel, 0, 3);

        infoGrid.add(typeSolBadge, 1, 3);

        Label statutLabel = new Label(parcelle.getStatut());

        if ("Libre".equalsIgnoreCase(parcelle.getStatut())) {

            statutLabel.getStyleClass().add("statut-libre");

        } else {

            statutLabel.getStyleClass().add("statut-occupee");

            statutLabel.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; " +

                    "-fx-padding: 5 10; -fx-background-radius: 12px; " +

                    "-fx-font-size: 12px; -fx-font-weight: bold;");

        }

        HBox buttonBox = new HBox(8);

        buttonBox.setAlignment(Pos.CENTER);

        Button editBtn = new Button("✏ Modifier");

        editBtn.setStyle("-fx-font-size: 13px;");

        editBtn.getStyleClass().addAll("card-button", "edit-button");

        editBtn.setOnAction(e -> {

            try {

                showUpdatePopup(parcelle);

            } catch (SQLException ex) {

                throw new RuntimeException(ex);

            }

        });

        Button deleteBtn = new Button("🗑 Supprimer");

        deleteBtn.setStyle("-fx-font-size: 13px;");

        deleteBtn.getStyleClass().addAll("card-button", "delete-button");

        deleteBtn.setOnAction(e -> handleDelete(parcelle));

        buttonBox.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(nameLabel, infoGrid, statutLabel, buttonBox);

        card.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {

                showCulturesInParcelle(parcelle);

            }

        });

        return card;

    }

    private String getTypeSolStyle(String typeSol) {

        String baseStyle = "-fx-padding: 5 10; -fx-background-radius: 12px; " +

                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;";

        switch (typeSol) {

            case "Sol Limoneux":    return baseStyle + " -fx-background-color: #8D6E63;";

            case "Sol Argileux":   return baseStyle + " -fx-background-color: #FF7043;";

            case "Sol Sablonneux": return baseStyle + " -fx-background-color: #FFB74D;";

            default:               return baseStyle + " -fx-background-color: #9E9E9E;";

        }

    }

    // ══════════════════════════════════════════════════════════════════════════

    // ADD POPUP — localisation replaced by ComboBox of 24 governorates

    // ══════════════════════════════════════════════════════════════════════════

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

        // Nom

        VBox nomBox = new VBox(5);

        Label nomLabel = new Label("Nom de la parcelle:");

        nomLabel.getStyleClass().add("form-label");

        TextField nomField = new TextField();

        nomField.setPromptText("Ex: Parcelle Nord");

        nomField.setPrefWidth(400);

        nomBox.getChildren().addAll(nomLabel, nomField);

        // Surface

        VBox surfaceBox = new VBox(5);

        Label surfaceLabel = new Label("Surface (m²):");

        surfaceLabel.getStyleClass().add("form-label");

        TextField surfaceField = new TextField();

        surfaceField.setPromptText("Ex: 500");

        surfaceField.setPrefWidth(400);

        surfaceBox.getChildren().addAll(surfaceLabel, surfaceField);

        // ── LOCALISATION — ComboBox with 24 governorates ──────────────────────

        VBox localisationBox = new VBox(5);

        Label localisationLabel = new Label("Gouvernorat (Localisation):");

        localisationLabel.getStyleClass().add("form-label");

        ComboBox<String> localisationCombo = new ComboBox<>();

        localisationCombo.setPromptText("Sélectionner le gouvernorat");

        localisationCombo.setPrefWidth(400);

        localisationCombo.getStyleClass().add("form-field");

        localisationCombo.getItems().addAll(GOUVERNORATS);

        localisationBox.getChildren().addAll(localisationLabel, localisationCombo);

        // Type de sol

        VBox typeSolBox = new VBox(5);

        Label typeSolLabel = new Label("Type de sol:");

        typeSolLabel.getStyleClass().add("form-label");

        ComboBox<String> typeSolComboBox = new ComboBox<>();

        typeSolComboBox.setPromptText("Sélectionner le type de sol");

        typeSolComboBox.setPrefWidth(400);

        typeSolComboBox.getStyleClass().add("form-field");

        typeSolComboBox.getItems().addAll("Sol Limoneux", "Sol Argileux", "Sol Sablonneux");

        typeSolBox.getChildren().addAll(typeSolLabel, typeSolComboBox);

        // Statut

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

            if (handleAddParcelle(nomField, surfaceField, localisationCombo,

                    typeSolComboBox, statutComboBox, messageLabel)) {

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

    // ══════════════════════════════════════════════════════════════════════════

    // UPDATE POPUP — localisation replaced by ComboBox of 24 governorates

    // ══════════════════════════════════════════════════════════════════════════

    private void showUpdatePopup(Parcelle parcelle) throws SQLException {

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

        // ID (read-only)

        VBox idBox = new VBox(5);

        Label idLabel = new Label("ID:");

        idLabel.getStyleClass().add("form-label");

        TextField idField = new TextField(String.valueOf(parcelle.getId()));

        idField.setEditable(false);

        idField.setDisable(true);

        idField.setStyle("-fx-opacity: 0.6;");

        idField.setPrefWidth(400);

        idBox.getChildren().addAll(idLabel, idField);

        // Nom

        VBox nomBox = new VBox(5);

        Label nomLabel = new Label("Nom de la parcelle:");

        nomLabel.getStyleClass().add("form-label");

        TextField nomField = new TextField(parcelle.getNom());

        nomField.setPrefWidth(400);

        nomBox.getChildren().addAll(nomLabel, nomField);

        // Surface

        VBox surfaceBox = new VBox(5);

        Label surfaceLabel = new Label("Surface (m²):");

        surfaceLabel.getStyleClass().add("form-label");

        TextField surfaceField = new TextField(String.valueOf(parcelle.getSurface()));

        surfaceField.setPrefWidth(400);

        // Check if this specific parcelle has cultures linked to it

        try {

            List<Culture> parcelCultures = cultureService.getAllCultures().stream()

                    .filter(c -> c.getParcelleId() == parcelle.getId())

                    .collect(Collectors.toList());

            if (!parcelCultures.isEmpty()) {

                // Has cultures linked → disable surface field

                surfaceField.setEditable(false);

                surfaceField.setDisable(true);

                surfaceField.setStyle("-fx-opacity: 0.6;");

                Label warningLabel = new Label("⚠️ Surface non modifiable - cultures affectées");

                warningLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 11px; -fx-font-style: italic;");

                surfaceBox.getChildren().addAll(surfaceLabel, surfaceField, warningLabel);

            } else {

                // No cultures linked → surface is freely editable

                surfaceBox.getChildren().addAll(surfaceLabel, surfaceField);

            }

        } catch (SQLException ex) {

            surfaceBox.getChildren().addAll(surfaceLabel, surfaceField);

        }

        // ── LOCALISATION — ComboBox pre-selected with current value ──────────

        VBox localisationBox = new VBox(5);

        Label localisationLabel = new Label("Gouvernorat (Localisation):");

        localisationLabel.getStyleClass().add("form-label");

        ComboBox<String> localisationCombo = new ComboBox<>();

        localisationCombo.setPrefWidth(400);

        localisationCombo.getStyleClass().add("form-field");

        localisationCombo.getItems().addAll(GOUVERNORATS);

        // Pre-select the current value if it matches a governorate

        String currentLoc = parcelle.getLocalisation();

        if (currentLoc != null && GOUVERNORATS.contains(currentLoc)) {

            localisationCombo.setValue(currentLoc);

        } else {

            localisationCombo.setPromptText("Sélectionner le gouvernorat");

        }

        localisationBox.getChildren().addAll(localisationLabel, localisationCombo);

        // Type de sol

        VBox typeSolBox = new VBox(5);

        Label typeSolLabel = new Label("Type de sol:");

        typeSolLabel.getStyleClass().add("form-label");

        ComboBox<String> typeSolComboBox = new ComboBox<>();

        typeSolComboBox.setPrefWidth(400);

        typeSolComboBox.getStyleClass().add("form-field");

        typeSolComboBox.getItems().addAll("Sol Limoneux", "Sol Argileux", "Sol Sablonneux");

        typeSolComboBox.setValue(parcelle.getTypeSol());

        typeSolBox.getChildren().addAll(typeSolLabel, typeSolComboBox);

        // Statut

        VBox statutBox = new VBox(5);

        Label statutLabel = new Label("Statut:");

        statutLabel.getStyleClass().add("form-label");

        ComboBox<String> statutComboBox = new ComboBox<>();

        statutComboBox.setPrefWidth(400);

        statutComboBox.getItems().addAll("Libre", "Occupée");

        // Auto-set statut based on surface_restant

        double surfaceRestant = service.getRemainingParcelleSize(parcelle.getId());

        if (surfaceRestant > 0) {

            // Surface restante > 0 → les deux options sont disponibles (Libre et Occupée)

            statutComboBox.getItems().setAll("Libre", "Occupée");

            statutComboBox.setValue(parcelle.getStatut());

        } else {

            // Surface = 0, can only be Occupée

            statutComboBox.getItems().setAll("Occupée");

            statutComboBox.setValue("Occupée");

            statutComboBox.setDisable(true);

            statutComboBox.setStyle("-fx-opacity: 0.7;");

        }

        statutBox.getChildren().addAll(statutLabel, statutComboBox);

        Label messageLabel = new Label();

        Button saveBtn = new Button("💾 Enregistrer");

        saveBtn.getStyleClass().addAll("card-button", "edit-button");

        saveBtn.setPrefWidth(150);

        saveBtn.setOnAction(e -> {

            if (handleUpdateParcelle(idField, nomField, surfaceField, localisationCombo,

                    typeSolComboBox, statutComboBox, messageLabel)) {

                popup.close();

                loadParcelleCards();

            }

        });

        HBox buttonContainer = new HBox(saveBtn);

        buttonContainer.setAlignment(Pos.CENTER);

        content.getChildren().addAll(header, idBox, nomBox, surfaceBox,

                localisationBox, typeSolBox, statutBox, messageLabel, buttonContainer);

        root.getChildren().add(content);

        Scene scene = new Scene(root);

        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        scene.getStylesheets().add(getClass().getResource("/css/cards.css").toExternalForm());

        popup.setScene(scene);

        popup.show();

    }

    // ══════════════════════════════════════════════════════════════════════════

    // handleAddParcelle — now receives ComboBox<String> for localisation

    // ══════════════════════════════════════════════════════════════════════════

    private boolean handleAddParcelle(TextField nomField, TextField surfaceField,

                                      ComboBox<String> localisationCombo,

                                      ComboBox<String> typeSolComboBox,

                                      ComboBox<String> statutComboBox, Label messageLabel) {

        try {

            String nom         = nomField.getText();

            String surfaceText = surfaceField.getText();

            String localisation = localisationCombo.getValue();

            String typeSol     = typeSolComboBox.getValue();

            String statut      = statutComboBox.getValue();

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

                if (surface <= 0) { showError(messageLabel, "❌ Surface doit être positive"); return false; }

            } catch (NumberFormatException e) {

                showError(messageLabel, "❌ Surface doit être un nombre");

                return false;

            }

            if (localisation == null || localisation.trim().isEmpty()) {

                showError(messageLabel, "❌ Veuillez sélectionner un gouvernorat");

                return false;

            }

            if (typeSol == null || typeSol.trim().isEmpty()) {

                showError(messageLabel, "❌ Veuillez sélectionner le type de sol");

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

    // ══════════════════════════════════════════════════════════════════════════

    // handleUpdateParcelle — now receives ComboBox<String> for localisation

    // ══════════════════════════════════════════════════════════════════════════

    private boolean handleUpdateParcelle(TextField idField, TextField nomField, TextField surfaceField,

                                         ComboBox<String> localisationCombo,

                                         ComboBox<String> typeSolComboBox,

                                         ComboBox<String> statutComboBox, Label messageLabel) {

        try {

            int    id          = Integer.parseInt(idField.getText());

            String nom         = nomField.getText();

            String surfaceText = surfaceField.getText();

            String localisation = localisationCombo.getValue();

            String typeSol     = typeSolComboBox.getValue();

            String statut      = statutComboBox.getValue();

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

                if (surface <= 0) { showError(messageLabel, "❌ Surface doit être positive"); return false; }

            } catch (NumberFormatException e) {

                showError(messageLabel, "❌ Surface doit être un nombre");

                return false;

            }

            // ── Statut validation ─────────────────────────────────────────────

            // On bloque "Libre" UNIQUEMENT si la surface restante est <= 0

            // (toute la parcelle est occupée par des cultures).

            // Si surface restante > 0, on autorise "Libre" même s'il y a des cultures.

            try {

                double remainingSurface = service.getRemainingParcelleSize(id);

                if ("Libre".equalsIgnoreCase(statut) && remainingSurface <= 0) {

                    showError(messageLabel, "❌ Impossible! Surface complètement occupée (0 m² restant)");

                    return false;

                }

            } catch (SQLException e) {

                showError(messageLabel, "❌ Erreur de vérification de la surface");

                return false;

            }

            // ─────────────────────────────────────────────────────────────────

            if (localisation == null || localisation.trim().isEmpty()) {

                showError(messageLabel, "❌ Veuillez sélectionner un gouvernorat");

                return false;

            }

            if (typeSol == null || typeSol.trim().isEmpty()) {

                showError(messageLabel, "❌ Veuillez sélectionner le type de sol");

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

        MainLayoutController controller = MainLayoutController.getInstance();

        if (controller != null) {

            controller.navigateToCulture();

        }

    }

    /**

     * Ouvre la carte de toutes les parcelles (sans zoomer sur une specifique).

     * Lie au bouton 🗺️ Carte dans la barre du haut.

     */

    @FXML

    private void showAllParcelleCarte() {

        showTunisiaMap(null);

    }

    // ══════════════════════════════════════════════════════════════════════════

    // SHOW CULTURES POPUP — Beautiful 2D graphical land map

    // ══════════════════════════════════════════════════════════════════════════

    // Soft, eye-friendly pastel culture colors — consistent index across ALL parcelles

    private static final Color[] CULTURE_COLORS = {

            Color.web("#7EC8A4"), // sage green

            Color.web("#7EB8D4"), // sky blue

            Color.web("#F0A868"), // warm peach

            Color.web("#C49AC8"), // soft lavender

            Color.web("#F0D068"), // warm honey

            Color.web("#88C8B0"), // mint teal

            Color.web("#E89898"), // dusty rose

            Color.web("#90B8E0"), // periwinkle

            Color.web("#A8C870"), // olive green

            Color.web("#D4A878")  // warm tan

    };

    // Paired darker accent for borders/labels on each color

    private static final Color[] CULTURE_DARK = {

            Color.web("#2E7D5A"), Color.web("#1565A0"), Color.web("#B85C10"),

            Color.web("#6A3080"), Color.web("#8B6E00"), Color.web("#1B6E58"),

            Color.web("#A02828"), Color.web("#2244A0"), Color.web("#4A6818"),

            Color.web("#704018")

    };

    private void showCulturesInParcelle(Parcelle parcelle) {

        Stage popup = new Stage();

        popup.initModality(Modality.APPLICATION_MODAL);

        popup.setTitle("🌾 Visualisation — " + parcelle.getNom());

        // ── Load cultures ──────────────────────────────────────────────────

        List<Culture> parcelleCultures = new ArrayList<>();

        double remaining = 0;

        try {

            parcelleCultures = cultureService.getAllCultures().stream()

                    .filter(c -> c.getParcelleId() == parcelle.getId())

                    .collect(Collectors.toList());

            remaining = service.getRemainingParcelleSize(parcelle.getId());

        } catch (SQLException e) { e.printStackTrace(); }

        final double rem = remaining;

        final double totalSurface = parcelle.getSurface();

        final double usageRatio = totalSurface > 0 ? (totalSurface - rem) / totalSurface : 0;

        // ── Soil-based theme ───────────────────────────────────────────────

        // Sol Sablonneux → warm golden yellows

        // Sol Argileux   → warm chocolate browns

        // Sol Limoneux   → muted olive greens (default)

        String typeSol = parcelle.getTypeSol() != null ? parcelle.getTypeSol() : "";

        String headerGrad, bgPage, canvasBg, soilFill, soilBorder, soilFree, legendBg, legendCard;

        switch (typeSol) {

            case "Sol Sablonneux":

                headerGrad  = "linear-gradient(to right, #c8960c, #e8b830)";

                bgPage      = "#FFF8E7";

                canvasBg    = "#FFF3D0";

                soilFill    = "#F5DFA0";  // sandy yellow base

                soilBorder  = "#C8960C";

                soilFree    = "#FAE8A0";

                legendBg    = "#FFFBF0";

                legendCard  = "#FFF3CC";

                break;

            case "Sol Argileux":

                headerGrad  = "linear-gradient(to right, #6D3A1F, #9B5A35)";

                bgPage      = "#FDF5F0";

                canvasBg    = "#F5E8DC";

                soilFill    = "#D4A882";  // clay brown base

                soilBorder  = "#7A3F20";

                soilFree    = "#E8D0B8";

                legendBg    = "#FDF8F5";

                legendCard  = "#F5E8DC";

                break;

            default: // Sol Limoneux

                headerGrad  = "linear-gradient(to right, #3A6B35, #5A9050)";

                bgPage      = "#F4FAF0";

                canvasBg    = "#E8F4E0";

                soilFill    = "#B8D8A0";  // loamy green base

                soilBorder  = "#3A6B35";

                soilFree    = "#D0EBBC";

                legendBg    = "#F7FCF4";

                legendCard  = "#E8F4E0";

                break;

        }

        // ── Root layout ────────────────────────────────────────────────────

        BorderPane root = new BorderPane();

        root.setStyle("-fx-background-color: " + bgPage + ";");

        // ── TOP HEADER ─────────────────────────────────────────────────────

        HBox headerWrapper = new HBox();

        headerWrapper.setAlignment(Pos.CENTER_LEFT);

        headerWrapper.setStyle("-fx-background-color: " + headerGrad + "; -fx-padding: 18 28; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 8, 0, 0, 3);");

        VBox headerText = new VBox(5);

        Label titleLbl = new Label("🌿 " + parcelle.getNom() + " — Carte des Cultures");

        titleLbl.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox statsRow = new HBox(28);

        statsRow.setAlignment(Pos.CENTER_LEFT);

        Label totalLbl = new Label("📏 Total: " + String.format("%.0f m²", totalSurface));

        Label usedLbl  = new Label("🌾 Utilisé: " + String.format("%.0f m²", totalSurface - rem));

        Label freeLbl  = new Label("✅ Libre: " + String.format("%.0f m²", rem));

        totalLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 12px; -fx-font-weight: bold;");

        usedLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 12px; -fx-font-weight: bold;");

        freeLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 12px; -fx-font-weight: bold;");

        statsRow.getChildren().addAll(totalLbl, usedLbl, freeLbl);

        headerText.getChildren().addAll(titleLbl, statsRow);

        Region hSpacer = new Region(); HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕ Fermer");

        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.25); -fx-text-fill: white; " +

                "-fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand; " +

                "-fx-font-size: 13px; -fx-padding: 8 20; -fx-border-color: rgba(255,255,255,0.4); " +

                "-fx-border-radius: 20;");

        closeBtn.setOnAction(e -> popup.close());

        headerWrapper.getChildren().addAll(headerText, hSpacer, closeBtn);

        root.setTop(headerWrapper);

        // ── CENTER ─────────────────────────────────────────────────────────

        HBox center = new HBox(20);

        center.setPadding(new Insets(22));

        center.setStyle("-fx-background-color: " + bgPage + ";");

        // Canvas dimensions

        final double CANVAS_W = 640, CANVAS_H = 490;

        final double M = 18; // margin

        final double mapW = CANVAS_W - 2 * M, mapH = CANVAS_H - 2 * M;

        Canvas canvas = new Canvas(CANVAS_W, CANVAS_H);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        // ── Outer glow / shadow ────────────────────────────────────────────

        // Draw soft background page shadow

        gc.setFill(Color.color(0, 0, 0, 0.06));

        gc.fillRoundRect(M + 4, M + 4, mapW, mapH, 14, 14);

        // Soil base fill

        Color soilBase = Color.web(soilFill);

        Color soilLight = soilBase.deriveColor(0, 0.6, 1.2, 1);

        LinearGradient soilGrad = new LinearGradient(M, M, M, M + mapH, false, CycleMethod.NO_CYCLE,

                new Stop(0, soilLight), new Stop(1, soilBase));

        gc.setFill(soilGrad);

        gc.fillRoundRect(M, M, mapW, mapH, 12, 12);

        // Soil border

        gc.setStroke(Color.web(soilBorder));

        gc.setLineWidth(2);

        gc.strokeRoundRect(M, M, mapW, mapH, 12, 12);

        // ── Draw cultures ──────────────────────────────────────────────────

        List<double[]> rects = new ArrayList<>();

        if (!parcelleCultures.isEmpty()) {

            double curY = M + 6;

            for (int ci = 0; ci < parcelleCultures.size(); ci++) {

                Culture c = parcelleCultures.get(ci);

                double ratio = c.getSurface() / totalSurface;

                double rH = (mapH - 12) * ratio;

                double rW = mapW - 12;

                double rX = M + 6;

                Color col  = CULTURE_COLORS[ci % CULTURE_COLORS.length];

                Color dark = CULTURE_DARK[ci % CULTURE_DARK.length];

                Color lite = col.deriveColor(0, 0.7, 1.18, 1);

                // Subtle drop shadow

                gc.setFill(Color.color(0, 0, 0, 0.10));

                gc.fillRoundRect(rX + 2, curY + 3, rW, rH, 10, 10);

                // Gradient fill (light top → base color bottom)

                LinearGradient cGrad = new LinearGradient(rX, curY, rX, curY + rH, false, CycleMethod.NO_CYCLE,

                        new Stop(0, lite), new Stop(1, col));

                gc.setFill(cGrad);

                gc.fillRoundRect(rX, curY, rW, rH, 10, 10);

                // Subtle inner highlight at top

                gc.setFill(Color.color(1, 1, 1, 0.28));

                gc.fillRoundRect(rX + 2, curY + 2, rW - 4, Math.min(rH * 0.35, 18), 8, 8);

                // Border

                gc.setStroke(dark.deriveColor(0, 1, 1, 0.5));

                gc.setLineWidth(1.5);

                gc.strokeRoundRect(rX, curY, rW, rH, 10, 10);

                // Subtle dot-grid texture

                gc.setFill(Color.color(1, 1, 1, 0.18));

                for (double px = rX + 12; px < rX + rW - 8; px += 20) {

                    for (double py = curY + 12; py < curY + rH - 8; py += 20) {

                        gc.fillOval(px, py, 4, 4);

                    }

                }

                // Culture name label (dark text for readability on light bg)

                if (rH > 20) {

                    double fontSize = Math.min(14, Math.max(10, rH * 0.28));

                    gc.setFont(Font.font("System", FontWeight.BOLD, fontSize));

                    // Text shadow

                    gc.setFill(Color.color(0, 0, 0, 0.2));

                    gc.fillText(c.getNom() + "   " + String.format("%.0f m²", c.getSurface()), rX + 13, curY + Math.min(22, rH * 0.55) + 1);

                    gc.setFill(dark);

                    gc.fillText(c.getNom() + "   " + String.format("%.0f m²", c.getSurface()), rX + 12, curY + Math.min(22, rH * 0.55));

                }

                if (rH > 42) {

                    gc.setFont(Font.font("System", FontWeight.NORMAL, Math.min(11, rH * 0.22)));

                    gc.setFill(dark.deriveColor(0, 1, 1.2, 0.75));

                    gc.fillText("  " + c.getDatePlantation() + "  →  " + c.getDateRecolte(), rX + 12, curY + Math.min(40, rH * 0.80));

                }

                rects.add(new double[]{rX, curY, rW, rH, ci});

                curY += rH;

            }

            // Free zone

            if (rem > 0) {

                double curY2 = rects.isEmpty() ? M + 6 : rects.get(rects.size()-1)[1] + rects.get(rects.size()-1)[3];

                double freeH = (M + mapH - 6) - curY2;

                if (freeH > 4) {

                    Color freeCol = Color.web(soilFree);

                    gc.setFill(freeCol.deriveColor(0, 0.5, 1.1, 0.7));

                    gc.fillRoundRect(M + 6, curY2, mapW - 12, freeH, 8, 8);

                    gc.setStroke(Color.web(soilBorder).deriveColor(0, 0.5, 1.4, 0.5));

                    gc.setLineWidth(1);

                    gc.strokeRoundRect(M + 6, curY2, mapW - 12, freeH, 8, 8);

                    if (freeH > 16) {

                        gc.setFont(Font.font("System", FontWeight.BOLD, 12));

                        gc.setFill(Color.web(soilBorder));

                        gc.fillText("✅  Surface libre : " + String.format("%.0f m²", rem), M + 16, curY2 + Math.min(20, freeH * 0.65));

                    }

                }

            }

        } else {

            // No cultures at all

            gc.setFont(Font.font("System", FontWeight.BOLD, 16));

            gc.setFill(Color.web(soilBorder).deriveColor(0,1,1,0.6));

            gc.fillText("Aucune culture affectée à cette parcelle", M + 40, M + mapH / 2);

        }

        // ── HOVER TOOLTIP ─────────────────────────────────────────────────

        VBox tooltip = new VBox(4);

        tooltip.setStyle("-fx-background-color: rgba(255,255,255,0.97); " +

                "-fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 10; " +

                "-fx-background-radius: 10; -fx-padding: 12 16; " +

                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0, 0, 4);");

        tooltip.setVisible(false);

        tooltip.setMouseTransparent(true);

        tooltip.setMaxWidth(210);

        Label ttTitle = new Label(); ttTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #222;");

        Label ttType  = new Label(); ttType.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        Label ttSurf  = new Label(); ttSurf.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        Label ttEtat  = new Label(); ttEtat.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        Label ttDates = new Label(); ttDates.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

        tooltip.getChildren().addAll(ttTitle, ttType, ttSurf, ttEtat, ttDates);

        final List<Culture> finalCultures = parcelleCultures;

        canvas.setOnMouseMoved(ev -> {

            boolean found = false;

            for (int i = 0; i < rects.size(); i++) {

                double[] r = rects.get(i);

                if (ev.getX() >= r[0] && ev.getX() <= r[0]+r[2] && ev.getY() >= r[1] && ev.getY() <= r[1]+r[3]) {

                    Culture c = finalCultures.get(i);

                    ttTitle.setText("🌱 " + c.getNom());

                    ttType.setText("Type : " + c.getTypeCulture());

                    ttSurf.setText("Surface : " + String.format("%.1f m²  (%.1f%%)", c.getSurface(), (c.getSurface()/totalSurface)*100));

                    ttEtat.setText("État : " + c.getEtat());

                    ttDates.setText("🗓  " + c.getDatePlantation() + "  →  " + c.getDateRecolte());

                    tooltip.setVisible(true);

                    found = true;

                    break;

                }

            }

            if (!found) tooltip.setVisible(false);

        });

        canvas.setOnMouseExited(e -> tooltip.setVisible(false));

        StackPane canvasPane = new StackPane(canvas, tooltip);

        StackPane.setAlignment(tooltip, Pos.TOP_RIGHT);

        canvasPane.setStyle("-fx-background-color: " + canvasBg + "; -fx-background-radius: 14; -fx-padding: 4;");

        DropShadow dsShadow = new DropShadow(16, Color.color(0,0,0,0.12));

        canvasPane.setEffect(dsShadow);

        // ── LEGEND PANEL ───────────────────────────────────────────────────

        ScrollPane legendScroll = new ScrollPane();

        legendScroll.setFitToWidth(true);

        legendScroll.setPrefWidth(240);

        legendScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox legend = new VBox(10);

        legend.setPrefWidth(230);

        legend.setPadding(new Insets(18));

        legend.setStyle("-fx-background-color: " + legendBg + "; -fx-background-radius: 14; " +

                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 2);");

        Label legendTitle = new Label("📋  Légende");

        legendTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #333;");

        legend.getChildren().add(legendTitle);

        // ── Usage progress bar ─────────────────────────────────────────────

        Label usageLbl = new Label("Taux d'occupation");

        usageLbl.setStyle("-fx-text-fill: #777; -fx-font-size: 11px; -fx-padding: 6 0 2 0;");

        StackPane progressBg = new StackPane();

        Rectangle bgBar = new Rectangle(196, 12);

        bgBar.setFill(Color.web("#E0E0E0"));

        bgBar.setArcWidth(8); bgBar.setArcHeight(8);

        double barW = 196 * Math.min(usageRatio, 1.0);

        Rectangle fillBar = new Rectangle(barW < 1 ? 1 : barW, 12);

        Color barColor = usageRatio >= 1.0 ? Color.web("#E53935")

                : usageRatio > 0.75 ? Color.web("#FB8C00")

                : Color.web("#43A047");

        fillBar.setFill(barColor);

        fillBar.setArcWidth(8); fillBar.setArcHeight(8);

        StackPane.setAlignment(fillBar, Pos.CENTER_LEFT);

        progressBg.getChildren().addAll(bgBar, fillBar);

        Label pctLbl = new Label(String.format("%.0f%%  utilisé", usageRatio * 100));

        pctLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #444;");

        legend.getChildren().addAll(usageLbl, progressBg, pctLbl);

        // Thin separator

        Region sep1 = new Region(); sep1.setPrefHeight(1);

        sep1.setStyle("-fx-background-color: #DDD; -fx-margin: 2 0;");

        legend.getChildren().add(sep1);

        // Cultures list

        Label cultTitle = new Label("Cultures  (" + parcelleCultures.size() + ")");

        cultTitle.setStyle("-fx-text-fill: #888; -fx-font-size: 11px; -fx-padding: 4 0 0 0;");

        legend.getChildren().add(cultTitle);

        for (int i = 0; i < parcelleCultures.size(); i++) {

            Culture c = parcelleCultures.get(i);

            Color col  = CULTURE_COLORS[i % CULTURE_COLORS.length];

            Color dark = CULTURE_DARK[i % CULTURE_DARK.length];

            HBox entry = new HBox(10);

            entry.setAlignment(Pos.CENTER_LEFT);

            entry.setPadding(new Insets(6, 10, 6, 10));

            entry.setStyle("-fx-background-color: " + legendCard + "; -fx-background-radius: 8;");

            Rectangle swatch = new Rectangle(14, 14);

            swatch.setFill(col);

            swatch.setArcWidth(4); swatch.setArcHeight(4);

            swatch.setStroke(dark.deriveColor(0,1,1,0.4));

            swatch.setStrokeWidth(1);

            VBox txt = new VBox(1);

            Label cn = new Label(c.getNom());

            cn.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2a2a2a;");

            Label cs = new Label(String.format("%.0f m²  (%.1f%%)", c.getSurface(), (c.getSurface()/totalSurface)*100));

            cs.setStyle("-fx-font-size: 10px; -fx-text-fill: #777;");

            txt.getChildren().addAll(cn, cs);

            entry.getChildren().addAll(swatch, txt);

            legend.getChildren().add(entry);

        }

        // Free zone entry

        if (rem > 0) {

            HBox freeEntry = new HBox(10);

            freeEntry.setAlignment(Pos.CENTER_LEFT);

            freeEntry.setPadding(new Insets(6, 10, 6, 10));

            freeEntry.setStyle("-fx-background-color: " + legendCard + "; -fx-background-radius: 8;");

            Rectangle fb = new Rectangle(14, 14);

            fb.setFill(Color.web(soilFree));

            fb.setArcWidth(4); fb.setArcHeight(4);

            fb.setStroke(Color.web(soilBorder).deriveColor(0,0.5,1.4,0.4));

            fb.setStrokeWidth(1);

            Label fl = new Label(String.format("Libre :  %.0f m²", rem));

            fl.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a4a4a;");

            freeEntry.getChildren().addAll(fb, fl);

            legend.getChildren().add(freeEntry);

        }

        // Separator + soil info

        Region sep2 = new Region(); sep2.setPrefHeight(1);

        sep2.setStyle("-fx-background-color: #DDD;");

        legend.getChildren().add(sep2);

        Label soilInfoLbl = new Label("🪨  " + (typeSol.isEmpty() ? "Sol inconnu" : typeSol));

        soilInfoLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        Label locInfoLbl = new Label("📍  " + (parcelle.getLocalisation() != null ? parcelle.getLocalisation() : "N/A"));

        locInfoLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        legend.getChildren().addAll(soilInfoLbl, locInfoLbl);

        legendScroll.setContent(legend);

        center.getChildren().addAll(canvasPane, legendScroll);

        HBox.setHgrow(canvasPane, Priority.ALWAYS);

        root.setCenter(center);

        // ── BOTTOM HINT ────────────────────────────────────────────────────

        HBox bottom = new HBox();

        bottom.setAlignment(Pos.CENTER);

        bottom.setPadding(new Insets(10));

        bottom.setStyle("-fx-background-color: " + legendCard + "; -fx-border-color: #DDD; -fx-border-width: 1 0 0 0;");

        Label hint = new Label("💡  Survolez les zones colorées pour voir les détails de chaque culture");

        hint.setStyle("-fx-text-fill: #999; -fx-font-size: 12px;");

        bottom.getChildren().add(hint);

        root.setBottom(bottom);

        Scene scene = new Scene(root, 990, 660);

        scene.getStylesheets().add(getClass().getResource("/css/cards.css").toExternalForm());

        popup.setScene(scene);

        popup.show();

    }

    private VBox createCultureCardForPopup(Culture culture) {

        VBox card = new VBox(10);

        card.getStyleClass().add("culture-card");

        card.setPrefWidth(260);

        card.setMaxWidth(260);

        card.setPrefHeight(300);

        card.setAlignment(Pos.TOP_CENTER);

        card.setPadding(new Insets(10));

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

                try { imageView.setImage(new Image(getClass().getResourceAsStream("/images/cultures/default.png"))); }

                catch (Exception ex) { imageView.setImage(null); }

            }

        }

        Label typeLabel = new Label(culture.getTypeCulture());

        typeLabel.getStyleClass().add("culture-type");

        typeLabel.setWrapText(true);

        typeLabel.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(culture.getNom());

        nameLabel.getStyleClass().add("culture-name");

        nameLabel.setWrapText(true);

        nameLabel.setAlignment(Pos.CENTER);

        Label etatLabel = new Label(culture.getEtat());

        String etatClass;

        String etat = culture.getEtat().toLowerCase();

        if (etat.contains("récolte prévue") || etat.contains("recolte prevue")) {

            etatClass = "etat-recolte-prevue";

        } else if (etat.contains("récolte en retard") || etat.contains("recolte en retard")) {

            etatClass = "etat-recolte-en-retard";

        } else {

            etatClass = "etat-" + etat.replace("é","e").replace("è","e").replace("à","a");

        }

        etatLabel.getStyleClass().addAll("culture-etat", etatClass);

        etatLabel.setWrapText(true);

        etatLabel.setMaxWidth(200);

        Label surfaceLabel = new Label("📏 " + culture.getSurface() + " m²");

        surfaceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666; -fx-font-weight: bold;");

        Label datesLabel = new Label("📅 " + culture.getDatePlantation() + " → " + culture.getDateRecolte());

        datesLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

        datesLabel.setWrapText(true);

        datesLabel.setAlignment(Pos.CENTER);

        card.getChildren().addAll(imageView, typeLabel, nameLabel, etatLabel, surfaceLabel, datesLabel);

        card.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {

                showCultureDetailFromParcelle(culture);

            }

        });

        return card;

    }

    private void showCultureDetailFromParcelle(Culture culture) {

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/culture_detail.fxml"));

            if (loader.getLocation() == null) {

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

        } catch (Exception e) { e.printStackTrace(); }

    }

    // ══════════════════════════════════════════════════════════════════════════

    // SHOW TUNISIA MAP — opens and zooms directly to the parcelle's governorate

    // ══════════════════════════════════════════════════════════════════════════

    private void showTunisiaMap(Parcelle targetParcelle) {

        Stage mapStage = new Stage();

        mapStage.initModality(Modality.APPLICATION_MODAL);

        mapStage.setTitle("🗺️ Carte des Parcelles — Tunisie");

        WebView webView = new WebView();

        WebEngine engine = webView.getEngine();

        java.net.URL mapUrl = getClass().getResource("/map/map.html");

        if (mapUrl == null) {

            Alert alert = new Alert(Alert.AlertType.ERROR);

            alert.setTitle("Erreur");

            alert.setHeaderText("Fichier carte introuvable");

            alert.setContentText("Assurez-vous que map/map.html est dans src/main/resources/map/");

            alert.showAndWait();

            return;

        }

        engine.load(mapUrl.toExternalForm());

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {

            if (newState == Worker.State.SUCCEEDED) {

                try {

                    // 1) Send all parcelles to JS

                    StringBuilder json = new StringBuilder("[");

                    boolean first = true;

                    for (Parcelle p : allParcelles) {

                        if (!first) json.append(",");

                        first = false;

                        json.append("{")

                                .append("\"nom\":\"").append(escapeJson(p.getNom())).append("\",")

                                .append("\"localisation\":\"").append(escapeJson(p.getLocalisation() != null ? p.getLocalisation() : "")).append("\",")

                                .append("\"statut\":\"").append(escapeJson(p.getStatut() != null ? p.getStatut() : "")).append("\",")

                                .append("\"typeSol\":\"").append(escapeJson(p.getTypeSol() != null ? p.getTypeSol() : "")).append("\",")

                                .append("\"surface\":").append(p.getSurface())

                                .append("}");

                    }

                    json.append("]");

                    engine.executeScript("setParcelles('" + json.toString().replace("'", "\\'") + "')");

                    // 2) If a specific parcelle was clicked, zoom to its governorate

                    if (targetParcelle != null && targetParcelle.getLocalisation() != null

                            && !targetParcelle.getLocalisation().isEmpty()) {

                        String govName     = escapeJson(targetParcelle.getLocalisation());

                        String parcelleName = escapeJson(targetParcelle.getNom());

                        engine.executeScript("focusOnParcelle('" + govName + "', '" + parcelleName + "')");

                    }

                } catch (Exception ex) {

                    ex.printStackTrace();

                }

            }

        });

        StackPane root = new StackPane(webView);

        Scene scene = new Scene(root, 1150, 700);

        mapStage.setScene(scene);

        mapStage.show();

    }

    private String escapeJson(String s) {

        if (s == null) return "";

        return s.replace("\\", "\\\\")

                .replace("\"", "\\\"")

                .replace("'", "\\'")

                .replace("\n", "\\n")

                .replace("\r", "")

                .replace("\t", " ");

    }

}