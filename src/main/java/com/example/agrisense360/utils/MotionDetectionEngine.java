package com.example.agrisense360.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

public class MotionDetectionEngine {
    // Sensitivity levels for different times
    public enum SensitivityLevel {
        NIGHT(0.01, 5, 3),       // EXTREMELY sensitive for night: 1% threshold, 5 min area, 3 frames
        DAY(0.08, 30, 4);         // Very sensitive for day: 8% threshold, 30 min area, 4 frames

        public final double diffThreshold;
        public final int minMotionArea;
        public final int frameThreshold;

        SensitivityLevel(double diffThreshold, int minMotionArea, int frameThreshold) {
            this.diffThreshold = diffThreshold;
            this.minMotionArea = minMotionArea;
            this.frameThreshold = frameThreshold;
        }
    }

    private Image previousFrame = null;
    private int motionCountdown = 0;
    private SensitivityLevel sensitivity;

    public MotionDetectionEngine(SensitivityLevel sensitivity) {
        this.sensitivity = sensitivity;
    }

    public void setSensitivityLevel(SensitivityLevel sensitivity) {
        this.sensitivity = sensitivity;
    }

    /**
     * Detect motion between frames using pixel comparison
     * @param currentFrame The current frame Image object
     * @return true if significant motion is detected, false otherwise
     */
    public boolean detectMotion(Image currentFrame) {
        if (currentFrame == null) {
            return false;
        }

        if (previousFrame == null) {
            previousFrame = currentFrame;
            return false;
        }

        // Compare frames pixel by pixel
        int diffPixels = countDifferentPixels(previousFrame, currentFrame);
        int totalPixels = (int) (currentFrame.getWidth() * currentFrame.getHeight());
        double motionPercentage = (double) diffPixels / totalPixels;

        previousFrame = currentFrame;

        // Compare against sensitivity threshold
        boolean hasMotion = motionPercentage > sensitivity.diffThreshold;

        if (hasMotion) {
            motionCountdown = sensitivity.frameThreshold;
        } else {
            motionCountdown = Math.max(0, motionCountdown - 1);
        }

        return motionCountdown > 0;
    }

    /**
     * Count pixels that differ significantly between two frames
     */
    private int countDifferentPixels(Image img1, Image img2) {
        try {
            int width = (int) Math.min(img1.getWidth(), img2.getWidth());
            int height = (int) Math.min(img1.getHeight(), img2.getHeight());
            
            PixelReader reader1 = img1.getPixelReader();
            PixelReader reader2 = img2.getPixelReader();
            
            int diffCount = 0;
            int step = 4; // Sample every 4 pixels for performance

            for (int y = 0; y < height; y += step) {
                for (int x = 0; x < width; x += step) {
                    int pixel1 = reader1.getArgb(x, y);
                    int pixel2 = reader2.getArgb(x, y);

                    if (colorDifference(pixel1, pixel2) > 12) {
                        diffCount += (step * step); // Count as multiple pixels
                    }
                }
            }

            return diffCount;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Calculate color difference between two ARGB pixels
     */
    private int colorDifference(int pixel1, int pixel2) {
        int a1 = (pixel1 >> 24) & 0xFF;
        int r1 = (pixel1 >> 16) & 0xFF;
        int g1 = (pixel1 >> 8) & 0xFF;
        int b1 = pixel1 & 0xFF;

        int a2 = (pixel2 >> 24) & 0xFF;
        int r2 = (pixel2 >> 16) & 0xFF;
        int g2 = (pixel2 >> 8) & 0xFF;
        int b2 = pixel2 & 0xFF;

        int dr = Math.abs(r1 - r2);
        int dg = Math.abs(g1 - g2);
        int db = Math.abs(b1 - b2);

        return (dr + dg + db) / 3;
    }

    /**
     * Get the current motion countdown value
     * @return motion countdown frames remaining
     */
    public int getMotionCountdown() {
        return motionCountdown;
    }

    /**
     * Reset motion detection state
     */
    public void reset() {
        motionCountdown = 0;
        previousFrame = null;
    }

    /**
     * Determine motion severity based on countdown
     * @return Severity level: LOW, MEDIUM, or HIGH
     */
    public static String determineSeverity(double motionRatio) {
        if (motionRatio < 0.3) {
            return "LOW";
        } else if (motionRatio < 0.7) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }
}
