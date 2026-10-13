package com.dronesar.detection;

import com.dronesar.model.DetectionEvent;
import com.dronesar.model.Drone;
import com.dronesar.model.PersonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Optical and Thermal detection sensor processor.
 * Scans sensor fields of view (FOV) to identify lost targets within drone camera/radar sweeps.
 */
public class DetectionEngine {

    private final SignalsSimulator signalsSimulator;

    public DetectionEngine() {
        this.signalsSimulator = new SignalsSimulator();
    }

    /**
     * Scans for undetected survivors in range of a patrolling drone.
     */
    public List<DetectionEvent> scanTargets(Drone drone, List<PersonNode> targets, String assignedTowerId) {
        List<DetectionEvent> events = new ArrayList<>();
        if (!drone.isOperational()) return events;

        for (PersonNode target : targets) {
            if (target.isDetected()) continue;

            double dist = drone.getPosition().distanceTo(target.getLocation());
            if (dist <= drone.getSensorRadius()) {
                target.markDetected(drone.getId());
                String eventId = "EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                DetectionEvent event = new DetectionEvent(
                        eventId,
                        target.getId(),
                        target.getName(),
                        target.getLocation(),
                        drone.getId(),
                        assignedTowerId
                );
                events.add(event);
            }
        }
        return events;
    }

    public SignalsSimulator getSignalsSimulator() {
        return signalsSimulator;
    }
}
