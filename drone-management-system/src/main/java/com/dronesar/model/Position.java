package com.dronesar.model;

import java.util.Objects;

public class Position {

    private double x;
    private double y;

    // Reference base coordinate (Blackwood Forest Reserve GPS origin)
    private static final double BASE_LAT = 45.3812;
    private static final double BASE_LON = 121.8425;

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
     * Calculates the distance between this position and another position.
     */
    public double distanceTo(Position other) {
        if (other == null) {
            throw new IllegalArgumentException("Position cannot be null");
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

    /**
     * Converts spatial coordinate to real-world GPS Latitude/Longitude format.
     * Example: "45.3910° N, 121.8103° W"
     */
    public String toGpsString() {
        double lat = BASE_LAT + (this.y * 0.000089);
        double lon = BASE_LON - (this.x * 0.000115);
        return String.format("%.4f° N, %.4f° W", lat, Math.abs(lon));
    }

    /**
     * Converts to high-precision DMS (Degrees, Minutes, Seconds) GPS format.
     * Example: "45°23'27.6\"N, 121°48'37.1\"W"
     */
    public String toDmsGpsString() {
        double lat = BASE_LAT + (this.y * 0.000089);
        double lon = BASE_LON - (this.x * 0.000115);

        int latDeg = (int) lat;
        int latMin = (int) ((lat - latDeg) * 60);
        double latSec = ((lat - latDeg) * 60 - latMin) * 60;

        int lonDeg = (int) lon;
        int lonMin = (int) ((lon - lonDeg) * 60);
        double lonSec = ((lon - lonDeg) * 60 - lonMin) * 60;

        return String.format("%d°%02d'%04.1f\"N, %d°%02d'%04.1f\"W",
                latDeg, Math.abs(latMin), Math.abs(latSec),
                lonDeg, Math.abs(lonMin), Math.abs(lonSec));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Position other)) return false;
        return Double.compare(x, other.x) == 0 && Double.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return toGpsString();
    }
}