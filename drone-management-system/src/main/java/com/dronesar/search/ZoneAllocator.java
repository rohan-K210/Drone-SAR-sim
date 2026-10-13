package com.dronesar.search;

import com.dronesar.model.Drone;
import com.dronesar.model.Position;
import com.dronesar.model.SearchZone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intelligent spatial zone partitioner.
 * Subdivides tactical terrain into balanced search sectors and assigns them to available UAVs.
 */
public class ZoneAllocator {

    /**
     * Divides an operational bounding area into a grid of SearchZones.
     */
    public List<SearchZone> allocateGridZones(double originX, double originY, double width, double height, int droneCount) {
        List<SearchZone> zones = new ArrayList<>();
        if (droneCount <= 0) {
            return zones;
        }

        int rows = (droneCount <= 3) ? 1 : 2;
        int zoneIdx = 0;

        for (int r = 0; r < rows; r++) {
            int colsInRow;
            if (rows == 1) {
                colsInRow = droneCount;
            } else {
                int colsTop = (droneCount + 1) / 2;
                int colsBottom = droneCount / 2;
                colsInRow = (r == 0) ? colsTop : colsBottom;
            }

            double cellHeight = height / rows;
            double cellWidth = width / colsInRow;

            double y1 = originY + r * cellHeight;
            double y2 = y1 + cellHeight;

            for (int c = 0; c < colsInRow; c++) {
                double x1 = originX + c * cellWidth;
                double x2 = x1 + cellWidth;

                String zoneId = "ZONE-" + (zoneIdx + 1);
                String zoneName = "Sector " + (char) ('A' + zoneIdx);
                zones.add(new SearchZone(zoneId, zoneName, x1, y1, x2, y2));
                zoneIdx++;
            }
        }
        return zones;
    }

    /**
     * Maps available drones to partitioned zones based on initial proximity.
     */
    public Map<String, SearchZone> assignDronesToZones(List<Drone> drones, List<SearchZone> zones) {
        Map<String, SearchZone> mapping = new HashMap<>();
        int count = Math.min(drones.size(), zones.size());
        for (int i = 0; i < count; i++) {
            Drone drone = drones.get(i);
            SearchZone zone = zones.get(i);
            drone.setAssignedZoneId(zone.getId());
            mapping.put(drone.getId(), zone);
        }
        return mapping;
    }
}
