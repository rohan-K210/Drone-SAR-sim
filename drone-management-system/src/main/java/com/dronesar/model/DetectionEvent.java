package com.dronesar.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Event generated when a drone locates a missing person and relays telemetry back to base.
 * Supports both tower-addressed detections (GUI simulation) and mesh-route detections
 * (multi-hop communication pipeline).
 */
public class DetectionEvent {
    private final String eventId;
    private final String personId;
    private final String personName;
    private final Position coordinates;
    private final String originDroneId;
    private final String towerId;
    private final LocalDateTime timestamp;
    private final List<String> route;
    private boolean successfullyDelivered;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Tower-addressed detection event. */
    public DetectionEvent(String eventId, String personId, String personName,
                          Position coordinates, String originDroneId, String towerId) {
        this(eventId, personId, personName, coordinates, originDroneId, towerId, Collections.emptyList());
    }

    /** Mesh-route detection event. */
    public DetectionEvent(String eventId, String personId, String personName,
                          Position coordinates, String originDroneId, List<String> route) {
        this(eventId, personId, personName, coordinates, originDroneId, null, route);
    }

    private DetectionEvent(String eventId, String personId, String personName,
                           Position coordinates, String originDroneId, String towerId, List<String> route) {
        this.eventId = eventId;
        this.personId = personId;
        this.personName = personName;
        this.coordinates = coordinates;
        this.originDroneId = originDroneId;
        this.towerId = towerId;
        this.timestamp = LocalDateTime.now();
        this.route = new ArrayList<>(route);
        this.successfullyDelivered = false;
    }

    public String getEventId() {
        return eventId;
    }

    public String getPersonId() {
        return personId;
    }

    public String getPersonName() {
        return personName;
    }

    public Position getCoordinates() {
        return coordinates;
    }

    public String getOriginDroneId() {
        return originDroneId;
    }

    public String getTowerId() {
        return towerId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        return timestamp.format(TIME_FMT);
    }

    public List<String> getRoute() {
        return Collections.unmodifiableList(route);
    }

    public int getHopCount() {
        return Math.max(0, route.size() - 1);
    }

    public boolean isSuccessfullyDelivered() {
        return successfullyDelivered;
    }

    public void setSuccessfullyDelivered(boolean successfullyDelivered) {
        this.successfullyDelivered = successfullyDelivered;
    }

    public String getRouteString() {
        return String.join(" \u2794 ", route);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DetectionEvent that)) return false;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return String.format("[%s] Target: %s (%s) @ %s | Drone: %s -> Tower: %s | Route: %s (Hops: %d)",
                getFormattedTime(), personId, personName, coordinates, originDroneId,
                towerId, getRouteString(), getHopCount());
    }
}
