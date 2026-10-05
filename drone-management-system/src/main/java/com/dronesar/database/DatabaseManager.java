package com.dronesar.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:drone_sar.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void testConnection() {
        try (Connection connection = getConnection()) {
            System.out.println("Database connected successfully!");
        } catch (SQLException e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
        }
    }

    public static void createTables() {
        String createDetectionsTable = """
            CREATE TABLE IF NOT EXISTS detections (
                event_id TEXT PRIMARY KEY,
                person_id TEXT,
                person_name TEXT,
                x REAL,
                y REAL,
                drone_id TEXT,
                timestamp TEXT,
                route TEXT,
                hop_count INTEGER,
                delivered INTEGER
            );
        """;

        String createRescueTeamsTable = """
            CREATE TABLE IF NOT EXISTS rescue_teams (
                team_id TEXT PRIMARY KEY,
                person_id TEXT,
                x REAL,
                y REAL,
                status TEXT CHECK(status IN ('AVAILABLE', 'DISPATCHED', 'RESCUING', 'COMPLETED'))
            );
        """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(createDetectionsTable);
            statement.execute(createRescueTeamsTable);
            System.out.println("Tables created successfully (or already existed).");

        } catch (SQLException e) {
            System.out.println("Table creation failed!");
            e.printStackTrace();
        }
    }
}