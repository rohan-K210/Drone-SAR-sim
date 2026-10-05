package com.dronesar;

import com.dronesar.gui.MainView;

/**
 * Entry point for the Drone SAR Fleet Management System.
 * Delegates to MainView, which is the JavaFX Application class.
 */
public final class Main {

    private Main() {
        // utility class
    }

    public static void main(String[] args) {
        MainView.main(args);
    }
}
