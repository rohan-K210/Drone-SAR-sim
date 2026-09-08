package com.dronesar.model;

/**
 * Represents a discrete, non-overlapping search sector in the forest.
 */
public class SearchZone {
    private final String id;
    private final String name;
    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;
    private String assignedDroneId;
    private double coveragePercentage;

    private static final int GRID_CELLS_X = 20;
    private static final int GRID_CELLS_Y = 20;
    private final boolean[][] sweptCells;

    public SearchZone(String id, String name, double minX, double minY, double maxX, double maxY) {
        this.id = id;
        this.name = name;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.coveragePercentage = 0.0;
        this.sweptCells = new boolean[GRID_CELLS_X][GRID_CELLS_Y];
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public double getWidth() {
        return maxX - minX;
    }

    public double getHeight() {
        return maxY - minY;
    }

    public Position getCenter() {
        return new Position((minX + maxX) / 2.0, (minY + maxY) / 2.0);
    }

    public String getAssignedDroneId() {
        return assignedDroneId;
    }

    public void setAssignedDroneId(String assignedDroneId) {
        this.assignedDroneId = assignedDroneId;
    }

    public double getCoveragePercentage() {
        return coveragePercentage;
    }

    public boolean contains(Position pos) {
        if (pos == null) return false;
        return pos.getX() >= minX && pos.getX() <= maxX &&
               pos.getY() >= minY && pos.getY() <= maxY;
    }

    public synchronized void recordSweep(Position dronePos, double sensorRadius) {
        if (!contains(dronePos)) return;

        double cellWidth = getWidth() / GRID_CELLS_X;
        double cellHeight = getHeight() / GRID_CELLS_Y;

        for (int i = 0; i < GRID_CELLS_X; i++) {
            double cellCenterX = minX + (i + 0.5) * cellWidth;
            for (int j = 0; j < GRID_CELLS_Y; j++) {
                if (sweptCells[i][j]) continue;
                double cellCenterY = minY + (j + 0.5) * cellHeight;
                Position cellCenter = new Position(cellCenterX, cellCenterY);
                if (dronePos.distanceTo(cellCenter) <= sensorRadius) {
                    sweptCells[i][j] = true;
                }
            }
        }

        int totalSwept = 0;
        for (int i = 0; i < GRID_CELLS_X; i++) {
            for (int j = 0; j < GRID_CELLS_Y; j++) {
                if (sweptCells[i][j]) totalSwept++;
            }
        }
        this.coveragePercentage = (totalSwept * 100.0) / (GRID_CELLS_X * GRID_CELLS_Y);
    }

    public boolean isCellSwept(int i, int j) {
        if (i < 0 || i >= GRID_CELLS_X || j < 0 || j >= GRID_CELLS_Y) return false;
        return sweptCells[i][j];
    }

    public int getGridCellsX() {
        return GRID_CELLS_X;
    }

    public int getGridCellsY() {
        return GRID_CELLS_Y;
    }

    public synchronized void reset() {
        this.coveragePercentage = 0.0;
        for (int i = 0; i < GRID_CELLS_X; i++) {
            for (int j = 0; j < GRID_CELLS_Y; j++) {
                sweptCells[i][j] = false;
            }
        }
    }
}
