package com.dronesar;

import com.dronesar.database.DatabaseManager;
import com.dronesar.database.DetectionDAO;
import com.dronesar.database.DetectionPersistenceListener;
import com.dronesar.event.SimulationEventBus;
import com.dronesar.model.DetectionEvent;
import com.dronesar.model.Position;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabasePersistenceTest {

    @BeforeAll
    public static void setup() {
        DatabaseManager.createTables();
    }

    @Test
    public void testDetectionEventPersistenceAndRetrieval() {
        DetectionDAO dao = new DetectionDAO();
        String testEventId = "TEST-EVT-" + System.currentTimeMillis();

        DetectionEvent event = new DetectionEvent(
                testEventId,
                "P-TEST",
                "Person 1",
                new Position(250.0, 350.0),
                "Eagle-1",
                "ALPHA-TOWER"
        );

        // 1. Save detection
        boolean saved = dao.saveDetection(event);
        assertTrue(saved, "Detection event should be saved successfully into SQLite");

        // 2. Query detection
        DetectionDAO.DetectionRecord record = dao.findById(testEventId);
        assertNotNull(record, "Saved detection record should be found in SQLite");
        assertEquals("P-TEST", record.getPersonId());
        assertEquals("Person 1", record.getPersonName());
        assertEquals(250.0, record.getX(), 0.001);
        assertEquals(350.0, record.getY(), 0.001);
        assertEquals("Eagle-1", record.getDroneId());
        assertFalse(record.isDelivered(), "Should initially be undelivered");

        // 3. Update delivery status
        event.setSuccessfullyDelivered(true);
        boolean updated = dao.updateDeliveryStatus(event);
        assertTrue(updated, "Delivery status should be updated successfully");

        DetectionDAO.DetectionRecord updatedRecord = dao.findById(testEventId);
        assertNotNull(updatedRecord);
        assertTrue(updatedRecord.isDelivered(), "Record should now be marked delivered");
    }

    @Test
    public void testEventListenerPersistence() {
        DetectionDAO dao = new DetectionDAO();
        DetectionPersistenceListener listener = new DetectionPersistenceListener();
        SimulationEventBus.getInstance().registerListener(listener);

        String busEventId = "BUS-EVT-" + System.currentTimeMillis();
        DetectionEvent busEvent = new DetectionEvent(
                busEventId,
                "P-BUS",
                "Person 2",
                new Position(100.0, 150.0),
                "Eagle-2",
                "BRAVO-TOWER"
        );

        // Publish detection event to bus
        SimulationEventBus.getInstance().publishPersonDetected(busEvent);

        DetectionDAO.DetectionRecord record = dao.findById(busEventId);
        assertNotNull(record, "Detection persisted via event bus listener");
        assertEquals("P-BUS", record.getPersonId());
    }

    @Test
    public void testDatabaseViewerPanelRenderingAndScreenshot() {
        com.dronesar.controller.SimulationController controller = new com.dronesar.controller.SimulationController();
        controller.initDefaultScenario(4, 5);

        // Advance simulation ticks so drones locate missing persons and store in SQLite
        for (int i = 0; i < 40; i++) {
            controller.tick(0.05);
        }

        com.dronesar.gui.DatabaseViewerPanel panel = new com.dronesar.gui.DatabaseViewerPanel(controller);
        panel.setSize(1260, 750);
        layoutTree(panel);

        // Render to BufferedImage
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1260, 750, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        panel.printAll(g2);
        g2.dispose();

        File targetFile = screenshotFile("database_viewer.png");
        try {
            javax.imageio.ImageIO.write(img, "png", targetFile);
            assertTrue(targetFile.exists(), "database_viewer.png must be created");
            assertTrue(targetFile.length() > 1000, "Screenshot file size must be non-empty");
        } catch (Exception e) {
            fail("Failed to capture DatabaseViewerPanel screenshot", e);
        }
    }

    @Test
    public void testCaptureSimulationDashboardScreenshot() {
        com.dronesar.controller.SimulationController controller = new com.dronesar.controller.SimulationController();
        // Test with 5 drones (odd number)
        controller.initDefaultScenario(5, 4);

        for (int i = 0; i < 30; i++) {
            controller.tick(0.05);
        }

        javax.swing.JPanel simPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
        simPanel.setBackground(com.dronesar.gui.Theme.BG_DARK);

        com.dronesar.gui.ForestCanvas canvas = new com.dronesar.gui.ForestCanvas(controller);
        com.dronesar.gui.TelemetryHUDPanel hud = new com.dronesar.gui.TelemetryHUDPanel(controller);
        com.dronesar.gui.StatusPanel status = new com.dronesar.gui.StatusPanel(controller);
        com.dronesar.gui.ControlBarPanel bar = new com.dronesar.gui.ControlBarPanel(controller, () -> {});

        status.setForestCanvas(canvas);
        canvas.setStatusPanel(status);
        hud.updateMetrics();
        status.updateTelemetryTable();

        simPanel.add(hud, java.awt.BorderLayout.NORTH);
        simPanel.add(canvas, java.awt.BorderLayout.CENTER);
        simPanel.add(status, java.awt.BorderLayout.EAST);
        simPanel.add(bar, java.awt.BorderLayout.SOUTH);

        simPanel.setSize(1260, 750);
        layoutTree(simPanel);

        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1260, 750, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        simPanel.printAll(g2);
        g2.dispose();

        File targetFile = screenshotFile("simulation_dashboard_clean.png");
        try {
            javax.imageio.ImageIO.write(img, "png", targetFile);
            assertTrue(targetFile.exists(), "simulation_dashboard_clean.png must be created");
        } catch (Exception e) {
            fail("Failed to capture simulation screenshot", e);
        }
    }

    /**
     * Resolves a screenshot path under the build output directory so the test is
     * portable across machines and operating systems.
     */
    private static File screenshotFile(String fileName) {
        File dir = new File("target", "screenshots");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            fail("Could not create screenshot directory: " + dir.getAbsolutePath());
        }
        return new File(dir, fileName);
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
