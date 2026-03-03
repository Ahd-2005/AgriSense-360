package com.example.agrisense360.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public class BrevoEmailService {

    private static final String BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailService() {
        Properties properties = loadProperties();
        String envKey = System.getenv("BREVO_API_KEY");
        this.apiKey = envKey != null && !envKey.isBlank()
            ? envKey.trim()
            : properties.getProperty("email.brevoApiKey", "").trim();
        this.senderEmail = properties.getProperty("email.sender", "").trim();
        this.senderName = properties.getProperty("email.senderName", "AgriSense AI").trim();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && senderEmail != null && !senderEmail.isBlank();
    }

    public String sendEmail(String toEmail, String subject, String htmlContent, String textContent) throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IOException("Email API is not configured. Set BREVO_API_KEY/email.brevoApiKey and email.sender.");
        }
        if (toEmail == null || toEmail.isBlank()) {
            throw new IOException("Recipient email is required.");
        }

        JSONObject payload = new JSONObject()
            .put("sender", new JSONObject()
                .put("name", senderName == null || senderName.isBlank() ? "AgriSense AI" : senderName)
                .put("email", senderEmail))
            .put("to", new JSONArray().put(new JSONObject().put("email", toEmail.trim())))
            .put("subject", subject == null || subject.isBlank() ? "AgriSense AI Daily Briefing" : subject.trim())
            .put("htmlContent", htmlContent == null ? "" : htmlContent)
            .put("textContent", textContent == null ? "" : textContent);

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(BREVO_ENDPOINT))
            .header("accept", "application/json")
            .header("content-type", "application/json")
            .header("api-key", apiKey)
            .timeout(Duration.ofSeconds(18))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            String message = "Brevo API error: " + response.statusCode();
            try {
                JSONObject body = new JSONObject(response.body());
                message = body.optString("message", message);
            } catch (Exception ignored) {
            }
            throw new IOException(message);
        }

        JSONObject result = new JSONObject(response.body());
        return result.optString("messageId", "");
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = BrevoEmailService.class.getResourceAsStream("/config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
        }

        Path localConfig = Path.of("src", "main", "resources", "config.local.properties");
        if (Files.exists(localConfig)) {
            try (InputStream input = Files.newInputStream(localConfig)) {
                properties.load(input);
            } catch (IOException ignored) {
            }
        }
        return properties;
    }
}
