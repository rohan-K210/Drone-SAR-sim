package com.dronesar.gui;

import com.dronesar.controller.SimulationController;
import com.dronesar.event.SimulationEventBus;
import com.dronesar.event.SimulationEventListener;
import com.dronesar.model.DetectionEvent;
import com.dronesar.model.Drone;
import com.dronesar.model.Position;
import com.dronesar.model.enums.DroneState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Status and Telemetry panel displaying active drones, direct tower link status,
 * fault injection controls, and real-time SAR event logs.
 */
public class StatusPanel extends JPanel implements SimulationEventListener {

    private final SimulationController controller;
    private final JTable droneTable;
    private final DefaultTableModel tableModel;
    private final JTextArea eventLogArea;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public StatusPanel(SimulationController controller) {
        this.controller = controller;

        setLayout(new BorderLayout(0, 10));
        setBackground(Theme.BG_PANEL);
        setPreferredSize(new Dimension(380, 560));
        setBorder(new MatteBorder(0, 1, 0, 0, Theme.BORDER_SUBTLE));

        // 1. Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.BG_PANEL);
        headerPanel.setBorder(new EmptyBorder(12, 14, 6, 14));

        JLabel titleLabel = new JLabel("FLEET TELEMETRY & TOWER FEED");
        titleLabel.setFont(Theme.FONT_HEADING);
        titleLabel.setForeground(Theme.CYAN_ACCENT);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // 2. Drone Telemetry Table
        String[] columnNames = {"ID", "Zone", "Status", "Batt", "Tower Link"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        droneTable = new JTable(tableModel);
        droneTable.setBackground(Theme.BG_DARK);
        droneTable.setForeground(Theme.TEXT_PRIMARY);
        droneTable.setGridColor(Theme.BORDER_SUBTLE);
        droneTable.setRowHeight(24);
        droneTable.setFont(Theme.FONT_SMALL);
        droneTable.getTableHeader().setBackground(Theme.BG_CONTROL);
        droneTable.getTableHeader().setForeground(Theme.TEXT_SECONDARY);
        droneTable.getTableHeader().setFont(Theme.FONT_SMALL);
        droneTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom Cell Renderer for status colors
        droneTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(isSelected ? Theme.BG_CONTROL : Theme.BG_DARK);

                if (column == 2) { // Status column
                    String status = String.valueOf(value);
                    if ("SEARCHING".equals(status)) setForeground(Theme.GREEN_ONLINE);
                    else if ("PERSON_FOUND".equals(status)) setForeground(Theme.AMBER_WARN);
                    else if ("OFFLINE".equals(status) || "EMERGENCY".equals(status)) setForeground(Theme.RED_ALERT);
                    else setForeground(Theme.TEXT_PRIMARY);
                } else if (column == 4) { // Tower Link column
                    String link = String.valueOf(value);
                    if ("CONNECTED".equals(link)) setForeground(Theme.CYAN_ACCENT);
                    else setForeground(Theme.TEXT_MUTED);
                } else {
                    setForeground(Theme.TEXT_PRIMARY);
                }
                return c;
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(droneTable);
        tableScrollPane.setPreferredSize(new Dimension(360, 190));
        tableScrollPane.setBorder(new MatteBorder(1, 0, 1, 0, Theme.BORDER_SUBTLE));
        tableScrollPane.getViewport().setBackground(Theme.BG_DARK);

        // Action button to simulate drone failure
        JButton btnToggleFailure = new JButton("Toggle Selected Drone Failure");
        btnToggleFailure.setFont(Theme.FONT_SMALL);
        btnToggleFailure.setBackground(Theme.BG_CONTROL);
        btnToggleFailure.setForeground(Theme.AMBER_WARN);
        btnToggleFailure.setFocusPainted(false);
        btnToggleFailure.addActionListener(e -> {
            int selectedRow = droneTable.getSelectedRow();
            if (selectedRow >= 0) {
                String droneId = (String) tableModel.getValueAt(selectedRow, 0);
                controller.toggleDroneFailure(droneId);
                logEvent("ALERT", "Drone " + droneId + " operational state toggled.");
            } else {
                JOptionPane.showMessageDialog(this, "Please select a drone from the table first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JPanel tableWrapper = new JPanel(new BorderLayout(0, 6));
        tableWrapper.setBackground(Theme.BG_PANEL);
        tableWrapper.setBorder(new EmptyBorder(0, 12, 6, 12));
        tableWrapper.add(tableScrollPane, BorderLayout.CENTER);
        tableWrapper.add(btnToggleFailure, BorderLayout.SOUTH);

        // 3. Real-Time Event Log Feed
        JPanel logPanel = new JPanel(new BorderLayout(0, 6));
        logPanel.setBackground(Theme.BG_PANEL);
        logPanel.setBorder(new EmptyBorder(6, 12, 12, 12));

        JLabel logHeader = new JLabel("MISSION INCIDENTS & TOWER DISPATCH");
        logHeader.setFont(Theme.FONT_HEADING);
        logHeader.setForeground(Theme.BLUE_TOWER);
        logPanel.add(logHeader, BorderLayout.NORTH);

        eventLogArea = new JTextArea();
        eventLogArea.setEditable(false);
        eventLogArea.setBackground(Theme.BG_DARK);
        eventLogArea.setForeground(Theme.TEXT_PRIMARY);
        eventLogArea.setFont(Theme.FONT_MONO);
        eventLogArea.setLineWrap(true);
        eventLogArea.setWrapStyleWord(true);

        JScrollPane logScrollPane = new JScrollPane(eventLogArea);
        logScrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE));
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        // Assemble status panel
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBackground(Theme.BG_PANEL);
        centerContainer.add(tableWrapper, BorderLayout.NORTH);
        centerContainer.add(logPanel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);
        add(centerContainer, BorderLayout.CENTER);

        // Register to event bus (Delegation Event Model)
        SimulationEventBus.getInstance().registerListener(this);
        logEvent("SYS", "Command Center & Direct Tower Comms initialized.");
    }

    public void updateTelemetryTable() {
        int rowCount = tableModel.getRowCount();
        java.util.List<Drone> drones = controller.getDrones();

        // Adjust row count if necessary
        if (rowCount != drones.size()) {
            tableModel.setRowCount(0);
            for (Drone d : drones) {
                tableModel.addRow(new Object[]{d.getId(), "", "", "", ""});
            }
        }

        for (int i = 0; i < drones.size(); i++) {
            Drone d = drones.get(i);
            boolean inRange = controller.isDroneInTowerRange(d);
            tableModel.setValueAt(d.getId(), i, 0);
            tableModel.setValueAt(d.getAssignedZoneId(), i, 1);
            tableModel.setValueAt(d.getState().name(), i, 2);
            tableModel.setValueAt(String.format("%.0f%%", d.getBatteryLevel()), i, 3);
            tableModel.setValueAt(inRange ? "CONNECTED" : "NO LINK", i, 4);
        }
    }

    public void logEvent(String tag, String message) {
        String timestamp = LocalTime.now().format(TIME_FMT);
        String entry = String.format("[%s] [%s] %s\n", timestamp, tag, message);
        SwingUtilities.invokeLater(() -> {
            eventLogArea.append(entry);
            eventLogArea.setCaretPosition(eventLogArea.getDocument().getLength());
        });
    }

    @Override
    public void onDroneMoved(String droneId, Position newPos, double heading) {
        // High frequency, handled in periodic table update
    }

    @Override
    public void onPersonDetected(DetectionEvent event) {
        logEvent("DETECT", "Survivor LOCATED: " + event.getPersonName() + " (" + event.getPersonId() + ") by " + event.getOriginDroneId());
    }

    @Override
    public void onZoneCoverageUpdated(String zoneId, double coveragePercentage) {
        // Handled in HUD
    }

    @Override
    public void onDroneStatusChanged(String droneId, DroneState newStatus) {
        logEvent("STATUS", "Drone " + droneId + " switched state -> " + newStatus);
    }

    @Override
    public void onDirectTransmission(String droneId, String towerId, DetectionEvent event) {
        logEvent("COMMS", "Direct transmission beam: " + droneId + " -> " + towerId + " for target " + event.getPersonName());
    }

    @Override
    public void onPacketDeliveredToTower(DetectionEvent event) {
        logEvent("BASE", "CONFIRMED: " + event.getPersonName() + " logged at Base Tower! Rescue dispatched.");
    }
}
