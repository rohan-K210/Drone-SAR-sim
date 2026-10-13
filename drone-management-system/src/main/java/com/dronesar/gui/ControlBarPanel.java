package com.dronesar.gui;

import com.dronesar.controller.SimulationController;
import com.dronesar.event.SimulationEventBus;
import com.dronesar.event.SimulationEventListener;
import com.dronesar.model.DetectionEvent;
import com.dronesar.model.Position;
import com.dronesar.model.enums.DroneState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Enhanced Control Bar Panel at the bottom of the screen.
 * Features:
 *  1. Live Survivor Geolocation & Detection Alert Bar
 *  2. Tactical Simulation Controls (Start, Step, Reset)
 */
public class ControlBarPanel extends JPanel implements SimulationEventListener {

    private final SimulationController controller;
    private final JButton btnPlayPause;
    private final JButton btnStep;
    private final JButton btnReset;

    // Real-time Geolocation Alert components
    private final JLabel lblGeolocationBanner;

    public ControlBarPanel(SimulationController controller, Runnable returnToBriefingAction) {
        this(controller, returnToBriefingAction, null);
    }

    public ControlBarPanel(SimulationController controller, Runnable returnToBriefingAction, Runnable resetScenarioAction) {
        this.controller = controller;

        setLayout(new BorderLayout(0, 4));
        setBackground(Theme.BG_PANEL);
        setBorder(new MatteBorder(1, 0, 0, 0, Theme.BORDER_SUBTLE));
        setPreferredSize(new Dimension(1280, 84));

        // 1. Top Row: Live Geolocation Alert Bar
        JPanel alertRow = new JPanel(new BorderLayout(10, 0));
        alertRow.setBackground(new Color(16, 21, 28));
        alertRow.setBorder(new EmptyBorder(6, 16, 6, 16));

        JLabel lblFeedTag = new JLabel("INCIDENT FEED:");
        lblFeedTag.setFont(Theme.FONT_HEADING);
        lblFeedTag.setForeground(Theme.CYAN_ACCENT);

        lblGeolocationBanner = new JLabel("SCANNING ACTIVE: Autonomous UAV swarm patrolling Blackwood Forest grid sectors... No casualties located yet.");
        lblGeolocationBanner.setFont(Theme.FONT_BODY);
        lblGeolocationBanner.setForeground(Theme.TEXT_PRIMARY);
        lblGeolocationBanner.setToolTipText("Active autonomous SAR feed");

        alertRow.add(lblFeedTag, BorderLayout.WEST);
        alertRow.add(lblGeolocationBanner, BorderLayout.CENTER);

        // 2. Bottom Row: Simulation Control Buttons
        JPanel controlsRow = new JPanel(new BorderLayout());
        controlsRow.setBackground(Theme.BG_PANEL);
        controlsRow.setBorder(new EmptyBorder(4, 16, 6, 16));

        JPanel buttonsLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttonsLeft.setBackground(Theme.BG_PANEL);

        btnPlayPause = createTacticalButton("Start Sweep", Theme.GREEN_ONLINE);
        btnPlayPause.addActionListener(e -> {
            controller.togglePlayPause();
            updatePlayPauseButton();
        });

        btnStep = createTacticalButton("Step", Theme.TEXT_PRIMARY);
        btnStep.addActionListener(e -> {
            if (!controller.isRunning()) {
                controller.startSimulation();
                controller.tick(0.08);
                controller.stopSimulation();
            }
        });

        btnReset = createTacticalButton("Reset Scenario", Theme.AMBER_WARN);
        btnReset.addActionListener(e -> {
            if (resetScenarioAction != null) {
                resetScenarioAction.run();
            } else {
                controller.stopSimulation();
                controller.initDefaultScenario(4, 3);
                updatePlayPauseButton();
                resetAlertBanner();
            }
        });

        buttonsLeft.add(btnPlayPause);
        buttonsLeft.add(btnStep);
        buttonsLeft.add(btnReset);

        controlsRow.add(buttonsLeft, BorderLayout.WEST);

        add(alertRow, BorderLayout.NORTH);
        add(controlsRow, BorderLayout.SOUTH);

        // Subscribe to simulation event bus for real-time geolocation alerts
        SimulationEventBus.getInstance().registerListener(this);
    }

    private JButton createTacticalButton(String text, Color accentColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean hover = getModel().isRollover();
                boolean pressed = getModel().isPressed();

                Color bg = pressed ? Theme.BG_CARD_HOVER : (hover ? Theme.BG_CARD_HOVER : Theme.BG_CONTROL);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));

                Color borderCol = hover ? accentColor : Theme.BORDER_SUBTLE;
                g2.setColor(borderCol);
                g2.setStroke(new BasicStroke(hover ? 1.5f : 1.0f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 8, 8));

                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(Theme.FONT_HEADING);
        btn.setForeground(accentColor);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(new EmptyBorder(6, 16, 6, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.repaint();
            }
        });

        return btn;
    }

    public void updatePlayPauseButton() {
        if (controller.isRunning()) {
            btnPlayPause.setText("Pause Sweep");
            btnPlayPause.setForeground(Theme.AMBER_WARN);
        } else {
            btnPlayPause.setText("Start Sweep");
            btnPlayPause.setForeground(Theme.GREEN_ONLINE);
        }
    }

    public void resetAlertBanner() {
        lblGeolocationBanner.setText("SCANNING ACTIVE: Autonomous UAV swarm patrolling Blackwood Forest grid sectors... No casualties located yet.");
        lblGeolocationBanner.setForeground(Theme.TEXT_PRIMARY);
    }

    @Override
    public void onPersonDetected(DetectionEvent event) {
        SwingUtilities.invokeLater(() -> {
            Position pos = event.getCoordinates();
            String gpsCoords = pos.toGpsString();
            String foundMsg = String.format("[SURVIVOR FOUND] %s (%s) LOCATED at GPS: %s | Reported by %s to %s | Ground SAR Dispatched!",
                    event.getPersonName(), event.getPersonId(), gpsCoords, event.getOriginDroneId(), event.getTowerId());

            lblGeolocationBanner.setText(foundMsg);
            lblGeolocationBanner.setToolTipText(foundMsg);
            lblGeolocationBanner.setForeground(Theme.GREEN_ONLINE);
        });
    }

    @Override
    public void onDroneMoved(String droneId, Position newPos, double heading) {}

    @Override
    public void onZoneCoverageUpdated(String zoneId, double coveragePercentage) {}

    @Override
    public void onDroneStatusChanged(String droneId, DroneState newStatus) {}

    @Override
    public void onDirectTransmission(String droneId, String towerId, DetectionEvent event) {}

    @Override
    public void onPacketDeliveredToTower(DetectionEvent event) {}
}
