package com.dronesar.gui;

import com.dronesar.controller.SimulationController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main application window (JFrame) hosting the SAR Tactical Dashboard, Radar Map,
 * Telemetry HUD, and Control Bar (MVC Pattern - Syllabus Module 4).
 */
public class DashboardFrame extends JFrame {

    private final SimulationController controller;
    private final ForestCanvas forestCanvas;
    private final TelemetryHUDPanel hudPanel;
    private final StatusPanel statusPanel;
    private final ControlBarPanel controlBar;
    private final Timer animationTimer;

    public DashboardFrame(SimulationController controller) {
        super("Tactical Drone SAR Operations & Direct Comms Control Center");
        this.controller = controller;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1360, 780);
        setMinimumSize(new Dimension(1100, 650));
        setLocationRelativeTo(null);

        // Dark background for root pane
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());

        // Initialize GUI Sub-Panels
        this.hudPanel = new TelemetryHUDPanel(controller);
        this.forestCanvas = new ForestCanvas(controller);
        this.statusPanel = new StatusPanel(controller);
        this.controlBar = new ControlBarPanel(controller);

        // Place components according to BorderLayout (Syllabus Module 4)
        add(hudPanel, BorderLayout.NORTH);
        add(forestCanvas, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.EAST);
        add(controlBar, BorderLayout.SOUTH);

        // 60 FPS Animation & Simulation Loop Timer (every 16ms)
        this.animationTimer = new Timer(16, e -> {
            controller.tick(0.016);
            forestCanvas.repaint();
            hudPanel.updateMetrics();
            statusPanel.updateTelemetryTable();
            controlBar.updatePlayPauseButton();
        });
        this.animationTimer.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                animationTimer.stop();
                controller.stopSimulation();
            }
        });
    }

    public ForestCanvas getForestCanvas() {
        return forestCanvas;
    }

    public StatusPanel getStatusPanel() {
        return statusPanel;
    }
}
