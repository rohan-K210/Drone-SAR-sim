package com.dronesar.gui;

import java.awt.Color;
import java.awt.Font;

/**
 * Tactical Cyber / Dark Theme color palette and fonts for Swing GUI.
 */
public final class Theme {
    private Theme() {}

    // Backgrounds
    public static final Color BG_DARK = new Color(13, 17, 23);         // #0D1117 (Deep Charcoal)
    public static final Color BG_PANEL = new Color(22, 27, 34);        // #161B22 (Card / Panel Surface)
    public static final Color BG_CANVAS = new Color(10, 14, 20);       // #0A0E14 (Tactical Radar Canvas)
    public static final Color BG_CONTROL = new Color(33, 38, 45);      // #21262D (Button / Control base)

    // Borders & Grid Lines
    public static final Color BORDER_SUBTLE = new Color(48, 54, 61);   // #30363D
    public static final Color GRID_LINE = new Color(24, 32, 44);       // Faint radar grid
    public static final Color GRID_ACCENT = new Color(38, 50, 68);

    // Accents & Telemetry Colors
    public static final Color CYAN_ACCENT = new Color(0, 229, 255);    // #00E5FF (Electric Cyan)
    public static final Color GREEN_ONLINE = new Color(63, 185, 80);   // #3FB950 (Emerald Green)
    public static final Color AMBER_WARN = new Color(245, 158, 11);    // #F59E0B (Amber Warning)
    public static final Color RED_ALERT = new Color(248, 81, 73);      // #F85149 (Incident / SOS Red)
    public static final Color PURPLE_RELAY = new Color(163, 113, 247); // #A371F7 (Comms Wave)
    public static final Color BLUE_TOWER = new Color(88, 166, 255);    // #58A6FF (Tower Base)

    // Typography Colors
    public static final Color TEXT_PRIMARY = new Color(240, 246, 252); // #F0F6FC
    public static final Color TEXT_SECONDARY = new Color(139, 148, 158);// #8B949E
    public static final Color TEXT_MUTED = new Color(101, 109, 118);

    // Fonts
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 15);
    public static final Font FONT_HEADING = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 11);
    public static final Font FONT_MONO_BOLD = new Font("Monospaced", Font.BOLD, 12);
}
