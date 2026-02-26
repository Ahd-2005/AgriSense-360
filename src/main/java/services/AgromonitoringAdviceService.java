package services;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

/**
 * Fetches actionable agricultural advice using the Agromonitoring API (real external API).
 *
 * API key resolution order:
 *  1) Environment variable AGROMONITORING_API_KEY
 *  2) Local properties file %USERPROFILE%\.agrisense\api-keys.properties with key agromonitoring.apiKey
 *
 * Coordinates (lat/lon) resolution order:
 *  1) Env AGRI_LAT / AGRI_LON (double)
 *  2) src/main/resources/config.properties advice.lat / advice.lon (if present)
 *  3) Defaults: Tunis, Tunisia (36.8065, 10.1815)
 *
 * Endpoints used (public Agromonitoring):
 *  - Weather: https://api.agromonitoring.com/agro/1.0/weather?lat={lat}&lon={lon}&appid={API_KEY}
 *  - Soil (optional): https://api.agromonitoring.com/agro/1.0/soil?lat={lat}&lon={lon}&appid={API_KEY}
 *
 * No WebView is used; this relies on Java HttpClient and org.json only.
 */
public class AgromonitoringAdviceService {

    public static class Advice {
        public final String text;
        public final String source;
        public Advice(String text, String source) {
            this.text = text;
            this.source = source;
        }
    }

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public Optional<Advice> fetchAdvice() {
        try {
            // 40% chance to return a general crop advice (no API dependency)
            if (Math.random() < 0.4) {
                String tip = randomGeneralTip();
                return Optional.of(new Advice(tip, "Conseil Général"));
            }

            String apiKey = resolveApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                return Optional.of(new Advice(
                        "Clé API Agromonitoring manquante. Veuillez la configurer pour obtenir des conseils météo/sol.",
                        "Agromonitoring"));
            }
            double lat = resolveLat();
            double lon = resolveLon();

            JSONObject weather = fetchJson("https://api.agromonitoring.com/agro/1.0/weather?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey);
            JSONObject soil = fetchJson("https://api.agromonitoring.com/agro/1.0/soil?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey);

            String advice = buildAdviceFrom(weather, soil);
            String where = String.format("%.4f, %.4f", lat, lon);
            return Optional.of(new Advice(advice, "Agromonitoring • " + where));
        } catch (Exception e) {
            // On any error, still try to provide a general tip so the UI shows something useful
            return Optional.of(new Advice(randomGeneralTip(), "Conseil Général"));
        }
    }

    private JSONObject fetchJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 200 && resp.statusCode() < 300 && resp.body() != null && !resp.body().isBlank()) {
            try {
                return new JSONObject(resp.body());
            } catch (Exception ex) {
                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    private String buildAdviceFrom(JSONObject weather, JSONObject soil) {
        // Weather fields
        double tempC = kelvinToC(weather.optJSONObject("main") != null ? weather.optJSONObject("main").optDouble("temp", Double.NaN) : Double.NaN);
        double humidity = weather.optJSONObject("main") != null ? weather.optJSONObject("main").optDouble("humidity", Double.NaN) : Double.NaN;
        double wind = weather.optJSONObject("wind") != null ? weather.optJSONObject("wind").optDouble("speed", Double.NaN) : Double.NaN;
        String desc = weather.optJSONArray("weather") != null && weather.optJSONArray("weather").length() > 0
                ? weather.optJSONArray("weather").optJSONObject(0).optString("description", null) : null;
        double rain1h = weather.optJSONObject("rain") != null ? weather.optJSONObject("rain").optDouble("1h", 0.0) : 0.0;

        // Soil fields
        double soilMoist = soil.optDouble("moisture", Double.NaN); // volumetric water content m3/m3
        double soilT = soil.optDouble("t0", Double.NaN); // top soil temp C (if provided)

        StringBuilder tip = new StringBuilder();
        if (!Double.isNaN(tempC)) {
            if (tempC >= 30) tip.append("Chaleur élevée (" + round(tempC) + "°C): arrosez en fin de journée et paillez pour limiter l'évaporation. ");
            else if (tempC <= 5) tip.append("Température basse (" + round(tempC) + "°C): protégez les jeunes plants (voile, serre) et évitez les transplantations. ");
            else tip.append("Température actuelle ~" + round(tempC) + "°C: adaptez l'irrigation selon les besoins des cultures. ");
        }
        if (!Double.isNaN(humidity)) {
            if (humidity >= 85) tip.append("Humidité élevée (" + (int) humidity + "%): surveillez mildiou/oidium, aérez et espacez l'arrosage. ");
            else if (humidity <= 35) tip.append("Air sec (" + (int) humidity + "%): privilégiez un arrosage plus long et paillage. ");
        }
        if (!Double.isNaN(wind) && wind >= 8) {
            tip.append("Vent fort (" + round(wind) + " m/s): installez des brise-vent et évitez traitements foliaires. ");
        }
        if (rain1h > 0.5) {
            tip.append("Pluie récente (" + round(rain1h) + " mm): réduisez l'irrigation aujourd'hui. ");
        }
        if (!Double.isNaN(soilMoist)) {
            if (soilMoist < 0.15) tip.append("Sol sec (humidité « " + round(soilMoist) + " m³/m³ »): planifiez un arrosage ciblé (goutte-à-goutte). ");
            else if (soilMoist > 0.40) tip.append("Sol très humide (" + round(soilMoist) + "): évitez le tassement, limitez le passage d'engins. ");
        }
        if (!Double.isNaN(soilT)) {
            if (soilT < 10) tip.append("Sol froid (" + round(soilT) + "°C): retarder semis sensibles et favoriser paillage sombre. ");
            else if (soilT > 25) tip.append("Sol chaud (" + round(soilT) + "°C): arrosage en profondeur et paillage clair conseillés. ");
        }

        String headline = tip.toString().trim();
        if (headline.isEmpty()) headline = "Conseil: surveillez l'humidité du sol et adaptez l'irrigation selon la météo locale.";

        // Build a compact metrics JSON so the UI can render colored chips
        JSONObject metrics = new JSONObject();
        if (!Double.isNaN(tempC)) metrics.put("tempC", roundDouble(tempC));
        if (!Double.isNaN(humidity)) metrics.put("humidity", Math.round(humidity));
        if (!Double.isNaN(wind)) metrics.put("wind", roundDouble(wind));
        if (rain1h > 0) metrics.put("rain1h", roundDouble(rain1h));
        if (!Double.isNaN(soilMoist)) metrics.put("soilMoist", roundDouble(soilMoist));
        if (!Double.isNaN(soilT)) metrics.put("soilT", roundDouble(soilT));
        if (desc != null) metrics.put("desc", desc);

        return headline.replaceAll("\\s+", " ").trim() + " __METRICS__=" + metrics.toString();
    }

    private static double kelvinToC(double k) {
        if (Double.isNaN(k)) return Double.NaN;
        return k - 273.15;
    }

    private static String resolveApiKey() {
        // 1) Environment variable
        String fromEnv = System.getenv("AGROMONITORING_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();

        // 2) Project config (classpath: /config.properties), which is gitignored in this repo
        String fromConfig = readStringFromConfig("agromonitoring.apiKey");
        if (fromConfig != null && !fromConfig.isBlank()) return fromConfig.trim();

        // 3) User-level properties in home directory
        try {
            Path p = Path.of(System.getProperty("user.home"), ".agrisense", "api-keys.properties");
            if (Files.exists(p)) {
                Properties props = new Properties();
                try (var in = Files.newInputStream(p)) { props.load(in); }
                String val = props.getProperty("agromonitoring.apiKey");
                if (val != null && !val.isBlank()) return val.trim();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static double resolveLat() {
        String env = System.getenv("AGRI_LAT");
        if (env != null) try { return Double.parseDouble(env); } catch (Exception ignored) {}
        Double cfg = readDoubleFromConfig("advice.lat");
        if (cfg != null) return cfg;
        return 36.8065; // Tunis default
    }

    private static double resolveLon() {
        String env = System.getenv("AGRI_LON");
        if (env != null) try { return Double.parseDouble(env); } catch (Exception ignored) {}
        Double cfg = readDoubleFromConfig("advice.lon");
        if (cfg != null) return cfg;
        return 10.1815; // Tunis default
    }

    private static Double readDoubleFromConfig(String key) {
        try {
            var url = AgromonitoringAdviceService.class.getResource("/config.properties");
            if (url == null) return null;
            Properties props = new Properties();
            try (var in = url.openStream()) { props.load(in); }
            String v = props.getProperty(key);
            if (v == null) return null;
            return Double.parseDouble(v.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String readStringFromConfig(String key) {
        try {
            var url = AgromonitoringAdviceService.class.getResource("/config.properties");
            if (url == null) return null;
            Properties props = new Properties();
            try (var in = url.openStream()) { props.load(in); }
            String v = props.getProperty(key);
            if (v == null || v.isBlank()) return null;
            return v.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static String round(double v) {
        if (Double.isNaN(v)) return "-";
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    private static double roundDouble(double v) {
        if (Double.isNaN(v)) return v;
        return Math.round(v * 10.0) / 10.0;
    }

    private static String randomGeneralTip() {
        String[] tips = new String[]{
                "Alternez cultures gourmandes et légumineuses pour enrichir le sol naturellement.",
                "Paillez (paille, broyat) pour garder l'humidité et limiter les mauvaises herbes.",
                "Arrosez tôt le matin ou en fin de journée pour réduire l'évaporation.",
                "Installez des bandes fleuries pour favoriser les auxiliaires (coccinelles, syrphes).",
                "Vérifiez l'état du sol avant d'arroser: la surface sèche ne signifie pas toujours un manque d'eau.",
                "Privilégiez des variétés adaptées au climat local pour réduire maladies et intrants.",
                "Nettoyez et désinfectez le matériel pour limiter la propagation des maladies.",
                "Compostez les résidus sains pour améliorer la structure du sol et sa fertilité.",
                "Espacez les plants pour améliorer l'aération et réduire les risques cryptogamiques.",
                "Observez régulièrement: de petites interventions évitent de gros problèmes."
        };
        int i = (int) Math.floor(Math.random() * tips.length);
        return "Conseil Culture: " + tips[i];
    }
}
