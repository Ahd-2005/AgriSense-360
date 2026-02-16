package services;

import entity.UserSession;
import entity.user;
import entity.user.Role;
import utils.MyDataBase;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.UUID;

public class SessionManager {
    private static SessionManager instance;
    private static final String SESSION_FILE = "agrisense_session.dat";
    private static final long SESSION_DURATION = 30L * 24 * 60 * 60 * 1000; // 30 days in milliseconds

    private UserSession currentSession;
    private user currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Create a new session for a user after successful login
     */
    public UserSession createSession(user user) throws SQLException {
        String sessionToken = generateSessionToken();
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + SESSION_DURATION);

        UserSession session = new UserSession(user.getId(), sessionToken, expiresAt);
        session.setDeviceInfo(getDeviceInfo());

        // Save to database
        saveSessionToDatabase(session);

        // Save to local file
        saveSessionToFile(session);

        // Set current session
        this.currentSession = session;
        this.currentUser = user;

        return session;
    }

    /**
     * Load session from local file on app startup
     */
    public boolean loadSavedSession() {
        try {
            File sessionFile = new File(SESSION_FILE);
            if (!sessionFile.exists()) {
                return false;
            }

            // Read session token from file
            try (BufferedReader reader = new BufferedReader(new FileReader(sessionFile))) {
                String sessionToken = reader.readLine();
                if (sessionToken != null && !sessionToken.isEmpty()) {
                    return validateAndLoadSession(sessionToken);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Validate session token and load user data
     */
    private boolean validateAndLoadSession(String sessionToken) {
        String query = "SELECT s.*, u.* FROM user_sessions s " +
                "JOIN user u ON s.user_id = u.id " +
                "WHERE s.session_token = ? AND s.is_active = TRUE AND s.expires_at > NOW()";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = MyDataBase.getInstance().getCnx();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, sessionToken);
            rs = stmt.executeQuery();

            if (rs.next()) {
                // Load session
                currentSession = new UserSession();
                currentSession.setSessionId(rs.getInt("session_id"));
                currentSession.setUserId(rs.getInt("user_id"));
                currentSession.setSessionToken(rs.getString("session_token"));
                currentSession.setCreatedAt(rs.getTimestamp("created_at"));
                currentSession.setLastActivity(rs.getTimestamp("last_activity"));
                currentSession.setExpiresAt(rs.getTimestamp("expires_at"));
                currentSession.setActive(rs.getBoolean("is_active"));

                // Load user with Role enum
                currentUser = new user();
                currentUser.setId(rs.getInt("id"));
                currentUser.setName(rs.getString("name"));
                currentUser.setEmail(rs.getString("email"));
                currentUser.setPhone(rs.getString("phone"));
                currentUser.setStatus(rs.getString("status"));

                // Convert String to Role enum
                String roleString = rs.getString("role");
                currentUser.setRoleFromString(roleString);

                // Update last activity
                updateLastActivity(sessionToken);

                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Don't close connection - it's managed by MyDataBase singleton
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Save session to database
     */
    private void saveSessionToDatabase(UserSession session) throws SQLException {
        String query = "INSERT INTO user_sessions (user_id, session_token, expires_at, device_info, ip_address, is_active) " +
                "VALUES (?, ?, ?, ?, ?, TRUE)";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = MyDataBase.getInstance().getCnx();
            stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            stmt.setInt(1, session.getUserId());
            stmt.setString(2, session.getSessionToken());
            stmt.setTimestamp(3, session.getExpiresAt());
            stmt.setString(4, session.getDeviceInfo());
            stmt.setString(5, getLocalIPAddress());

            stmt.executeUpdate();

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                session.setSessionId(rs.getInt(1));
            }
        } finally {
            // Don't close connection - it's managed by MyDataBase singleton
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Save session token to local file
     */
    private void saveSessionToFile(UserSession session) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SESSION_FILE))) {
            writer.write(session.getSessionToken());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update last activity timestamp
     */
    private void updateLastActivity(String sessionToken) {
        String query = "UPDATE user_sessions SET last_activity = NOW() WHERE session_token = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = MyDataBase.getInstance().getCnx();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, sessionToken);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Don't close connection - it's managed by MyDataBase singleton
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logout - Invalidate session
     */
    public void logout() {
        if (currentSession != null) {
            // Invalidate in database
            String query = "UPDATE user_sessions SET is_active = FALSE WHERE session_token = ?";

            Connection conn = null;
            PreparedStatement stmt = null;

            try {
                conn = MyDataBase.getInstance().getCnx();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, currentSession.getSessionToken());
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                // Don't close connection - it's managed by MyDataBase singleton
                try {
                    if (stmt != null) stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        // Delete local session file
        try {
            Files.deleteIfExists(Paths.get(SESSION_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Clear current session
        currentSession = null;
        currentUser = null;
    }

    /**
     * Get current logged-in user
     */
    public user getCurrentUser() {
        return currentUser;
    }

    /**
     * Get current session
     */
    public UserSession getCurrentSession() {
        return currentSession;
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return currentSession != null && currentUser != null;
    }

    /**
     * Generate unique session token
     */
    private String generateSessionToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    /**
     * Get device info (OS name and version)
     */
    private String getDeviceInfo() {
        return System.getProperty("os.name") + " " + System.getProperty("os.version");
    }

    /**
     * Get local IP address
     */
    private String getLocalIPAddress() {
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1"; // Default fallback
        }
    }

    /**
     * Clean up expired sessions (run periodically)
     */
    public void cleanupExpiredSessions() {
        String query = "UPDATE user_sessions SET is_active = FALSE WHERE expires_at < NOW()";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = MyDataBase.getInstance().getCnx();
            // Check if connection is still valid
            if (conn != null && !conn.isClosed()) {
                stmt = conn.prepareStatement(query);
                stmt.executeUpdate();
                System.out.println("Expired sessions cleaned up.");
            }
        } catch (SQLException e) {
            // Silently fail if database is already closed on app shutdown
            System.err.println("Could not cleanup sessions (connection may be closed): " + e.getMessage());
        } finally {
            // Don't close connection - it's managed by MyDataBase singleton
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                // Ignore errors on shutdown
            }
        }
    }
}