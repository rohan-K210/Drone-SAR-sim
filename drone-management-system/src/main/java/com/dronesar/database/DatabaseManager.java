package com.dronesar.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton Database and Persistence Manager (OOP Design Pattern - Singleton / DAO).
 * Provides thread-safe SQLite connection and data access for detection blackbox logs.
 */
public class DatabaseManager {

    private static final String URL = "jdbc:sqlite:drone_sar.db";
    private static volatile DatabaseManager instance;

    private final DetectionDAO detectionDAO;

    private DatabaseManager() {
        this.detectionDAO = new DetectionDAO();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void testConnection() {
        try (Connection connection = getConnection()) {
            System.out.println("Database connected successfully! (" + URL + ")");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
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
            System.out.println("SQLite database tables created successfully in drone_sar.db.");

        } catch (SQLException e) {
            System.err.println("Table creation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public DetectionDAO getDetectionDAO() {
        return detectionDAO;
    }

    public boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void clearAll() {
        detectionDAO.clear();
    }
}
