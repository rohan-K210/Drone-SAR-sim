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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsibility: Status Panels (Drone Fleet Status & Live Detection Log)
 */
public class StatusPanel extends VBox implements SimulationEventListener {
    private final SimulationController controller;

    private final VBox droneListContainer = new VBox(8);
    private final VBox detectionLogContainer = new VBox(6);
    private final Map<String, DroneCard> droneCards = new HashMap<>();

    public StatusPanel(SimulationController controller) {
        this.controller = controller;

        setPrefWidth(330);
        setMinWidth(300);
        setMaxWidth(360);
        setSpacing(12);
        setPadding(new Insets(12));
        getStyleClass().add("status-panel");

        setupUI();
        SimulationEventBus.getInstance().registerListener(this);
    }

    private void setupUI() {
        Label fleetSectionHeader = new Label("🛸 DRONE FLEET STATUS");
        fleetSectionHeader.getStyleClass().add("panel-section-header");

        ScrollPane droneScroll = new ScrollPane(droneListContainer);
        droneScroll.setFitToWidth(true);
        droneScroll.setPrefHeight(270);
        droneScroll.getStyleClass().add("panel-scroll");

        Label logSectionHeader = new Label("📡 DETECTION & RELAY FEED");
        logSectionHeader.getStyleClass().add("panel-section-header");

        ScrollPane logScroll = new ScrollPane(detectionLogContainer);
        logScroll.setFitToWidth(true);
        logScroll.setPrefHeight(260);
        VBox.setVgrow(logScroll, Priority.ALWAYS);
        logScroll.getStyleClass().add("panel-scroll");

        getChildren().addAll(fleetSectionHeader, droneScroll, logSectionHeader, logScroll);
        rebuildDroneCards();
    }

    public void rebuildDroneCards() {
        droneListContainer.getChildren().clear();
        droneCards.clear();

        for (Drone drone : controller.getDrones()) {
            DroneCard card = new DroneCard(drone);
            droneCards.put(drone.getId(), card);
            droneListContainer.getChildren().add(card);
        }
    }

    public void updateStatus() {
        for (Drone drone : controller.getDrones()) {
            DroneCard card = droneCards.get(drone.getId());
            if (card != null) {
                card.refresh();
            }
        }
    }

    @Override
    public void onDroneMoved(String droneId, Position newPos, double heading) {}

    @Override
    public void onPersonDetected(DetectionEvent event) {
        Platform.runLater(() -> {
            addLogEntry(String.format("🚨 %s FOUND by %s\nTarget: %s (%s) @ %s\nRoute: %s (Hops: %d)",
                    event.getPersonId(), event.getOriginDroneId(), event.getPersonName(),
                    event.getPersonId(), event.getCoordinates(), event.getRouteString(), event.getHopCount()), "log-entry-detected");
        });
    }

    @Override
    public void onZoneCoverageUpdated(String zoneId, double coveragePercentage) {}

    @Override
    public void onDroneStatusChanged(String droneId, DroneState newStatus) {
        Platform.runLater(() -> {
            DroneCard card = droneCards.get(droneId);
            if (card != null) {
                card.refresh();
            }
            if (newStatus == DroneState.OFFLINE) {
                addLogEntry("⚠️ FAULT: " + droneId + " lost connection! Mesh rerouting...", "log-entry-warning");
            }
        });
    }

    @Override
    public void onMultiHopRouteCalculated(String eventId, List<String> routeNodeIds) {
        Platform.runLater(() -> {
            addLogEntry("📡 Mesh Relay: " + String.join(" ➔ ", routeNodeIds), "log-entry-info");
        });
    }

    @Override
    public void onPacketDeliveredToTower(DetectionEvent event) {
        Platform.runLater(() -> {
            addLogEntry("✅ Base Tower Received " + event.getPersonId() + " data! Ground Team notified.", "log-entry-success");
        });
    }

    @Override
    public void onRouteRecalculated(String eventId, List<String> newRouteNodeIds) {
        Platform.runLater(() -> {
            addLogEntry("🔄 Dynamic Re-Route: " + String.join(" ➔ ", newRouteNodeIds), "log-entry-reroute");
        });
    }

    private void addLogEntry(String message, String styleClass) {
        VBox entry = new VBox(2);
        entry.getStyleClass().addAll("log-entry", styleClass);

        Label lbl = new Label(message);
        lbl.setWrapText(true);
        lbl.getStyleClass().add("log-text");
        entry.getChildren().add(lbl);

        if (detectionLogContainer.getChildren().size() >= 20) {
            detectionLogContainer.getChildren().remove(detectionLogContainer.getChildren().size() - 1);
        }
        detectionLogContainer.getChildren().add(0, entry);
    }

    private class DroneCard extends VBox {
        private final Drone drone;
        private final Label statusBadge = new Label();
        private final ProgressBar batteryBar = new ProgressBar();
        private final Label batteryPctLabel = new Label();
        private final Button toggleFailBtn = new Button();

        public DroneCard(Drone drone) {
            this.drone = drone;
            setSpacing(4);
            setPadding(new Insets(6, 8, 6, 8));
            getStyleClass().add("drone-card");

            HBox topRow = new HBox(6);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(drone.getCallsign() + " (" + drone.getId() + ")");
            nameLabel.getStyleClass().add("drone-card-title");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            statusBadge.getStyleClass().add("status-badge");
            topRow.getChildren().addAll(nameLabel, spacer, statusBadge);

            HBox batRow = new HBox(6);
            batRow.setAlignment(Pos.CENTER_LEFT);
            Label batTitle = new Label("BAT:");
            batTitle.getStyleClass().add("drone-card-sub");
            batteryBar.setPrefWidth(90);
            batteryBar.setPrefHeight(6);
            batteryPctLabel.getStyleClass().add("drone-card-sub");

            batRow.getChildren().addAll(batTitle, batteryBar, batteryPctLabel);

            HBox bottomRow = new HBox(6);
            bottomRow.setAlignment(Pos.CENTER_LEFT);
            Label sectorLabel = new Label("Zone: " + drone.getAssignedZoneId());
            sectorLabel.getStyleClass().add("drone-card-sub");

            Region spacer2 = new Region();
            HBox.setHgrow(spacer2, Priority.ALWAYS);

            toggleFailBtn.getStyleClass().addAll("btn", "btn-mini");
            toggleFailBtn.setOnAction(e -> controller.toggleDroneFailure(drone.getId()));

            bottomRow.getChildren().addAll(sectorLabel, spacer2, toggleFailBtn);
            getChildren().addAll(topRow, batRow, bottomRow);

            refresh();
        }

        public void refresh() {
            statusBadge.setText(drone.getState().getDisplayName());
            statusBadge.setStyle("-fx-text-fill: " + drone.getState().getHexColor() + "; -fx-border-color: " + drone.getState().getHexColor() + ";");

            double bat = drone.getBatteryLevel() / 100.0;
            batteryBar.setProgress(bat);
            batteryPctLabel.setText(String.format("%.0f%%", drone.getBatteryLevel()));

            if (!drone.isOperational()) {
                toggleFailBtn.setText("↺ REVIVE");
                toggleFailBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white;");
                getStyleClass().removeAll("drone-card-normal");
                if (!getStyleClass().contains("drone-card-failed")) {
                    getStyleClass().add("drone-card-failed");
                }
            } else {
                toggleFailBtn.setText("⚡ KILL");
                toggleFailBtn.setStyle("-fx-background-color: #991b1b; -fx-text-fill: #fecaca;");
                getStyleClass().removeAll("drone-card-failed");
                if (!getStyleClass().contains("drone-card-normal")) {
                    getStyleClass().add("drone-card-normal");
                }
            }
        }
    }
}
