package com.dronesar.model;

import com.dronesar.model.enums.DroneState;

public class Drone {

    private final String id;

    private Position position;

    private String assignedZoneId;

    private double batteryLevel;

    private double searchProgress;

    private DroneState state;

    public Drone(String id, Position position) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Drone ID cannot be empty"
            );
        }

        if (position == null) {
            throw new IllegalArgumentException(
                    "Drone position cannot be null"
            );
        }

        this.id = id;
        this.position = position;

        // Every newly created drone starts with full battery.
        this.batteryLevel = 100.0;

        // Search has not started yet.
        this.searchProgress = 0.0;

        // Initial state.
        this.state = DroneState.IDLE;
    }

    public String getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {

        if (position == null) {
            throw new IllegalArgumentException(
                    "Position cannot be null"
            );
        }

        this.position = position;
    }

    public String getAssignedZoneId() {
        return assignedZoneId;
    }

    public void setAssignedZoneId(String assignedZoneId) {
        this.assignedZoneId = assignedZoneId;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(double batteryLevel) {

        if (batteryLevel < 0 || batteryLevel > 100) {
            throw new IllegalArgumentException(
                    "Battery level must be between 0 and 100"
            );
        }

        this.batteryLevel = batteryLevel;
    }

    public double getSearchProgress() {
        return searchProgress;
    }

    public void setSearchProgress(double searchProgress) {

        if (searchProgress < 0 || searchProgress > 100) {
            throw new IllegalArgumentException(
                    "Search progress must be between 0 and 100"
            );
        }

        this.searchProgress = searchProgress;
    }

    public DroneState getState() {
        return state;
    }

    public void setState(DroneState state) {

        if (state == null) {
            throw new IllegalArgumentException(
                    "Drone state cannot be null"
            );
        }

        this.state = state;
    }

    /**
     * Returns true when the drone is available
     * for normal simulation operations.
     */
    public boolean isOperational() {
        return state != DroneState.OFFLINE;
    }

    @Override
    public String toString() {

        return "Drone{" +
                "id='" + id + '\'' +
                ", position=" + position +
                ", assignedZoneId='" + assignedZoneId + '\'' +
                ", batteryLevel=" + batteryLevel +
                ", searchProgress=" + searchProgress +
                ", state=" + state +
                '}';
    }
}