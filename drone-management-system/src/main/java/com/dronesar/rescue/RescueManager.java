package com.dronesar.rescue;

import com.dronesar.database.MissionDAO;

import java.util.ArrayList;
import java.util.List;

public class RescueManager {

    private final List<RescueTeam> teams = new ArrayList<>();
    private final MissionDAO missionDAO = new MissionDAO();

    public void addTeam(RescueTeam team) {
        teams.add(team);
    }

    public RescueTeam findAvailableTeam() {
        for (RescueTeam team : teams) {
            if (team.getStatus().equals("AVAILABLE")) {
                return team;
            }
        }
        return null; // no available team
    }

    public boolean dispatchTeam(String personId) {
        RescueTeam team = findAvailableTeam();
        if (team == null) {
            System.out.println("No available rescue teams.");
            return false;
        }

        team.setPersonId(personId);
        team.setStatus("DISPATCHED");

        missionDAO.updateStatus(team.getTeamId(), "DISPATCHED", personId);
        return true;
    }

    public boolean updateTeamStatus(String teamId, String newStatus) {
        for (RescueTeam team : teams) {
            if (team.getTeamId().equals(teamId)) {
                team.setStatus(newStatus);
                missionDAO.updateStatus(teamId, newStatus, team.getPersonId());
                return true;
            }
        }
        return false; // team not found
    }

    public List<RescueTeam> getActiveMissions() {
        List<RescueTeam> active = new ArrayList<>();
        for (RescueTeam team : teams) {
            if (!team.getStatus().equals("AVAILABLE")) {
                active.add(team);
            }
        }
        return active;
    }

    // --- Nested class: represents a single rescue team ---
    public static class RescueTeam {
        private final String teamId;
        private String personId;
        private double x;
        private double y;
        private String status;

        public RescueTeam(String teamId, double x, double y) {
            this.teamId = teamId;
            this.x = x;
            this.y = y;
            this.status = "AVAILABLE";
        }

        public String getTeamId() { return teamId; }
        public String getPersonId() { return personId; }
        public void setPersonId(String personId) { this.personId = personId; }
        public double getX() { return x; }
        public double getY() { return y; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}