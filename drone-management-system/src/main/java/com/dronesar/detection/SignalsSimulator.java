package com.dronesar.detection;

import com.dronesar.model.Drone;
import com.dronesar.model.PersonNode;
import com.dronesar.model.Position;
import com.dronesar.model.Tower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Simulates the radio-frequency layer of the forest search area:
 * each undetected missing person emits an emergency beacon, and this
 * engine computes the raw RSSI (Received Signal Strength Indicator,
 * in dBm) reports that every online mesh node (drones and the central
 * tower) would observe.
 *
 * <p>Propagation follows the log-distance path loss model of
 * {@link LocalizationEngine}; a Gaussian (normal) noise term is added
 * on top to mimic real-world RSSI fluctuations, and reports falling
 * below the noise floor are discarded as unreadable.</p>
 */
public class SignalsSimulator {

    /**
     * Beacon power (dBm) received at {@link LocalizationEngine#REFERENCE_DISTANCE},
     * i.e. the beacon's effective TX power after reference-distance loss.
     */
    public static final double BEACON_RSSI_AT_REFERENCE_DBM =
            LocalizationEngine.REFERENCE_RSSI_DBM;

    /** Standard deviation (dB) of the Gaussian RSSI measurement noise. */
    public static final double MEASUREMENT_NOISE_DB = 2.5;

    /** Reports weaker than this RSSI (dBm) are treated as background noise. */
    public static final double NOISE_FLOOR_DBM = -85.0;

    private final Random random;
    private double noiseStdDevDb;

    public SignalsSimulator() {
        this(new Random());
    }

    public SignalsSimulator(Random random) {
        this.random = (random != null) ? random : new Random();
        this.noiseStdDevDb = MEASUREMENT_NOISE_DB;
    }

    /** A single node's RSSI observation of a beacon source. */
    public static class SignalReport {
        private final String nodeId;
        private final boolean fromTower;
        private final Position nodePosition;
        private final double rssiDbm;
        private final double trueDistance;

        public SignalReport(String nodeId, boolean fromTower, Position nodePosition,
                            double rssiDbm, double trueDistance) {
            this.nodeId = nodeId;
            this.fromTower = fromTower;
            this.nodePosition = nodePosition;
            this.rssiDbm = rssiDbm;
            this.trueDistance = trueDistance;
        }

        public String getNodeId() { return nodeId; }
        public boolean isFromTower() { return fromTower; }
        public Position getNodePosition() { return nodePosition; }

        /** Measured signal strength in dBm (closer to 0 = stronger). */
        public double getRssiDbm() { return rssiDbm; }

        /**
         * Distance approximation obtained by inverting the RSSI path
         * loss model ({@link LocalizationEngine#estimateDistance(double)}).
         */
        public double getEstimatedDistance() {
            return LocalizationEngine.estimateDistance(rssiDbm);
        }

        public double getTrueDistance() { return trueDistance; }

        @Override
        public String toString() {
            return String.format("SignalReport{%s, rssi=%.1f dBm, dist=%.1f}",
                    nodeId, rssiDbm, trueDistance);
        }
    }

    /**
     * Collects beacon observations of one missing person from all
     * nodes that can hear the beacon.
     *
     * @param person the missing person whose beacon is being sensed
     * @param drones all fleet drones (offline ones are skipped)
     * @param tower  the central tower, or {@code null} to ignore it
     * @return immutable list of reports, strongest signal (highest RSSI) first
     */
    public List<SignalReport> collectBeaconReports(PersonNode person,
                                                   List<Drone> drones,
                                                   Tower tower) {
        if (person == null) return Collections.emptyList();

        Position beaconPos = person.getLocation();
        List<SignalReport> reports = new ArrayList<>();

        if (tower != null) {
            addReportIfAudible(reports, tower.getId(), true,
                    tower.getLocation(), beaconPos);
        }

        if (drones != null) {
            for (Drone drone : drones) {
                if (!drone.isOperational()) continue;
                addReportIfAudible(reports, drone.getId(), false,
                        drone.getPosition(), beaconPos);
            }
        }

        reports.sort((a, b) -> Double.compare(b.getRssiDbm(), a.getRssiDbm()));
        return Collections.unmodifiableList(reports);
    }

    private void addReportIfAudible(List<SignalReport> reports, String nodeId, boolean fromTower,
                                    Position nodePos, Position beaconPos) {
        if (nodeId == null || nodePos == null) return;

        double trueDistance = nodePos.distanceTo(beaconPos);
        double idealRssi = LocalizationEngine.rssiAtDistance(
                trueDistance,
                LocalizationEngine.REFERENCE_DISTANCE,
                BEACON_RSSI_AT_REFERENCE_DBM,
                LocalizationEngine.PATH_LOSS_EXPONENT);

        // Gaussian shadowing/measurement noise on top of the ideal model.
        double measuredRssi = idealRssi + random.nextGaussian() * noiseStdDevDb;
        measuredRssi = Math.max(LocalizationEngine.MIN_RSSI_DBM,
                Math.min(LocalizationEngine.MAX_RSSI_DBM, measuredRssi));

        if (measuredRssi < NOISE_FLOOR_DBM) return; // lost in background noise

        reports.add(new SignalReport(nodeId, fromTower, nodePos, measuredRssi, trueDistance));
    }

    public double getNoiseStdDevDb() { return noiseStdDevDb; }

    public void setNoiseStdDevDb(double noiseStdDevDb) {
        if (noiseStdDevDb < 0) {
            throw new IllegalArgumentException("Noise standard deviation cannot be negative");
        }
        this.noiseStdDevDb = noiseStdDevDb;
    }
}
