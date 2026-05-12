package entity;

import java.time.LocalDateTime;

public class farm {

    private int id;
    private String farmId;       // farm_id varchar(20) — unique business key
    private String name;
    private String location;
    private double surface;
    private String description;
    private String image;
    private LocalDateTime createdAt;
    private Integer ownerId;     // FK → user.id  (nullable in DB)

    public farm() {}

    public farm(int id, String farmId, String name, String location, double surface,
                String description, String image, LocalDateTime createdAt, Integer ownerId) {
        this.id = id;
        this.farmId = farmId;
        this.name = name;
        this.location = location;
        this.surface = surface;
        this.description = description;
        this.image = image;
        this.createdAt = createdAt;
        this.ownerId = ownerId;
    }

    // ===== GETTERS & SETTERS =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFarmId() { return farmId; }
    public void setFarmId(String farmId) { this.farmId = farmId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getSurface() { return surface; }
    public void setSurface(double surface) { this.surface = surface; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getOwnerId() { return ownerId; }
    public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }

    @Override
    public String toString() {
        return name + " (" + location + ")";
    }
}
