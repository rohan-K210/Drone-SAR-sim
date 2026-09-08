package com.dronesar.model.enums;

public enum DroneState {

    IDLE("Idle", "#94a3b8"),

    SEARCHING("Searching Zone", "#10b981"),

    PERSON_FOUND("Person Found", "#ef4444"),

    RELAYING("Relaying Mesh", "#3b82f6"),

    SWEEP_COMPLETE("Sweep Complete", "#eab308"),

    OFFLINE("Offline / Fault", "#dc2626");

    private final String displayName;
    private final String hexColor;

    DroneState() {
        this.displayName = name();
        this.hexColor = "#94a3b8";
    }

    DroneState(String displayName, String hexColor) {
        this.displayName = displayName;
        this.hexColor = hexColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHexColor() {
        return hexColor;
    }
}