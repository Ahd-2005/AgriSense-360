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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import entity.user;
import entity.user.Role;
import services.userservice;
import services.SessionManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AdminDashboard {

    @FXML private VBox userCardsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> roleFilterComboBox;
    @FXML private Label resultCountLabel;

    private user currentUser;
    private List<user> allUsers = new ArrayList<>();

    @FXML
    public void initialize() {
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            this.currentUser = sessionManager.getCurrentUser();
            if (currentUser.getRole() != Role.ROLE_ADMIN) {
                showAlert(Alert.AlertType.ERROR, "Accès refusé",
                        "Vous n'avez pas la permission d'accéder à cette page.");
                return;
            }
        }

        // Initialiser les ComboBox
        sortComboBox.getItems().addAll(
                "Nom (A → Z)",
                "Nom (Z → A)",
                "Email (A → Z)",
                "Rôle",
                "Statut"
        );
        sortComboBox.setPromptText("Choisir un tri...");

        statusFilterComboBox.getItems().addAll("Tous les statuts", "ACTIF", "BLOQUÉ");
        statusFilterComboBox.setValue("Tous les statuts");

        roleFilterComboBox.getItems().addAll(
                "Tous les rôles", "Administrateur", "Gérant", "Ouvrier"
        );
        roleFilterComboBox.setValue("Tous les rôles");

        loadUsers();
    }

    // ───────────────────────────────────────────────
    // CHARGEMENT
    // ───────────────────────────────────────────────
    private void loadUsers() {
        try {
            userservice service = new userservice();
            allUsers = service.getAllUsers();
            applyFiltersAndSort();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les utilisateurs : " + e.getMessage());
        }
    }

    // ───────────────────────────────────────────────
    // FILTRES & TRI
    // ───────────────────────────────────────────────
    @FXML
    private void handleSearch() {
        applyFiltersAndSort();
    }

    @FXML
    private void handleSort() {
        applyFiltersAndSort();
    }

    @FXML
    private void handleStatusFilter() {
        applyFiltersAndSort();
    }

    @FXML
    private void handleRoleFilter() {
        applyFiltersAndSort();
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        sortComboBox.setValue(null);
        sortComboBox.setPromptText("Choisir un tri...");
        statusFilterComboBox.setValue("Tous les statuts");
        roleFilterComboBox.setValue("Tous les rôles");
        applyFiltersAndSort();
    }

    private void applyFiltersAndSort() {
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String statusFilter = statusFilterComboBox.getValue();
        String roleFilter = roleFilterComboBox.getValue();
        String sort = sortComboBox.getValue();

        List<user> filtered = allUsers.stream()
                // Filtre recherche
                .filter(u -> search.isEmpty()
                        || u.getName().toLowerCase().contains(search)
                        || u.getEmail().toLowerCase().contains(search)
                        || (u.getPhone() != null && u.getPhone().toLowerCase().contains(search)))
                // Filtre statut
                .filter(u -> {
                    if (statusFilter == null || statusFilter.equals("Tous les statuts")) return true;
                    if (statusFilter.equals("ACTIF")) return "ACTIVE".equals(u.getStatus());
                    if (statusFilter.equals("BLOQUÉ")) return "BLOCKED".equals(u.getStatus());
                    return true;
                })
                // Filtre rôle
                .filter(u -> {
                    if (roleFilter == null || roleFilter.equals("Tous les rôles")) return true;
                    switch (roleFilter) {
                        case "Administrateur": return u.getRole() == Role.ROLE_ADMIN;
                        case "Gérant":         return u.getRole() == Role.ROLE_GERANT;
                        case "Ouvrier":        return u.getRole() == Role.ROLE_OUVRIER;
                        default: return true;
                    }
                })
                .collect(Collectors.toList());

        // Tri
        if (sort != null) {
            switch (sort) {
                case "Nom (A → Z)": filtered.sort(Comparator.comparing(u -> u.getName().toLowerCase())); break;
                case "Nom (Z → A)": filtered.sort((a, b) -> b.getName().toLowerCase().compareTo(a.getName().toLowerCase())); break;
                case "Email (A → Z)": filtered.sort(Comparator.comparing(u -> u.getEmail().toLowerCase())); break;
                case "Rôle":   filtered.sort(Comparator.comparing(u -> u.getRole().name())); break;
                case "Statut": filtered.sort(Comparator.comparing(user::getStatus)); break;
            }
        }

        // Mise à jour compteur
        int total = filtered.size();
        resultCountLabel.setText(total + " utilisateur" + (total > 1 ? "s" : "") + " trouvé" + (total > 1 ? "s" : ""));

        // Affichage des cartes
        userCardsContainer.getChildren().clear();
        if (filtered.isEmpty()) {
            Label emptyLabel = new Label("Aucun utilisateur trouvé.");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray; -fx-padding: 50;");
            userCardsContainer.getChildren().add(emptyLabel);
        } else {
            for (user u : filtered) {
                userCardsContainer.getChildren().add(createUserCard(u));
            }
        }
    }

    // ───────────────────────────────────────────────
    // CRÉATION DE LA CARTE
    // ───────────────────────────────────────────────
    private HBox createUserCard(user u) {
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

        // ── Photo de profil ──
        StackPane avatarPane = buildAvatar(u);

        // ── Infos principales ──
        VBox infoBox = new VBox(6);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Ligne 1 : Nom + Badge rôle
        HBox nameLine = new HBox(10);
        nameLine.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(u.getName());
        nameLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #22301b;");

        Label roleBadge = new Label(getRoleFriendlyName(u.getRole()));
        roleBadge.setStyle(
                "-fx-padding: 3 12; -fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;" +
                        getRoleColor(u.getRole())
        );
        nameLine.getChildren().addAll(nameLabel, roleBadge);

        // Ligne 2 : Email + Téléphone
        HBox contactLine = new HBox(22);
        contactLine.setAlignment(Pos.CENTER_LEFT);
        contactLine.getChildren().addAll(
                infoChip("📧", u.getEmail()),
                infoChip("📱", u.getPhone() != null ? u.getPhone() : "—")
        );

        infoBox.getChildren().addAll(nameLine, contactLine);

        // ── Badge statut ──
        Label statusBadge = new Label(
                "ACTIVE".equals(u.getStatus()) ? "✔ Actif" : "✖ Bloqué"
        );
        statusBadge.setStyle(
                "-fx-padding: 6 18; -fx-background-radius: 15px; -fx-font-size: 12px; -fx-font-weight: bold;" +
                        ("ACTIVE".equals(u.getStatus())
                                ? "-fx-background-color: rgba(90,152,20,0.15); -fx-text-fill: #5a9814;"
                                : "-fx-background-color: rgba(231,76,60,0.15); -fx-text-fill: #e74c3c;")
        );

        // ── Boutons d'action ──
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button viewBtn   = makeBtn("👁 Voir",       "#3498db");
        Button editBtn   = makeBtn("✏ Modifier",    "#f39c12");
        Button deleteBtn = makeBtn("🗑 Supprimer",   "#e74c3c");

        viewBtn.setOnAction(e   -> openUserDetails(u));
        editBtn.setOnAction(e   -> openUserEdit(u));
        deleteBtn.setOnAction(e -> handleDeleteUser(u));

        if (currentUser != null && u.getId() == currentUser.getId()) {
            deleteBtn.setDisable(true);
            deleteBtn.setStyle(deleteBtn.getStyle() + "-fx-opacity:0.4;");
        }

        actions.getChildren().addAll(viewBtn, editBtn, deleteBtn);

        card.getChildren().addAll(avatarPane, infoBox, statusBadge, actions);
        return card;
    }

    // ── Photo de profil / initiales ──────────────────────
    private StackPane buildAvatar(user u) {
        StackPane pane = new StackPane();
        pane.setMinSize(56, 56);
        pane.setMaxSize(56, 56);

        String picUrl = u.getProfilePicture();
        if (picUrl != null && !picUrl.isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(picUrl, 56, 56, true, true));
                iv.setFitWidth(56); iv.setFitHeight(56);
                Circle clip = new Circle(28, 28, 28);
                iv.setClip(clip);
                iv.setStyle("-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.15),6,0,0,2);");
                pane.getChildren().add(iv);
                return pane;
            } catch (Exception ignored) { /* fallback vers initiales */ }
        }

        // Initiales colorées
        String initials = getInitials(u.getName());
        Label initialesLabel = new Label(initials);
        initialesLabel.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;"
        );
        StackPane bg = new StackPane(initialesLabel);
        bg.setMinSize(56, 56); bg.setMaxSize(56, 56);
        bg.setStyle(
                "-fx-background-color:" + getAvatarColor(u.getName()) + ";" +
                        "-fx-background-radius: 28;"
        );
        pane.getChildren().add(bg);
        return pane;
    }

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

    // ── Info chip (icône + texte) ────────────────────────
    private HBox infoChip(String icon, String text) {
        HBox chip = new HBox(5);
        chip.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 12px;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(34,48,27,0.75);");
        chip.getChildren().addAll(ico, lbl);
        return chip;
    }

    // ── Rôle ────────────────────────────────────────────
    private String getRoleFriendlyName(Role role) {
        switch (role) {
            case ROLE_ADMIN:   return "Administrateur";
            case ROLE_GERANT:  return "Gérant";
            case ROLE_OUVRIER: return "Ouvrier";
            default:           return role.name();
        }
    }

    private String getRoleColor(Role role) {
        switch (role) {
            case ROLE_ADMIN:   return "-fx-background-color: rgba(142,68,173,0.15); -fx-text-fill: #8e44ad;";
            case ROLE_GERANT:  return "-fx-background-color: rgba(41,128,185,0.15); -fx-text-fill: #2980b9;";
            case ROLE_OUVRIER: return "-fx-background-color: rgba(211,84,0,0.15);   -fx-text-fill: #d35400;";
            default:           return "-fx-background-color: #eee; -fx-text-fill: #555;";
        }
    }

    // ── Bouton action ────────────────────────────────────
    private Button makeBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color:" + color + "; -fx-text-fill:white;" +
                        "-fx-padding: 7 14; -fx-cursor: hand; -fx-font-size: 12px;" +
                        "-fx-background-radius: 8px; -fx-font-weight: bold;"
        );
        return btn;
    }

    // ───────────────────────────────────────────────
    // ACTIONS
    // ───────────────────────────────────────────────
    private void openUserDetails(user selectedUser) {
        loadInMainLayout("/fxml/UserDetails.fxml", controller -> {
            UserDetails detailsController = (UserDetails) controller;
            detailsController.setUser(selectedUser);
            detailsController.setAdminDashboard(this);
        });
    }

    private void openUserEdit(user selectedUser) {
        loadInMainLayout("/fxml/UserEdit.fxml", controller -> {
            UserEdit editController = (UserEdit) controller;
            editController.setUser(selectedUser);
            editController.setAdminDashboard(this);
        });
    }

    private void handleDeleteUser(user selectedUser) {
        if (currentUser != null && selectedUser.getId() == currentUser.getId()) {
            showAlert(Alert.AlertType.WARNING, "Action impossible",
                    "Vous ne pouvez pas supprimer votre propre compte !");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmer la suppression");
        confirmAlert.setHeaderText("Supprimer l'utilisateur");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer " + selectedUser.getName() + " ?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userservice service = new userservice();
                    service.deleteUser(selectedUser.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur supprimé avec succès !");
                    refreshTable();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la suppression : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleAddUser() {
        loadInMainLayout("/fxml/UserAdd.fxml", controller -> {
            UserAdd addController = (UserAdd) controller;
            addController.setAdminDashboard(this);
        });
    }

    public void refreshTable() {
        loadUsers();
    }

    // ───────────────────────────────────────────────
    // NAVIGATION INTERNE
    // ───────────────────────────────────────────────
    private void loadInMainLayout(String fxmlPath, ControllerCallback callback) {
        try {
            Parent root = userCardsContainer.getScene().getRoot();
            if (root instanceof BorderPane) {
                BorderPane mainLayout = (BorderPane) root;
                StackPane contentArea = (StackPane) mainLayout.getCenter();

                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent content = loader.load();

                if (callback != null) {
                    callback.onControllerLoaded(loader.getController());
                }

                contentArea.getChildren().clear();
                contentArea.getChildren().add(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la page : " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ControllerCallback {
        void onControllerLoaded(Object controller);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}