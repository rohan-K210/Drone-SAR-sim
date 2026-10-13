package com.dronesar.gui;

import com.dronesar.controller.SimulationController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * High-tech Tactical Intro / Welcoming Screen for the Drone SAR Fleet Management System.
 * Displays mission briefing, tactical parameters, team module architecture, and mission launch controls.
 */
public class WelcomePanel extends JPanel {

    private final SimulationController controller;
    private final Consumer<Integer> launchCallback;
    private int selectedDroneCount = 6;
    private final JComboBox<String> fleetSizeCombo;

    public WelcomePanel(SimulationController controller, Consumer<Integer> launchCallback) {
        this.controller = controller;
        this.launchCallback = launchCallback;

        setLayout(new BorderLayout());
        setBackground(Theme.BG_DARK);

        // Main scrollable or padded center container
        JPanel contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setBackground(Theme.BG_DARK);
        contentContainer.setBorder(new EmptyBorder(25, 40, 25, 40));

        // 1. Hero Header with Insignia Badge
        contentContainer.add(createHeroHeader());
        contentContainer.add(Box.createVerticalStrut(25));

        // 2. Middle Section with Three Tactical Cards
        JPanel cardsGrid = new JPanel(new GridLayout(1, 3, 20, 0));
        cardsGrid.setBackground(Theme.BG_DARK);
        cardsGrid.setMaximumSize(new Dimension(1200, 360));
        cardsGrid.setPreferredSize(new Dimension(1150, 340));

        // Card A: Scenario Briefing
        cardsGrid.add(createScenarioCard());

        // Card B: Fleet Deployment Configuration
        fleetSizeCombo = new JComboBox<>(new String[]{"4 Drones (Light Patrol)", "6 Drones (Standard Swarm)", "8 Drones (Maximum Grid Sweep)"});
        fleetSizeCombo.setSelectedIndex(1);
        fleetSizeCombo.setFont(Theme.FONT_HEADING);
        fleetSizeCombo.setBackground(Theme.BG_CONTROL);
        fleetSizeCombo.setForeground(Theme.CYAN_ACCENT);
        fleetSizeCombo.addActionListener(e -> {
            int idx = fleetSizeCombo.getSelectedIndex();
            selectedDroneCount = (idx == 0) ? 4 : ((idx == 1) ? 6 : 8);
        });

        cardsGrid.add(createFleetConfigCard());

        // Card C: Team Architecture & Subsystems
        cardsGrid.add(createArchitectureCard());

        contentContainer.add(cardsGrid);
        contentContainer.add(Box.createVerticalStrut(30));

        // 3. Bottom Action Bar with Glowing Launch Button
        contentContainer.add(createActionLaunchBar());

        // Wrap in a subtle gradient/background panel
        add(contentContainer, BorderLayout.CENTER);
    }

    private JPanel createHeroHeader() {
        JPanel hero = new JPanel();
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBackground(Theme.BG_DARK);
        hero.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Insignia Icon Canvas
        JPanel badgePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                // Pulsing glow rings
                g2.setColor(new Color(0, 229, 255, 30));
                g2.fill(new Ellipse2D.Double(cx - 36, cy - 36, 72, 72));
                g2.setColor(new Color(0, 229, 255, 70));
                g2.draw(new Ellipse2D.Double(cx - 30, cy - 30, 60, 60));

                // Outer Hexagon or Ring
                g2.setColor(Theme.CYAN_ACCENT);
                g2.setStroke(new BasicStroke(2.0f));
                g2.draw(new Ellipse2D.Double(cx - 24, cy - 24, 48, 48));

                // Drone Wings / Crosshair
                g2.setColor(Theme.GREEN_ONLINE);
                g2.drawLine(cx - 18, cy, cx + 18, cy);
                g2.drawLine(cx, cy - 18, cx, cy + 18);

                // Central Node
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(cx - 5, cy - 5, 10, 10));

                g2.dispose();
            }
        };
        badgePanel.setPreferredSize(new Dimension(80, 80));
        badgePanel.setMaximumSize(new Dimension(80, 80));
        badgePanel.setBackground(Theme.BG_DARK);
        badgePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("AERO-SAR TACTICAL COMMAND CENTER");
        title.setFont(Theme.FONT_HERO);
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("AUTONOMOUS MULTI-UAV SWARM OPERATIONS • DUAL-BASE WIRELESS TELEMETRY");
        subtitle.setFont(Theme.FONT_TITLE);
        subtitle.setForeground(Theme.CYAN_ACCENT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel badge = new JLabel("  ● SYSTEM READY  |  TERRAIN RADAR ACTIVE  |  OOP MVC ARCHITECTURE  ");
        badge.setFont(Theme.FONT_SMALL);
        badge.setForeground(Theme.GREEN_ONLINE);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.GREEN_ONLINE, 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        hero.add(badgePanel);
        hero.add(Box.createVerticalStrut(8));
        hero.add(title);
        hero.add(Box.createVerticalStrut(4));
        hero.add(subtitle);
        hero.add(Box.createVerticalStrut(10));
        hero.add(badge);

        return hero;
    }

    private JPanel createScenarioCard() {
        JPanel card = createStyledCard("MISSION BRIEFING & TERRAIN", Theme.CYAN_ACCENT);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        addCardLine(card, "🗺️ Operational Area:", "Blackwood Forest (920m × 560m)");
        addCardLine(card, "🎯 Mission Target:", "Locate 5 Missing Civilians / Hikers");
        addCardLine(card, "📡 Base Stations:", "West Base Alpha (SW) & East Ridge (NE)");
        addCardLine(card, "🔄 Flight Algorithm:", "Boustrophedon Parallel Swarm Sweeps");
        addCardLine(card, "⚡ Flight Conditions:", "Real-Time Battery & Sensor Range Models");
        addCardLine(card, "📶 Comms Protocol:", "Direct RF Packet Drops & Retry Buffers");

        return card;
    }

    private JPanel createFleetConfigCard() {
        JPanel card = createStyledCard("FLEET DEPLOYMENT CONTROLS", Theme.GREEN_ONLINE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel lblSelect = new JLabel("Select Swarm Fleet Size:");
        lblSelect.setFont(Theme.FONT_HEADING);
        lblSelect.setForeground(Theme.TEXT_PRIMARY);
        lblSelect.setAlignmentX(Component.LEFT_ALIGNMENT);

        fleetSizeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        fleetSizeCombo.setMaximumSize(new Dimension(320, 36));

        card.add(lblSelect);
        card.add(Box.createVerticalStrut(8));
        card.add(fleetSizeCombo);
        card.add(Box.createVerticalStrut(16));

        addCardLine(card, "🚁 Swarm Protocol:", "Autonomous Waypoint Allocation");
        addCardLine(card, "🔋 Battery Range:", "Active Discharge & RTH Low-Battery Safety");
        addCardLine(card, "🚨 Target Recognition:", "Optical + Thermal Sensor Payloads");
        addCardLine(card, "🎮 Manual Overrides:", "Pause, Recall, Sector Sweep & Fail Toggles");

        return card;
    }

    private JPanel createArchitectureCard() {
        JPanel card = createStyledCard("TEAM SUBSYSTEMS & MODULES", Theme.BLUE_TOWER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        addCardLine(card, "🛰️ Swarm Navigation:", "com.dronesar.search (ZoneAllocator)");
        addCardLine(card, "📡 Comms & Mesh:", "com.dronesar.network (CommunicationManager)");
        addCardLine(card, "🎯 Sensor Detection:", "com.dronesar.detection (DetectionEngine)");
        addCardLine(card, "🚑 Ground Rescue:", "com.dronesar.rescue (RescueManager)");
        addCardLine(card, "💾 Mission Blackbox:", "com.dronesar.database (DetectionDAO / DB)");
        addCardLine(card, "🎛️ MVC Controller:", "com.dronesar.controller (SimulationController)");

        return card;
    }

    private JPanel createStyledCard(String title, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Card background with rounded corners
                g2.setColor(Theme.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                // Card border
                g2.setColor(Theme.BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                // Accent top bar
                g2.setColor(accentColor);
                g2.fillRect(16, 0, getWidth() - 32, 3);
                g2.dispose();
            }
        };
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setBackground(Theme.BG_DARK);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(accentColor);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(14));

        return card;
    }

    private void addCardLine(JPanel panel, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Theme.BG_CARD);
        row.setMaximumSize(new Dimension(380, 26));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblKey = new JLabel(label);
        lblKey.setFont(Theme.FONT_BODY);
        lblKey.setForeground(Theme.TEXT_SECONDARY);

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(Theme.FONT_SMALL);
        lblVal.setForeground(Theme.TEXT_PRIMARY);

        row.add(lblKey, BorderLayout.WEST);
        row.add(lblVal, BorderLayout.EAST);

        panel.add(row);
        panel.add(Box.createVerticalStrut(6));
    }

    private JPanel createActionLaunchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 0));
        bar.setBackground(Theme.BG_DARK);
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Big Glowing Hero Launch Button
        JButton btnLaunch = new JButton("🚀 INITIALIZE & LAUNCH SAR MISSION") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean hover = getModel().isRollover();
                boolean pressed = getModel().isPressed();

                // Glowing background
                if (pressed) {
                    g2.setColor(new Color(0, 180, 200));
                } else if (hover) {
                    g2.setColor(new Color(0, 240, 255));
                } else {
                    g2.setColor(new Color(0, 200, 225));
                }
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));

                // Inner glow outline
                g2.setColor(new Color(255, 255, 255, hover ? 140 : 80));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 16, 16));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnLaunch.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnLaunch.setForeground(new Color(10, 14, 20));
        btnLaunch.setFocusPainted(false);
        btnLaunch.setContentAreaFilled(false);
        btnLaunch.setBorder(new EmptyBorder(14, 34, 14, 34));
        btnLaunch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLaunch.addActionListener(e -> {
            if (launchCallback != null) {
                launchCallback.accept(selectedDroneCount);
            }
        });

        // Secondary Info Button
        JButton btnSpecs = new JButton("📘 System Architecture & OOP Specs");
        btnSpecs.setFont(Theme.FONT_HEADING);
        btnSpecs.setForeground(Theme.TEXT_SECONDARY);
        btnSpecs.setBackground(Theme.BG_CONTROL);
        btnSpecs.setFocusPainted(false);
        btnSpecs.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1, true),
                new EmptyBorder(12, 22, 12, 22)
        ));
        btnSpecs.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSpecs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnSpecs.setForeground(Theme.CYAN_ACCENT);
                btnSpecs.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.CYAN_ACCENT, 1, true),
                        new EmptyBorder(12, 22, 12, 22)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnSpecs.setForeground(Theme.TEXT_SECONDARY);
                btnSpecs.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1, true),
                        new EmptyBorder(12, 22, 12, 22)
                ));
            }
        });
        btnSpecs.addActionListener(e -> showSystemArchitectureDialog());

        bar.add(btnLaunch);
        bar.add(btnSpecs);

        return bar;
    }

    private void showSystemArchitectureDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "System Architecture & OOP Specifications", true);
        dialog.setSize(720, 520);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Theme.BG_DARK);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("Tactical Drone SAR System Specifications");
        title.setFont(Theme.FONT_SUBHERO);
        title.setForeground(Theme.CYAN_ACCENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(15));

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setBackground(Theme.BG_CARD);
        area.setForeground(Theme.TEXT_PRIMARY);
        area.setFont(Theme.FONT_MONO);
        area.setBorder(new EmptyBorder(12, 12, 12, 12));
        area.setText(
                "=================================================================\n" +
                " OBJECT-ORIENTED PROGRAMMING (OOP) ARCHITECTURE SPECIFICATION\n" +
                "=================================================================\n\n" +
                "1. MODEL-VIEW-CONTROLLER (MVC Pattern):\n" +
                "   - Model: Drone, SearchZone, Tower, TransmissionSignal, PersonNode\n" +
                "   - View: ForestCanvas, TelemetryHUDPanel, StatusPanel, WelcomePanel\n" +
                "   - Controller: SimulationController\n\n" +
                "2. DESIGN PATTERNS APPLIED:\n" +
                "   - Singleton: DatabaseManager for thread-safe blackbox access\n" +
                "   - Observer / Event Bus: SimulationEventBus & SimulationEventListener\n" +
                "   - DAO Pattern: DetectionDAO & MissionDAO for decoupled persistence\n" +
                "   - Strategy Pattern: SearchPattern (Boustrophedon sweep planning)\n\n" +
                "3. TEAM MODULE ALLOCATION & INTEGRATION:\n" +
                "   - Module 1 (Fleet & Models): Drone physics, battery discharge, states\n" +
                "   - Module 2 (Detection): Optical/Thermal FOV & Signal attenuation model\n" +
                "   - Module 3 (Navigation): Spatial sector allocation & waypoint generation\n" +
                "   - Module 4 (Comms): Dual-tower mesh, out-of-range packet buffer & retry\n" +
                "   - Module 5 (Database): Flight incident logging & survivor registry\n" +
                "   - Module 6 (Rescue): Emergency rapid-response ground squad dispatch\n" +
                "   - Module 7 (GUI): Swing 60-FPS tactical radar map & HUD telemetry\n"
        );

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE));
        panel.add(scroll);
        panel.add(Box.createVerticalStrut(15));

        JButton btnClose = new JButton("Close Specifications");
        btnClose.setFont(Theme.FONT_HEADING);
        btnClose.setForeground(Theme.TEXT_PRIMARY);
        btnClose.setBackground(Theme.BG_CONTROL);
        btnClose.setFocusPainted(false);
        btnClose.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnClose.addActionListener(e -> dialog.dispose());
        panel.add(btnClose);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
}
