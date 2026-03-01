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
    private static final String SESSION_FILE  = "agrisense_session.dat";
    private static final String CLOSE_TIME_FILE = "agrisense_close_time.dat"; // ← NEW

    // Full session duration in DB: 7 days
    private static final long SESSION_DURATION = 7L * 24 * 60 * 60 * 1000;

    // ✅ If app was closed MORE than 10 min ago → force re-login
    private static final long MAX_IDLE_MS = 10L * 60 * 1000; // 10 minutes

    private UserSession currentSession;
    private user currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ============================================================
    // CRÉER UNE SESSION après login réussi
    // ============================================================
    public UserSession createSession(user user) throws SQLException {
        String sessionToken = generateSessionToken();
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + SESSION_DURATION);

        UserSession session = new UserSession(user.getId(), sessionToken, expiresAt);
        session.setDeviceInfo(getDeviceInfo());

        saveSessionToDatabase(session);
        saveSessionToFile(session);

        this.currentSession = session;
        this.currentUser = user;

        System.out.println("✅ Session créée pour: " + user.getName() +
                " | Expire: " + expiresAt);
        return session;
    }

    // ============================================================
    // CHARGER LA SESSION AU DÉMARRAGE
    // Extra check: if app was closed > 10 min ago → force login
    // ============================================================
    public boolean loadSavedSession() {
        try {
            // ── 1. Check if app was closed more than 10 min ago ──
            // ✅ FIX: Read value fully FIRST, close the reader, THEN delete
            //    (Windows locks files that are still open — must close before delete)
            File closeTimeFile = new File(CLOSE_TIME_FILE);
            if (closeTimeFile.exists()) {
                long closeTime = 0;
                try (BufferedReader reader = new BufferedReader(new FileReader(closeTimeFile))) {
                    String line = reader.readLine();
                    if (line != null) {
                        closeTime = Long.parseLong(line.trim());
                    }
                } // ← reader fully closed here before any file operation

                long idleMs = System.currentTimeMillis() - closeTime;
                if (idleMs > MAX_IDLE_MS) {
                    System.out.println("⏱ App fermée il y a "
                            + (idleMs / 1000 / 60) + " min → session expirée. Re-login requis.");
                    clearAllSessionFiles(); // ← safe: reader already closed
                    return false;
                }
                System.out.println("✅ Idle time: " + (idleMs / 1000) + "s — OK");
            }

            // ── 2. Check if session file exists ──────────────────
            File sessionFile = new File(SESSION_FILE);
            if (!sessionFile.exists()) {
                System.out.println("No active session. Loading landing page...");
                return false;
            }

            // ── 3. Read token FIRST, close reader, THEN validate/delete
            String sessionToken = null;
            try (BufferedReader reader = new BufferedReader(new FileReader(sessionFile))) {
                sessionToken = reader.readLine();
            } // ← reader fully closed here

            if (sessionToken != null && !sessionToken.isEmpty()) {
                boolean valid = validateAndLoadSession(sessionToken);
                if (!valid) {
                    clearAllSessionFiles(); // ← safe: reader already closed
                    System.out.println("Session invalide ou expirée. Redirection vers login.");
                }
                return valid;
            }

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ============================================================
    // VALIDER LE TOKEN EN DB ET CHARGER L'USER
    // ============================================================
    private boolean validateAndLoadSession(String sessionToken) {
        String query = "SELECT s.*, u.* FROM user_sessions s " +
                "JOIN user u ON s.user_id = u.id " +
                "WHERE s.session_token = ? AND s.is_active = TRUE AND s.expires_at > NOW()";

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Connection conn = MyDataBase.getInstance().getCnx();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, sessionToken);
            rs = stmt.executeQuery();

            if (rs.next()) {
                currentSession = new UserSession();
                currentSession.setSessionId(rs.getInt("session_id"));
                currentSession.setUserId(rs.getInt("user_id"));
                currentSession.setSessionToken(rs.getString("session_token"));
                currentSession.setCreatedAt(rs.getTimestamp("created_at"));
                currentSession.setLastActivity(rs.getTimestamp("last_activity"));
                currentSession.setExpiresAt(rs.getTimestamp("expires_at"));
                currentSession.setActive(rs.getBoolean("is_active"));

                currentUser = new user();
                currentUser.setId(rs.getInt("id"));
                currentUser.setName(rs.getString("name"));
                currentUser.setEmail(rs.getString("email"));
                currentUser.setPhone(rs.getString("phone"));
                currentUser.setStatus(rs.getString("status"));
                currentUser.setRoleFromString(rs.getString("roles"));

                // ✅ Load profile picture too
                String profilePic = rs.getString("profile_picture");
                if (profilePic != null) currentUser.setProfilePicture(profilePic);

                updateLastActivity(sessionToken);

                System.out.println("✅ Session restaurée pour: " + currentUser.getName()
                        + " (" + currentUser.getRole() + ")");
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // ============================================================
    // SAUVEGARDER EN DB
    // ============================================================
    private void saveSessionToDatabase(UserSession session) throws SQLException {
        String query = "INSERT INTO user_sessions (user_id, session_token, expires_at, device_info, ip_address, is_active) " +
                "VALUES (?, ?, ?, ?, ?, TRUE)";

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Connection conn = MyDataBase.getInstance().getCnx();
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
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // ============================================================
    // SAUVEGARDER LE TOKEN DANS UN FICHIER LOCAL
    // ============================================================
    private void saveSessionToFile(UserSession session) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SESSION_FILE))) {
            writer.write(session.getSessionToken());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    // ✅ SAVE CLOSE TIMESTAMP — called from HelloApplication.stop()
    // ============================================================
    public void saveCloseTimestamp() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CLOSE_TIME_FILE))) {
            writer.write(String.valueOf(System.currentTimeMillis()));
            System.out.println("⏱ Close timestamp saved.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    // METTRE À JOUR last_activity
    // ============================================================
    private void updateLastActivity(String sessionToken) {
        String query = "UPDATE user_sessions SET last_activity = NOW() WHERE session_token = ?";
        PreparedStatement stmt = null;
        try {
            Connection conn = MyDataBase.getInstance().getCnx();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, sessionToken);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // ============================================================
    // LOGOUT — invalide la session et supprime les fichiers locaux
    // ============================================================
    public void logout() {
        if (currentSession != null) {
            String query = "UPDATE user_sessions SET is_active = FALSE WHERE session_token = ?";
            PreparedStatement stmt = null;
            try {
                Connection conn = MyDataBase.getInstance().getCnx();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, currentSession.getSessionToken());
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }

        clearAllSessionFiles();
        currentSession = null;
        currentUser    = null;
        System.out.println("✅ Déconnecté. Session supprimée.");
    }

    // ============================================================
    // DELETE both session files
    // ============================================================
    private void clearAllSessionFiles() {
        try {
            Files.deleteIfExists(Paths.get(SESSION_FILE));
            Files.deleteIfExists(Paths.get(CLOSE_TIME_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    // GETTERS
    // ============================================================
    public user getCurrentUser()           { return currentUser; }
    public UserSession getCurrentSession() { return currentSession; }
    public boolean isLoggedIn()            { return currentSession != null && currentUser != null; }

    // ============================================================
    // HELPERS
    // ============================================================
    private String generateSessionToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    private String getDeviceInfo() {
        return System.getProperty("os.name") + " " + System.getProperty("os.version");
    }

    private String getLocalIPAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    // ============================================================
    // NETTOYER LES SESSIONS EXPIRÉES
    // ============================================================
    public void cleanupExpiredSessions() {
        String query = "UPDATE user_sessions SET is_active = FALSE WHERE expires_at < NOW()";
        PreparedStatement stmt = null;
        try {
            Connection conn = MyDataBase.getInstance().getCnx();
            if (conn != null && !conn.isClosed()) {
                stmt = conn.prepareStatement(query);
                stmt.executeUpdate();
                System.out.println("Sessions expirées nettoyées.");
            }
        } catch (SQLException e) {
            System.err.println("Cleanup sessions: " + e.getMessage());
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignore */ }
        }
    }
}