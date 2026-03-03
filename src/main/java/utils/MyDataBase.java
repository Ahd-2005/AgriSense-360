package utils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

public class MyDataBase {

    private Connection myConnection;
    private final String url;
    private final String user;
    private final String password;
    private String lastConnectionError;

    private static MyDataBase instance;

    private MyDataBase(){
        Properties props = loadProperties();
        url = props.getProperty("db.url", "jdbc:mysql://127.0.0.1:3306/agrisense?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&connectTimeout=5000&socketTimeout=10000").trim();
        user = props.getProperty("db.user", "rayenadmin").trim();
        password = props.getProperty("db.password", "rayenadmin").trim();
        initializeConnection();
    }

    public synchronized Connection getMyConnection() {
        try {
            if (myConnection == null || myConnection.isClosed()) {
                initializeConnection();
            }
        } catch (SQLException e) {
            initializeConnection();
        }
        return myConnection;
    }

    public Connection getCnx() {
        return myConnection;
    }

    public synchronized String getLastConnectionError() {
        return lastConnectionError;
    }

    public static MyDataBase getInstance() {
        if(instance == null)
            instance = new MyDataBase();
        return instance;
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = MyDataBase.class.getResourceAsStream("/config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
        }
        return properties;
    }

    private void initializeConnection() {
        SQLException lastException = null;
        for (String candidateUrl : buildConnectionCandidates(url)) {
            try {
                myConnection = DriverManager.getConnection(candidateUrl, user, password);
                lastConnectionError = null;
                System.out.println("Connected...");
                return;
            } catch (SQLException e) {
                lastException = e;
            }
        }

        myConnection = null;
        if (lastException != null) {
            lastConnectionError = lastException.getMessage();
            System.err.println("Database connection failed: " + lastConnectionError);
            System.err.println("Connection URL: " + url);
            System.err.println("Username: " + user);
            System.err.println("Hint: ensure WAMP MySQL (wampmysqld64) is running.");
        }
    }

    private List<String> buildConnectionCandidates(String baseUrl) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(baseUrl);

        String suffix = "";
        int slashAfterHost = baseUrl.indexOf('/', "jdbc:mysql://".length());
        if (slashAfterHost > 0) {
            suffix = baseUrl.substring(slashAfterHost);
        }

        String[] hosts = {"localhost", "127.0.0.1"};
        int[] ports = {3306, 3307, 3308};
        for (String host : hosts) {
            for (int port : ports) {
                candidates.add("jdbc:mysql://" + host + ":" + port + suffix);
            }
        }

        return new ArrayList<>(candidates);
    }
}