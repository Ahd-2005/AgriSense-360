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
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.concurrent.Worker;

import java.io.IOException;
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

        // ── Map button — opens Tunisia map zoomed to THIS parcelle's governorate ──
        Button mapBtn = new Button("📍 Carte");
        mapBtn.setStyle("-fx-font-size: 13px;");
        mapBtn.getStyleClass().addAll("card-button", "map-button");
        mapBtn.setOnAction(e -> showTunisiaMap(parcelle));  // pass the parcelle directly

        buttonBox.getChildren().addAll(editBtn, deleteBtn, mapBtn);
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

    // ══════════════════════════════════════════════════════════════════════════
    // SHOW CULTURES POPUP
    // ══════════════════════════════════════════════════════════════════════════
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

        VBox parcelleInfo = new VBox(8);
        parcelleInfo.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 12; -fx-background-radius: 8;");

        try {
            double remaining = service.getRemainingParcelleSize(parcelle.getId());
            Label surfaceLabel = new Label("📏 Surface totale: " + parcelle.getSurface() + " m²");
            Label restantLabel = new Label("📦 Surface restante: " + String.format("%.2f", remaining) + " m²");
            Label usedLabel    = new Label("🌾 Surface utilisée: " + String.format("%.2f", (parcelle.getSurface() - remaining)) + " m²");
            surfaceLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
            restantLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            usedLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #FF9800; -fx-font-weight: bold;");
            parcelleInfo.getChildren().addAll(surfaceLabel, usedLabel, restantLabel);
        } catch (SQLException e) { e.printStackTrace(); }

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
                int col = 0, row = 0;
                for (Culture culture : parcelleCultures) {
                    VBox cultureCard = createCultureCardForPopup(culture);
                    cultureGrid.add(cultureCard, col, row);
                    col++;
                    if (col == 3) { col = 0; row++; }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        scrollPane.setContent(cultureGrid);
        content.getChildren().addAll(header, parcelleInfo, scrollPane);

        root.getChildren().add(content);
        root.setOnMouseClicked(e -> { if (e.getTarget() == root) popup.close(); });

        Scene scene = new Scene(root, 950, 650);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
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