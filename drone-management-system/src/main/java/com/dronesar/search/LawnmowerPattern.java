package com.dronesar.search;

import com.dronesar.model.Position;
import com.dronesar.model.SearchZone;

import java.util.ArrayList;
import java.util.List;

/**
 * Classic back-and-forth "boustrophedon" sweep: the drone flies one edge
 * of the zone to the other, shifts down by stepSize, flies back, and so on
 * until the whole zone height is covered.
 */
public class LawnmowerPattern implements SearchPattern {

    @Override
    public List<Position> generatePath(SearchZone zone, double stepSize) {
        if (stepSize <= 0) {
            throw new IllegalArgumentException("stepSize must be positive");
        }

        List<Position> waypoints = new ArrayList<>();

        double left = zone.getX();
        double right = zone.getX() + zone.getWidth();
        double top = zone.getY();
        double bottom = zone.getY() + zone.getHeight();

        boolean movingRight = true;
        double y = top;

        // Guard against an infinite loop if stepSize is larger than the zone height
        while (y <= bottom) {
            if (movingRight) {
                waypoints.add(new Position(left, y));
                waypoints.add(new Position(right, y));
            } else {
                waypoints.add(new Position(right, y));
                waypoints.add(new Position(left, y));
            }
            y += stepSize;
            movingRight = !movingRight;
        }

        return waypoints;
    }
}