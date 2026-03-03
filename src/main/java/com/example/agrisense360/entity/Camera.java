package com.example.agrisense360.entity;

import java.time.LocalDateTime;

public class Camera {
    private int id;
    private String cameraName;
    private String streamUrl;
    private String location;
    private Integer equipmentId;
    private String sensitivityLevel; // "NIGHT" or "DAY"
    private boolean alertsEnabled;
    private boolean isActive;
    private LocalDateTime createdDate;

    // Constructors
    public Camera() {}

    public Camera(String cameraName, String streamUrl, String location, String sensitivityLevel) {
        this.cameraName = cameraName;
        this.streamUrl = streamUrl;
        this.location = location;
        this.sensitivityLevel = sensitivityLevel;
        this.alertsEnabled = true;
        this.isActive = true;
    }

    public Camera(String cameraName, String streamUrl, String location, Integer equipmentId, String sensitivityLevel) {
        this.cameraName = cameraName;
        this.streamUrl = streamUrl;
        this.location = location;
        this.equipmentId = equipmentId;
        this.sensitivityLevel = sensitivityLevel;
        this.alertsEnabled = true;
        this.isActive = true;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Integer equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getSensitivityLevel() {
        return sensitivityLevel;
    }

    public void setSensitivityLevel(String sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }

    public boolean isAlertsEnabled() {
        return alertsEnabled;
    }

    public void setAlertsEnabled(boolean alertsEnabled) {
        this.alertsEnabled = alertsEnabled;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public String toString() {
        return "Camera{" +
                "id=" + id +
                ", cameraName='" + cameraName + '\'' +
                ", streamUrl='" + streamUrl + '\'' +
                ", location='" + location + '\'' +
                ", sensitivityLevel='" + sensitivityLevel + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
