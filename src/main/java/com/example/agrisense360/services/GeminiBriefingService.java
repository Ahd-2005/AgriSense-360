package com.example.agrisense360.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

public class GeminiBriefingService {

    private static final String DEFAULT_MODEL = "llama-3.1-8b-instant";
    private static final String GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private String resolvedModel;

    public GeminiBriefingService() {
        Properties props = loadProperties();
        String fromEnv = System.getenv("GROQ_API_KEY");
        String fromConfig = props.getProperty("ai.groqApiKey", "").trim();
        this.apiKey = fromEnv != null && !fromEnv.isBlank() ? fromEnv.trim() : fromConfig;
        this.model = props.getProperty("ai.groqModel", DEFAULT_MODEL).trim();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String generateDailyBriefing(String contextJson) throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IOException("Groq API key is missing. Set GROQ_API_KEY or ai.groqApiKey in config.properties.");
        }

        List<String> candidates = buildModelCandidates();
        String lastError = null;
        for (String candidate : candidates) {
            try {
                String text = generateWithModel(candidate, contextJson);
                resolvedModel = candidate;
                return text;
            } catch (IOException e) {
                lastError = e.getMessage();
                if (!isModelCompatibilityError(lastError)) {
                    throw e;
                }
            }
        }

        throw new IOException("No compatible Groq model found for this API key. Last error: " + (lastError != null ? lastError : "unknown") + ". Set ai.groqModel to an available model like llama-3.1-8b-instant.");
    }

    private String generateWithModel(String modelName, String contextJson) throws IOException, InterruptedException {
        String modelToUse = modelName == null || modelName.isBlank() ? DEFAULT_MODEL : modelName;

        String prompt = """
You are an agricultural risk assistant.
Given the JSON context, write a practical briefing for today.
Prioritize cross-signal patterns between weather, equipment status, maintenance schedule, and motion events.
If there is a conflict (for example active heavy equipment but risky weather, or maintenance due soon under harsh weather), call it out clearly.
Find loopholes and weak points (coverage gaps, overdue maintenance pressure, off-hours activity, weak fleet availability) whenever data supports it.
Go beyond listing facts: infer likely causes, operational implications, and practical opportunities.

Output rules:
    - 1 short paragraph executive summary (max 90 words)
- then section title: Alerts Today
    - then up to 4 bullet points (each starts with "- ")
- then section title: Cross-Signal Patterns
    - then up to 4 bullet points, each must link at least 2 domains (weather/equipment/maintenance/camera)
- then section title: Loopholes & Opportunities
    - 2 to 4 bullet points describing gaps, root causes, and one tactical opportunity
- then section title: Recommended Actions
    - then up to 5 bullet points (each starts with "- ")
- explicitly mention if maintenance_summary.today_count > 0 using maintenance_today details
- include overall camera activity recap using all motion severities (HIGH/MEDIUM/LOW), not only high
- mention at least one weather-motion correlation insight if weather_motion_correlation is available
- use operational_loopholes if present and avoid generic wording
- include at least one idea that improves efficiency/cost or preventive reliability
- include maintenance reminders if upcoming_maintenance is not empty
- do not invent data, only use provided context

Context JSON:
""" + contextJson;

        JSONObject body = new JSONObject()
        .put("model", modelToUse)
        .put("messages", new JSONArray()
            .put(new JSONObject().put("role", "system").put("content", "You are a precise agricultural risk assistant."))
            .put(new JSONObject().put("role", "user").put("content", prompt)))
        .put("temperature", 0.25)
        .put("max_tokens", 420);

    HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    HttpRequest request = HttpRequest.newBuilder(URI.create(GROQ_ENDPOINT))
                .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
        String message = "Groq API error: " + response.statusCode();
            try {
                JSONObject err = new JSONObject(response.body()).optJSONObject("error");
                if (err != null) {
                    message = err.optString("message", message);
                }
            } catch (Exception ignored) {
            }
            throw new IOException(message);
        }

        JSONObject result = new JSONObject(response.body());
        JSONArray candidates = result.optJSONArray("candidates");
        if (candidates == null) {
            candidates = result.optJSONArray("choices");
        }
        if (candidates == null || candidates.isEmpty()) {
            throw new IOException("Groq returned no choices.");
        }

        JSONObject first = candidates.getJSONObject(0);
        JSONObject messageObj = first.optJSONObject("message");
        String text = messageObj != null ? messageObj.optString("content", "").trim() : "";
        if (text.isBlank()) {
            throw new IOException("Groq returned empty briefing.");
        }

        return text;
    }

    private List<String> buildModelCandidates() {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (resolvedModel != null && !resolvedModel.isBlank()) {
            candidates.add(resolvedModel);
        }
        if (model != null && !model.isBlank()) {
            candidates.add(model);
        }
        candidates.add(DEFAULT_MODEL);
        candidates.add("llama-3.3-70b-versatile");
        candidates.add("llama-3.1-70b-versatile");
        candidates.add("gemma2-9b-it");
        candidates.add("mixtral-8x7b-32768");
        return new ArrayList<>(candidates);
    }

    private boolean isModelCompatibilityError(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("not found")
            || normalized.contains("model_decommissioned")
            || normalized.contains("does not exist")
                || normalized.contains("no compatible")
                || normalized.contains("404");
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = GeminiBriefingService.class.getResourceAsStream("/config.properties")) {
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
