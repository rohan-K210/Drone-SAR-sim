package com.dronesar.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Presentation Helper Panel outlining team member modules, OOP concepts,
 * and design patterns for the classroom presentation.
 */
public class TeamGuidePanel extends JPanel {

    public TeamGuidePanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_DARK);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_DARK);

        JLabel title = new JLabel("TEAM MODULES & OOP ARCHITECTURE GUIDE");
        title.setFont(Theme.FONT_HERO);
        title.setForeground(Theme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Quick-reference guide for presenting Object-Oriented Programming (OOP) paradigms to evaluators");
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
        JPanel grid = new JPanel(new GridLayout(2, 2, 20, 20));
        grid.setBackground(Theme.BG_DARK);
        grid.setBorder(new EmptyBorder(20, 0, 0, 0));

        grid.add(createCard("1. MODEL-VIEW-CONTROLLER (MVC)", Theme.CYAN_ACCENT,
                "• Model (com.dronesar.model): Drone physics, SearchZone, Tower, Signal entities.\n" +
                "• View (com.dronesar.gui): ForestCanvas (2D radar), TelemetryHUDPanel, StatusPanel.\n" +
                "• Controller (com.dronesar.controller): SimulationController coordinates loop & events."));

        grid.add(createCard("2. CORE OOP PRINCIPLES", Theme.GREEN_ONLINE,
                "• Encapsulation: Drone battery, coordinates, and state variables protected via accessors.\n" +
                "• Polymorphism: SimulationEventListener interface implemented by UI views.\n" +
                "• Inheritance: NetworkNode base class extended by Drone and Tower.\n" +
                "• Abstraction: Search algorithms decoupled from GUI drawing."));

        grid.add(createCard("3. DESIGN PATTERNS APPLIED", Theme.BLUE_TOWER,
                "• Singleton: DatabaseManager provides centralized flight blackbox access.\n" +
                "• Observer: SimulationEventBus broadcasts detection incidents asynchronously.\n" +
                "• DAO Pattern: DetectionDAO & MissionDAO isolate persistent data operations.\n" +
                "• Strategy: SearchPattern encapsulates lawnmower waypoint calculations."));

        grid.add(createCard("4. PRESENTATION TALKING POINTS", Theme.GOLD_ACCENT,
                "• Standalone Authentication: Zero external backend dependency required for demo.\n" +
                "• Real-Time Concurrency: CopyOnWriteArrayList & ConcurrentHashMap prevent race conditions.\n" +
                "• Dual Base Comms: Drones buffer offline packets and auto-retry upon tower reconnection.\n" +
                "• 60 FPS Graphics2D: Double-buffered custom painting with animated radar sweeps."));

        add(grid, BorderLayout.CENTER);
    }

    private JPanel createCard(String title, Color accentColor, String contentText) {
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
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 20, 16, 20));
        card.setBackground(Theme.BG_DARK);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(accentColor);
        card.add(lblTitle, BorderLayout.NORTH);

        JTextArea area = new JTextArea(contentText);
        area.setEditable(false);
        area.setBackground(Theme.BG_CARD);
        area.setForeground(Theme.TEXT_PRIMARY);
        area.setFont(Theme.FONT_BODY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(12, 0, 0, 0));
        card.add(area, BorderLayout.CENTER);

        return card;
    }
}
