package com.dronesar.model;

import java.util.Objects;

/**
 * Represents a rapid-response Ground Rescue Unit dispatched to recover located survivors.
 * Implements OOP encapsulation and spatial trajectory tracking.
 */
public class RescueTeam {

    public enum Status {
        STANDBY,
        DISPATCHED,
        EN_ROUTE,
        ON_SITE,
        MISSION_COMPLETE
    }

    private final String teamId;
    private final String teamName;
    private Position currentPosition;
    private Position targetPosition;
    private Status status;
    private double speedKmh;
    private String assignedSurvivorId;

    public RescueTeam(String teamId, String teamName, Position basePosition) {
        this.teamId = Objects.requireNonNull(teamId, "teamId cannot be null");
        this.teamName = Objects.requireNonNull(teamName, "teamName cannot be null");
        this.currentPosition = Objects.requireNonNull(basePosition, "basePosition cannot be null");
        this.status = Status.STANDBY;
        this.speedKmh = 45.0; // Typical emergency vehicle speed in rugged terrain
    }

    public synchronized void dispatchTo(Position target, String survivorId) {
        this.targetPosition = target;
        this.assignedSurvivorId = survivorId;
        this.status = Status.DISPATCHED;
    }

    public synchronized void updatePosition(double deltaSeconds) {
        if (status != Status.DISPATCHED && status != Status.EN_ROUTE) {
            return;
        }
        if (targetPosition == null) {
            return;
        }

        double distance = currentPosition.distanceTo(targetPosition);
        double stepDistance = (speedKmh * 1000.0 / 3600.0) * deltaSeconds;

        if (distance <= stepDistance || distance < 5.0) {
            this.currentPosition = targetPosition;
            this.status = Status.ON_SITE;
        } else {
            this.status = Status.EN_ROUTE;
            double dx = targetPosition.getX() - currentPosition.getX();
            double dy = targetPosition.getY() - currentPosition.getY();
            double ratio = stepDistance / distance;
            this.currentPosition = new Position(
                    currentPosition.getX() + dx * ratio,
                    currentPosition.getY() + dy * ratio
            );
        }
    }

    public String getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public synchronized Position getCurrentPosition() {
        return currentPosition;
    }

    public synchronized Position getTargetPosition() {
        return targetPosition;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }

    public String getAssignedSurvivorId() {
        return assignedSurvivorId;
    }

    @Override
    public String toString() {
        return String.format("RescueTeam[%s (%s), Status=%s, Pos=%s]", teamId, teamName, status, currentPosition);
    }
}
