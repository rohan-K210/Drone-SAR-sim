package com.dronesar.network;

import com.dronesar.model.Position;

public class NetworkNode {
    private final String id;
    private final Position position;
    private final double transmissionRange;
    private final boolean isTower;
    private final boolean isOnline;

    public NetworkNode(String id, Position position, double transmissionRange, boolean isTower, boolean isOnline) {
        this.id = id;
        this.position = position;
        this.transmissionRange = transmissionRange;
        this.isTower = isTower;
        this.isOnline = isOnline;
    }

    public String getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public double getTransmissionRange() {
        return transmissionRange;
    }

    public boolean isTower() {
        return isTower;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public boolean canReach(NetworkNode other) {
        if (!isOnline || !other.isOnline) return false;
        return position.distanceTo(other.position) <= transmissionRange;
    }
}
