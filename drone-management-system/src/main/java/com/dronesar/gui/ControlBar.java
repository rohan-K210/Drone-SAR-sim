package com.dronesar.gui;

import com.dronesar.controller.SimulationController;
import com.dronesar.model.Drone;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Responsibility: Buttons & Controls
 */
public class ControlBar extends HBox {
    private final SimulationController controller;
    private final ForestView canvas;
    private final StatusPanel statusPanel;

    private final Button playPauseBtn = new Button("▶ START");
    private final Button resetBtn = new Button("↺ RESET");
    private final Slider speedSlider = new Slider(0.5, 4.0, 1.0);
    private final Label speedValLabel = new Label("1.0x");

    private final CheckBox meshLinksCb = new CheckBox("Mesh Links");
    private final CheckBox radioRangesCb = new CheckBox("Radio Radii");
    private final CheckBox sweepTrailsCb = new CheckBox("Trails");
    private final CheckBox heatmapCb = new CheckBox("Coverage Heatmap");

    private final ComboBox<Integer> fleetSizeCombo = new ComboBox<>(FXCollections.observableArrayList(5, 6, 8));
    private final Button injectFaultBtn = new Button("⚡ FAIL NODE");

    public ControlBar(SimulationController controller, ForestView canvas, StatusPanel statusPanel) {
        this.controller = controller;
        this.canvas = canvas;
        this.statusPanel = statusPanel;

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);
        setPadding(new Insets(8, 14, 8, 14));
        getStyleClass().add("control-bar");

        setupControls();
    }

    private void setupControls() {
        playPauseBtn.getStyleClass().addAll("btn", "btn-primary");
        playPauseBtn.setOnAction(e -> {
            controller.togglePlayPause();
            updatePlayBtnText();
        });

        resetBtn.getStyleClass().addAll("btn", "btn-secondary");
        resetBtn.setOnAction(e -> {
            int count = fleetSizeCombo.getValue() != null ? fleetSizeCombo.getValue() : 6;
            controller.initDefaultScenario(count);
            statusPanel.rebuildDroneCards();
            updatePlayBtnText();
        });

        speedSlider.setPrefWidth(90);
        speedSlider.setShowTickMarks(false);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double speed = newVal.doubleValue();
            controller.setSpeedMultiplier(speed);
            speedValLabel.setText(String.format("%.1fx", speed));
        });
        speedValLabel.getStyleClass().add("speed-label");

        HBox speedGroup = new HBox(5, new Label("SPEED:"), speedSlider, speedValLabel);
        speedGroup.setAlignment(Pos.CENTER_LEFT);

        fleetSizeCombo.setValue(6);
        fleetSizeCombo.getStyleClass().add("combo-box-dark");
        fleetSizeCombo.setOnAction(e -> {
            Integer count = fleetSizeCombo.getValue();
            if (count != null) {
                controller.initDefaultScenario(count);
                statusPanel.rebuildDroneCards();
                updatePlayBtnText();
            }
        });
        HBox fleetGroup = new HBox(5, new Label("DRONES:"), fleetSizeCombo);
        fleetGroup.setAlignment(Pos.CENTER_LEFT);

        meshLinksCb.setSelected(canvas.isShowMeshLinks());
        meshLinksCb.setOnAction(e -> canvas.toggleMeshLinks());

        radioRangesCb.setSelected(canvas.isShowRadioRanges());
        radioRangesCb.setOnAction(e -> canvas.toggleRadioRanges());

        sweepTrailsCb.setSelected(canvas.isShowTrails());
        sweepTrailsCb.setOnAction(e -> canvas.toggleTrails());

        heatmapCb.setSelected(canvas.isShowCoverageHeatmap());
        heatmapCb.setOnAction(e -> canvas.toggleCoverageHeatmap());

        injectFaultBtn.getStyleClass().addAll("btn", "btn-danger");
        injectFaultBtn.setTooltip(new Tooltip("Simulate failure of an active drone to demonstrate dynamic Dijkstra re-routing"));
        injectFaultBtn.setOnAction(e -> {
            for (Drone d : controller.getDrones()) {
                if (d.isOperational()) {
                    controller.toggleDroneFailure(d.getId());
                    break;
                }
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                playPauseBtn,
                resetBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                speedGroup,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                fleetGroup,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                meshLinksCb,
                radioRangesCb,
                sweepTrailsCb,
                heatmapCb,
                spacer,
                injectFaultBtn
        );
    }

    public void updatePlayBtnText() {
        if (controller.isRunning()) {
            playPauseBtn.setText("⏸ PAUSE");
            playPauseBtn.getStyleClass().removeAll("btn-primary");
            if (!playPauseBtn.getStyleClass().contains("btn-warning")) {
                playPauseBtn.getStyleClass().add("btn-warning");
            }
        } else {
            playPauseBtn.setText("▶ START");
            playPauseBtn.getStyleClass().removeAll("btn-warning");
            if (!playPauseBtn.getStyleClass().contains("btn-primary")) {
                playPauseBtn.getStyleClass().add("btn-primary");
            }
        }
    }
}
