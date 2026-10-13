package com.dronesar.detection;

import com.dronesar.model.PersonNode;
import com.dronesar.model.Position;

import java.util.Random;

/**
 * Simulates emergency beacon transmissions, RF signal attenuation, and environmental noise in dense forest.
 */
public class SignalsSimulator {

    private final Random random = new Random();

    /**
     * Calculates signal strength (RSSI in dBm) given distance and tree canopy loss factor.
     */
    public double calculateReceivedSignalStrength(Position transmitter, Position receiver, double forestAttenuationDbPerM) {
        double distance = transmitter.distanceTo(receiver);
        if (distance <= 0.0) return -20.0; // Near field saturated

        // Standard Log-Distance Path Loss Model
        double pathLoss = 20.0 * Math.log10(distance) + (forestAttenuationDbPerM * distance);
        double noise = (random.nextDouble() - 0.5) * 4.0; // Gaussian jitter
        return -30.0 - pathLoss + noise;
    }

    /**
     * Determines whether an emergency beacon ping successfully penetrates current weather/foliage.
     */
    public boolean isBeaconTransmittedSuccessfully(PersonNode survivor, double weatherInterferenceFactor) {
        if (survivor == null) return false;
        double clampedInterference = Math.max(0.0, Math.min(0.5, weatherInterferenceFactor));
        double packetLossThreshold = 0.05 + clampedInterference;
        return random.nextDouble() > packetLossThreshold;
    }
}
