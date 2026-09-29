package com.dronesar.controller;

import com.dronesar.event.SimulationEventBus;
import com.dronesar.model.*;
import com.dronesar.model.enums.DroneState;
import com.dronesar.network.CommunicationManager;
import com.dronesar.search.SearchPattern;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controller class in MVC architecture (Syllabus Module 4).
 * Coordinates drones, zones, direct tower communication, and survivor detection.
 */
public class SimulationController {

    private final List<Drone> drones;
    private final CommunicationManager communicationManager;
    private boolean running;

    private final double mapWidth;
    private final double mapHeight;

    private final List<SearchZone> zones = new CopyOnWriteArrayList<>();
    private final Map<String, List<Position>> droneWaypoints = new ConcurrentHashMap<>();
    private final Map<String, Integer> droneWaypointIndex = new ConcurrentHashMap<>();
    private final List<PersonNode> targets = new CopyOnWriteArrayList<>();
    private Tower centralTower;

    private final List<DetectionEvent> allDetectionEvents = new CopyOnWriteArrayList<>();
    private final List<TransmissionSignal> activeSignals = new CopyOnWriteArrayList<>();
    private final List<DetectionEvent> pendingOfflineEvents = new CopyOnWriteArrayList<>();

    private double speedMultiplier = 1.0;
    private long elapsedSimulationMillis = 0;

    public SimulationController() {
        this(920.0, 560.0);
    }

    public SimulationController(double mapWidth, double mapHeight) {
        this.drones = new CopyOnWriteArrayList<>();
        this.communicationManager = new CommunicationManager();
        this.running = false;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        initDefaultScenario(6);
    }

    public synchronized void initDefaultScenario(int droneCount) {
        drones.clear();
        zones.clear();
        droneWaypoints.clear();
        droneWaypointIndex.clear();
        targets.clear();
        allDetectionEvents.clear();
        activeSignals.clear();
        pendingOfflineEvents.clear();
        elapsedSimulationMillis = 0;

        // Position Central Tower at Base Station
        centralTower = new Tower("BASE-TOWER-01", "Command Base Station", new Position(70, 470), 380.0);

        int cols = (droneCount >= 8) ? 4 : 3;
        int rows = (droneCount >= 8) ? 2 : 2;
        int actualCount = Math.min(droneCount, cols * rows);

        double zoneMarginLeft = 140.0;
        double usableWidth = mapWidth - zoneMarginLeft - 20.0;
        double usableHeight = mapHeight - 35.0;

        double cellWidth = usableWidth / cols;
        double cellHeight = usableHeight / rows;

        int zoneCounter = 1;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (zoneCounter > actualCount) break;

                double minX = zoneMarginLeft + c * cellWidth;
                double minY = 20.0 + r * cellHeight;
                double maxX = minX + cellWidth;
                double maxY = minY + cellHeight;

                String zoneId = "Z-" + zoneCounter;
                String zoneName = "Sector-" + (char) ('A' + (zoneCounter - 1));
                SearchZone zone = new SearchZone(zoneId, zoneName, minX, minY, maxX, maxY);
                String droneId = String.format("DRONE-%02d", zoneCounter);
                String callsign = "Eagle-" + zoneCounter;
                zone.setAssignedDroneId(droneId);
                zones.add(zone);

                double sensorRadius = 38.0;
                double laneSpacing = sensorRadius * 1.6;
                List<Position> wps = SearchPattern.generateBoustrophedonWaypoints(zone, laneSpacing, 18.0);
                droneWaypoints.put(droneId, wps);
                droneWaypointIndex.put(droneId, 0);

                Position startPos = wps.isEmpty() ? zone.getCenter() : wps.get(0);
                Drone drone = new Drone(droneId, callsign, zoneId, startPos, sensorRadius, 380.0);
                addDrone(drone);

                zoneCounter++;
            }
        }

        // Deploy lost persons across search sectors
        targets.add(new PersonNode("P-101", "Rahul K.", new Position(280, 110)));
        targets.add(new PersonNode("P-102", "Ananya M.", new Position(540, 190)));
        targets.add(new PersonNode("P-103", "Vineeth S.", new Position(790, 130)));
        targets.add(new PersonNode("P-104", "Deepa T.", new Position(410, 410)));
        targets.add(new PersonNode("P-105", "Arun J.", new Position(720, 450)));
    }

    public synchronized void tick(double deltaSeconds) {
        if (!running) return;

        double effectiveDelta = deltaSeconds * speedMultiplier;
        elapsedSimulationMillis += (long) (effectiveDelta * 1000);

        if (centralTower != null) {
            centralTower.updateBeacon();
        }

        for (Drone drone : drones) {
            if (!drone.isOperational() || drone.getState() == DroneState.SWEEP_COMPLETE) {
                continue;
            }

            List<Position> wps = droneWaypoints.get(drone.getId());
            if (wps == null || wps.isEmpty()) continue;

            int currIdx = droneWaypointIndex.getOrDefault(drone.getId(), 0);
            Position targetWp = wps.get(currIdx);
            Position currentPos = drone.getPosition();

            double stepDist = drone.getSpeed() * speedMultiplier * 1.2;
            double distToTarget = currentPos.distanceTo(targetWp);

            if (distToTarget <= stepDist) {
                drone.setPosition(targetWp);
                int nextIdx = (currIdx + 1) % wps.size();
                droneWaypointIndex.put(drone.getId(), nextIdx);

                SearchZone zone = findZoneById(drone.getAssignedZoneId());
                if (nextIdx == 0 && zone != null && zone.getCoveragePercentage() > 95.0) {
                    drone.setState(DroneState.SWEEP_COMPLETE);
                    SimulationEventBus.getInstance().publishDroneStatusChanged(drone.getId(), DroneState.SWEEP_COMPLETE);
                }
            } else {
                double angle = Math.atan2(targetWp.getY() - currentPos.getY(), targetWp.getX() - currentPos.getX());
                double newX = currentPos.getX() + Math.cos(angle) * stepDist;
                double newY = currentPos.getY() + Math.sin(angle) * stepDist;
                drone.setPosition(new Position(newX, newY));
            }

            SearchZone zone = findZoneById(drone.getAssignedZoneId());
            if (zone != null) {
                zone.recordSweep(drone.getPosition(), drone.getSensorRadius());
                drone.setSearchProgress(zone.getCoveragePercentage());
                SimulationEventBus.getInstance().publishZoneCoverageUpdated(zone.getId(), zone.getCoveragePercentage());
            }

            drone.consumeBattery(0.006 * effectiveDelta);
            SimulationEventBus.getInstance().publishDroneMoved(drone.getId(), drone.getPosition(), drone.getHeading());
        }

        checkPersonDetections();
        checkPendingTransmissions();
        updateActiveSignals(deltaSeconds);

        for (PersonNode target : targets) {
            target.updatePulseAnimation();
        }
    }

    private void checkPersonDetections() {
        for (PersonNode target : targets) {
            if (target.isDetected()) continue;

            for (Drone drone : drones) {
                if (!drone.isOperational()) continue;

                if (drone.getPosition().distanceTo(target.getLocation()) <= drone.getSensorRadius()) {
                    target.markDetected(drone.getId());
                    drone.setState(DroneState.PERSON_FOUND);
                    SimulationEventBus.getInstance().publishDroneStatusChanged(drone.getId(), DroneState.PERSON_FOUND);

                    String eventId = "EVT-" + (System.currentTimeMillis() % 10000);
                    DetectionEvent event = new DetectionEvent(
                            eventId,
                            target.getId(),
                            target.getName(),
                            target.getLocation(),
                            drone.getId(),
                            centralTower.getId()
                    );

                    allDetectionEvents.add(event);
                    SimulationEventBus.getInstance().publishPersonDetected(event);

                    transmitDetectionToTower(drone, event);
                    break;
                }
            }
        }
    }

    private void transmitDetectionToTower(Drone drone, DetectionEvent event) {
        if (isDroneInTowerRange(drone)) {
            communicationManager.sendToTower(drone, "SURVIVOR FOUND: " + event.getPersonName() + " at " + event.getCoordinates());
            activeSignals.add(new TransmissionSignal(drone.getId(), centralTower.getId(), drone.getPosition(), centralTower.getLocation(), event));
            SimulationEventBus.getInstance().publishDirectTransmission(drone.getId(), centralTower.getId(), event);
        } else {
            pendingOfflineEvents.add(event);
            System.out.println("Drone " + drone.getId() + " is outside tower range. Detection buffered.");
        }
    }

    private void checkPendingTransmissions() {
        Iterator<DetectionEvent> it = pendingOfflineEvents.iterator();
        while (it.hasNext()) {
            DetectionEvent event = it.next();
            Drone drone = getDrone(event.getOriginDroneId());
            if (drone != null && isDroneInTowerRange(drone)) {
                it.remove();
                transmitDetectionToTower(drone, event);
            }
        }
    }

    private void updateActiveSignals(double deltaSeconds) {
        Iterator<TransmissionSignal> iterator = activeSignals.iterator();
        while (iterator.hasNext()) {
            TransmissionSignal signal = iterator.next();
            boolean arrived = signal.step(deltaSeconds * speedMultiplier);
            if (arrived) {
                signal.getEvent().setSuccessfullyDelivered(true);
                centralTower.recordDetection(signal.getEvent());
                SimulationEventBus.getInstance().publishPacketDelivered(signal.getEvent());

                Drone originDrone = getDrone(signal.getDroneId());
                if (originDrone != null && originDrone.isOperational() && originDrone.getState() == DroneState.PERSON_FOUND) {
                    originDrone.setState(DroneState.SEARCHING);
                    SimulationEventBus.getInstance().publishDroneStatusChanged(originDrone.getId(), DroneState.SEARCHING);
                }
                iterator.remove();
            }
        }
    }

    public boolean isDroneInTowerRange(Drone drone) {
        if (drone == null || !drone.isOperational() || centralTower == null) return false;
        double dist = drone.getPosition().distanceTo(centralTower.getLocation());
        return dist <= centralTower.getReceptionRadius();
    }

    public void addTarget(PersonNode person) {
        if (person != null) {
            targets.add(person);
        }
    }

    public void toggleDroneFailure(String droneId) {
        Drone drone = getDrone(droneId);
        if (drone != null) {
            drone.toggleOffline();
            SimulationEventBus.getInstance().publishDroneStatusChanged(drone.getId(), drone.getState());
        }
    }

    public SearchZone findZoneById(String id) {
        for (SearchZone z : zones) {
            if (z.getId().equals(id)) return z;
        }
        return null;
    }

    public void togglePlayPause() {
        if (running) stopSimulation();
        else startSimulation();
    }

    public double getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(double mult) { this.speedMultiplier = mult; }

    public List<SearchZone> getZones() { return zones; }
    public List<PersonNode> getTargets() { return targets; }
    public Tower getCentralTower() { return centralTower; }
    public List<TransmissionSignal> getActiveSignals() { return activeSignals; }
    public List<DetectionEvent> getAllDetectionEvents() { return allDetectionEvents; }
    public long getElapsedSimulationMillis() { return elapsedSimulationMillis; }

    public double getOverallCoveragePercentage() {
        if (zones.isEmpty()) return 0.0;
        double sum = 0.0;
        for (SearchZone z : zones) {
            sum += z.getCoveragePercentage();
        }
        return sum / zones.size();
    }

    public void addDrone(Drone drone) {
        if (drone == null) throw new IllegalArgumentException("Drone cannot be null");
        if (getDrone(drone.getId()) != null) throw new IllegalArgumentException("Drone ID already exists: " + drone.getId());
        drones.add(drone);
    }

    public void removeDrone(String droneId) {
        Drone drone = getDrone(droneId);
        if (drone != null) drones.remove(drone);
    }

    public Drone getDrone(String droneId) {
        for (Drone drone : drones) {
            if (drone.getId().equals(droneId)) return drone;
        }
        return null;
    }

    public List<Drone> getDrones() {
        return Collections.unmodifiableList(drones);
    }

    public void startSimulation() {
        running = true;
    }

    public void stopSimulation() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}