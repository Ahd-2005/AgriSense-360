package services;

import utils.MyDataBase;

import java.sql.*;
import java.util.Random;

public class PasswordResetService {

    // ============================================================
    // GÉNÉRER ET SAUVEGARDER LE CODE
    // expiry calculé côté SQL pour éviter le décalage de timezone
    // ============================================================
    public static String generateAndSaveCode(int userId) throws SQLException {
        String code = String.format("%06d", new Random().nextInt(999999));

        // Supprimer les anciens codes de ce user
        deleteOldCodes(userId);

        // ✅ expires_at calculé par MySQL directement (NOW() + 15 min)
        // Évite tout décalage timezone entre Java et MySQL
        String sql = "INSERT INTO password_reset (user_id, code, expires_at, used) " +
                "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 15 MINUTE), FALSE)";

        Connection conn = MyDataBase.getInstance().getCnx();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setString(2, code);
        ps.executeUpdate();
        ps.close();

        System.out.println("✅ Code reset généré pour user_id: " + userId + " | Code: " + code);
        return code;
    }

    // ============================================================
    // VÉRIFIER LE CODE
    // ============================================================
    public static boolean verifyCode(int userId, String code) throws SQLException {
        // ✅ Comparaison entièrement côté MySQL — pas de problème timezone
        String sql = "SELECT id FROM password_reset " +
                "WHERE user_id = ? AND code = ? AND used = FALSE AND expires_at > NOW()";

        Connection conn = MyDataBase.getInstance().getCnx();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setString(2, code.trim());
        ResultSet rs = ps.executeQuery();

        boolean valid = rs.next();
        rs.close();
        ps.close();

        System.out.println("🔍 Code verification for user " + userId + ": " + (valid ? "✅ VALID" : "❌ INVALID/EXPIRED"));
        return valid;
    }

    // ============================================================
    // MARQUER LE CODE COMME UTILISÉ
    // ============================================================
    public static void markCodeAsUsed(int userId, String code) throws SQLException {
        String sql = "UPDATE password_reset SET used = TRUE WHERE user_id = ? AND code = ?";
        Connection conn = MyDataBase.getInstance().getCnx();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setString(2, code);
        ps.executeUpdate();
        ps.close();
    }

    // ============================================================
    // SUPPRIMER LES ANCIENS CODES
    // ============================================================
    private static void deleteOldCodes(int userId) throws SQLException {
        String sql = "DELETE FROM password_reset WHERE user_id = ?";
        Connection conn = MyDataBase.getInstance().getCnx();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
        ps.close();
    }
}