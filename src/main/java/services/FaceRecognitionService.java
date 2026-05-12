package services;

import org.json.JSONObject;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class FaceRecognitionService {

    private static FaceRecognitionService instance;
    private final String pythonExecutable;
    // ✅ scriptPath is resolved fresh on every call — never cached as a field
    private static boolean initialized = false;

    private FaceRecognitionService(String pythonExecutable) {
        this.pythonExecutable = pythonExecutable;
        initialized = true;
    }

    public static synchronized FaceRecognitionService getInstance() {
        if (instance == null) {
            instance = new FaceRecognitionService(resolvePython());
        }
        return instance;
    }

    public static boolean isInitialized() { return initialized; }

    // ✅ ALWAYS extract fresh — called on every register/compare, not cached
    private static String extractScriptToTemp(String scriptName) throws Exception {
        // Always overwrite so stale cached versions never survive
        InputStream in = FaceRecognitionService.class.getResourceAsStream("/scripts/" + scriptName);
        if (in != null) {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "agrisense_scripts");
            Files.createDirectories(tempDir);
            Path dest = tempDir.resolve(scriptName);
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            in.close();
            System.out.println("[FaceRecognition] Script extracted to: " + dest);
            return dest.toAbsolutePath().toString();
        }

        // Try next to the exe (for installed app)
        try {
            String exeDir = new File(
                    FaceRecognitionService.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI()
            ).getParentFile().getAbsolutePath();
            File f = new File(exeDir, scriptName);
            if (f.exists()) return f.getAbsolutePath();
            File up = new File(exeDir + File.separator + ".." + File.separator + scriptName);
            if (up.exists()) return up.getCanonicalPath();
        } catch (Exception ignored) {}

        // IntelliJ fallback
        return scriptName;
    }

    private static String resolvePython() {
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
        for (String c : candidates) { if (new File(c).exists()) return c; }
        return "python";
    }

    // ── REGISTER ─────────────────────────────────────────────────────────────
    public FaceRecognitionResult registerFace(int userId, String imagePath)
            throws IOException, FaceRecognitionException {
        String scriptPath = getScriptPath();
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);
        command.add("register");
        command.add("--user_id"); command.add(String.valueOf(userId));
        command.add("--image");   command.add(imagePath);

        JSONObject result = executeScript(command);
        if (result.has("error")) throw new FaceRecognitionException(result.getString("error"));
        return new FaceRecognitionResult(true, result.optString("message", "Face registered successfully"), null, 0.0);
    }

    // ── COMPARE ──────────────────────────────────────────────────────────────
    public FaceRecognitionResult compareFace(String imagePath, double threshold)
            throws IOException, FaceRecognitionException {
        String scriptPath = getScriptPath();
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);
        command.add("compare");
        command.add("--image");     command.add(imagePath);
        command.add("--threshold"); command.add(String.valueOf(threshold));

        JSONObject result = executeScript(command);
        if (result.has("error")) throw new FaceRecognitionException(result.getString("error"));
        return new FaceRecognitionResult(true, "Face matched successfully",
                result.getInt("user_id"), result.getDouble("distance"));
    }

    public int loginByFace(String imagePath) {
        try {
            FaceRecognitionResult result = compareFace(imagePath, 0.6);
            if (result.isSuccess() && result.getUserId() != null) return result.getUserId();
        } catch (FaceRecognitionException e) {
            System.out.println("Face login failed: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Face recognition I/O error: " + e.getMessage());
        }
        return -1;
    }

    // ✅ Fresh path every call — no stale cache
    private String getScriptPath() {
        try {
            return extractScriptToTemp("face_recognition_script.py");
        } catch (Exception e) {
            return "face_recognition_script.py";
        }
    }

    private JSONObject executeScript(List<String> command) throws IOException {
        System.out.println("[FaceRecognition] Running: " + command);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("[FaceRecognition] >> " + line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                JSONObject error = new JSONObject();
                error.put("error", "Script failed (exit=" + exitCode + "): " + output.toString().trim());
                return error;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            JSONObject error = new JSONObject();
            error.put("error", "Interrupted");
            return error;
        }

        // Find the JSON line in output (ignore any debug prints)
        String jsonLine = null;
        for (String line : output.toString().split("\n")) {
            line = line.trim();
            if (line.startsWith("{") && line.endsWith("}")) {
                jsonLine = line;
                break;
            }
        }

        if (jsonLine == null || jsonLine.isEmpty()) {
            JSONObject error = new JSONObject();
            error.put("error", "No JSON response from script. Full output: " + output.toString().trim());
            return error;
        }
        return new JSONObject(jsonLine);
    }

    public static class FaceRecognitionException extends Exception {
        public FaceRecognitionException(String message) { super(message); }
    }

    public static class FaceRecognitionResult {
        private final boolean success;
        private final String  message;
        private final Integer userId;
        private final double  distance;

        public FaceRecognitionResult(boolean success, String message, Integer userId, double distance) {
            this.success = success; this.message = message; this.userId = userId; this.distance = distance;
        }
        public boolean isSuccess()   { return success; }
        public String  getMessage()  { return message; }
        public Integer getUserId()   { return userId; }
        public double  getDistance() { return distance; }
    }
}