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
 * Coordinates drones, search sectors, dual-base towers, and direct wireless communications.
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
    private final List<Tower> towers = new CopyOnWriteArrayList<>();

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
        towers.clear();
        allDetectionEvents.clear();
        activeSignals.clear();
        pendingOfflineEvents.clear();
        elapsedSimulationMillis = 0;

        // 1. Establish Two Strategic Communication Towers for Full Map Coverage
        // West Command Base (South-West)
        towers.add(new Tower("BASE-WEST-01", "West Base Tower", new Position(70, 470), 340.0));
        // East Outpost Base (North-East)
        towers.add(new Tower("BASE-EAST-02", "East Base Tower", new Position(850, 95), 340.0));

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
                double laneSpacing = sensorRadius * 1.5;
                List<Position> wps = SearchPattern.generateBoustrophedonWaypoints(zone, laneSpacing, 16.0);

                // Stagger sweep direction and starting positions so drones move independently
                if (zoneCounter % 2 == 1 && wps.size() > 2) {
                    Collections.reverse(wps);
                }

                droneWaypoints.put(droneId, wps);
                int startingWpIdx = (zoneCounter * 3) % Math.max(1, wps.size());
                droneWaypointIndex.put(droneId, startingWpIdx);

                Position startPos = (wps.isEmpty()) ? zone.getCenter() : wps.get(startingWpIdx);
                Drone drone = new Drone(droneId, callsign, zoneId, startPos, sensorRadius, 340.0);

                // Give each drone a distinct operational speed (2.1 to 2.8 units)
                drone.setSpeed(2.1 + ((zoneCounter * 7) % 4) * 0.25);
                addDrone(drone);

                zoneCounter++;
            }
        }

        // Deploy lost persons across search sectors
        targets.add(new PersonNode("P-101", "Rahul K.", new Position(280, 110)));
        targets.add(new PersonNode("P-102", "Ananya M.", new Position(540, 190)));
        targets.add(new PersonNode("P-103", "Vineeth S.", new Position(780, 140)));
        targets.add(new PersonNode("P-104", "Deepa T.", new Position(410, 410)));
        targets.add(new PersonNode("P-105", "Arun J.", new Position(720, 450)));
    }

    public synchronized void tick(double deltaSeconds) {
        if (!running) return;

        double effectiveDelta = deltaSeconds * speedMultiplier;
        elapsedSimulationMillis += (long) (effectiveDelta * 1000);

        for (Tower tower : towers) {
            tower.updateBeacon();
        }

        // Update each drone independently
        for (Drone drone : drones) {
            // If offline, idle (manually paused), or out of battery, hold position
            if (!drone.isOperational() || drone.getState() == DroneState.IDLE) {
                continue;
            }

            List<Position> wps = droneWaypoints.get(drone.getId());
            if (wps == null || wps.isEmpty()) continue;

            int currIdx = droneWaypointIndex.getOrDefault(drone.getId(), 0);
            Position targetWp = wps.get(currIdx);
            Position currentPos = drone.getPosition();

            double stepDist = drone.getSpeed() * speedMultiplier * 1.15;
            double distToTarget = currentPos.distanceTo(targetWp);

            if (distToTarget <= stepDist) {
                drone.setPosition(targetWp);
                // Continuous search: loop waypoints so drones keep patrolling
                int nextIdx = (currIdx + 1) % wps.size();
                droneWaypointIndex.put(drone.getId(), nextIdx);
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

            drone.consumeBattery(0.005 * effectiveDelta);
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
                if (!drone.isOperational() || drone.getState() == DroneState.IDLE) continue;

                if (drone.getPosition().distanceTo(target.getLocation()) <= drone.getSensorRadius()) {
                    target.markDetected(drone.getId());
                    drone.setState(DroneState.PERSON_FOUND);
                    SimulationEventBus.getInstance().publishDroneStatusChanged(drone.getId(), DroneState.PERSON_FOUND);

                    Tower nearestTower = getNearestTower(drone.getPosition());
                    String towerId = (nearestTower != null) ? nearestTower.getId() : "BASE-WEST-01";

                    String eventId = "EVT-" + (System.currentTimeMillis() % 10000);
                    DetectionEvent event = new DetectionEvent(
                            eventId,
                            target.getId(),
                            target.getName(),
                            target.getLocation(),
                            drone.getId(),
                            towerId
                    );

                    allDetectionEvents.add(event);
                    SimulationEventBus.getInstance().publishPersonDetected(event);

                    transmitDetectionToTower(drone, event);
                    // Crucial: other drones continue their sweep! Only this target check breaks
                    break;
                }
            }
        }
    }

    private void transmitDetectionToTower(Drone drone, DetectionEvent event) {
        Tower towerInRange = getNearestTowerInRange(drone);
        if (towerInRange != null) {
            communicationManager.sendToTower(drone, "SURVIVOR FOUND: " + event.getPersonName() + " at " + event.getCoordinates());
            activeSignals.add(new TransmissionSignal(drone.getId(), towerInRange.getId(), drone.getPosition(), towerInRange.getLocation(), event));
            SimulationEventBus.getInstance().publishDirectTransmission(drone.getId(), towerInRange.getId(), event);
        } else {
            pendingOfflineEvents.add(event);
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
                Tower targetTower = getTower(signal.getTowerId());
                if (targetTower != null) {
                    targetTower.recordDetection(signal.getEvent());
                }
                SimulationEventBus.getInstance().publishPacketDelivered(signal.getEvent());

                // When transmission packet arrives at base, drone resumes search
                Drone originDrone = getDrone(signal.getDroneId());
                if (originDrone != null && originDrone.isOperational() && originDrone.getState() == DroneState.PERSON_FOUND) {
                    originDrone.setState(DroneState.SEARCHING);
                    SimulationEventBus.getInstance().publishDroneStatusChanged(originDrone.getId(), DroneState.SEARCHING);
                }
                iterator.remove();
            }
        }
    }

    // --- Tower Connectivity Logic ---
    public Tower getNearestTower(Position pos) {
        if (towers.isEmpty() || pos == null) return null;
        Tower nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (Tower t : towers) {
            double dist = pos.distanceTo(t.getLocation());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = t;
            }
        }
        return nearest;
    }

    public Tower getNearestTowerInRange(Drone drone) {
        if (drone == null || !drone.isOperational()) return null;
        Tower nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (Tower t : towers) {
            double dist = drone.getPosition().distanceTo(t.getLocation());
            if (dist <= t.getReceptionRadius() && dist < minDistance) {
                minDistance = dist;
                nearest = t;
            }
        }
        return nearest;
    }

    public boolean isDroneInTowerRange(Drone drone) {
        return getNearestTowerInRange(drone) != null;
    }

    public Tower getTower(String id) {
        for (Tower t : towers) {
            if (t.getId().equals(id)) return t;
        }
        return null;
    }

    public List<Tower> getTowers() {
        return Collections.unmodifiableList(towers);
    }

    public Tower getCentralTower() {
        return towers.isEmpty() ? null : towers.get(0);
    }

    // --- Individual Drone GUI Control Methods ---
    public void toggleDronePause(String droneId) {
        Drone drone = getDrone(droneId);
        if (drone != null && drone.isOperational()) {
            if (drone.getState() == DroneState.IDLE) {
                drone.setState(DroneState.SEARCHING);
            } else {
                drone.setState(DroneState.IDLE);
            }
            SimulationEventBus.getInstance().publishDroneStatusChanged(drone.getId(), drone.getState());
        }
    }

    public void recallDroneToBase(String droneId) {
        Drone drone = getDrone(droneId);
        if (drone != null && drone.isOperational()) {
            Tower nearest = getNearestTower(drone.getPosition());
            if (nearest != null) {
                List<Position> recallWps = new ArrayList<>();
                recallWps.add(nearest.getLocation());
                droneWaypoints.put(droneId, recallWps);
                droneWaypointIndex.put(droneId, 0);
                drone.setState(DroneState.SEARCHING);
                SimulationEventBus.getInstance().publishDroneStatusChanged(droneId, DroneState.SEARCHING);
            }
        }
    }

    public void resumeDroneSearch(String droneId) {
        Drone drone = getDrone(droneId);
        if (drone != null && drone.isOperational()) {
            SearchZone zone = findZoneById(drone.getAssignedZoneId());
            if (zone != null) {
                double laneSpacing = drone.getSensorRadius() * 1.5;
                List<Position> wps = SearchPattern.generateBoustrophedonWaypoints(zone, laneSpacing, 16.0);
                droneWaypoints.put(droneId, wps);
                droneWaypointIndex.put(droneId, 0);
                drone.setState(DroneState.SEARCHING);
                SimulationEventBus.getInstance().publishDroneStatusChanged(droneId, DroneState.SEARCHING);
            }
        }
    }

    public void toggleDroneFailure(String droneId) {
        Drone drone = getDrone(droneId);
        if (drone != null) {
            drone.toggleOffline();
            SimulationEventBus.getInstance().publishDroneStatusChanged(drone.getId(), drone.getState());
        }
    }

    public void addTarget(PersonNode person) {
        if (person != null) {
            targets.add(person);
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