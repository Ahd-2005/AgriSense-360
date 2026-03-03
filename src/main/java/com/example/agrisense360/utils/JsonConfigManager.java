package com.example.agrisense360.utils;

import com.example.agrisense360.entity.Camera;
import com.example.agrisense360.entity.MotionEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * File-based configuration manager using JSON
 * Stores cameras and motion events in local JSON files
 */
public class JsonConfigManager {
    private static JsonConfigManager instance;
    private final String appDataDir;
    private final String camerasFile;
    private final String motionEventsFile;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private JsonConfigManager() {
        // Create app data directory in user's home
        String userHome = System.getProperty("user.home");
        this.appDataDir = userHome + File.separator + ".agrisense360";
        this.camerasFile = appDataDir + File.separator + "cameras.json";
        this.motionEventsFile = appDataDir + File.separator + "motion_events.json";

        initializeDirectories();
    }

    public static JsonConfigManager getInstance() {
        if (instance == null) {
            instance = new JsonConfigManager();
        }
        return instance;
    }

    private void initializeDirectories() {
        try {
            File dir = new File(appDataDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // Create files if they don't exist
            if (!new File(camerasFile).exists()) {
                saveJson(camerasFile, new JSONArray());
            }
            if (!new File(motionEventsFile).exists()) {
                saveJson(motionEventsFile, new JSONArray());
            }
        } catch (IOException e) {
            System.err.println("Error initializing directories: " + e.getMessage());
        }
    }

    // ========== CAMERA OPERATIONS ==========

    public List<Camera> loadAllCameras() {
        List<Camera> cameras = new ArrayList<>();
        try {
            JSONArray jsonArray = loadJson(camerasFile);
            for (int i = 0; i < jsonArray.length(); i++) {
                cameras.add(jsonToCamera(jsonArray.getJSONObject(i)));
            }
        } catch (IOException e) {
            System.err.println("Error loading cameras: " + e.getMessage());
        }
        return cameras;
    }

    public void saveCamera(Camera camera) {
        try {
            JSONArray jsonArray = loadJson(camerasFile);
            JSONObject cameraJson = cameraToJson(camera);

            if (camera.getId() == 0) {
                // New camera - generate ID
                int newId = jsonArray.length() > 0 ? 
                    jsonArray.getJSONObject(jsonArray.length() - 1).getInt("id") + 1 : 1;
                cameraJson.put("id", newId);
                camera.setId(newId);
                jsonArray.put(cameraJson);
            } else {
                // Update existing
                for (int i = 0; i < jsonArray.length(); i++) {
                    if (jsonArray.getJSONObject(i).getInt("id") == camera.getId()) {
                        jsonArray.put(i, cameraJson);
                        break;
                    }
                }
            }
            saveJson(camerasFile, jsonArray);
        } catch (IOException e) {
            System.err.println("Error saving camera: " + e.getMessage());
        }
    }

    public void deleteCamera(int cameraId) {
        try {
            JSONArray jsonArray = loadJson(camerasFile);
            for (int i = 0; i < jsonArray.length(); i++) {
                if (jsonArray.getJSONObject(i).getInt("id") == cameraId) {
                    jsonArray.remove(i);
                    break;
                }
            }
            saveJson(camerasFile, jsonArray);
            
            // Also delete motion events for this camera
            deleteMotionEventsForCamera(cameraId);
        } catch (IOException e) {
            System.err.println("Error deleting camera: " + e.getMessage());
        }
    }

    // ========== MOTION EVENT OPERATIONS ==========

    public List<MotionEvent> loadMotionEventsForCamera(int cameraId) {
        List<MotionEvent> events = new ArrayList<>();
        try {
            JSONArray jsonArray = loadJson(motionEventsFile);
            for (int i = 0; i < jsonArray.length(); i++) {
                MotionEvent event = jsonToMotionEvent(jsonArray.getJSONObject(i));
                if (event.getCameraId() == cameraId) {
                    events.add(event);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading motion events: " + e.getMessage());
        }
        return events;
    }

    public void saveMotionEvent(MotionEvent event) {
        try {
            JSONArray jsonArray = loadJson(motionEventsFile);
            JSONObject eventJson = motionEventToJson(event);

            if (event.getId() == 0) {
                // New event
                int newId = jsonArray.length() > 0 ? 
                    jsonArray.getJSONObject(jsonArray.length() - 1).getInt("id") + 1 : 1;
                eventJson.put("id", newId);
                event.setId(newId);
                jsonArray.put(eventJson);
            }
            saveJson(motionEventsFile, jsonArray);
        } catch (IOException e) {
            System.err.println("Error saving motion event: " + e.getMessage());
        }
    }

    public void deleteMotionEventsForCamera(int cameraId) {
        try {
            JSONArray jsonArray = loadJson(motionEventsFile);
            for (int i = jsonArray.length() - 1; i >= 0; i--) {
                if (jsonArray.getJSONObject(i).getInt("cameraId") == cameraId) {
                    jsonArray.remove(i);
                }
            }
            saveJson(motionEventsFile, jsonArray);
        } catch (IOException e) {
            System.err.println("Error deleting motion events: " + e.getMessage());
        }
    }

    // ========== JSON CONVERSION ==========

    private JSONObject cameraToJson(Camera camera) {
        JSONObject obj = new JSONObject();
        obj.put("id", camera.getId());
        obj.put("cameraName", camera.getCameraName());
        obj.put("streamUrl", camera.getStreamUrl());
        obj.put("location", camera.getLocation());
        obj.put("equipmentId", camera.getEquipmentId());
        obj.put("sensitivityLevel", camera.getSensitivityLevel());
        obj.put("alertsEnabled", camera.isAlertsEnabled());
        obj.put("isActive", camera.isActive());
        obj.put("createdDate", camera.getCreatedDate() != null ? 
            camera.getCreatedDate().format(dateFormatter) : LocalDateTime.now().format(dateFormatter));
        return obj;
    }

    private Camera jsonToCamera(JSONObject obj) {
        Camera camera = new Camera();
        camera.setId(obj.getInt("id"));
        camera.setCameraName(obj.getString("cameraName"));
        camera.setStreamUrl(obj.getString("streamUrl"));
        camera.setLocation(obj.optString("location", null));
        camera.setEquipmentId(obj.has("equipmentId") && !obj.isNull("equipmentId") ? 
            obj.getInt("equipmentId") : null);
        camera.setSensitivityLevel(obj.getString("sensitivityLevel"));
        camera.setAlertsEnabled(obj.getBoolean("alertsEnabled"));
        camera.setActive(obj.getBoolean("isActive"));
        
        if (obj.has("createdDate")) {
            camera.setCreatedDate(LocalDateTime.parse(obj.getString("createdDate"), dateFormatter));
        }
        
        return camera;
    }

    private JSONObject motionEventToJson(MotionEvent event) {
        JSONObject obj = new JSONObject();
        obj.put("id", event.getId());
        obj.put("cameraId", event.getCameraId());
        obj.put("detectionTime", event.getDetectionTime().format(dateFormatter));
        obj.put("motionFrameCount", event.getMotionFrameCount());
        obj.put("severity", event.getSeverity());
        return obj;
    }

    private MotionEvent jsonToMotionEvent(JSONObject obj) {
        MotionEvent event = new MotionEvent();
        event.setId(obj.getInt("id"));
        event.setCameraId(obj.getInt("cameraId"));
        event.setDetectionTime(LocalDateTime.parse(obj.getString("detectionTime"), dateFormatter));
        event.setMotionFrameCount(obj.getInt("motionFrameCount"));
        event.setSeverity(obj.getString("severity"));
        return event;
    }

    // ========== FILE I/O ==========

    private JSONArray loadJson(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        return new JSONArray(content.isEmpty() ? "[]" : content);
    }

    private void saveJson(String filePath, JSONArray jsonArray) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(jsonArray.toString(2)); // Pretty print with 2-space indent
        }
    }
}
