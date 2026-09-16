package com.dronesar.model;

import com.dronesar.model.enums.DroneState;

import java.util.List;

/**
 * Represents a single drone's physical/behavioral state: position, battery,
 * assigned zone, and search path.
 *
 * OWNERSHIP NOTE: This file also needs networking-related fields
 * (id used for mesh routing, neighbor table, comms module) owned by the
 * Architecture/Networking teammate. Recommended approach: keep those
 * concerns in a separate class (e.g. CommsModule) and hold a reference to
 * it here via composition, rather than both people editing this file
 * directly. Example:
 *
 *   private CommsModule comms;
 *   public CommsModule getComms() { return comms; }
 *
 * Coordinate with that teammate before merging both halves.
 */
public class Drone {

    private static final double LOW_BATTERY_THRESHOLD = 20.0;
    private static final double BATTERY_DEPLETED = 0.0;
    private static final double ARRIVAL_TOLERANCE = 0.5;

    private final int id;
    private Position position;
    private DroneState state;
    private double battery; // percentage, 0-100

    private SearchZone assignedZone;
    private List<Position> path;
    private int waypointIndex;

    private final double speed;            // units traveled per tick
    private final double batteryDrainRate; // % battery consumed per tick while moving

    public Drone(int id, Position startPosition, double speed, double batteryDrainRate) {
        this.id = id;
        this.position = startPosition;
        this.speed = speed;
        this.batteryDrainRate = batteryDrainRate;
        this.state = DroneState.IDLE;
        this.battery = 100.0;
        this.waypointIndex = 0;
    }

    // ---- Getters (Person 4 / UI should only ever call these, never mutate directly) ----

    public int getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public DroneState getState() {
        return state;
    }

    public double getBattery() {
        return battery;
    }

    public SearchZone getAssignedZone() {
        return assignedZone;
    }

    public List<Position> getPath() {
        return path;
    }

    public int getWaypointIndex() {
        return waypointIndex;
    }

    // ---- Mutators (should only be called by SearchManager / ZoneAllocator) ----

    public void setAssignedZone(SearchZone zone) {
        this.assignedZone = zone;
        this.state = DroneState.EN_ROUTE_TO_ZONE;
    }

    public void setPath(List<Position> path) {
        this.path = path;
        this.waypointIndex = 0;
    }

    public void recallToBase() {
        this.state = DroneState.RETURNING_TO_BASE;
    }

    /**
     * Advances the drone by one simulation tick: moves toward the next
     * waypoint, drains battery, and updates state as needed.
     */
    public void tick() {
        if (state == DroneState.OFFLINE) {
            return;
        }

        if (battery <= BATTERY_DEPLETED) {
            state = DroneState.OFFLINE;
            return;
        }

        if (path == null || waypointIndex >= path.size()) {
            if (state == DroneState.SEARCHING && assignedZone != null) {
                assignedZone.markCovered();
            }
            if (state != DroneState.RETURNING_TO_BASE) {
                state = DroneState.IDLE;
            }
            return;
        }

        Position target = path.get(waypointIndex);
        moveToward(target);
        drainBattery();
        updateBatteryState();

        if (position.distanceTo(target) < ARRIVAL_TOLERANCE) {
            waypointIndex++;
            if (state == DroneState.EN_ROUTE_TO_ZONE) {
                state = DroneState.SEARCHING;
            }
        }
    }

    private void moveToward(Position target) {
        double dx = target.getX() - position.getX();
        double dy = target.getY() - position.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance <= speed) {
            position = target;
        } else {
            double ratio = speed / distance;
            position = new Position(
                    position.getX() + dx * ratio,
                    position.getY() + dy * ratio
            );
        }
    }

    private void drainBattery() {
        battery = Math.max(BATTERY_DEPLETED, battery - batteryDrainRate);
    }

    private void updateBatteryState() {
        if (battery <= BATTERY_DEPLETED) {
            state = DroneState.OFFLINE;
        } else if (battery <= LOW_BATTERY_THRESHOLD && state != DroneState.RETURNING_TO_BASE) {
            state = DroneState.LOW_BATTERY;
        }
    }

    @Override
    public String toString() {
        return String.format(
                "Drone[%d] pos=%s state=%s battery=%.1f%%",
                id, position, state, battery
        );
    }
}