package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Correcting the JDBC URL format
    private static final String URL = "jdbc:postgresql://pg-3303d83d-avtomobili-pb.j.aivencloud.com:17124/defaultdb?sslmode=require";
    private static final String USER = "avnadmin";  // Your username
    private static final String PASSWORD = "AVNS_oKFnv-upkNX_68X6RhE";  // Your password

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver"); // Load PostgreSQL JDBC driver
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
