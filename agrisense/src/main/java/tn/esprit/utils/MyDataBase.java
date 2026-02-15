package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private Connection myConnection;

    private static MyDataBase instance;

    private MyDataBase(){
        try {
            String URL = "jdbc:mysql://localhost:3306/agrisense";
            String USER = "root";
            String PW = "";
            myConnection = DriverManager.getConnection(URL,USER,PW);
            System.out.println("Connected...");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public Connection getMyConnection() {
        return myConnection;
    }

    public static MyDataBase getInstance() {
        if(instance == null)
            instance = new MyDataBase();
        return instance;
    }
}