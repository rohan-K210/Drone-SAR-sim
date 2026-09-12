package com.dronesar.detection;

import com.dronesar.event.SimulationEventBus;
import com.dronesar.model.DetectionEvent;
import com.dronesar.model.Drone;
import com.dronesar.model.PersonNode;
import com.dronesar.model.Position;
import com.dronesar.model.Tower;
import com.dronesar.model.enums.DroneState;
import com.dronesar.network.DijkstraRouter;
import com.dronesar.network.MeshNetworkGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central detection pipeline of the SAR simulation.
 *
 * <p>Each scan combines two complementary detection methods:</p>
 * <ol>
 *   <li><b>Proximity sweep</b> - a drone physically passing within its
 *       sensor radius of a missing person spots them directly.</li>
 *   <li><b>RF beacon localization</b> - the person's emergency beacon is
 *       heard by several mesh nodes ({@link SignalsSimulator}); the raw
 *       signal reports are turned into a position estimate by the
 *       {@link LocalizationEngine} (trilateration). When the estimate
 *       falls close enough to a drone, that drone is credited with the
 *       detection.</li>
 * </ol>
 *
 * <p>A confirmed detection raises a {@link DetectionEvent}, computes the
 * mesh route to the tower with Dijkstra, marks the person as detected
 * and publishes everything on the {@link SimulationEventBus}.</p>
 */
public class DetectionEngine {

    /** Minimum beacon reports needed to attempt trilateration. */
    public static final int MIN_REPORTS_FOR_LOCALIZATION = 3;

    /** A localization counts as confirmed within this multiple of the drone's sensor radius. */
    public static final double LOCALIZATION_ALERT_RADIUS_FACTOR = 1.8;

    /**
     * Fallback: a single report at least this strong (dBm) triggers a
     * detection on its own. -58 dBm corresponds to roughly 68 map units
     * (default sensor radius 38 x LOCALIZATION_ALERT_RADIUS_FACTOR).
     */
    public static final double STRONG_SIGNAL_ALERT_THRESHOLD_DBM = -58.0;

    private static final String EVENT_ID_PREFIX = "EVT-";

    private final List<PersonNode> targets;
    private final List<Drone> drones;
    private final Tower tower;
    private final MeshNetworkGraph meshGraph;

    private SignalsSimulator signalsSimulator;

    private final List<DetectionEvent> detectionEvents = new CopyOnWriteArrayList<>();
    private final Map<String, Position> localizationEstimates = new ConcurrentHashMap<>();

    private long totalProximityDetections = 0;
    private long totalRfDetections = 0;
    private long totalLocalizationFailures = 0;
    private long eventIdCounter = 0;

    public DetectionEngine(List<PersonNode> targets, List<Drone> drones,
                           Tower tower, MeshNetworkGraph meshGraph) {
        this.targets = (targets != null) ? targets : new CopyOnWriteArrayList<>();
        this.drones = (drones != null) ? drones : new CopyOnWriteArrayList<>();
        this.tower = tower;
        this.meshGraph = meshGraph;
        this.signalsSimulator = new SignalsSimulator();
    }

    /**
     * Runs one detection pass over all undetected persons.
     *
     * @return the list of detection events raised during this scan
     *         (possibly empty, never {@code null})
     */
    public List<DetectionEvent> scan() {
        List<DetectionEvent> newEvents = new ArrayList<>();

        for (PersonNode target : targets) {
            if (target.isDetected()) continue;

            DetectionEvent event = detectByProximity(target);
            if (event == null) {
                event = detectByBeaconSignals(target);
            }
            if (event != null) {
                newEvents.add(event);
            }
        }
        return newEvents;
    }

    /** Direct visual detection: any operational drone within sensor radius. */
    private DetectionEvent detectByProximity(PersonNode target) {
        for (Drone drone : drones) {
            if (!drone.isOperational()) continue;

            double distance = drone.getPosition().distanceTo(target.getLocation());
            if (distance <= drone.getSensorRadius()) {
                return registerDetection(target, drone.getId(), null);
            }
        }
        return null;
    }

    /** RF detection: localize the beacon from mesh signal reports. */
    private DetectionEvent detectByBeaconSignals(PersonNode target) {
        List<SignalsSimulator.SignalReport> reports =
                signalsSimulator.collectBeaconReports(target, drones, tower);
        if (reports.isEmpty()) return null;

        Position estimate = null;
        if (reports.size() >= MIN_REPORTS_FOR_LOCALIZATION) {
            List<Position> anchors = new ArrayList<>();
            List<Double> distances = new ArrayList<>();
            for (SignalsSimulator.SignalReport report : reports) {
                anchors.add(report.getNodePosition());
                distances.add(report.getEstimatedDistance());
            }
            estimate = LocalizationEngine.trilaterate(anchors, distances);
            if (estimate == null) {
                totalLocalizationFailures++;
            }
        }

        // The credited drone is the reporting drone closest to the estimate
        // (or to the strongest report when no estimate is available).
        Position reference = (estimate != null)
                ? estimate
                : reports.get(0).getNodePosition();

        Drone nearestDrone = null;
        double nearestDistance = Double.MAX_VALUE;
        for (SignalsSimulator.SignalReport report : reports) {
            if (report.isFromTower()) continue;
            Drone drone = getDroneById(report.getNodeId());
            if (drone == null) continue;

            double distance = drone.getPosition().distanceTo(reference);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestDrone = drone;
            }
        }

        boolean confirmed;
        if (estimate != null && nearestDrone != null) {
            confirmed = nearestDistance
                    <= nearestDrone.getSensorRadius() * LOCALIZATION_ALERT_RADIUS_FACTOR;
        } else {
            // No usable geometry: fall back to a strong-signal alert.
            confirmed = reports.get(0).getRssiDbm() >= STRONG_SIGNAL_ALERT_THRESHOLD_DBM;
        }
        if (!confirmed) return null;

        String originId = (nearestDrone != null)
                ? nearestDrone.getId()
                : reports.get(0).getNodeId(); // tower-only case

        return registerDetection(target, originId, estimate);
    }

    /**
     * Marks the person detected, builds the {@link DetectionEvent} with its
     * mesh route, and publishes all related simulation events.
     */
    private synchronized DetectionEvent registerDetection(PersonNode target,
                                                          String originNodeId,
                                                          Position estimate) {
        if (target.isDetected()) return null;

        target.markDetected(originNodeId);

        Drone originDrone = getDroneById(originNodeId);
        if (originDrone != null) {
            originDrone.setState(DroneState.PERSON_FOUND);
            SimulationEventBus.getInstance()
                    .publishDroneStatusChanged(originDrone.getId(), DroneState.PERSON_FOUND);
        }

        String eventId = EVENT_ID_PREFIX + String.format("%05d", ++eventIdCounter);
        List<String> route = (tower != null)
                ? DijkstraRouter.findShortestPath(meshGraph, originNodeId, tower.getId())
                : Collections.emptyList();

        DetectionEvent event = new DetectionEvent(
                eventId,
                target.getId(),
                target.getName(),
                target.getLocation(),
                originNodeId,
                route
        );
        detectionEvents.add(event);

        if (estimate != null) {
            localizationEstimates.put(target.getId(), estimate);
            totalRfDetections++;
        } else {
            totalProximityDetections++;
        }

        SimulationEventBus.getInstance().publishPersonDetected(event);

        if (!route.isEmpty() && route.get(route.size() - 1).equals(getTowerId())) {
            tower.recordDetection(event);
            event.setSuccessfullyDelivered(true);
            SimulationEventBus.getInstance().publishPacketDelivered(event);
        }
        if (route.size() > 1) {
            SimulationEventBus.getInstance().publishMultiHopRoute(eventId, route);
        }

        return event;
    }

    private Drone getDroneById(String droneId) {
        if (droneId == null) return null;
        for (Drone drone : drones) {
            if (drone.getId().equals(droneId)) return drone;
        }
        return null;
    }

    private String getTowerId() {
        return (tower != null) ? tower.getId() : null;
    }

    // --- Accessors / configuration ---

    public SignalsSimulator getSignalsSimulator() { return signalsSimulator; }

    public void setSignalsSimulator(SignalsSimulator signalsSimulator) {
        if (signalsSimulator == null) {
            throw new IllegalArgumentException("SignalsSimulator cannot be null");
        }
        this.signalsSimulator = signalsSimulator;
    }

    public List<DetectionEvent> getDetectionEvents() {
        return Collections.unmodifiableList(new ArrayList<>(detectionEvents));
    }

    /** Trilaterated position estimate for a detected person, or {@code null}. */
    public Position getLocalizationEstimate(String personId) {
        return localizationEstimates.get(personId);
    }

    public long getTotalProximityDetections() { return totalProximityDetections; }
    public long getTotalRfDetections() { return totalRfDetections; }
    public long getTotalLocalizationFailures() { return totalLocalizationFailures; }

    /** Clears accumulated results; person/drone state is not modified. */
    public synchronized void reset() {
        detectionEvents.clear();
        localizationEstimates.clear();
        totalProximityDetections = 0;
        totalRfDetections = 0;
        totalLocalizationFailures = 0;
        eventIdCounter = 0;
    }
}
