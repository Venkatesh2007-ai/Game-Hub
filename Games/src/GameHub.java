import games.GameFrame;
import games.LeaderboardWindow;
import games.ScoreService;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class GameHub extends JFrame {

    private static final Font TITLE_FONT = new Font("Dialog", Font.BOLD, 64);
    private static final Font SUBTITLE_FONT = new Font("Dialog", Font.PLAIN, 20);
    private static final Font INFO_FONT = new Font("Dialog", Font.BOLD, 20);
    private static final Font BUTTON_FONT = new Font("Dialog", Font.BOLD, 30);

    private final GraphicsDevice graphicsDevice;
    private final DarkNeonBackgroundPanel backgroundPanel;
    private final List<NeonGameButton> gameButtons;
    private final Timer buttonAnimationTimer;

    private JLabel playerInfoLabel;
    private float buttonPhase;

    public GameHub() {
        setTitle("GameHub Arcade");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);

        graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        backgroundPanel = new DarkNeonBackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout(22, 22));
        backgroundPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        setContentPane(backgroundPanel);

        gameButtons = new ArrayList<>();
        buttonPhase = 0f;
        buttonAnimationTimer = new Timer(33, this::animateGameButtons);

        String user = ScoreService.ensureUsername(null);
        if (user == null) {
            dispose();
            return;
        }

        buildInterface();
        registerExitShortcut();
        buttonAnimationTimer.start();
        launchFullscreen();
    }

    private void buildInterface() {
        JPanel shell = new JPanel(new BorderLayout(18, 18));
        shell.setOpaque(false);

        JPanel titlePanel = new JPanel(new BorderLayout(8, 8));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JLabel titleLabel = new JLabel("GAMEHUB", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(new Color(103, 255, 241));
        titlePanel.add(titleLabel, BorderLayout.NORTH);

        JLabel subtitleLabel = new JLabel("Dark Neon Arcade Mode", SwingConstants.CENTER);
        subtitleLabel.setFont(SUBTITLE_FONT);
        subtitleLabel.setForeground(new Color(255, 105, 196));
        titlePanel.add(subtitleLabel, BorderLayout.CENTER);

        playerInfoLabel = new JLabel("", SwingConstants.CENTER);
        playerInfoLabel.setFont(INFO_FONT);
        playerInfoLabel.setForeground(new Color(190, 226, 255));
        titlePanel.add(playerInfoLabel, BorderLayout.SOUTH);
        refreshPlayerInfo();

        shell.add(titlePanel, BorderLayout.NORTH);

        NeonGameButton mazeEscape = createGameButton(
            "MAZE ESCAPE",
            new Color(255, 126, 76),
            new Color(255, 189, 89),
            new Color(255, 99, 43),
            0.0f
        );

        NeonGameButton cakeMaker = createGameButton(
            "CAKE MAKER",
            new Color(255, 80, 189),
            new Color(195, 94, 255),
            new Color(255, 83, 206),
            1.2f
        );

        NeonGameButton whackAMole = createGameButton(
            "WHACK-A-MOLE",
            new Color(113, 255, 206),
            new Color(78, 193, 255),
            new Color(88, 255, 223),
            1.9f
        );

        NeonGameButton responseTime = createGameButton(
            "RESPONSE TEST",
            new Color(255, 146, 101),
            new Color(255, 94, 178),
            new Color(255, 129, 116),
            2.7f
        );

        NeonGameButton flappyBird = createGameButton(
            "FLAPPY PIG",
            new Color(255, 195, 88),
            new Color(255, 127, 79),
            new Color(255, 218, 110),
            3.1f
        );

        NeonGameButton syntaxSnake = createGameButton(
            "SYNTAX SNAKE",
            new Color(118, 255, 152),
            new Color(83, 208, 255),
            new Color(144, 255, 194),
            3.45f
        );

        NeonGameButton leaderboard = createGameButton(
            "LEADERBOARD",
            new Color(66, 226, 255),
            new Color(80, 164, 255),
            new Color(39, 246, 255),
            3.9f
        );

        mazeEscape.addActionListener(e -> openGame(GameFrame::new));
        cakeMaker.addActionListener(e -> openGame(games.cakemaker.GameFrame::new));
        whackAMole.addActionListener(e -> openGame(games.whackamole.GameFrame::new));
        responseTime.addActionListener(e -> openGame(games.responsetime.GameFrame::new));
        flappyBird.addActionListener(e -> openGame(games.flappybird.GameFrame::new));
        syntaxSnake.addActionListener(e -> openGame(games.syntaxsnake.GameFrame::new));
        leaderboard.addActionListener(e -> openGame(LeaderboardWindow::new));

        setButtonSize(mazeEscape, 340, 148);
        setButtonSize(cakeMaker, 360, 152);
        setButtonSize(whackAMole, 430, 152);
        setButtonSize(responseTime, 340, 148);
        setButtonSize(flappyBird, 360, 152);
        setButtonSize(syntaxSnake, 390, 148);
        setButtonSize(leaderboard, 430, 132);

        JPanel dashboardPanel = new JPanel(new GridBagLayout());
        dashboardPanel.setOpaque(false);

        addDashboardButton(dashboardPanel, mazeEscape, 0, 0, 1, new Insets(0, 0, 18, 20));
        addDashboardButton(dashboardPanel, cakeMaker, 1, 0, 1, new Insets(0, 0, 18, 20));
        addDashboardButton(dashboardPanel, whackAMole, 2, 0, 1, new Insets(0, 0, 18, 0));
        addDashboardButton(dashboardPanel, responseTime, 0, 1, 1, new Insets(0, 0, 18, 20));
        addDashboardButton(dashboardPanel, flappyBird, 1, 1, 1, new Insets(0, 0, 18, 20));
        addDashboardButton(dashboardPanel, syntaxSnake, 2, 1, 1, new Insets(0, 0, 18, 0));
        addDashboardButton(dashboardPanel, leaderboard, 0, 2, 3, new Insets(6, 0, 0, 0));

        JPanel centerHolder = new JPanel(new GridBagLayout());
        centerHolder.setOpaque(false);
        centerHolder.add(dashboardPanel);
        shell.add(centerHolder, BorderLayout.CENTER);

        JLabel hintLabel = new JLabel("Press ESC to exit fullscreen", SwingConstants.CENTER);
        hintLabel.setFont(new Font("Dialog", Font.PLAIN, 15));
        hintLabel.setForeground(new Color(175, 210, 255));
        shell.add(hintLabel, BorderLayout.SOUTH);

        backgroundPanel.add(shell, BorderLayout.CENTER);
    }

    private void setButtonSize(NeonGameButton button, int width, int height) {
        Dimension size = new Dimension(width, height);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
    }

    private void addDashboardButton(
        JPanel dashboardPanel,
        NeonGameButton button,
        int gridX,
        int gridY,
        int gridWidth,
        Insets insets
    ) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = gridX;
        constraints.gridy = gridY;
        constraints.gridwidth = gridWidth;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = insets;
        dashboardPanel.add(button, constraints);
    }

    private NeonGameButton createGameButton(
        String text,
        Color primaryColor,
        Color secondaryColor,
        Color glowColor,
        float phaseOffset
    ) {
        NeonGameButton button = new NeonGameButton(text, primaryColor, secondaryColor, glowColor, phaseOffset);
        button.setFont(BUTTON_FONT);
        button.setForeground(new Color(243, 249, 255));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gameButtons.add(button);
        return button;
    }

    private void animateGameButtons(ActionEvent event) {
        buttonPhase += 0.06f;
        for (NeonGameButton button : gameButtons) {
            button.setAnimationPhase(buttonPhase);
        }
    }

    private void registerExitShortcut() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "exit-hub");
        getRootPane().getActionMap().put("exit-hub", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
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

    private void refreshPlayerInfo() {
        String user = ScoreService.getUsername();
        int total = ScoreService.getTotalScore(user);
        playerInfoLabel.setText("Player: " + user + " | Combined Score: " + total);
    }

    private void openGame(Supplier<JFrame> gameFactory) {
        hideDashboardForChildWindow();

        JFrame game;
        try {
            game = gameFactory.get();
        } catch (RuntimeException exception) {
            restoreDashboardAfterChild();
            throw exception;
        }

        final boolean[] restored = {false};
        game.addWindowListener(new WindowAdapter() {
            private void restoreOnce() {
                if (restored[0]) {
                    return;
                }
                restored[0] = true;
                restoreDashboardAfterChild();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                restoreOnce();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                restoreOnce();
            }
        });
    }

    private void hideDashboardForChildWindow() {
        if (graphicsDevice != null && graphicsDevice.getFullScreenWindow() == this) {
            graphicsDevice.setFullScreenWindow(null);
        }
        setVisible(false);
    }

    private void restoreDashboardAfterChild() {
        if (!isDisplayable()) {
            return;
        }

        refreshPlayerInfo();
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

    private static class NeonGameButton extends JButton {

        private enum ArtType {
            MAZE,
            CAKE,
            MOLE,
            RESPONSE,
            FLAPPY,
            LEADERBOARD,
            DEFAULT
        }

        private final Color primaryColor;
        private final Color secondaryColor;
        private final Color glowColor;
        private final float phaseOffset;
        private final ArtType artType;

        private float animationPhase;
        private boolean hovered;

        private NeonGameButton(
            String text,
            Color primaryColor,
            Color secondaryColor,
            Color glowColor,
            float phaseOffset
        ) {
            super(text);
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.glowColor = glowColor;
            this.phaseOffset = phaseOffset;
            this.artType = resolveArtType(text);
            this.animationPhase = phaseOffset;
            this.hovered = false;

            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setPreferredSize(new Dimension(360, 160));

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

        private void setAnimationPhase(float basePhase) {
            animationPhase = basePhase + phaseOffset;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2d = (Graphics2D) graphics.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            Shape buttonShape = createButtonShape(0, 0, width - 1, height - 1);
            Shape glowShape = createButtonShape(6, 10, width - 12, height - 14);

            float pulse = 0.5f + 0.5f * (float) Math.sin(animationPhase);
            float hoverBoost = hovered ? 0.22f : 0.0f;

            Color darkTopBase = new Color(10, 15, 36);
            Color darkBottomBase = new Color(4, 7, 23);
            Color topColor = blend(darkTopBase, primaryColor, 0.40f + (pulse * 0.30f));
            Color bottomColor = blend(darkBottomBase, secondaryColor, 0.34f + (pulse * 0.22f));

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.26f + hoverBoost));
            g2d.setColor(glowColor);
            g2d.fill(glowShape);

            g2d.setComposite(AlphaComposite.SrcOver);
            g2d.setClip(buttonShape);
            g2d.setPaint(new GradientPaint(0, 0, topColor, 0, height, bottomColor));
            g2d.fillRect(0, 0, width, height);

            drawBackgroundArtwork(g2d, width, height, pulse);

            int sheenWidth = Math.max(36, width / 5);
            int travel = width + sheenWidth + 40;
            int sheenX = -sheenWidth - 20 + (int) ((animationPhase % (Math.PI * 2)) / (Math.PI * 2) * travel);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.21f));
            g2d.setColor(new Color(255, 255, 255, 190));
            g2d.fillRoundRect(sheenX, 6, sheenWidth, height - 12, 22, 22);
            g2d.setComposite(AlphaComposite.SrcOver);
            g2d.setClip(null);

            g2d.setStroke(new BasicStroke(3.0f));
            g2d.setColor(blend(glowColor, Color.WHITE, 0.30f + pulse * 0.45f + hoverBoost));
            g2d.draw(createButtonShape(2, 2, width - 5, height - 5));

            super.paintComponent(g2d);
            g2d.dispose();
        }

        private Shape createButtonShape(float x, float y, float width, float height) {
            if (artType == ArtType.LEADERBOARD) {
                return createLeaderboardShape(x, y, width, height);
            }
            return new RoundRectangle2D.Float(x, y, width, height, 28f, 28f);
        }

        private Shape createLeaderboardShape(float x, float y, float width, float height) {
            float right = x + width;
            float bottom = y + height;
            float shoulderY = bottom - Math.min(28f, height * 0.22f);
            float shoulderInset = Math.min(18f, width * 0.05f);
            float topInset = Math.min(26f, width * 0.08f);
            float notchWidth = Math.min(104f, width * 0.26f);
            float notchLeft = x + ((width - notchWidth) / 2f);
            float notchRight = notchLeft + notchWidth;

            Path2D.Float path = new Path2D.Float();
            path.moveTo(x + topInset, y);
            path.lineTo(right - topInset, y);
            path.lineTo(right, y + 22f);
            path.lineTo(right - shoulderInset, shoulderY);
            path.lineTo(notchRight + 22f, shoulderY);
            path.lineTo(notchRight, bottom);
            path.lineTo(notchLeft, bottom);
            path.lineTo(notchLeft - 22f, shoulderY);
            path.lineTo(x + shoulderInset, shoulderY);
            path.lineTo(x, y + 22f);
            path.closePath();
            return path;
        }

        private void drawBackgroundArtwork(Graphics2D g2d, int width, int height, float pulse) {
            Graphics2D art = (Graphics2D) g2d.create();
            art.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float baseAlpha = 0.19f + (pulse * 0.07f) + (hovered ? 0.06f : 0f);
            art.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.34f, baseAlpha)));

            switch (artType) {
                case MAZE:
                    drawMazeArtwork(art, width, height);
                    break;
                case CAKE:
                    drawCakeArtwork(art, width, height);
                    break;
                case MOLE:
                    drawMoleArtwork(art, width, height);
                    break;
                case RESPONSE:
                    drawResponseArtwork(art, width, height, pulse);
                    break;
                case FLAPPY:
                    drawFlappyArtwork(art, width, height);
                    break;
                case LEADERBOARD:
                    drawLeaderboardArtwork(art, width, height);
                    break;
                default:
                    break;
            }

            art.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hovered ? 0.24f : 0.30f));
            art.setColor(new Color(4, 8, 24, 190));
            art.fillRoundRect(10, 10, width - 20, height - 20, 20, 20);

            art.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.14f + (pulse * 0.07f) + (hovered ? 0.04f : 0f)));
            art.setColor(new Color(255, 255, 255, 164));
            art.fillOval((width / 2) - (width / 4), 12, width / 2, 36);

            art.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.38f));
            art.setColor(new Color(5, 9, 22, 172));
            art.fillRoundRect(14, height - 54, width - 28, 38, 16, 16);
            art.dispose();
        }

        private void drawMazeArtwork(Graphics2D g2d, int width, int height) {
            int left = 20;
            int top = 18;
            int artWidth = width - 40;
            int artHeight = height - 66;

            g2d.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(new Color(182, 232, 255, 220));
            g2d.drawRoundRect(left, top, artWidth, artHeight, 14, 14);

            int rowStep = Math.max(10, artHeight / 4);
            int colStep = Math.max(18, artWidth / 6);

            for (int y = top + rowStep; y < top + artHeight; y += rowStep) {
                g2d.drawLine(left + 6, y, left + artWidth - 16, y);
            }
            for (int x = left + colStep; x < left + artWidth; x += colStep) {
                g2d.drawLine(x, top + 6, x, top + artHeight - 18);
            }

            g2d.setColor(new Color(255, 206, 96, 220));
            g2d.fillOval(left + artWidth - 22, top + artHeight - 22, 16, 16);
        }

        private void drawCakeArtwork(Graphics2D g2d, int width, int height) {
            int centerX = width / 2;
            int baseY = height - 52;

            g2d.setColor(new Color(230, 245, 255, 220));
            g2d.fillOval(centerX - 76, baseY, 152, 20);

            g2d.setColor(new Color(255, 209, 167, 220));
            g2d.fillRoundRect(centerX - 58, baseY - 32, 116, 24, 16, 16);
            g2d.setColor(new Color(255, 236, 197, 220));
            g2d.fillRoundRect(centerX - 52, baseY - 46, 104, 16, 14, 14);
            g2d.setColor(new Color(255, 168, 193, 220));
            g2d.fillRoundRect(centerX - 46, baseY - 58, 92, 12, 12, 12);

            g2d.setColor(new Color(255, 84, 122, 220));
            g2d.fillOval(centerX - 8, baseY - 70, 16, 16);
            g2d.setColor(new Color(108, 210, 127, 220));
            g2d.drawLine(centerX, baseY - 58, centerX + 8, baseY - 70);
        }

        private void drawMoleArtwork(Graphics2D g2d, int width, int height) {
            int cx = width / 2;
            int holeY = height - 62;

            g2d.setColor(new Color(41, 22, 18, 210));
            g2d.fillOval(cx - 82, holeY, 164, 30);
            g2d.setColor(new Color(190, 139, 96, 220));
            g2d.fillOval(cx - 30, holeY - 44, 60, 54);
            g2d.setColor(new Color(255, 223, 196, 220));
            g2d.fillOval(cx - 10, holeY - 24, 8, 8);
            g2d.fillOval(cx + 2, holeY - 24, 8, 8);

            g2d.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(new Color(255, 198, 108, 220));
            g2d.drawLine(cx + 40, holeY - 48, cx + 84, holeY - 80);
            g2d.setColor(new Color(255, 108, 170, 220));
            g2d.fillOval(cx + 74, holeY - 96, 26, 26);
        }

        private void drawResponseArtwork(Graphics2D g2d, int width, int height, float pulse) {
            int cx = width / 2;
            int cy = (height / 2) - 6;
            int radius = 34;

            g2d.setStroke(new BasicStroke(3f));
            g2d.setColor(new Color(214, 241, 255, 220));
            g2d.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

            g2d.setColor(new Color(214, 241, 255, 220));
            g2d.fillRoundRect(cx - 10, cy - radius - 14, 20, 10, 6, 6);

            double angle = (-Math.PI / 2) + (Math.sin(animationPhase) * 1.1);
            int handX = cx + (int) Math.round(Math.cos(angle) * (radius - 8));
            int handY = cy + (int) Math.round(Math.sin(angle) * (radius - 8));

            g2d.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(new Color(255, 124, 180, 220));
            g2d.drawLine(cx, cy, handX, handY);

            int flash = 8 + Math.round(6 * pulse);
            g2d.setColor(new Color(255, 226, 128, 220));
            g2d.fillOval(cx - flash / 2, cy - radius - 24, flash, flash);
        }

        private void drawFlappyArtwork(Graphics2D g2d, int width, int height) {
            int pipeWidth = 30;
            int gapTop = 28;
            int gapBottom = height - 74;
            int pipeX = width - 92;

            g2d.setColor(new Color(120, 243, 164, 220));
            g2d.fillRect(pipeX, 12, pipeWidth, gapTop);
            g2d.fillRect(pipeX, gapBottom, pipeWidth, height - gapBottom - 14);

            int birdX = width / 2 - 30;
            int birdY = height / 2 - 14;
            g2d.setColor(new Color(255, 214, 98, 220));
            g2d.fillOval(birdX, birdY, 34, 24);
            g2d.setColor(new Color(255, 138, 75, 220));
            g2d.fillOval(birdX + 26, birdY + 8, 11, 8);
            g2d.setColor(new Color(237, 246, 255, 220));
            g2d.fillOval(birdX + 20, birdY + 5, 8, 8);
        }

        private void drawLeaderboardArtwork(Graphics2D g2d, int width, int height) {
            int bottom = height - 22;
            int centerX = width / 2;

            g2d.setColor(new Color(122, 198, 255, 220));
            g2d.fillRoundRect(centerX - 72, bottom - 30, 34, 30, 8, 8);
            g2d.fillRoundRect(centerX - 17, bottom - 46, 34, 46, 8, 8);
            g2d.fillRoundRect(centerX + 38, bottom - 24, 34, 24, 8, 8);

            int cupX = centerX - 13;
            int cupY = bottom - 68;
            g2d.setColor(new Color(255, 211, 91, 220));
            g2d.fillRoundRect(cupX, cupY, 26, 16, 8, 8);
            g2d.fillRoundRect(centerX - 5, cupY + 16, 10, 10, 5, 5);
            g2d.fillRoundRect(centerX - 11, cupY + 26, 22, 6, 4, 4);
            g2d.drawOval(cupX - 8, cupY + 3, 8, 8);
            g2d.drawOval(cupX + 26, cupY + 3, 8, 8);
        }

        private static ArtType resolveArtType(String text) {
            String normalized = text == null ? "" : text.toLowerCase();
            if (normalized.contains("maze")) {
                return ArtType.MAZE;
            }
            if (normalized.contains("cake")) {
                return ArtType.CAKE;
            }
            if (normalized.contains("mole")) {
                return ArtType.MOLE;
            }
            if (normalized.contains("response")) {
                return ArtType.RESPONSE;
            }
            if (normalized.contains("flappy")) {
                return ArtType.FLAPPY;
            }
            if (normalized.contains("leaderboard")) {
                return ArtType.LEADERBOARD;
            }
            return ArtType.DEFAULT;
        }
    }

    private static class DarkNeonBackgroundPanel extends JPanel {

        private static final int STAR_COUNT = 140;
        private static final int JOYSTICK_COUNT = 4;
        private static final int CAR_COUNT = 4;
        private static final int DORAEMON_COUNT = 2;

        private final Random random;
        private final Star[] stars;
        private final JoystickSprite[] joysticks;
        private final CarSprite[] cars;
        private final DoraemonSprite[] doraemons;
        private final Timer animationTimer;

        private float gridShift;
        private float glowPhase;
        private float objectPhase;
        private boolean initialized;

        private DarkNeonBackgroundPanel() {
            random = new Random();
            stars = new Star[STAR_COUNT];
            joysticks = new JoystickSprite[JOYSTICK_COUNT];
            cars = new CarSprite[CAR_COUNT];
            doraemons = new DoraemonSprite[DORAEMON_COUNT];

            for (int i = 0; i < STAR_COUNT; i++) {
                stars[i] = new Star();
            }

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

            joysticks[0] = new JoystickSprite(0.08f, 0.18f, 0.9f, 24f, 16f, 0f);
            joysticks[1] = new JoystickSprite(0.90f, 0.25f, 1.1f, 20f, 14f, 1.8f);
            joysticks[2] = new JoystickSprite(0.11f, 0.82f, 1.0f, 22f, 17f, 2.9f);
            joysticks[3] = new JoystickSprite(0.89f, 0.74f, 1.2f, 26f, 15f, 4.1f);

            cars[0] = new CarSprite(-220f, 0.92f, 1.9f, 1.0f, new Color(255, 91, 177), true);
            cars[1] = new CarSprite(width + 160f, 0.87f, 1.5f, 0.9f, new Color(71, 243, 255), false);
            cars[2] = new CarSprite(-280f, 0.16f, 1.3f, 0.8f, new Color(250, 209, 78), true);
            cars[3] = new CarSprite(width + 260f, 0.12f, 1.6f, 0.85f, new Color(134, 255, 118), false);

            doraemons[0] = new DoraemonSprite(0.16f, 0.52f, 30f, 24f, 0.75f, 0f);
            doraemons[1] = new DoraemonSprite(0.86f, 0.48f, 28f, 22f, 0.9f, 2.4f);

            initialized = true;
        }

        private void advanceAnimation(ActionEvent event) {
            int width = Math.max(1, getWidth());
            int height = Math.max(1, getHeight());
            initializeIfNeeded(width, height);

            for (Star star : stars) {
                star.y += star.speed;
                star.x += Math.sin((objectPhase + star.phase) * 0.6) * 0.15f;
                if (star.y > height + 12) {
                    resetStar(star, width, height, false);
                }
            }

            for (CarSprite car : cars) {
                if (car.movingRight) {
                    car.x += car.speed;
                    if (car.x > width + 220) {
                        car.x = -220f;
                    }
                } else {
                    car.x -= car.speed;
                    if (car.x < -260) {
                        car.x = width + 220f;
                    }
                }
            }

            gridShift = (gridShift + 2.2f) % 84f;
            glowPhase += 0.045f;
            objectPhase += 0.052f;
            repaint();
        }

        private void resetStar(Star star, int width, int height, boolean randomY) {
            star.x = random.nextFloat() * width;
            star.y = randomY ? random.nextFloat() * height : -random.nextInt(140) - 10;
            star.size = 1 + random.nextInt(3);
            star.speed = 0.5f + random.nextFloat() * 1.6f;
            star.alpha = 65 + random.nextInt(115);
            star.phase = random.nextFloat() * 6.2f;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2d = (Graphics2D) graphics.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            initializeIfNeeded(width, height);

            drawDarkBase(g2d, width, height);
            drawNeonGlow(g2d, width, height);
            drawNeonGrid(g2d, width, height);
            drawStars(g2d);
            drawCars(g2d, width, height);
            drawJoysticks(g2d, width, height);
            drawDoraemons(g2d, width, height);
            drawScanlines(g2d, width, height);

            g2d.dispose();
        }

        private void drawDarkBase(Graphics2D g2d, int width, int height) {
            GradientPaint base = new GradientPaint(
                0, 0, new Color(6, 8, 24),
                0, height, new Color(16, 7, 39)
            );
            g2d.setPaint(base);
            g2d.fillRect(0, 0, width, height);
        }

        private void drawNeonGlow(Graphics2D g2d, int width, int height) {
            int mainRadius = 220 + (int) (Math.sin(glowPhase) * 26);
            int mainX = (width / 2) - mainRadius;
            int mainY = (height / 2) - mainRadius - 40;

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
            g2d.setColor(new Color(255, 52, 162));
            g2d.fillOval(mainX, mainY, mainRadius * 2, mainRadius * 2);

            int secondRadius = 165 + (int) (Math.cos(glowPhase * 1.4f) * 22);
            g2d.setColor(new Color(45, 233, 255));
            g2d.fillOval((width / 2) - secondRadius, (height / 2) - secondRadius - 28, secondRadius * 2, secondRadius * 2);

            g2d.setComposite(AlphaComposite.SrcOver);
        }

        private void drawNeonGrid(Graphics2D g2d, int width, int height) {
            int horizonY = Math.max(height / 3, 180);
            g2d.setStroke(new BasicStroke(1.3f));

            for (int i = 0; i <= 11; i++) {
                float progress = i / 11f;
                int y = horizonY + (int) (Math.pow(progress, 1.75) * (height - horizonY));
                int alpha = 48 + (int) (progress * 104);
                int wave = (int) Math.round(Math.sin((gridShift + (i * 6.8f)) * 0.11) * 11);
                g2d.setColor(new Color(80, 244, 255, alpha));
                g2d.drawLine(wave, y, width + wave, y);
            }

            for (int x = -width; x < width * 2; x += 78) {
                int bottomX = x + Math.round(gridShift);
                int topX = (width / 2) + (int) ((bottomX - (width / 2.0)) * 0.14);
                g2d.setColor(new Color(255, 96, 200, 112));
                g2d.drawLine(bottomX, height, topX, horizonY);
            }
        }

        private void drawStars(Graphics2D g2d) {
            for (Star star : stars) {
                g2d.setColor(new Color(185, 226, 255, star.alpha));
                g2d.fillOval(Math.round(star.x), Math.round(star.y), star.size, star.size);
            }
        }

        private void drawCars(Graphics2D g2d, int width, int height) {
            for (CarSprite car : cars) {
                float y = height * car.laneRatio;
                drawCar(g2d, car.x, y, car.scale, car.color, car.movingRight);
            }
        }

        private void drawJoysticks(Graphics2D g2d, int width, int height) {
            for (JoystickSprite joystick : joysticks) {
                float px = (width * joystick.xRatio)
                    + (float) Math.sin((objectPhase * joystick.speed) + joystick.phase) * joystick.wobbleX;
                float py = (height * joystick.yRatio)
                    + (float) Math.cos((objectPhase * joystick.speed * 0.9f) + joystick.phase) * joystick.wobbleY;
                drawJoystick(g2d, px, py, joystick.scale);
            }
        }

        private void drawDoraemons(Graphics2D g2d, int width, int height) {
            for (DoraemonSprite doraemon : doraemons) {
                float px = (width * doraemon.centerXRatio)
                    + (float) Math.cos(objectPhase * doraemon.speed + doraemon.phase) * doraemon.wobbleX;
                float py = (height * doraemon.centerYRatio)
                    + (float) Math.sin(objectPhase * doraemon.speed + doraemon.phase) * doraemon.wobbleY;
                float pulse = 0.5f + 0.5f * (float) Math.sin(objectPhase * 1.9f + doraemon.phase);
                drawDoraemon(g2d, px, py, doraemon.scale, pulse);
            }
        }

        private void drawScanlines(Graphics2D g2d, int width, int height) {
            g2d.setColor(new Color(4, 6, 19, 58));
            for (int y = 0; y < height; y += 4) {
                g2d.drawLine(0, y, width, y);
            }
        }

        private void drawCar(Graphics2D g2d, float x, float y, float scale, Color bodyColor, boolean facingRight) {
            int carWidth = Math.round(130 * scale);
            int carHeight = Math.round(46 * scale);
            int px = Math.round(x);
            int py = Math.round(y);

            Graphics2D copy = (Graphics2D) g2d.create();
            if (!facingRight) {
                copy.translate(px + carWidth / 2.0, 0);
                copy.scale(-1, 1);
                copy.translate(-(px + carWidth / 2.0), 0);
            }

            copy.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
            copy.setColor(new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), 190));
            copy.fillRoundRect(px - 6, py - 8, carWidth + 12, carHeight + 16, 30, 30);
            copy.setComposite(AlphaComposite.SrcOver);

            copy.setColor(bodyColor);
            copy.fillRoundRect(px, py, carWidth, carHeight, 22, 22);
            copy.setColor(new Color(245, 252, 255));
            copy.fillRoundRect(px + 22, py + 8, Math.round(48 * scale), Math.round(15 * scale), 10, 10);
            copy.fillRoundRect(px + 76, py + 8, Math.round(28 * scale), Math.round(15 * scale), 10, 10);

            copy.setColor(new Color(11, 16, 36));
            copy.fillOval(px + 16, py + carHeight - 8, Math.round(24 * scale), Math.round(24 * scale));
            copy.fillOval(px + carWidth - Math.round(40 * scale), py + carHeight - 8, Math.round(24 * scale), Math.round(24 * scale));

            copy.setColor(new Color(110, 247, 255));
            copy.setStroke(new BasicStroke(2f));
            copy.drawRoundRect(px, py, carWidth, carHeight, 22, 22);

            copy.dispose();
        }

        private void drawJoystick(Graphics2D g2d, float x, float y, float scale) {
            int size = Math.round(74 * scale);
            int px = Math.round(x - size / 2f);
            int py = Math.round(y - size / 2f);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
            g2d.setColor(new Color(79, 233, 255));
            g2d.fillRoundRect(px - 8, py - 6, size + 16, size + 24, 28, 28);
            g2d.setComposite(AlphaComposite.SrcOver);

            g2d.setColor(new Color(20, 26, 58));
            g2d.fillRoundRect(px, py + 12, size, size - 8, 18, 18);
            g2d.setColor(new Color(99, 246, 255));
            g2d.setStroke(new BasicStroke(2.2f));
            g2d.drawRoundRect(px, py + 12, size, size - 8, 18, 18);

            int stickX = px + size / 2;
            int stickTop = py + 4;
            int stickBottom = py + size / 2 + 8;
            g2d.setColor(new Color(240, 242, 248));
            g2d.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(stickX, stickTop + 10, stickX, stickBottom);
            g2d.setColor(new Color(255, 80, 186));
            g2d.fillOval(stickX - 13, stickTop - 2, 26, 26);

            g2d.setColor(new Color(255, 197, 74));
            g2d.fillOval(px + 11, py + size / 2 + 12, 14, 14);
            g2d.setColor(new Color(136, 255, 120));
            g2d.fillOval(px + size - 25, py + size / 2 + 12, 14, 14);
        }

        private void drawDoraemon(Graphics2D g2d, float centerX, float centerY, float scale, float pulse) {
            int headSize = Math.round(90 * scale);
            int bodyW = Math.round(72 * scale);
            int bodyH = Math.round(60 * scale);

            int headX = Math.round(centerX - headSize / 2f);
            int headY = Math.round(centerY - headSize / 2f);
            int bodyX = Math.round(centerX - bodyW / 2f);
            int bodyY = headY + headSize - Math.round(8 * scale);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
            g2d.setColor(new Color(75, 228, 255));
            g2d.fillOval(headX - 12, headY - 8, headSize + 24, headSize + bodyH);
            g2d.setComposite(AlphaComposite.SrcOver);

            Color blueTone = blend(new Color(37, 132, 255), new Color(76, 206, 255), pulse * 0.55f);
            g2d.setColor(blueTone);
            g2d.fillOval(headX, headY, headSize, headSize);
            g2d.fillRoundRect(bodyX, bodyY, bodyW, bodyH, 24, 24);

            g2d.setColor(new Color(245, 251, 255));
            g2d.fillOval(headX + Math.round(12 * scale), headY + Math.round(18 * scale), Math.round(66 * scale), Math.round(58 * scale));
            g2d.fillOval(bodyX + Math.round(16 * scale), bodyY + Math.round(16 * scale), Math.round(40 * scale), Math.round(30 * scale));

            int eyeY = headY + Math.round(28 * scale);
            g2d.setColor(Color.WHITE);
            g2d.fillOval(headX + Math.round(28 * scale), eyeY, Math.round(14 * scale), Math.round(20 * scale));
            g2d.fillOval(headX + Math.round(46 * scale), eyeY, Math.round(14 * scale), Math.round(20 * scale));
            g2d.setColor(new Color(16, 20, 31));
            g2d.fillOval(headX + Math.round(34 * scale), eyeY + Math.round(8 * scale), Math.round(4 * scale), Math.round(6 * scale));
            g2d.fillOval(headX + Math.round(52 * scale), eyeY + Math.round(8 * scale), Math.round(4 * scale), Math.round(6 * scale));

            g2d.setColor(new Color(255, 61, 71));
            g2d.fillOval(headX + Math.round(40 * scale), headY + Math.round(46 * scale), Math.round(12 * scale), Math.round(12 * scale));

            g2d.setColor(new Color(26, 34, 50));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawOval(headX, headY, headSize, headSize);
            g2d.drawRoundRect(bodyX, bodyY, bodyW, bodyH, 24, 24);
        }

        private static class Star {
            private float x;
            private float y;
            private float speed;
            private float phase;
            private int size;
            private int alpha;
        }

        private static class JoystickSprite {
            private final float xRatio;
            private final float yRatio;
            private final float speed;
            private final float wobbleX;
            private final float wobbleY;
            private final float phase;
            private final float scale;

            private JoystickSprite(float xRatio, float yRatio, float speed, float wobbleX, float wobbleY, float phase) {
                this.xRatio = xRatio;
                this.yRatio = yRatio;
                this.speed = speed;
                this.wobbleX = wobbleX;
                this.wobbleY = wobbleY;
                this.phase = phase;
                this.scale = 0.9f;
            }
        }

        private static class CarSprite {
            private float x;
            private final float laneRatio;
            private final float speed;
            private final float scale;
            private final Color color;
            private final boolean movingRight;

            private CarSprite(float x, float laneRatio, float speed, float scale, Color color, boolean movingRight) {
                this.x = x;
                this.laneRatio = laneRatio;
                this.speed = speed;
                this.scale = scale;
                this.color = color;
                this.movingRight = movingRight;
            }
        }

        private static class DoraemonSprite {
            private final float centerXRatio;
            private final float centerYRatio;
            private final float wobbleX;
            private final float wobbleY;
            private final float speed;
            private final float phase;
            private final float scale;

            private DoraemonSprite(
                float centerXRatio,
                float centerYRatio,
                float wobbleX,
                float wobbleY,
                float speed,
                float phase
            ) {
                this.centerXRatio = centerXRatio;
                this.centerYRatio = centerYRatio;
                this.wobbleX = wobbleX;
                this.wobbleY = wobbleY;
                this.speed = speed;
                this.phase = phase;
                this.scale = 0.92f;
            }
        }
    }

    public static void main(String[] args) {
        new GameHub();
    }
}
