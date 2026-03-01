package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import services.AgriBotService;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ChatBotController — Facebook-Messenger-style floating chatbot
 * ══════════════════════════════════════════════════════════════
 * All styling is done inline (no external CSS dependency).
 * Hover effects are applied programmatically.
 */
public class ChatBotController {

    // ── FXML Injections ──────────────────────────────────────────────
    @FXML private VBox      chatPopup;
    @FXML private VBox      messagesContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField  messageInput;
    @FXML private Button     sendButton;
    @FXML private Button     minimizeButton;
    @FXML private Button     closeButton;
    @FXML private Button     floatingButton;
    @FXML private Label      notificationDot;
    @FXML private Label      chatStatusLabel;
    @FXML private HBox       typingIndicator;

    // ── Services & State ─────────────────────────────────────────────
    private final AgriBotService botService = new AgriBotService();
    private final ExecutorService executor  = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AgriBot-Thread");
        t.setDaemon(true);
        return t;
    });

    private boolean isOpen      = false;
    private boolean greeted     = false;
    private int     unreadCount = 0;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── Styles ───────────────────────────────────────────────────────
    private static final String FLOAT_BTN_NORMAL =
            "-fx-min-width:58px;-fx-min-height:58px;-fx-max-width:58px;-fx-max-height:58px;" +
                    "-fx-background-radius:50;-fx-border-radius:50;" +
                    "-fx-background-color:#2d7a3a;-fx-text-fill:white;-fx-font-size:22px;" +
                    "-fx-cursor:hand;-fx-border-width:0;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.35),10,0,0,3);";

    private static final String FLOAT_BTN_HOVER =
            "-fx-min-width:58px;-fx-min-height:58px;-fx-max-width:58px;-fx-max-height:58px;" +
                    "-fx-background-radius:50;-fx-border-radius:50;" +
                    "-fx-background-color:#1e5c28;-fx-text-fill:white;-fx-font-size:22px;" +
                    "-fx-cursor:hand;-fx-border-width:0;" +
                    "-fx-scale-x:1.08;-fx-scale-y:1.08;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),16,0,0,5);";

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

    private static final String CTRL_BTN_NORMAL =
            "-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;" +
                    "-fx-font-size:13px;-fx-min-width:28;-fx-min-height:28;" +
                    "-fx-max-width:28;-fx-max-height:28;-fx-background-radius:50;" +
                    "-fx-cursor:hand;-fx-border-width:0;-fx-padding:0;";

    private static final String CTRL_BTN_HOVER =
            "-fx-background-color:rgba(255,255,255,0.3);-fx-text-fill:white;" +
                    "-fx-font-size:13px;-fx-min-width:28;-fx-min-height:28;" +
                    "-fx-max-width:28;-fx-max-height:28;-fx-background-radius:50;" +
                    "-fx-cursor:hand;-fx-border-width:0;-fx-padding:0;";

    // ── Initialization ────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Start hidden
        chatPopup.setVisible(false);
        chatPopup.setManaged(false);
        chatPopup.setOpacity(0);

        // Hide notification dot
        notificationDot.setVisible(false);
        notificationDot.setManaged(false);

        // Hide typing indicator
        typingIndicator.setVisible(false);
        typingIndicator.setManaged(false);

        // Programmatic hover effects (inline CSS can't do :hover)
        applyHoverEffect(floatingButton, FLOAT_BTN_NORMAL, FLOAT_BTN_HOVER);
        applyHoverEffect(minimizeButton, CTRL_BTN_NORMAL,  CTRL_BTN_HOVER);
        applyHoverEffect(closeButton,    CTRL_BTN_NORMAL,  CTRL_BTN_HOVER);

        // Send button state management
        sendButton.setStyle(SEND_BTN_DISABLED);
        messageInput.textProperty().addListener((obs, old, nv) -> {
            boolean empty = nv == null || nv.trim().isEmpty();
            sendButton.setDisable(empty);
            sendButton.setStyle(empty ? SEND_BTN_DISABLED : SEND_BTN_NORMAL);
        });
        sendButton.setOnMouseEntered(e -> { if (!sendButton.isDisabled()) sendButton.setStyle(SEND_BTN_HOVER); });
        sendButton.setOnMouseExited(e  -> { if (!sendButton.isDisabled()) sendButton.setStyle(SEND_BTN_NORMAL); });

        // Send on Enter key
        messageInput.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleSend();
            }
        });
    }

    private void applyHoverEffect(Button btn, String normal, String hover) {
        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(normal));
    }

    // ── Toggle ────────────────────────────────────────────────────────
    @FXML
    public void toggleChat() {
        if (isOpen) minimizeChat();
        else        openChat();
    }

    @FXML
    public void openChat() {
        isOpen = true;
        clearNotification();

        chatPopup.setVisible(true);
        chatPopup.setManaged(true);

        ScaleTransition st = new ScaleTransition(Duration.millis(220), chatPopup);
        st.setFromX(0.75); st.setFromY(0.75);
        st.setToX(1.0);    st.setToY(1.0);

        FadeTransition ft = new FadeTransition(Duration.millis(220), chatPopup);
        ft.setFromValue(0); ft.setToValue(1);

        st.play(); ft.play();

        if (!greeted) {
            greeted = true;
            Platform.runLater(() -> addBotMessage(
                    "👋 Hi! I'm AgriBot, your AI farming assistant.\n\n" +
                            "Should I help you with something? 🌱\n" +
                            "Ask me about crops, soil, irrigation, livestock, or any farm topic!"
            ));
        }

        Platform.runLater(this::scrollToBottom);
        messageInput.requestFocus();
    }

    @FXML
    public void minimizeChat() {
        isOpen = false;
        FadeTransition ft = new FadeTransition(Duration.millis(180), chatPopup);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            chatPopup.setVisible(false);
            chatPopup.setManaged(false);
        });
        ft.play();
    }

    @FXML
    public void closeChat() {
        minimizeChat();
        clearNotification();
    }

    // ── Send ──────────────────────────────────────────────────────────
    @FXML
    public void handleSend() {
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;

        messageInput.clear();
        sendButton.setDisable(true);
        sendButton.setStyle(SEND_BTN_DISABLED);

        addUserMessage(text);
        scrollToBottom();
        showTyping(true);
        setChatStatus("● Typing...");

        executor.submit(() -> {
            String reply = botService.chat(text);
            Platform.runLater(() -> {
                showTyping(false);
                setChatStatus("● Online");
                addBotMessage(reply);
                scrollToBottom();
                if (!isOpen) showNotification();
            });
        });
    }

    // ── Message builders ──────────────────────────────────────────────

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
        msg.setStyle(
                "-fx-background-color:#2d7a3a;" +
                        "-fx-text-fill:white;" +
                        "-fx-padding:9 13 9 13;" +
                        "-fx-background-radius:18 18 4 18;" +
                        "-fx-font-size:13px;"
        );

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
        avatar.setStyle(
                "-fx-background-color:#e8f5e9;" +
                        "-fx-padding:5 6 5 6;" +
                        "-fx-background-radius:50;" +
                        "-fx-font-size:14px;" +
                        "-fx-min-width:32;-fx-min-height:32;" +
                        "-fx-max-width:32;-fx-max-height:32;" +
                        "-fx-alignment:CENTER;"
        );

        VBox bubble = new VBox(2);
        bubble.setMaxWidth(230);

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setTextAlignment(TextAlignment.LEFT);
        msg.setStyle(
                "-fx-background-color:#f1f8e9;" +
                        "-fx-text-fill:#1b3a1f;" +
                        "-fx-padding:9 13 9 13;" +
                        "-fx-background-radius:18 18 18 4;" +
                        "-fx-border-color:#c8e6c9;" +
                        "-fx-border-radius:18 18 18 4;" +
                        "-fx-border-width:1;" +
                        "-fx-font-size:13px;"
        );

        Label time = new Label(LocalTime.now().format(TIME_FMT));
        time.setStyle("-fx-font-size:10px;-fx-text-fill:#aaa;");

        bubble.getChildren().addAll(msg, time);
        row.getChildren().addAll(avatar, bubble);
        messagesContainer.getChildren().add(row);

        FadeTransition ft = new FadeTransition(Duration.millis(280), row);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void showTyping(boolean show) {
        typingIndicator.setVisible(show);
        typingIndicator.setManaged(show);
        if (show) scrollToBottom();
    }

    private void setChatStatus(String s) {
        if (chatStatusLabel != null) chatStatusLabel.setText(s);
    }

    private void showNotification() {
        unreadCount++;
        notificationDot.setVisible(true);
        notificationDot.setManaged(true);
        notificationDot.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
    }

    private void clearNotification() {
        unreadCount = 0;
        notificationDot.setVisible(false);
        notificationDot.setManaged(false);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}