package com.auraprive.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages database connectivity using JDBC.
 * Implements Singleton connection pattern for thread safety and efficiency.
 */
public class DatabaseConnection {

    // Standard local MariaDB/MySQL port is 3306
    private static final String URL = "jdbc:mysql://localhost:3306/auraprive_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    
    // Read credentials securely from environment variables (defaults to root with empty password for offline fallback)
    private static final String USERNAME = System.getenv("DB_USERNAME") != null ? System.getenv("DB_USERNAME") : "root";
    private static final String PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "";

    private static Connection connection = null;

    private DatabaseConnection() {
        // Private constructor to prevent direct instantiation
    }

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // If no password environment variable is set, gracefully return null to trigger offline mode
                if (PASSWORD.isEmpty()) {
                    return null;
                }
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("[DatabaseConnection] Connection established successfully.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection Error] JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection Error] Failed to connect to database: " + e.getMessage());
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DatabaseConnection] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection Error] Error closing connection: " + e.getMessage());
        }
    }

    // Quick test runner to verify connection without running a server
    public static void main(String[] args) {
        System.out.println("Testing database connection to MariaDB...");
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            System.out.println(" SUCCESS: Connected to MariaDB 'auraprive_db'!");
            DatabaseConnection.closeConnection();
        } else {
            System.err.println(" FAILED: Connection could not be established (Running in Offline Mode).");
        }
    }
}