package com.dronesar.model.enums;

/**
 * Types of radio signals exchanged over the drone mesh network
 * and between drones and the central tower.
 */
public enum SignalType {

    /** Periodic status ping confirming a drone is alive and operational. */
    HEARTBEAT("Heartbeat", "#94a3b8"),

    /** Routine telemetry: position, battery level and search progress. */
    TELEMETRY("Telemetry", "#06b6d4"),

    /** Alert raised when a drone locates a missing person. */
    DETECTION("Detection Alert", "#ef4444"),

    /** Urgent call for help, e.g. critical battery or drone fault. */
    DISTRESS("Distress Call", "#f97316"),

    /** Packet forwarded through mesh nodes toward the tower. */
    RELAY("Mesh Relay", "#3b82f6"),

    /** Acknowledgement of a received signal or command. */
    ACK("Acknowledgement", "#10b981"),

    /** Instruction issued by the tower to a drone (e.g. new zone, return home). */
    COMMAND("Tower Command", "#8b5cf6"),

    /** Tower beacon pulse advertising its position and reception radius. */
    BEACON("Tower Beacon", "#eab308");

    private final String displayName;
    private final String hexColor;

    SignalType(String displayName, String hexColor) {
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
