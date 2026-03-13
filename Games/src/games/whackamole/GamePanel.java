package games.whackamole;

import games.ScoreService;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Random;

public class GamePanel extends JPanel {

    private static final String GAME_NAME = "Whack-A-Mole";
    private static final int MOLE_MOVE_INTERVAL_MS = 1200;
    private static final int ANIMATION_INTERVAL_MS = 16;
    private static final int OUTER_MARGIN = 28;
    private static final int GRID_GAP = 12;
    private static final int HEADER_GRID_GAP = 14;
    private static final int MIN_CELL_SIZE = 118;
    private static final int MAX_CELL_SIZE = 220;
    private static final Font TITLE_FONT = new Font("Dialog", Font.BOLD, 32);
    private static final Font METRIC_FONT = new Font("Dialog", Font.BOLD, 20);
    private static final Font STATUS_FONT = new Font("Dialog", Font.PLAIN, 16);
    private static final int[] KEYPAD_LAYOUT = {7, 8, 9, 4, 5, 6, 1, 2, 3};

    private static final Color[] CELL_COLORS = {
        new Color(0, 255, 208),
        new Color(72, 199, 255),
        new Color(143, 173, 255),
        new Color(174, 133, 255),
        new Color(255, 109, 214),
        new Color(255, 113, 171),
        new Color(255, 140, 128),
        new Color(255, 184, 102),
        new Color(247, 238, 98)
    };

    private final GameLogic gameLogic;
    private final MoleCell[] cells;
    private final JLabel scoreLabel;
    private final JLabel timeLabel;
    private final JLabel statusLabel;
    private final JButton startButton;
    private final JPanel headerPanel;
    private final JPanel gridPanel;
    private final JPanel centeredContentPanel;
    private final Timer moleTimer;
    private final Timer countdownTimer;
    private final Timer animationTimer;

    private boolean roundFinished;
    private long animationTick;
    private float glowPhase;

    public GamePanel() {
        gameLogic = new GameLogic(new Random());
        cells = new MoleCell[GameLogic.GRID_SIZE];
        roundFinished = true;
        animationTick = 0L;
        glowPhase = 0f;

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(OUTER_MARGIN, OUTER_MARGIN, OUTER_MARGIN, OUTER_MARGIN));
        setBackground(new Color(14, 14, 34));
        setFocusable(true);

        scoreLabel = createMetricLabel("Score: 0");
        timeLabel = createMetricLabel("Time: 30s");
        statusLabel = new JLabel("Press Start Game, then use keypad order: 7 8 9 / 4 5 6 / 1 2 3.", SwingConstants.CENTER);
        statusLabel.setFont(STATUS_FONT);
        statusLabel.setForeground(new Color(235, 242, 255));

        startButton = new JButton("Start Game");
        startButton.setFont(new Font("Dialog", Font.BOLD, 18));
        startButton.setFocusPainted(false);
        startButton.setBorder(BorderFactory.createLineBorder(new Color(167, 247, 255), 2, true));
        startButton.setBackground(new Color(88, 255, 222));
        startButton.setForeground(new Color(16, 44, 52));
        startButton.addActionListener(e -> startGame());

        headerPanel = createHeaderPanel();
        gridPanel = createGridPanel();

        JPanel centeredGridPanel = new JPanel(new GridBagLayout());
        centeredGridPanel.setOpaque(false);
        centeredGridPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centeredGridPanel.add(gridPanel);

        centeredContentPanel = new JPanel();
        centeredContentPanel.setOpaque(false);
        centeredContentPanel.setLayout(new BoxLayout(centeredContentPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centeredContentPanel.add(headerPanel);
        centeredContentPanel.add(Box.createVerticalStrut(HEADER_GRID_GAP));
        centeredContentPanel.add(centeredGridPanel);

        GridBagConstraints contentConstraints = new GridBagConstraints();
        contentConstraints.gridx = 0;
        contentConstraints.gridy = 0;
        contentConstraints.anchor = GridBagConstraints.CENTER;
        contentConstraints.insets = new Insets(0, 0, 0, 0);
        add(centeredContentPanel, contentConstraints);

        registerKeyboardControls();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateGridSizing();
            }
        });

        moleTimer = new Timer(MOLE_MOVE_INTERVAL_MS, this::onMoleTimerTick);
        countdownTimer = new Timer(1000, this::onSecondTick);
        animationTimer = new Timer(ANIMATION_INTERVAL_MS, this::onAnimationTick);
        animationTimer.start();

        updateGridSizing();
        refreshMetrics();
        refreshGrid();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();

        float pulse = 0.5f + (float) (Math.sin(glowPhase) * 0.5);
        Color topColor = GameHubColor.blend(new Color(10, 16, 42), new Color(22, 44, 90), pulse * 0.55f);
        Color bottomColor = GameHubColor.blend(new Color(7, 9, 27), new Color(42, 15, 75), pulse * 0.45f);
        g2d.setPaint(new GradientPaint(0, 0, topColor, 0, height, bottomColor));
        g2d.fillRect(0, 0, width, height);

        int glowSize = 220 + (int) (Math.sin(glowPhase * 1.2f) * 40);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
        g2d.setColor(new Color(62, 255, 220));
        g2d.fillOval(-60, 40, glowSize, glowSize);
        g2d.setColor(new Color(255, 104, 210));
        g2d.fillOval(width - glowSize + 60, height - glowSize - 20, glowSize, glowSize);
        g2d.setComposite(AlphaComposite.SrcOver);

        g2d.setColor(new Color(176, 232, 255, 28));
        for (int y = 0; y < height; y += 4) {
            g2d.drawLine(0, y, width, y);
        }

        g2d.dispose();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("WHACK-A-MOLE", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(new Color(255, 238, 119));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel metricsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 22, 0));
        metricsRow.setOpaque(false);
        metricsRow.add(scoreLabel);
        metricsRow.add(timeLabel);
        metricsRow.add(startButton);
        metricsRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(metricsRow);
        panel.add(Box.createVerticalStrut(5));
        panel.add(statusLabel);
        return panel;
    }

    private JPanel createGridPanel() {
        JPanel grid = new JPanel(new GridLayout(3, 3, GRID_GAP, GRID_GAP));
        grid.setOpaque(false);

        for (int index = 0; index < GameLogic.GRID_SIZE; index++) {
            MoleCell cell = new MoleCell(index, CELL_COLORS[index % CELL_COLORS.length]);
            cells[index] = cell;
            grid.add(cell);
        }

        return grid;
    }

    private JLabel createMetricLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(METRIC_FONT);
        label.setForeground(new Color(246, 252, 255));
        return label;
    }

    private void registerKeyboardControls() {
        for (int index = 0; index < GameLogic.GRID_SIZE; index++) {
            int key = KEYPAD_LAYOUT[index];
            bindHitKey(Integer.toString(key), "hit_digit_" + key, index);
            bindHitKey("NUMPAD" + key, "hit_numpad_" + key, index);
        }
    }

    private void bindHitKey(String keyStroke, String actionKey, int cellIndex) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyStroke), actionKey);
        getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                onCellKeyPressed(cellIndex);
            }
        });
    }

    private void startGame() {
        gameLogic.startGame();
        roundFinished = false;

        startButton.setText("Restart Game");
        statusLabel.setText("Hit using keypad order: 7 8 9 / 4 5 6 / 1 2 3.");

        moleTimer.restart();
        countdownTimer.restart();
        refreshMetrics();
        refreshGrid();
        requestFocusInWindow();
    }

    private void onCellKeyPressed(int cellIndex) {
        if (!gameLogic.isRunning()) {
            return;
        }

        boolean hit = gameLogic.hitMole(cellIndex);
        if (!hit) {
            return;
        }

        statusLabel.setText("Nice hit! Keep going.");
        refreshMetrics();
        refreshGrid();
    }

    private void onMoleTimerTick(ActionEvent event) {
        if (!gameLogic.isRunning()) {
            return;
        }
        gameLogic.moveMole();
        refreshGrid();
    }

    private void onSecondTick(ActionEvent event) {
        if (!gameLogic.isRunning()) {
            return;
        }

        boolean gameEnded = gameLogic.tickSecond();
        refreshMetrics();
        if (gameEnded) {
            endGame();
        }
    }

    private void onAnimationTick(ActionEvent event) {
        animationTick++;
        glowPhase += 0.06f;
        for (MoleCell cell : cells) {
            if (cell != null) {
                cell.stepAnimation(animationTick);
            }
        }
        repaint();
    }

    private void endGame() {
        if (roundFinished) {
            return;
        }

        roundFinished = true;
        moleTimer.stop();
        countdownTimer.stop();
        gameLogic.stopGame();
        refreshGrid();
        refreshMetrics();

        int finalScore = gameLogic.getScore();
        try {
            ScoreService.recordScore(GAME_NAME, finalScore);
        } catch (RuntimeException ignored) {
            // Keep the round on screen even if score persistence fails.
        }
        startButton.setText("Play Again");
        statusLabel.setText("Game Over! Final Score: " + finalScore + ". Press Play Again.");
    }

    private void refreshMetrics() {
        scoreLabel.setText("Score: " + gameLogic.getScore());
        timeLabel.setText("Time: " + gameLogic.getTimeRemaining() + "s");
    }

    private void refreshGrid() {
        int activeIndex = gameLogic.getActiveMoleIndex();
        boolean running = gameLogic.isRunning();
        for (int i = 0; i < cells.length; i++) {
            cells[i].setMoleVisible(running && i == activeIndex);
        }
    }

    private void updateGridSizing() {
        int width = Math.max(640, getWidth());
        int height = Math.max(480, getHeight());

        int availableWidth = Math.max(320, width - (OUTER_MARGIN * 2));
        int headerHeight = headerPanel.getPreferredSize().height;
        int availableHeight = Math.max(320, height - (OUTER_MARGIN * 2) - headerHeight - HEADER_GRID_GAP);

        int boundedByScreen = Math.min(availableWidth, availableHeight);
        int maxGridSize = (MAX_CELL_SIZE * 3) + (GRID_GAP * 2);
        int minGridSize = (MIN_CELL_SIZE * 3) + (GRID_GAP * 2);

        int gridSize = Math.min(maxGridSize, boundedByScreen);
        if (gridSize < minGridSize) {
            gridSize = Math.max((96 * 3) + (GRID_GAP * 2), boundedByScreen);
        }

        int cellSize = (gridSize - (GRID_GAP * 2)) / 3;
        int normalizedGridSize = (cellSize * 3) + (GRID_GAP * 2);

        Dimension cellDimension = new Dimension(cellSize, cellSize);
        for (MoleCell cell : cells) {
            if (cell != null) {
                cell.setPreferredSize(cellDimension);
            }
        }

        gridPanel.setPreferredSize(new Dimension(normalizedGridSize, normalizedGridSize));
        centeredContentPanel.revalidate();
        centeredContentPanel.repaint();
    }

    @Override
    public void removeNotify() {
        if (moleTimer != null) {
            moleTimer.stop();
        }
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        if (animationTimer != null) {
            animationTimer.stop();
        }
        super.removeNotify();
    }

    private class MoleCell extends JPanel {

        private final int index;
        private final Color baseColor;
        private boolean moleVisible;
        private float moleVisibility;
        private float popSway;

        private MoleCell(int index, Color baseColor) {
            this.index = index;
            this.baseColor = baseColor;
            this.moleVisible = false;
            this.moleVisibility = 0f;
            this.popSway = 0f;
            setOpaque(false);
            setPreferredSize(new Dimension(150, 150));
        }

        private void setMoleVisible(boolean moleVisible) {
            this.moleVisible = moleVisible;
        }

        private void stepAnimation(long tick) {
            float target = moleVisible ? 1f : 0f;
            moleVisibility += (target - moleVisibility) * 0.24f;
            popSway = (float) Math.sin((tick * 0.20) + (index * 0.85));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2d = (Graphics2D) graphics.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            float shimmer = 0.5f + 0.5f * (float) Math.sin((glowPhase * 1.4f) + (index * 0.6f));
            Color top = GameHubColor.blend(baseColor, Color.WHITE, 0.24f + (shimmer * 0.18f));
            Color bottom = GameHubColor.blend(baseColor, new Color(8, 10, 26), 0.35f);
            g2d.setPaint(new GradientPaint(0, 0, top, 0, height, bottom));
            g2d.fillRoundRect(0, 0, width - 1, height - 1, 20, 20);

            g2d.setColor(new Color(188, 242, 255, 214));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRoundRect(1, 1, width - 3, height - 3, 20, 20);

            if (moleVisibility > 0.05f) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.55f, moleVisibility * 0.55f)));
                g2d.setColor(new Color(255, 248, 139));
                g2d.setStroke(new BasicStroke(4f));
                g2d.drawRoundRect(4, 4, width - 9, height - 9, 18, 18);
                g2d.setComposite(AlphaComposite.SrcOver);
            }

            drawKeyLabel(g2d);
            if (moleVisibility > 0.02f) {
                drawMole(g2d, width, height);
            }

            g2d.dispose();
        }

        private void drawKeyLabel(Graphics2D g2d) {
            int keyNumber = KEYPAD_LAYOUT[index];
            String keyText = String.valueOf(keyNumber);
            g2d.setFont(new Font("Dialog", Font.BOLD, 24));
            int textWidth = g2d.getFontMetrics().stringWidth(keyText);
            g2d.setColor(new Color(11, 26, 53, 220));
            g2d.drawString(keyText, (getWidth() - textWidth) / 2, 30);
        }

        private void drawMole(Graphics2D g2d, int width, int height) {
            int moundW = width - 42;
            int moundH = 40;
            int moundX = (width - moundW) / 2;
            int moundY = height - moundH - 18;

            g2d.setColor(new Color(112, 73, 46));
            g2d.fillOval(moundX, moundY, moundW, moundH);

            int faceW = width - 58;
            int faceH = height - 78;
            int popOffset = (int) Math.round((1.0 - moleVisibility) * 28.0);
            int bobOffset = (int) Math.round(popSway * 2.5 * moleVisibility);
            int faceX = (width - faceW) / 2;
            int faceY = (height / 2) - (faceH / 2) + 6 + popOffset + bobOffset;

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.15f, moleVisibility)));

            g2d.setColor(new Color(147, 98, 68));
            g2d.fillOval(faceX, faceY, faceW, faceH);
            g2d.setColor(new Color(88, 55, 36));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawOval(faceX, faceY, faceW, faceH);

            int eyeY = faceY + 20;
            g2d.setColor(Color.WHITE);
            g2d.fillOval(faceX + 18, eyeY, 16, 18);
            g2d.fillOval(faceX + faceW - 34, eyeY, 16, 18);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(faceX + 24, eyeY + 7, 5, 6);
            g2d.fillOval(faceX + faceW - 28, eyeY + 7, 5, 6);

            int noseX = faceX + (faceW / 2) - 10;
            int noseY = faceY + 45;
            g2d.setColor(new Color(255, 141, 171));
            g2d.fillOval(noseX, noseY, 20, 14);
            g2d.setColor(new Color(151, 79, 96));
            g2d.drawOval(noseX, noseY, 20, 14);

            g2d.setColor(new Color(245, 245, 235));
            g2d.fillRect(faceX + (faceW / 2) - 8, faceY + 58, 6, 11);
            g2d.fillRect(faceX + (faceW / 2) + 2, faceY + 58, 6, 11);

            g2d.setComposite(AlphaComposite.SrcOver);
        }
    }

    private static class GameHubColor {
        private static Color blend(Color from, Color to, float ratio) {
            float clamped = Math.max(0f, Math.min(1f, ratio));
            int red = Math.round(from.getRed() + ((to.getRed() - from.getRed()) * clamped));
            int green = Math.round(from.getGreen() + ((to.getGreen() - from.getGreen()) * clamped));
            int blue = Math.round(from.getBlue() + ((to.getBlue() - from.getBlue()) * clamped));
            return new Color(red, green, blue);
        }
    }
}
