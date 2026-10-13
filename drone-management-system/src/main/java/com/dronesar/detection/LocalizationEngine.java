package com.dronesar.detection;

import com.dronesar.model.Position;

import java.util.List;

/**
 * Trilateration and spatial position estimation engine.
 * Computes high-accuracy GPS coordinates using signal pings from multiple UAVs or Towers.
 */
public class LocalizationEngine {

    /**
     * Estimates target location using weighted centroid from multi-point sightings.
     */
    public Position estimateTargetPosition(List<Position> observationPoints, List<Double> signalWeights) {
        if (observationPoints == null || observationPoints.isEmpty()) {
            return new Position(0, 0);
        }
        if (observationPoints.size() == 1 || signalWeights == null || signalWeights.size() != observationPoints.size()) {
            return observationPoints.get(0);
        }

        double totalWeight = 0.0;
        double weightedX = 0.0;
        double weightedY = 0.0;

        for (int i = 0; i < observationPoints.size(); i++) {
            double w = Math.max(0.001, signalWeights.get(i));
            Position p = observationPoints.get(i);
            weightedX += p.getX() * w;
            weightedY += p.getY() * w;
            totalWeight += w;
        }

        return new Position(weightedX / totalWeight, weightedY / totalWeight);
    }
}
