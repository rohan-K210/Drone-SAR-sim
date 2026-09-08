package com.dronesar.gui;

import com.dronesar.controller.SimulationController;
import com.dronesar.model.*;
import com.dronesar.network.MeshNetworkGraph;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Responsibility: Forest canvas, Drone icons, Person icons, Tower,
 * Search zones, Communication links, and Route animations.
 */
public class ForestView extends Canvas {
    private final SimulationController controller;

    private boolean showMeshLinks = true;
    private boolean showRadioRanges = true;
    private boolean showTrails = true;
    private boolean showCoverageHeatmap = true;
    private boolean showSensorCones = true;

    private double mouseX = -100;
    private double mouseY = -100;
    private boolean mouseInside = false;
    private double rotorAngle = 0;

    public ForestView(SimulationController controller, double width, double height) {
        super(width, height);
        this.controller = controller;

        initInteractions();
    }

    private void initInteractions() {
        setOnMouseMoved(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
            mouseInside = true;
        });

        setOnMouseExited(e -> mouseInside = false);

        setOnMouseClicked(this::handleCanvasClick);
    }

    private void handleCanvasClick(MouseEvent e) {
        Position clickPos = new Position(e.getX(), e.getY());
        for (Drone drone : controller.getDrones()) {
            if (drone.getPosition().distanceTo(clickPos) <= 24.0) {
                controller.toggleDroneFailure(drone.getId());
                return;
            }
        }
    }

    public void render() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        rotorAngle = (rotorAngle + 24) % 360;

        drawForestTerrain(gc, w, h);
        drawSearchZones(gc);

        if (showRadioRanges) {
            drawTransmissionRanges(gc);
        }

        if (showMeshLinks) {
            drawCommunicationLinks(gc);
        }

        drawCentralTower(gc);

        if (showTrails) {
            drawDroneTrails(gc);
        }

        drawPersonIcons(gc);
        drawDroneIcons(gc);
        drawRoutePacketAnimation(gc);
        drawTacticalOverlay(gc, w, h);
    }

    private void drawForestTerrain(GraphicsContext gc, double w, double h) {
        gc.setFill(Color.web("#060e0a"));
        gc.fillRect(0, 0, w, h);

        gc.setStroke(Color.web("#0d2417"));
        gc.setLineWidth(1.0);
        for (double x = 0; x < w; x += 40) {
            gc.strokeLine(x, 0, x, h);
        }
        for (double y = 0; y < h; y += 40) {
            gc.strokeLine(0, y, w, y);
        }

        gc.setStroke(Color.web("#0f311f"));
        gc.setLineWidth(1.2);
        gc.strokeOval(240, 70, 420, 240);
        gc.strokeOval(310, 110, 280, 160);
        gc.strokeOval(580, 290, 260, 190);

        drawCanopyCluster(gc, 200, 120);
        drawCanopyCluster(gc, 480, 170);
        drawCanopyCluster(gc, 680, 220);
        drawCanopyCluster(gc, 300, 390);
        drawCanopyCluster(gc, 590, 440);
    }

    private void drawCanopyCluster(GraphicsContext gc, double cx, double cy) {
        gc.setFill(Color.web("#092214"));
        gc.fillOval(cx - 26, cy - 18, 52, 36);
        gc.fillOval(cx - 14, cy - 24, 40, 32);
        gc.setStroke(Color.web("#123822"));
        gc.setLineWidth(0.8);
        gc.strokeOval(cx - 26, cy - 18, 52, 36);
    }

    private void drawSearchZones(GraphicsContext gc) {
        for (SearchZone zone : controller.getZones()) {
            double zx = zone.getMinX();
            double zy = zone.getMinY();
            double zw = zone.getWidth();
            double zh = zone.getHeight();

            if (showCoverageHeatmap) {
                int cellsX = zone.getGridCellsX();
                int cellsY = zone.getGridCellsY();
                double cellW = zw / cellsX;
                double cellH = zh / cellsY;

                gc.setFill(Color.color(0.06, 0.72, 0.50, 0.16));
                for (int i = 0; i < cellsX; i++) {
                    for (int j = 0; j < cellsY; j++) {
                        if (zone.isCellSwept(i, j)) {
                            gc.fillRect(zx + i * cellW, zy + j * cellH, cellW, cellH);
                        }
                    }
                }
            }

            gc.setStroke(Color.web("#1e3a2b"));
            gc.setLineWidth(1.5);
            gc.strokeRect(zx, zy, zw, zh);

            gc.setFill(Color.web("#4ade80"));
            gc.setFont(Font.font("Menlo", FontWeight.BOLD, 11));
            String label = String.format("%s [%.0f%%]", zone.getName(), zone.getCoveragePercentage());
            gc.fillText(label, zx + 8, zy + 16);

            gc.setFill(Color.web("#94a3b8"));
            gc.setFont(Font.font("Menlo", FontWeight.NORMAL, 9));
            gc.fillText("ASSIGNED: " + zone.getAssignedDroneId(), zx + 8, zy + 28);
        }
    }

    private void drawTransmissionRanges(GraphicsContext gc) {
        gc.setLineWidth(0.8);

        Tower tower = controller.getCentralTower();
        if (tower != null) {
            gc.setStroke(Color.color(0.23, 0.51, 0.96, 0.15));
            gc.strokeOval(
                    tower.getLocation().getX() - tower.getReceptionRadius(),
                    tower.getLocation().getY() - tower.getReceptionRadius(),
                    tower.getReceptionRadius() * 2,
                    tower.getReceptionRadius() * 2
            );
        }

        for (Drone drone : controller.getDrones()) {
            if (!drone.isOperational()) continue;
            gc.setStroke(Color.color(0.06, 0.72, 0.50, 0.09));
            gc.strokeOval(
                    drone.getPosition().getX() - drone.getCommunicationRadius(),
                    drone.getPosition().getY() - drone.getCommunicationRadius(),
                    drone.getCommunicationRadius() * 2,
                    drone.getCommunicationRadius() * 2
            );
        }
    }

    private void drawCommunicationLinks(GraphicsContext gc) {
        MeshNetworkGraph graph = controller.getMeshGraph();
        List<MeshNetworkGraph.LinkSegment> links = graph.getAllActiveLinks();

        gc.setStroke(Color.color(0.22, 0.74, 0.97, 0.25));
        gc.setLineWidth(1.0);
        gc.setLineDashes(4, 4);

        for (MeshNetworkGraph.LinkSegment link : links) {
            Position a = link.getNodeA().getPosition();
            Position b = link.getNodeB().getPosition();
            gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
        }
        gc.setLineDashes(null);

        for (SimulationController.ActiveRelaySession session : controller.getActiveRelaySessions()) {
            List<String> route = session.getRoute();
            if (route.size() >= 2) {
                gc.setStroke(Color.color(0.23, 0.51, 0.96, 0.85));
                gc.setLineWidth(2.6);

                for (int i = 0; i < route.size() - 1; i++) {
                    Position from = controller.getNodeCoordinates(route.get(i));
                    Position to = controller.getNodeCoordinates(route.get(i + 1));
                    if (from != null && to != null) {
                        gc.strokeLine(from.getX(), from.getY(), to.getX(), to.getY());
                    }
                }
            }
        }
    }

    private void drawCentralTower(GraphicsContext gc) {
        Tower tower = controller.getCentralTower();
        if (tower == null) return;
        Position pos = tower.getLocation();

        double pulse = tower.getBeaconPulseRadius();
        gc.setStroke(Color.color(0.23, 0.51, 0.96, Math.max(0.0, 1.0 - (pulse / tower.getReceptionRadius()))));
        gc.setLineWidth(1.4);
        gc.strokeOval(pos.getX() - pulse, pos.getY() - pulse, pulse * 2, pulse * 2);

        gc.setFill(Color.web("#1e293b"));
        gc.setStroke(Color.web("#38bdf8"));
        gc.setLineWidth(2.0);
        gc.fillOval(pos.getX() - 16, pos.getY() - 16, 32, 32);
        gc.strokeOval(pos.getX() - 16, pos.getY() - 16, 32, 32);

        gc.setStroke(Color.web("#f8fafc"));
        gc.setLineWidth(2.2);
        gc.strokeLine(pos.getX(), pos.getY() - 12, pos.getX(), pos.getY() + 10);
        gc.strokeLine(pos.getX() - 8, pos.getY() + 10, pos.getX() + 8, pos.getY() + 10);
        gc.strokeLine(pos.getX() - 5, pos.getY() - 4, pos.getX() + 5, pos.getY() - 4);
        gc.strokeLine(pos.getX() - 8, pos.getY() - 9, pos.getX() + 8, pos.getY() - 9);

        gc.setFill(Color.web("#38bdf8"));
        gc.fillOval(pos.getX() - 3, pos.getY() - 15, 6, 6);

        gc.setFill(Color.web("#e2e8f0"));
        gc.setFont(Font.font("Menlo", FontWeight.BOLD, 10));
        gc.fillText(tower.getName(), pos.getX() - 50, pos.getY() + 28);
        gc.setFill(Color.web("#38bdf8"));
        gc.setFont(Font.font("Menlo", FontWeight.NORMAL, 9));
        gc.fillText(String.format("BASE TOWER [RX: %d]", tower.getReceivedDetections().size()), pos.getX() - 44, pos.getY() + 40);
    }

    private void drawDroneTrails(GraphicsContext gc) {
        for (Drone drone : controller.getDrones()) {
            List<Position> trail = drone.getTrailSnapshot();
            if (trail.size() < 2) continue;

            for (int i = 0; i < trail.size() - 1; i++) {
                double alpha = (double) i / trail.size();
                gc.setStroke(Color.color(0.06, 0.72, 0.50, alpha * 0.45));
                gc.setLineWidth(1.8);
                Position p1 = trail.get(i);
                Position p2 = trail.get(i + 1);
                gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
            }
        }
    }

    private void drawPersonIcons(GraphicsContext gc) {
        for (PersonNode target : controller.getTargets()) {
            Position pos = target.getLocation();

            if (target.isDetected()) {
                double r = target.getPulseRadius();
                double alpha = Math.max(0.0, 1.0 - (r / 38.0));
                gc.setStroke(Color.color(0.94, 0.27, 0.27, alpha));
                gc.setLineWidth(2.0);
                gc.strokeOval(pos.getX() - r, pos.getY() - r, r * 2, r * 2);

                gc.setFill(Color.web("#ef4444"));
                gc.fillOval(pos.getX() - 6, pos.getY() - 6, 12, 12);
                gc.setStroke(Color.web("#ffffff"));
                gc.setLineWidth(1.5);
                gc.strokeOval(pos.getX() - 6, pos.getY() - 6, 12, 12);

                gc.setFill(Color.web("#fca5a5"));
                gc.setFont(Font.font("Menlo", FontWeight.BOLD, 10));
                gc.fillText(String.format("FOUND: %s (%s)", target.getId(), target.getName()), pos.getX() + 10, pos.getY() - 6);
                gc.setFill(Color.web("#cbd5e1"));
                gc.setFont(Font.font("Menlo", FontWeight.NORMAL, 8));
                gc.fillText(String.format("COORDS: %.0f, %.0f", pos.getX(), pos.getY()), pos.getX() + 10, pos.getY() + 6);
            } else {
                gc.setFill(Color.web("#1e4a30"));
                gc.fillOval(pos.getX() - 3, pos.getY() - 3, 6, 6);
            }
        }
    }

    private void drawDroneIcons(GraphicsContext gc) {
        for (Drone drone : controller.getDrones()) {
            Position pos = drone.getPosition();

            if (showSensorCones && drone.isOperational()) {
                double coneRadius = drone.getSensorRadius();
                RadialGradient coneGrad = new RadialGradient(
                        0, 0,
                        pos.getX(), pos.getY(),
                        coneRadius,
                        false,
                        CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.color(0.06, 0.72, 0.50, 0.28)),
                        new Stop(1.0, Color.color(0.06, 0.72, 0.50, 0.0))
                );
                gc.setFill(coneGrad);
                gc.fillOval(pos.getX() - coneRadius, pos.getY() - coneRadius, coneRadius * 2, coneRadius * 2);
            }

            gc.save();
            gc.translate(pos.getX(), pos.getY());
            gc.rotate(drone.getHeading());

            if (!drone.isOperational()) {
                gc.setStroke(Color.web("#ef4444"));
                gc.setLineWidth(3.0);
                gc.strokeLine(-10, -10, 10, 10);
                gc.strokeLine(-10, 10, 10, -10);
            } else {
                gc.setStroke(Color.web("#cbd5e1"));
                gc.setLineWidth(2.0);
                gc.strokeLine(-10, -10, 10, 10);
                gc.strokeLine(-10, 10, 10, -10);

                gc.setFill(Color.color(0.2, 0.8, 0.6, 0.65));
                gc.fillOval(-14, -14, 8, 8);
                gc.fillOval(6, -14, 8, 8);
                gc.fillOval(-14, 6, 8, 8);
                gc.fillOval(6, 6, 8, 8);

                Color bodyColor = Color.web(drone.getState().getHexColor());
                gc.setFill(bodyColor);
                gc.fillOval(-7, -7, 14, 14);
                gc.setStroke(Color.web("#ffffff"));
                gc.setLineWidth(1.2);
                gc.strokeOval(-7, -7, 14, 14);

                gc.setFill(Color.web("#ffffff"));
                gc.fillOval(4, -2, 4, 4);
            }
            gc.restore();

            double tagX = pos.getX() + 14;
            double tagY = pos.getY() - 10;

            gc.setFill(Color.web("#f8fafc"));
            gc.setFont(Font.font("Menlo", FontWeight.BOLD, 9));
            gc.fillText(drone.getCallsign(), tagX, tagY);

            gc.setFill(Color.web(drone.getState().getHexColor()));
            gc.setFont(Font.font("Menlo", FontWeight.NORMAL, 8));
            gc.fillText(drone.getState().getDisplayName(), tagX, tagY + 10);

            gc.setFill(Color.web("#334155"));
            gc.fillRect(tagX, tagY + 13, 30, 4);
            gc.setFill(drone.getBatteryLevel() > 25 ? Color.web("#10b981") : Color.web("#ef4444"));
            gc.fillRect(tagX, tagY + 13, (drone.getBatteryLevel() / 100.0) * 30, 4);
        }
    }

    private void drawRoutePacketAnimation(GraphicsContext gc) {
        for (SimulationController.ActiveRelaySession session : controller.getActiveRelaySessions()) {
            PacketHop hop = session.getCurrentHop();
            if (hop == null) continue;

            Position packetPos = hop.getCurrentPosition();

            RadialGradient packetGlow = new RadialGradient(
                    0, 0,
                    packetPos.getX(), packetPos.getY(),
                    10.0,
                    false,
                    CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#ffffff")),
                    new Stop(0.4, Color.web("#38bdf8")),
                    new Stop(1.0, Color.color(0.22, 0.74, 0.97, 0.0))
            );
            gc.setFill(packetGlow);
            gc.fillOval(packetPos.getX() - 10, packetPos.getY() - 10, 20, 20);

            gc.setFill(Color.web("#ffffff"));
            gc.fillOval(packetPos.getX() - 3, packetPos.getY() - 3, 6, 6);

            gc.setFill(Color.web("#38bdf8"));
            gc.setFont(Font.font("Menlo", FontWeight.BOLD, 8));
            gc.fillText(String.format("RELAY [%s➔%s]", hop.getFromId(), hop.getToId()), packetPos.getX() + 8, packetPos.getY() - 6);
        }
    }

    private void drawTacticalOverlay(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.web("#1e293b"));
        gc.setLineWidth(1.0);
        gc.strokeRect(0, 0, w, h);

        if (mouseInside && mouseX >= 0 && mouseY >= 0) {
            gc.setStroke(Color.color(0.58, 0.64, 0.72, 0.35));
            gc.setLineWidth(0.8);
            gc.setLineDashes(3, 3);
            gc.strokeLine(mouseX, 0, mouseX, h);
            gc.strokeLine(0, mouseY, w, mouseY);
            gc.setLineDashes(null);

            gc.setFill(Color.web("#0f172a"));
            gc.fillRect(mouseX + 10, mouseY + 10, 115, 20);
            gc.setStroke(Color.web("#38bdf8"));
            gc.strokeRect(mouseX + 10, mouseY + 10, 115, 20);

            gc.setFill(Color.web("#38bdf8"));
            gc.setFont(Font.font("Menlo", FontWeight.BOLD, 9));
            gc.fillText(String.format("GPS: %.0f, %.0f", mouseX, mouseY), mouseX + 16, mouseY + 24);
        }

        double cx = w - 45;
        double cy = 40;
        gc.setStroke(Color.web("#475569"));
        gc.setLineWidth(1.0);
        gc.strokeOval(cx - 16, cy - 16, 32, 32);
        gc.setFill(Color.web("#ef4444"));
        gc.fillPolygon(new double[]{cx, cx - 4, cx + 4}, new double[]{cy - 14, cy - 2, cy - 2}, 3);
        gc.setFill(Color.web("#94a3b8"));
        gc.fillPolygon(new double[]{cx, cx - 4, cx + 4}, new double[]{cy + 14, cy + 2, cy + 2}, 3);
        gc.setFont(Font.font("Menlo", FontWeight.BOLD, 8));
        gc.fillText("N", cx - 2.5, cy - 18);

        double sx = w - 130;
        double sy = h - 18;
        gc.setStroke(Color.web("#94a3b8"));
        gc.setLineWidth(1.2);
        gc.strokeLine(sx, sy, sx + 100, sy);
        gc.strokeLine(sx, sy - 4, sx, sy + 4);
        gc.strokeLine(sx + 100, sy - 4, sx + 100, sy + 4);
        gc.setFill(Color.web("#94a3b8"));
        gc.setFont(Font.font("Menlo", FontWeight.NORMAL, 8));
        gc.fillText("100 METERS", sx + 22, sy - 4);
    }

    public void toggleMeshLinks() { showMeshLinks = !showMeshLinks; }
    public void toggleRadioRanges() { showRadioRanges = !showRadioRanges; }
    public void toggleTrails() { showTrails = !showTrails; }
    public void toggleCoverageHeatmap() { showCoverageHeatmap = !showCoverageHeatmap; }
    public void toggleSensorCones() { showSensorCones = !showSensorCones; }

    public boolean isShowMeshLinks() { return showMeshLinks; }
    public boolean isShowRadioRanges() { return showRadioRanges; }
    public boolean isShowTrails() { return showTrails; }
    public boolean isShowCoverageHeatmap() { return showCoverageHeatmap; }
}
