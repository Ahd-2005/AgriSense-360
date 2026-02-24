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

    // ✅ 7 jours — session persiste après fermeture de l'app
    private static final long SESSION_DURATION = 7L * 24 * 60 * 60 * 1000;

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
    // ✅ CHARGER LA SESSION AU DÉMARRAGE
    // Si le token est valide → restaure l'user → retourne true
    // Si expiré ou inexistant → retourne false → landing page
    // ============================================================
    public boolean loadSavedSession() {
        try {
            File sessionFile = new File(SESSION_FILE);
            if (!sessionFile.exists()) {
                System.out.println("No active session. Loading landing page...");
                return false;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(sessionFile))) {
                String sessionToken = reader.readLine();
                if (sessionToken != null && !sessionToken.isEmpty()) {
                    boolean valid = validateAndLoadSession(sessionToken);
                    if (!valid) {
                        // Token expiré → nettoyer le fichier
                        Files.deleteIfExists(Paths.get(SESSION_FILE));
                        System.out.println("Session expirée. Redirection vers login.");
                    }
                    return valid;
                }
            }
        } catch (IOException e) {
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
                // ✅ Charger la session
                currentSession = new UserSession();
                currentSession.setSessionId(rs.getInt("session_id"));
                currentSession.setUserId(rs.getInt("user_id"));
                currentSession.setSessionToken(rs.getString("session_token"));
                currentSession.setCreatedAt(rs.getTimestamp("created_at"));
                currentSession.setLastActivity(rs.getTimestamp("last_activity"));
                currentSession.setExpiresAt(rs.getTimestamp("expires_at"));
                currentSession.setActive(rs.getBoolean("is_active"));

                // ✅ Charger l'user
                currentUser = new user();
                currentUser.setId(rs.getInt("id"));
                currentUser.setName(rs.getString("name"));
                currentUser.setEmail(rs.getString("email"));
                currentUser.setPhone(rs.getString("phone"));
                currentUser.setStatus(rs.getString("status"));

                String roleString = rs.getString("roles");
                currentUser.setRoleFromString(roleString);

                // ✅ Mettre à jour last_activity
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
    // LOGOUT — invalide la session et supprime le fichier local
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

        // ✅ Supprimer le fichier → prochain démarrage → landing page
        try {
            Files.deleteIfExists(Paths.get(SESSION_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }

        currentSession = null;
        currentUser = null;
        System.out.println("✅ Déconnecté. Session supprimée.");
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
    // NETTOYER LES SESSIONS EXPIRÉES (appelé à la fermeture)
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