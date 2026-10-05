package com.dronesar.search;

import com.dronesar.model.Drone;
import com.dronesar.model.SearchZone;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for:
 *  1. Partitioning the forest into non-overlapping rectangular zones.
 *  2. Assigning exactly one zone to each drone.
 */
public class ZoneAllocator {

    /**
     * Splits the forest into a roughly square grid of zones, one per drone.
     * Example: 6 drones -> 3 columns x 2 rows.
     */
    public List<SearchZone> createZones(double forestWidth, double forestHeight, int numDrones) {
        if (numDrones <= 0) {
            throw new IllegalArgumentException("numDrones must be positive");
        }

        List<SearchZone> zones = new ArrayList<>();

        int cols = (int) Math.ceil(Math.sqrt(numDrones));
        int rows = (int) Math.ceil((double) numDrones / cols);

        double zoneWidth = forestWidth / cols;
        double zoneHeight = forestHeight / rows;

        int id = 0;
        for (int r = 0; r < rows && id < numDrones; r++) {
            for (int c = 0; c < cols && id < numDrones; c++) {
                double x = c * zoneWidth;
                double y = r * zoneHeight;
                zones.add(new SearchZone(id, x, y, zoneWidth, zoneHeight));
                id++;
            }
        }
        return zones;
    }

    /**
     * Assigns zones to drones on a 1-to-1 basis (drone i gets zone i).
     * For the MVP this simple pairing is sufficient; a nearest-zone
     * assignment could be swapped in later without changing the interface.
     */
    public void allocateZones(List<Drone> drones, List<SearchZone> zones) {
        if (drones.size() != zones.size()) {
            throw new IllegalArgumentException(
                    "Number of drones (" + drones.size() + ") must match number of zones (" + zones.size() + ")"
            );
        }

        for (int i = 0; i < drones.size(); i++) {
            Drone drone = drones.get(i);
            SearchZone zone = zones.get(i);
            zone.assignTo(drone.getId());
            drone.setAssignedZone(zone);
        }
    }
}