package services;

import entity.Culture;
import entity.Produit;
import entity.Stock;
import org.json.JSONObject;
import utils.MyDataBase;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HarvestAIService {

    private final ServiceStockStock stockService = new ServiceStockStock();
    private final ServiceStockProduit produitService = new ServiceStockProduit();
    private static final int DEFAULT_AGRICULTEUR_ID = 3;

    // ─── Result ──────────────────────────────────────────────────────────────
    public static class HarvestResult {
        public final double quantiteKg;
        public final int rendementPct;
        public final String explication;
        public final boolean success;
        public final String errorMessage;
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

    // ─── Main entry point ────────────────────────────────────────────────────
    public HarvestResult recolterCulture(Culture culture, String emplacement) {
        HarvestResult aiResult = callPythonModel(culture);
        if (!aiResult.success) return aiResult;

        aiResult.surfaceM2   = culture.getSurface();
        aiResult.surfaceHa   = culture.getSurface() / 10000.0;
        aiResult.parcelleId  = culture.getParcelleId();
        aiResult.cultureName = culture.getNom();
        aiResult.emplacement = emplacement;

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

    // ─── Produit helper ──────────────────────────────────────────────────────
    private int getOrCreateProduitForCulture(Culture culture) throws Exception {
        String nom = culture.getNom();
        Integer existingId = findProduitIdByNom(nom);
        if (existingId != null) return existingId;

        Produit produit = new Produit();
        produit.setAgriculteurId(DEFAULT_AGRICULTEUR_ID);
        produit.setNom(nom);
        String categorie = culture.getTypeCulture() != null ? culture.getTypeCulture() : "Récolte";
        produit.setCategorie(categorie);
        produit.setDescription("Récolte automatique depuis la culture. Surface: "
                + String.format("%.0f", culture.getSurface()) + " m² | Type: " + categorie);
        produit.setPrixUnitaire(BigDecimal.ZERO);
        String imgPath = culture.getImg();
        produit.setPhotoUrl(imgPath != null && !imgPath.isBlank() ? "/images/cultures/" + imgPath : "");
        produit.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        int newId = produitService.ajouter(produit);
        if (newId == -1) throw new Exception("Impossible de creer le produit pour: " + nom);
        return newId;
    }

    private Integer findProduitIdByNom(String nom) {
        String sql = "SELECT id FROM produit WHERE nom = ? LIMIT 1";
        try (PreparedStatement pst = MyDataBase.getInstance().getCnx().prepareStatement(sql)) {
            pst.setString(1, nom);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void insertOrUpdateStockFromCulture(Culture culture, double quantiteKg, String emplacement) throws Exception {
        int produitId = getOrCreateProduitForCulture(culture);
        Stock existing = stockService.recupererParProduitId(produitId);

        if (existing != null) {
            existing.setQuantiteActuelle(existing.getQuantiteActuelle().add(BigDecimal.valueOf(quantiteKg)));
            existing.setDateReception(Date.valueOf(LocalDate.now()));
            if (emplacement != null && !emplacement.isBlank()) existing.setEmplacement(emplacement);
            stockService.modifier(existing);
        } else {
            Stock stock = new Stock();
            stock.setProduitId(produitId);
            stock.setQuantiteActuelle(BigDecimal.valueOf(quantiteKg));
            stock.setSeuilAlerte(BigDecimal.valueOf(50));
            stock.setUniteMesure("kg");
            stock.setDateReception(Date.valueOf(LocalDate.now()));
            stock.setDateExpiration(null);
            stock.setEmplacement(emplacement != null && !emplacement.isBlank() ? emplacement : "Entrepot principal");
            stockService.ajouter(stock);
        }
    }

    // ─── Call Python ─────────────────────────────────────────────────────────
    private HarvestResult callPythonModel(Culture culture) {
        try {
            // ✅ FIX: Extract script from jar/resources to a real temp file
            String scriptPath = extractScriptToTemp("harvest_ai.py");
            String pythonExe  = resolvePythonExe();
            String today      = LocalDate.now().toString();

            System.out.println("[HarvestAI] Python: " + pythonExe);
            System.out.println("[HarvestAI] Script: " + scriptPath);

            String etat           = culture.getEtat()           != null ? culture.getEtat()           : "maturite";
            String dateRecolte    = culture.getDateRecolte()    != null ? culture.getDateRecolte().toString()    : today;
            String datePlantation = culture.getDatePlantation() != null ? culture.getDatePlantation().toString() : today;
            String typeCulture    = culture.getTypeCulture()    != null ? culture.getTypeCulture()    : "Cereales";
            String nom            = culture.getNom()            != null ? culture.getNom()            : "Culture";
            double surfaceHa      = culture.getSurface() / 10000.0;

            List<String> cmd = new ArrayList<>();
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
            pb.directory(new File(scriptPath).getParentFile());

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

    // ─── ✅ KEY FIX: Extract .py from jar to a real temp file ────────────────
    /**
     * When running as an exe (fat jar), getResource() returns a jar:// URL.
     * Python cannot run a script inside a jar — it needs a real file on disk.
     * This method copies the script to %TEMP% and returns the real path.
     */
    private String extractScriptToTemp(String scriptName) throws Exception {
        // 1. Try next to the exe first (for easy customization after install)
        try {
            String exeDir = new File(
                    getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
            ).getParentFile().getAbsolutePath();

            File next = new File(exeDir, scriptName);
            if (next.exists()) return next.getAbsolutePath();

            File up = new File(exeDir + File.separator + ".." + File.separator + scriptName);
            if (up.exists()) return up.getCanonicalPath();
        } catch (Exception ignored) {}

        // 2. Extract from resources (works from fat jar)
        InputStream in = getClass().getResourceAsStream("/scripts/" + scriptName);
        if (in != null) {
            // Use a stable temp location so we don't re-extract every call
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "agrisense_scripts");
            Files.createDirectories(tempDir);
            Path dest = tempDir.resolve(scriptName);
            // Always overwrite to ensure latest version
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            in.close();
            System.out.println("[HarvestAI] Script extracted to: " + dest);
            return dest.toAbsolutePath().toString();
        }

        // 3. IntelliJ fallback
        Path fallback = Paths.get("src", "main", "resources", "scripts", scriptName);
        if (Files.exists(fallback)) return fallback.toAbsolutePath().toString();

        throw new Exception(
                "Script " + scriptName + " introuvable. Placez-le dans src/main/resources/scripts/"
        );
    }

    // ─── Parse JSON from Python ──────────────────────────────────────────────
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

    // ─── Find Python executable ──────────────────────────────────────────────
    private String resolvePythonExe() {
        String fromEnv = System.getenv("PYTHON_EXE");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();

        String[] candidates = {
                "C:\\Program Files\\Python313\\python.exe",
                "C:\\Program Files\\Python312\\python.exe",
                "C:\\Program Files\\Python311\\python.exe",
                "C:\\Program Files\\Python310\\python.exe",
                "C:\\Python313\\python.exe",
                "C:\\Python312\\python.exe",
                "C:\\Python310\\python.exe",
                "C:\\Python39\\python.exe",
        };
        for (String c : candidates) {
            if (new File(c).exists()) {
                System.out.println("[HarvestAI] Found Python: " + c);
                return c;
            }
        }
        return "python";
    }
}