package controllers;

import entity.farm;
import entity.user;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.SessionManager;
import services.farmservice;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Owner dashboard: shows the owner's farms + button to add a new farm.
 * Loaded into the MainLayout contentArea (no separate Stage needed).
 */
public class OwnerFarmController {

    // ── Farm list ─────────────────────────────────────────────────────────
    @FXML private GridPane farmsGrid;
    @FXML private Label    ownerNameLabel;
    @FXML private Label    farmCountLabel;
    @FXML private Label    emptyLabel;

    // ── Add-farm form ────────────────────────────────────────────────────
    @FXML private VBox     addFarmPane;
    @FXML private TextField nameField;
    @FXML private TextField locationField;
    @FXML private TextField surfaceField;
    @FXML private TextArea  descArea;
    @FXML private Label    imageFileLabel;
    @FXML private Label    formError;

    private user    currentOwner;
    private farmservice  svc;
    private File    selectedImageFile;
    private static final String IMG_UPLOAD_DIR = "uploads/farms/";

    // ─────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        try {
            svc = new farmservice();
            currentOwner = SessionManager.getInstance().getCurrentUser();
            if (currentOwner != null) {
                ownerNameLabel.setText("Bonjour " + currentOwner.getName() + " 👋");
                loadFarms();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (addFarmPane != null) {
            addFarmPane.setVisible(false);
            addFarmPane.setManaged(false);
        }
    }

    // ── Load & render the owner's farms ───────────────────────────────────
    private void loadFarms() {
        farmsGrid.getChildren().clear();
        try {
            List<farm> farms = svc.getFarmsByOwner(currentOwner.getId());
            farmCountLabel.setText(farms.size() + " ferme" + (farms.size() > 1 ? "s" : ""));

            if (farms.isEmpty()) {
                emptyLabel.setVisible(true);
                emptyLabel.setManaged(true);
                return;
            }
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);

            int col = 0, row = 0;
            for (farm f : farms) {
                farmsGrid.add(buildFarmCard(f), col, row);
                col++;
                if (col == 3) { col = 0; row++; }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox buildFarmCard(farm f) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(310);
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #c8e6c9;" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1.5;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        );

        // Image
        ImageView iv = new ImageView();
        iv.setFitWidth(280); iv.setFitHeight(140);
        iv.setPreserveRatio(false);
        if (f.getImage() != null && !f.getImage().isEmpty()) {
            try { iv.setImage(new Image(f.getImage(), true)); } catch (Exception ignored) {}
        }

        Label name = new Label(f.getName());
        name.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
        name.setWrapText(true);

        Label loc = new Label("📍 " + (f.getLocation() != null ? f.getLocation() : "—"));
        loc.setStyle("-fx-font-size: 13px; -fx-text-fill: #546e7a;");

        Label surf = new Label("📐 " + f.getSurface() + " ha");
        surf.setStyle("-fx-font-size: 13px; -fx-text-fill: #546e7a;");

        card.getChildren().addAll(iv, name, loc, surf);

        if (f.getDescription() != null && !f.getDescription().isEmpty()) {
            Label desc = new Label(f.getDescription());
            desc.setWrapText(true); desc.setMaxWidth(280);
            desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");
            card.getChildren().add(desc);
        }

        // "Voir les candidatures" button
        Button appBtn = new Button("📋 Candidatures");
        appBtn.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white;" +
                "-fx-font-size: 12px; -fx-font-weight: bold;" +
                "-fx-background-radius: 20; -fx-cursor: hand;" +
                "-fx-pref-width: 280; -fx-pref-height: 34;"
        );
        appBtn.setOnAction(e -> openCandidatures());
        card.getChildren().add(appBtn);

        return card;
    }

    // ── Toggle add-farm panel ─────────────────────────────────────────────
    @FXML
    private void handleShowAddForm() {
        boolean show = !addFarmPane.isVisible();
        addFarmPane.setVisible(show);
        addFarmPane.setManaged(show);
        clearForm();
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        Stage stage = (Stage) nameField.getScene().getWindow();
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            selectedImageFile = f;
            imageFileLabel.setText("🖼 " + f.getName());
            imageFileLabel.setStyle("-fx-text-fill: #2e7d32;");
        }
    }

    @FXML
    private void handleSaveFarm() {
        formError.setVisible(false);

        String name = nameField.getText().trim();
        String loc  = locationField.getText().trim();
        String surfTxt = surfaceField.getText().trim();

        if (name.isEmpty() || loc.isEmpty() || surfTxt.isEmpty()) {
            formError.setText("⚠ Nom, localisation et superficie sont obligatoires.");
            formError.setVisible(true);
            return;
        }

        double surface;
        try { surface = Double.parseDouble(surfTxt); }
        catch (NumberFormatException ex) {
            formError.setText("⚠ La superficie doit être un nombre.");
            formError.setVisible(true);
            return;
        }

        try {
            String imgPath = null;
            if (selectedImageFile != null) {
                imgPath = saveImage(selectedImageFile);
            }

            farm newFarm = new farm();
            newFarm.setFarmId("FARM_" + System.currentTimeMillis());
            newFarm.setName(name);
            newFarm.setLocation(loc);
            newFarm.setSurface(surface);
            newFarm.setDescription(descArea.getText().trim());
            newFarm.setImage(imgPath);
            newFarm.setOwnerId(currentOwner.getId());

            svc.addFarm(newFarm);

            addFarmPane.setVisible(false);
            addFarmPane.setManaged(false);
            clearForm();
            loadFarms();

            showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Ferme ajoutée avec succès !");

        } catch (Exception e) {
            e.printStackTrace();
            formError.setText("❌ Erreur : " + e.getMessage());
            formError.setVisible(true);
        }
    }

    @FXML
    private void handleCancelAdd() {
        addFarmPane.setVisible(false);
        addFarmPane.setManaged(false);
        clearForm();
    }

    private String saveImage(File file) throws IOException {
        Path dir = Paths.get(IMG_UPLOAD_DIR);
        Files.createDirectories(dir);
        String ext = file.getName().substring(file.getName().lastIndexOf('.'));
        String newName = "farm_" + currentOwner.getId() + "_" + System.currentTimeMillis() + ext;
        Path dest = dir.resolve(newName);
        Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toUri().toString();   // file:// URI so ImageView can load it
    }

    private void clearForm() {
        if (nameField != null) nameField.clear();
        if (locationField != null) locationField.clear();
        if (surfaceField != null) surfaceField.clear();
        if (descArea != null) descArea.clear();
        if (imageFileLabel != null) imageFileLabel.setText("Aucun fichier choisi");
        selectedImageFile = null;
        if (formError != null) formError.setVisible(false);
    }

    // ── Navigate to pending applications ─────────────────────────────────
    private void openCandidatures() {
        try {
            Node root = farmsGrid.getScene().getRoot();
            if (root instanceof BorderPane) {
                StackPane contentArea = (StackPane) ((BorderPane) root).getCenter();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PendingApplications.fxml"));
                Node content = loader.load();
                contentArea.getChildren().setAll(content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
