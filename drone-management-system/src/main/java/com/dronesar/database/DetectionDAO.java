package com.dronesar.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DetectionDAO {

    // Save a DetectionEvent directly, extracting its fields for storage
    public boolean saveDetection(com.dronesar.model.DetectionEvent event) {
        return saveDetection(
                event.getEventId(),
                event.getPersonId(),
                event.getPersonName(),
                event.getCoordinates().getX(),
                event.getCoordinates().getY(),
                event.getOriginDroneId(),
                event.getTimestamp().toString(), // full ISO timestamp, e.g. 2026-01-01T10:00:00
                event.getRouteString(),
                event.getHopCount(),
                event.isSuccessfullyDelivered()
        );
    }

    // Save a new detection event into the database
    public boolean saveDetection(String eventId, String personId, String personName,
                                 double x, double y, String droneId, String timestamp,
                                 String route, int hopCount, boolean delivered) {

        String sql = """
            INSERT INTO detections
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
            System.out.println("Failed to save detection: " + e.getMessage());
            return false;
        }
    }

    // Retrieve all detection events (detection history)
    public List<DetectionRecord> getAllDetections() {
        List<DetectionRecord> detections = new ArrayList<>();
        String sql = "SELECT * FROM detections;";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                detections.add(mapRow(resultSet));
            }

        } catch (SQLException e) {
            System.out.println("Failed to retrieve detections: " + e.getMessage());
        }

        return detections;
    }

    // Find a single detection event by its event ID
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
            System.out.println("Failed to find detection: " + e.getMessage());
        }

        return null; // not found
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
    }
}