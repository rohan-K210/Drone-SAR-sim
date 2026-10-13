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
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Tactical Forest & Radar Map Canvas implemented with pure Java Swing and Graphics2D.
 * Features:
 *  - High-precision terrain grid & procedural forest canopy
 *  - Dynamic comms topology (Direct Tower links + Multi-Hop Drone Mesh Relays)
 *  - Tactical label de-collision (automatic offset near towers and targets)
 *  - Ground SAR Dispatch vectors with distance & ground ETA tags
 *  - Interactive survivor targeting reticle
 */
public class ForestCanvas extends JPanel {

    private final SimulationController controller;
    private StatusPanel statusPanel;
    private String selectedDroneId = null;
    private String focusedTargetId = null;
    private String expandedEtaTargetId = null;
    private double focusReticlePulse = 0.0;

    // Procedural tree coordinates for consistent aesthetic rendering
    private static final int[][] TREE_CLUSTERS = {
            {160, 90}, {190, 110}, {210, 80}, {340, 140}, {380, 160}, {410, 120},
            {520, 70}, {560, 90}, {590, 60}, {700, 130}, {740, 150}, {780, 110},
            {180, 320}, {210, 350}, {240, 310}, {360, 420}, {400, 450}, {430, 410},
            {540, 340}, {580, 360}, {620, 330}, {720, 440}, {760, 460}, {800, 420},
            {280, 220}, {460, 260}, {640, 240}, {820, 280}
    };

    public ForestCanvas(SimulationController controller) {
        this.controller = controller;
        setBackground(Theme.BG_CANVAS);
        setPreferredSize(new Dimension(920, 560));
        setDoubleBuffered(true);

        // Click handler for Drones, Survivors, and Rescue ETA points
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double mx = e.getX();
                double my = e.getY();
                Position clickPos = new Position(mx, my);

                // 1. Check ETA Point click (toggle expanded card)
                for (PersonNode target : controller.getTargets()) {
                    if (!target.isDetected()) continue;
                    Tower closest = controller.getNearestTower(target.getLocation());
                    if (closest == null) continue;
                    double midX = (closest.getLocation().getX() + target.getLocation().getX()) / 2.0;
                    double midY = (closest.getLocation().getY() + target.getLocation().getY()) / 2.0;
                    if (clickPos.distanceTo(new Position(midX, midY)) <= 22.0) {
                        if (target.getId().equals(expandedEtaTargetId)) {
                            expandedEtaTargetId = null;
                        } else {
                            expandedEtaTargetId = target.getId();
                            focusOnTarget(target.getId());
                        }
                        repaint();
                        return;
                    }
                }

                // 2. Check drone click
                for (Drone d : controller.getDrones()) {
                    if (d.getPosition().distanceTo(clickPos) <= 24.0) {
                        selectedDroneId = d.getId();
                        if (statusPanel != null) {
                            statusPanel.setSelectedDrone(selectedDroneId);
                        }
                        repaint();
                        return;
                    }
                }

                // 3. Check survivor click (only if detected)
                for (PersonNode target : controller.getTargets()) {
                    if (target.isDetected() && target.getLocation().distanceTo(clickPos) <= 24.0) {
                        focusOnTarget(target.getId());
                        expandedEtaTargetId = target.getId();
                        repaint();
                        return;
                    }
                }

                // 4. Click elsewhere dismisses expanded ETA card
                if (expandedEtaTargetId != null) {
                    expandedEtaTargetId = null;
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Position pos = new Position(e.getX(), e.getY());
                boolean hoverInteractive = false;

                for (PersonNode target : controller.getTargets()) {
                    if (!target.isDetected()) continue;
                    Tower closest = controller.getNearestTower(target.getLocation());
                    if (closest != null) {
                        double midX = (closest.getLocation().getX() + target.getLocation().getX()) / 2.0;
                        double midY = (closest.getLocation().getY() + target.getLocation().getY()) / 2.0;
                        if (pos.distanceTo(new Position(midX, midY)) <= 22.0) {
                            hoverInteractive = true;
                            break;
                        }
                    }
                    if (pos.distanceTo(target.getLocation()) <= 24.0) {
                        hoverInteractive = true;
                        break;
                    }
                }

                if (!hoverInteractive) {
                    for (Drone d : controller.getDrones()) {
                        if (pos.distanceTo(d.getPosition()) <= 24.0) {
                            hoverInteractive = true;
                            break;
                        }
                    }
                }

                setCursor(hoverInteractive ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
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

    public void focusOnTarget(String targetId) {
        this.focusedTargetId = targetId;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();

        // 1. Grid & terrain rulers
        drawGrid(g2, width, height);

        // 2. Forest canopy foliage
        drawForestVegetation(g2);

        // 3. Search sectors
        drawSearchZones(g2);

        // 7. Base stations & towers
        drawBaseTowers(g2);

        // 8. Dynamic comms topology (Direct + Mesh relays)
        drawDirectCommsLinks(g2);
        drawMeshRelayLinks(g2);

        // 9. Active transmission signal packets
        drawTransmissionSignals(g2);

        // 10. Ground SAR dispatch route vectors & ETA
        drawGroundDispatchVectors(g2);

        // 11. Missing persons / survivors
        drawSurvivors(g2);

        // 12. Drones (de-collided labels & sensor FOV)
        drawDrones(g2);

        // 13. Interactive target lock reticle (if focused)
        drawTargetFocusReticle(g2);

        // 14. HUD overlays & tactical corner brackets
        drawHUDOverlay(g2, width, height);

        // 15. Large presentation callout for clicked Rescue ETA point
        drawExpandedEtaCard(g2);

        g2.dispose();
    }

    private void drawGrid(Graphics2D g2, int width, int height) {
        g2.setColor(Theme.GRID_LINE);
        g2.setStroke(new BasicStroke(1.0f));
        int cellSize = 40;
        for (int x = 0; x < width; x += cellSize) {
            g2.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += cellSize) {
            g2.drawLine(0, y, width, y);
        }

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

    private void drawForestVegetation(Graphics2D g2) {
        for (int[] cluster : TREE_CLUSTERS) {
            int cx = cluster[0];
            int cy = cluster[1];

            g2.setColor(new Color(18, 38, 26, 45));
            g2.fillOval(cx - 14, cy - 14, 28, 28);

            g2.setColor(new Color(28, 58, 38, 80));
            g2.fillOval(cx - 9, cy - 9, 18, 18);

            g2.setColor(new Color(42, 85, 55, 100));
            g2.drawLine(cx, cy - 8, cx, cy + 8);
            g2.drawLine(cx - 8, cy, cx + 8, cy);
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

            g2.setColor(new Color(31, 111, 235, 14));
            g2.fill(new Rectangle2D.Double(x, y, w, h));

            int cellsX = zone.getGridCellsX();
            int cellsY = zone.getGridCellsY();
            double cellW = w / cellsX;
            double cellH = h / cellsY;

            g2.setColor(new Color(63, 185, 80, 50));
            for (int cx = 0; cx < cellsX; cx++) {
                for (int cy = 0; cy < cellsY; cy++) {
                    if (zone.isCellSwept(cx, cy)) {
                        g2.fill(new Rectangle2D.Double(x + cx * cellW, y + cy * cellH, cellW, cellH));
                    }
                }
            }

            g2.setColor(new Color(56, 139, 253, 110));
            float[] dash = {6.0f, 4.0f};
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2.draw(new Rectangle2D.Double(x, y, w, h));

            g2.setColor(Theme.TEXT_SECONDARY);
            g2.setFont(Theme.FONT_MONO_BOLD);
            String label = zone.getName() + " [" + zone.getId() + "] " + String.format("%.0f%%", zone.getCoveragePercentage());
            g2.drawString(label, (float) (x + 8), (float) (y + 18));
        }
    }



    private void drawBaseTowers(Graphics2D g2) {
        List<Tower> towers = controller.getTowers();
        if (towers == null) return;

        for (Tower tower : towers) {
            double tx = tower.getLocation().getX();
            double ty = tower.getLocation().getY();
            double radius = tower.getReceptionRadius();

            // Coverage ring
            g2.setColor(new Color(56, 139, 253, 18));
            g2.fill(new Ellipse2D.Double(tx - radius, ty - radius, radius * 2, radius * 2));
            g2.setColor(new Color(56, 139, 253, 80));
            float[] dash = {8.0f, 6.0f};
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2.draw(new Ellipse2D.Double(tx - radius, ty - radius, radius * 2, radius * 2));

            // Radiating pulse
            double pulse = tower.getBeaconPulseRadius();
            float pulseAlpha = (float) Math.max(0.0, 1.0 - (pulse / radius));
            g2.setColor(new Color(88, 166, 255, (int) (pulseAlpha * 120)));
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(new Ellipse2D.Double(tx - pulse, ty - pulse, pulse * 2, pulse * 2));

            // Helipad pad
            g2.setColor(new Color(22, 27, 34, 220));
            g2.fillOval((int) (tx - 18), (int) (ty - 18), 36, 36);
            g2.setColor(Theme.BLUE_TOWER);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval((int) (tx - 18), (int) (ty - 18), 36, 36);

            // Mast
            Polygon mast = new Polygon();
            mast.addPoint((int) tx, (int) (ty - 14));
            mast.addPoint((int) (tx - 10), (int) (ty + 12));
            mast.addPoint((int) (tx + 10), (int) (ty + 12));
            g2.drawPolygon(mast);

            // Emitter dish
            g2.setColor(Theme.CYAN_ACCENT);
            g2.fillOval((int) (tx - 4), (int) (ty - 18), 8, 8);

            // Tower Label
            g2.setFont(Theme.FONT_MONO_BOLD);
            g2.setColor(Theme.TEXT_PRIMARY);
            g2.drawString(tower.getName(), (float) (tx - 45), (float) (ty + 30));
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.GREEN_ONLINE);
            g2.drawString("BASE LINK ONLINE", (float) (tx - 42), (float) (ty + 42));
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

                g2.setColor(new Color(0, 229, 255, 75));
                float[] dash = {5.0f, 5.0f};
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                g2.draw(new Line2D.Double(dx, dy, tx, ty));
            }
        }
    }

    private void drawMeshRelayLinks(Graphics2D g2) {
        for (Drone drone : controller.getDrones()) {
            if (!drone.isOperational()) continue;

            // If not connected directly to tower, look for multi-hop mesh relay
            if (!controller.isDroneInTowerRange(drone)) {
                Drone relay = controller.getMeshRelayDrone(drone);
                if (relay != null) {
                    double d1x = drone.getPosition().getX();
                    double d1y = drone.getPosition().getY();
                    double d2x = relay.getPosition().getX();
                    double d2y = relay.getPosition().getY();

                    // Render mesh link in Purple Relay color
                    g2.setColor(Theme.PURPLE_RELAY);
                    float[] meshDash = {4.0f, 4.0f};
                    g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, meshDash, 0.0f));
                    g2.draw(new Line2D.Double(d1x, d1y, d2x, d2y));

                    // Mesh indicator badge at link midpoint
                    double mx = (d1x + d2x) / 2.0;
                    double my = (d1y + d2y) / 2.0;
                    g2.setColor(new Color(163, 113, 247, 45));
                    g2.fillOval((int) (mx - 6), (int) (my - 6), 12, 12);
                    g2.setColor(Theme.PURPLE_RELAY);
                    g2.fillOval((int) (mx - 3), (int) (my - 3), 6, 6);
                }
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

            g2.setColor(new Color(0, 229, 255, 95));
            g2.fill(new Ellipse2D.Double(x - 9, y - 9, 18, 18));

            g2.setColor(Theme.CYAN_ACCENT);
            g2.fill(new Ellipse2D.Double(x - 5, y - 5, 10, 10));
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(x - 2, y - 2, 4, 4));

            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.CYAN_ACCENT);
            g2.drawString("BEACON ALERT", (float) (x + 8), (float) (y - 4));
        }
    }

    private void drawGroundDispatchVectors(Graphics2D g2) {
        for (PersonNode target : controller.getTargets()) {
            if (!target.isDetected()) continue;

            Tower closest = controller.getNearestTower(target.getLocation());
            if (closest == null) continue;

            double tx = closest.getLocation().getX();
            double ty = closest.getLocation().getY();
            double px = target.getLocation().getX();
            double py = target.getLocation().getY();

            // Emergency route dashed vector line
            g2.setColor(new Color(245, 158, 11, 160));
            float[] routeDash = {6.0f, 4.0f};
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, routeDash, 0.0f));
            g2.draw(new Line2D.Double(tx, ty, px, py));

            // Midpoint clickable ETA waypoint point / badge
            double mx = (tx + px) / 2.0;
            double my = (ty + py) / 2.0;

            boolean isExpanded = target.getId().equals(expandedEtaTargetId);

            // Active pulse ring around point
            if (isExpanded) {
                g2.setColor(new Color(245, 158, 11, 70));
                g2.fill(new Ellipse2D.Double(mx - 18, my - 18, 36, 36));
                g2.setColor(Theme.AMBER_WARN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Ellipse2D.Double(mx - 18, my - 18, 36, 36));
            } else {
                g2.setColor(new Color(245, 158, 11, 35));
                g2.fill(new Ellipse2D.Double(mx - 15, my - 15, 30, 30));
            }

            // Waypoint badge surface
            g2.setColor(new Color(13, 17, 23, 240));
            g2.fill(new Ellipse2D.Double(mx - 13, my - 13, 26, 26));

            g2.setColor(isExpanded ? Theme.CYAN_ACCENT : Theme.AMBER_WARN);
            g2.setStroke(new BasicStroke(isExpanded ? 2.0f : 1.5f));
            g2.draw(new Ellipse2D.Double(mx - 13, my - 13, 26, 26));

            // Centered "ETA" text
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(isExpanded ? Theme.CYAN_ACCENT : Theme.AMBER_WARN);
            FontMetrics fm = g2.getFontMetrics();
            int sw = fm.stringWidth("ETA");
            g2.drawString("ETA", (float) (mx - sw / 2.0), (float) (my + fm.getAscent() / 2.0 - 2));
        }
    }

    private void drawExpandedEtaCard(Graphics2D g2) {
        if (expandedEtaTargetId == null) return;

        PersonNode target = null;
        for (PersonNode p : controller.getTargets()) {
            if (p.getId().equals(expandedEtaTargetId) && p.isDetected()) {
                target = p;
                break;
            }
        }
        if (target == null) return;

        Tower closest = controller.getNearestTower(target.getLocation());
        if (closest == null) return;

        double tx = closest.getLocation().getX();
        double ty = closest.getLocation().getY();
        double px = target.getLocation().getX();
        double py = target.getLocation().getY();
        double mx = (tx + px) / 2.0;
        double my = (ty + py) / 2.0;

        double distance = target.getLocation().distanceTo(closest.getLocation());
        double speedMps = (45.0 * 1000.0) / 3600.0;
        int etaSec = (int) Math.max(1, distance / speedMps);
        int etaMin = etaSec / 60;
        int etaRemSec = etaSec % 60;

        int cardW = 380;
        int cardH = 150;

        // Position card near midpoint but keep completely within canvas bounds
        int cardX = (int) Math.max(15, Math.min(getWidth() - cardW - 15, mx + 20));
        int cardY = (int) Math.max(15, Math.min(getHeight() - cardH - 15, my - 60));

        // Connective pointer line from midpoint ETA point to card
        g2.setColor(Theme.AMBER_WARN);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine((int) mx, (int) my, cardX, cardY + cardH / 2);

        // Drop shadow
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fill(new RoundRectangle2D.Double(cardX + 4, cardY + 4, cardW, cardH, 12, 12));

        // Card Surface
        g2.setColor(new Color(16, 22, 31, 250));
        g2.fill(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 12, 12));

        // Outer glow & border
        g2.setColor(Theme.AMBER_WARN);
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 12, 12));

        // Header strip
        g2.setColor(new Color(245, 158, 11, 40));
        g2.fill(new RoundRectangle2D.Double(cardX + 2, cardY + 2, cardW - 4, 30, 10, 10));

        // Header Title
        g2.setFont(Theme.FONT_HEADING);
        g2.setColor(Theme.AMBER_WARN);
        g2.drawString("RESCUE DISPATCH TELEMETRY", cardX + 14, cardY + 21);

        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString("[CLOSE X]", cardX + cardW - 85, cardY + 21);

        // Big ETA Value
        g2.setFont(Theme.FONT_SUBHERO);
        g2.setColor(Theme.TEXT_PRIMARY);
        String etaStr = String.format("ETA: %02dm %02ds", etaMin, etaRemSec);
        g2.drawString(etaStr, cardX + 14, cardY + 60);

        g2.setFont(Theme.FONT_BODY);
        g2.setColor(Theme.CYAN_ACCENT);
        g2.drawString(String.format("Distance: %.0f m", distance), cardX + 225, cardY + 58);

        // Survivor & Base details
        g2.setFont(Theme.FONT_BODY);
        g2.setColor(Theme.GREEN_ONLINE);
        g2.drawString("Survivor: " + target.getName() + " (" + target.getId() + ")", cardX + 14, cardY + 86);

        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.drawString("Base Origin: " + closest.getName() + " [" + closest.getId() + "]", cardX + 14, cardY + 108);

        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString("Speed: 45 km/h Ground SAR unit • Click to dismiss", cardX + 14, cardY + 128);
    }

    private void drawSurvivors(Graphics2D g2) {
        List<PersonNode> targets = controller.getTargets();
        if (targets == null) return;

        for (PersonNode target : targets) {
            double px = target.getLocation().getX();
            double py = target.getLocation().getY();

            if (target.isDetected()) {
                // Located halo
                g2.setColor(new Color(63, 185, 80, 50));
                g2.fill(new Ellipse2D.Double(px - 16, py - 16, 32, 32));

                g2.setColor(Theme.GREEN_ONLINE);
                g2.setStroke(new BasicStroke(2.0f));
                g2.draw(new Ellipse2D.Double(px - 10, py - 10, 20, 20));
                g2.fill(new Ellipse2D.Double(px - 4, py - 4, 8, 8));

                g2.setFont(Theme.FONT_MONO_BOLD);
                g2.setColor(Theme.GREEN_ONLINE);
                g2.drawString("LOCATED: " + target.getName(), (float) (px + 12), (float) (py + 2));
                g2.setFont(Theme.FONT_SMALL);
                g2.setColor(Theme.CYAN_ACCENT);
                g2.drawString(target.getLocation().toGpsString(), (float) (px + 12), (float) (py + 14));
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
                g2.draw(new Ellipse2D.Double(dx - 24, dy - 24, 48, 48));
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
            g2.setStroke(new BasicStroke(2.4f));
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

            // Directional Nose
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

            // 4. Tactical Label with De-collision Offset
            Point labelPos = calculateLabelOffset(drone, dx, dy);
            g2.setFont(Theme.FONT_MONO);
            g2.setColor(Theme.TEXT_PRIMARY);
            String tag = drone.getCallsign();
            g2.drawString(tag, (float) labelPos.x, (float) labelPos.y);
        }
    }

    /**
     * De-collision logic: checks proximity to Towers and Survivors.
     * If too close, offsets label upward or draws a small leader indicator.
     */
    private Point calculateLabelOffset(Drone drone, double dx, double dy) {
        if (drone.getState() == DroneState.IDLE) {
            Tower tower = controller.getNearestTower(new Position(dx, dy));
            if (tower != null) {
                double vx = dx - tower.getLocation().getX();
                double vy = dy - tower.getLocation().getY();
                double dist = Math.max(1.0, Math.sqrt(vx * vx + vy * vy));
                return new Point((int) (dx + (vx / dist) * 18 - 14), (int) (dy + (vy / dist) * 18 + 4));
            }
        }

        boolean collisionDetected = false;

        // Check proximity to base towers
        for (Tower tower : controller.getTowers()) {
            if (tower.getLocation().distanceTo(new Position(dx, dy)) < 55.0) {
                collisionDetected = true;
                break;
            }
        }

        // Check proximity to detected survivors
        if (!collisionDetected) {
            for (PersonNode target : controller.getTargets()) {
                if (target.isDetected() && target.getLocation().distanceTo(new Position(dx, dy)) < 45.0) {
                    collisionDetected = true;
                    break;
                }
            }
        }

        if (collisionDetected) {
            // Offset label above the drone to prevent overlapping
            return new Point((int) (dx - 20), (int) (dy - 24));
        } else {
            // Standard placement below drone
            return new Point((int) (dx - 18), (int) (dy + 22));
        }
    }

    private void drawTargetFocusReticle(Graphics2D g2) {
        if (focusedTargetId == null) return;

        PersonNode target = null;
        for (PersonNode p : controller.getTargets()) {
            if (p.getId().equals(focusedTargetId)) {
                target = p;
                break;
            }
        }
        if (target == null) return;

        double tx = target.getLocation().getX();
        double ty = target.getLocation().getY();

        focusReticlePulse = (focusReticlePulse + 0.05) % (Math.PI * 2);
        double r = 26.0 + Math.sin(focusReticlePulse) * 4.0;

        // Animated lock brackets
        g2.setColor(Theme.GOLD_ACCENT);
        g2.setStroke(new BasicStroke(1.8f));

        // Corner reticle brackets
        int len = 8;
        // Top-left
        g2.drawLine((int) (tx - r), (int) (ty - r), (int) (tx - r + len), (int) (ty - r));
        g2.drawLine((int) (tx - r), (int) (ty - r), (int) (tx - r), (int) (ty - r + len));
        // Top-right
        g2.drawLine((int) (tx + r), (int) (ty - r), (int) (tx + r - len), (int) (ty - r));
        g2.drawLine((int) (tx + r), (int) (ty - r), (int) (tx + r), (int) (ty - r + len));
        // Bottom-left
        g2.drawLine((int) (tx - r), (int) (ty + r), (int) (tx - r + len), (int) (ty + r));
        g2.drawLine((int) (tx - r), (int) (ty + r), (int) (tx - r), (int) (ty + r - len));
        // Bottom-right
        g2.drawLine((int) (tx + r), (int) (ty + r), (int) (tx + r - len), (int) (ty + r));
        g2.drawLine((int) (tx + r), (int) (ty + r), (int) (tx + r), (int) (ty + r - len));

        // Center crosshair
        g2.setColor(new Color(251, 191, 36, 150));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawLine((int) (tx - r - 4), (int) ty, (int) (tx + r + 4), (int) ty);
        g2.drawLine((int) tx, (int) (ty - r - 4), (int) tx, (int) (ty + r + 4));

        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.GOLD_ACCENT);
        g2.drawString("TARGET LOCK", (float) (tx - 34), (float) (ty - r - 6));
    }

    private void drawHUDOverlay(Graphics2D g2, int width, int height) {
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

        // Tactical Corner Bracket Reticles
        g2.setColor(new Color(0, 229, 255, 60));
        g2.setStroke(new BasicStroke(1.5f));
        int bSize = 14;
        g2.drawLine(10, 10, 10 + bSize, 10);
        g2.drawLine(10, 10, 10, 10 + bSize);
        g2.drawLine(width - 10, 10, width - 10 - bSize, 10);
        g2.drawLine(width - 10, 10, width - 10, 10 + bSize);
        g2.drawLine(10, height - 10, 10 + bSize, height - 10);
        g2.drawLine(10, height - 10, 10, height - 10 - bSize);
        g2.drawLine(width - 10, height - 10, width - 10 - bSize, height - 10);
        g2.drawLine(width - 10, height - 10, width - 10, height - 10 - bSize);

        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(new Color(0, 229, 255, 120));
        g2.drawString("TACTICAL RADAR 2D • DYNAMIC COMMS MESH ACTIVE", 30, height - 22);
    }
}
