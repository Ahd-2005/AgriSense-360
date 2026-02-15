package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static MyConnection instance;
    private Connection cnx;

    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/smartfarm?useSSL=false&serverTimezone=UTC",
                    "root",
                    ""
            );
            System.out.println("✅ Connexion réussie à la base de données");
        } catch (SQLException e) {
            System.out.println("❌ Erreur connexion DB : " + e.getMessage());
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}
