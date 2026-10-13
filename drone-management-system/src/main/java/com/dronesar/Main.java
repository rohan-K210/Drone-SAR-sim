package com.dronesar;

import com.dronesar.controller.SimulationController;
import com.dronesar.database.DatabaseManager;
import com.dronesar.database.DetectionPersistenceListener;
import com.dronesar.event.SimulationEventBus;
import com.dronesar.gui.DashboardFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main application launcher for the Drone SAR Fleet Management System.
 */
public class Main {
    public static void main(String[] args) {
        // Set cross-platform look-and-feel or system look-and-feel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("ScrollBarUI", "com.dronesar.gui.ModernScrollBarUI");
            UIManager.put("TabbedPaneUI", "com.dronesar.gui.CyberTabbedPaneUI");
        } catch (Exception ignored) {
        }

        // Initialize SQLite database and register event persistence listener
        DatabaseManager.createTables();
        SimulationEventBus.getInstance().registerListener(new DetectionPersistenceListener());

        SwingUtilities.invokeLater(() -> {
            SimulationController controller = new SimulationController();
            DashboardFrame frame = new DashboardFrame(controller);
            frame.setVisible(true);
        });
    }
}
