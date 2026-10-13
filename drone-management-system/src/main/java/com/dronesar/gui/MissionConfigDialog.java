package com.dronesar.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Tactical Modal Dialog for configuring SAR mission parameters:
 *  - Swarm Fleet Size (number of UAVs)
 *  - Reported Missing Persons (casualties to locate)
 */
public class MissionConfigDialog extends JDialog {

    private final JSpinner spinnerDrones;
    private final JSpinner spinnerPersons;
    private boolean confirmed = false;

    public MissionConfigDialog(Frame owner) {
        super(owner, "AERO-SAR Mission Setup & Dispatch", true);
        setResizable(false);
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BG_DARK);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // 1. Header Box
        JLabel lblTag = new JLabel("MISSION DISPATCH CONFIGURATION");
        lblTag.setFont(Theme.FONT_HERO);
        lblTag.setForeground(Theme.CYAN_ACCENT);

        JLabel lblSub = new JLabel("Configure autonomous swarm fleet assets & casualty parameters.");
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_SECONDARY);

        JPanel headerBox = new JPanel();
        headerBox.setLayout(new BoxLayout(headerBox, BoxLayout.Y_AXIS));
        headerBox.setBackground(Theme.BG_DARK);
        headerBox.add(lblTag);
        headerBox.add(Box.createVerticalStrut(4));
        headerBox.add(lblSub);
        root.add(headerBox);
        root.add(Box.createVerticalStrut(18));

        // 2. Parameters Card
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(Theme.BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        card.setLayout(new GridBagLayout());
        card.setBackground(Theme.BG_DARK);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Drones count row
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.6;
        JLabel lblDrones = new JLabel("Swarm Fleet Size (UAVs):");
        lblDrones.setFont(Theme.FONT_HEADING);
        lblDrones.setForeground(Theme.TEXT_PRIMARY);
        card.add(lblDrones, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.4;
        SpinnerNumberModel dronesModel = new SpinnerNumberModel(4, 2, 8, 1);
        spinnerDrones = new JSpinner(dronesModel);
        styleSpinner(spinnerDrones);
        card.add(spinnerDrones, gbc);

        // Missing persons row
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.6;
        JLabel lblPersons = new JLabel("Missing Persons (Lost Casualties):");
        lblPersons.setFont(Theme.FONT_HEADING);
        lblPersons.setForeground(Theme.TEXT_PRIMARY);
        card.add(lblPersons, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.4;
        SpinnerNumberModel personsModel = new SpinnerNumberModel(3, 1, 8, 1);
        spinnerPersons = new JSpinner(personsModel);
        styleSpinner(spinnerPersons);
        card.add(spinnerPersons, gbc);

        root.add(card);
        root.add(Box.createVerticalStrut(14));

        // 3. Tactical Operational Briefing notes
        JPanel notesBox = new JPanel();
        notesBox.setLayout(new BoxLayout(notesBox, BoxLayout.Y_AXIS));
        notesBox.setBackground(Theme.BG_DARK);

        JLabel n1 = new JLabel("• Swarm fleet stages on standby across West & East Base Towers.");
        n1.setFont(Theme.FONT_SMALL);
        n1.setForeground(Theme.TEXT_MUTED);

        JLabel n2 = new JLabel("• Target coordinates are dynamically randomized across search sectors.");
        n2.setFont(Theme.FONT_SMALL);
        n2.setForeground(Theme.TEXT_MUTED);

        JLabel n3 = new JLabel("• Unlocated targets are hidden on radar until physical sensor sweep.");
        n3.setFont(Theme.FONT_SMALL);
        n3.setForeground(Theme.TEXT_MUTED);

        notesBox.add(n1);
        notesBox.add(Box.createVerticalStrut(3));
        notesBox.add(n2);
        notesBox.add(Box.createVerticalStrut(3));
        notesBox.add(n3);

        root.add(notesBox);
        root.add(Box.createVerticalStrut(18));

        // 4. Buttons Row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(Theme.BG_DARK);

        JButton btnCancel = createTacticalButton("Cancel", Theme.TEXT_MUTED);
        btnCancel.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton btnDeploy = createTacticalButton("Deploy Fleet to Base Station", Theme.GREEN_ONLINE);
        btnDeploy.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        btnRow.add(btnCancel);
        btnRow.add(btnDeploy);
        root.add(btnRow);

        add(root, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(owner);
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(Theme.FONT_HEADING);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setBackground(Theme.BG_CONTROL);
            de.getTextField().setForeground(Theme.CYAN_ACCENT);
            de.getTextField().setHorizontalAlignment(JTextField.CENTER);
            de.getTextField().setFont(Theme.FONT_HEADING);
            de.getTextField().setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));
        }
    }

    private JButton createTacticalButton(String text, Color accent) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                Color bg = hover ? Theme.BG_CARD_HOVER : Theme.BG_CONTROL;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(hover ? accent : Theme.BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(hover ? 1.5f : 1.0f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(Theme.FONT_HEADING);
        btn.setForeground(accent);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btn.repaint(); }
            @Override
            public void mouseExited(MouseEvent e) { btn.repaint(); }
        });
        return btn;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public int getDroneCount() {
        return (Integer) spinnerDrones.getValue();
    }

    public int getPersonCount() {
        return (Integer) spinnerPersons.getValue();
    }
}
