package services;

import entity.user;
import entity.user.Role;
import utils.MyDataBase;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class userservice {

    private Connection cnx;

    public userservice() throws SQLException {
        cnx = MyDataBase.getInstance().getCnx();
    }

    // ===============================
    // SIGN UP  (password hashed here)
    // ===============================
    public void ajouter(user user) throws SQLException {
        // ✅ Hash the plain-text password before storing
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));

        // ✅ Updated for Symfony compatibility (roles as JSON, added status, first_login, auth_provider)
        String sql = "INSERT INTO user (name, email, password, phone, roles, status, profile_picture, first_login, auth_provider, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, user.getName());
        ps.setString(2, user.getEmail());
        ps.setString(3, hashedPassword);          
        ps.setString(4, user.getPhone());
        
        // Store role as JSON array for Symfony
        ps.setString(5, "[\"" + user.getRole().name() + "\"]"); 
        
        ps.setString(6, user.getStatus()); // Use object status
        ps.setString(7, user.getProfilePicture());
        ps.setBoolean(8, true); // first_login
        ps.setString(9, "local"); // auth_provider
        ps.setTimestamp(10, new java.sql.Timestamp(System.currentTimeMillis()));
        
        ps.executeUpdate();
        System.out.println("✅ User inserted with hashed password and JSON roles");
    }

    public void updateProfilePicture(int userId, String pictureUrl) throws SQLException {
        String sql = "UPDATE user SET profile_picture = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, pictureUrl);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    // ===============================
    // LOGIN  (BCrypt.checkpw)
    // ===============================
    public user login(String email, String plainPassword) {
        // We fetch by email only, then verify password with BCrypt
        String sql = "SELECT * FROM user WHERE email = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                String status     = rs.getString("status");

                // ✅ FIX: Symfony uses $2y$ which jBCrypt doesn't support. 
                // We convert it to $2a$ (they are identical algorithms).
                String compatibleHash = storedHash;
                if (storedHash != null && storedHash.startsWith("$2y$")) {
                    compatibleHash = "$2a$" + storedHash.substring(4);
                }

                // ✅ Verify plain password against compatible hash
                if (!BCrypt.checkpw(plainPassword, compatibleHash)) {
                    return null; // wrong password
                }

                if ("blocked".equals(status)) {
                    // Return user object so the controller can show the "blocked" message
                    return mapRow(rs);
                }

                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ===============================
    // GET ALL USERS
    // ===============================
    public List<user> getAllUsers() throws SQLException {
        List<user> list = new ArrayList<>();
        String sql = "SELECT * FROM user";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    // ===============================
    // GET ONLY OUVRIERS
    // ===============================
    public List<user> getAllOuvriers() throws SQLException {
        List<user> list = new ArrayList<>();
        String sql = "SELECT * FROM user WHERE roles = 'ROLE_OUVRIER'";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    // ===============================
    // UPDATE USER
    // ===============================
    public void updateUser(user user) throws SQLException {
        String sql = "UPDATE user SET name = ?, email = ?, phone = ?, roles = ?, status = ?, updated_at = ?";

        boolean hasNewPassword = user.getPassword() != null && !user.getPassword().isEmpty()
                && !user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2y$");

        if (hasNewPassword) {
            sql += ", password = ?";
        }
        sql += " WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, user.getName());
        ps.setString(2, user.getEmail());
        ps.setString(3, user.getPhone());
        
        // Store role as JSON array for Symfony
        ps.setString(4, "[\"" + user.getRole().name() + "\"]");
        
        ps.setString(5, user.getStatus());
        ps.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));

        if (hasNewPassword) {
            String newHash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));
            ps.setString(7, newHash);
            ps.setInt(8, user.getId());
        } else {
            ps.setInt(7, user.getId());
        }

        ps.executeUpdate();
        System.out.println("✅ User updated with JSON roles");
    }

    // ===============================
    // DELETE USER
    // ===============================
    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM user WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    // ===============================
    // TOGGLE USER STATUS
    // ===============================
    public void toggleUserStatus(int userId, String newStatus) throws SQLException {
        String sql = "UPDATE user SET status = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, newStatus);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    // ===============================
    // UPDATE USER ROLE (used by owner when accepting applications)
    // ===============================
    public void updateUserRole(int userId, user.Role newRole) throws SQLException {
        String sql = "UPDATE user SET roles = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, newRole.name());
        ps.setInt(2, userId);
        ps.executeUpdate();
        System.out.println("✅ User role updated to: " + newRole.name());
    }

    // ===============================
    // FIND BY EMAIL
    // ===============================
    public user findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ===============================
    // FIND BY ID
    // ===============================
    public user findById(int id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ===============================
    // GOOGLE LOGIN - Find or Create
    // ===============================
    public user findOrCreateGoogleUser(String email, String name) throws SQLException {
        user existingUser = findByEmail(email);
        if (existingUser != null) {
            if ("BLOCKED".equals(existingUser.getStatus())) return null;
            return existingUser;
        }

        user newUser = new user();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword(BCrypt.hashpw("GOOGLE_AUTH_" + System.currentTimeMillis(), BCrypt.gensalt(12)));
        newUser.setPhone("00000000");
        newUser.setRole(user.Role.ROLE_PENDING);
        newUser.setStatus("ACTIVE");

        // ✅ Updated for Symfony compatibility
        String sql = "INSERT INTO user (name, email, password, phone, roles, status, auth_provider, first_login, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, newUser.getName());
        ps.setString(2, newUser.getEmail());
        ps.setString(3, newUser.getPassword());
        ps.setString(4, newUser.getPhone());
        ps.setString(5, "[\"ROLE_PENDING\"]");
        ps.setString(6, "ACTIVE");
        ps.setString(7, "google");
        ps.setBoolean(8, false);
        ps.setTimestamp(9, new java.sql.Timestamp(System.currentTimeMillis()));
        ps.executeUpdate();

        return findByEmail(email);
    }

    // ===============================
    // PRIVATE HELPER
    // ===============================
    private user mapRow(ResultSet rs) throws SQLException {
        String rolesStr = rs.getString("roles");
        String roleName = "ROLE_PENDING";

        if (rolesStr != null) {
            if (rolesStr.startsWith("[")) {
                // Parse JSON array
                org.json.JSONArray rolesArray = new org.json.JSONArray(rolesStr);
                if (rolesArray.length() > 0) {
                    roleName = rolesArray.getString(0);
                }
            } else {
                // Fallback for legacy plain string roles
                roleName = rolesStr;
            }
        }

        Role role;
        try {
            role = Role.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ Unknown role found in DB: " + roleName + ". Defaulting to ROLE_PENDING.");
            role = Role.ROLE_PENDING;
        }

        Integer farmId = rs.getObject("farm_id") != null ? rs.getInt("farm_id") : null;
        String cvFile = rs.getString("cv_file");
        String desiredRole = rs.getString("ai_suggested_role");

        return new user(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("phone"),
                role,
                rs.getString("status"),
                rs.getString("profile_picture"),
                farmId,
                cvFile,
                desiredRole
        );
    }
    // ===============================
    // GET USERS BY LIST OF IDs  ← ADD THIS METHOD to userservice.java
    // ===============================
    public List<user> getUsersByIds(List<Integer> ids) throws SQLException {
        List<user> list = new ArrayList<>();
        if (ids == null || ids.isEmpty()) return list;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            placeholders.append(i == 0 ? "?" : ",?");
        }
        String sql = "SELECT * FROM user WHERE id IN (" + placeholders + ")";
        PreparedStatement ps = cnx.prepareStatement(sql);
        for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i));
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }
}