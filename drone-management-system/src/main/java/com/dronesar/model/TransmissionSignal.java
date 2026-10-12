package com.dronesar.model;

/**
 * Represents a direct wireless transmission signal traveling between a drone and a tower.
 */
public class TransmissionSignal {
    private final String droneId;
    private final String towerId;
    private final Position startPos;
    private final Position endPos;
    private double progress; // 0.0 to 1.0
    private final DetectionEvent event;

    public TransmissionSignal(String droneId, String towerId, Position startPos, Position endPos, DetectionEvent event) {
        this.droneId = droneId;
        this.towerId = towerId;
        this.startPos = startPos;
        this.endPos = endPos;
        this.progress = 0.0;
        this.event = event;
    }

    public boolean step(double delta) {
        progress += delta * 1.8;
        return progress >= 1.0;
    }

    public Position getCurrentPosition() {
        double clampedProgress = Math.min(1.0, Math.max(0.0, progress));
        double x = startPos.getX() + (endPos.getX() - startPos.getX()) * clampedProgress;
        double y = startPos.getY() + (endPos.getY() - startPos.getY()) * clampedProgress;
        return new Position(x, y);
    }

    public String getDroneId() {
        return droneId;
    }

    public String getTowerId() {
        return towerId;
    }

    public Position getStartPos() {
        return startPos;
    }

    public Position getEndPos() {
        return endPos;
    }

    public double getProgress() {
        return progress;
    }

    public DetectionEvent getEvent() {
        return event;
    }
}
