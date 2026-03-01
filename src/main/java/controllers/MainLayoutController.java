package controllers;

import entity.Produit;
import entity.Stock;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import services.NotificationService;
import services.StockAlertService;


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
    @FXML private Button ProdBtn;
    @FXML private Button addstockBtn;
    @FXML private Button adduserBtn;
    @FXML private Button addprodBtn;
    @FXML private Button liststockBtn;
    @FXML private Button ModstockBtn;
    @FXML private Button editprodBtn;
    @FXML private Button btnBOS;
    @FXML private Button exchangeRateBtn;


    // Labels to hide/show
    @FXML private Label homeLabel;
    @FXML private Label animalsLabel;
    @FXML private Label equipmentLabel;
    @FXML private Label stockLabel;
    @FXML private Label cultureLabel;
    @FXML private Label usersLabel;
    @FXML private Label workersLabel;
    @FXML private Label BOStockLabel;
    @FXML private Label exchangeRateLabel;

    @FXML private Label stockAlertBadge;
    private boolean sidebarCollapsed = false;


    // Store reference to self for access from child controllers
    private static MainLayoutController instance;

    @FXML
    public void initialize() {
        instance = this;

        NotificationService.getInstance().init(contentArea);

        navigateToHome();
        demarrerAlertes();
    }

    public static MainLayoutController getInstance() {
        return instance;
    }

    private void demarrerAlertes() {
        StockAlertService.getInstance().setAlertCallback(alerts -> {
            if (stockAlertBadge != null) {
                if (alerts.isEmpty()) {
                    stockAlertBadge.setVisible(false);
                    stockAlertBadge.setManaged(false);
                } else {
                    stockAlertBadge.setText(String.valueOf(alerts.size()));
                    stockAlertBadge.setVisible(true);
                    stockAlertBadge.setManaged(true);
                }
            }
        });
        StockAlertService.getInstance().demarrer();
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

            // Show all labels
            homeLabel.setVisible(true);
            homeLabel.setManaged(true);
            animalsLabel.setVisible(true);
            animalsLabel.setManaged(true);
            equipmentLabel.setVisible(true);
            equipmentLabel.setManaged(true);
            stockLabel.setVisible(true);
            stockLabel.setManaged(true);
            cultureLabel.setVisible(true);
            cultureLabel.setManaged(true);
            usersLabel.setVisible(true);
            usersLabel.setManaged(true);
            workersLabel.setVisible(true);
            workersLabel.setManaged(true);
            BOStockLabel.setVisible(true);
            BOStockLabel.setManaged(true);
            exchangeRateLabel.setVisible(true);
            exchangeRateLabel.setManaged(true);

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
            BOStockLabel.setVisible(false);
            BOStockLabel.setManaged(false);

            toggleIcon.setText("»");
            sidebarCollapsed = true;
        }
    }

    @FXML
    public void navigateToHome() {
        loadContent("/fxml/home_content.fxml");
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
    public void navigateToCulture() {
        loadContent("/fxml/afficher_culture.fxml");
        setActiveButton(cultureBtn);
    }
    @FXML
    public void navigateToParcelle() {
        loadContent("/fxml/afficher_parcelle.fxml");
        setActiveButton(cultureBtn); // Keep culture highlighted since parcelle is sub-page
    }

    @FXML
    public void navigateToUsers() {
        loadContent("/fxml/afficher_users.fxml");
        setActiveButton(usersBtn);
    }

    @FXML
    public void navigateToWorkers() {
        loadContent("/fxml/affectation_view.fxml");
        setActiveButton(workersBtn);
    }
    @FXML
    public void navigateToEvaluation() {
        loadContent("/fxml/evaluation_view.fxml");
        // optionnel : garder workersBtn actif
        setActiveButton(workersBtn);
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
    public void navigateToCommodityPrice() {
        loadContent("/fxml/CommodityPrice.fxml");
    }



    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            System.err.println("Error loading content: " + fxmlPath);
            e.printStackTrace();
            showPlaceholder(fxmlPath);
        }
    }

    private void showPlaceholder(String pageName) {
        contentArea.getChildren().clear();

        VBox placeholder = new VBox(20);
        placeholder.setAlignment(javafx.geometry.Pos.CENTER);
        placeholder.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 40px;");

        Label icon = new Label("🚧");
        icon.setStyle("-fx-font-size: 64px;");

        Label title = new Label("Coming Soon");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label message = new Label("This module will be implemented by your team members.");
        message.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
        message.setWrapText(true);
        message.setMaxWidth(500);

        Label nav = new Label("✅ Navigation is working! Click other modules to test.");
        nav.setStyle("-fx-font-size: 14px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");

        placeholder.getChildren().addAll(icon, title, message, nav);
        contentArea.getChildren().add(placeholder);
    }

    private void setActiveButton(Button activeButton) {
        if (activeButton == null) {
            System.out.println("Attention : activeButton est null – bouton non trouvé");
            return;  // ← Protection contre null (évite le crash)
        }

        // Remove active class from all buttons
        if (homeBtn != null) homeBtn.getStyleClass().remove("active");
        if (animalsBtn != null) animalsBtn.getStyleClass().remove("active");
        if (equipmentBtn != null) equipmentBtn.getStyleClass().remove("active");
        if (stockBtn != null) stockBtn.getStyleClass().remove("active");
        if (cultureBtn != null) cultureBtn.getStyleClass().remove("active");
        if (usersBtn != null) usersBtn.getStyleClass().remove("active");
        if (workersBtn != null) workersBtn.getStyleClass().remove("active");
        if (btnBOS != null) btnBOS.getStyleClass().remove("active");
        if (exchangeRateBtn != null) exchangeRateBtn.getStyleClass().remove("active");


        // Add active class to clicked button
        activeButton.getStyleClass().add("active");
    }
    public void navigateToExchangeRate() {
        loadContent("/fxml/ExchangeRate.fxml");
        setActiveButton(exchangeRateBtn);
    }
}