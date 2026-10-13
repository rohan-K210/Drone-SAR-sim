package com.dronesar.gui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * Modern cyber dark theme scrollbar UI matching the tactical dashboard aesthetic.
 * Removes chunky native arrows and renders a slim, rounded track and thumb.
 */
public class ModernScrollBarUI extends BasicScrollBarUI {

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

    /**
     * Helper to apply sleek cyber scrollbars to any JScrollPane.
     */
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
