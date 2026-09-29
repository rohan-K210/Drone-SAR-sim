package com.dronesar.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Event generated when a drone locates a missing person and alerts the central base tower.
 */
public class DetectionEvent {
    private final String eventId;
    private final String personId;
    private final String personName;
    private final Position coordinates;
    private final String originDroneId;
    private final String towerId;
    private final LocalDateTime timestamp;
    private boolean successfullyDelivered;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public DetectionEvent(String eventId, String personId, String personName,
                          Position coordinates, String originDroneId, String towerId) {
        this.eventId = eventId;
        this.personId = personId;
        this.personName = personName;
        this.coordinates = coordinates;
        this.originDroneId = originDroneId;
        this.towerId = towerId;
        this.timestamp = LocalDateTime.now();
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

    public boolean isSuccessfullyDelivered() {
        return successfullyDelivered;
    }

    public void setSuccessfullyDelivered(boolean successfullyDelivered) {
        this.successfullyDelivered = successfullyDelivered;
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
        return String.format("[%s] Target: %s (%s) @ %s | Drone: %s -> Tower: %s",
                getFormattedTime(), personId, personName, coordinates, originDroneId, towerId);
    }
}
