package com.dronesar;

import com.dronesar.controller.SimulationController;
import com.dronesar.model.Drone;
import com.dronesar.model.PersonNode;
import com.dronesar.model.Position;
import com.dronesar.model.Tower;
import com.dronesar.network.CommunicationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommunicationAndSimulationTest {

    private SimulationController controller;
    private CommunicationManager commsManager;

    @BeforeEach
    void setUp() {
        controller = new SimulationController();
        commsManager = new CommunicationManager();
    }

    @Test
    void testDirectTowerCommunication() {
        Tower tower = controller.getCentralTower();
        assertNotNull(tower);

        // Place a drone within tower's reception radius (380m)
        Drone closeDrone = new Drone("D-TEST-1", "Test-1", "Z-1", tower.getLocation(), 40.0, 380.0);
        assertTrue(controller.isDroneInTowerRange(closeDrone));
        assertTrue(commsManager.sendToTower(closeDrone, "STATUS: IN RANGE"));
    }

    @Test
    void testOfflineDroneFailsCommunication() {
        Tower tower = controller.getCentralTower();
        Drone drone = new Drone("D-TEST-2", "Test-2", "Z-1", tower.getLocation(), 40.0, 380.0);
        drone.toggleOffline();

        assertFalse(drone.isOperational());
        assertFalse(commsManager.sendToTower(drone, "STATUS: TEST"));
    }

    @Test
    void testSimulationStepMovementAndBattery() {
        Drone drone = controller.getDrones().get(0);
        Position initialPos = drone.getPosition();
        double initialBattery = drone.getBatteryLevel();

        controller.startSimulation();
        controller.tick(1.0);

        // Position should update or waypoint progress should occur
        assertTrue(drone.getBatteryLevel() < initialBattery, "Battery level must deplete during active flight");
    }

    @Test
    void testSurvivorDetectionDirectAlert() {
        Drone drone = controller.getDrones().get(0);
        // Place survivor right at drone's current position
        PersonNode survivor = new PersonNode("P-TEST", "Lost Hiker", drone.getPosition());
        controller.addTarget(survivor);

        controller.startSimulation();
        controller.tick(0.1);

        assertTrue(survivor.isDetected(), "Survivor within sensor range must be detected");
        assertFalse(controller.getAllDetectionEvents().isEmpty(), "Detection event must be recorded");
    }

    @Test
    void testOddDroneCountAreaPartitioningCoverage() {
        // Test with 3 drones (odd)
        controller.initDefaultScenario(3, 3);
        assertEquals(3, controller.getDrones().size());
        assertEquals(3, controller.getZones().size());

        double totalArea3 = 0.0;
        for (var zone : controller.getZones()) {
            totalArea3 += (zone.getMaxX() - zone.getMinX()) * (zone.getMaxY() - zone.getMinY());
        }

        // Test with 5 drones (odd)
        controller.initDefaultScenario(5, 5);
        assertEquals(5, controller.getDrones().size());
        assertEquals(5, controller.getZones().size());

        double totalArea5 = 0.0;
        for (var zone : controller.getZones()) {
            totalArea5 += (zone.getMaxX() - zone.getMinX()) * (zone.getMaxY() - zone.getMinY());
        }

        // Total search area should remain equal and fixed across scenarios regardless of odd/even drone counts
        assertEquals(totalArea3, totalArea5, 0.01, "Search space area must be fully preserved across any drone count");
    }

    @Test
    void testEvenDroneCountAreaPartitioningCoverage() {
        controller.initDefaultScenario(4, 4);
        assertEquals(4, controller.getDrones().size());
        assertEquals(4, controller.getZones().size());

        double totalArea4 = 0.0;
        for (var zone : controller.getZones()) {
            totalArea4 += (zone.getMaxX() - zone.getMinX()) * (zone.getMaxY() - zone.getMinY());
        }

        controller.initDefaultScenario(6, 4);
        assertEquals(6, controller.getDrones().size());
        assertEquals(6, controller.getZones().size());

        double totalArea6 = 0.0;
        for (var zone : controller.getZones()) {
            totalArea6 += (zone.getMaxX() - zone.getMinX()) * (zone.getMaxY() - zone.getMinY());
        }

        assertEquals(totalArea4, totalArea6, 0.01, "Search space area must remain identical for 4 and 6 drones");
    }
}
