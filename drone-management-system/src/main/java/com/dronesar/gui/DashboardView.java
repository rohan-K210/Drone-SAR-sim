package com.dronesar.gui;

import com.dronesar.controller.SimulationController;
import javafx.animation.AnimationTimer;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

/**
 * Responsibility: Master Dashboard coordinating all GUI components:
 * - ForestView (Forest Canvas & search zones, drone, person, tower icons)
 * - TelemetryHUD (Dashboard metrics)
 * - StatusPanel (Drone fleet status & detection log feed)
 * - ControlBar (Buttons, sliders, and layer toggles)
 */
public class DashboardView extends BorderPane {
    private static final double DEFAULT_CANVAS_WIDTH = 920.0;
    private static final double DEFAULT_CANVAS_HEIGHT = 560.0;

    private final SimulationController controller;
    private final ForestView forestView;
    private final TelemetryHUD hud;
    private final StatusPanel statusPanel;
    private final ControlBar controlBar;

    private AnimationTimer gameLoop;

    public DashboardView() {
        this(new SimulationController(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT));
    }

    public DashboardView(SimulationController controller) {
        this.controller = controller;

        this.forestView = new ForestView(controller, DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT);
        this.hud = new TelemetryHUD(controller);
        this.statusPanel = new StatusPanel(controller);
        this.controlBar = new ControlBar(controller, forestView, statusPanel);

        setTop(hud);
        setCenter(new StackPane(forestView));
        setRight(statusPanel);
        setBottom(controlBar);

        initAnimationLoop();
    }

    private void initAnimationLoop() {
        gameLoop = new AnimationTimer() {
            private long lastTimestamp = 0;

            @Override
            public void handle(long now) {
                if (lastTimestamp == 0) {
                    lastTimestamp = now;
                    return;
                }

                double deltaSeconds = (now - lastTimestamp) / 1_000_000_000.0;
                lastTimestamp = now;
                deltaSeconds = Math.min(deltaSeconds, 0.05);

                controller.tick(deltaSeconds);
                hud.updateMetrics();
                statusPanel.updateStatus();
                forestView.render();
            }
        };
        gameLoop.start();
    }

    public void stop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }

    public SimulationController getController() {
        return controller;
    }

    public ForestView getForestView() {
        return forestView;
    }

    public StatusPanel getStatusPanel() {
        return statusPanel;
    }

    public ControlBar getControlBar() {
        return controlBar;
    }

    public TelemetryHUD getHud() {
        return hud;
    }
}
