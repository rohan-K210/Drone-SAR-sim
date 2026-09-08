package com.dronesar.search;

import com.dronesar.model.Position;
import com.dronesar.model.SearchZone;

import java.util.ArrayList;
import java.util.List;

public class SearchPattern {

    /**
     * Generates back-and-forth parallel boustrophedon sweep waypoints for a search zone.
     */
    public static List<Position> generateBoustrophedonWaypoints(SearchZone zone, double laneSpacing, double margin) {
        List<Position> waypoints = new ArrayList<>();

        double startX = zone.getMinX() + margin;
        double endX = zone.getMaxX() - margin;
        double startY = zone.getMinY() + margin;
        double endY = zone.getMaxY() - margin;

        if (startX >= endX || startY >= endY) {
            waypoints.add(zone.getCenter());
            return waypoints;
        }

        boolean movingDown = true;
        double currentX = startX;

        while (currentX <= endX) {
            if (movingDown) {
                waypoints.add(new Position(currentX, startY));
                waypoints.add(new Position(currentX, endY));
            } else {
                waypoints.add(new Position(currentX, endY));
                waypoints.add(new Position(currentX, startY));
            }
            currentX += laneSpacing;
            movingDown = !movingDown;
        }

        waypoints.add(new Position(startX, startY));
        return waypoints;
    }
}
