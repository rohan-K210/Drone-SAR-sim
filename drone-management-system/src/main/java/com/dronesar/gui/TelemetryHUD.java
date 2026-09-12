package com.dronesar.gui;

import com.dronesar.controller.SimulationController;
import com.dronesar.event.SimulationEventBus;
import com.dronesar.event.SimulationEventListener;
import com.dronesar.model.DetectionEvent;
import com.dronesar.model.Drone;
import com.dronesar.model.Position;
import com.dronesar.model.enums.DroneState;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Responsibility: Dashboard Telemetry HUD
 */
public class TelemetryHUD extends VBox implements SimulationEventListener {
    private final SimulationController controller;

    private final Label timerLabel = new Label("00:00:00");
    private final Label fleetLabel = new Label("0 ACTIVE");
    private final Label coverageLabel = new Label("0.0% SWEPT");
    private final Label targetsLabel = new Label("0 / 0 FOUND");
    private final Label meshStatusLabel = new Label("MESH CONNECTED");
    private final Label alertTickerLabel = new Label("PATROL IN PROGRESS - AWAITING SENSOR PINGS");

    public TelemetryHUD(SimulationController controller) {
        this.controller = controller;

        setSpacing(6);
        setPadding(new Insets(10, 16, 10, 16));
        getStyleClass().add("telemetry-hud");

        HBox metricsRow = new HBox(20);
        metricsRow.setAlignment(Pos.CENTER_LEFT);

        metricsRow.getChildren().addAll(
                createMetricCard("MISSION ELAPSED", timerLabel, "metric-cyan"),
                createMetricCard("FLEET UNITS", fleetLabel, "metric-emerald"),
                createMetricCard("FOREST COVERAGE", coverageLabel, "metric-emerald"),
                createMetricCard("LOCATED PERSONS", targetsLabel, "metric-rose"),
                createMetricCard("DYNAMIC ROUTING", meshStatusLabel, "metric-sky")
        );

        HBox tickerRow = new HBox(8);
        tickerRow.setAlignment(Pos.CENTER_LEFT);
        Label tickerIcon = new Label("🔔 COMMS LINK:");
        tickerIcon.getStyleClass().add("ticker-title");
        alertTickerLabel.getStyleClass().add("ticker-text");
        tickerRow.getChildren().addAll(tickerIcon, alertTickerLabel);

        getChildren().addAll(metricsRow, tickerRow);

        SimulationEventBus.getInstance().registerListener(this);
    }

    private VBox createMetricCard(String title, Label valueLabel, String valueStyleClass) {
        VBox card = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");

        valueLabel.getStyleClass().addAll("metric-value", valueStyleClass);
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    public void updateMetrics() {
        long millis = controller.getElapsedSimulationMillis();
        long secs = (millis / 1000) % 60;
        long mins = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60));
        timerLabel.setText(String.format("%02d:%02d:%02d", hours, mins, secs));

        int activeDrones = 0;
        int offlineDrones = 0;
        for (Drone d : controller.getDrones()) {
            if (!d.isOperational()) offlineDrones++;
            else activeDrones++;
        }
        fleetLabel.setText(String.format("%d ONLINE%s", activeDrones, (offlineDrones > 0 ? (" (" + offlineDrones + " FAULT)") : "")));

        double cov = controller.getOverallCoveragePercentage();
        coverageLabel.setText(String.format("%.1f%% SWEPT", cov));

        long foundCount = controller.getTargets().stream().filter(com.dronesar.model.PersonNode::isDetected).count();
        targetsLabel.setText(String.format("%d / %d FOUND", foundCount, controller.getTargets().size()));

        if (offlineDrones > 0) {
            meshStatusLabel.setText("DYNAMIC RE-ROUTE [ACTIVE]");
        } else {
            meshStatusLabel.setText("DIJKSTRA FULL MESH");
        }
    }

    @Override
    public void onDroneMoved(String droneId, Position newPos, double heading) {}

    @Override
    public void onPersonDetected(DetectionEvent event) {
        Platform.runLater(() -> {
            alertTickerLabel.setText(String.format("🚨 DETECTED: %s (%s) by %s | Relaying via %s",
                    event.getPersonId(), event.getPersonName(), event.getOriginDroneId(), event.getRouteString()));
        });
    }

    @Override
    public void onZoneCoverageUpdated(String zoneId, double coveragePercentage) {}

    @Override
    public void onDroneStatusChanged(String droneId, DroneState newStatus) {
        Platform.runLater(() -> {
            if (newStatus == DroneState.OFFLINE) {
                alertTickerLabel.setText(String.format("⚠️ WARNING: Node %s FAULT! Mesh recalculating optimal path...", droneId));
            }
        });
    }

    @Override
    public void onMultiHopRouteCalculated(String eventId, List<String> routeNodeIds) {
        Platform.runLater(() -> {
            alertTickerLabel.setText(String.format("📡 MULTI-HOP RELAY ACTIVE: %s (Total Hops: %d)",
                    String.join(" ➔ ", routeNodeIds), routeNodeIds.size() - 1));
        });
    }

    @Override
    public void onPacketDeliveredToTower(DetectionEvent event) {
        Platform.runLater(() -> {
            alertTickerLabel.setText(String.format("✅ DISPATCHED: %s arrived at Base Tower (%d hops) ➔ Ground Team Alerted!",
                    event.getPersonId(), event.getHopCount()));
        });
    }

    @Override
    public void onRouteRecalculated(String eventId, List<String> newRouteNodeIds) {
        Platform.runLater(() -> {
            alertTickerLabel.setText(String.format("🔄 RE-ROUTE COMPLETE: Bypassed failed node ➔ %s",
                    String.join(" ➔ ", newRouteNodeIds)));
        });
    }
}
