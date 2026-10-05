package com.dronesar.gui;

import com.dronesar.controller.SimulationController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * Control Bar Panel at the bottom of the screen with primary simulation controls
 * (Syllabus Module 4 - Swing Controls & Events).
 */
public class ControlBarPanel extends JPanel {

    private final SimulationController controller;
    private final JButton btnPlayPause;
    private final JButton btnStep;
    private final JButton btnReset;

    public ControlBarPanel(SimulationController controller) {
        this.controller = controller;

        setLayout(new FlowLayout(FlowLayout.CENTER, 18, 10));
        setBackground(Theme.BG_PANEL);
        setBorder(new MatteBorder(1, 0, 0, 0, Theme.BORDER_SUBTLE));
        setPreferredSize(new Dimension(1280, 52));

        btnPlayPause = createStyledButton("▶ Start SAR Sweep", Theme.GREEN_ONLINE);
        btnPlayPause.addActionListener(e -> {
            controller.togglePlayPause();
            updatePlayPauseButton();
        });

        btnStep = createStyledButton("⏭ Step", Theme.TEXT_PRIMARY);
        btnStep.addActionListener(e -> {
            if (!controller.isRunning()) {
                controller.startSimulation();
                controller.tick(0.08);
                controller.stopSimulation();
            }
        });

        btnReset = createStyledButton("🔄 Reset Scenario", Theme.AMBER_WARN);
        btnReset.addActionListener(e -> {
            controller.stopSimulation();
            controller.initDefaultScenario(6);
            updatePlayPauseButton();
        });

        add(btnPlayPause);
        add(btnStep);
        add(btnReset);
    }

    private JButton createStyledButton(String text, Color accentColor) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_HEADING);
        btn.setBackground(Theme.BG_CONTROL);
        btn.setForeground(accentColor);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                new EmptyBorder(6, 16, 6, 16)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public void updatePlayPauseButton() {
        if (controller.isRunning()) {
            btnPlayPause.setText("⏸ Pause SAR Sweep");
            btnPlayPause.setForeground(Theme.AMBER_WARN);
        } else {
            btnPlayPause.setText("▶ Start SAR Sweep");
            btnPlayPause.setForeground(Theme.GREEN_ONLINE);
        }
    }
}
