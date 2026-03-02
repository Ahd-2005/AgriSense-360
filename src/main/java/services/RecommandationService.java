package services;

import entity.Produit;
import entity.Stock;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Moteur de recommandation intelligent basé sur :
 * - Similarité de catégorie (même catégorie = forte affinité)
 * - Proximité de prix (distance normalisée)
 * - Score de stock (quantité par rapport au seuil)
 * - Fraîcheur (date de réception)
 *
 * Algorithme : score de similarité composé, retourne les N produits les plus proches.
 */
public class RecommandationService {

    private static RecommandationService instance;

    // Poids de chaque critère dans le score final (total = 1.0)
    private static final double POIDS_CATEGORIE = 0.45;
    private static final double POIDS_PRIX      = 0.35;
    private static final double POIDS_STOCK     = 0.20;

    private RecommandationService() {}

    public static RecommandationService getInstance() {
        if (instance == null) instance = new RecommandationService();
        return instance;
    }

    // ── Classe résultat ───────────────────────────────────────────────────────

    public static class Recommandation {
        public final Produit produit;
        public final Stock   stock;
        public final double  score;         // 0.0 → 1.0 (1 = parfait)
        public final String  raison;        // explication lisible
        public final String  niveau;        // "ÉLEVÉ" | "MOYEN" | "FAIBLE"

        public Recommandation(Produit produit, Stock stock, double score, String raison) {
            this.produit = produit;
            this.stock   = stock;
            this.score   = score;
            this.raison  = raison;
            this.niveau  = score >= 0.75 ? "ÉLEVÉ" : score >= 0.50 ? "MOYEN" : "FAIBLE";
        }
    }

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Retourne les N produits les plus similaires au produit cible.
     *
     * @param cible         produit de référence
     * @param stockCible    stock du produit de référence
     * @param tousLesProduits liste complète avec leurs stocks
     * @param stocksMap     map produitId → Stock pour accès rapide
     * @param topN          nombre de recommandations souhaitées
     */
    public List<Recommandation> recommander(
            Produit cible,
            Stock stockCible,
            List<Produit> tousLesProduits,
            Map<Integer, Stock> stocksMap,
            int topN
    ) {
        if (cible == null || tousLesProduits == null || tousLesProduits.isEmpty()) {
            return Collections.emptyList();
        }

        // Prix max pour normalisation
        double prixMax = tousLesProduits.stream()
            .filter(p -> p.getPrixUnitaire() != null)
            .mapToDouble(p -> p.getPrixUnitaire().doubleValue())
            .max()
            .orElse(1.0);

        double prixMin = tousLesProduits.stream()
            .filter(p -> p.getPrixUnitaire() != null)
            .mapToDouble(p -> p.getPrixUnitaire().doubleValue())
            .min()
            .orElse(0.0);

        double ecartPrix = prixMax - prixMin;
        if (ecartPrix == 0) ecartPrix = 1.0;

        double prixCible = cible.getPrixUnitaire() != null
            ? cible.getPrixUnitaire().doubleValue() : 0.0;
        String categorieCible = cible.getCategorie() != null
            ? cible.getCategorie().toLowerCase().trim() : "";

        List<Recommandation> resultats = new ArrayList<>();

        for (Produit candidat : tousLesProduits) {
            // Exclure le produit lui-même
            if (candidat.getId() == cible.getId()) continue;

            Stock stockCandidat = stocksMap.get(candidat.getId());

            // ── Score catégorie ───────────────────────────────────────────────
            String categorieCandidat = candidat.getCategorie() != null
                ? candidat.getCategorie().toLowerCase().trim() : "";

            double scoreCategorie;
            if (categorieCible.equals(categorieCandidat)) {
                scoreCategorie = 1.0; // même catégorie exacte
            } else if (!categorieCible.isEmpty() && !categorieCandidat.isEmpty()
                    && (categorieCible.contains(categorieCandidat)
                        || categorieCandidat.contains(categorieCible))) {
                scoreCategorie = 0.6; // catégorie partiellement similaire
            } else {
                scoreCategorie = 0.0;
            }

            // ── Score prix ────────────────────────────────────────────────────
            double prixCandidat = candidat.getPrixUnitaire() != null
                ? candidat.getPrixUnitaire().doubleValue() : 0.0;
            double diffPrixNormalisee = Math.abs(prixCible - prixCandidat) / ecartPrix;
            double scorePrix = 1.0 - diffPrixNormalisee; // 1 = prix identique, 0 = prix extrême

            // ── Score stock ───────────────────────────────────────────────────
            double scoreStock = 0.5; // neutre par défaut
            if (stockCandidat != null
                    && stockCandidat.getQuantiteActuelle() != null
                    && stockCandidat.getSeuilAlerte() != null) {
                double qte    = stockCandidat.getQuantiteActuelle().doubleValue();
                double seuil  = stockCandidat.getSeuilAlerte().doubleValue();
                if (seuil > 0) {
                    double ratio = qte / seuil;
                    // Stock abondant = meilleure recommandation (on peut substituer)
                    scoreStock = Math.min(ratio / 3.0, 1.0);
                } else {
                    scoreStock = qte > 0 ? 0.8 : 0.1;
                }
            }

            // ── Score composite ───────────────────────────────────────────────
            double scoreTotal = (scoreCategorie * POIDS_CATEGORIE)
                              + (scorePrix      * POIDS_PRIX)
                              + (scoreStock      * POIDS_STOCK);

            // ── Raison lisible ────────────────────────────────────────────────
            String raison = construireRaison(
                scoreCategorie, scorePrix, scoreStock,
                prixCible, prixCandidat, categorieCible, categorieCandidat,
                stockCandidat
            );

            resultats.add(new Recommandation(candidat, stockCandidat, scoreTotal, raison));
        }

        // Trier par score décroissant, garder les topN
        return resultats.stream()
            .sorted(Comparator.comparingDouble(r -> -r.score))
            .limit(topN)
            .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String construireRaison(
            double scoreCategorie, double scorePrix, double scoreStock,
            double prixCible, double prixCandidat,
            String categorieCible, String categorieCandidat,
            Stock stockCandidat
    ) {
        List<String> points = new ArrayList<>();

        // Catégorie
        if (scoreCategorie == 1.0) {
            points.add("même catégorie (" + categorieCandidat + ")");
        } else if (scoreCategorie > 0) {
            points.add("catégorie proche");
        } else {
            points.add("catégorie différente");
        }

        // Prix
        double diff = Math.abs(prixCible - prixCandidat);
        if (diff < 0.01) {
            points.add("prix identique");
        } else if (scorePrix >= 0.8) {
            points.add(String.format("prix très proche (%.2f TND)", prixCandidat));
        } else if (scorePrix >= 0.5) {
            points.add(String.format("prix similaire (%.2f TND)", prixCandidat));
        } else {
            points.add(String.format("prix différent (%.2f TND)", prixCandidat));
        }

        // Stock
        if (stockCandidat == null) {
            points.add("stock non renseigné");
        } else if (scoreStock >= 0.8) {
            points.add("stock abondant ✓");
        } else if (scoreStock >= 0.4) {
            points.add("stock disponible");
        } else {
            points.add("stock limité");
        }

        return String.join(" · ", points);
    }

    /**
     * Construit la map produitId → Stock depuis deux listes parallèles.
     */
    public static Map<Integer, Stock> construireStocksMap(
            List<Produit> produits,
            List<Stock> stocks
    ) {
        Map<Integer, Stock> map = new HashMap<>();
        for (Stock s : stocks) {
            map.put(s.getProduitId(), s);
        }
        return map;
    }
}
