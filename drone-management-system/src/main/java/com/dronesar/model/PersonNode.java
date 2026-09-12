package com.dronesar.model;

import java.time.LocalDateTime;

/**
 * Represents a missing person/target scattered in the forest area.
 */
public class PersonNode {
    private final String id;
    private final String name;
    private final Position location;
    private boolean detected;
    private LocalDateTime detectionTime;
    private String detectingDroneId;
    private double pulseRadius;

    public PersonNode(String id, String name, Position location) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.detected = false;
        this.pulseRadius = 0.0;
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

    public synchronized boolean isDetected() {
        return detected;
    }

    public synchronized void markDetected(String droneId) {
        if (!this.detected) {
            this.detected = true;
            this.detectionTime = LocalDateTime.now();
            this.detectingDroneId = droneId;
            this.pulseRadius = 4.0;
        }
    }

    public synchronized LocalDateTime getDetectionTime() {
        return detectionTime;
    }

    public synchronized String getDetectingDroneId() {
        return detectingDroneId;
    }

    public synchronized double getPulseRadius() {
        return pulseRadius;
    }

    public synchronized void updatePulseAnimation() {
        if (detected) {
            pulseRadius += 0.8;
            if (pulseRadius > 38.0) {
                pulseRadius = 4.0;
            }
        }
    }

    public synchronized void reset() {
        this.detected = false;
        this.detectionTime = null;
        this.detectingDroneId = null;
        this.pulseRadius = 0.0;
    }
}
