package com.dronesar.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base Station / Central Tower communicating with drones and dispatching rescue teams.
 */
public class Tower {
    private final String id;
    private final String name;
    private final Position location;
    private final double receptionRadius;
    private final List<DetectionEvent> receivedDetections;
    private double beaconPulseRadius;

    public Tower(String id, String name, Position location, double receptionRadius) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.receptionRadius = receptionRadius;
        this.receivedDetections = new ArrayList<>();
        this.beaconPulseRadius = 10.0;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Position getLocation() {
        return location;
    }

    public double getReceptionRadius() {
        return receptionRadius;
    }

    public synchronized void recordDetection(DetectionEvent event) {
        if (event != null && !receivedDetections.contains(event)) {
            receivedDetections.add(event);
        }
    }

    public synchronized List<DetectionEvent> getReceivedDetections() {
        return Collections.unmodifiableList(new ArrayList<>(receivedDetections));
    }

    public synchronized double getBeaconPulseRadius() {
        return beaconPulseRadius;
    }

    public synchronized void updateBeacon() {
        beaconPulseRadius += 0.5;
        if (beaconPulseRadius > receptionRadius) {
            beaconPulseRadius = 10.0;
        }
    }

    public synchronized void reset() {
        receivedDetections.clear();
        beaconPulseRadius = 10.0;
    }
}
