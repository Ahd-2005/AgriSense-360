package com.example.agrisense360.entity;

import java.time.LocalDateTime;

public class MotionEvent {
    private int id;
    private int cameraId;
    private LocalDateTime detectionTime;
    private int motionFrameCount;
    private String severity; // "LOW", "MEDIUM", "HIGH"
    private long elapsedSeconds; // Elapsed time since monitoring started

    // Constructors
    public MotionEvent() {}

    public MotionEvent(int cameraId, LocalDateTime detectionTime, int motionFrameCount, String severity) {
        this.cameraId = cameraId;
        this.detectionTime = detectionTime;
        this.motionFrameCount = motionFrameCount;
        this.severity = severity;
        this.elapsedSeconds = 0;
    }
    
    public MotionEvent(int cameraId, LocalDateTime detectionTime, int motionFrameCount, String severity, long elapsedSeconds) {
        this.cameraId = cameraId;
        this.detectionTime = detectionTime;
        this.motionFrameCount = motionFrameCount;
        this.severity = severity;
        this.elapsedSeconds = elapsedSeconds;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCameraId() {
        return cameraId;
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    public LocalDateTime getDetectionTime() {
        return detectionTime;
    }

    public void setDetectionTime(LocalDateTime detectionTime) {
        this.detectionTime = detectionTime;
    }

    public int getMotionFrameCount() {
        return motionFrameCount;
    }

    public void setMotionFrameCount(int motionFrameCount) {
        this.motionFrameCount = motionFrameCount;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public long getElapsedSeconds() {
        return elapsedSeconds;
    }
    
    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    @Override
    public String toString() {
        return "MotionEvent{" +
                "id=" + id +
                ", cameraId=" + cameraId +
                ", detectionTime=" + detectionTime +
                ", motionFrameCount=" + motionFrameCount +
                ", severity='" + severity + '\'' +
                ", elapsedSeconds=" + elapsedSeconds +
                '}';
    }
}
