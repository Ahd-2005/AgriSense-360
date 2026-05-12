package entity;

import java.time.LocalDateTime;

public class application {

    public enum Status {
        PENDING,    // submitted, waiting owner review
        ACCEPTED,   // owner accepted
        REJECTED    // owner rejected
    }

    public enum DesiredRole {
        ROLE_GERANT,
        ROLE_OUVRIER
    }

    private int id;
    private int userId;          // applicant
    private int farmId;          // target farm
    private DesiredRole desiredRole;
    private String cvPath;       // path to uploaded CV file
    private Status status;
    private LocalDateTime appliedAt;

    // Joined fields (not stored in DB — loaded via JOIN)
    private String userName;
    private String userEmail;
    private String farmName;

    public application() {
        this.status = Status.PENDING;
    }

    public application(int userId, int farmId, DesiredRole desiredRole, String cvPath) {
        this.userId = userId;
        this.farmId = farmId;
        this.desiredRole = desiredRole;
        this.cvPath = cvPath;
        this.status = Status.PENDING;
    }

    // ===== GETTERS & SETTERS =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getFarmId() { return farmId; }
    public void setFarmId(int farmId) { this.farmId = farmId; }

    public DesiredRole getDesiredRole() { return desiredRole; }
    public void setDesiredRole(DesiredRole desiredRole) { this.desiredRole = desiredRole; }

    public String getCvPath() { return cvPath; }
    public void setCvPath(String cvPath) { this.cvPath = cvPath; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }
}
