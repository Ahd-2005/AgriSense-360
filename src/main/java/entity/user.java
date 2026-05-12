package entity;

public class user {

    private int id;
    private String name;
    private String email;
    private String password;
    private String phone;
    private Role role;
    private String status; // ACTIVE or BLOCKED
    private String profilePicture;

    public enum Role {
        ROLE_PENDING,   // signed up but not yet accepted into any farm
        ROLE_OWNER,     // owns a farm, can accept/reject applications
        ROLE_GERANT,    // assigned by owner as manager
        ROLE_OUVRIER,   // assigned by owner as worker
        ROLE_ADMIN      // system admin
    }

    // Empty constructor
    public user() {
        this.status = "ACTIVE";
    }

    // Constructor without id (for signup)
    public user(String name, String email, String password, String phone, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role;
        this.status = "ACTIVE";
    }

    // Constructor with id (for DB fetch)
    public user(int id, String name, String email, String password, String phone, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role;
        this.status = "ACTIVE";
    }

    // Full constructor with status
    public user(int id, String name, String email, String password, String phone, Role role, String status, String profilePicture) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.profilePicture = profilePicture;
    }

    // ===== GETTERS & SETTERS =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRoleAsString() { return role.name(); }

    public void setRoleFromString(String roleString) {
        this.role = Role.valueOf(roleString);
    }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "', role=" + role + ", status='" + status + "'}";
    }
}