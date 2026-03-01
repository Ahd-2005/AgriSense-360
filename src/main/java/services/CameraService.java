package services;

import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * CameraService — zero-disk-I/O webcam streaming.
 *
 * Spawns ONE Python process running camera_capture_script.py in stream mode.
 * Python prints base64-encoded JPEG frames line by line to stdout.
 * Java reads them directly via BufferedReader — no temp files, no lag.
 */
public class CameraService {

    private static final String PYTHON = "C:\\Users\\iamam\\OneDrive\\Bureau\\AgriSense-360\\.venv\\Scripts\\python.exe";
    private static final String SCRIPT     = "camera_capture_script.py";

    private Process       streamProcess;
    private BufferedReader stdoutReader;
    private Thread        previewThread;
    private Thread        captureThread;
    private final AtomicBoolean previewRunning = new AtomicBoolean(false);
    private volatile boolean    cameraReady    = false;

    // ── Open: spawn Python stream process ────────────────
    public boolean open() {
        try {
            ProcessBuilder pb = new ProcessBuilder(PYTHON, SCRIPT, "stream");
            pb.redirectErrorStream(false);   // keep stdout and stderr separate
            streamProcess = pb.start();

            stdoutReader = new BufferedReader(
                    new InputStreamReader(streamProcess.getInputStream()));

            // Wait for "READY" signal (max 8 seconds)
            long deadline = System.currentTimeMillis() + 8000;
            String line;
            while (System.currentTimeMillis() < deadline) {
                if (stdoutReader.ready()) {
                    line = stdoutReader.readLine();
                    if (line == null) break;
                    if (line.startsWith("ERROR")) {
                        System.err.println("Camera script: " + line);
                        return false;
                    }
                    if (line.equals("READY")) {
                        cameraReady = true;
                        return true;
                    }
                }
                Thread.sleep(100);
            }
            System.err.println("Camera: timed out waiting for READY");
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── Start preview: read base64 lines → JavaFX Image ──
    public void startPreview(Consumer<Image> imageConsumer) {
        if (!cameraReady) return;
        previewRunning.set(true);

        previewThread = new Thread(() -> {
            try {
                String line;
                while (previewRunning.get() && (line = stdoutReader.readLine()) != null) {
                    if (line.startsWith("ERROR") || line.startsWith("OK")) continue;

                    byte[] jpegBytes = Base64.getDecoder().decode(line);
                    // Wrap bytes in stream for JavaFX Image
                    ByteArrayInputStream bis = new ByteArrayInputStream(jpegBytes);
                    Image img = new Image(bis, 480, 360, true, false);

                    Platform.runLater(() -> imageConsumer.accept(img));
                }
            } catch (IOException e) {
                if (previewRunning.get()) e.printStackTrace();
            }
        });
        previewThread.setDaemon(true);
        previewThread.start();
    }

    // ── Capture one high-res snapshot ────────────────────
    // Stops streaming first, runs a separate capture process
    public String captureAndSave() {
        // Pause preview so camera isn't held by two processes
        previewRunning.set(false);
        if (previewThread != null) previewThread.interrupt();
        stopProcess();

        try {
            Path tmp = Files.createTempDirectory("agrisense_snap_");
            String outPath = tmp.resolve("snap_" + System.currentTimeMillis() + ".png")
                    .toAbsolutePath().toString();

            // Small delay so Windows releases the camera
            Thread.sleep(600);

            ProcessBuilder pb = new ProcessBuilder(
                    PYTHON, SCRIPT, "capture", "--output", outPath);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Read output for error checking
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println("[capture] " + line);
                    if (line.startsWith("ERROR")) return null;
                }
            }
            p.waitFor();

            File f = new File(outPath);
            return f.exists() ? outPath : null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Stop everything ───────────────────────────────────
    public void stop() {
        previewRunning.set(false);
        cameraReady = false;
        if (previewThread != null) previewThread.interrupt();
        stopProcess();
    }

    private void stopProcess() {
        if (streamProcess != null && streamProcess.isAlive()) {
            streamProcess.destroyForcibly();
        }
    }

    public boolean isOpen() {
        return cameraReady && streamProcess != null && streamProcess.isAlive();
    }
}