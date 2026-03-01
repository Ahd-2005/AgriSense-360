package services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import entity.AffectationTravail;
import entity.EvaluationPerformance;
import utils.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Service de génération de rapports IA via l'API Groq (100% gratuit).
 * Clé chargée depuis src/main/resources/config.properties (non commité).
 */
public class AIReportService {

    private static final String API_KEY = AppConfig.get("groq.api.key");
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    /**
     * Génère un rapport de performance IA basé sur les évaluations d'une affectation.
     *
     * @param affectation L'affectation de travail agricole
     * @param evaluations Liste des évaluations de performance associées
     * @return Le rapport texte généré par l'IA, ou un message d'erreur
     */
    public static String generatePerformanceReport(AffectationTravail affectation,
                                                    List<EvaluationPerformance> evaluations) {
        if (evaluations == null || evaluations.isEmpty()) {
            return "Aucune évaluation disponible pour cette affectation.\nAjoutez des évaluations avant de générer un rapport.";
        }

        String prompt = buildPrompt(affectation, evaluations);

        try {
            // Construction du body JSON pour l'API OpenAI
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);

            JsonArray messages = new JsonArray();
            messages.add(message);

            JsonObject body = new JsonObject();
            body.addProperty("model", MODEL);
            body.add("messages", messages);
            body.addProperty("max_tokens", 700);
            body.addProperty("temperature", 0.65);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(body)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[AIReportService/Groq] HTTP " + response.statusCode() + " → " + response.body());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                return json.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString().trim();

            } else if (response.statusCode() == 401) {
                return "❌ Clé API Groq invalide.\nObtenez une clé gratuite sur console.groq.com → API Keys";
            } else if (response.statusCode() == 429) {
                return "⚠️ Limite de requêtes Groq atteinte. Réessayez dans quelques instants.";
            } else {
                return "Erreur API Groq (" + response.statusCode() + ").\nDétail : " + response.body();
            }

        } catch (java.net.UnknownHostException e) {
            System.err.println("[AIReportService] UnknownHost: " + e.getMessage());
            return "❌ Pas de connexion Internet. Vérifiez votre réseau.";
        } catch (Exception e) {
            System.err.println("[AIReportService] Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return "❌ Erreur lors de la génération : " + e.getMessage();
        }
    }

    /**
     * Construit le prompt envoyé à l'IA avec toutes les données de l'affectation.
     */
    private static String buildPrompt(AffectationTravail affectation,
                                       List<EvaluationPerformance> evaluations) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un expert en gestion agricole et en ressources humaines agricoles. ")
          .append("Analyse les évaluations de performance suivantes et génère un rapport professionnel en français.\n\n");

        sb.append("═══ AFFECTATION DE TRAVAIL ═══\n");
        sb.append("• Type de travail   : ").append(affectation.getTypeTravail()).append("\n");
        sb.append("• Zone de travail   : ").append(affectation.getZoneTravail()).append("\n");
        sb.append("• Période           : du ").append(affectation.getDateDebut())
          .append(" au ").append(affectation.getDateFin()).append("\n");
        sb.append("• Statut            : ").append(affectation.getStatut()).append("\n\n");

        sb.append("═══ ÉVALUATIONS (").append(evaluations.size()).append(" au total) ═══\n");

        double totalNote = 0;
        int countFaible = 0, countMoyenne = 0, countBonne = 0;

        for (int i = 0; i < evaluations.size(); i++) {
            EvaluationPerformance e = evaluations.get(i);
            totalNote += e.getNote();

            String qualite = e.getQualite() != null && !e.getQualite().isEmpty() ? e.getQualite() : "N/A";
            if ("Faible".equals(qualite)) countFaible++;
            else if ("Moyenne".equals(qualite)) countMoyenne++;
            else if ("Bonne".equals(qualite)) countBonne++;

            sb.append("  [").append(i + 1).append("] Note: ").append(e.getNote()).append("/5");
            sb.append("  |  Qualité: ").append(qualite);
            if (e.getCommentaire() != null && !e.getCommentaire().trim().isEmpty()) {
                sb.append("  |  Commentaire: \"").append(e.getCommentaire().trim()).append("\"");
            }
            sb.append("  |  Date: ").append(e.getDateEvaluation()).append("\n");
        }

        double avg = totalNote / evaluations.size();
        sb.append("\n• Moyenne des notes : ").append(String.format("%.2f", avg)).append("/5\n");
        sb.append("• Répartition qualité : Bonne=").append(countBonne)
          .append("  Moyenne=").append(countMoyenne)
          .append("  Faible=").append(countFaible).append("\n\n");

        sb.append("═══ FORMAT DU RAPPORT ATTENDU ═══\n");
        sb.append("Génère un rapport structuré avec exactement ces 3 sections en français :\n\n");
        sb.append("📊 DIAGNOSTIC GLOBAL\n");
        sb.append("(2-3 phrases d'analyse de la performance générale de cette tâche agricole)\n\n");
        sb.append("✅ POINTS FORTS\n");
        sb.append("(liste de 2-3 points positifs observés)\n\n");
        sb.append("🔧 AXES D'AMÉLIORATION\n");
        sb.append("(liste de 2-3 recommandations pratiques adaptées au contexte agricole)\n");

        return sb.toString();
    }
}
