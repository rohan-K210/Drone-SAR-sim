package com.dronesar.model;

import com.dronesar.model.enums.DroneState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Drone {

    private final String id;

    private String callsign;

    private Position position;

    private String assignedZoneId;

    private double batteryLevel;

    private double searchProgress;

    private DroneState state;

    private double heading; // degrees

    private double speed; // units per update

    private double sensorRadius;

    private double communicationRadius;

    // Trail history for GUI rendering
    private static final int MAX_TRAIL_POINTS = 45;
    private final Deque<Position> trailHistory = new ArrayDeque<>();

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
        this.callsign = id;
        this.position = position;

        // Every newly created drone starts with full battery.
        this.batteryLevel = 100.0;

        // Search has not started yet.
        this.searchProgress = 0.0;

        // Initial state.
        this.state = DroneState.IDLE;

        this.heading = 0.0;
        this.speed = 2.4;
        this.sensorRadius = 38.0;
        this.communicationRadius = 230.0;
        recordPosition(position);
    }

    public Drone(String id, String callsign, String assignedZoneId, Position position,
                 double sensorRadius, double communicationRadius) {
        this(id, position);
        this.callsign = callsign;
        this.assignedZoneId = assignedZoneId;
        this.sensorRadius = sensorRadius;
        this.communicationRadius = communicationRadius;
        this.state = DroneState.SEARCHING;
    }

    public String getId() {
        return id;
    }

    public String getCallsign() {
        return callsign != null ? callsign : id;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }

    public Position getPosition() {
        return position;
    }

    public synchronized void setPosition(Position position) {

        if (position == null) {
            throw new IllegalArgumentException(
                    "Position cannot be null"
            );
        }

        if (!position.equals(this.position)) {
            this.heading = this.position.angleTo(position);
            this.position = position;
            recordPosition(position);
        }
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

    public synchronized void setBatteryLevel(double batteryLevel) {

        if (batteryLevel < 0 || batteryLevel > 100) {
            throw new IllegalArgumentException(
                    "Battery level must be between 0 and 100"
            );
        }

        this.batteryLevel = batteryLevel;
        if (batteryLevel <= 0) {
            this.state = DroneState.OFFLINE;
        }
    }

    public synchronized void consumeBattery(double amount) {
        setBatteryLevel(Math.max(0.0, this.batteryLevel - amount));
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

    public synchronized void setState(DroneState state) {

        if (state == null) {
            throw new IllegalArgumentException(
                    "Drone state cannot be null"
            );
        }

        this.state = state;
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getSensorRadius() {
        return sensorRadius;
    }

    public void setSensorRadius(double sensorRadius) {
        this.sensorRadius = sensorRadius;
    }

    public double getCommunicationRadius() {
        return communicationRadius;
    }

    public void setCommunicationRadius(double communicationRadius) {
        this.communicationRadius = communicationRadius;
    }

    /**
     * Returns true when the drone is available
     * for normal simulation operations.
     */
    public boolean isOperational() {
        return state != DroneState.OFFLINE;
    }

    public synchronized void toggleOffline() {
        if (state == DroneState.OFFLINE) {
            state = DroneState.SEARCHING;
        } else {
            state = DroneState.OFFLINE;
        }
    }

    private synchronized void recordPosition(Position pos) {
        if (trailHistory.size() >= MAX_TRAIL_POINTS) {
            trailHistory.pollFirst();
        }
        trailHistory.offerLast(pos);
    }

    public synchronized List<Position> getTrailSnapshot() {
        return new ArrayList<>(trailHistory);
    }

    public boolean canReach(Position other) {
        if (!isOperational() || other == null) return false;
        return position.distanceTo(other) <= communicationRadius;
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