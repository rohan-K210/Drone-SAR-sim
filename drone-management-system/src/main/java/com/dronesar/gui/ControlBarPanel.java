package com.dronesar.gui;

import com.dronesar.controller.SimulationController;
import com.dronesar.model.PersonNode;
import com.dronesar.model.Position;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.Random;

/**
 * Control Bar Panel at bottom of screen with simulation buttons, speed controls,
 * and incident scenario triggers (Syllabus Module 4 - Swing Controls & Events).
 */
public class ControlBarPanel extends JPanel {

    private final SimulationController controller;
    private final JButton btnPlayPause;
    private final JButton btnStep;
    private final JButton btnReset;
    private final JButton btnAddTarget;
    private final JSlider sliderSpeed;
    private final JLabel lblSpeedVal;
    private final Random random = new Random();

    public ControlBarPanel(SimulationController controller) {
        this.controller = controller;

        setLayout(new BorderLayout(15, 0));
        setBackground(Theme.BG_PANEL);
        setBorder(new MatteBorder(1, 0, 0, 0, Theme.BORDER_SUBTLE));
        setPreferredSize(new Dimension(1280, 52));

        // Left: Primary Action Buttons
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        leftActions.setBackground(Theme.BG_PANEL);

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

        btnAddTarget = createStyledButton("+ Deploy Missing Person", Theme.CYAN_ACCENT);
        btnAddTarget.addActionListener(e -> {
            double rx = 180 + random.nextDouble() * 680;
            double ry = 60 + random.nextDouble() * 420;
            int nextNum = 100 + controller.getTargets().size() + 1;
            PersonNode newPerson = new PersonNode("P-" + nextNum, "Survivor-" + nextNum, new Position(rx, ry));
            controller.addTarget(newPerson);
        });

        leftActions.add(btnPlayPause);
        leftActions.add(btnStep);
        leftActions.add(btnReset);
        leftActions.add(btnAddTarget);

        // Right: Simulation Speed Slider
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        rightControls.setBackground(Theme.BG_PANEL);

        JLabel lblSpeed = new JLabel("SIMULATION SPEED:");
        lblSpeed.setFont(Theme.FONT_SMALL);
        lblSpeed.setForeground(Theme.TEXT_SECONDARY);

        lblSpeedVal = new JLabel("1.0x");
        lblSpeedVal.setFont(Theme.FONT_MONO_BOLD);
        lblSpeedVal.setForeground(Theme.CYAN_ACCENT);

        sliderSpeed = new JSlider(10, 50, 10);
        sliderSpeed.setPreferredSize(new Dimension(140, 26));
        sliderSpeed.setBackground(Theme.BG_PANEL);
        sliderSpeed.setForeground(Theme.CYAN_ACCENT);
        sliderSpeed.addChangeListener(e -> {
            double speed = sliderSpeed.getValue() / 10.0;
            controller.setSpeedMultiplier(speed);
            lblSpeedVal.setText(String.format("%.1fx", speed));
        });

        rightControls.add(lblSpeed);
        rightControls.add(sliderSpeed);
        rightControls.add(lblSpeedVal);

        add(leftActions, BorderLayout.WEST);
        add(rightControls, BorderLayout.EAST);
    }

    private JButton createStyledButton(String text, Color accentColor) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_HEADING);
        btn.setBackground(Theme.BG_CONTROL);
        btn.setForeground(accentColor);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                new EmptyBorder(5, 12, 5, 12)
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
