package utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates database tables if they do not exist.
 */
public class DbInit {

    public static void initTables() {
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) {
            System.err.println("Cannot init tables: no connection.");
            return;
        }

        String createAffectation = """
            CREATE TABLE IF NOT EXISTS affectation_travail (
                id_affectation INT AUTO_INCREMENT PRIMARY KEY,
                type_travail VARCHAR(255),
                date_debut DATE,
                date_fin DATE,
                zone_travail VARCHAR(255),
                statut VARCHAR(100)
            )
            """;

        String createEvaluation = """
            CREATE TABLE IF NOT EXISTS evaluation_performance (
                id_evaluation INT AUTO_INCREMENT PRIMARY KEY,
                id_affectation INT NOT NULL,
                note INT,
                qualite VARCHAR(100),
                commentaire TEXT,
                date_evaluation DATE,
                FOREIGN KEY (id_affectation) REFERENCES affectation_travail(id_affectation) ON DELETE CASCADE
            )
            """;

        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(createAffectation);
            st.executeUpdate(createEvaluation);
            System.out.println("Tables initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error initializing tables: " + e.getMessage());
        }
    }
}
