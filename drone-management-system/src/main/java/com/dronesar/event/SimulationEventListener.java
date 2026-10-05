package com.dronesar.event;

import com.dronesar.model.DetectionEvent;
import com.dronesar.model.Position;
import com.dronesar.model.enums.DroneState;

import java.util.List;

/**
 * Listener interface for simulation events (Delegation Event Model - Syllabus Module 4).
 */
public interface SimulationEventListener {
    void onDroneMoved(String droneId, Position newPos, double heading);
    void onPersonDetected(DetectionEvent event);
    void onZoneCoverageUpdated(String zoneId, double coveragePercentage);
    void onDroneStatusChanged(String droneId, DroneState newStatus);
    default void onDirectTransmission(String droneId, String towerId, DetectionEvent event) {}
    default void onMultiHopRouteCalculated(String eventId, List<String> routeNodeIds) {}
    default void onRouteRecalculated(String eventId, List<String> newRouteNodeIds) {}
    default void onPacketDeliveredToTower(DetectionEvent event) {}
}
