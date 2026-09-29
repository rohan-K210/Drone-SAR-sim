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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Status and Telemetry panel displaying active drones, direct dual-tower link status,
 * individual drone command controls, and real-time SAR event logs.
 */
public class StatusPanel extends JPanel implements SimulationEventListener {

    private final SimulationController controller;
    private ForestCanvas forestCanvas;
    private final JTable droneTable;
    private final DefaultTableModel tableModel;
    private final JTextArea eventLogArea;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public StatusPanel(SimulationController controller) {
        this.controller = controller;

        setLayout(new BorderLayout(0, 8));
        setBackground(Theme.BG_PANEL);
        setPreferredSize(new Dimension(390, 560));
        setBorder(new MatteBorder(0, 1, 0, 0, Theme.BORDER_SUBTLE));

        // 1. Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.BG_PANEL);
        headerPanel.setBorder(new EmptyBorder(10, 14, 4, 14));

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
        droneTable.setRowHeight(22);
        droneTable.setFont(Theme.FONT_SMALL);
        droneTable.getTableHeader().setBackground(Theme.BG_CONTROL);
        droneTable.getTableHeader().setForeground(Theme.TEXT_SECONDARY);
        droneTable.getTableHeader().setFont(Theme.FONT_SMALL);
        droneTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Selection Listener: highlight drone on canvas when table row selected
        droneTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = droneTable.getSelectedRow();
                if (row >= 0 && row < tableModel.getRowCount()) {
                    String id = (String) tableModel.getValueAt(row, 0);
                    if (forestCanvas != null) {
                        forestCanvas.setSelectedDroneId(id);
                    }
                }
            }
        });

        // Custom Cell Renderer for status and tower links
        droneTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(isSelected ? Theme.BG_CONTROL : Theme.BG_DARK);

                if (column == 2) { // Status column
                    String status = String.valueOf(value);
                    if ("SEARCHING".equals(status)) setForeground(Theme.GREEN_ONLINE);
                    else if ("PERSON_FOUND".equals(status)) setForeground(Theme.AMBER_WARN);
                    else if ("IDLE".equals(status)) setForeground(Theme.TEXT_MUTED);
                    else if ("OFFLINE".equals(status)) setForeground(Theme.RED_ALERT);
                    else setForeground(Theme.TEXT_PRIMARY);
                } else if (column == 4) { // Tower Link column
                    String link = String.valueOf(value);
                    if (link != null && link.startsWith("BASE-")) setForeground(Theme.CYAN_ACCENT);
                    else setForeground(Theme.TEXT_MUTED);
                } else {
                    setForeground(Theme.TEXT_PRIMARY);
                }
                return c;
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(droneTable);
        tableScrollPane.setPreferredSize(new Dimension(370, 160));
        tableScrollPane.setBorder(new MatteBorder(1, 0, 1, 0, Theme.BORDER_SUBTLE));
        tableScrollPane.getViewport().setBackground(Theme.BG_DARK);

        // 3. Individual Drone Tactical Control Buttons
        JPanel droneControlBox = new JPanel(new GridLayout(2, 2, 6, 6));
        droneControlBox.setBackground(Theme.BG_PANEL);
        droneControlBox.setBorder(new EmptyBorder(6, 12, 6, 12));

        JButton btnPauseResume = createCommandButton("⏸ / ▶ Pause/Resume", Theme.CYAN_ACCENT);
        btnPauseResume.addActionListener(e -> {
            String selectedId = getSelectedDroneId();
            if (selectedId != null) {
                controller.toggleDronePause(selectedId);
                logEvent("CMD", "Drone " + selectedId + " pause state toggled.");
            } else {
                showSelectDroneAlert();
            }
        });

        JButton btnResumeSearch = createCommandButton("🔄 Sweep Sector", Theme.GREEN_ONLINE);
        btnResumeSearch.addActionListener(e -> {
            String selectedId = getSelectedDroneId();
            if (selectedId != null) {
                controller.resumeDroneSearch(selectedId);
                logEvent("CMD", "Drone " + selectedId + " ordered to resume sector sweep.");
            } else {
                showSelectDroneAlert();
            }
        });

        JButton btnRecall = createCommandButton("🏠 Recall to Base", Theme.BLUE_TOWER);
        btnRecall.addActionListener(e -> {
            String selectedId = getSelectedDroneId();
            if (selectedId != null) {
                controller.recallDroneToBase(selectedId);
                logEvent("CMD", "Drone " + selectedId + " recalled to nearest Base Tower.");
            } else {
                showSelectDroneAlert();
            }
        });

        JButton btnToggleFail = createCommandButton("⚠️ Toggle Fail", Theme.AMBER_WARN);
        btnToggleFail.addActionListener(e -> {
            String selectedId = getSelectedDroneId();
            if (selectedId != null) {
                controller.toggleDroneFailure(selectedId);
                logEvent("ALERT", "Drone " + selectedId + " failure toggled.");
            } else {
                showSelectDroneAlert();
            }
        });

        droneControlBox.add(btnPauseResume);
        droneControlBox.add(btnResumeSearch);
        droneControlBox.add(btnRecall);
        droneControlBox.add(btnToggleFail);

        JPanel tableWrapper = new JPanel(new BorderLayout(0, 4));
        tableWrapper.setBackground(Theme.BG_PANEL);
        tableWrapper.add(tableScrollPane, BorderLayout.CENTER);
        tableWrapper.add(droneControlBox, BorderLayout.SOUTH);

        // 4. Real-Time Event Log Feed
        JPanel logPanel = new JPanel(new BorderLayout(0, 4));
        logPanel.setBackground(Theme.BG_PANEL);
        logPanel.setBorder(new EmptyBorder(4, 12, 10, 12));

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

        // Register to event bus
        SimulationEventBus.getInstance().registerListener(this);
        logEvent("SYS", "Dual-Tower Comms & Mission Center online.");
    }

    public void setForestCanvas(ForestCanvas canvas) {
        this.forestCanvas = canvas;
    }

    public void setSelectedDroneInTable(String droneId) {
        if (droneId == null) return;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (droneId.equals(tableModel.getValueAt(i, 0))) {
                droneTable.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    private String getSelectedDroneId() {
        int row = droneTable.getSelectedRow();
        if (row >= 0 && row < tableModel.getRowCount()) {
            return (String) tableModel.getValueAt(row, 0);
        }
        return null;
    }

    private void showSelectDroneAlert() {
        JOptionPane.showMessageDialog(this, "Please select a drone from the table or click on a drone on the radar map.", "Target Selection Required", JOptionPane.INFORMATION_MESSAGE);
    }

    private JButton createCommandButton(String text, Color accent) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_SMALL);
        btn.setBackground(Theme.BG_CONTROL);
        btn.setForeground(accent);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public void updateTelemetryTable() {
        int rowCount = tableModel.getRowCount();
        java.util.List<Drone> drones = controller.getDrones();

        if (rowCount != drones.size()) {
            tableModel.setRowCount(0);
            for (Drone d : drones) {
                tableModel.addRow(new Object[]{d.getId(), "", "", "", ""});
            }
        }

        for (int i = 0; i < drones.size(); i++) {
            Drone d = drones.get(i);
            Tower connectedTower = controller.getNearestTowerInRange(d);
            tableModel.setValueAt(d.getId(), i, 0);
            tableModel.setValueAt(d.getAssignedZoneId(), i, 1);
            tableModel.setValueAt(d.getState().name(), i, 2);
            tableModel.setValueAt(String.format("%.0f%%", d.getBatteryLevel()), i, 3);
            tableModel.setValueAt(connectedTower != null ? connectedTower.getId().replace("BASE-", "") : "NO LINK", i, 4);
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
    public void onDroneMoved(String droneId, Position newPos, double heading) {}

    @Override
    public void onPersonDetected(DetectionEvent event) {
        logEvent("DETECT", "Survivor FOUND: " + event.getPersonName() + " (" + event.getPersonId() + ") by " + event.getOriginDroneId());
    }

    @Override
    public void onZoneCoverageUpdated(String zoneId, double coveragePercentage) {}

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
        logEvent("BASE", "CONFIRMED: " + event.getPersonName() + " logged at Base Tower! Rescue team notified.");
    }
}
