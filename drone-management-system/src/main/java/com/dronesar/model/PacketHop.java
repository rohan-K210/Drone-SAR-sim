package com.dronesar.model;

/**
 * Represents an in-flight packet hop segment between two mesh nodes.
 */
public class PacketHop {
    private final String eventId;
    private final String fromId;
    private final String toId;
    private final Position fromPos;
    private final Position toPos;
    private double progress;
    private final double speed;

    public PacketHop(String eventId, String fromId, String toId, Position fromPos, Position toPos) {
        this.eventId = eventId;
        this.fromId = fromId;
        this.toId = toId;
        this.fromPos = fromPos;
        this.toPos = toPos;
        this.progress = 0.0;
        this.speed = 0.035;
    }

    public String getEventId() {
        return eventId;
    }

    public String getFromId() {
        return fromId;
    }

    public String getToId() {
        return toId;
    }

    public Position getFromPos() {
        return fromPos;
    }

    public Position getToPos() {
        return toPos;
    }

    public double getProgress() {
        return progress;
    }

    public boolean isComplete() {
        return progress >= 1.0;
    }

    public void step() {
        progress = Math.min(1.0, progress + speed);
    }

    public Position getCurrentPosition() {
        double currentX = fromPos.getX() + (toPos.getX() - fromPos.getX()) * progress;
        double currentY = fromPos.getY() + (toPos.getY() - fromPos.getY()) * progress;
        return new Position(currentX, currentY);
    }
}
