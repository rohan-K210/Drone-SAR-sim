package com.dronesar;

import com.dronesar.controller.SimulationController;
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
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            SimulationController controller = new SimulationController();
            DashboardFrame frame = new DashboardFrame(controller);
            frame.setVisible(true);
        });
    }
}
