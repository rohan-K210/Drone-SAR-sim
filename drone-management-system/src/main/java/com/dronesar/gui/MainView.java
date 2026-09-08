package com.dronesar.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Main JavaFX Application launcher for the SAR Drone Fleet Simulation GUI.
 */
public class MainView extends Application {
    private static final double WINDOW_WIDTH = 1260.0;
    private static final double WINDOW_HEIGHT = 700.0;

    private DashboardView dashboardView;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Drone SAR Fleet Management System | Tactical Command & Map");

        dashboardView = new DashboardView();

        Scene scene = new Scene(dashboardView, WINDOW_WIDTH, WINDOW_HEIGHT);
        URL cssUrl = getClass().getResource("/styles/dark-theme.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (dashboardView != null) {
            dashboardView.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
