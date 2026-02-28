package services;

import entity.Culture;
import entity.Produit;
import entity.Stock;
import org.json.JSONObject;
import utils.MyDataBase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HarvestAIService
 * Appelle harvest_ai.py (modele sklearn local) pour calculer la quantite recoltee,
 * cree un Produit depuis la Culture (avec sa photo/img), puis insere le resultat dans la table stock.
 */
public class HarvestAIService {

    private final ServiceStockStock stockService = new ServiceStockStock();
    private final ServiceStockProduit produitService = new ServiceStockProduit();

    // Agriculteur ID par defaut - idéalement recupere depuis SessionManager
    private static final int DEFAULT_AGRICULTEUR_ID = 3;

    // ─── Resultat retourne au Controller ────────────────────────────────────
    public static class HarvestResult {
        public final double quantiteKg;
        public final int rendementPct;
        public final String explication;
        public final boolean success;
        public final String errorMessage;
        // Infos supplementaires pour affichage
        public double surfaceM2;
        public double surfaceHa;
        public int parcelleId;
        public String cultureName;
        public String emplacement;

        public HarvestResult(double quantiteKg, int rendementPct, String explication) {
            this.quantiteKg   = quantiteKg;
            this.rendementPct = rendementPct;
            this.explication  = explication;
            this.success      = true;
            this.errorMessage = null;
        }

        public HarvestResult(String errorMessage) {
            this.quantiteKg   = 0;
            this.rendementPct = 0;
            this.explication  = null;
            this.success      = false;
            this.errorMessage = errorMessage;
        }
    }

    // ─── Point d'entree principal ────────────────────────────────────────────
    /**
     * 1) Appelle Python pour calculer la quantite
     * 2) Si quantite > 0, cree un Produit (depuis la Culture) puis insere dans stock
     * 3) Retourne le resultat pour l'affichage
     */
    public HarvestResult recolterCulture(Culture culture, String emplacement) {
        // Calculer via le modele IA
        HarvestResult aiResult = callPythonModel(culture);

        if (!aiResult.success) {
            return aiResult;
        }

        // Enrichir le resultat avec les infos culture/parcelle
        aiResult.surfaceM2    = culture.getSurface();
        aiResult.surfaceHa    = culture.getSurface() / 10000.0;
        aiResult.parcelleId   = culture.getParcelleId();
        aiResult.cultureName  = culture.getNom();
        aiResult.emplacement  = emplacement;

        // Si quantite > 0 -> creer produit si besoin, puis inserer/mettre a jour le stock
        if (aiResult.quantiteKg > 0) {
            try {
                insertOrUpdateStockFromCulture(culture, aiResult.quantiteKg, emplacement);
            } catch (Exception e) {
                return new HarvestResult(
                        "Calcul IA reussi (" + aiResult.quantiteKg + " kg) " +
                                "mais erreur insertion stock: " + e.getMessage()
                );
            }
        }

        return aiResult;
    }

    // ─── Creer ou recuperer le Produit associe a la Culture ──────────────────
    /**
     * La table stock requiert un produit_id (FK vers produit).
     * On cree donc d'abord un produit qui correspond a la culture recoltee,
     * en reutilisant l'image de la culture (culture.getImg()) comme photo_url.
     *
     * Si un produit avec le meme nom existe deja pour cet agriculteur,
     * on le reutilise (contrainte unique : agriculteur_id + nom).
     */
    private int getOrCreateProduitForCulture(Culture culture) throws Exception {
        String nom = culture.getNom();

        // 1. Chercher un produit existant par nom (pour eviter la contrainte unique)
        Integer existingId = findProduitIdByNom(nom);
        if (existingId != null) {
            return existingId;
        }

        // 2. Creer un nouveau produit depuis les donnees de la culture
        Produit produit = new Produit();
        produit.setAgriculteurId(DEFAULT_AGRICULTEUR_ID);
        produit.setNom(nom);

        // Categorie = type_Culture de la culture
        String categorie = culture.getTypeCulture() != null ? culture.getTypeCulture() : "Récolte";
        produit.setCategorie(categorie);

        // Description avec les infos de la culture
        String description = "Récolte automatique depuis la culture. " +
                "Surface: " + String.format("%.0f", culture.getSurface()) + " m² | " +
                "Type: " + categorie;
        produit.setDescription(description);

        // Prix unitaire par defaut (0.00)
        produit.setPrixUnitaire(BigDecimal.ZERO);

        // Photo: utiliser l'image de la culture
        // Dans la DB, culture.img stocke juste le nom du fichier (ex: "avoine.png")
        // On construit le chemin complet comme utilise dans CultureController
        String imgPath = culture.getImg();
        if (imgPath != null && !imgPath.isBlank()) {
            // Construire le meme chemin que CultureController utilise
            produit.setPhotoUrl("/images/cultures/" + imgPath);
        } else {
            produit.setPhotoUrl("");
        }

        produit.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        int newId = produitService.ajouter(produit);
        if (newId == -1) {
            throw new Exception("Impossible de creer le produit pour la culture: " + nom);
        }
        return newId;
    }

    /**
     * Cherche un produit par son nom dans la table produit.
     * Retourne l'ID si trouve, null sinon.
     */
    private Integer findProduitIdByNom(String nom) {
        String sql = "SELECT id FROM produit WHERE nom = ? LIMIT 1";
        try (PreparedStatement pst = MyDataBase.getInstance().getCnx().prepareStatement(sql)) {
            pst.setString(1, nom);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ─── Insertion dans la table stock ───────────────────────────────────────
    /**
     * 1) Recupere ou cree le produit correspondant a la culture
     * 2) Cree ou met a jour l'entree stock avec ce produit_id
     */
    private void insertOrUpdateStockFromCulture(Culture culture, double quantiteKg, String emplacement) throws Exception {
        // Etape 1: obtenir un produit_id valide (FK produit)
        int produitId = getOrCreateProduitForCulture(culture);

        // Etape 2: verifier si un stock existe deja pour ce produit
        Stock existing = stockService.recupererParProduitId(produitId);

        if (existing != null) {
            // Additionner a l'existant
            BigDecimal newQty = existing.getQuantiteActuelle()
                    .add(BigDecimal.valueOf(quantiteKg));
            existing.setQuantiteActuelle(newQty);
            existing.setDateReception(Date.valueOf(LocalDate.now()));
            // Mettre a jour l'emplacement si fourni
            if (emplacement != null && !emplacement.isBlank()) {
                existing.setEmplacement(emplacement);
            }
            stockService.modifier(existing);
        } else {
            // Creer une nouvelle entree stock
            Stock stock = new Stock();
            stock.setProduitId(produitId);
            stock.setQuantiteActuelle(BigDecimal.valueOf(quantiteKg));
            stock.setSeuilAlerte(BigDecimal.valueOf(50));
            stock.setUniteMesure("kg");
            stock.setDateReception(Date.valueOf(LocalDate.now()));
            stock.setDateExpiration(null);
            stock.setEmplacement(
                    emplacement != null && !emplacement.isBlank()
                            ? emplacement
                            : "Entrepot principal"
            );
            stockService.ajouter(stock);
        }
    }

    // ─── Appel Python ────────────────────────────────────────────────────────
    private HarvestResult callPythonModel(Culture culture) {
        try {
            String scriptPath = resolveScriptPath();
            String today = LocalDate.now().toString();

            String etat           = culture.getEtat()           != null ? culture.getEtat()           : "maturite";
            String dateRecolte    = culture.getDateRecolte()    != null ? culture.getDateRecolte().toString()    : today;
            String datePlantation = culture.getDatePlantation() != null ? culture.getDatePlantation().toString() : today;
            String typeCulture    = culture.getTypeCulture()    != null ? culture.getTypeCulture()    : "Cereales";
            String nom            = culture.getNom()            != null ? culture.getNom()            : "Culture";
            // Surface en m2 dans la DB -> convertir en ha pour le modele
            double surfaceHa      = culture.getSurface() / 10000.0;

            List<String> cmd = new ArrayList<>();
            String pythonExe = resolvePythonExe();
            cmd.add(pythonExe);
            cmd.add(scriptPath);
            cmd.add(nom);
            cmd.add(typeCulture);
            cmd.add(etat);
            cmd.add(String.valueOf(surfaceHa));
            cmd.add(datePlantation);
            cmd.add(dateRecolte);
            cmd.add(today);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.environment().remove("PYTHONHOME");
            pb.environment().remove("PYTHONPATH");
            pb.directory(new java.io.File(scriptPath).getParentFile());

            Process process = pb.start();

            String stdout = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n")).trim();

            String stderr = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n")).trim();

            int exitCode = process.waitFor();

            if (exitCode != 0 || stdout.isEmpty()) {
                return new HarvestResult("Erreur script Python (exit=" + exitCode + "): " + stderr);
            }

            return parseResult(stdout);

        } catch (Exception e) {
            return new HarvestResult("Impossible d'executer harvest_ai.py: " + e.getMessage());
        }
    }

    // ─── Parse JSON retourne par Python ─────────────────────────────────────
    private HarvestResult parseResult(String jsonStr) {
        try {
            JSONObject obj = new JSONObject(jsonStr);
            double qty  = obj.optDouble("quantite_kg", 0);
            int    pct  = obj.optInt("rendement_pct", 0);
            String expl = obj.optString("explication", "Calcul effectue.");
            return new HarvestResult(qty, pct, expl);
        } catch (Exception e) {
            return new HarvestResult("Erreur parsing JSON: " + e.getMessage() + " | Raw: " + jsonStr);
        }
    }

    // ─── Chemin du script Python ─────────────────────────────────────────────
    private String resolvePythonExe() {
        String fromEnv = System.getenv("PYTHON_EXE");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String progFiles = "C:\\Program Files";
        String[] candidates = {
                progFiles + "\\Python313\\python.exe",
                progFiles + "\\Python312\\python.exe",
                progFiles + "\\Python311\\python.exe",
                progFiles + "\\Python310\\python.exe",
                "C:\\Python313\\python.exe",
                "C:\\Python312\\python.exe",
                "C:\\Python310\\python.exe",
                "C:\\Python39\\python.exe",
        };
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && new java.io.File(candidate).exists()) {
                System.out.println("[HarvestAI] Python: " + candidate);
                return candidate;
            }
        }
        return "python";
    }

    private String resolveScriptPath() throws Exception {
        var url = getClass().getResource("/scripts/harvest_ai.py");
        if (url != null) {
            return java.nio.file.Paths.get(url.toURI()).toString();
        }
        java.nio.file.Path fallback = java.nio.file.Paths.get(
                "src", "main", "resources", "scripts", "harvest_ai.py"
        );
        if (java.nio.file.Files.exists(fallback)) {
            return fallback.toAbsolutePath().toString();
        }
        throw new Exception(
                "Script harvest_ai.py introuvable. " +
                        "Placez-le dans src/main/resources/scripts/"
        );
    }
}