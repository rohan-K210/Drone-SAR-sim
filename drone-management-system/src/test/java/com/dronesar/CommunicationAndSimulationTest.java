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
}
