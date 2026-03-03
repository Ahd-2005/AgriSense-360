package com.example.agrisense360.utils;

import com.example.agrisense360.entity.Camera;
import com.example.agrisense360.entity.MotionEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Simple in-memory session-based camera manager.
 * Everything stored in memory during the session - nothing persisted to disk.
 */
public class SessionCameraManager {
    private static SessionCameraManager instance;
    private ObservableList<Camera> cameras;
    private ObservableList<MotionEvent> motionEvents;
    private int cameraIdCounter = 1;
    private int motionEventIdCounter = 1;

    private SessionCameraManager() {
        this.cameras = FXCollections.observableArrayList();
        this.motionEvents = FXCollections.observableArrayList();
    }

    public static SessionCameraManager getInstance() {
        if (instance == null) {
            instance = new SessionCameraManager();
        }
        return instance;
    }

    public void addCamera(Camera camera) {
        if (camera.getId() == 0) {
            camera.setId(cameraIdCounter++);
        }
        cameras.add(camera);
    }

    public ObservableList<Camera> getAllCameras() {
        return cameras;
    }

    public Camera getCameraById(int id) {
        return cameras.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public void updateCamera(Camera camera) {
        int index = -1;
        for (int i = 0; i < cameras.size(); i++) {
            if (cameras.get(i).getId() == camera.getId()) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            cameras.set(index, camera);
        }
    }

    public void deleteCamera(int id) {
        cameras.removeIf(c -> c.getId() == id);
        // Also remove all motion events for this camera
        motionEvents.removeIf(e -> e.getCameraId() == id);
    }

    public void addMotionEvent(MotionEvent event) {
        if (event.getId() == 0) {
            event.setId(motionEventIdCounter++);
        }
        motionEvents.add(event);
    }

    public ObservableList<MotionEvent> getMotionEventsForCamera(int cameraId) {
        return FXCollections.observableArrayList(
                motionEvents.stream()
                        .filter(e -> e.getCameraId() == cameraId)
                        .toList()
        );
    }

    public void clearMotionEvents() {
        motionEvents.clear();
    }

    public void clearAll() {
        cameras.clear();
        motionEvents.clear();
        cameraIdCounter = 1;
        motionEventIdCounter = 1;
    }
}
