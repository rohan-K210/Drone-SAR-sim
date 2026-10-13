package com.dronesar;

import com.dronesar.controller.SimulationController;
import com.dronesar.gui.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ScreenshotGenerator {

    public static void main(String[] args) {
        String outputDir = (args.length > 0) ? args[0] : ".";
        new File(outputDir).mkdirs();

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        SimulationController controller = new SimulationController();

        // 1. Capture Welcome Screen
        renderWelcomeScreen(controller, new File(outputDir, "welcome_screen.png"));

        // 2. Advance simulation so drones fly, radar sweeps, and targets are detected
        controller.startSimulation();
        for (int i = 0; i < 40; i++) {
            controller.tick(0.05);
        }
        controller.stopSimulation();

        // 3. Capture Tactical Dashboard Screen
        renderSimulationDashboard(controller, new File(outputDir, "simulation_dashboard.png"));

        // 4. Capture Database Viewer Screen
        renderDatabaseViewer(controller, new File(outputDir, "database_viewer.png"));

        System.out.println("Screenshots successfully generated in: " + outputDir);
        System.exit(0);
    }

    private static void renderWelcomeScreen(SimulationController controller, File targetFile) {
        int width = 1360;
        int height = 780;

        JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.setSize(width, height);

        WelcomePanel welcomePanel = new WelcomePanel(controller, count -> {});
        frame.setContentPane(welcomePanel);
        frame.pack();
        frame.setSize(width, height);
        layoutTree(frame);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        frame.paint(g2);
        g2.dispose();
        frame.dispose();

        try {
            ImageIO.write(image, "png", targetFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void renderSimulationDashboard(SimulationController controller, File targetFile) {
        int width = 1360;
        int height = 780;

        JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.setSize(width, height);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_DARK);

        ForestCanvas canvas = new ForestCanvas(controller);
        TelemetryHUDPanel hud = new TelemetryHUDPanel(controller);
        StatusPanel status = new StatusPanel(controller);
        ControlBarPanel bar = new ControlBarPanel(controller, () -> {});

        status.setForestCanvas(canvas);
        canvas.setStatusPanel(status);
        hud.updateMetrics();
        status.updateTelemetryTable();

        panel.add(hud, BorderLayout.NORTH);
        panel.add(canvas, BorderLayout.CENTER);
        panel.add(status, BorderLayout.EAST);
        panel.add(bar, BorderLayout.SOUTH);

        frame.setContentPane(panel);
        frame.pack();
        frame.setSize(width, height);
        layoutTree(frame);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        frame.paint(g2);
        g2.dispose();
        frame.dispose();

        try {
            ImageIO.write(image, "png", targetFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void renderDatabaseViewer(SimulationController controller, File targetFile) {
        int width = 1360;
        int height = 780;

        JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.setSize(width, height);

        DatabaseViewerPanel panel = new DatabaseViewerPanel(controller);
        panel.refreshData();

        frame.setContentPane(panel);
        frame.pack();
        frame.setSize(width, height);
        layoutTree(frame);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        frame.paint(g2);
        g2.dispose();
        frame.dispose();

        try {
            ImageIO.write(image, "png", targetFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void layoutTree(Component comp) {
        comp.doLayout();
        if (comp instanceof Container c) {
            for (Component child : c.getComponents()) {
                layoutTree(child);
            }
        }
    }
}
