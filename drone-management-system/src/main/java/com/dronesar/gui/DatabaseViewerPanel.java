package com.dronesar.gui;

import com.dronesar.controller.SimulationController;
import com.dronesar.database.DatabaseManager;
import com.dronesar.database.DetectionDAO;
import com.dronesar.event.SimulationEventBus;
import com.dronesar.event.SimulationEventListener;
import com.dronesar.model.DetectionEvent;
import com.dronesar.model.Position;
import com.dronesar.model.enums.DroneState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * Tactical Real-Time Database Viewer Panel.
 * Displays persistent records from the SQLite database (drone_sar.db) in real time
 * as drones detect missing persons and deliver emergency telemetry packets to base towers.
 */
public class DatabaseViewerPanel extends JPanel implements SimulationEventListener {

    private final SimulationController controller;
    private final DetectionDAO detectionDAO;

    // KPI Badges
    private JLabel lblDbStatus;
    private JLabel lblTotalRecords;
    private JLabel lblDeliveredPackets;
    private JLabel lblPendingPackets;

    // Table models
    private DefaultTableModel detectionsTableModel;
    private JTable detectionsTable;

    private DefaultTableModel rawSchemaTableModel;
    private JTable rawSchemaTable;

    private JTabbedPane tabbedPane;
    private JLabel lblFooterInfo;

    public DatabaseViewerPanel(SimulationController controller) {
        this.controller = controller;
        this.detectionDAO = new DetectionDAO();

        setLayout(new BorderLayout());
        setBackground(Theme.BG_DARK);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. Top Header
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Center Content with KPIs and Tabbed Tables
        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setBackground(Theme.BG_DARK);

        // KPI Badges Row
        centerPanel.add(createKpiGrid(), BorderLayout.NORTH);

        // Tabbed Tables Section
        centerPanel.add(createTabbedTablesPanel(), BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Status Bar
        add(createFooterPanel(), BorderLayout.SOUTH);

        // Register to receive real-time detection & delivery events
        SimulationEventBus.getInstance().registerListener(this);

        // Initial Data Load
        refreshData();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_DARK);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE));
        header.setPreferredSize(new Dimension(1000, 56));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setBackground(Theme.BG_DARK);

        JLabel title = new JLabel("SQLITE DATABASE VIEWER (BLACKBOX PERSISTENCE)");
        title.setFont(Theme.FONT_HERO);
        title.setForeground(Theme.CYAN_ACCENT);

        JLabel subtitle = new JLabel("Real-time persistent relational storage: 'drone_sar.db' • Thread-safe DAO Blackbox");
        subtitle.setFont(Theme.FONT_SMALL);
        subtitle.setForeground(Theme.TEXT_SECONDARY);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(subtitle);

        // Action Buttons (Clear Detections action)
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actionBox.setBackground(Theme.BG_DARK);

        JButton btnClear = createStyledButton("Clear Detections", Theme.RED_ALERT);
        btnClear.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to clear all records from the 'detections' table in SQLite?",
                    "Confirm Clear SQLite Data",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (opt == JOptionPane.YES_OPTION) {
                detectionDAO.clear();
                refreshData();
            }
        });

        actionBox.add(btnClear);

        header.add(titleBox, BorderLayout.WEST);
        header.add(actionBox, BorderLayout.EAST);

        return header;
    }

    private JPanel createKpiGrid() {
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        kpiGrid.setBackground(Theme.BG_DARK);
        kpiGrid.setMaximumSize(new Dimension(1400, 90));
        kpiGrid.setPreferredSize(new Dimension(1000, 85));

        lblDbStatus = new JLabel("ONLINE");
        lblTotalRecords = new JLabel("0 STORED");
        lblDeliveredPackets = new JLabel("0 DELIVERED");
        lblPendingPackets = new JLabel("0 IN TRANSIT");

        kpiGrid.add(createKpiCard("SQLITE ENGINE STATUS", lblDbStatus, Theme.GREEN_ONLINE));
        kpiGrid.add(createKpiCard("TOTAL PERSISTED EVENTS", lblTotalRecords, Theme.CYAN_ACCENT));
        kpiGrid.add(createKpiCard("DELIVERED TO TOWER", lblDeliveredPackets, Theme.BLUE_TOWER));
        kpiGrid.add(createKpiCard("RELAYED IN TRANSIT", lblPendingPackets, Theme.AMBER_WARN));

        return kpiGrid;
    }

    private JPanel createKpiCard(String label, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(Theme.BORDER_SUBTLE);
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.setColor(accentColor);
                g2.fillRect(0, 0, 4, getHeight());
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setBackground(Theme.BG_CARD);

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_SECONDARY);

        valueLabel.setFont(Theme.FONT_TITLE);
        valueLabel.setForeground(accentColor);

        card.add(lbl);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);
        return card;
    }

    private JPanel createTabbedTablesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_DARK);

        tabbedPane = new JTabbedPane();
        tabbedPane.setUI(new Theme.CyberTabbedPaneUI());
        tabbedPane.setBackground(Theme.BG_DARK);
        tabbedPane.setForeground(Theme.CYAN_ACCENT);
        tabbedPane.setFont(Theme.FONT_HEADING);

        // 1. Detections Table
        tabbedPane.addTab("detections Table (Live)", createDetectionsTablePanel());

        // 2. Database Schema & Tables Info Tab
        tabbedPane.addTab("Schema & Table Metadata", createSchemaInfoPanel());

        panel.add(tabbedPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDetectionsTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));

        String[] cols = {
                "Event ID",
                "Person ID",
                "Target Name",
                "Coordinates (X, Y) | GPS",
                "Detecting Drone",
                "Timestamp",
                "Mesh Route",
                "Hops",
                "Status"
        };

        detectionsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        detectionsTable = new JTable(detectionsTableModel);
        styleTable(detectionsTable);

        // Column widths
        detectionsTable.getColumnModel().getColumn(0).setPreferredWidth(90);  // Event ID
        detectionsTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // Person ID
        detectionsTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Target Name
        detectionsTable.getColumnModel().getColumn(3).setPreferredWidth(220); // GPS
        detectionsTable.getColumnModel().getColumn(4).setPreferredWidth(110); // Drone
        detectionsTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Timestamp
        detectionsTable.getColumnModel().getColumn(6).setPreferredWidth(180); // Route
        detectionsTable.getColumnModel().getColumn(7).setPreferredWidth(55);  // Hops
        detectionsTable.getColumnModel().getColumn(8).setPreferredWidth(110); // Status

        // Custom Cell Renderer
        detectionsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setBackground(row % 2 == 0 ? Theme.BG_DARK : Theme.BG_CARD);

                String strVal = String.valueOf(value);
                if (col == 0) {
                    setForeground(Theme.CYAN_ACCENT);
                    setFont(Theme.FONT_MONO);
                } else if (col == 3) {
                    setForeground(Theme.CYAN_ACCENT);
                    setFont(Theme.FONT_MONO);
                } else if (col == 8) {
                    if (strVal.contains("DELIVERED")) {
                        setForeground(Theme.GREEN_ONLINE);
                    } else {
                        setForeground(Theme.AMBER_WARN);
                    }
                    setFont(Theme.FONT_HEADING);
                } else {
                    setForeground(Theme.TEXT_PRIMARY);
                    setFont(Theme.FONT_BODY);
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(detectionsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE));
        scrollPane.getViewport().setBackground(Theme.BG_DARK);
        Theme.applyModernScrollBar(scrollPane);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSchemaInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));

        String[] cols = {"Table Name", "Column Name", "Type", "Primary Key", "Nullable"};
        rawSchemaTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        rawSchemaTable = new JTable(rawSchemaTableModel);
        styleTable(rawSchemaTable);

        rawSchemaTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setBackground(row % 2 == 0 ? Theme.BG_DARK : Theme.BG_CARD);

                if (col == 0) {
                    setForeground(Theme.CYAN_ACCENT);
                    setFont(Theme.FONT_HEADING);
                } else if (col == 1) {
                    setForeground(Theme.TEXT_PRIMARY);
                    setFont(Theme.FONT_MONO);
                } else if (col == 3 && "YES".equals(value)) {
                    setForeground(Theme.GOLD_ACCENT);
                    setFont(Theme.FONT_HEADING);
                } else {
                    setForeground(Theme.TEXT_SECONDARY);
                    setFont(Theme.FONT_BODY);
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(rawSchemaTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE));
        scrollPane.getViewport().setBackground(Theme.BG_DARK);
        Theme.applyModernScrollBar(scrollPane);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void styleTable(JTable table) {
        table.setBackground(Theme.BG_DARK);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setGridColor(Theme.BORDER_SUBTLE);
        table.setRowHeight(32);
        table.setFont(Theme.FONT_BODY);
        table.setFillsViewportHeight(true);

        JTableHeader th = table.getTableHeader();
        th.setBackground(Theme.BG_CONTROL);
        th.setForeground(Theme.CYAN_ACCENT);
        th.setFont(Theme.FONT_HEADING);
        th.setPreferredSize(new Dimension(0, 36));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE));
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.BG_DARK);
        footer.setBorder(new EmptyBorder(10, 4, 4, 4));

        lblFooterInfo = new JLabel("Database: drone_sar.db | Engine: SQLite 3 | Real-time Auto-Sync: Active");
        lblFooterInfo.setFont(Theme.FONT_BODY);
        lblFooterInfo.setForeground(Theme.TEXT_MUTED);

        JLabel lblSyncStatus = new JLabel("● AUTO-SYNC ACTIVE");
        lblSyncStatus.setFont(Theme.FONT_BODY);
        lblSyncStatus.setForeground(Theme.GREEN_ONLINE);

        footer.add(lblFooterInfo, BorderLayout.WEST);
        footer.add(lblSyncStatus, BorderLayout.EAST);

        return footer;
    }

    private JButton createStyledButton(String text, Color accentColor) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_BODY);
        btn.setForeground(accentColor);
        btn.setBackground(Theme.BG_CONTROL);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                new EmptyBorder(6, 14, 6, 14)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Reads all detection records directly from SQLite and updates the JTable and KPIs.
     */
    public synchronized void refreshData() {
        try {
            List<DetectionDAO.DetectionRecord> records = detectionDAO.getAllDetections();

            // Clear table
            detectionsTableModel.setRowCount(0);

            int deliveredCount = 0;
            int pendingCount = 0;

            for (DetectionDAO.DetectionRecord r : records) {
                if (r.isDelivered()) {
                    deliveredCount++;
                } else {
                    pendingCount++;
                }

                // Format coordinates to real-world GPS
                double lat = 45.3500 + (r.getY() / 800.0) * 0.1000;
                double lon = 121.7500 + (r.getX() / 1000.0) * 0.1000;
                String gpsCoords = String.format("(%.1f, %.1f) • %.4f°N, %.4f°W", r.getX(), r.getY(), lat, lon);

                String statusBadge = r.isDelivered() ? "DELIVERED" : "IN TRANSIT";

                detectionsTableModel.addRow(new Object[]{
                        r.getEventId(),
                        r.getPersonId(),
                        r.getPersonName(),
                        gpsCoords,
                        r.getDroneId(),
                        r.getTimestamp(),
                        r.getRoute() != null ? r.getRoute() : "-",
                        r.getHopCount(),
                        statusBadge
                });
            }

            // Update KPI cards
            lblTotalRecords.setText(records.size() + " STORED");
            lblDeliveredPackets.setText(deliveredCount + " DELIVERED");
            lblPendingPackets.setText(pendingCount + " IN TRANSIT");

            // Update footer info with SQLite file size (without showing local absolute directory path)
            File dbFile = new File("drone_sar.db");
            String fileSize = dbFile.exists() ? (dbFile.length() / 1024) + " KB" : "0 KB";
            lblFooterInfo.setText(String.format("Database: drone_sar.db (%s) | Records: %d | SQLite 3 JDBC Connected",
                    fileSize, records.size()));

            lblDbStatus.setText("ONLINE");
            lblDbStatus.setForeground(Theme.GREEN_ONLINE);

            // Populate Schema Information
            loadSchemaInfo();

        } catch (Exception e) {
            lblDbStatus.setText("ERROR");
            lblDbStatus.setForeground(Theme.RED_ALERT);
            System.err.println("DatabaseViewerPanel refresh error: " + e.getMessage());
        }
    }

    private void loadSchemaInfo() {
        rawSchemaTableModel.setRowCount(0);
        String[] tables = {"detections", "rescue_teams"};

        try (Connection conn = DatabaseManager.getConnection()) {
            for (String tableName : tables) {
                String sql = "PRAGMA table_info(" + tableName + ");";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        String colName = rs.getString("name");
                        String colType = rs.getString("type");
                        boolean notNull = rs.getInt("notnull") == 1;
                        boolean pk = rs.getInt("pk") == 1;

                        rawSchemaTableModel.addRow(new Object[]{
                                tableName,
                                colName,
                                colType,
                                pk ? "YES" : "NO",
                                notNull ? "NO" : "YES"
                        });
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    // ==========================================
    // SimulationEventListener Real-Time Triggers
    // ==========================================

    @Override
    public void onPersonDetected(DetectionEvent event) {
        SwingUtilities.invokeLater(this::refreshData);
    }

    @Override
    public void onPacketDeliveredToTower(DetectionEvent event) {
        SwingUtilities.invokeLater(this::refreshData);
    }

    @Override
    public void onDroneMoved(String droneId, Position newPos, double heading) {}

    @Override
    public void onZoneCoverageUpdated(String zoneId, double coveragePercentage) {}

    @Override
    public void onDroneStatusChanged(String droneId, DroneState newStatus) {}
}
