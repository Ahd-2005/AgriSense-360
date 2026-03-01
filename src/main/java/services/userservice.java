package services;

import entity.user;
import entity.user.Role;
import utils.MyDataBase;

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
    // SIGN UP
    // ===============================
    public void ajouter(user user) throws SQLException {
        String sql = "INSERT INTO user (name, email, password, phone, roles, status,profile_picture) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, user.getName());
        ps.setString(2, user.getEmail());
        ps.setString(3, user.getPassword());
        ps.setString(4, user.getPhone());
        ps.setString(5, user.getRole().name());
        ps.setString(6, "ACTIVE"); // default status
        ps.setString(7, user.getProfilePicture());
        ps.executeUpdate();
        System.out.println("✅ User inserted into database");
    }

    public void updateProfilePicture(int userId, String pictureUrl) throws SQLException {
        String sql = "UPDATE user SET profile_picture = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, pictureUrl);
        ps.setInt(2, userId);
        ps.executeUpdate();
        System.out.println("✅ Profile picture updated");
    }
    // ===============================
    // LOGIN (Check status)
    // ===============================
    public user login(String email, String pass) {
        String sql = "SELECT * FROM user WHERE email = ? AND password = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, pass);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String status = rs.getString("status");

                // Check if account is blocked
                if ("BLOCKED".equals(status)) {
                    return null; // Account blocked
                }

                return new user(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("phone"),
                        Role.valueOf(rs.getString("roles")),
                        rs.getString("status"),
                        rs.getString("profile_picture")
                );
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
            user u = new user(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("phone"),
                    user.Role.valueOf(rs.getString("roles")),
                    rs.getString("status"),
                    rs.getString("profile_picture")
            );
            list.add(u);
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
            user u = new user(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("phone"),
                    user.Role.valueOf(rs.getString("roles")),
                    rs.getString("status"),
                    rs.getString("profile_picture")
            );
            list.add(u);
        }

        return list;
    }

    // ===============================
    // UPDATE USER
    // ===============================
    public void updateUser(user user) throws SQLException {
        String sql = "UPDATE user SET name = ?, email = ?, phone = ?, roles = ?, status = ?";

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            sql += ", password = ?";
        }

        sql += " WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, user.getName());
        ps.setString(2, user.getEmail());
        ps.setString(3, user.getPhone());
        ps.setString(4, user.getRole().name());
        ps.setString(5, user.getStatus());

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            ps.setString(6, user.getPassword());
            ps.setInt(7, user.getId());
        } else {
            ps.setInt(6, user.getId());
        }

        ps.executeUpdate();
        System.out.println("✅ User updated in database");
    }

    // ===============================
    // DELETE USER
    // ===============================
    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM user WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);

        ps.executeUpdate();
        System.out.println("✅ User deleted from database");
    }

    // ===============================
    // TOGGLE USER STATUS (Block/Activate)
    // ===============================
    public void toggleUserStatus(int userId, String newStatus) throws SQLException {
        String sql = "UPDATE user SET status = ? WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, newStatus);
        ps.setInt(2, userId);

        ps.executeUpdate();
        System.out.println("✅ User status updated to: " + newStatus);
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

            if (rs.next()) {
                return new user(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("phone"),
                        Role.valueOf(rs.getString("roles")),
                        rs.getString("status"),
                        rs.getString("profile_picture")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }



    // ===============================
// GOOGLE LOGIN - Find or Create
// ===============================
    public user findOrCreateGoogleUser(String email, String name) throws SQLException {
        // 1. Chercher si l'email existe déjà
        user existingUser = findByEmail(email);

        if (existingUser != null) {
            // User existe → vérifier s'il est bloqué
            if ("BLOCKED".equals(existingUser.getStatus())) {
                return null; // bloqué
            }
            return existingUser; // connexion normale
        }

        // 2. User n'existe pas → créer un nouveau compte automatiquement
        user newUser = new user();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword("GOOGLE_AUTH_" + System.currentTimeMillis()); // password fictif
        newUser.setPhone("00000000"); // valeur par défaut
        newUser.setRole(user.Role.ROLE_OUVRIER); // rôle par défaut
        newUser.setStatus("ACTIVE");

        ajouter(newUser);

        // Récupérer l'user avec son ID
        return findByEmail(email);
    }
}