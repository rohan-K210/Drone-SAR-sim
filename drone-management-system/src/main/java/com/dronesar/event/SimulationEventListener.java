package com.dronesar.event;

import com.dronesar.model.DetectionEvent;
import com.dronesar.model.Position;
import com.dronesar.model.enums.DroneState;

import java.util.List;

public interface SimulationEventListener {
    void onDroneMoved(String droneId, Position newPos, double heading);
    void onPersonDetected(DetectionEvent event);
    void onZoneCoverageUpdated(String zoneId, double coveragePercentage);
    void onDroneStatusChanged(String droneId, DroneState newStatus);
    void onMultiHopRouteCalculated(String eventId, List<String> routeNodeIds);
    void onPacketDeliveredToTower(DetectionEvent event);
    void onRouteRecalculated(String eventId, List<String> newRouteNodeIds);
}
