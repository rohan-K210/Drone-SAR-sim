package com.dronesar.database;

import com.dronesar.event.SimulationEventListener;
import com.dronesar.model.DetectionEvent;
import com.dronesar.model.Position;
import com.dronesar.model.enums.DroneState;

/**
 * Event listener that automatically persists detection and packet relay events
 * into the local SQLite database in real-time.
 */
public class DetectionPersistenceListener implements SimulationEventListener {

    private final DetectionDAO detectionDAO = new DetectionDAO();

    @Override
    public void onPersonDetected(DetectionEvent event) {
        detectionDAO.saveDetection(event);
    }

    @Override
    public void onPacketDeliveredToTower(DetectionEvent event) {
        event.setSuccessfullyDelivered(true);
        if (!detectionDAO.updateDeliveryStatus(event)) {
            detectionDAO.saveDetection(event);
        }
    }

    @Override
    public void onDroneMoved(String droneId, Position newPos, double heading) {}

    @Override
    public void onZoneCoverageUpdated(String zoneId, double coveragePercentage) {}

    @Override
    public void onDroneStatusChanged(String droneId, DroneState newStatus) {}
}
