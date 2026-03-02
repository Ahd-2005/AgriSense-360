package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import utils.AppConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Service météo utilisant l'API OpenWeatherMap.
 * Clé chargée depuis src/main/resources/config.properties (non commité).
 */
public class WeatherService {

    private static final String API_KEY = AppConfig.get("weather.api.key");
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    /**
     * Récupère les conditions météo actuelles pour une zone (ville/région).
     *
     * @param zoneTravail Le nom de la ville ou zone de travail
     * @return WeatherInfo avec les données météo et l'aptitude au travail agricole
     */
    public static WeatherInfo fetchWeather(String zoneTravail) {
        if (zoneTravail == null || zoneTravail.trim().isEmpty()) {
            return WeatherInfo.error("Veuillez renseigner la zone de travail.");
        }

        try {
            String encodedZone = URLEncoder.encode(zoneTravail.trim(), StandardCharsets.UTF_8);
            String url = BASE_URL + "?q=" + encodedZone
                    + "&appid=" + API_KEY
                    + "&units=metric"
                    + "&lang=fr";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[WeatherService] HTTP " + response.statusCode() + " → " + response.body());

            if (response.statusCode() == 200) {
                return parseResponse(response.body());
            } else if (response.statusCode() == 404) {
                return WeatherInfo.error("Zone introuvable : \"" + zoneTravail + "\". Essayez un nom de ville (ex: Tunis, Paris).");
            } else if (response.statusCode() == 401) {
                return WeatherInfo.error("Clé API non activée.\nLes nouvelles clés OpenWeatherMap prennent jusqu'à 2h pour s'activer.\nRéessayez dans quelques instants.");
            } else {
                return WeatherInfo.error("Erreur API météo (" + response.statusCode() + ") : " + response.body());
            }

        } catch (java.net.UnknownHostException e) {
            System.err.println("[WeatherService] UnknownHost: " + e.getMessage());
            return WeatherInfo.error("Pas de connexion Internet.");
        } catch (Exception e) {
            System.err.println("[WeatherService] Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return WeatherInfo.error("Erreur réseau : " + e.getMessage());
        }
    }

    private static WeatherInfo parseResponse(String body) {
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        double temp = json.getAsJsonObject("main").get("temp").getAsDouble();
        double feelsLike = json.getAsJsonObject("main").get("feels_like").getAsDouble();
        int humidity = json.getAsJsonObject("main").get("humidity").getAsInt();
        double windSpeed = json.getAsJsonObject("wind").get("speed").getAsDouble();

        JsonArray weatherArr = json.getAsJsonArray("weather");
        String description = weatherArr.get(0).getAsJsonObject().get("description").getAsString();
        String mainCondition = weatherArr.get(0).getAsJsonObject().get("main").getAsString();

        String suitability = evaluateSuitability(temp, mainCondition, windSpeed);
        String icon = getWeatherIcon(mainCondition);
        String cityName = json.has("name") ? json.get("name").getAsString() : "";

        return new WeatherInfo(temp, feelsLike, description, windSpeed, humidity,
                suitability, icon, cityName, true, null);
    }

    private static String evaluateSuitability(double temp, String mainCondition, double windSpeed) {
        boolean hasRain = mainCondition.equalsIgnoreCase("Rain")
                || mainCondition.equalsIgnoreCase("Drizzle")
                || mainCondition.equalsIgnoreCase("Thunderstorm");
        boolean hasSnow = mainCondition.equalsIgnoreCase("Snow");
        boolean extremeTemp = temp < 0 || temp > 40;
        boolean strongWind = windSpeed > 15;

        if (extremeTemp || hasSnow || (hasRain && strongWind)) return "DECONSEILLE";
        if (hasRain || strongWind || temp > 35) return "ATTENTION";
        return "FAVORABLE";
    }

    private static String getWeatherIcon(String mainCondition) {
        switch (mainCondition.toLowerCase()) {
            case "rain":
            case "drizzle":    return "🌧️";
            case "thunderstorm": return "⛈️";
            case "snow":         return "❄️";
            case "clouds":       return "☁️";
            case "mist":
            case "fog":
            case "haze":         return "🌫️";
            case "clear":        return "☀️";
            default:             return "🌤️";
        }
    }

    // ────────────── Data class ──────────────

    public static class WeatherInfo {
        public final double temperature;
        public final double feelsLike;
        public final String description;
        public final double windSpeed;
        public final int humidity;
        public final String suitability;
        public final String icon;
        public final String cityName;
        public final boolean success;
        public final String errorMessage;

        public WeatherInfo(double temperature, double feelsLike, String description,
                           double windSpeed, int humidity, String suitability,
                           String icon, String cityName, boolean success, String errorMessage) {
            this.temperature = temperature;
            this.feelsLike = feelsLike;
            this.description = description;
            this.windSpeed = windSpeed;
            this.humidity = humidity;
            this.suitability = suitability;
            this.icon = icon;
            this.cityName = cityName;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static WeatherInfo error(String msg) {
            return new WeatherInfo(0, 0, "", 0, 0, "", "❌", "", false, msg);
        }

        public String getSuitabilityLabel() {
            switch (suitability) {
                case "FAVORABLE":   return "✅ Favorable au travail";
                case "ATTENTION":   return "⚠️ Conditions difficiles";
                case "DECONSEILLE": return "❌ Travail déconseillé";
                default:            return "";
            }
        }

        public String getSuitabilityColor() {
            switch (suitability) {
                case "FAVORABLE":   return "#27ae60";
                case "ATTENTION":   return "#d68910";
                case "DECONSEILLE": return "#c0392b";
                default:            return "#555";
            }
        }

        public String getSummary() {
            return String.format("%s %s  |  %.1f°C (ressenti %.1f°C)  |  Vent : %.1f m/s  |  Humidité : %d%%",
                    icon, capitalise(description), temperature, feelsLike, windSpeed, humidity);
        }

        private String capitalise(String s) {
            if (s == null || s.isEmpty()) return s;
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
}
