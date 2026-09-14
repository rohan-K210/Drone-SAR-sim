package com.dronesar.controller;

import com.dronesar.event.SimulationEventBus;
import com.dronesar.model.*;
import com.dronesar.model.enums.DroneState;
import com.dronesar.network.CommunicationManager;
import com.dronesar.network.DijkstraRouter;
import com.dronesar.network.MeshNetworkGraph;
import com.dronesar.network.NetworkNode;
import com.dronesar.search.SearchPattern;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimulationController {

    private final List<Drone> drones;// Thread-safe list for concurrent access
    private final CommunicationManager communicationManager;
    private boolean running;

    private final double mapWidth;
    private final double mapHeight;

    private final List<SearchZone> zones = new CopyOnWriteArrayList<>();
    private final Map<String, List<Position>> droneWaypoints = new ConcurrentHashMap<>();
    private final Map<String, Integer> droneWaypointIndex = new ConcurrentHashMap<>();
    private final List<PersonNode> targets = new CopyOnWriteArrayList<>();
    private Tower centralTower;

    private final MeshNetworkGraph meshGraph = new MeshNetworkGraph();
    private final List<DetectionEvent> allDetectionEvents = new CopyOnWriteArrayList<>();
    private final List<ActiveRelaySession> activeRelaySessions = new CopyOnWriteArrayList<>();

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
        activeRelaySessions.clear();
        elapsedSimulationMillis = 0;

        centralTower = new Tower("BASE-TOWER-01", "Command Base Station", new Position(75, 480), 220.0);

        int cols = (droneCount >= 8) ? 4 : 3;
        int rows = (droneCount >= 8) ? 2 : 2;
        int actualCount = Math.min(droneCount, cols * rows);

        double zoneMarginLeft = 135.0;
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
                String zoneName = "Sector-" + (char)('A' + (zoneCounter - 1));
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
                Drone drone = new Drone(droneId, callsign, zoneId, startPos, sensorRadius, 230.0);
                addDrone(drone);

                zoneCounter++;
            }
        }

        targets.add(new PersonNode("P-101", "Rahul K.", new Position(280, 110)));
        targets.add(new PersonNode("P-102", "Ananya M.", new Position(540, 190)));
        targets.add(new PersonNode("P-103", "Vineeth S.", new Position(790, 130)));
        targets.add(new PersonNode("P-104", "Deepa T.", new Position(410, 410)));
        targets.add(new PersonNode("P-105", "Arun J.", new Position(720, 450)));

        updateMeshNetworkTopology();
    }

    public synchronized void tick(double deltaSeconds) {
        if (!running) return;

        double effectiveDelta = deltaSeconds * speedMultiplier;
        elapsedSimulationMillis += (long)(effectiveDelta * 1000);

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

        updateMeshNetworkTopology();
        checkPersonDetections();
        updateRelaySessions();

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

                    List<String> route = DijkstraRouter.findShortestPath(meshGraph, drone.getId(), centralTower.getId());
                    String eventId = "EVT-" + (System.currentTimeMillis() % 10000);
                    DetectionEvent event = new DetectionEvent(
                            eventId,
                            target.getId(),
                            target.getName(),
                            target.getLocation(),
                            drone.getId(),
                            route
                    );

                    allDetectionEvents.add(event);
                    SimulationEventBus.getInstance().publishPersonDetected(event);

                    if (!route.isEmpty()) {
                        SimulationEventBus.getInstance().publishMultiHopRoute(eventId, route);
                        activeRelaySessions.add(new ActiveRelaySession(event, route));
                    }
                    break;
                }
            }
        }
    }

    private void updateRelaySessions() {
        Iterator<ActiveRelaySession> iterator = activeRelaySessions.iterator();
        while (iterator.hasNext()) {
            ActiveRelaySession session = iterator.next();
            PacketHop currentHop = session.getCurrentHop();

            if (currentHop != null) {
                currentHop.step();
                if (currentHop.isComplete()) {
                    boolean hasMore = session.advanceToNextHop();
                    if (!hasMore) {
                        session.getEvent().setSuccessfullyDelivered(true);
                        centralTower.recordDetection(session.getEvent());
                        SimulationEventBus.getInstance().publishPacketDelivered(session.getEvent());

                        Drone originDrone = getDrone(session.getEvent().getOriginDroneId());
                        if (originDrone != null && originDrone.isOperational() && originDrone.getState() == DroneState.PERSON_FOUND) {
                            originDrone.setState(DroneState.SEARCHING);
                            SimulationEventBus.getInstance().publishDroneStatusChanged(originDrone.getId(), DroneState.SEARCHING);
                        }
                        activeRelaySessions.remove(session);
                    }
                }
            }
        }
    }

    public void updateMeshNetworkTopology() {
        meshGraph.clear();

        if (centralTower != null) {
            meshGraph.addNode(new NetworkNode(
                    centralTower.getId(),
                    centralTower.getLocation(),
                    centralTower.getReceptionRadius(),
                    true,
                    true
            ));
        }

        for (Drone drone : drones) {
            meshGraph.addNode(new NetworkNode(
                    drone.getId(),
                    drone.getPosition(),
                    drone.getCommunicationRadius(),
                    false,
                    drone.isOperational()
            ));
        }

        meshGraph.updateEdges();

        for (ActiveRelaySession session : activeRelaySessions) {
            PacketHop hop = session.getCurrentHop();
            if (hop == null) continue;

            NetworkNode nextNode = meshGraph.getNode(hop.getToId());
            if (nextNode == null || !nextNode.isOnline()) {
                String currentNodeId = hop.getFromId();
                List<String> newSubRoute = DijkstraRouter.findShortestPath(meshGraph, currentNodeId, centralTower.getId());
                if (!newSubRoute.isEmpty()) {
                    session.reRoute(newSubRoute);
                    SimulationEventBus.getInstance().publishRouteRecalculated(session.getEvent().getEventId(), newSubRoute);
                }
            }
        }
    }

    public void toggleDroneFailure(String droneId) {
        Drone drone = getDrone(droneId);
        if (drone != null) {
            drone.toggleOffline();
            SimulationEventBus.getInstance().publishDroneStatusChanged(drone.getId(), drone.getState());
            updateMeshNetworkTopology();
        }
    }

    public SearchZone findZoneById(String id) {
        for (SearchZone z : zones) {
            if (z.getId().equals(id)) return z;
        }
        return null;
    }

    public Position getNodeCoordinates(String nodeId) {
        if (centralTower != null && centralTower.getId().equals(nodeId)) {
            return centralTower.getLocation();
        }
        Drone d = getDrone(nodeId);
        return (d != null) ? d.getPosition() : null;
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
    public MeshNetworkGraph getMeshGraph() { return meshGraph; }
    public List<DetectionEvent> getAllDetectionEvents() { return allDetectionEvents; }
    public List<ActiveRelaySession> getActiveRelaySessions() { return activeRelaySessions; }
    public long getElapsedSimulationMillis() { return elapsedSimulationMillis; }

    public double getOverallCoveragePercentage() {
        if (zones.isEmpty()) return 0.0;
        double sum = 0.0;
        for (SearchZone z : zones) {
            sum += z.getCoveragePercentage();
        }
        return sum / zones.size();
    }

    // --- Original methods authored by Rohan ---
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

    public void updateDronePosition(String droneId, Position position) {
        Drone drone = getDrone(droneId);
        if (drone == null) throw new IllegalArgumentException("Drone not found: " + droneId);
        drone.setPosition(position);
    }

    public void startSimulation() {
        running = true;
        System.out.println("Drone SAR simulation started.");
    }

    public void stopSimulation() {
        running = false;
        System.out.println("Drone SAR simulation stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    public boolean sendMessageToTower(String droneId, String message) {
        Drone drone = getDrone(droneId);
        if (drone == null) throw new IllegalArgumentException("Drone not found: " + droneId);
        return communicationManager.sendToTower(drone, message);
    }

    public class ActiveRelaySession {
        private final DetectionEvent event;
        private List<String> route;
        private int currentHopIndex;
        private PacketHop currentHop;

        public ActiveRelaySession(DetectionEvent event, List<String> route) {
            this.event = event;
            this.route = new ArrayList<>(route);
            this.currentHopIndex = 0;
            initHop();
        }

        private void initHop() {
            if (currentHopIndex < route.size() - 1) {
                String fromId = route.get(currentHopIndex);
                String toId = route.get(currentHopIndex + 1);
                Position fromPos = getNodeCoordinates(fromId);
                Position toPos = getNodeCoordinates(toId);

                if (fromPos != null && toPos != null) {
                    this.currentHop = new PacketHop(event.getEventId(), fromId, toId, fromPos, toPos);
                    Drone drone = getDrone(fromId);
                    if (drone != null && drone.isOperational() && drone.getState() != DroneState.PERSON_FOUND) {
                        drone.setState(DroneState.RELAYING);
                    }
                }
            } else {
                this.currentHop = null;
            }
        }

        public boolean advanceToNextHop() {
            if (currentHopIndex < route.size() - 1) {
                String prevId = route.get(currentHopIndex);
                Drone drone = getDrone(prevId);
                if (drone != null && drone.getState() == DroneState.RELAYING) {
                    drone.setState(DroneState.SEARCHING);
                }
            }
            currentHopIndex++;
            initHop();
            return currentHop != null;
        }

        public void reRoute(List<String> newSubRoute) {
            this.route = new ArrayList<>(newSubRoute);
            this.currentHopIndex = 0;
            initHop();
        }

        public DetectionEvent getEvent() { return event; }
        public List<String> getRoute() { return route; }
        public PacketHop getCurrentHop() { return currentHop; }
    }
}