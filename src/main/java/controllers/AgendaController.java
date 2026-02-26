package controllers;

import entity.Culture;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import services.CultureService;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AgendaController
 *
 * Ouvre une fenêtre WebView qui charge AgendaCulture.html
 * et injecte les cultures réelles depuis la base de données.
 */
public class AgendaController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Point d'entrée : appelé depuis CultureController.openAgenda()
     * Ouvre la fenêtre agenda modale avec les cultures de la BD.
     */
    public static void open(Stage ownerStage) {
        // ── Charger les cultures depuis la BD ──────────────────────────
        CultureService service = new CultureService();
        List<Culture> cultures;
        try {
            cultures = service.getAllCultures();
        } catch (SQLException e) {
            e.printStackTrace();
            cultures = new java.util.ArrayList<>();
        }

        // ── Construire le JSON des cultures ───────────────────────────
        StringBuilder json = buildCulturesJson(cultures);

        // ── Créer la Stage ─────────────────────────────────────────────
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        if (ownerStage != null) stage.initOwner(ownerStage);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("📅 Agenda des Cultures");
        stage.setWidth(1200);
        stage.setHeight(800);
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        // ── Header JavaFX ──────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #1e3c2c, #2a5a3a);");

        Label title = new Label("📅 Agenda des Cultures");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕ Fermer");
        closeBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15);" +
                        "-fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 8;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 16;"
        );
        closeBtn.setOnAction(e -> stage.close());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.25);" +
                        "-fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-border-color: rgba(255,255,255,0.5); -fx-border-radius: 8;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 16;"
        ));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15);" +
                        "-fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 8;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 16;"
        ));

        header.getChildren().addAll(title, spacer, closeBtn);

        // ── WebView ────────────────────────────────────────────────────
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        VBox.setVgrow(webView, Priority.ALWAYS);

        // Charger le fichier HTML depuis les ressources
        URL htmlUrl = AgendaController.class.getResource("/agenda/AgendaCulture.html");
        if (htmlUrl != null) {
            engine.load(htmlUrl.toExternalForm());
        } else {
            // Fallback : afficher un message d'erreur
            engine.loadContent("<h2 style='font-family:sans-serif;color:red;padding:40px'>❌ Fichier AgendaCulture.html introuvable dans /resources/agenda/</h2>");
        }

        // Injecter les données réelles une fois la page chargée
        final String culturesJsonStr = json.toString();
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                // Injecter les cultures depuis la BD dans la page HTML
                String script = "if(typeof loadFromJava === 'function') { loadFromJava(" + culturesJsonStr + "); }";
                engine.executeScript(script);
            }
        });

        // ── Layout ────────────────────────────────────────────────────
        VBox root = new VBox(0, header, webView);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Construit un tableau JSON à partir des cultures de la BD.
     * Ex: [{"nom":"Tomates","type":"Légumes","plantation":"2024-03-15"}, ...]
     */
    private static StringBuilder buildCulturesJson(List<Culture> cultures) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cultures.size(); i++) {
            Culture c = cultures.get(i);
            String plantation = c.getDatePlantation().toLocalDate().format(FMT);
            sb.append("{")
                    .append("\"nom\":\"").append(escapeJson(c.getNom())).append("\",")
                    .append("\"type\":\"").append(escapeJson(c.getTypeCulture())).append("\",")
                    .append("\"plantation\":\"").append(plantation).append("\"")
                    .append("}");
            if (i < cultures.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}