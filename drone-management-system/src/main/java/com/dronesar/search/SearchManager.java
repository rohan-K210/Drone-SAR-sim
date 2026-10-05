package com.dronesar.search;

import com.dronesar.model.Drone;
import com.dronesar.model.Position;
import com.dronesar.model.SearchZone;

import java.util.List;

/**
 * Top-level coordinator for your module. This is the class the
 * SimulationController (Person 1) and the GUI (Person 4) will interact with.
 *
 * Responsibilities:
 *  - Generate a lawnmower path for every drone based on its assigned zone
 *  - Advance every drone by one tick when the simulation loop calls update()
 *  - Report whether the search phase is complete
 */
public class SearchManager {

    private final List<Drone> drones;
    private final SearchPattern searchPattern;
    private final double sweepStepSize; // typically matches detection radius from Person 3

    public SearchManager(List<Drone> drones, SearchPattern searchPattern, double sweepStepSize) {
        this.drones = drones;
        this.searchPattern = searchPattern;
        this.sweepStepSize = sweepStepSize;
    }

    /** Call once after zones have been allocated, before the simulation loop starts. */
    public void initializePaths() {
        for (Drone drone : drones) {
            SearchZone zone = drone.getAssignedZone();
            if (zone != null) {
                List<Position> path = searchPattern.generatePath(zone, sweepStepSize);
                drone.setPath(path);
            }
        }
    }

    /** Advances every drone in the fleet by one simulation tick. */
    public void update() {
        for (Drone drone : drones) {
            drone.tick();
        }
    }

    /** True once every assigned zone has been fully swept. */
    public boolean allZonesCovered() {
        for (Drone drone : drones) {
            SearchZone zone = drone.getAssignedZone();
            if (zone != null && !zone.isCovered()) {
                return false;
            }
        }
        return true;
    }

    public List<Drone> getDrones() {
        return drones;
    }
}