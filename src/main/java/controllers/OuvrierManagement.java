package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import entity.user;
import services.TacheService;
import services.userservice;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OuvrierManagement {

    @FXML private VBox cardContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label resultCountLabel;

    private List<user> allOuvriers = new ArrayList<>();

    @FXML
    public void initialize() {
        sortComboBox.getItems().addAll(
                "Nom (A → Z)", "Nom (Z → A)", "Email (A → Z)", "Statut"
        );
        sortComboBox.setPromptText("Choisir un tri...");

        statusFilterComboBox.getItems().addAll("Tous les statuts", "ACTIF", "BLOQUÉ");
        statusFilterComboBox.setValue("Tous les statuts");

        loadOuvriers();
    }

    // ───────────────────────────────────────────────
    // CHARGEMENT
    // ───────────────────────────────────────────────
    private void loadOuvriers() {
        try {
            allOuvriers = new userservice().getAllOuvriers();
            applyFiltersAndSort();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec du chargement : " + e.getMessage());
        }
    }

    // ───────────────────────────────────────────────
    // FILTRES & TRI
    // ───────────────────────────────────────────────
    @FXML private void handleSearch()       { applyFiltersAndSort(); }
    @FXML private void handleSort()         { applyFiltersAndSort(); }
    @FXML private void handleStatusFilter() { applyFiltersAndSort(); }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        sortComboBox.setValue(null);
        sortComboBox.setPromptText("Choisir un tri...");
        statusFilterComboBox.setValue("Tous les statuts");
        applyFiltersAndSort();
    }

    private void applyFiltersAndSort() {
        String search       = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String statusFilter = statusFilterComboBox.getValue();
        String sort         = sortComboBox.getValue();

        List<user> filtered = allOuvriers.stream()
                .filter(u -> search.isEmpty()
                        || u.getName().toLowerCase().contains(search)
                        || u.getEmail().toLowerCase().contains(search)
                        || (u.getPhone() != null && u.getPhone().toLowerCase().contains(search)))
                .filter(u -> {
                    if (statusFilter == null || statusFilter.equals("Tous les statuts")) return true;
                    if (statusFilter.equals("ACTIF"))  return "ACTIVE".equals(u.getStatus());
                    if (statusFilter.equals("BLOQUÉ")) return "BLOCKED".equals(u.getStatus());
                    return true;
                })
                .collect(Collectors.toList());

        if (sort != null) {
            switch (sort) {
                case "Nom (A → Z)":   filtered.sort(Comparator.comparing(u -> u.getName().toLowerCase())); break;
                case "Nom (Z → A)":   filtered.sort((a, b) -> b.getName().toLowerCase().compareTo(a.getName().toLowerCase())); break;
                case "Email (A → Z)": filtered.sort(Comparator.comparing(u -> u.getEmail().toLowerCase())); break;
                case "Statut":        filtered.sort(Comparator.comparing(user::getStatus)); break;
            }
        }

        int total = filtered.size();
        resultCountLabel.setText(total + " ouvrier" + (total > 1 ? "s" : "") +
                " trouvé" + (total > 1 ? "s" : ""));

        cardContainer.getChildren().clear();
        if (filtered.isEmpty()) {
            Label empty = new Label("Aucun ouvrier trouvé.");
            empty.setStyle("-fx-font-size: 16px; -fx-text-fill: gray; -fx-padding: 50;");
            cardContainer.getChildren().add(empty);
        } else {
            for (user o : filtered) cardContainer.getChildren().add(createOuvrierCard(o));
        }
    }

    // ───────────────────────────────────────────────
    // CARTE OUVRIER (avec compteur tâches)
    // ───────────────────────────────────────────────
    private HBox createOuvrierCard(user ouvrier) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18, 24, 18, 20));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);" +
                        "-fx-border-color: rgba(43,59,31,0.07); -fx-border-width: 1;" +
                        "-fx-border-radius: 16px;"
        );

        // ── Avatar ──
        StackPane avatarPane = buildAvatar(ouvrier);

        // ── Infos ──
        VBox infoBox = new VBox(6);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Nom + badge rôle
        HBox nameLine = new HBox(10);
        nameLine.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(ouvrier.getName());
        nameLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #22301b;");
        Label roleBadge = new Label("Ouvrier");
        roleBadge.setStyle(
                "-fx-padding: 3 12; -fx-background-radius: 12px; -fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-color: rgba(211,84,0,0.15); -fx-text-fill: #d35400;");
        nameLine.getChildren().addAll(nameLabel, roleBadge);

        // Email + téléphone
        HBox contactLine = new HBox(22);
        contactLine.setAlignment(Pos.CENTER_LEFT);
        contactLine.getChildren().addAll(
                infoChip("📧", ouvrier.getEmail()),
                infoChip("📱", ouvrier.getPhone() != null ? ouvrier.getPhone() : "—")
        );

        // ── Compteur de tâches actives ──
        HBox tacheLine = buildTacheChip(ouvrier.getId());

        infoBox.getChildren().addAll(nameLine, contactLine, tacheLine);

        // ── Badge statut ──
        Label statusBadge = new Label("ACTIVE".equals(ouvrier.getStatus()) ? "✔ Actif" : "✖ Bloqué");
        statusBadge.setStyle(
                "-fx-padding: 6 18; -fx-background-radius: 15px; -fx-font-size: 12px; -fx-font-weight: bold;" +
                        ("ACTIVE".equals(ouvrier.getStatus())
                                ? "-fx-background-color: rgba(90,152,20,0.15); -fx-text-fill: #5a9814;"
                                : "-fx-background-color: rgba(231,76,60,0.15); -fx-text-fill: #e74c3c;")
        );

        // ── Boutons ──
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button viewBtn   = makeBtn("👁 Voir",     "#3498db");
        Button editBtn   = makeBtn("✏ Modifier",  "#f39c12");
        Button deleteBtn = makeBtn("🗑 Supprimer", "#e74c3c");

        viewBtn.setOnAction(e   -> handleViewOuvrier(ouvrier));
        editBtn.setOnAction(e   -> handleModifyOuvrier(ouvrier));
        deleteBtn.setOnAction(e -> handleDeleteOuvrier(ouvrier));

        actions.getChildren().addAll(viewBtn, editBtn, deleteBtn);
        card.getChildren().addAll(avatarPane, infoBox, statusBadge, actions);
        return card;
    }

    /**
     * Construit la puce de comptage de tâches pour un ouvrier.
     * Affiche : "📋 N tâche(s) active(s)" ou "✅ Toutes terminées" ou "📋 Aucune tâche"
     */
    private HBox buildTacheChip(int ouvrierId) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);

        try {
            TacheService ts = new TacheService();
            int actives = ts.countTachesActives(ouvrierId);
            List<entity.Tache> toutesLesTaches = ts.getTachesParOuvrier(ouvrierId);
            int total = toutesLesTaches.size();

            String texte;
            String style;

            if (total == 0) {
                texte = "📋 Aucune tâche";
                style = "-fx-font-size:12px; -fx-text-fill:rgba(34,48,27,0.45);";
            } else if (actives == 0) {
                texte = "✅ Toutes terminées (" + total + ")";
                style = "-fx-font-size:12px; -fx-text-fill:#27ae60; -fx-font-weight:bold;";
            } else {
                texte = "📋 " + actives + " tâche" + (actives > 1 ? "s" : "") + " active" + (actives > 1 ? "s" : "");
                style = "-fx-font-size:12px; -fx-text-fill:#e67e22; -fx-font-weight:bold;";
            }

            Label lbl = new Label(texte);
            lbl.setStyle(style);
            chip.getChildren().add(lbl);

        } catch (SQLException e) {
            Label lbl = new Label("📋 —");
            lbl.setStyle("-fx-font-size:12px; -fx-text-fill:rgba(34,48,27,0.45);");
            chip.getChildren().add(lbl);
        }

        return chip;
    }

    // ── Avatar ────────────────────────────────────
    private StackPane buildAvatar(user u) {
        StackPane pane = new StackPane();
        pane.setMinSize(56, 56); pane.setMaxSize(56, 56);
        String picUrl = u.getProfilePicture();
        if (picUrl != null && !picUrl.isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(picUrl, 56, 56, true, true));
                iv.setFitWidth(56); iv.setFitHeight(56);
                Circle clip = new Circle(28, 28, 28);
                iv.setClip(clip);
                pane.getChildren().add(iv);
                return pane;
            } catch (Exception ignored) {}
        }
        Label lbl = new Label(getInitials(u.getName()));
        lbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane bg = new StackPane(lbl);
        bg.setMinSize(56, 56); bg.setMaxSize(56, 56);
        bg.setStyle("-fx-background-color:" + getAvatarColor(u.getName()) + "; -fx-background-radius:28;");
        pane.getChildren().add(bg);
        return pane;
    }

    private HBox infoChip(String icon, String text) {
        HBox chip = new HBox(5);
        chip.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon); ico.setStyle("-fx-font-size:12px;");
        Label lbl = new Label(text); lbl.setStyle("-fx-font-size:13px; -fx-text-fill:rgba(34,48,27,0.75);");
        chip.getChildren().addAll(ico, lbl);
        return chip;
    }

    private Button makeBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white;" +
                "-fx-padding:7 14; -fx-cursor:hand; -fx-font-size:12px;" +
                "-fx-background-radius:8px; -fx-font-weight:bold;");
        return btn;
    }

    // ───────────────────────────────────────────────
    // ACTIONS
    // ───────────────────────────────────────────────
    private void handleViewOuvrier(user ouvrier) {
        loadInMainLayout("/fxml/OuvrierDetails.fxml", controller -> {
            OuvrierDetails c = (OuvrierDetails) controller;
            c.setOuvrier(ouvrier);
            c.setOuvrierManagement(this);
        });
    }

    private void handleModifyOuvrier(user ouvrier) {
        loadInMainLayout("/fxml/OuvrierEdit.fxml", controller -> {
            OuvrierEdit c = (OuvrierEdit) controller;
            c.setOuvrier(ouvrier);
            c.setOuvrierManagement(this);
        });
    }

    private void handleDeleteOuvrier(user ouvrier) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer l'ouvrier");
        confirm.setContentText("Supprimer définitivement " + ouvrier.getName() + " ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    new userservice().deleteUser(ouvrier.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Ouvrier supprimé avec succès !");
                    refreshTable();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de suppression : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleAddOuvrier() {
        loadInMainLayout("/fxml/OuvrierAdd.fxml", controller -> {
            OuvrierAdd c = (OuvrierAdd) controller;
            c.setOuvrierManagement(this);
        });
    }

    public void refreshTable() { loadOuvriers(); }

    // ───────────────────────────────────────────────
    // NAVIGATION
    // ───────────────────────────────────────────────
    private void loadInMainLayout(String fxmlPath, ControllerCallback callback) {
        try {
            Parent root = cardContainer.getScene().getRoot();
            if (root instanceof BorderPane) {
                StackPane contentArea = (StackPane) ((BorderPane) root).getCenter();
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent content = loader.load();
                if (callback != null) callback.onControllerLoaded(loader.getController());
                contentArea.getChildren().clear();
                contentArea.getChildren().add(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la page : " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ControllerCallback { void onControllerLoaded(Object controller); }

    // ── Helpers avatar ────────────────────────────
    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
    }

    private String getAvatarColor(String name) {
        String[] colors = {"#2d7a3a","#2980b9","#8e44ad","#c0392b","#d35400","#16a085","#2c3e50"};
        return colors[Math.abs(name.hashCode()) % colors.length];
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}