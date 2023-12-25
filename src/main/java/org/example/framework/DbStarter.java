package org.example.framework;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbStarter {
    private static Connection connection = null;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Connection getConnection() {
        if (connection == null) {
            try {
                String url = "jdbc:sqlite:your_database_name.db";
                connection = DriverManager.getConnection(url);
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }
        return connection;
    }

    public static ObjectMapper getObjectMapper(){
        return objectMapper;
    }
}