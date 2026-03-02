package services;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FaceRecognitionService {

    private static FaceRecognitionService instance;
    private final String pythonExecutable;
    private final Path scriptPath;
    private static boolean initialized = false;

    // ── Default paths — adjust if needed ──────────────
    private static final String DEFAULT_PYTHON = "python";        // or "python3" on Linux/Mac
    private static final String DEFAULT_SCRIPT = "face_recognition_script.py"; // in project root

    private FaceRecognitionService(String pythonExecutable, String scriptPath) {
        this.pythonExecutable = pythonExecutable;
        this.scriptPath       = Paths.get(scriptPath);
        initialized           = true;
    }

    // ── Singleton with custom paths ────────────────────
    public static synchronized FaceRecognitionService getInstance(String pythonExecutable, String scriptPath) {
        if (instance == null) {
            instance = new FaceRecognitionService(pythonExecutable, scriptPath);
        }
        return instance;
    }

    // ✅ Singleton with default paths (use this in login)
    public static synchronized FaceRecognitionService getInstance() {
        if (instance == null) {
            instance = new FaceRecognitionService(DEFAULT_PYTHON, DEFAULT_SCRIPT);
        }
        return instance;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    // ── REGISTER ───────────────────────────────────────
    public FaceRecognitionResult registerFace(int userId, String imagePath)
            throws IOException, FaceRecognitionException {

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath.toString());
        command.add("register");
        command.add("--user_id");
        command.add(String.valueOf(userId));
        command.add("--image");
        command.add(imagePath);

        JSONObject result = executeScript(command);

        if (result.has("error")) {
            throw new FaceRecognitionException(result.getString("error"));
        }

        return new FaceRecognitionResult(
                true,
                result.optString("message", "Face registered successfully"),
                null,
                0.0
        );
    }

    // ── COMPARE ────────────────────────────────────────
    public FaceRecognitionResult compareFace(String imagePath, double threshold)
            throws IOException, FaceRecognitionException {

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath.toString());
        command.add("compare");
        command.add("--image");
        command.add(imagePath);
        command.add("--threshold");
        command.add(String.valueOf(threshold));

        JSONObject result = executeScript(command);

        if (result.has("error")) {
            throw new FaceRecognitionException(result.getString("error"));
        }

        return new FaceRecognitionResult(
                true,
                "Face matched successfully",
                result.getInt("user_id"),
                result.getDouble("distance")
        );
    }

    // ── LOGIN BY FACE — main entry point for login page ──
    // Returns the matched user's DB id, or -1 if no match
    public int loginByFace(String imagePath) {
        try {
            FaceRecognitionResult result = compareFace(imagePath, 0.6);
            if (result.isSuccess() && result.getUserId() != null) {
                return result.getUserId();
            }
        } catch (FaceRecognitionException e) {
            System.out.println("Face login failed: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Face recognition I/O error: " + e.getMessage());
        }
        return -1; // no match
    }

    // ── EXECUTE PYTHON SCRIPT ──────────────────────────
    private JSONObject executeScript(List<String> command) throws IOException {
        // Add -W ignore to suppress Python deprecation warnings on stderr
        List<String> fullCmd = new ArrayList<>(command);
        fullCmd.add(1, "-W");
        fullCmd.add(2, "ignore");

        ProcessBuilder pb = new ProcessBuilder(fullCmd);
        pb.redirectErrorStream(false);  // Keep stderr separate from stdout!
        Process process = pb.start();

        // Read stdout (JSON output)
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        // Drain stderr to prevent blocking (log for debugging)
        try (BufferedReader errReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errReader.readLine()) != null) {
                System.err.println("[face_recognition] " + line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                JSONObject error = new JSONObject();
                error.put("error", "Script failed with exit code " + exitCode + ": " + output);
                return error;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            JSONObject error = new JSONObject();
            error.put("error", "Script interrupted: " + e.getMessage());
            return error;
        }

        String outputStr = output.toString().trim();

        // Extract JSON from output (skip any non-JSON prefix)
        int jsonStart = outputStr.indexOf('{');
        if (jsonStart > 0) {
            outputStr = outputStr.substring(jsonStart);
        }

        if (outputStr.isEmpty()) {
            JSONObject error = new JSONObject();
            error.put("error", "Empty response from Python script");
            return error;
        }

        return new JSONObject(outputStr);
    }

    // ── EXCEPTION ──────────────────────────────────────
    public static class FaceRecognitionException extends Exception {
        public FaceRecognitionException(String message) { super(message); }
    }

    // ── RESULT ─────────────────────────────────────────
    public static class FaceRecognitionResult {
        private final boolean success;
        private final String  message;
        private final Integer userId;
        private final double  distance;

        public FaceRecognitionResult(boolean success, String message, Integer userId, double distance) {
            this.success  = success;
            this.message  = message;
            this.userId   = userId;
            this.distance = distance;
        }

        public boolean isSuccess()   { return success; }
        public String  getMessage()  { return message; }
        public Integer getUserId()   { return userId; }
        public double  getDistance() { return distance; }

        @Override
        public String toString() {
            return "FaceRecognitionResult{success=" + success
                    + ", message='" + message + '\''
                    + ", userId=" + userId
                    + ", distance=" + distance + '}';
        }
    }
}
