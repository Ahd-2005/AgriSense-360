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
        String sql = "INSERT INTO user (name, email, password, phone, roles, status) VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, user.getName());
        ps.setString(2, user.getEmail());
        ps.setString(3, user.getPassword());
        ps.setString(4, user.getPhone());
        ps.setString(5, user.getRole().name());
        ps.setString(6, "ACTIVE"); // default status

        ps.executeUpdate();
        System.out.println("✅ User inserted into database");
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
                        rs.getString("status")
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
                    rs.getString("status")
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
                    rs.getString("status")
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
                        rs.getString("status")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}