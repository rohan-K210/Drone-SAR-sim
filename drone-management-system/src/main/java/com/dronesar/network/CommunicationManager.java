package com.dronesar.network;

import com.dronesar.model.Drone;

public class CommunicationManager {

    /**
     * Simulates direct communication between
     * a drone and the central tower.
     */
    public boolean sendToTower(Drone drone, String message) {

        if (drone == null) {
            throw new IllegalArgumentException(
                    "Drone cannot be null"
            );
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Message cannot be empty"
            );
        }

        // An offline drone cannot communicate.
        if (!drone.isOperational()) {

            System.out.println(
                    "Communication failed: Drone "
                            + drone.getId()
                            + " is offline."
            );

            return false;
        }

        System.out.println(
                "Drone "
                        + drone.getId()
                        + " -> Central Tower: "
                        + message
        );

        return true;
    }
}