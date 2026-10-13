package com.dronesar.search;

import com.dronesar.model.Drone;
import com.dronesar.model.Position;
import com.dronesar.model.SearchZone;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-level coordinator for multi-UAV sweep patterns and waypoint schedules.
 */
public class SearchManager {

    private final ZoneAllocator zoneAllocator;
    private final Map<String, List<Position>> droneFlightPaths = new ConcurrentHashMap<>();
    private final Map<String, Integer> currentWaypointIndices = new ConcurrentHashMap<>();

    public SearchManager() {
        this.zoneAllocator = new ZoneAllocator();
    }

    public List<SearchZone> initializeSearchGrid(double originX, double originY, double width, double height, int droneCount) {
        return zoneAllocator.allocateGridZones(originX, originY, width, height, droneCount);
    }

    public void planZoneCoverage(Drone drone, SearchZone zone, double laneSpacing, double margin) {
        List<Position> waypoints = SearchPattern.generateBoustrophedonWaypoints(zone, laneSpacing, margin);
        droneFlightPaths.put(drone.getId(), waypoints);
        currentWaypointIndices.put(drone.getId(), 0);
    }

    public Position getNextWaypoint(String droneId) {
        List<Position> path = droneFlightPaths.get(droneId);
        if (path == null || path.isEmpty()) return null;

        int index = currentWaypointIndices.getOrDefault(droneId, 0);
        if (index >= path.size()) {
            index = 0; // Continuous loop sweep
            currentWaypointIndices.put(droneId, 0);
        }
        return path.get(index);
    }

    public void advanceWaypoint(String droneId) {
        List<Position> path = droneFlightPaths.get(droneId);
        if (path != null && !path.isEmpty()) {
            int current = currentWaypointIndices.getOrDefault(droneId, 0);
            currentWaypointIndices.put(droneId, (current + 1) % path.size());
        }
    }

    public List<Position> getFlightPath(String droneId) {
        return droneFlightPaths.getOrDefault(droneId, Collections.emptyList());
    }

    public void clear() {
        droneFlightPaths.clear();
        currentWaypointIndices.clear();
    }
}
