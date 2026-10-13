package com.dronesar.gui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Tactical Cyber / Dark Theme color palette, typography tokens,
 * and custom UI components (ModernScrollBarUI & CyberTabbedPaneUI).
 */
public final class Theme {
    private Theme() {}

    // Backgrounds
    public static final Color BG_DARK = new Color(13, 17, 23);         // #0D1117 (Deep Charcoal)
    public static final Color BG_PANEL = new Color(22, 27, 34);        // #161B22 (Card / Panel Surface)
    public static final Color BG_CANVAS = new Color(10, 14, 20);       // #0A0E14 (Tactical Radar Canvas)
    public static final Color BG_CONTROL = new Color(33, 38, 45);      // #21262D (Button / Control base)
    public static final Color BG_CARD = new Color(21, 28, 38);         // Sleek card surface
    public static final Color BG_CARD_HOVER = new Color(28, 38, 52);   // Card surface on hover

    // Borders & Grid Lines
    public static final Color BORDER_SUBTLE = new Color(48, 54, 61);   // #30363D
    public static final Color BORDER_CYAN = new Color(0, 229, 255, 120);
    public static final Color GRID_LINE = new Color(24, 32, 44);       // Faint radar grid
    public static final Color GRID_ACCENT = new Color(38, 50, 68);

    // Accents & Telemetry Colors
    public static final Color CYAN_ACCENT = new Color(0, 229, 255);    // #00E5FF (Electric Cyan)
    public static final Color CYAN_GLOW = new Color(0, 229, 255, 45);
    public static final Color GREEN_ONLINE = new Color(63, 185, 80);   // #3FB950 (Emerald Green)
    public static final Color GREEN_GLOW = new Color(63, 185, 80, 45);
    public static final Color AMBER_WARN = new Color(245, 158, 11);    // #F59E0B (Amber Warning)
    public static final Color RED_ALERT = new Color(248, 81, 73);      // #F85149 (Incident / SOS Red)
    public static final Color PURPLE_RELAY = new Color(163, 113, 247); // #A371F7 (Comms Wave)
    public static final Color BLUE_TOWER = new Color(88, 166, 255);    // #58A6FF (Tower Base)
    public static final Color GOLD_ACCENT = new Color(251, 191, 36);   // #FBBF24

    // Typography Colors
    public static final Color TEXT_PRIMARY = new Color(240, 246, 252); // #F0F6FC
    public static final Color TEXT_SECONDARY = new Color(139, 148, 158);// #8B949E
    public static final Color TEXT_MUTED = new Color(101, 109, 118);

    // Fonts (Enlarged for High Visibility Presentation)
    public static final Font FONT_HERO = new Font("SansSerif", Font.BOLD, 34);
    public static final Font FONT_SUBHERO = new Font("SansSerif", Font.BOLD, 24);
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 20);
    public static final Font FONT_HEADING = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 15);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 14);
    public static final Font FONT_MONO_BOLD = new Font("Monospaced", Font.BOLD, 15);

    /**
     * Applies sleek cyber scrollbars to any JScrollPane.
     */
    public static void applyModernScrollBar(JScrollPane scrollPane) {
        ModernScrollBarUI.apply(scrollPane);
    }

    // =========================================================================
    // Nested UI Component: ModernScrollBarUI
    // =========================================================================

    /**
     * Modern cyber dark theme scrollbar UI matching the tactical dashboard aesthetic.
     */
    public static class ModernScrollBarUI extends BasicScrollBarUI {

        public static ComponentUI createUI(JComponent c) {
            return new ModernScrollBarUI();
        }

        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = Theme.BORDER_SUBTLE;
            this.thumbHighlightColor = Theme.CYAN_ACCENT;
            this.trackColor = Theme.BG_DARK;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            btn.setMinimumSize(new Dimension(0, 0));
            btn.setMaximumSize(new Dimension(0, 0));
            return btn;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(Theme.BG_DARK);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean isHover = isThumbRollover();
            g2.setColor(isHover ? Theme.CYAN_ACCENT : Theme.BORDER_SUBTLE);
            g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 2, Math.max(4, thumbBounds.width - 2), Math.max(8, thumbBounds.height - 4), 6, 6);
            g2.dispose();
        }

        public static void apply(JScrollPane scrollPane) {
            if (scrollPane == null) return;
            scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
            scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
            scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
            scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 8));
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        }
    }

    // =========================================================================
    // Nested UI Component: CyberTabbedPaneUI
    // =========================================================================

    /**
     * Cyberpunk / Tactical Dark Glass TabbedPane UI with cyan active highlight bar.
     */
    public static class CyberTabbedPaneUI extends BasicTabbedPaneUI {

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
                GradientPaint glassGrad = new GradientPaint(
                        x, y, new Color(34, 48, 68, 235),
                        x, y + h, new Color(18, 26, 38, 245)
                );
                g2.setPaint(glassGrad);
                g2.fill(new RoundRectangle2D.Double(x + 1, y + 2, w - 2, h - 2, 8, 8));

                g2.setColor(new Color(255, 255, 255, 30));
                g2.drawLine(x + 4, y + 3, x + w - 5, y + 3);

                g2.setColor(Theme.CYAN_ACCENT);
                g2.fillRect(x + 3, y + h - 3, w - 6, 3);
            } else {
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

            g2.setFont(Theme.FONT_HEADING);
            if (isSelected) {
                g2.setColor(Theme.CYAN_ACCENT);
            } else {
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
            int tabAreaHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
            g2.drawLine(0, tabAreaHeight - 1, tabPane.getWidth(), tabAreaHeight - 1);
            g2.dispose();
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        }
    }
}
