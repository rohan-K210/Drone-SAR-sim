package com.dronesar.model;

import java.util.Objects;

public class Position {

    private double x;
    private double y;

    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    /**
     * Calculates the distance between this position
     * and another position.
     */
    public double distanceTo(Position other) {

        if (other == null) {
            throw new IllegalArgumentException(
                    "Position cannot be null"
            );
        }

        double dx = this.x - other.x;
        double dy = this.y - other.y;

        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculates heading in degrees to another position (0 = East, 90 = South).
     */
    public double angleTo(Position other) {
        if (other == null) return 0.0;
        return Math.toDegrees(Math.atan2(other.y - this.y, other.x - this.x));
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Position)) {
            return false;
        }

        Position other = (Position) obj;

        return Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}