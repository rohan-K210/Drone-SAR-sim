package com.dronesar.search;

import com.dronesar.model.Position;
import com.dronesar.model.SearchZone;

import java.util.List;


public interface SearchPattern {

    /**
     * Generates an ordered list of waypoints covering the given zone.
     *
     * @param zone     the zone to sweep
     * @param stepSize spacing between sweep lines (typically the detection radius)
     * @return ordered waypoints from entry point to exit point
     */
    List<Position> generatePath(SearchZone zone, double stepSize);
}