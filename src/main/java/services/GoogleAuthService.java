package services;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.net.http.HttpClient;       // ← manquait
import java.net.http.HttpRequest;      // ← manquait
import java.net.http.HttpResponse;     // ← manquait
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.Arrays;
import java.util.Properties;
import java.util.Scanner;

public class GoogleAuthService {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private String clientId;
    private String clientSecret;
    private String redirectUri;

    // Les infos du user récupérées après auth
    private String userEmail;
    private String userName;
    private String userPicture;

    public GoogleAuthService() {
        loadConfig();
    }

    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("google-config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            this.clientId = prop.getProperty("google.client.id");
            this.clientSecret = prop.getProperty("google.client.secret");
            this.redirectUri = prop.getProperty("google.redirect.uri");
        } catch (IOException e) {
            throw new RuntimeException("❌ Impossible de charger google-config.properties", e);
        }
    }

    // ==========================================
    // ÉTAPE PRINCIPALE : Lance le flow OAuth
    // ==========================================
    public boolean authenticate() throws Exception {

        // 1. Construire l'URL d'auth Google
        String authUrl = buildAuthorizationUrl();

        // 2. Ouvrir le navigateur
        Desktop.getDesktop().browse(new URI(authUrl));

        // 3. Écouter le callback sur localhost:8888
        String authCode = listenForAuthCode();

        if (authCode == null) {
            return false;
        }

        // 4. Échanger le code contre un token
        String accessToken = exchangeCodeForToken(authCode);

        if (accessToken == null) {
            return false;
        }

        // 5. Récupérer les infos du user
        fetchUserInfo(accessToken);

        return true;
    }

    private String buildAuthorizationUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=email%20profile" +
                "&access_type=offline";
    }

    // Crée un petit serveur HTTP temporaire pour capter le code
    private String listenForAuthCode() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            serverSocket.setSoTimeout(120000); // timeout 2 minutes
            Socket socket = serverSocket.accept();

            // Lire la requête HTTP du navigateur
            Scanner scanner = new Scanner(socket.getInputStream());
            String requestLine = scanner.nextLine();

            // Envoyer une réponse HTML au navigateur
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println();
            out.println("<html><body style='font-family:Arial;text-align:center;padding-top:50px'>");
            out.println("<h2>✅ Authentification réussie!</h2>");
            out.println("<p>Vous pouvez fermer cet onglet et retourner sur AgriSense 360.</p>");
            out.println("</body></html>");
            out.flush();

            // Extraire le code de l'URL
            // requestLine ressemble à: GET /callback?code=4/XXXX HTTP/1.1
            if (requestLine.contains("code=")) {
                String[] parts = requestLine.split("code=");
                String codePart = parts[1].split("&")[0].split(" ")[0];
                return codePart;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String exchangeCodeForToken(String authCode) {
        try {
            // Requête POST vers Google pour échanger le code
            GenericUrl tokenUrl = new GenericUrl("https://oauth2.googleapis.com/token");

            String body = "code=" + authCode +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&redirect_uri=" + redirectUri +
                    "&grant_type=authorization_code";

            com.google.api.client.http.HttpRequest request = HTTP_TRANSPORT
                    .createRequestFactory()
                    .buildPostRequest(tokenUrl,
                            new com.google.api.client.http.ByteArrayContent(
                                    "application/x-www-form-urlencoded",
                                    body.getBytes()
                            ));

            String response = request.execute().parseAsString();

            // Parser le JSON pour extraire access_token
            // Format: {"access_token":"xxx","expires_in":3599,...}
            if (response.contains("access_token")) {
                String[] parts = response.split("\"access_token\":");
                String tokenPart = parts[1].split("\"")[1];
                return tokenPart;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void fetchUserInfo(String accessToken) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/oauth2/v2/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("🔍 UserInfo response: " + response.body()); // debug

        // Parser robuste sans org.json
        String body = response.body();
        this.userEmail   = extractValue(body, "email");
        this.userName    = extractValue(body, "name");
        this.userPicture = extractValue(body, "picture");

        System.out.println("👤 Google User: " + userName + " (" + userEmail + ")");
    }

    private String extractValue(String json, String key) {
        try {
            String search = "\"" + key + "\"";
            int keyIndex = json.indexOf(search);
            if (keyIndex == -1) return null;

            int colonIndex = json.indexOf(":", keyIndex);
            int quoteStart = json.indexOf("\"", colonIndex) + 1;
            int quoteEnd = json.indexOf("\"", quoteStart);

            String value = json.substring(quoteStart, quoteEnd);
            return value.isEmpty() ? null : value;
        } catch (Exception e) {
            return null;
        }
    }
    // Helper simple pour extraire une valeur JSON
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":\"";
            int start = json.indexOf(searchKey) + searchKey.length();
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    // ===== GETTERS =====
    public String getUserEmail() { return userEmail; }
    public String getUserName() { return userName; }
    public String getUserPicture() { return userPicture; }
}