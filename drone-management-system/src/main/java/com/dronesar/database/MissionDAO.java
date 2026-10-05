package com.dronesar.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MissionDAO {

    // Create a new rescue team row, starting as AVAILABLE
    public boolean createTeam(String teamId, double x, double y) {
        String sql = """
            INSERT INTO rescue_teams (team_id, person_id, x, y, status)
            VALUES (?, NULL, ?, ?, 'AVAILABLE');
        """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, teamId);
            statement.setDouble(2, x);
            statement.setDouble(3, y);

            statement.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Failed to create rescue team: " + e.getMessage());
            return false;
        }
    }

    // Update a team's status (and optionally which person they're assigned to)
    public boolean updateStatus(String teamId, String newStatus, String personId) {
        String sql = """
            UPDATE rescue_teams
            SET status = ?, person_id = ?
            WHERE team_id = ?;
        """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newStatus);
            statement.setString(2, personId);
            statement.setString(3, teamId);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.out.println("Failed to update rescue status: " + e.getMessage());
            return false;
        }
    }

    // Retrieve all currently active missions (any team not AVAILABLE)
    public List<MissionRecord> getActiveMissions() {
        List<MissionRecord> missions = new ArrayList<>();
        String sql = "SELECT * FROM rescue_teams WHERE status != 'AVAILABLE';";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                missions.add(mapRow(resultSet));
            }

        } catch (SQLException e) {
            System.out.println("Failed to retrieve active missions: " + e.getMessage());
        }

        return missions;
    }

    // Find a single rescue team by its team ID
    public MissionRecord findById(String teamId) {
        String sql = "SELECT * FROM rescue_teams WHERE team_id = ?;";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, teamId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }

        } catch (SQLException e) {
            System.out.println("Failed to find rescue team: " + e.getMessage());
        }

        return null; // not found
    }

    // Helper: maps a ResultSet row into a MissionRecord object
    private MissionRecord mapRow(ResultSet resultSet) throws SQLException {
        return new MissionRecord(
                resultSet.getString("team_id"),
                resultSet.getString("person_id"),
                resultSet.getDouble("x"),
                resultSet.getDouble("y"),
                resultSet.getString("status")
        );
    }

    // Simple data holder for a rescue_teams row read from the database
    public static class MissionRecord {
        public final String teamId;
        public final String personId;
        public final double x;
        public final double y;
        public final String status;

        public MissionRecord(String teamId, String personId, double x, double y, String status) {
            this.teamId = teamId;
            this.personId = personId;
            this.x = x;
            this.y = y;
            this.status = status;
        }
    }
}