package services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * AgriBotService — Groq API (free, fast, reliable)
 * ─────────────────────────────────────────────────
 * Model: llama-3.3-70b-versatile (same as AgendaCulture.html)
 *
 * HOW TO GET YOUR FREE KEY:
 *   1. Go to https://console.groq.com
 *   2. Sign in → API Keys → Create API Key
 *   3. Add to config.properties:
 *        groq.apiKey=gsk_xxxxxxxxxxxxxxxx
 */
public class AgriBotService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama-3.3-70b-versatile";

    private static final String SYSTEM_PROMPT =
            "You are AgriBot, an expert AI agricultural assistant for AgriSense 360, a smart farm management platform. " +
                    "Your expertise covers: crop management, soil health, irrigation scheduling, pest & disease control, " +
                    "harvest timing, livestock care, equipment maintenance, fertilization, weather impact on farming, " +
                    "sustainable farming practices, and farm financial planning. " +
                    "Always give practical, actionable advice. Be concise (2-4 sentences unless more detail is needed). " +
                    "Use farming emojis occasionally to stay friendly (🌱🌾🚜💧🌿🐄). " +
                    "If asked about non-agriculture topics, politely say you specialize in farming. " +
                    "Reply in the same language the user writes in (French or English).";

    private final String apiKey;
    private final List<String[]> history = new ArrayList<>();

    public AgriBotService() {
        this.apiKey = loadApiKey();
    }

    private String loadApiKey() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
                String key = props.getProperty("groq.api.key", "").trim();
                if (!key.isEmpty() && !key.equals("gsk_YOUR_KEY_HERE")) {
                    return key;
                }
            }
        } catch (IOException e) {
            System.err.println("[AgriBotService] Could not read config.properties: " + e.getMessage());
        }
        System.err.println("[AgriBotService] WARNING: groq.apiKey not set in config.properties!");
        return "MISSING_KEY";
    }

    public String chat(String userMessage) {
        if ("MISSING_KEY".equals(apiKey)) {
            return "⚠️ API key not configured.\n\nAdd this to config.properties:\n  groq.apiKey=gsk_...\n\nGet a FREE key at:\nhttps://console.groq.com";
        }

        history.add(new String[]{"user", userMessage});

        try {
            String body = buildRequestBody();

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",  "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(40000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                String reply = parseReply(sb.toString());
                history.add(new String[]{"assistant", reply});
                return reply;
            } else {
                StringBuilder sb = new StringBuilder();
                InputStream err = conn.getErrorStream();
                if (err != null) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(err, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                }
                System.err.println("[AgriBotService] HTTP " + code + ": " + sb);
                history.remove(history.size() - 1);
                return getFallback(code);
            }

        } catch (Exception e) {
            e.printStackTrace();
            history.remove(history.size() - 1);
            return "⚠️ Connection error. Please check your internet connection.";
        }
    }

    private String buildRequestBody() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(MODEL).append("\",\"messages\":[");
        sb.append("{\"role\":\"system\",\"content\":\"").append(escapeJson(SYSTEM_PROMPT)).append("\"}");
        for (String[] turn : history) {
            sb.append(",{\"role\":\"").append(turn[0]).append("\",\"content\":\"")
                    .append(escapeJson(turn[1])).append("\"}");
        }
        sb.append("],\"max_tokens\":512,\"temperature\":0.7}");
        return sb.toString();
    }

    private String parseReply(String json) {
        int idx = json.indexOf("\"content\":");
        if (idx == -1) return "🌾 I couldn't process that. Please try again.";
        int start = json.indexOf("\"", idx + 10) + 1;
        int end = start;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
            end++;
        }
        if (start <= 0 || end <= start) return "🌾 Empty response. Please try again.";
        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\\\", "\\")
                .replace("\\t", "  ");
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String getFallback(int code) {
        if (code == 401) return "🔑 Invalid Groq API key.\nGet a free key at: https://console.groq.com";
        if (code == 429) return "⏳ Rate limit. Please wait a moment and try again.";
        return "⚠️ Service error (HTTP " + code + "). Please try again.";
    }

    public void clearHistory() { history.clear(); }
}