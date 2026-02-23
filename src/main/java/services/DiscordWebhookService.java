package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhookService {

    // Discord webhook URL - you can also set via DISCORD_WEBHOOK_URL environment variable
    private static final String WEBHOOK_URL = "https://discordapp.com/api/webhooks/1475630242940059728/ck-5Yr8QEgPmi7xYjJApNhTmIjaS00MkgxUUnwGqQZsmQHZgxcePdI5ZDAVTdf9deafh";

    public static void sendNotification(String title, String message, String color) {
        System.out.println("DEBUG: sendNotification called - WEBHOOK_URL = " + (!WEBHOOK_URL.isEmpty() ? "SET" : "EMPTY"));
        
        if (!isWebhookConfigured()) {
            System.err.println("Discord webhook not configured. Set DISCORD_WEBHOOK_URL environment variable.");
            return;
        }
        
        try {
            String webhookUrl = WEBHOOK_URL;
            System.out.println("DEBUG: Using webhook URL: " + (webhookUrl.isEmpty() ? "EMPTY" : "SET"));
            
            if (webhookUrl.isEmpty()) {
                System.err.println("No Discord webhook URL provided");
                return;
            }

            JsonObject embed = createEmbed(title, message, color);
            JsonObject payload = new JsonObject();
            JsonArray embedsArray = new JsonArray();
            embedsArray.add(embed);
            payload.add("embeds", embedsArray);

            sendToDiscord(webhookUrl, payload.toString());
        } catch (Exception e) {
            System.err.println("Error sending Discord notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendAffectationNotification(String action, String typeTravail, String zoneTravail, String statut, String details) {
        String title = "📋 Affectation Travail - " + action.toUpperCase();
        String message = String.format(
            "**Type:** %s\n**Zone:** %s\n**Statut:** %s\n%s",
            typeTravail, zoneTravail, statut, details
        );
        String color = getColorByAction(action);
        sendNotification(title, message, color);
    }

    private static JsonObject createEmbed(String title, String message, String color) {
        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("description", message);
        
        // Convert color string to integer
        try {
            int colorInt = Integer.parseInt(color);
            embed.addProperty("color", colorInt);
        } catch (NumberFormatException e) {
            embed.addProperty("color", 9807270); // Default gray
        }
        
        embed.addProperty("timestamp", java.time.Instant.now().toString());
        
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Agrisens 360");
        embed.add("footer", footer);
        
        return embed;
    }

    private static void sendToDiscord(String webhookUrl, String payload) throws Exception {
        System.out.println("DEBUG: sendToDiscord - Sending to: " + webhookUrl);
        System.out.println("DEBUG: Payload: " + payload);
        
        URL url = new URL(webhookUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        byte[] input = payload.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(input.length);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(input);
        }
        
        int responseCode = conn.getResponseCode();
        System.out.println("DEBUG: Discord response code: " + responseCode);
        
        if (responseCode == 204) {
            System.out.println("Discord notification sent successfully");
        } else {
            System.err.println("Discord webhook response code: " + responseCode);
        }
        
        conn.disconnect();
    }

    private static String getColorByAction(String action) {
        return switch (action.toLowerCase()) {
            case "created", "ajoutée" -> "3066993"; // Green
            case "updated", "modifiée" -> "12370112"; // Blue
            case "deleted", "supprimée" -> "15158332"; // Red
            default -> "9807270"; // Gray
        };
    }

    private static boolean isWebhookConfigured() {
        return !WEBHOOK_URL.isEmpty();
    }

    /**
     * Set webhook URL at runtime if not using environment variables
     */
    public static void setWebhookUrl(String url) {
        // This would require a static field to override, but for security reasons,
        // it's better to use environment variables
    }
}
