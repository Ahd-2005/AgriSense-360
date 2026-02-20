package utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for managing culture growth durations and state calculations
 */
public class CultureDurations {

    // Durées en jours pour chaque phase: [Semis, Croissance, Maturité]
    private static final Map<String, int[]> DURATIONS = new HashMap<>();

    static {
        // CÉRÉALES
        DURATIONS.put("Maïs", new int[]{30, 60, 30});      // Total: 120 jours
        DURATIONS.put("Riz", new int[]{25, 90, 30});       // Total: 145 jours
        DURATIONS.put("Blé", new int[]{20, 80, 30});       // Total: 130 jours

        // LÉGUMES
        DURATIONS.put("Oignon", new int[]{15, 70, 20});    // Total: 105 jours
        DURATIONS.put("Salades", new int[]{10, 40, 15});   // Total: 65 jours
        DURATIONS.put("Pomme de terre", new int[]{20, 70, 25}); // Total: 115 jours

        // FRUITS
        DURATIONS.put("Pêche", new int[]{30, 90, 40});     // Total: 160 jours
        DURATIONS.put("Fraise", new int[]{15, 50, 25});    // Total: 90 jours

        // ORNEMENTALES
        DURATIONS.put("Rosier", new int[]{20, 60, 40});    // Total: 120 jours
        DURATIONS.put("Tulipe", new int[]{15, 45, 30});    // Total: 90 jours
    }

    public static int getTotalDuration(String cultureName) {
        int[] phases = DURATIONS.get(cultureName);
        if (phases == null) {
            return 90;
        }
        return phases[0] + phases[1] + phases[2];
    }

    public static LocalDate calculateHarvestDate(LocalDate plantationDate, String cultureName) {
        int totalDays = getTotalDuration(cultureName);
        return plantationDate.plusDays(totalDays);
    }

    public static String calculateCurrentState(LocalDate plantationDate,
                                               LocalDate recolteDate,
                                               String cultureName) {
        LocalDate today = LocalDate.now();
        long daysSincePlantation = ChronoUnit.DAYS.between(plantationDate, today);
        long daysUntilHarvest = ChronoUnit.DAYS.between(today, recolteDate);

        int[] phases = DURATIONS.getOrDefault(cultureName, new int[]{20, 50, 20});
        int semisDuration = phases[0];
        int croissanceDuration = phases[1];

        if (daysUntilHarvest < 0) {
            return "Récolte en Retard";
        }

        if (daysUntilHarvest <= 7) {
            return "Récolte Prévue";
        }

        if (daysSincePlantation < semisDuration) {
            return "Semis";
        }

        if (daysSincePlantation < semisDuration + croissanceDuration) {
            return "Croissance";
        }

        return "Maturité";
    }

    public static boolean isReadyToHarvest(String etat) {
        return etat.equals("Maturité") ||
                etat.equals("Récolte Prévue") ||
                etat.equals("Récolte en Retard");
    }

    public static String getEarlyHarvestWarning(String etat) {
        switch (etat) {
            case "Semis":
                return "La culture vient juste d'être plantée! La récolte maintenant donnera très peu de rendement.";
            case "Croissance":
                return "La culture est en pleine croissance. Une récolte prématurée réduira significativement le rendement.";
            case "Maturité":
                return "La culture est mature mais pas encore au pic de récolte optimale.";
            default:
                return "La culture n'est pas encore prête à être récoltée.";
        }
    }
}