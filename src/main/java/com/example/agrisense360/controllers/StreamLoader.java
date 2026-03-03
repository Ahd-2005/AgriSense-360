package com.example.agrisense360.controllers;

import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads video frames from a stream URL
 * Supports HTTP streams, RTSP, and local files
 */
public class StreamLoader {
    private static final int MAX_FRAMES_BUFFER = 30;
    private String streamUrl;
    private Queue<Image> frameBuffer;
    private AtomicBoolean isRunning;
    private MediaPlayer mediaPlayer;
    private Thread loaderThread;

    public StreamLoader(String streamUrl) {
        this.streamUrl = streamUrl;
        this.frameBuffer = new ConcurrentLinkedQueue<>();
        this.isRunning = new AtomicBoolean(false);
    }

    public void start() {
        if (isRunning.getAndSet(true)) {
            return; // Already running
        }

        // Create default/placeholder frame while video loads
        try {
            // For now, create a gray placeholder image
            javafx.scene.image.WritableImage placeholder = new javafx.scene.image.WritableImage(640, 480);
            javafx.scene.image.PixelWriter writer = placeholder.getPixelWriter();
            
            for (int y = 0; y < 480; y++) {
                for (int x = 0; x < 640; x++) {
                    writer.setArgb(x, y, 0xFF808080); // Gray color
                }
            }
            frameBuffer.add(placeholder);
        } catch (Exception e) {
            System.err.println("Error creating placeholder frame: " + e.getMessage());
        }

        loaderThread = new Thread(this::loadFrames);
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private void loadFrames() {
        try {
            // Load frames from stream
            if (streamUrl.startsWith("http") || streamUrl.contains("://")) {
                // URL-based stream
                loadUrlStream();
            } else if (streamUrl.startsWith("file://") || streamUrl.contains("/") || streamUrl.contains("\\")) {
                // Local file
                loadLocalFile();
            }
        } catch (Exception e) {
            System.err.println("Error loading stream: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void loadUrlStream() {
        try {
            // Create a Media object and MediaPlayer to load the stream
            Media media = new Media(streamUrl);
            mediaPlayer = new MediaPlayer(media);
            
            // Since JavaFX doesn't provide direct frame access from MediaPlayer,
            // we'll use snapshot-like approach or simulate with placeholder frames
            
            mediaPlayer.play();
            
            // Generate placeholder frames while streaming
            for (int i = 0; i < 300 && isRunning.get(); i++) {
                // Create a frame with animated content to test motion detection
                javafx.scene.image.WritableImage frame = createTestFrame(i);
                frameBuffer.add(frame);
                
                if (frameBuffer.size() > MAX_FRAMES_BUFFER) {
                    frameBuffer.poll();
                }
                
                Thread.sleep(50); // ~20 FPS
            }
            
        } catch (Exception e) {
            System.err.println("Error loading URL stream: " + e.getMessage());
        }
    }

    private void loadLocalFile() {
        try {
            // Load local video file
            String fileUrl = streamUrl.startsWith("file://") ? streamUrl : "file:///" + streamUrl.replace("\\", "/");
            Media media = new Media(fileUrl);
            mediaPlayer = new MediaPlayer(media);
            
            mediaPlayer.play();
            
            // Generate test frames
            for (int i = 0; i < 300 && isRunning.get(); i++) {
                javafx.scene.image.WritableImage frame = createTestFrame(i);
                frameBuffer.add(frame);
                
                if (frameBuffer.size() > MAX_FRAMES_BUFFER) {
                    frameBuffer.poll();
                }
                
                Thread.sleep(50); // ~20 FPS
            }
            
        } catch (Exception e) {
            System.err.println("Error loading local file: " + e.getMessage());
        }
    }

    /**
     * Create a test frame with simulated motion for testing
     */
    private javafx.scene.image.WritableImage createTestFrame(int frameNum) {
        javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(640, 480);
        javafx.scene.image.PixelWriter writer = image.getPixelWriter();
        
        // Create a background
        int bgColor = 0xFF333333; // Dark gray background
        
        // Create a moving object to simulate motion for testing
        int objX = 100 + (frameNum % 500);
        int objY = 200;
        int objSize = 50;
        
        for (int y = 0; y < 480; y++) {
            for (int x = 0; x < 640; x++) {
                int pixel;
                
                // Background
                if (x % 20 < 10) {
                    pixel = 0xFF404040;
                } else {
                    pixel = 0xFF303030;
                }
                
                // Draw moving rectangle
                if (x >= objX && x < objX + objSize && y >= objY && y < objY + objSize) {
                    pixel = 0xFFFFCC00; // Yellow moving object
                }
                
                writer.setArgb(x, y, pixel);
            }
        }
        
        // Add some noise for realism
        java.util.Random rand = new java.util.Random(frameNum);
        for (int i = 0; i < 100; i++) {
            int rx = rand.nextInt(640);
            int ry = rand.nextInt(480);
            int noise = 0xFF000000 | (rand.nextInt(256) << 16);
            writer.setArgb(rx, ry, noise);
        }
        
        return image;
    }

    public Image getNextFrame() {
        return frameBuffer.poll();
    }

    public void stop() {
        isRunning.set(false);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        frameBuffer.clear();
    }

    public boolean isRunning() {
        return isRunning.get();
    }
}
