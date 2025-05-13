package org.example;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

public class TestDatabaseConnection {
    public static void main(String[] args) {
        try {
            // Establish the connection
            Connection connection = DatabaseConnection.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM avtomobili"); // Replace with your actual table name

            // Output the results
            while (resultSet.next()) {
                System.out.println("Row: " + resultSet.getString("your_column_name")); // Replace with your column name
            }

            connection.close(); // Always close the connection after use
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
