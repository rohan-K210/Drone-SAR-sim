package com.dronesar.detection;

import com.dronesar.model.Position;

import java.util.List;

/**
 * Pure-math engine for estimating the position of a signal source
 * (e.g. a detected missing person's emergency beacon) from RSSI
 * (Received Signal Strength Indicator, in dBm) measurements reported
 * by mesh nodes.
 *
 * <p>Signal propagation uses the log-distance path loss model:</p>
 * <pre>
 *     RSSI(d) = TX_POWER - 10 * n * log10(d / D0)
 * </pre>
 * <p>where {@code TX_POWER} is the received strength at the reference
 * distance {@code D0}, and {@code n} is the path loss exponent
 * (n = 2 in free space; n &gt; 2 models extra attenuation in forest
 * terrain). Inverting the model converts a measured RSSI value back
 * into an estimated distance, and combining three or more such
 * estimates yields a position via weighted linear least-squares
 * trilateration.</p>
 */
public final class LocalizationEngine {

    /** Reference distance (map units) for the path loss model. */
    public static final double REFERENCE_DISTANCE = 10.0;

    /**
     * Received power (dBm) at the reference distance, i.e. the beacon's
     * effective TX power after antenna gains and reference-distance free
     * space loss.
     */
    public static final double REFERENCE_RSSI_DBM = -40.0;

    /**
     * Path loss exponent: 2.0 = free space, &gt; 2 = obstructed
     * (forest/indoor) terrain.
     */
    public static final double PATH_LOSS_EXPONENT = 2.2;

    /** Strongest RSSI (dBm) the model can report (cap at ~1 m range). */
    public static final double MAX_RSSI_DBM = -30.0;

    /**
     * Weak RSSI (dBm) floor used to keep the inverse model finite;
     * values below {@link SignalsSimulator#NOISE_FLOOR_DBM} are treated
     * as unreadable noise anyway.
     */
    public static final double MIN_RSSI_DBM = -100.0;

    private static final double EPSILON = 1e-9;

    private LocalizationEngine() {
        // Static utility class - no instances.
    }

    /**
     * RSSI (dBm) observed by a node at the given distance, using the
     * default calibration constants.
     */
    public static double rssiAtDistance(double distance) {
        return rssiAtDistance(distance, REFERENCE_DISTANCE, REFERENCE_RSSI_DBM,
                PATH_LOSS_EXPONENT);
    }

    /**
     * RSSI (dBm) observed by a node at the given distance using the
     * log-distance path loss model. Capped at {@link #MAX_RSSI_DBM}.
     */
    public static double rssiAtDistance(double distance, double referenceDistance,
                                        double rssiAtReference, double pathLossExponent) {
        if (distance < 0) {
            throw new IllegalArgumentException("Distance cannot be negative");
        }
        if (distance <= EPSILON || referenceDistance <= EPSILON) {
            return MAX_RSSI_DBM;
        }
        double rssi = rssiAtReference
                - 10.0 * pathLossExponent * Math.log10(distance / referenceDistance);
        // RSSI is negative (closer to 0 = stronger): cap with min so that
        // readings never exceed MAX_RSSI_DBM at very short range.
        return Math.min(rssi, MAX_RSSI_DBM);
    }

    /**
     * Inverse of {@link #rssiAtDistance(double)} with the default
     * calibration: converts a measured RSSI value into an estimated
     * distance (distance approximation from RSSI).
     */
    public static double estimateDistance(double measuredRssi) {
        return estimateDistance(measuredRssi, REFERENCE_DISTANCE, REFERENCE_RSSI_DBM,
                PATH_LOSS_EXPONENT);
    }

    /**
     * Inverse of the log-distance path loss model with explicit
     * calibration:
     * <pre>
     *     d = D0 * 10^((RSSI@D0 - RSSI) / (10 * n))
     * </pre>
     */
    public static double estimateDistance(double measuredRssi, double referenceDistance,
                                          double rssiAtReference, double pathLossExponent) {
        double rssi = Math.max(MIN_RSSI_DBM, Math.min(MAX_RSSI_DBM, measuredRssi));
        return referenceDistance
                * Math.pow(10.0, (rssiAtReference - rssi) / (10.0 * pathLossExponent));
    }

    /**
     * Trilaterates a position from anchor points (reporter positions) and
     * their estimated distances to the unknown source.
     *
     * <p>With 3+ anchors this performs weighted linear least-squares
     * trilateration. With 2 anchors it degrades to a distance-weighted
     * midpoint; with 1 anchor it returns that anchor as the best guess.
     * Returns {@code null} when no estimate can be produced or the
     * anchor geometry is degenerate (e.g. collinear anchors).</p>
     *
     * @param anchorPositions    positions of the reporting nodes
     * @param estimatedDistances estimated source distance for each reporter
     * @return estimated source position, or {@code null}
     */
    public static Position trilaterate(List<Position> anchorPositions, List<Double> estimatedDistances) {
        if (anchorPositions == null || estimatedDistances == null) return null;
        if (anchorPositions.isEmpty()
                || anchorPositions.size() != estimatedDistances.size()) {
            return null;
        }

        int n = anchorPositions.size();

        if (n == 1) {
            return copyOf(anchorPositions.get(0));
        }

        if (n == 2) {
            // Degraded mode: distance-weighted midpoint, biased toward the
            // closer (stronger) reporter.
            List<Double> weights = List.of(
                    weightFor(estimatedDistances.get(0)),
                    weightFor(estimatedDistances.get(1)));
            return weightedCentroid(anchorPositions, weights);
        }

        // Linear least squares with the first anchor as reference:
        // for every other anchor i, the equation
        //     ax*dx + ay*dy = b
        // relates the unknown offset (dx, dy) from the reference anchor.
        Position p0 = anchorPositions.get(0);
        double r0 = Math.max(estimatedDistances.get(0), EPSILON);

        double mxx = 0, mxy = 0, myy = 0;
        double vx = 0, vy = 0;

        for (int i = 1; i < n; i++) {
            Position pi = anchorPositions.get(i);
            double ri = Math.max(estimatedDistances.get(i), EPSILON);

            double ax = pi.getX() - p0.getX();
            double ay = pi.getY() - p0.getY();
            double anchorDistSq = ax * ax + ay * ay;
            double b = 0.5 * (anchorDistSq + r0 * r0 - ri * ri);

            // Nearer reporters (smaller estimated distance) are more reliable.
            double w = weightFor(ri);

            mxx += w * ax * ax;
            mxy += w * ax * ay;
            myy += w * ay * ay;
            vx += w * ax * b;
            vy += w * ay * b;
        }

        double determinant = mxx * myy - mxy * mxy;
        if (Math.abs(determinant) < EPSILON) {
            return null; // collinear anchors - caller should fall back to centroid
        }

        double dx = (myy * vx - mxy * vy) / determinant;
        double dy = (mxx * vy - mxy * vx) / determinant;

        return new Position(p0.getX() + dx, p0.getY() + dy);
    }

    /**
     * Weighted centroid (arithmetic mean with per-point weights).
     * Used as a fallback when trilateration is degenerate.
     *
     * @return weighted average position, or {@code null} for empty input
     */
    public static Position weightedCentroid(List<Position> points, List<Double> weights) {
        if (points == null || weights == null) return null;
        if (points.isEmpty() || points.size() != weights.size()) return null;

        double sumW = 0, sumX = 0, sumY = 0;
        for (int i = 0; i < points.size(); i++) {
            double w = weights.get(i);
            sumW += w;
            sumX += w * points.get(i).getX();
            sumY += w * points.get(i).getY();
        }
        if (sumW <= EPSILON) return null;

        return new Position(sumX / sumW, sumY / sumW);
    }

    /** Reliability weight for a reporter at the given estimated distance. */
    private static double weightFor(double distance) {
        return 1.0 / Math.max(distance * distance, EPSILON);
    }

    private static Position copyOf(Position p) {
        return p == null ? null : new Position(p.getX(), p.getY());
    }
}
