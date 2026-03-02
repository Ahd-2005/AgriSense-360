package services;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import services.StockAlertService;

import java.util.List;

public class NotificationService {

    private static NotificationService instance;

    // Référence au conteneur racine de l'app (StackPane du contentArea ou root)
    private StackPane rootPane;

    // Barre de notification en bas
    private HBox notifBar;
    private Label notifLabel;

    private NotificationService() {}

    public static NotificationService getInstance() {
        if (instance == null) instance = new NotificationService();
        return instance;
    }

    /**
     * À appeler une seule fois depuis MainLayoutController.initialize()
     * en passant le StackPane racine de l'application.
     */
    public void init(StackPane root) {
        this.rootPane = root;
        creerBarreNotification();
    }

    private void creerBarreNotification() {
        notifBar = new HBox(12);
        notifBar.setAlignment(Pos.CENTER_LEFT);
        notifBar.setPadding(new Insets(14, 20, 14, 20));
        notifBar.setStyle(
            "-fx-background-color: #b71c1c;" +
            "-fx-background-radius: 10px 10px 0 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, -4);"
        );
        notifBar.setMaxHeight(56);
        notifBar.setVisible(false);
        notifBar.setManaged(false);
        notifBar.setOpacity(0);

        Label icon = new Label("⚠️");
        icon.setStyle("-fx-font-size: 18px;");

        notifLabel = new Label("");
        notifLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");
        notifLabel.setWrapText(false);

        Label closeBtn = new Label("  ✕");
        closeBtn.setStyle("-fx-font-size: 16px; -fx-text-fill: rgba(255,255,255,0.7); -fx-cursor: hand;");
        closeBtn.setOnMouseClicked(e -> masquerBarre());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        notifBar.getChildren().addAll(icon, notifLabel, spacer, closeBtn);

        // Positionner en bas du StackPane
        StackPane.setAlignment(notifBar, Pos.BOTTOM_CENTER);
        rootPane.getChildren().add(notifBar);
    }

    /**
     * Affiche la barre de notification in-app.
     */
    public void afficherBarre(String message) {
        Platform.runLater(() -> {
            if (notifBar == null || rootPane == null) return;

            notifLabel.setText(message);
            notifBar.setVisible(true);
            notifBar.setManaged(true);

            // Fade in
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), notifBar);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            // Pause 6 secondes
            PauseTransition pause = new PauseTransition(Duration.seconds(6));

            // Fade out
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), notifBar);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> masquerBarre());

            new SequentialTransition(fadeIn, pause, fadeOut).play();
        });
    }

    private void masquerBarre() {
        if (notifBar != null) {
            notifBar.setVisible(false);
            notifBar.setManaged(false);
            notifBar.setOpacity(0);
        }
    }

    /**
     * Envoie une notification Windows native (toast) via PowerShell.
     * Fonctionne sur Windows 10/11 sans dépendance externe.
     */
    public void envoyerToastWindows(String titre, String message) {
        new Thread(() -> {
            try {
                // Script PowerShell pour toast Windows natif
                String script =
                    "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType=WindowsRuntime] | Out-Null\n" +
                    "[Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType=WindowsRuntime] | Out-Null\n" +
                    "$template = [Windows.UI.Notifications.ToastTemplateType]::ToastText02\n" +
                    "$xml = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent($template)\n" +
                    "$nodes = $xml.GetElementsByTagName('text')\n" +
                    "$nodes.Item(0).AppendChild($xml.CreateTextNode('" + escapePs(titre) + "')) | Out-Null\n" +
                    "$nodes.Item(1).AppendChild($xml.CreateTextNode('" + escapePs(message) + "')) | Out-Null\n" +
                    "$notif = [Windows.UI.Notifications.ToastNotification]::new($xml)\n" +
                    "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('AgriSense 360').Show($notif)";

                ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NonInteractive", "-NoProfile", "-Command", script
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.waitFor();
            } catch (Exception e) {
                System.err.println("Erreur toast Windows : " + e.getMessage());
            }
        }, "ToastThread").start();
    }

    /**
     * À appeler après chaque changement de vue pour garder la barre au-dessus.
     */
    public void bringToFront() {
        if (notifBar != null && rootPane != null) {
            rootPane.getChildren().remove(notifBar);
            rootPane.getChildren().add(notifBar);
        }
    }

    private String escapePs(String s) {
        return s.replace("'", "''").replace("\n", " ");
    }

    /**
     * Méthode principale : envoie les deux types de notification pour les stocks en alerte.
     */
    public void notifierStocksEnAlerte(List<StockAlertService.StockAlert> alertes) {
        if (alertes.isEmpty()) return;

        // Message résumé
        String resume;
        if (alertes.size() == 1) {
            StockAlertService.StockAlert a = alertes.get(0);
            resume = "⚠️ " + a.nomProduit + " : stock bas (" + a.stock.getQuantiteActuelle()
                + " / seuil " + a.stock.getSeuilAlerte() + ")";
        } else {
            resume = "⚠️ " + alertes.size() + " produits sous le seuil d'alerte !";
        }

        // 1. Barre in-app
        afficherBarre(resume);

        // 2. Toast Windows
        String titreToast = "AgriSense 360 — Alerte Stock";
        String msgToast;
        if (alertes.size() == 1) {
            StockAlertService.StockAlert a = alertes.get(0);
            msgToast = a.nomProduit + " : " + a.stock.getQuantiteActuelle()
                + " " + (a.stock.getUniteMesure() != null ? a.stock.getUniteMesure() : "")
                + " (seuil : " + a.stock.getSeuilAlerte() + ")";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(alertes.size(), 3); i++) {
                sb.append(alertes.get(i).nomProduit).append(", ");
            }
            if (alertes.size() > 3) sb.append("...");
            msgToast = alertes.size() + " produits : " + sb.toString().replaceAll(", $", "");
        }
        envoyerToastWindows(titreToast, msgToast);
    }
}
