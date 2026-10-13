import com.dronesar.model.Drone;
import com.dronesar.model.Position;
import com.dronesar.search.LawnmowerPattern;
import com.dronesar.search.SearchManager;
import com.dronesar.search.SearchPattern;
import com.dronesar.search.ZoneAllocator;
import com.dronesar.model.SearchZone;

import java.util.ArrayList;
import java.util.List;

/**
 * Scratch class to test the search module in isolation, with no JavaFX
 * dependency. Delete this or move it under src/test before final merge,
 * or keep it as a dev tool - just don't wire it into Main.java.
 *
 * Run this directly (right-click -> Run) to see zone creation, drone
 * assignment, and the lawnmower sweep play out tick by tick in the console.
 */
public class SearchTestRunner {

    public static void main(String[] aSrgs) {
        final int numDrones = 6;
        final double forestWidth = 800;
        final double forestHeight = 600;
        final double droneSpeed = 8.0;
        final double batteryDrainRate = 0.15; // tune this up to see LOW_BATTERY / OFFLINE kick in mid-sweep
        final double sweepStepSize = 40.0; // stand-in for detection radius

        // 1. Create zones
        ZoneAllocator allocator = new ZoneAllocator();
        List<SearchZone> zones = allocator.createZones(forestWidth, forestHeight, numDrones);

        System.out.println("=== Zones created ===");
        for (SearchZone z : zones) {
            System.out.println(z);
        }

        // 2. Create drones, all starting at base (0,0)
        List<Drone> drones = new ArrayList<>();
        for (int i = 0; i < numDrones; i++) {
            drones.add(new Drone(i, new Position(0, 0), droneSpeed, batteryDrainRate));
        }

        // 3. Assign zones to drones
        allocator.allocateZones(drones, zones);

        // 4. Set up search manager and generate lawnmower paths
        SearchPattern pattern = new LawnmowerPattern();
        SearchManager manager = new SearchManager(drones, pattern, sweepStepSize);
        manager.initializePaths();

        System.out.println("\n=== Waypoint count per drone ===");
        for (Drone d : drones) {
            System.out.printf("Drone %d -> %d waypoints in zone %d%n",
                    d.getId(), d.getPath().size(), d.getAssignedZone().getId());
        }

        // 5. Run the simulation loop
        System.out.println("\n=== Running simulation ===");
        int tick = 0;
        final int maxTicks = 500; // safety cap

        while (!manager.allZonesCovered() && tick < maxTicks) {
            manager.update();

            if (tick % 20 == 0) { // print every 20 ticks to keep output readable
                System.out.println("---- Tick " + tick + " ----");
                for (Drone d : drones) {
                    System.out.println("  " + d);
                }
            }
            tick++;
        }

        System.out.println("\n=== Simulation finished at tick " + tick + " ===");
        for (Drone d : drones) {
            System.out.println(d);
        }
        System.out.println("All zones covered: " + manager.allZonesCovered());
    }
}