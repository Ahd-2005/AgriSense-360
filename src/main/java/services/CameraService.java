package services;

import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class CameraService {

    private Process        streamProcess;
    private BufferedReader stdoutReader;
    private Thread         previewThread;
    private final AtomicBoolean previewRunning = new AtomicBoolean(false);
    private volatile boolean    cameraReady    = false;

    private static String extractScriptToTemp(String scriptName) throws Exception {
        try {
            String exeDir = new File(
                    CameraService.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI()
            ).getParentFile().getAbsolutePath();
            File f = new File(exeDir, scriptName);
            if (f.exists()) return f.getAbsolutePath();
            File up = new File(exeDir + File.separator + ".." + File.separator + scriptName);
            if (up.exists()) return up.getCanonicalPath();
        } catch (Exception ignored) {}

        InputStream in = CameraService.class.getResourceAsStream("/scripts/" + scriptName);
        if (in != null) {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "agrisense_scripts");
            Files.createDirectories(tempDir);
            Path dest = tempDir.resolve(scriptName);
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            in.close();
            System.out.println("[CameraService] Script extracted to: " + dest);
            return dest.toAbsolutePath().toString();
        }
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

    public boolean open() {
        try {
            String python = resolvePython();
            String script = extractScriptToTemp("camera_capture_script.py");
            System.out.println("[CameraService] Python: " + python);
            System.out.println("[CameraService] Script: " + script);

            ProcessBuilder pb = new ProcessBuilder(python, script, "stream");
            pb.redirectErrorStream(false);
            streamProcess = pb.start();
            stdoutReader = new BufferedReader(new InputStreamReader(streamProcess.getInputStream()));

            long deadline = System.currentTimeMillis() + 8000;
            String line;
            while (System.currentTimeMillis() < deadline) {
                if (stdoutReader.ready()) {
                    line = stdoutReader.readLine();
                    if (line == null) break;
                    if (line.startsWith("ERROR")) { System.err.println("Camera: " + line); return false; }
                    if (line.equals("READY")) { cameraReady = true; return true; }
                }
                Thread.sleep(100);
            }
            System.err.println("Camera: timed out waiting for READY");
            return false;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public void startPreview(Consumer<Image> imageConsumer) {
        if (!cameraReady) return;
        previewRunning.set(true);
        previewThread = new Thread(() -> {
            try {
                String line;
                while (previewRunning.get() && (line = stdoutReader.readLine()) != null) {
                    if (line.startsWith("ERROR") || line.startsWith("OK")) continue;
                    byte[] jpegBytes = Base64.getDecoder().decode(line);
                    Image img = new Image(new ByteArrayInputStream(jpegBytes), 480, 360, true, false);
                    Platform.runLater(() -> imageConsumer.accept(img));
                }
            } catch (IOException e) { if (previewRunning.get()) e.printStackTrace(); }
        });
        previewThread.setDaemon(true);
        previewThread.start();
    }

    public String captureAndSave() {
        previewRunning.set(false);
        if (previewThread != null) previewThread.interrupt();
        stopProcess();

        try {
            Path tmpDir = Files.createTempDirectory("agrisense_snap_");
            // ✅ We pass .png path but script now saves as .jpg (always 3-channel)
            String requestedPath = tmpDir.resolve("snap_" + System.currentTimeMillis() + ".png")
                    .toAbsolutePath().toString();
            Thread.sleep(600);

            String python = resolvePython();
            String script = extractScriptToTemp("camera_capture_script.py");

            ProcessBuilder pb = new ProcessBuilder(python, script, "capture", "--output", requestedPath);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            String actualPath = null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println("[capture] " + line);
                    if (line.startsWith("ERROR")) return null;
                    // ✅ Script prints "OK:<actual_path>" — extract the real saved path
                    if (line.startsWith("OK:")) {
                        actualPath = line.substring(3).trim();
                    }
                }
            }
            p.waitFor();

            // Return the actual saved path (may be .jpg instead of .png)
            if (actualPath != null && new File(actualPath).exists()) {
                return actualPath;
            }
            // Fallback: check if .jpg version exists
            File jpgFile = new File(requestedPath.replace(".png", ".jpg"));
            if (jpgFile.exists()) return jpgFile.getAbsolutePath();

            return null;

        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public void stop() {
        previewRunning.set(false);
        cameraReady = false;
        if (previewThread != null) previewThread.interrupt();
        stopProcess();
    }

    private void stopProcess() {
        if (streamProcess != null && streamProcess.isAlive()) streamProcess.destroyForcibly();
    }

    public boolean isOpen() {
        return cameraReady && streamProcess != null && streamProcess.isAlive();
    }
}