package com.example.agrisense360.utils;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads real video frames from files using FFmpeg
 */
public class StreamLoader {
    private static final int TARGET_FPS = 15;
    private static final int FRAME_DELAY_MS = 1000 / TARGET_FPS;
    private static final long YOUTUBE_RESOLVE_CACHE_MS = 240_000;
    private static volatile String cachedYtDlpExecutable;
    private static volatile String cachedYoutubeInputUrl;
    private static volatile String cachedYoutubePlayableUrl;
    private static volatile long cachedYoutubeResolvedAt;

    private String streamUrl;
    private boolean isRunning = false;
    private BlockingQueue<Image> frameQueue;
    private ExecutorService executor;
    private AtomicReference<Image> lastFrame = new AtomicReference<>();
    private Process ffmpegProcess;
    private AtomicBoolean ffmpegRunning = new AtomicBoolean(false);
    private final AtomicBoolean warmupInProgress = new AtomicBoolean(false);
    private volatile String preparedInputUrl;
    private static final String YT_DLP_RELEASE_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";
    
    // EXACT FFmpeg location where user installed it
    private static final String FFMPEG_PATH = 
        "C:\\Users\\rayen\\Downloads\\ffmpeg-2026-02-18-git-52b676bb29-essentials_build\\ffmpeg-2026-02-18-git-52b676bb29-essentials_build\\bin\\ffmpeg.exe";

    public StreamLoader(String streamUrl) {
        this.streamUrl = streamUrl;
        this.frameQueue = new LinkedBlockingQueue<>(30);
        this.executor = null;
        this.preparedInputUrl = null;
    }

    private ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "StreamLoader-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        if (isRunning) return;
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = createExecutor();
        }
        isRunning = true;
        frameQueue.clear();
        lastFrame.set(null);

        executor.submit(() -> {
            try {
                System.out.println("\n=== VIDEO LOAD START ===");
                System.out.println("Video URL: " + streamUrl);
                
                // Check FFmpeg exists
                File ffmpegFile = new File(FFMPEG_PATH);
                if (!ffmpegFile.exists()) {
                    System.out.println("✗ FFmpeg NOT found at:");
                    System.out.println("  " + FFMPEG_PATH);
                    System.out.println("\nYour FFmpeg folder is at:");
                    System.out.println("  C:\\Users\\rayen\\Downloads\\ffmpeg-2026-02-18-git-52b676bb29-essentials_build\\");
                    System.out.println("\nPlease move ffmpeg.exe to the path above");
                    System.out.println("==================\n");
                    generateTestFrames();
                    return;
                }
                
                System.out.println("✓ FFmpeg found");

                if (isRemoteStreamUrl(streamUrl)) {
                    System.out.println("✓ Remote stream URL detected");
                } else {
                    File videoFile = new File(streamUrl);
                    if (!videoFile.exists()) {
                        System.out.println("✗ Video file NOT found:");
                        System.out.println("  " + streamUrl);
                        System.out.println("==================\n");
                        generateTestFrames();
                        return;
                    }
                    System.out.println("✓ Video file found (" + (videoFile.length() / 1024 / 1024) + " MB)");
                }
                
                loadVideoFrames();
                System.out.println("==================\n");
            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
                e.printStackTrace();
                System.out.println("==================\n");
                generateTestFrames();
            }
        });
    }

    public void warmUpAsync() {
        if (!isRemoteStreamUrl(streamUrl)) {
            return;
        }
        if (preparedInputUrl != null && !preparedInputUrl.isBlank()) {
            return;
        }
        if (!warmupInProgress.compareAndSet(false, true)) {
            return;
        }

        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = createExecutor();
        }

        executor.submit(() -> {
            try {
                preparedInputUrl = resolvePlayableStreamUrl(streamUrl);
                if (preparedInputUrl != null && !preparedInputUrl.isBlank()) {
                    System.out.println("✓ Stream warm-up complete");
                }
            } catch (Exception ignored) {
            } finally {
                warmupInProgress.set(false);
            }
        });
    }

    public void stop() {
        isRunning = false;
        if (ffmpegProcess != null) {
            try {
                ffmpegProcess.destroyForcibly();
            } catch (Exception e) {
                e.printStackTrace();
            }
            ffmpegProcess = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public Image getNextFrame() {
        try {
            Image frame = frameQueue.poll(40, TimeUnit.MILLISECONDS);
            if (frame != null) {
                lastFrame.set(frame);
                return frame;
            }
            return lastFrame.get();
        } catch (InterruptedException e) {
            return lastFrame.get();
        }
    }

    private void loadVideoFrames() throws Exception {
        if (isRemoteStreamUrl(streamUrl)) {
            loadRemoteStreamWithFFmpeg(streamUrl);
            return;
        }

        if (isLikelyLocalPath(streamUrl)) {
            File videoFile = new File(streamUrl);
            if (videoFile.exists()) {
                loadLocalVideoWithFFmpeg(videoFile);
                return;
            }
        }

        generateTestFrames();
    }

    private boolean isLikelyLocalPath(String value) {
        if (value == null || value.isBlank()) return false;
        return (value.length() > 1 && value.charAt(1) == ':') || value.startsWith("\\\\") || value.startsWith("/");
    }

    private boolean isRemoteStreamUrl(String value) {
        if (value == null) return false;
        return value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");
    }

    private boolean isYouTubeUrl(String value) {
        if (value == null) return false;
        String lower = value.toLowerCase();
        return lower.contains("youtube.com") || lower.contains("youtu.be");
    }

    private String resolvePlayableStreamUrl(String url) {
        if (!isYouTubeUrl(url)) {
            return url;
        }

        if (url.equals(cachedYoutubeInputUrl)
                && cachedYoutubePlayableUrl != null
                && (System.currentTimeMillis() - cachedYoutubeResolvedAt) < YOUTUBE_RESOLVE_CACHE_MS) {
            System.out.println("✓ Using cached YouTube stream URL");
            return cachedYoutubePlayableUrl;
        }

        System.out.println("YouTube URL detected - resolving direct stream URL...");
        String ytDlpExecutable = ensureYtDlpExecutable();
        if (ytDlpExecutable == null) {
            System.out.println("⚠ Could not find or download yt-dlp. YouTube livestream requires yt-dlp.");
            return url;
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(ytDlpExecutable);
        cmd.add("--no-playlist");
        cmd.add("--no-warnings");
        cmd.add("--prefer-free-formats");
        cmd.add("-g");
        cmd.add("-f");
        cmd.add("best[ext=mp4]/best");
        cmd.add(url);

        try {
            Process process = new ProcessBuilder(cmd).start();
            String resolved;
            String errorOutput;
            try (BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                resolved = stdout.readLine();
                errorOutput = stderr.lines().reduce("", (a, b) -> a + (a.isEmpty() ? "" : "\n") + b);
                int code = process.waitFor();
                if (code == 0 && resolved != null && !resolved.isBlank()) {
                    System.out.println("✓ YouTube stream URL resolved");
                    String playableUrl = resolved.trim();
                    cachedYoutubeInputUrl = url;
                    cachedYoutubePlayableUrl = playableUrl;
                    cachedYoutubeResolvedAt = System.currentTimeMillis();
                    return playableUrl;
                }
                if (!errorOutput.isBlank()) {
                    System.out.println("yt-dlp details: " + errorOutput);
                }
            }
            System.out.println("⚠ Could not resolve YouTube URL with yt-dlp. Falling back to original URL.");
        } catch (Exception e) {
            System.out.println("⚠ yt-dlp not available or failed: " + e.getMessage());
            System.out.println("  Install yt-dlp and ensure it's in PATH for best YouTube livestream support.");
        }

        return url;
    }

    private String ensureYtDlpExecutable() {
        if (cachedYtDlpExecutable != null && isExecutableAvailable(cachedYtDlpExecutable)) {
            return cachedYtDlpExecutable;
        }

        String[] candidates = new String[] {
                "yt-dlp",
                "yt-dlp.exe",
                new File(System.getProperty("user.home"), "AppData\\Local\\Programs\\yt-dlp\\yt-dlp.exe").getAbsolutePath(),
                new File(System.getProperty("user.home"), ".agrisense\\tools\\yt-dlp.exe").getAbsolutePath()
        };

        for (String candidate : candidates) {
            if (isExecutableAvailable(candidate)) {
                cachedYtDlpExecutable = candidate;
                return candidate;
            }
        }

        File target = new File(System.getProperty("user.home"), ".agrisense\\tools\\yt-dlp.exe");
        if (downloadYtDlp(target) && isExecutableAvailable(target.getAbsolutePath())) {
            cachedYtDlpExecutable = target.getAbsolutePath();
            return cachedYtDlpExecutable;
        }

        return null;
    }

    private boolean isExecutableAvailable(String command) {
        try {
            Process process = new ProcessBuilder(command, "--version").start();
            int code = process.waitFor();
            return code == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean downloadYtDlp(File targetFile) {
        try {
            System.out.println("yt-dlp not found. Downloading yt-dlp.exe...");
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            HttpURLConnection connection = (HttpURLConnection) new URL(YT_DLP_RELEASE_URL).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setRequestProperty("User-Agent", "AgrISense360/1.0");

            try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            targetFile.setExecutable(true);
            System.out.println("✓ yt-dlp downloaded to: " + targetFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            System.out.println("✗ Failed to download yt-dlp: " + e.getMessage());
            return false;
        }
    }

    private void loadRemoteStreamWithFFmpeg(String url) throws Exception {
        System.out.println("Starting remote stream extraction...");
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "camera_frames_" + System.nanoTime());
        tempDir.mkdirs();
        System.out.println("Temp dir: " + tempDir.getAbsolutePath());

        String inputUrl = preparedInputUrl;
        if (inputUrl == null || inputUrl.isBlank()) {
            inputUrl = resolvePlayableStreamUrl(url);
            preparedInputUrl = inputUrl;
        } else {
            System.out.println("✓ Using pre-resolved stream URL");
        }

        List<String> cmdList = new ArrayList<>();
        cmdList.add(FFMPEG_PATH);
        cmdList.add("-y");
        cmdList.add("-hide_banner");
        cmdList.add("-loglevel");
        cmdList.add("warning");
        cmdList.add("-fflags");
        cmdList.add("nobuffer");
        cmdList.add("-flags");
        cmdList.add("low_delay");
        cmdList.add("-analyzeduration");
        cmdList.add("0");
        cmdList.add("-probesize");
        cmdList.add("32768");
        cmdList.add("-rw_timeout");
        cmdList.add("15000000");
        cmdList.add("-reconnect");
        cmdList.add("1");
        cmdList.add("-reconnect_streamed");
        cmdList.add("1");
        cmdList.add("-reconnect_delay_max");
        cmdList.add("2");
        cmdList.add("-i");
        cmdList.add(inputUrl);
        cmdList.add("-vf");
        cmdList.add("fps=" + TARGET_FPS + ",scale=640:480");
        cmdList.add("-frames:v");
        cmdList.add("600");
        cmdList.add(tempDir.getAbsolutePath() + File.separator + "frame_%04d.ppm");

        try {
            System.out.println("Running FFmpeg on remote stream...");
            ProcessBuilder pb = new ProcessBuilder(cmdList);
            ffmpegProcess = pb.start();
            ffmpegRunning.set(true);
            Thread ffmpegErrThread = new Thread(() -> {
                try (BufferedReader ffmpegErr = new BufferedReader(new InputStreamReader(ffmpegProcess.getErrorStream()))) {
                    String line;
                    while ((line = ffmpegErr.readLine()) != null) {
                        if (line.toLowerCase().contains("error")) {
                            System.out.println("FFmpeg: " + line);
                        }
                    }
                } catch (Exception ignored) {
                }
            }, "FFmpeg-Err-Drain");
            ffmpegErrThread.setDaemon(true);
            ffmpegErrThread.start();

            int nextFrameIndex = 1;
            int frameCount = 0;
            long lastFrameAt = System.currentTimeMillis();

            while (isRunning) {
                String frameName = String.format("frame_%04d.ppm", nextFrameIndex);
                File frameFile = new File(tempDir, frameName);

                if (frameFile.exists()) {
                    try {
                        Image frame = loadPPMFrame(frameFile);
                        if (frame != null) {
                            frameQueue.offer(frame);
                            frameCount++;
                            lastFrameAt = System.currentTimeMillis();
                            if (frameCount == 1) {
                                System.out.println("✓✓✓ FIRST REMOTE FRAME LOADED ✓✓✓");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error loading remote frame " + frameFile.getName() + ": " + e.getMessage());
                    }
                    frameFile.delete();
                    nextFrameIndex++;
                    Thread.sleep(FRAME_DELAY_MS);
                    continue;
                }

                boolean processAlive = ffmpegProcess != null && ffmpegProcess.isAlive();
                if (!processAlive) {
                    if (System.currentTimeMillis() - lastFrameAt > 1500) {
                        break;
                    }
                }
                Thread.sleep(100);
            }

            if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                ffmpegProcess.destroyForcibly();
            }
            ffmpegRunning.set(false);

            if (frameCount == 0) {
                System.out.println("✗ No frames extracted from remote stream");
                generateTestFrames();
                cleanupTempDir(tempDir);
                return;
            }

            System.out.println("✓ Remote stream playback complete: " + frameCount + " frames");
            cleanupTempDir(tempDir);
        } catch (Exception e) {
            System.out.println("✗ Remote stream error: " + e.getMessage());
            cleanupTempDir(tempDir);
            generateTestFrames();
        }
    }

    private void loadLocalVideoWithFFmpeg(File videoFile) throws Exception {
        System.out.println("Starting FFmpeg extraction...");
        System.out.println("Using temp directory for frame extraction...");
        
        // Create temp directory for frames
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "camera_frames_" + System.nanoTime());
        tempDir.mkdirs();
        System.out.println("Temp dir: " + tempDir.getAbsolutePath());
        
        // FFmpeg will extract frames to files instead of pipe
        java.util.List<String> cmdList = new java.util.ArrayList<>();
        cmdList.add(FFMPEG_PATH);
        cmdList.add("-y");
        cmdList.add("-hide_banner");
        cmdList.add("-loglevel");
        cmdList.add("warning");
        cmdList.add("-i");
        cmdList.add(videoFile.getAbsolutePath());
        cmdList.add("-vf");
        cmdList.add("fps=" + TARGET_FPS + ",scale=640:480");
        cmdList.add(tempDir.getAbsolutePath() + File.separator + "frame_%04d.ppm");

        try {
            System.out.println("Running FFmpeg to extract frames...");
            ProcessBuilder pb = new ProcessBuilder(cmdList);
            ffmpegProcess = pb.start();

            int nextFrameIndex = 1;
            int frameCount = 0;
            long lastFrameAt = System.currentTimeMillis();

            while (isRunning) {
                String frameName = String.format("frame_%04d.ppm", nextFrameIndex);
                File frameFile = new File(tempDir, frameName);

                if (frameFile.exists()) {
                    try {
                        Image frame = loadPPMFrame(frameFile);
                        if (frame != null) {
                            frameQueue.offer(frame);
                            frameCount++;
                            lastFrameAt = System.currentTimeMillis();
                            if (frameCount == 1) {
                                System.out.println("✓✓✓ FIRST FRAME LOADED - VIDEO IS WORKING! ✓✓✓");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error loading frame " + frameFile.getName() + ": " + e.getMessage());
                    }
                    frameFile.delete();
                    nextFrameIndex++;
                    Thread.sleep(FRAME_DELAY_MS);
                    continue;
                }

                boolean processAlive = ffmpegProcess != null && ffmpegProcess.isAlive();
                if (!processAlive) {
                    if (System.currentTimeMillis() - lastFrameAt > 1500) {
                        break;
                    }
                }
                Thread.sleep(35);
            }

            if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                ffmpegProcess.destroyForcibly();
            }

            if (frameCount == 0) {
                System.out.println("✗ No frames extracted!");
                generateTestFrames();
                cleanupTempDir(tempDir);
                return;
            }

            System.out.println("✓ Video complete: " + frameCount + " frames");
            cleanupTempDir(tempDir);
            
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
            cleanupTempDir(tempDir);
            generateTestFrames();
        }
    }
    
    private Image loadPPMFrame(File ppmFile) throws Exception {
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(ppmFile, "r");
        
        try {
            // Read PPM header
            String magic = raf.readLine(); // P6
            String dimensions = raf.readLine(); // 640 480
            String maxval = raf.readLine(); // 255
            
            String[] dims = dimensions.trim().split("\\s+");
            int width = Integer.parseInt(dims[0]);
            int height = Integer.parseInt(dims[1]);
            
            WritableImage image = new WritableImage(width, height);
            PixelWriter writer = image.getPixelWriter();
            
            // Read raw RGB data
            byte[] rgb = new byte[width * height * 3];
            raf.read(rgb);
            
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int r = rgb[index++] & 0xFF;
                    int g = rgb[index++] & 0xFF;
                    int b = rgb[index++] & 0xFF;
                    int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
                    writer.setArgb(x, y, argb);
                }
            }
            
            return image;
        } finally {
            raf.close();
        }
    }
    
    private void cleanupTempDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        dir.delete();
        System.out.println("Cleaned up temp directory");
    }

    private void generateTestFrames() {
        System.out.println("\n!!! SHOWING TEST ANIMATION (video failed to load) !!!\n");
        for (int i = 0; i < 500 && isRunning; i++) {
            Image testFrame = createTestFrame(i);
            frameQueue.offer(testFrame);

            try {
                Thread.sleep(FRAME_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private Image createTestFrame(int frameNumber) {
        WritableImage image = new WritableImage(640, 480);
        PixelWriter writer = image.getPixelWriter();

        // Dark background
        for (int y = 0; y < 480; y++) {
            for (int x = 0; x < 640; x++) {
                writer.setArgb(x, y, 0xFF1a1a1a);
            }
        }

        // Moving circle
        int centerX = 320 + (frameNumber % 100) - 50;
        int centerY = 240;
        drawCircle(writer, centerX, centerY, 30, 0xFF00FF00);

        return image;
    }

    private Image bytesToImage(byte[] rgb, int width, int height) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();

        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (index + 2 < rgb.length) {
                    int r = rgb[index++] & 0xFF;
                    int g = rgb[index++] & 0xFF;
                    int b = rgb[index++] & 0xFF;
                    int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
                    writer.setArgb(x, y, argb);
                } else {
                    writer.setArgb(x, y, 0xFF000000);
                }
            }
        }

        return image;
    }

    private void drawCircle(PixelWriter writer, int centerX, int centerY, int radius, int argb) {
        for (int y = Math.max(0, centerY - radius); y < Math.min(480, centerY + radius); y++) {
            for (int x = Math.max(0, centerX - radius); x < Math.min(640, centerX + radius); x++) {
                if (Math.hypot(x - centerX, y - centerY) < radius) {
                    writer.setArgb(x, y, argb);
                }
            }
        }
    }
}


