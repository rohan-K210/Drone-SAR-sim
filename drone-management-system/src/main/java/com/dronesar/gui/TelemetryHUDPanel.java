package com.dronesar.gui;

import com.dronesar.controller.SimulationController;
import com.dronesar.model.Drone;
import com.dronesar.model.PersonNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * Top Telemetry HUD displaying high-level SAR mission metrics.
 */
public class TelemetryHUDPanel extends JPanel {

    private final SimulationController controller;

    private final JLabel lblFleetStatus;
    private final JLabel lblBatteryAvg;
    private final JLabel lblCoverage;
    private final JLabel lblSurvivors;
    private final JLabel lblClock;

    public TelemetryHUDPanel(SimulationController controller) {
        this.controller = controller;

        setLayout(new FlowLayout(FlowLayout.CENTER, 28, 10));
        setBackground(Theme.BG_PANEL);
        setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE));

        lblFleetStatus = createMetricValue("6 / 6 ONLINE", Theme.GREEN_ONLINE);
        lblBatteryAvg = createMetricValue("100%", Theme.CYAN_ACCENT);
        lblCoverage = createMetricValue("0.0%", Theme.BLUE_TOWER);
        lblSurvivors = createMetricValue("0 / 5 FOUND", Theme.AMBER_WARN);
        lblClock = createMetricValue("00:00:00", Theme.TEXT_PRIMARY);

        add(createMetricCard("FLEET READINESS", lblFleetStatus));
        add(createDivider());
        add(createMetricCard("AVERAGE BATTERY", lblBatteryAvg));
        add(createDivider());
        add(createMetricCard("SEARCH COVERAGE", lblCoverage));
        add(createDivider());
        add(createMetricCard("SURVIVORS LOCATED", lblSurvivors));
        add(createDivider());
        add(createMetricCard("MISSION DURATION", lblClock));
    }

    private JPanel createMetricCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.BG_PANEL);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_SMALL);
        lblTitle.setForeground(Theme.TEXT_SECONDARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(3));
        card.add(valueLabel);
        return card;
    }

    private JLabel createMetricValue(String initialText, Color color) {
        JLabel label = new JLabel(initialText);
        label.setFont(Theme.FONT_HEADING);
        label.setForeground(color);
        return label;
    }

    private JComponent createDivider() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 28));
        sep.setForeground(Theme.BORDER_SUBTLE);
        return sep;
    }

    public void updateMetrics() {
        int totalDrones = controller.getDrones().size();
        int operationalDrones = 0;
        double totalBattery = 0.0;

        for (Drone d : controller.getDrones()) {
            if (d.isOperational()) operationalDrones++;
            totalBattery += d.getBatteryLevel();
        }

        double avgBattery = (totalDrones > 0) ? (totalBattery / totalDrones) : 0.0;
        lblFleetStatus.setText(operationalDrones + " / " + totalDrones + " ONLINE");
        lblFleetStatus.setForeground(operationalDrones == totalDrones ? Theme.GREEN_ONLINE : Theme.AMBER_WARN);

        lblBatteryAvg.setText(String.format("%.1f%%", avgBattery));
        lblBatteryAvg.setForeground(avgBattery > 40.0 ? Theme.CYAN_ACCENT : Theme.RED_ALERT);

        double coverage = controller.getOverallCoveragePercentage();
        lblCoverage.setText(String.format("%.1f%%", coverage));

        int foundCount = 0;
        int totalTargets = controller.getTargets().size();
        for (PersonNode target : controller.getTargets()) {
            if (target.isDetected()) foundCount++;
        }
        lblSurvivors.setText(foundCount + " / " + totalTargets + " FOUND");
        lblSurvivors.setForeground(foundCount > 0 ? Theme.GREEN_ONLINE : Theme.AMBER_WARN);

        long millis = controller.getElapsedSimulationMillis();
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60));
        lblClock.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }
}
