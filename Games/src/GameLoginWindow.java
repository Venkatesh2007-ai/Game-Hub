import games.ScoreService;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

public class GameLoginWindow extends JFrame {

    private static final Font TITLE_FONT = new Font("Dialog", Font.BOLD, 64);
    private static final Font SUBTITLE_FONT = new Font("Dialog", Font.PLAIN, 22);
    private static final Font FIELD_LABEL_FONT = new Font("Dialog", Font.BOLD, 20);
    private static final Font FIELD_FONT = new Font("Dialog", Font.PLAIN, 21);
    private static final Font STATUS_FONT = new Font("Dialog", Font.BOLD, 16);

    private final GraphicsDevice graphicsDevice;
    private final ArcadeThemeBackgroundPanel backgroundPanel;
    private final NeonActionButton loginButton;
    private final NeonActionButton signupButton;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JLabel statusLabel;
    private final Timer buttonAnimationTimer;

    private float buttonPhase;
    private boolean launchQueued;

    public GameLoginWindow() {
        setTitle("GameHub Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);

        graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        backgroundPanel = new ArcadeThemeBackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout());
        backgroundPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        setContentPane(backgroundPanel);

        buttonPhase = 0f;
        launchQueued = false;
        buttonAnimationTimer = new Timer(33, this::animateButtons);

        JPanel shell = new JPanel();
        shell.setOpaque(false);
        shell.setLayout(new BoxLayout(shell, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("GAMEHUB ACCESS", SwingConstants.CENTER);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(new Color(118, 241, 255));
        shell.add(titleLabel);

        JLabel subtitleLabel = new JLabel("Login or Sign Up to Enter the Arcade", SwingConstants.CENTER);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);
        subtitleLabel.setFont(SUBTITLE_FONT);
        subtitleLabel.setForeground(new Color(255, 136, 198));
        shell.add(subtitleLabel);
        shell.add(Box.createVerticalStrut(18));

        JPanel card = new JPanel(new BorderLayout(16, 16));
        card.setOpaque(true);
        card.setBackground(new Color(8, 15, 35, 224));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(92, 236, 255), 2, true),
            BorderFactory.createEmptyBorder(22, 26, 22, 26)
        ));
        card.setMaximumSize(new Dimension(680, 440));
        card.setPreferredSize(new Dimension(680, 440));

        JPanel formPanel = new JPanel(new GridLayout(0, 1, 8, 8));
        formPanel.setOpaque(false);

        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(FIELD_LABEL_FONT);
        usernameLabel.setForeground(new Color(193, 230, 255));
        formPanel.add(usernameLabel);

        usernameField = new JTextField();
        styleInput(usernameField);
        formPanel.add(usernameField);

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(FIELD_LABEL_FONT);
        passwordLabel.setForeground(new Color(193, 230, 255));
        formPanel.add(passwordLabel);

        passwordField = new JPasswordField();
        styleInput(passwordField);
        formPanel.add(passwordField);

        statusLabel = new JLabel("Use Login for existing account, Sign Up for new account", SwingConstants.CENTER);
        statusLabel.setFont(STATUS_FONT);
        statusLabel.setForeground(new Color(187, 217, 255));
        formPanel.add(statusLabel);
        card.add(formPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 14, 0));
        actions.setOpaque(false);

        loginButton = new NeonActionButton(
            "LOGIN",
            new Color(67, 224, 255),
            new Color(69, 136, 255),
            new Color(85, 238, 255),
            0.0f
        );
        loginButton.setFont(new Font("Dialog", Font.BOLD, 25));
        loginButton.setForeground(Color.WHITE);
        loginButton.addActionListener(e -> attemptLogin());

        signupButton = new NeonActionButton(
            "SIGN UP",
            new Color(255, 145, 87),
            new Color(255, 97, 167),
            new Color(255, 175, 104),
            1.4f
        );
        signupButton.setFont(new Font("Dialog", Font.BOLD, 25));
        signupButton.setForeground(Color.WHITE);
        signupButton.addActionListener(e -> attemptSignup());

        actions.add(loginButton);
        actions.add(signupButton);
        card.add(actions, BorderLayout.SOUTH);

        shell.add(card);
        shell.add(Box.createVerticalStrut(12));

        JLabel hintLabel = new JLabel("ENTER = Login | CTRL+ENTER = Sign Up | ESC = Exit", SwingConstants.CENTER);
        hintLabel.setAlignmentX(CENTER_ALIGNMENT);
        hintLabel.setFont(new Font("Dialog", Font.PLAIN, 15));
        hintLabel.setForeground(new Color(181, 219, 255));
        shell.add(hintLabel);

        JPanel centerHolder = new JPanel(new BorderLayout());
        centerHolder.setOpaque(false);
        centerHolder.add(shell, BorderLayout.CENTER);
        backgroundPanel.add(centerHolder, BorderLayout.CENTER);

        registerShortcuts();
        buttonAnimationTimer.start();
        launchFullscreen();
        SwingUtilities.invokeLater(usernameField::requestFocusInWindow);
    }

    private void styleInput(JTextField field) {
        field.setFont(FIELD_FONT);
        field.setForeground(new Color(222, 243, 255));
        field.setBackground(new Color(14, 28, 58));
        field.setCaretColor(new Color(95, 225, 255));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(72, 186, 255), 2, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }

    private void registerShortcuts() {
        getRootPane().setDefaultButton(loginButton);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("control ENTER"), "signup-shortcut");
        getRootPane().getActionMap().put("signup-shortcut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                attemptSignup();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "exit-login");
        getRootPane().getActionMap().put("exit-login", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
    }

    private void animateButtons(ActionEvent event) {
        buttonPhase += 0.06f;
        loginButton.setAnimationPhase(buttonPhase);
        signupButton.setAnimationPhase(buttonPhase);
    }

    private void attemptLogin() {
        if (launchQueued) {
            return;
        }

        String user = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (user.isBlank() || password.isBlank()) {
            setStatus("Enter both username and password", new Color(255, 133, 133));
            return;
        }

        if (!ScoreService.login(user, password)) {
            setStatus("Login failed. Check credentials or use Sign Up", new Color(255, 133, 133));
            return;
        }

        setStatus("Login successful. Launching arcade...", new Color(122, 255, 170));
        launchGameHub();
    }

    private void attemptSignup() {
        if (launchQueued) {
            return;
        }

        String user = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (user.isBlank() || password.isBlank()) {
            setStatus("Username and password are required for sign up", new Color(255, 133, 133));
            return;
        }

        if (!ScoreService.signup(user, password)) {
            setStatus("Sign up failed. Username already exists", new Color(255, 133, 133));
            return;
        }

        setStatus("Account created. Entering arcade...", new Color(122, 255, 170));
        launchGameHub();
    }

    private void launchGameHub() {
        launchQueued = true;
        loginButton.setEnabled(false);
        signupButton.setEnabled(false);

        Timer launchTimer = new Timer(320, e -> {
            ((Timer) e.getSource()).stop();
            dispose();
            new GameHub();
        });
        launchTimer.setRepeats(false);
        launchTimer.start();
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private void launchFullscreen() {
        if (graphicsDevice != null && graphicsDevice.isFullScreenSupported()) {
            graphicsDevice.setFullScreenWindow(this);
            setVisible(true);
            validate();
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setVisible(true);
        }
    }

    @Override
    public void dispose() {
        buttonAnimationTimer.stop();
        backgroundPanel.stopAnimation();
        if (graphicsDevice != null && graphicsDevice.getFullScreenWindow() == this) {
            graphicsDevice.setFullScreenWindow(null);
        }
        super.dispose();
    }

    private static Color blend(Color from, Color to, float ratio) {
        float clamped = Math.max(0f, Math.min(1f, ratio));
        int r = Math.round(from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int g = Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int b = Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        return new Color(r, g, b);
    }

    private static class NeonActionButton extends JButton {
        private final Color primary;
        private final Color secondary;
        private final Color glow;
        private final float offset;

        private float animationPhase;
        private boolean hovered;

        private NeonActionButton(String text, Color primary, Color secondary, Color glow, float offset) {
            super(text);
            this.primary = primary;
            this.secondary = secondary;
            this.glow = glow;
            this.offset = offset;
            this.animationPhase = offset;
            this.hovered = false;

            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(280, 64));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        private void setAnimationPhase(float base) {
            animationPhase = base + offset;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2d = (Graphics2D) graphics.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int arc = 22;
            float pulse = 0.5f + 0.5f * (float) Math.sin(animationPhase);
            float hoverBoost = hovered ? 0.24f : 0f;

            Color top = blend(new Color(8, 16, 42), primary, 0.40f + pulse * 0.28f);
            Color bottom = blend(new Color(6, 10, 29), secondary, 0.34f + pulse * 0.22f);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.24f + hoverBoost));
            g2d.setColor(glow);
            g2d.fillRoundRect(6, 8, width - 12, height - 12, arc + 2, arc + 2);

            g2d.setComposite(AlphaComposite.SrcOver);
            g2d.setPaint(new GradientPaint(0, 0, top, 0, height, bottom));
            g2d.fillRoundRect(0, 0, width - 1, height - 1, arc, arc);

            int sheenWidth = Math.max(34, width / 6);
            int travel = width + sheenWidth + 40;
            int sheenX = -sheenWidth - 20 + (int) ((animationPhase % (Math.PI * 2)) / (Math.PI * 2) * travel);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
            g2d.setColor(new Color(255, 255, 255, 192));
            g2d.fillRoundRect(sheenX, 6, sheenWidth, height - 10, 18, 18);
            g2d.setComposite(AlphaComposite.SrcOver);

            g2d.setStroke(new BasicStroke(2.6f));
            g2d.setColor(blend(glow, Color.WHITE, 0.36f + pulse * 0.35f + hoverBoost));
            g2d.drawRoundRect(2, 2, width - 5, height - 5, arc, arc);

            super.paintComponent(g2d);
            g2d.dispose();
        }
    }

    private static class ArcadeThemeBackgroundPanel extends JPanel {
        private static final int STAR_COUNT = 150;
        private static final int ORB_COUNT = 38;
        private static final int ACCESSORY_COUNT = 6;

        private final Random random;
        private final Star[] stars;
        private final Orb[] orbs;
        private final Accessory[] accessories;
        private final Timer animationTimer;

        private float phase;
        private float gridShift;
        private boolean initialized;

        private ArcadeThemeBackgroundPanel() {
            random = new Random();
            stars = new Star[STAR_COUNT];
            orbs = new Orb[ORB_COUNT];
            accessories = new Accessory[ACCESSORY_COUNT];

            for (int i = 0; i < STAR_COUNT; i++) {
                stars[i] = new Star();
            }
            for (int i = 0; i < ORB_COUNT; i++) {
                orbs[i] = new Orb();
            }

            accessories[0] = new Accessory(0, 0.08f, 0.20f, 1.0f, 20f, 16f, 0f);
            accessories[1] = new Accessory(1, 0.89f, 0.22f, 0.95f, 18f, 14f, 1.6f);
            accessories[2] = new Accessory(2, 0.10f, 0.77f, 1.0f, 18f, 15f, 2.5f);
            accessories[3] = new Accessory(0, 0.89f, 0.77f, 1.05f, 22f, 17f, 3.4f);
            accessories[4] = new Accessory(3, 0.50f, 0.12f, 1.0f, 12f, 10f, 4.3f);
            accessories[5] = new Accessory(2, 0.50f, 0.86f, 1.06f, 14f, 12f, 5.2f);

            animationTimer = new Timer(33, this::advanceAnimation);
            animationTimer.start();
        }

        private void stopAnimation() {
            animationTimer.stop();
        }

        private void initializeIfNeeded(int width, int height) {
            if (initialized) {
                return;
            }

            for (Star star : stars) {
                resetStar(star, width, height, true);
            }

            for (Orb orb : orbs) {
                resetOrb(orb, width, height, true);
            }

            initialized = true;
        }

        private void advanceAnimation(ActionEvent event) {
            int width = Math.max(1, getWidth());
            int height = Math.max(1, getHeight());
            initializeIfNeeded(width, height);

            for (Star star : stars) {
                star.y += star.speed;
                star.x += Math.sin((phase + star.phase) * 0.65f) * 0.16f;
                if (star.y > height + 16) {
                    resetStar(star, width, height, false);
                }
            }

            for (Orb orb : orbs) {
                orb.y -= orb.speed;
                orb.x += Math.sin((phase * orb.waveSpeed) + orb.phase) * orb.waveAmount;
                if (orb.y < -orb.size - 20) {
                    resetOrb(orb, width, height, false);
                }
            }

            phase += 0.050f;
            gridShift = (gridShift + 2.0f) % 84f;
            repaint();
        }

        private void resetStar(Star star, int width, int height, boolean randomY) {
            star.x = random.nextFloat() * width;
            star.y = randomY ? random.nextFloat() * height : -random.nextInt(130) - 10;
            star.size = 1 + random.nextInt(3);
            star.speed = 0.45f + random.nextFloat() * 1.5f;
            star.alpha = 70 + random.nextInt(120);
            star.phase = random.nextFloat() * 6.2f;
        }

        private void resetOrb(Orb orb, int width, int height, boolean randomY) {
            orb.size = 10 + random.nextInt(26);
            orb.x = random.nextFloat() * width;
            orb.y = randomY ? random.nextFloat() * height : height + random.nextInt(160);
            orb.speed = 0.4f + random.nextFloat() * 1.4f;
            orb.alpha = 30 + random.nextInt(70);
            orb.phase = random.nextFloat() * 6.2f;
            orb.waveAmount = 0.3f + random.nextFloat();
            orb.waveSpeed = 0.5f + random.nextFloat();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2d = (Graphics2D) graphics.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            initializeIfNeeded(width, height);

            drawBase(g2d, width, height);
            drawGlow(g2d, width, height);
            drawGrid(g2d, width, height);
            drawStars(g2d);
            drawOrbs(g2d);
            drawAccessories(g2d, width, height);
            drawScanlines(g2d, width, height);

            g2d.dispose();
        }

        private void drawBase(Graphics2D g2d, int width, int height) {
            GradientPaint base = new GradientPaint(
                0, 0, new Color(7, 10, 27),
                0, height, new Color(18, 8, 43)
            );
            g2d.setPaint(base);
            g2d.fillRect(0, 0, width, height);
        }

        private void drawGlow(Graphics2D g2d, int width, int height) {
            int r1 = 240 + (int) (Math.sin(phase) * 20);
            int r2 = 180 + (int) (Math.cos(phase * 1.4f) * 18);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
            g2d.setColor(new Color(255, 84, 178));
            g2d.fillOval((width / 2) - r1, (height / 2) - r1 - 36, r1 * 2, r1 * 2);

            g2d.setColor(new Color(64, 223, 255));
            g2d.fillOval((width / 2) - r2, (height / 2) - r2 - 8, r2 * 2, r2 * 2);

            g2d.setColor(new Color(255, 178, 89));
            g2d.fillOval(70, 84, 140, 140);
            g2d.fillOval(width - 220, height - 230, 150, 150);
            g2d.setComposite(AlphaComposite.SrcOver);
        }

        private void drawGrid(Graphics2D g2d, int width, int height) {
            int horizonY = Math.max(height / 3, 190);
            g2d.setStroke(new BasicStroke(1.2f));

            for (int i = 0; i <= 11; i++) {
                float p = i / 11f;
                int y = horizonY + (int) (Math.pow(p, 1.7) * (height - horizonY));
                int alpha = 46 + (int) (p * 102);
                int wave = (int) Math.round(Math.sin((gridShift + i * 7.0f) * 0.1) * 10);
                g2d.setColor(new Color(93, 232, 255, alpha));
                g2d.drawLine(wave, y, width + wave, y);
            }

            for (int x = -width; x < width * 2; x += 82) {
                int bottomX = x + Math.round(gridShift);
                int topX = (width / 2) + (int) ((bottomX - (width / 2.0)) * 0.15);
                g2d.setColor(new Color(255, 113, 195, 114));
                g2d.drawLine(bottomX, height, topX, horizonY);
            }
        }

        private void drawStars(Graphics2D g2d) {
            for (Star star : stars) {
                g2d.setColor(new Color(194, 233, 255, star.alpha));
                g2d.fillOval(Math.round(star.x), Math.round(star.y), star.size, star.size);
            }
        }

        private void drawOrbs(Graphics2D g2d) {
            for (Orb orb : orbs) {
                int x = Math.round(orb.x);
                int y = Math.round(orb.y);
                g2d.setColor(new Color(255, 255, 255, orb.alpha));
                g2d.fillOval(x, y, orb.size, orb.size);
                g2d.setColor(new Color(180, 233, 255, Math.min(255, orb.alpha + 28)));
                g2d.drawOval(x, y, orb.size, orb.size);
            }
        }

        private void drawAccessories(Graphics2D g2d, int width, int height) {
            for (Accessory accessory : accessories) {
                float px = (width * accessory.xRatio)
                    + (float) Math.sin((phase * accessory.speed) + accessory.phase) * accessory.wobbleX;
                float py = (height * accessory.yRatio)
                    + (float) Math.cos((phase * accessory.speed * 0.9f) + accessory.phase) * accessory.wobbleY;

                if (accessory.type == 0) {
                    drawController(g2d, px, py, accessory.scale);
                } else if (accessory.type == 1) {
                    drawJoystick(g2d, px, py, accessory.scale);
                } else if (accessory.type == 2) {
                    drawHeadset(g2d, px, py, accessory.scale);
                } else {
                    drawDPad(g2d, px, py, accessory.scale);
                }
            }
        }

        private void drawController(Graphics2D g2d, float x, float y, float scale) {
            int w = Math.round(136 * scale);
            int h = Math.round(72 * scale);
            int px = Math.round(x - w / 2f);
            int py = Math.round(y - h / 2f);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
            g2d.setColor(new Color(91, 228, 255));
            g2d.fillRoundRect(px - 8, py - 6, w + 16, h + 12, 32, 32);
            g2d.setComposite(AlphaComposite.SrcOver);

            g2d.setColor(new Color(32, 44, 88));
            g2d.fillRoundRect(px, py, w, h, 30, 30);
            g2d.setColor(new Color(100, 232, 255));
            g2d.drawRoundRect(px, py, w, h, 30, 30);

            int d = Math.round(10 * scale);
            int cx = px + Math.round(30 * scale);
            int cy = py + Math.round(30 * scale);
            g2d.setColor(new Color(230, 241, 255));
            g2d.fillRect(cx - d, cy, d * 3, d);
            g2d.fillRect(cx, cy - d, d, d * 3);

            int b = Math.round(12 * scale);
            g2d.setColor(new Color(255, 190, 88));
            g2d.fillOval(px + Math.round(95 * scale), py + Math.round(21 * scale), b, b);
            g2d.setColor(new Color(106, 239, 255));
            g2d.fillOval(px + Math.round(83 * scale), py + Math.round(34 * scale), b, b);
            g2d.setColor(new Color(255, 119, 182));
            g2d.fillOval(px + Math.round(107 * scale), py + Math.round(34 * scale), b, b);
        }

        private void drawJoystick(Graphics2D g2d, float x, float y, float scale) {
            int size = Math.round(76 * scale);
            int px = Math.round(x - size / 2f);
            int py = Math.round(y - size / 2f);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
            g2d.setColor(new Color(255, 117, 192));
            g2d.fillRoundRect(px - 8, py - 6, size + 16, size + 22, 24, 24);
            g2d.setComposite(AlphaComposite.SrcOver);

            g2d.setColor(new Color(20, 28, 60));
            g2d.fillRoundRect(px, py + 12, size, size - 8, 18, 18);
            g2d.setColor(new Color(255, 146, 205));
            g2d.drawRoundRect(px, py + 12, size, size - 8, 18, 18);

            int stickX = px + size / 2;
            int top = py + 2;
            g2d.setColor(new Color(240, 246, 255));
            g2d.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(stickX, top + 12, stickX, py + size / 2 + 7);
            g2d.setColor(new Color(255, 100, 165));
            g2d.fillOval(stickX - 12, top - 1, 24, 24);
        }

        private void drawHeadset(Graphics2D g2d, float x, float y, float scale) {
            int w = Math.round(120 * scale);
            int h = Math.round(78 * scale);
            int px = Math.round(x - w / 2f);
            int py = Math.round(y - h / 2f);

            g2d.setColor(new Color(255, 156, 94));
            g2d.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawArc(px + 14, py, w - 28, h - 8, 0, 180);

            g2d.fillRoundRect(px + 4, py + 26, 24, 34, 8, 8);
            g2d.fillRoundRect(px + w - 28, py + 26, 24, 34, 8, 8);
            g2d.drawLine(px + w - 10, py + 53, px + w + 8, py + 64);
            g2d.fillOval(px + w + 5, py + 59, 10, 10);
        }

        private void drawDPad(Graphics2D g2d, float x, float y, float scale) {
            int block = Math.round(19 * scale);
            int px = Math.round(x - (block * 1.5f));
            int py = Math.round(y - (block * 1.5f));

            g2d.setColor(new Color(201, 231, 255));
            g2d.fillRoundRect(px + block, py, block, block * 3, 8, 8);
            g2d.fillRoundRect(px, py + block, block * 3, block, 8, 8);
            g2d.setColor(new Color(119, 164, 199));
            g2d.drawRoundRect(px + block, py, block, block * 3, 8, 8);
            g2d.drawRoundRect(px, py + block, block * 3, block, 8, 8);
        }

        private void drawScanlines(Graphics2D g2d, int width, int height) {
            g2d.setColor(new Color(4, 6, 19, 56));
            for (int y = 0; y < height; y += 4) {
                g2d.drawLine(0, y, width, y);
            }
        }

        private static class Star {
            private float x;
            private float y;
            private float speed;
            private float phase;
            private int size;
            private int alpha;
        }

        private static class Orb {
            private float x;
            private float y;
            private float speed;
            private float waveAmount;
            private float waveSpeed;
            private float phase;
            private int size;
            private int alpha;
        }

        private static class Accessory {
            private final int type;
            private final float xRatio;
            private final float yRatio;
            private final float scale;
            private final float wobbleX;
            private final float wobbleY;
            private final float phase;
            private final float speed;

            private Accessory(
                int type,
                float xRatio,
                float yRatio,
                float scale,
                float wobbleX,
                float wobbleY,
                float phase
            ) {
                this.type = type;
                this.xRatio = xRatio;
                this.yRatio = yRatio;
                this.scale = scale;
                this.wobbleX = wobbleX;
                this.wobbleY = wobbleY;
                this.phase = phase;
                this.speed = 0.85f;
            }
        }
    }
}
