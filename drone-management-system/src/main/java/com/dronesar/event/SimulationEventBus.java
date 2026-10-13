package com.dronesar.event;

import com.dronesar.model.DetectionEvent;
import com.dronesar.model.Position;
import com.dronesar.model.enums.DroneState;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Event bus implementing Singleton design pattern (Syllabus Module 3)
 * and Delegation Event Model (Syllabus Module 4).
 */
public class SimulationEventBus {
    private static final SimulationEventBus INSTANCE = new SimulationEventBus();
    private final List<SimulationEventListener> listeners = new CopyOnWriteArrayList<>();

    private SimulationEventBus() {}

    public static SimulationEventBus getInstance() {
        return INSTANCE;
    }

    public void registerListener(SimulationEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(SimulationEventListener listener) {
        listeners.remove(listener);
    }

    public void publishDroneMoved(String droneId, Position newPos, double heading) {
        for (SimulationEventListener listener : listeners) {
            listener.onDroneMoved(droneId, newPos, heading);
        }
    }

    public void publishPersonDetected(DetectionEvent event) {
        for (SimulationEventListener listener : listeners) {
            listener.onPersonDetected(event);
        }
    }

    public void publishZoneCoverageUpdated(String zoneId, double coveragePercentage) {
        for (SimulationEventListener listener : listeners) {
            listener.onZoneCoverageUpdated(zoneId, coveragePercentage);
        }
    }

    public void publishDroneStatusChanged(String droneId, DroneState newStatus) {
        for (SimulationEventListener listener : listeners) {
            listener.onDroneStatusChanged(droneId, newStatus);
        }
    }

    public void publishDirectTransmission(String droneId, String towerId, DetectionEvent event) {
        for (SimulationEventListener listener : listeners) {
            listener.onDirectTransmission(droneId, towerId, event);
        }
    }

    public void publishMultiHopRoute(String eventId, List<String> routeNodeIds) {
        for (SimulationEventListener listener : listeners) {
            listener.onMultiHopRouteCalculated(eventId, routeNodeIds);
        }
    }

    public void publishRouteRecalculated(String eventId, List<String> newRouteNodeIds) {
        for (SimulationEventListener listener : listeners) {
            listener.onRouteRecalculated(eventId, newRouteNodeIds);
        }
    }

    public void publishPacketDelivered(DetectionEvent event) {
        for (SimulationEventListener listener : listeners) {
            listener.onPacketDeliveredToTower(event);
        }
    }
}
