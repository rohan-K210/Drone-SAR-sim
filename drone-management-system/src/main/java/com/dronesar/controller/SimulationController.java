package com.dronesar.controller;

import com.dronesar.model.Drone;
import com.dronesar.model.Position;
import com.dronesar.network.CommunicationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimulationController {

    private final List<Drone> drones;

    private final CommunicationManager communicationManager;

    private boolean running;

    public SimulationController() {

        this.drones = new ArrayList<>();

        this.communicationManager =
                new CommunicationManager();

        this.running = false;
    }

    /**
     * Adds a drone to the simulation.
     */
    public void addDrone(Drone drone) {

        if (drone == null) {
            throw new IllegalArgumentException(
                    "Drone cannot be null"
            );
        }

        // Drone IDs must be unique.
        if (getDrone(drone.getId()) != null) {

            throw new IllegalArgumentException(
                    "Drone ID already exists: "
                            + drone.getId()
            );
        }

        drones.add(drone);
    }

    /**
     * Removes a drone from the simulation.
     */
    public void removeDrone(String droneId) {

        Drone drone = getDrone(droneId);

        if (drone != null) {
            drones.remove(drone);
        }
    }

    /**
     * Finds a drone using its unique ID.
     */
    public Drone getDrone(String droneId) {

        for (Drone drone : drones) {

            if (drone.getId().equals(droneId)) {
                return drone;
            }
        }

        return null;
    }

    /**
     * Returns all drones currently registered
     * in the simulation.
     */
    public List<Drone> getDrones() {

        return Collections.unmodifiableList(drones);
    }

    /**
     * Updates the position of a drone.
     */
    public void updateDronePosition(
            String droneId,
            Position position) {

        Drone drone = getDrone(droneId);

        if (drone == null) {

            throw new IllegalArgumentException(
                    "Drone not found: " + droneId
            );
        }

        drone.setPosition(position);
    }

    /**
     * Starts the simulation.
     */
    public void startSimulation() {

        running = true;

        System.out.println(
                "Drone SAR simulation started."
        );
    }

    /**
     * Stops the simulation.
     */
    public void stopSimulation() {

        running = false;

        System.out.println(
                "Drone SAR simulation stopped."
        );
    }

    /**
     * Returns whether the simulation is currently running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Sends a message from a drone to the central tower.
     */
    public boolean sendMessageToTower(
            String droneId,
            String message) {

        Drone drone = getDrone(droneId);

        if (drone == null) {

            throw new IllegalArgumentException(
                    "Drone not found: " + droneId
            );
        }

        return communicationManager.sendToTower(
                drone,
                message
        );
    }
}