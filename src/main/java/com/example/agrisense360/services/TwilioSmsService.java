package com.example.agrisense360.services;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

public class TwilioSmsService {

    private final String accountSid;
    private final String authToken;
    private final String defaultFromNumber;

    public TwilioSmsService() {
        Properties props = loadProperties();
        this.accountSid = readSetting("TWILIO_ACCOUNT_SID", "sms.twilio.accountSid", props);
        this.authToken = readSetting("TWILIO_AUTH_TOKEN", "sms.twilio.authToken", props);
        this.defaultFromNumber = props.getProperty("sms.twilio.fromNumber", "").trim();
    }

    public boolean isConfigured() {
        return accountSid != null && !accountSid.isBlank() && authToken != null && !authToken.isBlank();
    }

    public String getDefaultFromNumber() {
        return defaultFromNumber;
    }

    public String sendSms(String toNumber, String fromNumber, String message) throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IOException("Twilio credentials are missing. Set TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN (or sms.twilio.* in config)." );
        }
        if (toNumber == null || toNumber.isBlank()) {
            throw new IOException("Recipient number is required.");
        }
        if (message == null || message.isBlank()) {
            throw new IOException("SMS content is empty.");
        }

        String from = fromNumber != null && !fromNumber.isBlank() ? fromNumber.trim() : defaultFromNumber;
        if (from == null || from.isBlank()) {
            throw new IOException("Twilio sender number is required (provide From field or sms.twilio.fromNumber).");
        }

        String endpoint = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
        String form = "To=" + encode(toNumber.trim())
            + "&From=" + encode(from)
            + "&Body=" + encode(message);

        String basicAuth = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
            .header("Authorization", "Basic " + basicAuth)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(Duration.ofSeconds(15))
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            String messageText = "Twilio API error: " + response.statusCode();
            try {
                JSONObject error = new JSONObject(response.body());
                messageText = error.optString("message", messageText);
            } catch (Exception ignored) {
            }
            throw new IOException(messageText);
        }

        JSONObject result = new JSONObject(response.body());
        return result.optString("sid", "");
    }

    public JSONObject getMessageStatus(String messageSid) throws IOException, InterruptedException {
        if (messageSid == null || messageSid.isBlank()) {
            throw new IOException("Message SID is required.");
        }
        if (!isConfigured()) {
            throw new IOException("Twilio credentials are missing.");
        }

        String endpoint = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages/" + messageSid + ".json";
        String basicAuth = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
            .header("Authorization", "Basic " + basicAuth)
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            String messageText = "Twilio status API error: " + response.statusCode();
            try {
                JSONObject error = new JSONObject(response.body());
                messageText = error.optString("message", messageText);
            } catch (Exception ignored) {
            }
            throw new IOException(messageText);
        }

        return new JSONObject(response.body());
    }

    private String readSetting(String envName, String configName, Properties props) {
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return props.getProperty(configName, "").trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = TwilioSmsService.class.getResourceAsStream("/config.properties")) {
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
