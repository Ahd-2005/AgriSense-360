package controllers;

import entity.Stock;
import entity.Produit;
import entity.user;
import entity.user.Role;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.SessionManager;
import services.CultureNotificationService;
import utils.EmailService;

import java.io.IOException;

public class MainLayoutController {

    @FXML private VBox sidebar;
    @FXML private StackPane contentArea;
    @FXML private Button toggleButton;

    // Sidebar elements to hide/show
    @FXML private VBox brandText;
    @FXML private Label toggleText;
    @FXML private Label toggleIcon;
    @FXML private Label navTitle;
    @FXML private Label footerText;

    // Navigation buttons
    @FXML private Button homeBtn;
    @FXML private Button animalsBtn;
    @FXML private Button equipmentBtn;
    @FXML private Button stockBtn;
    @FXML private Button cultureBtn;
    @FXML private Button usersBtn;
    @FXML private Button workersBtn;
    @FXML private Button profileBtn;
    @FXML private Button logoutBtn;
    @FXML private Button ProdBtn;
    @FXML private Button addstockBtn;
    @FXML private Button addprodBtn;
    @FXML private Button liststockBtn;
    @FXML private Button ModstockBtn;
    @FXML private Button editprodBtn;
    @FXML private Button btnBOS;

    // Labels to hide/show
    @FXML private Label homeLabel;
    @FXML private Label animalsLabel;
    @FXML private Label equipmentLabel;
    @FXML private Label stockLabel;
    @FXML private Label cultureLabel;
    @FXML private Label usersLabel;
    @FXML private Label workersLabel;
    @FXML private Label profileLabel;
    @FXML private Label logoutLabel;
    @FXML private Button ouvrierBtn;
    @FXML private Label ouvrierLabel;

    private boolean sidebarCollapsed = false;
    private user currentUser;

    // Store reference to self for access from child controllers
    private static MainLayoutController instance;

    @FXML
    public void initialize() {
        instance = this;

        // Get current user from session
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            this.currentUser = sessionManager.getCurrentUser();
            configureForUserRole(currentUser.getRole());
        }

        // Schedule harvest notifications: on start and at 10:00 local, if email is enabled
        if (EmailService.isEnabled()) {
            CultureNotificationService notifier = new CultureNotificationService();
            notifier.scheduleDailyAtTen();
        }

        // Load home page by default
        navigateToHome();
    }

    public static MainLayoutController getInstance() {
        return instance;
    }

    /**
     * Set current user (called after login)
     */
    public void setCurrentUser(user user) {
        this.currentUser = user;
    }

    /**
     * Get current user
     */
    public user getCurrentUser() {
        return currentUser;
    }

    /**
     * Configure sidebar based on user role
     * @param role - the Role enum of the logged-in user
     */
    public void configureForUserRole(Role role) {
        // Hide all buttons first
        hideAllButtons();

        // Show buttons based on role
        switch (role) {
            case ROLE_ADMIN:
                // Admin ONLY sees Home and User Management
                showButton(homeBtn, homeLabel);
                showButton(usersBtn, usersLabel);
                break;

            case ROLE_GERANT:
                // Gerant sees everything EXCEPT User Management
                showButton(homeBtn, homeLabel);
                showButton(animalsBtn, animalsLabel);
                showButton(equipmentBtn, equipmentLabel);
                showButton(stockBtn, stockLabel);
                showButton(cultureBtn, cultureLabel);
                showButton(workersBtn, workersLabel);
                showButton(ouvrierBtn, ouvrierLabel);
                break;

            case ROLE_OUVRIER:
                // Ouvrier ONLY sees "Mes Tâches"
                showButton(homeBtn, homeLabel);
                // Change label to "Mes Tâches" for workers
                homeLabel.setText("Mes Tâches");
                break;

            default:
                // Default: show only home
                showButton(homeBtn, homeLabel);
                break;
        }

        // ALWAYS ensure Profile and Logout are visible
        ensureProfileLogoutVisible();
    }

    private void hideAllButtons() {
        // Hide all management buttons
        animalsBtn.setVisible(false);
        animalsBtn.setManaged(false);
        equipmentBtn.setVisible(false);
        equipmentBtn.setManaged(false);
        stockBtn.setVisible(false);
        stockBtn.setManaged(false);
        cultureBtn.setVisible(false);
        cultureBtn.setManaged(false);
        usersBtn.setVisible(false);
        usersBtn.setManaged(false);
        workersBtn.setVisible(false);
        workersBtn.setManaged(false);
        ouvrierBtn.setVisible(false);
        ouvrierBtn.setManaged(false);
    }

    private void showButton(Button button, Label label) {
        button.setVisible(true);
        button.setManaged(true);

        // If sidebar is collapsed, hide the label
        if (sidebarCollapsed) {
            label.setVisible(false);
            label.setManaged(false);
        } else {
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    /**
     * ALWAYS ensure Profile and Logout buttons are visible
     */
    private void ensureProfileLogoutVisible() {
        // Always show profile and logout buttons regardless of role
        profileBtn.setVisible(true);
        profileBtn.setManaged(true);
        logoutBtn.setVisible(true);
        logoutBtn.setManaged(true);

        // Show labels if sidebar is expanded
        if (!sidebarCollapsed) {
            profileLabel.setVisible(true);
            profileLabel.setManaged(true);
            logoutLabel.setVisible(true);
            logoutLabel.setManaged(true);
        }
    }

    @FXML
    private void toggleSidebar() {
        if (sidebarCollapsed) {
            // Expand sidebar
            sidebar.setPrefWidth(280);
            sidebar.setMaxWidth(280);

            // Show text elements
            brandText.setVisible(true);
            brandText.setManaged(true);
            toggleText.setVisible(true);
            toggleText.setManaged(true);
            navTitle.setVisible(true);
            navTitle.setManaged(true);
            footerText.setText("Connected");

            // Show all visible button labels
            showVisibleLabels();

            toggleIcon.setText("☰");
            sidebarCollapsed = false;

        } else {
            // Collapse sidebar
            sidebar.setPrefWidth(80);
            sidebar.setMaxWidth(80);

            // Hide text elements
            brandText.setVisible(false);
            brandText.setManaged(false);
            toggleText.setVisible(false);
            toggleText.setManaged(false);
            navTitle.setVisible(false);
            navTitle.setManaged(false);
            footerText.setText("•");

            // Hide all labels
            hideAllLabels();

            toggleIcon.setText("»");
            sidebarCollapsed = true;
        }
    }

    private void showVisibleLabels() {
        // Only show labels for buttons that are visible
        if (homeBtn.isVisible()) {
            homeLabel.setVisible(true);
            homeLabel.setManaged(true);
        }
        if (animalsBtn.isVisible()) {
            animalsLabel.setVisible(true);
            animalsLabel.setManaged(true);
        }
        if (equipmentBtn.isVisible()) {
            equipmentLabel.setVisible(true);
            equipmentLabel.setManaged(true);
        }
        if (stockBtn.isVisible()) {
            stockLabel.setVisible(true);
            stockLabel.setManaged(true);
        }
        if (cultureBtn.isVisible()) {
            cultureLabel.setVisible(true);
            cultureLabel.setManaged(true);
        }
        if (usersBtn.isVisible()) {
            usersLabel.setVisible(true);
            usersLabel.setManaged(true);
        }
        if (workersBtn.isVisible()) {
            workersLabel.setVisible(true);
            workersLabel.setManaged(true);
        }
        if (ouvrierBtn.isVisible()) {
            ouvrierLabel.setVisible(true);
            ouvrierLabel.setManaged(true);
        }

        // Profile and Logout are ALWAYS visible
        profileLabel.setVisible(true);
        profileLabel.setManaged(true);
        logoutLabel.setVisible(true);
        logoutLabel.setManaged(true);
    }

    private void hideAllLabels() {
        homeLabel.setVisible(false);
        homeLabel.setManaged(false);
        animalsLabel.setVisible(false);
        animalsLabel.setManaged(false);
        equipmentLabel.setVisible(false);
        equipmentLabel.setManaged(false);
        stockLabel.setVisible(false);
        stockLabel.setManaged(false);
        cultureLabel.setVisible(false);
        cultureLabel.setManaged(false);
        usersLabel.setVisible(false);
        usersLabel.setManaged(false);
        workersLabel.setVisible(false);
        workersLabel.setManaged(false);
        profileLabel.setVisible(false);
        profileLabel.setManaged(false);
        logoutLabel.setVisible(false);
        logoutLabel.setManaged(false);
        ouvrierLabel.setVisible(false);
        ouvrierLabel.setManaged(false);
    }

    @FXML
    public void navigateToHome() {
        // Check user role and load appropriate home page
        if (currentUser != null) {
            switch (currentUser.getRole()) {
                case ROLE_OUVRIER:
                    // Workers see their tasks page
                    loadContent("/fxml/mes_taches.fxml");
                    break;
                case ROLE_ADMIN:
                case ROLE_GERANT:
                default:
                    // Admin and Gerant see normal home
                    loadContent("/fxml/home_content.fxml");
                    break;
            }
        } else {
            loadContent("/fxml/home_content.fxml");
        }
        setActiveButton(homeBtn);
    }

    @FXML
    public void navigateToAnimals() {
        loadContent("/fxml/animals_management_view.fxml");
        setActiveButton(animalsBtn);
    }

    @FXML
    public void navigateToEquipment() {
        loadContent("/fxml/afficher_equipment.fxml");
        setActiveButton(equipmentBtn);
    }

    @FXML
    public void navigateToStock() {
        loadContent("/fxml/StockHome.fxml");
        setActiveButton(stockBtn);
    }
    @FXML
    public void navigateToAddStock() {
        loadContent("/fxml/addStock.fxml");
        setActiveButton(addstockBtn);
    }

    @FXML
    public void navigateToEditStock() {
        loadContent("/fxml/editStock.fxml");
        setActiveButton(ModstockBtn);
    }


    @FXML
    public void navigateToProductList() {
        loadContent("/fxml/ProductList.fxml");
        setActiveButton(ProdBtn);
    }
    @FXML
    public void navigateToStockList() {
        loadContent("/fxml/StockList.fxml");
        setActiveButton(liststockBtn);
    }

    @FXML
    public void navigateToEditProduct() {
        loadContent("/fxml/ModifProd.fxml");
        setActiveButton(editprodBtn);
    }

    @FXML
    public void navigateToAddProduct() {
        loadContent("/fxml/AjoutPrduit.fxml");
        setActiveButton(addprodBtn);
    }

    private static Stock stockToEdit;  // Variable temporaire pour passer le stock

    public static void setStockToEdit(Stock stock) {
        stockToEdit = stock;
    }

    public static Stock getStockToEdit() {
        return stockToEdit;
    }

    // Dans MainLayoutController.java, ajoutez ces lignes après les autres champs

    private static Produit produitToEdit;  // Variable temporaire pour passer le produit

    public static void setProduitToEdit(Produit produit) {
        produitToEdit = produit;
    }

    public static Produit getProduitToEdit() {
        return produitToEdit;
    }
    @FXML
    public void navigateToBackOfficeStock() {
        loadContent("/fxml/BackOfficeStock.fxml");
        setActiveButton(btnBOS);
    }


    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.<Node>load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            System.err.println("Error loading content (IOException): " + fxmlPath);
            e.printStackTrace();
            showPlaceholder(fxmlPath);
        } catch (Exception e) {
            System.err.println("Error loading content (Exception): " + fxmlPath);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            System.err.println("Root cause: " + cause.getMessage());
            cause.printStackTrace();
            showPlaceholder(fxmlPath);
        }
    }
    @FXML
    public void navigateToCulture() {
        loadContent("/fxml/dashboard_culture.fxml");
        setActiveButton(cultureBtn);
    }

    /**
     * Navigate to Afficher Culture page (called from dashboard)
     */
    @FXML
    public void navigateToAfficherCulture() {
        loadContent("/fxml/afficher_culture.fxml");
        setActiveButton(cultureBtn);
    }

    /**
     * Navigate to Afficher Parcelle page (called from dashboard)
     */
    @FXML
    public void navigateToParcelle() {
        loadContent("/fxml/afficher_parcelle.fxml");
        setActiveButton(cultureBtn);
    }

    @FXML
    public void navigateToUsers() {
        loadContent("/fxml/AdminDashboard.fxml");
        setActiveButton(usersBtn);
    }

    @FXML
    public void navigateToWorkers() {
        loadContent("/fxml/affectation_view.fxml");
        setActiveButton(workersBtn);
    }
    @FXML
    public void navigateToOuvrier() {
        loadContent("/fxml/OuvrierManagement.fxml");
        setActiveButton(ouvrierBtn);
    }
    @FXML
    public void navigateToEvaluation() {
        loadContent("/fxml/evaluation_view.fxml");
        // optionnel : garder workersBtn actif
        setActiveButton(workersBtn);
    }


    @FXML
    public void navigateToProfile() {
        loadContent("/fxml/GerantProfile.fxml");
        setActiveButton(profileBtn);
    }

    @FXML
    public void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText("Êtes-vous sûr de vouloir vous déconnecter?");
        alert.setContentText("Vous serez redirigé vers la page de connexion.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Logout from session
                    SessionManager.getInstance().logout();

                    // Load landing page
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Landingpage.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) logoutBtn.getScene().getWindow();
                    Scene scene = new Scene(root, 1400, 800);
                    stage.setScene(scene);
                    stage.centerOnScreen();
                    stage.show();

                } catch (IOException e) {
                    e.printStackTrace();
                    showError("Erreur de déconnexion", "Impossible de retourner à la page d'accueil.");
                }
            }
        });
    }



    private void showPlaceholder(String pageName) {
        contentArea.getChildren().clear();

        VBox placeholder = new VBox(20);
        placeholder.setAlignment(javafx.geometry.Pos.CENTER);
        placeholder.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 40px;");

        Label icon = new Label("🚧");
        icon.setStyle("-fx-font-size: 64px;");

        Label title = new Label("À Venir");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label message = new Label("Ce module sera implémenté prochainement.");
        message.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
        message.setWrapText(true);
        message.setMaxWidth(500);

        Label nav = new Label("✅ La navigation fonctionne! Cliquez sur d'autres modules pour tester.");
        nav.setStyle("-fx-font-size: 14px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");

        placeholder.getChildren().addAll(icon, title, message, nav);
        contentArea.getChildren().add(placeholder);
    }

    private void setActiveButton(Button activeButton) {
        // Remove active class from all buttons
        homeBtn.getStyleClass().remove("active");
        animalsBtn.getStyleClass().remove("active");
        equipmentBtn.getStyleClass().remove("active");
        stockBtn.getStyleClass().remove("active");
        cultureBtn.getStyleClass().remove("active");
        usersBtn.getStyleClass().remove("active");
        workersBtn.getStyleClass().remove("active");
        profileBtn.getStyleClass().remove("active");
        ouvrierBtn.getStyleClass().remove("active");


        if (!activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}