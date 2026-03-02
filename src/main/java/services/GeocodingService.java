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
 * Service de géocodage utilisant l'API OpenCage Geocoding.
 * Convertit une adresse/zone en coordonnées GPS (latitude, longitude).
 * API gratuite (2500 req/jour), clé chargée depuis config.properties.
 */
public class GeocodingService {

    private static final String API_KEY = AppConfig.get("opencage.api.key");
    private static final String BASE_URL = "https://api.opencagedata.com/geocode/v1/json";

    /**
     * Résultat du géocodage contenant les coordonnées et l'adresse formatée.
     */
    public static class GeocodingResult {
        public final boolean success;
        public final double latitude;
        public final double longitude;
        public final String formattedAddress;
        public final String errorMessage;

        private GeocodingResult(boolean success, double latitude, double longitude,
                                String formattedAddress, String errorMessage) {
            this.success = success;
            this.latitude = latitude;
            this.longitude = longitude;
            this.formattedAddress = formattedAddress;
            this.errorMessage = errorMessage;
        }

        public static GeocodingResult ok(double lat, double lng, String address) {
            return new GeocodingResult(true, lat, lng, address, null);
        }

        public static GeocodingResult error(String message) {
            return new GeocodingResult(false, 0, 0, null, message);
        }

        /**
         * Retourne l'URL Google Maps pour ces coordonnées.
         */
        public String getGoogleMapsUrl() {
            return "https://www.google.com/maps?q=" + latitude + "," + longitude;
        }

        /**
         * Résumé textuel des coordonnées.
         */
        public String getSummary() {
            return String.format("📍 %s\n🌐 Lat: %.6f | Lng: %.6f", formattedAddress, latitude, longitude);
        }
    }

    /**
     * Géocode une adresse ou zone de travail en coordonnées GPS.
     *
     * @param address L'adresse, ville ou zone à géocoder
     * @return GeocodingResult avec les coordonnées ou un message d'erreur
     */
    public static GeocodingResult geocode(String address) {
        if (address == null || address.trim().isEmpty()) {
            return GeocodingResult.error("Veuillez renseigner une adresse ou zone de travail.");
        }

        if (API_KEY == null || API_KEY.trim().isEmpty()) {
            return GeocodingResult.error("Clé API OpenCage non configurée dans config.properties.");
        }

        try {
            String encodedAddress = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
            String url = BASE_URL + "?q=" + encodedAddress
                    + "&key=" + API_KEY
                    + "&language=fr"
                    + "&limit=1"
                    + "&no_annotations=0";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                return GeocodingResult.error("Clé API OpenCage invalide. Vérifiez config.properties.");
            }
            if (response.statusCode() == 402) {
                return GeocodingResult.error("Quota OpenCage dépassé (2500 req/jour max).");
            }
            if (response.statusCode() != 200) {
                return GeocodingResult.error("Erreur HTTP " + response.statusCode()
                        + " lors de l'appel à l'API OpenCage.");
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray results = json.getAsJsonArray("results");

            if (results == null || results.size() == 0) {
                return GeocodingResult.error("Aucune localisation trouvée pour « " + address + " ».");
            }

            JsonObject firstResult = results.get(0).getAsJsonObject();
            JsonObject geometry = firstResult.getAsJsonObject("geometry");
            double lat = geometry.get("lat").getAsDouble();
            double lng = geometry.get("lng").getAsDouble();
            String formatted = firstResult.get("formatted").getAsString();

            return GeocodingResult.ok(lat, lng, formatted);

        } catch (Exception e) {
            return GeocodingResult.error("Erreur de connexion: " + e.getMessage());
        }
    }
}
