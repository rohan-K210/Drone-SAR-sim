package com.dronesar.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * Simple, professional, and visually attractive Login Screen for the SAR Drone System.
 * Operates standalone without requiring an external backend database.
 */
public class LoginPanel extends JPanel {

    // Default demonstration credentials
    public static final String DEFAULT_USER = "commander";
    public static final String DEFAULT_PASS = "sar2026";

    private final JTextField txtUsername;
    private final JPasswordField txtPassword;
    private final JLabel lblErrorMessage;
    private final Consumer<String> onLoginSuccess;

    public LoginPanel(Consumer<String> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;

        setLayout(new GridBagLayout());
        setBackground(Theme.BG_DARK);

        // Center card container
        JPanel loginCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Card surface
                g2.setColor(Theme.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));

                // Border
                g2.setColor(Theme.BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));

                // Top Cyan Accent line
                g2.setColor(Theme.CYAN_ACCENT);
                g2.fillRect(30, 0, getWidth() - 60, 3);

                g2.dispose();
            }
        };
        loginCard.setLayout(new BoxLayout(loginCard, BoxLayout.Y_AXIS));
        loginCard.setBackground(Theme.BG_DARK);
        loginCard.setBorder(new EmptyBorder(32, 42, 32, 42));
        loginCard.setPreferredSize(new Dimension(440, 520));

        // 1. Icon Badge
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                // Glowing rings
                g2.setColor(new Color(0, 229, 255, 30));
                g2.fill(new Ellipse2D.Double(cx - 30, cy - 30, 60, 60));
                g2.setColor(Theme.CYAN_ACCENT);
                g2.setStroke(new BasicStroke(2.0f));
                g2.draw(new Ellipse2D.Double(cx - 22, cy - 22, 44, 44));

                // Drone Crosshair
                g2.setColor(Theme.GREEN_ONLINE);
                g2.drawLine(cx - 14, cy, cx + 14, cy);
                g2.drawLine(cx, cy - 14, cx, cy + 14);
                g2.setColor(Color.WHITE);
                g2.fillOval(cx - 4, cy - 4, 8, 8);
                g2.dispose();
            }
        };
        iconPanel.setPreferredSize(new Dimension(64, 64));
        iconPanel.setMaximumSize(new Dimension(64, 64));
        iconPanel.setBackground(Theme.BG_CARD);
        iconPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginCard.add(iconPanel);
        loginCard.add(Box.createVerticalStrut(10));

        // 2. Titles
        JLabel title = new JLabel("AERO-SAR PORTAL");
        title.setFont(Theme.FONT_SUBHERO);
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginCard.add(title);

        JLabel subtitle = new JLabel("Drone SAR Fleet Operations Command");
        subtitle.setFont(Theme.FONT_SMALL);
        subtitle.setForeground(Theme.CYAN_ACCENT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginCard.add(subtitle);
        loginCard.add(Box.createVerticalStrut(22));

        // 3. Username Field
        JLabel lblUser = new JLabel("Operator Callsign / Username:");
        lblUser.setFont(Theme.FONT_HEADING);
        lblUser.setForeground(Theme.TEXT_SECONDARY);
        lblUser.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginCard.add(lblUser);
        loginCard.add(Box.createVerticalStrut(6));

        txtUsername = createInputField();
        txtUsername.setText(DEFAULT_USER);
        loginCard.add(txtUsername);
        loginCard.add(Box.createVerticalStrut(14));

        // 4. Password Field
        JLabel lblPass = new JLabel("Access Key / Password:");
        lblPass.setFont(Theme.FONT_HEADING);
        lblPass.setForeground(Theme.TEXT_SECONDARY);
        lblPass.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginCard.add(lblPass);
        loginCard.add(Box.createVerticalStrut(6));

        txtPassword = createPasswordField();
        txtPassword.setText(DEFAULT_PASS);
        loginCard.add(txtPassword);
        loginCard.add(Box.createVerticalStrut(8));

        // 5. Error message label
        lblErrorMessage = new JLabel(" ");
        lblErrorMessage.setFont(Theme.FONT_SMALL);
        lblErrorMessage.setForeground(Theme.RED_ALERT);
        lblErrorMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginCard.add(lblErrorMessage);
        loginCard.add(Box.createVerticalStrut(10));

        // 6. Login Button
        JButton btnLogin = new JButton("ACCESS COMMAND CENTER") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                boolean pressed = getModel().isPressed();

                g2.setColor(pressed ? new Color(0, 180, 200) : (hover ? new Color(0, 240, 255) : new Color(0, 215, 235)));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLogin.setForeground(new Color(10, 14, 20));
        btnLogin.setFocusPainted(false);
        btnLogin.setContentAreaFilled(false);
        btnLogin.setBorder(new EmptyBorder(12, 20, 12, 20));
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(350, 44));
        btnLogin.addActionListener(e -> attemptLogin());

        // Press Enter to login
        KeyAdapter enterKey = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    attemptLogin();
                }
            }
        };
        txtUsername.addKeyListener(enterKey);
        txtPassword.addKeyListener(enterKey);

        loginCard.add(btnLogin);
        loginCard.add(Box.createVerticalStrut(16));

        // 7. Demo Helper Tip
        JLabel hint = new JLabel("Demo Access: commander / sar2026");
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginCard.add(hint);

        add(loginCard);
    }

    private JTextField createInputField() {
        JTextField field = new JTextField();
        field.setFont(Theme.FONT_BODY);
        field.setBackground(Theme.BG_CONTROL);
        field.setForeground(Theme.TEXT_PRIMARY);
        field.setCaretColor(Theme.CYAN_ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        field.setMaximumSize(new Dimension(350, 38));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        return field;
    }

    private JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(Theme.FONT_BODY);
        field.setBackground(Theme.BG_CONTROL);
        field.setForeground(Theme.TEXT_PRIMARY);
        field.setCaretColor(Theme.CYAN_ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        field.setMaximumSize(new Dimension(350, 38));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        return field;
    }

    private void attemptLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblErrorMessage.setText("Please enter both username and password");
            return;
        }

        // Accepts default demo credentials or any non-empty input for ease of presentation
        if (DEFAULT_USER.equalsIgnoreCase(username) && DEFAULT_PASS.equals(password)) {
            lblErrorMessage.setText(" ");
            if (onLoginSuccess != null) {
                onLoginSuccess.accept(username);
            }
        } else if ("admin".equalsIgnoreCase(username) && "admin".equals(password)) {
            lblErrorMessage.setText(" ");
            if (onLoginSuccess != null) {
                onLoginSuccess.accept(username);
            }
        } else {
            lblErrorMessage.setText("Invalid credentials. Try: commander / sar2026");
        }
    }

    public void resetForm() {
        txtUsername.setText(DEFAULT_USER);
        txtPassword.setText(DEFAULT_PASS);
        lblErrorMessage.setText(" ");
    }
}
