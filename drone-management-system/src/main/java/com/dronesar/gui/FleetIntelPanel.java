package com.dronesar.gui;

import com.dronesar.controller.SimulationController;
import com.dronesar.model.Drone;
import com.dronesar.model.PersonNode;
import com.dronesar.model.Tower;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Secondary Dashboard Screen displaying Mission Analytics, Fleet Health,
 * and expandable spaces for additional presentation features.
 */
public class FleetIntelPanel extends JPanel {

    private final SimulationController controller;
    private final JPanel cardsContainer;

    public FleetIntelPanel(SimulationController controller) {
        this.controller = controller;

        setLayout(new BorderLayout());
        setBackground(Theme.BG_DARK);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        // Top Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_DARK);

        JLabel title = new JLabel("MISSION ANALYTICS & FLEET INTEL");
        title.setFont(Theme.FONT_HERO);
        title.setForeground(Theme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Real-time telemetry aggregation, RF signal statistics, and target acquisition tracking");
        subtitle.setFont(Theme.FONT_TITLE);
        subtitle.setForeground(Theme.CYAN_ACCENT);

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setBackground(Theme.BG_DARK);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitle);

        header.add(titleBox, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Center Content with Grid
        cardsContainer = new JPanel(new GridLayout(2, 2, 20, 20));
        cardsContainer.setBackground(Theme.BG_DARK);
        cardsContainer.setBorder(new EmptyBorder(20, 0, 0, 0));

        // 1. Drone Fleet Summary Card
        cardsContainer.add(createFleetSummaryCard());

        // 2. Survivor Discovery Status Card
        cardsContainer.add(createSurvivorStatusCard());

        // 3. Dual Base Comms Network Card
        cardsContainer.add(createCommsNetworkCard());

        // 4. Extensible Reserved Space Card
        cardsContainer.add(createReservedWorkspaceCard());

        add(cardsContainer, BorderLayout.CENTER);
    }

    private JPanel createFleetSummaryCard() {
        JPanel card = createStyledCard("🚁 UAV SWARM INVENTORY", Theme.CYAN_ACCENT);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        for (Drone drone : controller.getDrones()) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(Theme.BG_CARD);
            row.setMaximumSize(new Dimension(500, 26));

            JLabel lblId = new JLabel("• " + drone.getId() + " (" + drone.getCallsign() + ")");
            lblId.setFont(Theme.FONT_BODY);
            lblId.setForeground(Theme.TEXT_PRIMARY);

            String status = drone.isOperational() ? String.format("%.0f%% Batt | %s", drone.getBatteryLevel(), drone.getState()) : "OFFLINE";
            JLabel lblStatus = new JLabel(status);
            lblStatus.setFont(Theme.FONT_SMALL);
            lblStatus.setForeground(drone.isOperational() ? Theme.GREEN_ONLINE : Theme.RED_ALERT);

            row.add(lblId, BorderLayout.WEST);
            row.add(lblStatus, BorderLayout.EAST);
            card.add(row);
            card.add(Box.createVerticalStrut(4));
        }
        return card;
    }

    private JPanel createSurvivorStatusCard() {
        JPanel card = createStyledCard("🎯 SEARCH TARGETS (HIKERS)", Theme.GREEN_ONLINE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        for (PersonNode target : controller.getTargets()) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(Theme.BG_CARD);
            row.setMaximumSize(new Dimension(500, 26));

            JLabel lblName = new JLabel("• " + target.getName() + " (" + target.getId() + ")");
            lblName.setFont(Theme.FONT_BODY);
            lblName.setForeground(Theme.TEXT_PRIMARY);

            String statusText = target.isDetected() ? ("✔ LOCATED: " + target.getLocation().toGpsString()) : "⏳ SEARCHING...";
            JLabel lblFound = new JLabel(statusText);
            lblFound.setFont(Theme.FONT_SMALL);
            lblFound.setForeground(target.isDetected() ? Theme.GREEN_ONLINE : Theme.AMBER_WARN);

            row.add(lblName, BorderLayout.WEST);
            row.add(lblFound, BorderLayout.EAST);
            card.add(row);
            card.add(Box.createVerticalStrut(4));
        }
        return card;
    }

    private JPanel createCommsNetworkCard() {
        JPanel card = createStyledCard("📡 DUAL-BASE WIRELESS MESH", Theme.BLUE_TOWER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        for (Tower tower : controller.getTowers()) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(Theme.BG_CARD);
            row.setMaximumSize(new Dimension(500, 26));

            JLabel lblTower = new JLabel("• " + tower.getName() + " [" + tower.getId() + "]");
            lblTower.setFont(Theme.FONT_BODY);
            lblTower.setForeground(Theme.TEXT_PRIMARY);

            JLabel lblRange = new JLabel("Range: " + (int) tower.getReceptionRadius() + "m | ONLINE");
            lblRange.setFont(Theme.FONT_SMALL);
            lblRange.setForeground(Theme.GREEN_ONLINE);

            row.add(lblTower, BorderLayout.WEST);
            row.add(lblRange, BorderLayout.EAST);
            card.add(row);
            card.add(Box.createVerticalStrut(6));
        }

        JLabel note = new JLabel("Transmission Buffer: Automatic offline packet store & forward enabled.");
        note.setFont(Theme.FONT_SMALL);
        note.setForeground(Theme.TEXT_SECONDARY);
        card.add(Box.createVerticalStrut(10));
        card.add(note);

        return card;
    }

    private JPanel createReservedWorkspaceCard() {
        JPanel card = createStyledCard("📋 RESERVED PRESENTATION MODULE", Theme.GOLD_ACCENT);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel info1 = new JLabel("⚡ Space reserved for upcoming team features & live demo metrics.");
        info1.setFont(Theme.FONT_BODY);
        info1.setForeground(Theme.TEXT_PRIMARY);

        JLabel info2 = new JLabel("• Weather conditions & wind drift simulation");
        info2.setFont(Theme.FONT_SMALL);
        info2.setForeground(Theme.TEXT_SECONDARY);

        JLabel info3 = new JLabel("• Dynamic survivor rescue vehicle routing logs");
        info3.setFont(Theme.FONT_SMALL);
        info3.setForeground(Theme.TEXT_SECONDARY);

        JLabel info4 = new JLabel("• Flight telemetry export to CSV / Blackbox database");
        info4.setFont(Theme.FONT_SMALL);
        info4.setForeground(Theme.TEXT_SECONDARY);

        card.add(info1);
        card.add(Box.createVerticalStrut(10));
        card.add(info2);
        card.add(Box.createVerticalStrut(4));
        card.add(info3);
        card.add(Box.createVerticalStrut(4));
        card.add(info4);

        return card;
    }

    private JPanel createStyledCard(String title, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(Theme.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));

                g2.setColor(Theme.BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));

                g2.setColor(accentColor);
                g2.fillRect(16, 0, getWidth() - 32, 3);
                g2.dispose();
            }
        };
        card.setBorder(new EmptyBorder(16, 20, 16, 20));
        card.setBackground(Theme.BG_DARK);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(accentColor);
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(12));

        return card;
    }
}
