package com.dronesar.gui;

import com.dronesar.controller.SimulationController;
import com.dronesar.model.*;
import com.dronesar.model.enums.DroneState;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Tactical Forest & Radar Map Canvas implemented with pure Java Swing and Graphics2D.
 * Handles rendering of terrain grid, search zones, dual base towers, direct comms links,
 * drone icons, flight trails, and survivor radar beacons.
 */
public class ForestCanvas extends JPanel {

    private final SimulationController controller;
    private StatusPanel statusPanel;
    private String selectedDroneId = null;

    public ForestCanvas(SimulationController controller) {
        this.controller = controller;
        setBackground(Theme.BG_CANVAS);
        setPreferredSize(new Dimension(920, 560));
        setDoubleBuffered(true);

        // Allow clicking on any drone to select it
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double mx = e.getX();
                double my = e.getY();
                for (Drone d : controller.getDrones()) {
                    if (d.getPosition().distanceTo(new Position(mx, my)) <= 24.0) {
                        selectedDroneId = d.getId();
                        if (statusPanel != null) {
                            statusPanel.setSelectedDroneInTable(selectedDroneId);
                        }
                        repaint();
                        break;
                    }
                }
            }
        });
    }

    public void setStatusPanel(StatusPanel statusPanel) {
        this.statusPanel = statusPanel;
    }

    public void setSelectedDroneId(String droneId) {
        this.selectedDroneId = droneId;
        repaint();
    }

    public String getSelectedDroneId() {
        return selectedDroneId;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // Enable high-quality anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();

        // 1. Draw Tactical Grid & Coordinate Rulers
        drawGrid(g2, width, height);

        // 2. Draw Search Sectors / Zones
        drawSearchZones(g2);

        // 3. Draw Planned Waypoint Routes
        drawPlannedRoutes(g2);

        // 4. Draw All Base Communication Towers & Reception Ranges
        drawBaseTowers(g2);

        // 5. Draw Direct Wireless Communication Links
        drawDirectCommsLinks(g2);

        // 6. Draw Animated Wireless Transmission Signals (Drone -> Tower)
        drawTransmissionSignals(g2);

        // 7. Draw Missing Persons / Survivors
        drawSurvivors(g2);

        // 8. Draw Drones (Quadcopter Icons, Heading, FOV Sensor Rings)
        drawDrones(g2);

        // 9. Draw HUD Overlay Elements (Compass Rose, Scale Marker)
        drawHUDOverlay(g2, width, height);

        g2.dispose();
    }

    private void drawGrid(Graphics2D g2, int width, int height) {
        // Minor grid lines
        g2.setColor(Theme.GRID_LINE);
        g2.setStroke(new BasicStroke(1.0f));
        int cellSize = 40;
        for (int x = 0; x < width; x += cellSize) {
            g2.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += cellSize) {
            g2.drawLine(0, y, width, y);
        }

        // Major grid lines & coordinates
        g2.setColor(Theme.GRID_ACCENT);
        g2.setStroke(new BasicStroke(1.2f));
        g2.setFont(Theme.FONT_SMALL);
        for (int x = 0; x < width; x += 160) {
            g2.drawLine(x, 0, x, height);
            g2.drawString(x + "m", x + 4, 14);
        }
        for (int y = 0; y < height; y += 160) {
            g2.drawLine(0, y, width, y);
            g2.drawString(y + "m", 4, y - 4);
        }
    }

    private void drawSearchZones(Graphics2D g2) {
        List<SearchZone> zones = controller.getZones();
        if (zones == null) return;

        for (SearchZone zone : zones) {
            double x = zone.getMinX();
            double y = zone.getMinY();
            double w = zone.getMaxX() - zone.getMinX();
            double h = zone.getMaxY() - zone.getMinY();

            // Translucent sector fill
            g2.setColor(new Color(31, 111, 235, 18));
            g2.fill(new Rectangle2D.Double(x, y, w, h));

            // Render swept coverage blocks
            int cellsX = zone.getGridCellsX();
            int cellsY = zone.getGridCellsY();
            double cellW = w / cellsX;
            double cellH = h / cellsY;

            g2.setColor(new Color(63, 185, 80, 45)); // Emerald swept trail
            for (int cx = 0; cx < cellsX; cx++) {
                for (int cy = 0; cy < cellsY; cy++) {
                    if (zone.isCellSwept(cx, cy)) {
                        g2.fill(new Rectangle2D.Double(x + cx * cellW, y + cy * cellH, cellW, cellH));
                    }
                }
            }

            // Sector boundary
            g2.setColor(new Color(56, 139, 253, 110));
            float[] dash = {6.0f, 4.0f};
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2.draw(new Rectangle2D.Double(x, y, w, h));

            // Sector Label & Coverage %
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.setFont(Theme.FONT_MONO_BOLD);
            String label = zone.getName() + " [" + zone.getId() + "] " + String.format("%.0f%%", zone.getCoveragePercentage());
            g2.drawString(label, (float) (x + 8), (float) (y + 18));
        }
    }

    private void drawPlannedRoutes(Graphics2D g2) {
        g2.setColor(new Color(56, 139, 253, 40));
        float[] dash = {3.0f, 3.0f};
        g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));

        for (Drone drone : controller.getDrones()) {
            SearchZone zone = controller.findZoneById(drone.getAssignedZoneId());
            if (zone != null) {
                Position center = zone.getCenter();
                g2.draw(new Line2D.Double(drone.getPosition().getX(), drone.getPosition().getY(), center.getX(), center.getY()));
            }
        }
    }

    private void drawBaseTowers(Graphics2D g2) {
        List<Tower> towers = controller.getTowers();
        if (towers == null) return;

        for (Tower tower : towers) {
            double tx = tower.getLocation().getX();
            double ty = tower.getLocation().getY();
            double radius = tower.getReceptionRadius();

            // 1. Reception coverage boundary
            g2.setColor(new Color(56, 139, 253, 20));
            g2.fill(new Ellipse2D.Double(tx - radius, ty - radius, radius * 2, radius * 2));
            g2.setColor(new Color(56, 139, 253, 85));
            float[] dash = {8.0f, 6.0f};
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2.draw(new Ellipse2D.Double(tx - radius, ty - radius, radius * 2, radius * 2));

            // 2. Radiating beacon pulse
            double pulse = tower.getBeaconPulseRadius();
            float pulseAlpha = (float) Math.max(0.0, 1.0 - (pulse / radius));
            g2.setColor(new Color(88, 166, 255, (int) (pulseAlpha * 120)));
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(new Ellipse2D.Double(tx - pulse, ty - pulse, pulse * 2, pulse * 2));

            // 3. Base Station Tower Mast Icon
            g2.setColor(Theme.BLUE_TOWER);
            g2.setStroke(new BasicStroke(2.2f));

            Polygon mast = new Polygon();
            mast.addPoint((int) tx, (int) (ty - 16));
            mast.addPoint((int) (tx - 12), (int) (ty + 14));
            mast.addPoint((int) (tx + 12), (int) (ty + 14));
            g2.drawPolygon(mast);

            g2.drawLine((int) (tx - 7), (int) (ty + 2), (int) (tx + 7), (int) (ty + 2));
            g2.drawLine((int) (tx - 4), (int) (ty - 6), (int) (tx + 4), (int) (ty - 6));

            // Emitter dish
            g2.setColor(Theme.CYAN_ACCENT);
            g2.fillOval((int) (tx - 4), (int) (ty - 20), 8, 8);

            // Tower Labels
            g2.setFont(Theme.FONT_MONO_BOLD);
            g2.setColor(Theme.TEXT_PRIMARY);
            g2.drawString(tower.getName(), (float) (tx - 45), (float) (ty + 28));
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.GREEN_ONLINE);
            g2.drawString("BASE LINK ONLINE", (float) (tx - 42), (float) (ty + 40));
        }
    }

    private void drawDirectCommsLinks(Graphics2D g2) {
        for (Drone drone : controller.getDrones()) {
            if (!drone.isOperational()) continue;

            Tower connectedTower = controller.getNearestTowerInRange(drone);
            if (connectedTower != null) {
                double dx = drone.getPosition().getX();
                double dy = drone.getPosition().getY();
                double tx = connectedTower.getLocation().getX();
                double ty = connectedTower.getLocation().getY();

                // Draw glowing direct wireless link to the connected tower
                g2.setColor(new Color(0, 229, 255, 75));
                float[] dash = {5.0f, 5.0f};
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                g2.draw(new Line2D.Double(dx, dy, tx, ty));
            }
        }
    }

    private void drawTransmissionSignals(Graphics2D g2) {
        List<TransmissionSignal> signals = controller.getActiveSignals();
        if (signals == null) return;

        for (TransmissionSignal signal : signals) {
            Position current = signal.getCurrentPosition();
            double x = current.getX();
            double y = current.getY();

            // Glow outer halo
            g2.setColor(new Color(0, 229, 255, 95));
            g2.fill(new Ellipse2D.Double(x - 9, y - 9, 18, 18));

            // Core electric transmission packet
            g2.setColor(Theme.CYAN_ACCENT);
            g2.fill(new Ellipse2D.Double(x - 5, y - 5, 10, 10));
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(x - 2, y - 2, 4, 4));

            // Packet telemetry tag
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.CYAN_ACCENT);
            g2.drawString("BEACON ALERT", (float) (x + 8), (float) (y - 4));
        }
    }

    private void drawSurvivors(Graphics2D g2) {
        List<PersonNode> targets = controller.getTargets();
        if (targets == null) return;

        for (PersonNode target : targets) {
            double px = target.getLocation().getX();
            double py = target.getLocation().getY();

            if (target.isDetected()) {
                g2.setColor(new Color(63, 185, 80, 50));
                g2.fill(new Ellipse2D.Double(px - 16, py - 16, 32, 32));

                g2.setColor(Theme.GREEN_ONLINE);
                g2.setStroke(new BasicStroke(2.0f));
                g2.draw(new Ellipse2D.Double(px - 10, py - 10, 20, 20));
                g2.fill(new Ellipse2D.Double(px - 4, py - 4, 8, 8));

                g2.setFont(Theme.FONT_MONO_BOLD);
                g2.setColor(Theme.GREEN_ONLINE);
                g2.drawString("FOUND: " + target.getName(), (float) (px + 12), (float) (py + 4));
            } else {
                double pulse = target.getPulseRadius();
                float alpha = (float) Math.max(0.0, 1.0 - (pulse / 35.0));

                g2.setColor(new Color(248, 81, 73, (int) (alpha * 160)));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Ellipse2D.Double(px - pulse, py - pulse, pulse * 2, pulse * 2));

                g2.setColor(Theme.RED_ALERT);
                g2.fill(new Ellipse2D.Double(px - 4, py - 4, 8, 8));

                g2.setFont(Theme.FONT_SMALL);
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString(target.getId(), (float) (px + 7), (float) (py - 5));
            }
        }
    }

    private void drawDrones(Graphics2D g2) {
        for (Drone drone : controller.getDrones()) {
            double dx = drone.getPosition().getX();
            double dy = drone.getPosition().getY();
            double sensorR = drone.getSensorRadius();
            boolean isOperational = drone.isOperational();
            boolean isSelected = drone.getId().equals(selectedDroneId);

            // Selection Highlight Ring
            if (isSelected) {
                g2.setColor(Theme.CYAN_ACCENT);
                g2.setStroke(new BasicStroke(2.0f));
                g2.draw(new Ellipse2D.Double(dx - 22, dy - 22, 44, 44));
            }

            // 1. Sensor Field of View (FOV) Circle
            if (isOperational) {
                Color fovColor = (drone.getState() == DroneState.PERSON_FOUND)
                        ? new Color(245, 158, 11, 40)
                        : new Color(0, 229, 255, 25);
                g2.setColor(fovColor);
                g2.fill(new Ellipse2D.Double(dx - sensorR, dy - sensorR, sensorR * 2, sensorR * 2));

                g2.setColor(new Color(0, 229, 255, 90));
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new Ellipse2D.Double(dx - sensorR, dy - sensorR, sensorR * 2, sensorR * 2));
            }

            // 2. Drone Quadcopter Vector Icon (Rotated along heading)
            AffineTransform oldTransform = g2.getTransform();
            g2.translate(dx, dy);
            g2.rotate(drone.getHeading());

            Color droneColor;
            if (!isOperational) {
                droneColor = Theme.RED_ALERT;
            } else if (drone.getState() == DroneState.PERSON_FOUND) {
                droneColor = Theme.AMBER_WARN;
            } else if (drone.getState() == DroneState.IDLE) {
                droneColor = Theme.TEXT_MUTED;
            } else {
                droneColor = Theme.GREEN_ONLINE;
            }

            g2.setColor(droneColor);
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(-8, -8, 8, 8);
            g2.drawLine(-8, 8, 8, -8);

            // 4 Quadcopter Rotors
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawOval(-11, -11, 6, 6);
            g2.drawOval(5, -11, 6, 6);
            g2.drawOval(-11, 5, 6, 6);
            g2.drawOval(5, 5, 6, 6);

            // Fuselage
            g2.setColor(Theme.BG_DARK);
            g2.fillOval(-4, -4, 8, 8);
            g2.setColor(droneColor);
            g2.drawOval(-4, -4, 8, 8);

            // Nose
            Polygon nose = new Polygon();
            nose.addPoint(0, -9);
            nose.addPoint(-3, -5);
            nose.addPoint(3, -5);
            g2.fillPolygon(nose);

            g2.setTransform(oldTransform);

            // 3. Battery Mini Bar
            double barWidth = 24.0;
            double barHeight = 4.0;
            double barX = dx - barWidth / 2.0;
            double barY = dy - 18.0;

            g2.setColor(new Color(33, 38, 45, 190));
            g2.fill(new Rectangle2D.Double(barX, barY, barWidth, barHeight));

            double batteryPct = Math.max(0.0, Math.min(1.0, drone.getBatteryLevel() / 100.0));
            Color battColor = (batteryPct > 0.5) ? Theme.GREEN_ONLINE : ((batteryPct > 0.2) ? Theme.AMBER_WARN : Theme.RED_ALERT);
            g2.setColor(battColor);
            g2.fill(new Rectangle2D.Double(barX, barY, barWidth * batteryPct, barHeight));
            g2.setColor(Theme.BORDER_SUBTLE);
            g2.setStroke(new BasicStroke(0.8f));
            g2.draw(new Rectangle2D.Double(barX, barY, barWidth, barHeight));

            // 4. Drone Callsign & Status Tag
            g2.setFont(Theme.FONT_MONO);
            g2.setColor(Theme.TEXT_PRIMARY);
            String tag = drone.getCallsign() + (drone.getState() == DroneState.IDLE ? " [PAUSED]" : "");
            g2.drawString(tag, (float) (dx - 18), (float) (dy + 22));
        }
    }

    private void drawHUDOverlay(Graphics2D g2, int width, int height) {
        // Tactical North Compass
        int cx = width - 40;
        int cy = 40;
        g2.setColor(new Color(22, 27, 34, 180));
        g2.fillOval(cx - 18, cy - 18, 36, 36);
        g2.setColor(Theme.BORDER_SUBTLE);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(cx - 18, cy - 18, 36, 36);

        Polygon needle = new Polygon();
        needle.addPoint(cx, cy - 14);
        needle.addPoint(cx - 4, cy);
        needle.addPoint(cx + 4, cy);
        g2.setColor(Theme.RED_ALERT);
        g2.fillPolygon(needle);

        Polygon southNeedle = new Polygon();
        southNeedle.addPoint(cx, cy + 14);
        southNeedle.addPoint(cx - 4, cy);
        southNeedle.addPoint(cx + 4, cy);
        g2.setColor(Theme.TEXT_MUTED);
        g2.fillPolygon(southNeedle);

        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.drawString("N", cx - 4, cy - 19);

        // Scale marker
        int sx = 20;
        int sy = height - 20;
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawLine(sx, sy, sx + 60, sy);
        g2.drawLine(sx, sy - 4, sx, sy + 4);
        g2.drawLine(sx + 60, sy - 4, sx + 60, sy + 4);
        g2.setFont(Theme.FONT_SMALL);
        g2.drawString("60m", sx + 18, sy - 6);
    }
}
