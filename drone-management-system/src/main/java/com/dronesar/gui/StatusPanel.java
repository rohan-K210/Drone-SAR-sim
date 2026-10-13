package com.dronesar.gui;

import com.dronesar.controller.SimulationController;
import com.dronesar.event.SimulationEventBus;
import com.dronesar.event.SimulationEventListener;
import com.dronesar.model.DetectionEvent;
import com.dronesar.model.Drone;
import com.dronesar.model.Position;
import com.dronesar.model.Tower;
import com.dronesar.model.enums.DroneState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Modernized Right Sidebar Panel:
 *  - Custom-rendered mini telemetry cards for each UAV.
 *  - Displays Callsign, State Chip, Graphical Battery Bar, and Comms Link.
 *  - Sleek modern scrollbar matching the dark cyber UI.
 *  - Tactical UAV command controls.
 */
public class StatusPanel extends JPanel implements SimulationEventListener {

    private final SimulationController controller;
    private ForestCanvas forestCanvas;

    // UAV Telemetry Cards Container
    private final JPanel droneCardsContainer;
    private final List<UavCardView> uavCardViews = new ArrayList<>();
    private String selectedDroneId = null;

    public StatusPanel(SimulationController controller) {
        this.controller = controller;

        setLayout(new BorderLayout(0, 6));
        setBackground(Theme.BG_PANEL);
        setPreferredSize(new Dimension(305, 560));
        setBorder(new MatteBorder(0, 1, 0, 0, Theme.BORDER_SUBTLE));

        // 1. Top Section: Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.BG_PANEL);
        headerPanel.setBorder(new EmptyBorder(12, 14, 8, 14));

        JLabel titleLabel = new JLabel("SWARM TELEMETRY");
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.CYAN_ACCENT);

        JLabel subCount = new JLabel(controller.getDrones().size() + " UAVs");
        subCount.setFont(Theme.FONT_BODY);
        subCount.setForeground(Theme.TEXT_SECONDARY);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(subCount, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Center Section: Scrollable UAV Cards Container
        droneCardsContainer = new JPanel();
        droneCardsContainer.setLayout(new BoxLayout(droneCardsContainer, BoxLayout.Y_AXIS));
        droneCardsContainer.setBackground(Theme.BG_DARK);
        droneCardsContainer.setBorder(new EmptyBorder(6, 8, 6, 8));

        JScrollPane cardsScroll = new JScrollPane(droneCardsContainer);
        cardsScroll.setBorder(new MatteBorder(1, 0, 1, 0, Theme.BORDER_SUBTLE));
        cardsScroll.getViewport().setBackground(Theme.BG_DARK);
        cardsScroll.getVerticalScrollBar().setUnitIncrement(12);

        Theme.applyModernScrollBar(cardsScroll);

        add(cardsScroll, BorderLayout.CENTER);

        // 3. Bottom Section: 4 Tactical Command Buttons
        JPanel droneControlBox = new JPanel(new GridLayout(2, 2, 6, 6));
        droneControlBox.setBackground(Theme.BG_PANEL);
        droneControlBox.setBorder(new EmptyBorder(8, 12, 10, 12));

        JButton btnPauseResume = createCommandButton("Pause / Resume", Theme.CYAN_ACCENT);
        btnPauseResume.addActionListener(e -> {
            if (selectedDroneId != null) {
                controller.toggleDronePause(selectedDroneId);
            } else {
                showSelectDroneAlert();
            }
        });

        JButton btnResumeSearch = createCommandButton("Sweep", Theme.GREEN_ONLINE);
        btnResumeSearch.addActionListener(e -> {
            if (selectedDroneId != null) {
                controller.resumeDroneSearch(selectedDroneId);
            } else {
                showSelectDroneAlert();
            }
        });

        JButton btnRecall = createCommandButton("Recall", Theme.BLUE_TOWER);
        btnRecall.addActionListener(e -> {
            if (selectedDroneId != null) {
                controller.recallDroneToBase(selectedDroneId);
            } else {
                showSelectDroneAlert();
            }
        });

        JButton btnToggleFail = createCommandButton("Fail UAV", Theme.AMBER_WARN);
        btnToggleFail.addActionListener(e -> {
            if (selectedDroneId != null) {
                controller.toggleDroneFailure(selectedDroneId);
            } else {
                showSelectDroneAlert();
            }
        });

        droneControlBox.add(btnPauseResume);
        droneControlBox.add(btnResumeSearch);
        droneControlBox.add(btnRecall);
        droneControlBox.add(btnToggleFail);

        add(droneControlBox, BorderLayout.SOUTH);

        // Register to event bus
        SimulationEventBus.getInstance().registerListener(this);
        initDroneCards();
    }

    public void setForestCanvas(ForestCanvas canvas) {
        this.forestCanvas = canvas;
    }

    public void setSelectedDrone(String droneId) {
        this.selectedDroneId = droneId;
        for (UavCardView card : uavCardViews) {
            card.setSelected(card.droneId.equals(droneId));
        }
        repaint();
    }

    private void showSelectDroneAlert() {
        JOptionPane.showMessageDialog(this, "Please select a UAV card or click a drone on the radar map.", "Target Selection Required", JOptionPane.INFORMATION_MESSAGE);
    }

    private JButton createCommandButton(String text, Color accent) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_HEADING);
        btn.setBackground(Theme.BG_CONTROL);
        btn.setForeground(accent);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                new EmptyBorder(7, 10, 7, 10)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void initDroneCards() {
        droneCardsContainer.removeAll();
        uavCardViews.clear();

        for (Drone d : controller.getDrones()) {
            UavCardView card = new UavCardView(d);
            uavCardViews.add(card);
            droneCardsContainer.add(card);
            droneCardsContainer.add(Box.createVerticalStrut(6));
        }
        droneCardsContainer.revalidate();
        droneCardsContainer.repaint();
    }

    public void updateTelemetryTable() {
        if (uavCardViews.size() != controller.getDrones().size()) {
            initDroneCards();
        }

        // Update each card's live telemetry
        for (UavCardView card : uavCardViews) {
            Drone d = controller.getDrone(card.droneId);
            if (d != null) {
                card.updateTelemetry(d);
            }
        }
    }

    public void resetSurvivorsTable() {
        initDroneCards();
    }

    @Override
    public void onPersonDetected(DetectionEvent event) {}

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



    /**
     * Custom-rendered mini telemetry card widget for an individual UAV.
     */
    private class UavCardView extends JPanel {
        private final String droneId;
        private boolean isCardSelected = false;

        private final JLabel lblCallsign;
        private final JLabel lblStateBadge;
        private final JLabel lblBatteryPct;
        private final JProgressBar batteryBar;
        private final JLabel lblCommsLink;

        public UavCardView(Drone drone) {
            this.droneId = drone.getId();

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Theme.BG_CARD);
            setBorder(new EmptyBorder(8, 12, 8, 12));
            setMaximumSize(new Dimension(285, 84));
            setPreferredSize(new Dimension(280, 80));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Row 1: Callsign & State Badge
            JPanel row1 = new JPanel(new BorderLayout());
            row1.setBackground(Theme.BG_CARD);

            lblCallsign = new JLabel(drone.getCallsign() + " [" + drone.getId() + "]");
            lblCallsign.setFont(Theme.FONT_HEADING);
            lblCallsign.setForeground(Theme.TEXT_PRIMARY);

            lblStateBadge = new JLabel(drone.getState().name());
            lblStateBadge.setFont(new Font("SansSerif", Font.BOLD, 12));
            updateStateBadge(drone.getState());

            row1.add(lblCallsign, BorderLayout.WEST);
            row1.add(lblStateBadge, BorderLayout.EAST);

            // Row 2: Battery Graphical Bar & %
            JPanel row2 = new JPanel(new BorderLayout(8, 0));
            row2.setBackground(Theme.BG_CARD);

            batteryBar = new JProgressBar(0, 100);
            batteryBar.setValue((int) drone.getBatteryLevel());
            batteryBar.setPreferredSize(new Dimension(160, 8));
            batteryBar.setBackground(new Color(33, 38, 45));
            batteryBar.setBorderPainted(false);
            updateBatteryColor(drone.getBatteryLevel());

            lblBatteryPct = new JLabel(String.format("%.0f%%", drone.getBatteryLevel()));
            lblBatteryPct.setFont(Theme.FONT_BODY);
            lblBatteryPct.setForeground(Theme.TEXT_SECONDARY);

            row2.add(batteryBar, BorderLayout.CENTER);
            row2.add(lblBatteryPct, BorderLayout.EAST);

            // Row 3: Comms Link Status
            JPanel row3 = new JPanel(new BorderLayout());
            row3.setBackground(Theme.BG_CARD);

            lblCommsLink = new JLabel("Connecting...");
            lblCommsLink.setFont(Theme.FONT_BODY);
            lblCommsLink.setForeground(Theme.TEXT_MUTED);
            row3.add(lblCommsLink, BorderLayout.WEST);

            add(row1);
            add(Box.createVerticalStrut(3));
            add(row2);
            add(Box.createVerticalStrut(3));
            add(row3);

            // Card click listener
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setSelectedDrone(droneId);
                    if (forestCanvas != null) {
                        forestCanvas.setSelectedDroneId(droneId);
                    }
                }
            });

            updateTelemetry(drone);
        }

        public void setSelected(boolean sel) {
            this.isCardSelected = sel;
            repaint();
        }

        public void updateTelemetry(Drone d) {
            lblCallsign.setText(d.getCallsign() + " [" + d.getId() + "]");
            updateStateBadge(d.getState());

            double batt = d.getBatteryLevel();
            batteryBar.setValue((int) batt);
            lblBatteryPct.setText(String.format("%.0f%%", batt));
            updateBatteryColor(batt);

            // Comms link status without emojis
            if (!d.isOperational()) {
                lblCommsLink.setText("Offline / Non-Operational");
                lblCommsLink.setForeground(Theme.RED_ALERT);
            } else if (controller.isDroneInTowerRange(d)) {
                Tower t = controller.getNearestTowerInRange(d);
                lblCommsLink.setText("Direct: " + (t != null ? t.getId() : "Tower"));
                lblCommsLink.setForeground(Theme.CYAN_ACCENT);
            } else {
                Drone relay = controller.getMeshRelayDrone(d);
                if (relay != null) {
                    lblCommsLink.setText("Mesh Relay: " + relay.getCallsign());
                    lblCommsLink.setForeground(Theme.PURPLE_RELAY);
                } else {
                    lblCommsLink.setText("Out of Range (Buffering)");
                    lblCommsLink.setForeground(Theme.AMBER_WARN);
                }
            }
        }

        private void updateStateBadge(DroneState state) {
            lblStateBadge.setText(state.name());
            if (state == DroneState.SEARCHING) {
                lblStateBadge.setForeground(Theme.GREEN_ONLINE);
            } else if (state == DroneState.PERSON_FOUND) {
                lblStateBadge.setForeground(Theme.AMBER_WARN);
            } else if (state == DroneState.OFFLINE) {
                lblStateBadge.setForeground(Theme.RED_ALERT);
            } else {
                lblStateBadge.setForeground(Theme.TEXT_MUTED);
            }
        }

        private void updateBatteryColor(double batt) {
            if (batt > 50) {
                batteryBar.setForeground(Theme.GREEN_ONLINE);
            } else if (batt > 20) {
                batteryBar.setForeground(Theme.AMBER_WARN);
            } else {
                batteryBar.setForeground(Theme.RED_ALERT);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color bg = isCardSelected ? Theme.BG_CARD_HOVER : Theme.BG_CARD;
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));

            Color borderCol = isCardSelected ? Theme.CYAN_ACCENT : Theme.BORDER_SUBTLE;
            g2.setColor(borderCol);
            g2.setStroke(new BasicStroke(isCardSelected ? 1.5f : 1.0f));
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 8, 8));

            g2.dispose();
        }
    }
}
