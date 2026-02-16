package com.example.agrisens360.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private final String URL ="jdbc:mysql://127.0.0.1:3306/agrisense-360";
    private final String USER= "root";
    private final String PSW ="";

    private Connection myConnection;

    private static MyDataBase instance;

    private MyDataBase(){
        try {
            myConnection = DriverManager.getConnection(URL,USER,PSW);
            System.out.println("Connection établie!");
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
