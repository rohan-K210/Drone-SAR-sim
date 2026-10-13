package com.dronesar.database;

import com.dronesar.model.DetectionEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO Pattern) for SAR detection events.
 * Persists and queries real-time detection telemetry in the SQLite database (drone_sar.db).
 */
public class DetectionDAO {

    // Save a DetectionEvent directly, extracting its fields for storage
    public boolean saveDetection(DetectionEvent event) {
        if (event == null) return false;
        return saveDetection(
                event.getEventId(),
                event.getPersonId(),
                event.getPersonName(),
                event.getCoordinates() != null ? event.getCoordinates().getX() : 0.0,
                event.getCoordinates() != null ? event.getCoordinates().getY() : 0.0,
                event.getOriginDroneId(),
                event.getTimestamp() != null ? event.getTimestamp().toString() : "",
                event.getRouteString(),
                event.getHopCount(),
                event.isSuccessfullyDelivered()
        );
    }

    // Compatible alias for insert
    public void insert(DetectionEvent event) {
        saveDetection(event);
    }

    // Save a detection using plain parameters.
    // INSERT OR REPLACE: a repeated event_id overwrites the old row instead of failing.
    public boolean saveDetection(String eventId, String personId, String personName,
                                 double x, double y, String droneId, String timestamp,
                                 String route, int hopCount, boolean delivered) {

        String sql = """
            INSERT OR REPLACE INTO detections
            (event_id, person_id, person_name, x, y, drone_id, timestamp, route, hop_count, delivered)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, eventId);
            statement.setString(2, personId);
            statement.setString(3, personName);
            statement.setDouble(4, x);
            statement.setDouble(5, y);
            statement.setString(6, droneId);
            statement.setString(7, timestamp);
            statement.setString(8, route);
            statement.setInt(9, hopCount);
            statement.setInt(10, delivered ? 1 : 0);

            statement.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Failed to save detection to SQLite: " + e.getMessage());
            return false;
        }
    }

    // Update an existing detection once its packet reaches the tower
    public boolean updateDeliveryStatus(DetectionEvent event) {
        if (event == null) return false;
        String sql = """
            UPDATE detections
            SET delivered = ?, route = ?, hop_count = ?
            WHERE event_id = ?;
        """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, event.isSuccessfullyDelivered() ? 1 : 0);
            statement.setString(2, event.getRouteString());
            statement.setInt(3, event.getHopCount());
            statement.setString(4, event.getEventId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Failed to update delivery status in SQLite: " + e.getMessage());
            return false;
        }
    }

    // Retrieve all detection events (detection history) from SQLite
    public List<DetectionRecord> getAllDetections() {
        List<DetectionRecord> detections = new ArrayList<>();
        String sql = "SELECT * FROM detections ORDER BY timestamp ASC;";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                detections.add(mapRow(resultSet));
            }

        } catch (SQLException e) {
            System.err.println("Failed to retrieve detections from SQLite: " + e.getMessage());
        }

        return detections;
    }

    // Find a single detection by its event ID
    public DetectionRecord findById(String eventId) {
        String sql = "SELECT * FROM detections WHERE event_id = ?;";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, eventId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }

        } catch (SQLException e) {
            System.err.println("Failed to find detection in SQLite: " + e.getMessage());
        }

        return null;
    }

    public int getCount() {
        String sql = "SELECT COUNT(*) FROM detections;";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get detection count: " + e.getMessage());
        }
        return 0;
    }

    public void clear() {
        String sql = "DELETE FROM detections;";
        try (Connection connection = DatabaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Failed to clear detections table: " + e.getMessage());
        }
    }

    // Helper: maps a ResultSet row into a DetectionRecord object
    private DetectionRecord mapRow(ResultSet resultSet) throws SQLException {
        return new DetectionRecord(
                resultSet.getString("event_id"),
                resultSet.getString("person_id"),
                resultSet.getString("person_name"),
                resultSet.getDouble("x"),
                resultSet.getDouble("y"),
                resultSet.getString("drone_id"),
                resultSet.getString("timestamp"),
                resultSet.getString("route"),
                resultSet.getInt("hop_count"),
                resultSet.getInt("delivered") == 1
        );
    }

    // Simple data holder for a detection row read from the database
    public static class DetectionRecord {
        public final String eventId;
        public final String personId;
        public final String personName;
        public final double x;
        public final double y;
        public final String droneId;
        public final String timestamp;
        public final String route;
        public final int hopCount;
        public final boolean delivered;

        public DetectionRecord(String eventId, String personId, String personName,
                               double x, double y, String droneId, String timestamp,
                               String route, int hopCount, boolean delivered) {
            this.eventId = eventId;
            this.personId = personId;
            this.personName = personName;
            this.x = x;
            this.y = y;
            this.droneId = droneId;
            this.timestamp = timestamp;
            this.route = route;
            this.hopCount = hopCount;
            this.delivered = delivered;
        }

        public String getEventId() { return eventId; }
        public String getPersonId() { return personId; }
        public String getPersonName() { return personName; }
        public double getX() { return x; }
        public double getY() { return y; }
        public String getDroneId() { return droneId; }
        public String getTimestamp() { return timestamp; }
        public String getRoute() { return route; }
        public int getHopCount() { return hopCount; }
        public boolean isDelivered() { return delivered; }
    }
}
