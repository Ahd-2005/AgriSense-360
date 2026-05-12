package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.SessionManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LandingPage {

    @FXML private Label quoteText;
    @FXML private Label quoteAuthor;
    @FXML private VBox quoteCard;
    @FXML private Button refreshBtn;

    @FXML
    public void initialize() {
        // Check if user is already logged in
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            redirectToMainLayout();
        }

        // Load motivational quote from ZenQuotes API in background thread
        fetchQuote();
    }

    /**
     * Fetches a random motivational quote from ZenQuotes API.
     * Runs on a background thread so the UI stays responsive.
     */
    private void fetchQuote() {
        // Disable refresh button while loading
        if (refreshBtn != null) {
            refreshBtn.setDisable(true);
        }

        Thread thread = new Thread(() -> {
            try {
                URL url = new URL("https://zenquotes.io/api/random");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "AgriSense360App");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    // Parse JSON manually (no extra lib needed)
                    // Response format: [{"q":"quote text","a":"author","h":"<blockquote>..."}]
                    String json = sb.toString();
                    String quote = extractJsonField(json, "q");
                    String author = extractJsonField(json, "a");

                    Platform.runLater(() -> {
                        quoteText.setText(quote != null ? quote : "Every day is a new opportunity to grow.");
                        quoteAuthor.setText("— " + (author != null ? author : "Unknown"));
                        if (refreshBtn != null) refreshBtn.setDisable(false);
                    });
                } else {
                    setFallbackQuote();
                }
                conn.disconnect();
            } catch (Exception e) {
                // Silently fallback on timeout or network error
                setFallbackQuote();
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Minimal JSON field extractor — avoids adding a JSON library dependency.
     */
    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end)
                .replace("\\u0027", "'")
                .replace("\\u0026", "&")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    /**
     * Shows a hardcoded fallback quote if the API is unreachable.
     */
    private void setFallbackQuote() {
        Platform.runLater(() -> {
            quoteText.setText("The best time to plant a tree was 20 years ago. The second best time is now.");
            quoteAuthor.setText("— Proverbe Chinois");
            if (refreshBtn != null) refreshBtn.setDisable(false);
        });
    }

    @FXML
    private void refreshQuote(ActionEvent event) {
        quoteText.setText("Chargement...");
        quoteAuthor.setText("— ...");
        fetchQuote();
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Connexion - AgriSense 360");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page de connexion: " + e.getMessage());
        }
    }

    @FXML
    private void goToSignup(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/signup.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Créer un compte - AgriSense 360");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page d'inscription: " + e.getMessage());
        }
    }

    private void redirectToMainLayout() {
        try {
            SessionManager sessionManager = SessionManager.getInstance();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
            Parent root = loader.load();

            MainLayoutController mainController = loader.getController();
            mainController.configureForUserRole(sessionManager.getCurrentUser().getRole());
            mainController.setCurrentUser(sessionManager.getCurrentUser());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}