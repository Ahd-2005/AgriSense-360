package controllers;

import entity.Stock;
import entity.Produit;
import entity.user;
import entity.user.Role;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.AgriBotService;
import services.SessionManager;
import services.CultureNotificationService;
import services.NotificationService;
import services.StockAlertService;
import utils.EmailService;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainLayoutController {

    // ── Original sidebar fields (100% unchanged) ─────────────────────
    @FXML private VBox sidebar;
    @FXML private StackPane contentArea;
    @FXML private Button toggleButton;

    @FXML private VBox brandText;
    @FXML private Label toggleText;
    @FXML private Label toggleIcon;
    @FXML private Label navTitle;
    @FXML private Label footerText;

    @FXML private Button homeBtn;
    @FXML private Button animalsBtn;
    @FXML private Button equipmentBtn;
    @FXML private Button stockBtn;
    @FXML private Button cultureBtn;
    @FXML private Button usersBtn;
    @FXML private Button workersBtn;
    @FXML private Button evaluationBtn;
    @FXML private Button profileBtn;
    @FXML private Button logoutBtn;
    @FXML private Button ProdBtn;
    @FXML private Button addstockBtn;
    @FXML private Button addprodBtn;
    @FXML private Button liststockBtn;
    @FXML private Button ModstockBtn;
    @FXML private Button editprodBtn;
    @FXML private Button btnBOS;
    @FXML private Button exchangeRateBtn;

    @FXML private Label homeLabel;
    @FXML private Label animalsLabel;
    @FXML private Label equipmentLabel;
    @FXML private Label stockLabel;
    @FXML private Label cultureLabel;
    @FXML private Label usersLabel;
    @FXML private Label workersLabel;
    @FXML private Label evaluationLabel;
    @FXML private Label profileLabel;
    @FXML private Label logoutLabel;
    @FXML private Button ouvrierBtn;
    @FXML private Label ouvrierLabel;
    @FXML private Label exchangeRateLabel;
    @FXML private Label stockAlertBadge;

    private boolean badgeFermeParUtilisateur = false;

    private boolean sidebarCollapsed = false;
    private user currentUser;
    private static MainLayoutController instance;

    // ── Chatbot state ────────────────────────────────────────────────
    private final AgriBotService  botService = new AgriBotService();
    private final ExecutorService executor   = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AgriBot-Thread");
        t.setDaemon(true);
        return t;
    });
    private boolean greeted     = false;
    private int     unreadCount = 0;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Floating chat window (created once, reused)
    private Stage     chatStage;
    private VBox      messagesContainer;
    private ScrollPane chatScrollPane;
    private TextField  messageInput;
    private Button     sendButton;
    private Label      chatStatusLabel;
    private HBox       typingIndicator;

    private static final String SEND_BTN_NORMAL =
            "-fx-background-color:#2d7a3a;-fx-text-fill:white;-fx-font-size:15px;" +
                    "-fx-min-width:38;-fx-min-height:38;-fx-max-width:38;-fx-max-height:38;" +
                    "-fx-background-radius:50;-fx-cursor:hand;-fx-border-width:0;-fx-padding:0;";
    private static final String SEND_BTN_HOVER =
            "-fx-background-color:#1e5c28;-fx-text-fill:white;-fx-font-size:15px;" +
                    "-fx-min-width:38;-fx-min-height:38;-fx-max-width:38;-fx-max-height:38;" +
                    "-fx-background-radius:50;-fx-cursor:hand;-fx-border-width:0;-fx-padding:0;";
    private static final String SEND_BTN_DISABLED =
            "-fx-background-color:#a5d6a7;-fx-text-fill:white;-fx-font-size:15px;" +
                    "-fx-min-width:38;-fx-min-height:38;-fx-max-width:38;-fx-max-height:38;" +
                    "-fx-background-radius:50;-fx-border-width:0;-fx-padding:0;-fx-opacity:0.7;";

    // ════════════════════════════════════════════════════════════════
    // INITIALIZE
    // ════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        instance = this;

        // ── Original sidebar init ──
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            this.currentUser = sessionManager.getCurrentUser();
            configureForUserRole(currentUser.getRole());
        }
        if (EmailService.isEnabled()) {
            CultureNotificationService notifier = new CultureNotificationService();
            notifier.scheduleDailyAtTen();
        }
        NotificationService.getInstance().init(contentArea);
        StockAlertService.getInstance().initialiser();
        navigateToHome();
    }

    // ── Build the chat window (called once) ───────────────────────────
    private void buildChatStage() {
        chatStage = new Stage();
        chatStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        chatStage.initOwner(contentArea.getScene().getWindow());
        chatStage.setAlwaysOnTop(true);
        chatStage.setResizable(false);

        // ── Root VBox ──
        VBox root = new VBox();
        root.setPrefWidth(320);
        root.setPrefHeight(480);
        root.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:16;" +
                        "-fx-border-radius:16;" +
                        "-fx-border-color:#c8e6c9;" +
                        "-fx-border-width:1.5;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.28),20,0,0,6);"
        );

        // ── Header ──
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color:linear-gradient(to right,#2d7a3a,#43a047);" +
                        "-fx-background-radius:14 14 0 0;-fx-padding:11 12 11 14;"
        );
        Label icon = new Label("🌾");
        icon.setStyle(
                "-fx-font-size:20px;-fx-background-color:rgba(255,255,255,0.2);" +
                        "-fx-background-radius:50;-fx-padding:5 7 5 7;" +
                        "-fx-min-width:34;-fx-min-height:34;-fx-alignment:CENTER;"
        );
        VBox titleBox = new VBox(1);
        javafx.scene.layout.HBox.setHgrow(titleBox, javafx.scene.layout.Priority.ALWAYS);
        Label titleLbl = new Label("AgriBot");
        titleLbl.setStyle("-fx-text-fill:white;-fx-font-size:14px;-fx-font-weight:bold;");
        chatStatusLabel = new Label("● Online");
        chatStatusLabel.setStyle("-fx-text-fill:#c8f5ca;-fx-font-size:11px;");
        titleBox.getChildren().addAll(titleLbl, chatStatusLabel);

        // Close & minimize buttons
        Button minBtn = makeHeaderBtn("—");
        Button closeBtn = makeHeaderBtn("✕");
        minBtn.setOnAction(e -> chatStage.hide());
        closeBtn.setOnAction(e -> { chatStage.hide(); clearNotification(); });

        header.getChildren().addAll(icon, titleBox, minBtn, closeBtn);

        // ── Messages scroll ──
        chatScrollPane = new ScrollPane();
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(chatScrollPane, javafx.scene.layout.Priority.ALWAYS);
        chatScrollPane.setStyle(
                "-fx-background-color:#fafff9;-fx-background:#fafff9;" +
                        "-fx-border-color:transparent;-fx-padding:0;"
        );
        messagesContainer = new VBox(6);
        messagesContainer.setStyle("-fx-background-color:#fafff9;-fx-padding:12 4 12 4;");

        // Typing indicator
        typingIndicator = new HBox(8);
        typingIndicator.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        typingIndicator.setStyle("-fx-padding:3 40 3 8;");
        typingIndicator.setVisible(false);
        typingIndicator.setManaged(false);
        Label tAvatar = new Label("🌿");
        tAvatar.setStyle(
                "-fx-background-color:#e8f5e9;-fx-padding:5 6 5 6;" +
                        "-fx-background-radius:50;-fx-font-size:14px;" +
                        "-fx-min-width:32;-fx-min-height:32;-fx-alignment:CENTER;"
        );
        HBox dots = new HBox(5);
        dots.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        dots.setStyle(
                "-fx-background-color:#f1f8e9;-fx-background-radius:18 18 18 4;" +
                        "-fx-border-color:#c8e6c9;-fx-border-radius:18 18 18 4;" +
                        "-fx-border-width:1;-fx-padding:9 14 9 14;"
        );
        for (int i = 0; i < 3; i++) {
            Label d = new Label("●");
            d.setStyle("-fx-text-fill:#66bb6a;-fx-font-size:11px;");
            dots.getChildren().add(d);
        }
        typingIndicator.getChildren().addAll(tAvatar, dots);
        messagesContainer.getChildren().add(typingIndicator);
        chatScrollPane.setContent(messagesContainer);

        // ── Input bar ──
        HBox inputBar = new HBox(8);
        inputBar.setAlignment(javafx.geometry.Pos.CENTER);
        inputBar.setStyle(
                "-fx-background-color:#f5f5f5;-fx-background-radius:0 0 14 14;" +
                        "-fx-border-color:#e0e0e0;-fx-border-width:1 0 0 0;-fx-padding:10 12 10 12;"
        );
        messageInput = new TextField();
        messageInput.setPromptText("Ask AgriBot anything...");
        HBox.setHgrow(messageInput, javafx.scene.layout.Priority.ALWAYS);
        messageInput.setStyle(
                "-fx-background-color:white;-fx-background-radius:20;" +
                        "-fx-border-color:#c8e6c9;-fx-border-radius:20;-fx-border-width:1.5;" +
                        "-fx-font-size:13px;-fx-text-fill:#333;-fx-padding:8 14 8 14;"
        );
        sendButton = new Button("➤");
        sendButton.setDisable(true);
        sendButton.setStyle(SEND_BTN_DISABLED);
        messageInput.textProperty().addListener((obs, old, nv) -> {
            boolean empty = nv == null || nv.trim().isEmpty();
            sendButton.setDisable(empty);
            sendButton.setStyle(empty ? SEND_BTN_DISABLED : SEND_BTN_NORMAL);
        });
        sendButton.setOnMouseEntered(e -> { if (!sendButton.isDisabled()) sendButton.setStyle(SEND_BTN_HOVER); });
        sendButton.setOnMouseExited(e  -> { if (!sendButton.isDisabled()) sendButton.setStyle(SEND_BTN_NORMAL); });
        sendButton.setOnAction(e -> handleSend());
        messageInput.setOnKeyPressed(ev -> {
            if (ev.getCode() == javafx.scene.input.KeyCode.ENTER) handleSend();
        });
        inputBar.getChildren().addAll(messageInput, sendButton);

        root.getChildren().addAll(header, chatScrollPane, inputBar);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 320, 480);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        chatStage.setScene(scene);

        // Position near the button when shown
        chatStage.setOnShowing(e -> positionChatStage());
    }

    private Button makeHeaderBtn(String txt) {
        Button b = new Button(txt);
        String normal = "-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;" +
                "-fx-font-size:13px;-fx-min-width:28;-fx-min-height:28;" +
                "-fx-max-width:28;-fx-max-height:28;-fx-background-radius:50;" +
                "-fx-cursor:hand;-fx-border-width:0;-fx-padding:0;";
        String hover  = "-fx-background-color:rgba(255,255,255,0.32);-fx-text-fill:white;" +
                "-fx-font-size:13px;-fx-min-width:28;-fx-min-height:28;" +
                "-fx-max-width:28;-fx-max-height:28;-fx-background-radius:50;" +
                "-fx-cursor:hand;-fx-border-width:0;-fx-padding:0;";
        b.setStyle(normal);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(normal));
        return b;
    }

    private void positionChatStage() {
        // Always position bottom-right corner of screen — Facebook Messenger style
        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
        chatStage.setX(screen.getMaxX() - 328);   // 320 wide + 8px margin
        chatStage.setY(screen.getMaxY() - 488);   // 480 tall + 8px margin
    }

    // ── Toggle ────────────────────────────────────────────────────────
    @FXML
    public void toggleChat() {
        if (chatStage != null && chatStage.isShowing()) {
            chatStage.hide();
        } else {
            if (chatStage == null) buildChatStage();
            clearNotification();
            chatStage.show();
            if (!greeted) {
                greeted = true;
                Platform.runLater(() -> addBotMessage(
                        "👋 Hi! I'm AgriBot, your AI farming assistant.\n\n" +
                                "Ask me about crops, soil, irrigation, livestock, or any farm topic! 🌱"));
            }
            Platform.runLater(() -> { if (chatScrollPane != null) chatScrollPane.setVvalue(1.0); });
            Platform.runLater(() -> { if (messageInput != null) messageInput.requestFocus(); });
        }
    }

    @FXML
    public void minimizeChat() {
        if (chatStage != null) chatStage.hide();
    }

    @FXML
    public void closeChat() {
        minimizeChat();
        clearNotification();
    }

    @FXML
    public void handleSend() {
        if (messageInput == null) return;
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;

        messageInput.clear();
        sendButton.setDisable(true);
        sendButton.setStyle(SEND_BTN_DISABLED);

        addUserMessage(text);
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));

        typingIndicator.setVisible(true);
        typingIndicator.setManaged(true);
        if (chatStatusLabel != null) chatStatusLabel.setText("● Typing...");

        executor.submit(() -> {
            String reply = botService.chat(text);
            Platform.runLater(() -> {
                typingIndicator.setVisible(false);
                typingIndicator.setManaged(false);
                if (chatStatusLabel != null) chatStatusLabel.setText("● Online");
                addBotMessage(reply);
                Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
                if (chatStage == null || !chatStage.isShowing()) {
                    unreadCount++;
                }
            });
        });
    }

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(3, 8, 3, 50));
        VBox bubble = new VBox(2);
        bubble.setMaxWidth(230);
        bubble.setAlignment(Pos.CENTER_RIGHT);
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setTextAlignment(TextAlignment.LEFT);
        msg.setStyle("-fx-background-color:#2d7a3a;-fx-text-fill:white;-fx-padding:9 13 9 13;" +
                "-fx-background-radius:18 18 4 18;-fx-font-size:13px;");
        Label time = new Label(LocalTime.now().format(TIME_FMT));
        time.setStyle("-fx-font-size:10px;-fx-text-fill:#aaa;");
        time.setAlignment(Pos.CENTER_RIGHT);
        bubble.getChildren().addAll(msg, time);
        row.getChildren().add(bubble);
        messagesContainer.getChildren().add(row);
    }

    private void addBotMessage(String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 50, 3, 8));
        Label avatar = new Label("🌿");
        avatar.setStyle("-fx-background-color:#e8f5e9;-fx-padding:5 6 5 6;-fx-background-radius:50;" +
                "-fx-font-size:14px;-fx-min-width:32;-fx-min-height:32;" +
                "-fx-max-width:32;-fx-max-height:32;-fx-alignment:CENTER;");
        VBox bubble = new VBox(2);
        bubble.setMaxWidth(230);
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setTextAlignment(TextAlignment.LEFT);
        msg.setStyle("-fx-background-color:#f1f8e9;-fx-text-fill:#1b3a1f;-fx-padding:9 13 9 13;" +
                "-fx-background-radius:18 18 18 4;-fx-border-color:#c8e6c9;" +
                "-fx-border-radius:18 18 18 4;-fx-border-width:1;-fx-font-size:13px;");
        Label time = new Label(LocalTime.now().format(TIME_FMT));
        time.setStyle("-fx-font-size:10px;-fx-text-fill:#aaa;");
        bubble.getChildren().addAll(msg, time);
        row.getChildren().addAll(avatar, bubble);
        messagesContainer.getChildren().add(row);
        FadeTransition ft = new FadeTransition(Duration.millis(280), row);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    private void clearNotification() {
        unreadCount = 0;
    }

    public static MainLayoutController getInstance() {
        return instance;
    }

    public void setCurrentUser(user user) {
        this.currentUser = user;
    }

    public user getCurrentUser() {
        return currentUser;
    }

    public void configureForUserRole(Role role) {
        hideAllButtons();

        switch (role) {
            case ROLE_ADMIN:
                showButton(homeBtn, homeLabel);
                showButton(usersBtn, usersLabel);
                break;

            case ROLE_GERANT:
                showButton(homeBtn, homeLabel);
                showButton(animalsBtn, animalsLabel);
                showButton(equipmentBtn, equipmentLabel);
                showButton(stockBtn, stockLabel);
                showButton(cultureBtn, cultureLabel);
                showButton(workersBtn, workersLabel);
                showButton(ouvrierBtn, ouvrierLabel);
                break;

            case ROLE_OUVRIER:
                showButton(homeBtn, homeLabel);
                homeLabel.setText("Mes Tâches");
                break;

            default:
                showButton(homeBtn, homeLabel);
                break;
        }

        ensureProfileLogoutVisible();
    }

    private void hideAllButtons() {
        animalsBtn.setVisible(false);   animalsBtn.setManaged(false);
        equipmentBtn.setVisible(false); equipmentBtn.setManaged(false);
        stockBtn.setVisible(false);     stockBtn.setManaged(false);
        cultureBtn.setVisible(false);   cultureBtn.setManaged(false);
        usersBtn.setVisible(false);     usersBtn.setManaged(false);
        workersBtn.setVisible(false);   workersBtn.setManaged(false);
        ouvrierBtn.setVisible(false);   ouvrierBtn.setManaged(false);
    }

    private void showButton(Button button, Label label) {
        button.setVisible(true);
        button.setManaged(true);

        if (sidebarCollapsed) {
            label.setVisible(false);
            label.setManaged(false);
        } else {
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private void ensureProfileLogoutVisible() {
        profileBtn.setVisible(true);
        profileBtn.setManaged(true);
        logoutBtn.setVisible(true);
        logoutBtn.setManaged(true);

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
            sidebar.setPrefWidth(280);
            sidebar.setMaxWidth(280);

            brandText.setVisible(true);
            brandText.setManaged(true);
            toggleText.setVisible(true);
            toggleText.setManaged(true);
            navTitle.setVisible(true);
            navTitle.setManaged(true);
            footerText.setText("Connected");

            showVisibleLabels();

            toggleIcon.setText("☰");
            sidebarCollapsed = false;

        } else {
            sidebar.setPrefWidth(80);
            sidebar.setMaxWidth(80);

            brandText.setVisible(false);
            brandText.setManaged(false);
            toggleText.setVisible(false);
            toggleText.setManaged(false);
            navTitle.setVisible(false);
            navTitle.setManaged(false);
            footerText.setText("•");

            hideAllLabels();

            toggleIcon.setText("»");
            sidebarCollapsed = true;
        }
    }

    private void showVisibleLabels() {
        if (homeBtn.isVisible())      { homeLabel.setVisible(true);      homeLabel.setManaged(true); }
        if (animalsBtn.isVisible())   { animalsLabel.setVisible(true);   animalsLabel.setManaged(true); }
        if (equipmentBtn.isVisible()) { equipmentLabel.setVisible(true); equipmentLabel.setManaged(true); }
        if (stockBtn.isVisible())     { stockLabel.setVisible(true);     stockLabel.setManaged(true); }
        if (cultureBtn.isVisible())   { cultureLabel.setVisible(true);   cultureLabel.setManaged(true); }
        if (usersBtn.isVisible())     { usersLabel.setVisible(true);     usersLabel.setManaged(true); }
        if (workersBtn.isVisible())   { workersLabel.setVisible(true);   workersLabel.setManaged(true); }
        if (ouvrierBtn.isVisible())   { ouvrierLabel.setVisible(true);   ouvrierLabel.setManaged(true); }
        profileLabel.setVisible(true); profileLabel.setManaged(true);
        logoutLabel.setVisible(true);  logoutLabel.setManaged(true);
    }

    private void hideAllLabels() {
        homeLabel.setVisible(false);      homeLabel.setManaged(false);
        animalsLabel.setVisible(false);   animalsLabel.setManaged(false);
        equipmentLabel.setVisible(false); equipmentLabel.setManaged(false);
        stockLabel.setVisible(false);     stockLabel.setManaged(false);
        cultureLabel.setVisible(false);   cultureLabel.setManaged(false);
        usersLabel.setVisible(false);     usersLabel.setManaged(false);
        workersLabel.setVisible(false);   workersLabel.setManaged(false);
        profileLabel.setVisible(false);   profileLabel.setManaged(false);
        logoutLabel.setVisible(false);    logoutLabel.setManaged(false);
        ouvrierLabel.setVisible(false);   ouvrierLabel.setManaged(false);
    }

    @FXML
    public void navigateToHome() {
        if (currentUser != null) {
            switch (currentUser.getRole()) {
                case ROLE_OUVRIER:
                    loadContent("/fxml/mes_taches.fxml");
                    break;
                case ROLE_ADMIN:
                case ROLE_GERANT:
                default:
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

    private static Stock stockToEdit;

    public static void setStockToEdit(Stock stock) {
        stockToEdit = stock;
    }

    public static Stock getStockToEdit() {
        return stockToEdit;
    }

    private static Produit produitToEdit;

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

    @FXML
    public void navigateToExchangeRate() {
        loadContent("/fxml/ExchangeRate.fxml");
        setActiveButton(exchangeRateBtn);
    }

    @FXML
    public void navigateToCommodityPrice() {
        loadContent("/fxml/CommodityPrice.fxml");
    }

    public void signalerNouvelleAlerte(int nbAlertes) {
        badgeFermeParUtilisateur = false;
        if (stockAlertBadge != null) {
            stockAlertBadge.setText(String.valueOf(nbAlertes));
            stockAlertBadge.setVisible(true);
            stockAlertBadge.setManaged(true);
        }
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

    @FXML
    public void navigateToAfficherCulture() {
        loadContent("/fxml/afficher_culture.fxml");
        setActiveButton(cultureBtn);
    }

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
        setActiveButton(evaluationBtn);
    }

    public void navigateToDashboardWorkers() {
        loadContent("/fxml/workers_dashboard.fxml");
        setActiveButton(workersBtn);
    }

    public void navigateToCalendar() {
        loadContent("/fxml/workers_calendar.fxml");
        setActiveButton(workersBtn);
    }

    @FXML
    public void navigateToProfile() {
        loadContent("/fxml/GerantProfile.fxml");
        setActiveButton(profileBtn);
    }

    public void loadFaceVerification() {
        loadContent("/fxml/FaceVerification.fxml");
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
                    SessionManager.getInstance().logout();

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
        for (Button btn : new Button[]{homeBtn, animalsBtn, equipmentBtn, stockBtn,
                cultureBtn, usersBtn, workersBtn, evaluationBtn, profileBtn, ouvrierBtn}) {
            if (btn != null) btn.getStyleClass().remove("active");
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
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