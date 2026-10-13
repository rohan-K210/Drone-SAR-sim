package com.dronesar.gui;

import com.dronesar.controller.SimulationController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Main application window (JFrame).
 * Streamlined with:
 *  - Standalone Login Screen
 *  - Minimal Sidebar Navigation with 2 Core Views:
 *      1. 🛰️ SAR Live Sweep (Interactive 60-FPS Drone Simulation)
 *      2. 📄 Mission Report (Executive Incident & Survivor Casualty Report)
 *  - Operator Badge and 1-Click Logout
 */
public class DashboardFrame extends JFrame {

    private final SimulationController controller;

    // Root Frame CardLayout: LOGIN vs MAIN_APP
    private final CardLayout rootCardLayout;
    private final JPanel rootCardContainer;
    private static final String CARD_LOGIN = "LOGIN";
    private static final String CARD_MAIN_APP = "MAIN_APP";

    // Inner App CardLayout: SIMULATION vs REPORT vs DATABASE
    private final CardLayout appCardLayout;
    private final JPanel appContentContainer;
    private static final String VIEW_SIMULATION = "VIEW_SIMULATION";
    private static final String VIEW_REPORT = "VIEW_REPORT";
    private static final String VIEW_DATABASE = "VIEW_DATABASE";

    private final LoginPanel loginPanel;
    private final ForestCanvas forestCanvas;
    private final TelemetryHUDPanel hudPanel;
    private final StatusPanel statusPanel;
    private final ControlBarPanel controlBar;
    private final MissionReportPanel missionReportPanel;
    private final DatabaseViewerPanel databaseViewerPanel;

    private final Timer animationTimer;
    private boolean isSimulationActive = false;
    private String currentView = VIEW_SIMULATION;

    // Sidebar navigation buttons
    private JButton btnNavSim;
    private JButton btnNavReport;
    private JButton btnNavDatabase;

    public DashboardFrame(SimulationController controller) {
        super("AERO-SAR Tactical Fleet Management Command");
        this.controller = controller;

        try {
            UIManager.put("ScrollBarUI", "com.dronesar.gui.Theme$ModernScrollBarUI");
            UIManager.put("TabbedPaneUI", "com.dronesar.gui.Theme$CyberTabbedPaneUI");
        } catch (Exception ignored) {
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1360, 820);
        setMinimumSize(new Dimension(1120, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);

        rootCardLayout = new CardLayout();
        rootCardContainer = new JPanel(rootCardLayout);
        rootCardContainer.setBackground(Theme.BG_DARK);

        // 1. Build Login Panel
        this.loginPanel = new LoginPanel(this::onLoginSuccess);
        rootCardContainer.add(loginPanel, CARD_LOGIN);

        // 2. Build Main Application Portal (Sidebar + Content View)
        this.appCardLayout = new CardLayout();
        this.appContentContainer = new JPanel(appCardLayout);
        this.appContentContainer.setBackground(Theme.BG_DARK);

        // Sub-views for the content container
        this.hudPanel = new TelemetryHUDPanel(controller);
        this.forestCanvas = new ForestCanvas(controller);
        this.statusPanel = new StatusPanel(controller);
        this.controlBar = new ControlBarPanel(controller, () -> switchView(VIEW_REPORT), this::openMissionSetupDialog);

        this.statusPanel.setForestCanvas(forestCanvas);
        this.forestCanvas.setStatusPanel(statusPanel);

        // Simulation view panel
        JPanel simulationViewPanel = new JPanel(new BorderLayout());
        simulationViewPanel.setBackground(Theme.BG_DARK);
        simulationViewPanel.add(hudPanel, BorderLayout.NORTH);
        simulationViewPanel.add(forestCanvas, BorderLayout.CENTER);
        simulationViewPanel.add(statusPanel, BorderLayout.EAST);
        simulationViewPanel.add(controlBar, BorderLayout.SOUTH);

        // Dedicated Mission Report Panel & Database Viewer Panel
        this.missionReportPanel = new MissionReportPanel(controller);
        this.databaseViewerPanel = new DatabaseViewerPanel(controller);

        appContentContainer.add(simulationViewPanel, VIEW_SIMULATION);
        appContentContainer.add(missionReportPanel, VIEW_REPORT);
        appContentContainer.add(databaseViewerPanel, VIEW_DATABASE);

        // Assemble Main App Panel with Sidebar
        JPanel mainAppPanel = new JPanel(new BorderLayout());
        mainAppPanel.setBackground(Theme.BG_DARK);
        mainAppPanel.add(createSidebarPanel(), BorderLayout.WEST);
        mainAppPanel.add(appContentContainer, BorderLayout.CENTER);

        rootCardContainer.add(mainAppPanel, CARD_MAIN_APP);

        setLayout(new BorderLayout());
        add(rootCardContainer, BorderLayout.CENTER);

        // Start at Login Screen
        showLoginScreen();

        // 60 FPS Animation & Simulation Loop Timer (every 16ms)
        this.animationTimer = new Timer(16, e -> {
            try {
                if (isSimulationActive && VIEW_SIMULATION.equals(currentView)) {
                    controller.tick(0.016);
                    forestCanvas.repaint();
                    hudPanel.updateMetrics();
                    statusPanel.updateTelemetryTable();
                    controlBar.updatePlayPauseButton();
                }
            } catch (Exception ex) {
                System.err.println("Animation loop error: " + ex.getMessage());
            }
        });
        this.animationTimer.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                animationTimer.stop();
                controller.stopSimulation();
            }
        });
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.BG_PANEL);
        sidebar.setPreferredSize(new Dimension(255, 780));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, Theme.BORDER_SUBTLE));

        // Top Brand Header
        JPanel brandBox = new JPanel();
        brandBox.setLayout(new BoxLayout(brandBox, BoxLayout.Y_AXIS));
        brandBox.setBackground(Theme.BG_PANEL);
        brandBox.setBorder(new EmptyBorder(22, 18, 20, 18));

        JLabel brandTitle = new JLabel("AERO-SAR TACTICAL");
        brandTitle.setFont(Theme.FONT_TITLE);
        brandTitle.setForeground(Theme.CYAN_ACCENT);

        JLabel brandSub = new JLabel("Drone Fleet Management");
        brandSub.setFont(Theme.FONT_SMALL);
        brandSub.setForeground(Theme.TEXT_SECONDARY);

        brandBox.add(brandTitle);
        brandBox.add(Box.createVerticalStrut(3));
        brandBox.add(brandSub);

        // Middle Nav Items (Only Simulation & Report)
        JPanel navMenu = new JPanel();
        navMenu.setLayout(new BoxLayout(navMenu, BoxLayout.Y_AXIS));
        navMenu.setBackground(Theme.BG_PANEL);
        navMenu.setBorder(new EmptyBorder(10, 12, 10, 12));

        btnNavSim = createNavButton("SAR Live Sweep", true);
        btnNavSim.addActionListener(e -> switchView(VIEW_SIMULATION));

        btnNavReport = createNavButton("Mission Report", false);
        btnNavReport.addActionListener(e -> switchView(VIEW_REPORT));

        btnNavDatabase = createNavButton("Database Records", false);
        btnNavDatabase.addActionListener(e -> switchView(VIEW_DATABASE));

        navMenu.add(btnNavSim);
        navMenu.add(Box.createVerticalStrut(10));
        navMenu.add(btnNavReport);
        navMenu.add(Box.createVerticalStrut(10));
        navMenu.add(btnNavDatabase);

        // Bottom User Info & Logout
        JPanel userFooter = new JPanel();
        userFooter.setLayout(new BoxLayout(userFooter, BoxLayout.Y_AXIS));
        userFooter.setBackground(Theme.BG_PANEL);
        userFooter.setBorder(new EmptyBorder(16, 16, 20, 16));

        JLabel lblUser = new JLabel("Commander (Active)");
        lblUser.setFont(Theme.FONT_BODY);
        lblUser.setForeground(Theme.GREEN_ONLINE);

        JButton btnLogout = new JButton("Log Out");
        btnLogout.setFont(Theme.FONT_BODY);
        btnLogout.setForeground(Theme.RED_ALERT);
        btnLogout.setBackground(Theme.BG_CONTROL);
        btnLogout.setFocusPainted(false);
        btnLogout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                new EmptyBorder(7, 14, 7, 14)
        ));
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> logout());

        userFooter.add(lblUser);
        userFooter.add(Box.createVerticalStrut(10));
        userFooter.add(btnLogout);

        sidebar.add(brandBox, BorderLayout.NORTH);
        sidebar.add(navMenu, BorderLayout.CENTER);
        sidebar.add(userFooter, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createNavButton(String text, boolean active) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean hover = getModel().isRollover();
                boolean selected = (btnNavSim != null && this == btnNavSim && VIEW_SIMULATION.equals(currentView))
                        || (btnNavReport != null && this == btnNavReport && VIEW_REPORT.equals(currentView))
                        || (btnNavDatabase != null && this == btnNavDatabase && VIEW_DATABASE.equals(currentView));

                Color bg = selected ? Theme.BG_CARD_HOVER : (hover ? Theme.BG_CONTROL : Theme.BG_PANEL);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));

                if (selected) {
                    g2.setColor(Theme.CYAN_ACCENT);
                    g2.fillRect(0, 4, 3, getHeight() - 8);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(Theme.FONT_HEADING);
        btn.setForeground(active ? Theme.CYAN_ACCENT : Theme.TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(10, 14, 10, 14));
        btn.setMaximumSize(new Dimension(216, 42));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.repaint();
            }
        });

        return btn;
    }

    private void switchView(String viewName) {
        this.currentView = viewName;
        appCardLayout.show(appContentContainer, viewName);

        // Update nav button colors
        btnNavSim.setForeground(VIEW_SIMULATION.equals(viewName) ? Theme.CYAN_ACCENT : Theme.TEXT_PRIMARY);
        btnNavReport.setForeground(VIEW_REPORT.equals(viewName) ? Theme.CYAN_ACCENT : Theme.TEXT_PRIMARY);
        if (btnNavDatabase != null) {
            btnNavDatabase.setForeground(VIEW_DATABASE.equals(viewName) ? Theme.CYAN_ACCENT : Theme.TEXT_PRIMARY);
        }

        btnNavSim.repaint();
        btnNavReport.repaint();
        if (btnNavDatabase != null) {
            btnNavDatabase.repaint();
        }

        if (VIEW_SIMULATION.equals(viewName)) {
            controlBar.updatePlayPauseButton();
        } else if (VIEW_REPORT.equals(viewName)) {
            missionReportPanel.refreshReportData();
        } else if (VIEW_DATABASE.equals(viewName)) {
            databaseViewerPanel.refreshData();
        }
    }

    private void onLoginSuccess(String username) {
        rootCardLayout.show(rootCardContainer, CARD_MAIN_APP);
        isSimulationActive = true;
        setTitle("AERO-SAR Command Center - Logged in as " + username);
        switchView(VIEW_SIMULATION);
        SwingUtilities.invokeLater(this::openMissionSetupDialog);
    }

    public void openMissionSetupDialog() {
        controller.stopSimulation();
        MissionConfigDialog dialog = new MissionConfigDialog(this);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            int drones = dialog.getDroneCount();
            int persons = dialog.getPersonCount();
            controller.initDefaultScenario(drones, persons);
            statusPanel.resetSurvivorsTable();
            controlBar.resetAlertBanner();
            controlBar.updatePlayPauseButton();
            missionReportPanel.refreshReportData();
            databaseViewerPanel.refreshData();
            forestCanvas.repaint();
        }
    }

    public void showLoginScreen() {
        isSimulationActive = false;
        controller.stopSimulation();
        loginPanel.resetForm();
        rootCardLayout.show(rootCardContainer, CARD_LOGIN);
        setTitle("AERO-SAR Fleet Management - Operator Authentication");
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of the Command Center?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            showLoginScreen();
        }
    }

    public ForestCanvas getForestCanvas() {
        return forestCanvas;
    }

    public StatusPanel getStatusPanel() {
        return statusPanel;
    }
}
