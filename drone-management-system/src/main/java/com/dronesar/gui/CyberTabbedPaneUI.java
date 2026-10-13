package com.dronesar.gui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Cyberpunk / Tactical Dark Glass TabbedPane UI.
 * Features:
 *  - Frosted translucent glass effect when tabs are selected
 *  - Electric cyan active tab highlight bar
 *  - High-contrast legible typography
 *  - Seamless border matching Theme.BG_DARK
 */
public class CyberTabbedPaneUI extends BasicTabbedPaneUI {

    public static ComponentUI createUI(JComponent c) {
        return new CyberTabbedPaneUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        highlight = Theme.BORDER_SUBTLE;
        lightHighlight = Theme.BORDER_SUBTLE;
        shadow = Theme.BG_DARK;
        darkShadow = Theme.BG_DARK;
        focus = Theme.CYAN_ACCENT;
    }

    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        return new Insets(8, 20, 8, 20);
    }

    @Override
    protected Insets getSelectedTabPadInsets(int tabPlacement) {
        return new Insets(0, 0, 0, 0);
    }

    @Override
    protected Insets getContentBorderInsets(int tabPlacement) {
        return new Insets(4, 0, 0, 0);
    }

    @Override
    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        return Math.max(38, fontHeight + 18);
    }

    @Override
    protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
        return super.calculateTabWidth(tabPlacement, tabIndex, metrics) + 24;
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isSelected) {
            // Frosted Glass Effect: translucent layered cyan-tinted slate gradient
            GradientPaint glassGrad = new GradientPaint(
                    x, y, new Color(34, 48, 68, 235),
                    x, y + h, new Color(18, 26, 38, 245)
            );
            g2.setPaint(glassGrad);
            g2.fill(new RoundRectangle2D.Double(x + 1, y + 2, w - 2, h - 2, 8, 8));

            // Glass inner shine reflection
            g2.setColor(new Color(255, 255, 255, 30));
            g2.drawLine(x + 4, y + 3, x + w - 5, y + 3);

            // Active cyan indicator underline
            g2.setColor(Theme.CYAN_ACCENT);
            g2.fillRect(x + 3, y + h - 3, w - 6, 3);
        } else {
            // Unselected tab: subtle dark background
            g2.setColor(new Color(21, 26, 34, 190));
            g2.fill(new RoundRectangle2D.Double(x + 1, y + 4, w - 2, h - 4, 6, 6));
        }

        g2.dispose();
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isSelected) {
            // Cyan illuminated border
            g2.setColor(Theme.CYAN_ACCENT);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Double(x + 1, y + 2, w - 3, h - 3, 8, 8));
        } else {
            g2.setColor(Theme.BORDER_SUBTLE);
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new RoundRectangle2D.Double(x + 1, y + 4, w - 3, h - 5, 6, 6));
        }

        g2.dispose();
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // High contrast readable typography
        g2.setFont(Theme.FONT_HEADING);
        if (isSelected) {
            // Glowing Electric Cyan text when active
            g2.setColor(Theme.CYAN_ACCENT);
        } else {
            // Crisp silver secondary text when inactive
            g2.setColor(Theme.TEXT_SECONDARY);
        }

        int textY = textRect.y + metrics.getAscent() + (isSelected ? 0 : 1);
        g2.drawString(title, textRect.x, textY);
        g2.dispose();
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Theme.BORDER_SUBTLE);
        g2.setStroke(new BasicStroke(1.0f));
        // Draw sleek hairline separator between tabs and content panel
        int tabAreaHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
        g2.drawLine(0, tabAreaHeight - 1, tabPane.getWidth(), tabAreaHeight - 1);
        g2.dispose();
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        // No dashed focus indicator for clean aesthetic
    }
}
