package games;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Random;

public class GamePanel extends JPanel {

    private static final int WIN_SCORE = 10;
    private static final int ANIMATION_DELAY_MS = 16;
    private static final int MOVE_BOUNCE_FRAMES = 10;
    private static final int HIT_SHAKE_FRAMES = 12;
    private static final int HUD_HEIGHT = 118;
    private static final int OUTER_MARGIN = 34;
    private static final int BOARD_FRAME_PADDING = 14;
    private static final int PARTICLE_COUNT = 120;

    private static final Font TITLE_FONT = new Font("Dialog", Font.BOLD, 42);
    private static final Font SUBTITLE_FONT = new Font("Dialog", Font.PLAIN, 18);
    private static final Font HUD_FONT = new Font("Dialog", Font.BOLD, 19);
    private static final Font OVERLAY_TITLE_FONT = new Font("Dialog", Font.BOLD, 50);
    private static final Font OVERLAY_TEXT_FONT = new Font("Dialog", Font.BOLD, 24);

    private final GameLogic gameLogic;
    private final Timer animationTimer;
    private final Random random;
    private final Particle[] particles;

    private boolean completionShown;
    private boolean scorePersisted;
    private boolean particlesInitialized;
    private long animationTick;
    private float glowPhase;
    private int bounceFramesRemaining;
    private int shakeFramesRemaining;
    private int moveCount;
    private int completionFrames;
    private int obstacleCount;
    private Timer closeTimer;

    public GamePanel(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
        this.random = new Random();
        this.particles = new Particle[PARTICLE_COUNT];
        this.completionShown = false;
        this.scorePersisted = false;
        this.particlesInitialized = false;
        this.animationTick = 0L;
        this.glowPhase = 0f;
        this.bounceFramesRemaining = 0;
        this.shakeFramesRemaining = 0;
        this.moveCount = 0;
        this.completionFrames = 0;
        this.closeTimer = null;

        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle();
        }

        obstacleCount = countObstacles();

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1280, 720));
        setBackground(new Color(5, 10, 28));
        setFocusable(true);
        bindMovementKeys();

        animationTimer = new Timer(ANIMATION_DELAY_MS, this::onAnimationFrame);
        animationTimer.start();
    }

    @Override
    public void removeNotify() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        if (closeTimer != null) {
            closeTimer.stop();
        }
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getSafeWidth();
        int height = getSafeHeight();
        initializeParticlesIfNeeded(width, height);

        drawBackground(g2d, width, height);
        BoardLayout layout = computeBoardLayout(width, height);
        drawHud(g2d, width, layout);
        drawBoardFrame(g2d, layout);
        drawMaze(g2d, layout);
        drawExit(g2d, layout);
        drawPlayer(g2d, layout);

        if (completionShown) {
            drawCompletionOverlay(g2d, width, height);
        }

        g2d.dispose();
    }

    private void bindMovementKeys() {
        bindSingleKey("UP", -1, 0);
        bindSingleKey("DOWN", 1, 0);
        bindSingleKey("LEFT", 0, -1);
        bindSingleKey("RIGHT", 0, 1);
        bindSingleKey("W", -1, 0);
        bindSingleKey("S", 1, 0);
        bindSingleKey("A", 0, -1);
        bindSingleKey("D", 0, 1);
    }

    private void bindSingleKey(String key, int rowOffset, int colOffset) {
        String actionKey = "move_" + key.toLowerCase();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), actionKey);
        getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                attemptMove(rowOffset, colOffset);
            }
        });
    }

    private void attemptMove(int rowOffset, int colOffset) {
        if (completionShown) {
            return;
        }

        boolean moved = gameLogic.movePlayer(rowOffset, colOffset);
        if (!moved) {
            shakeFramesRemaining = HIT_SHAKE_FRAMES;
            repaint();
            return;
        }

        moveCount++;
        bounceFramesRemaining = MOVE_BOUNCE_FRAMES;
        repaint();

        if (!gameLogic.isCompleted()) {
            return;
        }

        handleCompletion();
    }

    private void handleCompletion() {
        completionShown = true;
        if (!scorePersisted) {
            ScoreService.recordScore("Maze Escape", WIN_SCORE);
            scorePersisted = true;
        }

        if (closeTimer != null) {
            closeTimer.stop();
        }

        closeTimer = new Timer(1400, e -> {
            ((Timer) e.getSource()).stop();
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            if (parentWindow != null) {
                parentWindow.dispose();
            }
        });
        closeTimer.setRepeats(false);
        closeTimer.start();
    }

    private void onAnimationFrame(ActionEvent event) {
        animationTick++;
        glowPhase += 0.055f;

        if (bounceFramesRemaining > 0) {
            bounceFramesRemaining--;
        }
        if (shakeFramesRemaining > 0) {
            shakeFramesRemaining--;
        }
        if (completionShown) {
            completionFrames++;
        }

        updateParticles();
        repaint();
    }

    private void drawBackground(Graphics2D g2d, int width, int height) {
        GradientPaint base = new GradientPaint(
            0, 0, new Color(5, 10, 28),
            0, height, new Color(15, 6, 42)
        );
        g2d.setPaint(base);
        g2d.fillRect(0, 0, width, height);

        int glowRadius = 280 + (int) (Math.sin(glowPhase) * 26);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
        g2d.setColor(new Color(55, 232, 255));
        g2d.fillOval((width / 2) - glowRadius, 44, glowRadius * 2, glowRadius * 2);
        g2d.setColor(new Color(255, 92, 186));
        g2d.fillOval((width / 2) - 210, height - 320, 420, 340);
        g2d.setComposite(AlphaComposite.SrcOver);

        for (Particle particle : particles) {
            g2d.setColor(new Color(190, 232, 255, particle.alpha));
            g2d.fillOval(Math.round(particle.x), Math.round(particle.y), particle.size, particle.size);
        }

        g2d.setColor(new Color(8, 12, 28, 64));
        for (int y = 0; y < height; y += 4) {
            g2d.drawLine(0, y, width, y);
        }
    }

    private void drawHud(Graphics2D g2d, int width, BoardLayout layout) {
        int hudX = OUTER_MARGIN;
        int hudY = 16;
        int hudWidth = width - (OUTER_MARGIN * 2);
        int hudHeight = HUD_HEIGHT - 26;

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
        g2d.setColor(new Color(78, 232, 255));
        g2d.fillRoundRect(hudX + 6, hudY + 8, hudWidth - 12, hudHeight - 12, 26, 26);
        g2d.setComposite(AlphaComposite.SrcOver);

        g2d.setColor(new Color(9, 20, 47, 226));
        g2d.fillRoundRect(hudX, hudY, hudWidth, hudHeight, 24, 24);
        g2d.setColor(new Color(95, 237, 255, 210));
        g2d.setStroke(new BasicStroke(2.2f));
        g2d.drawRoundRect(hudX, hudY, hudWidth, hudHeight, 24, 24);

        g2d.setFont(TITLE_FONT);
        g2d.setColor(new Color(116, 244, 255));
        g2d.drawString("MAZE ESCAPE", hudX + 20, hudY + 49);

        g2d.setFont(SUBTITLE_FONT);
        g2d.setColor(new Color(255, 135, 208));
        g2d.drawString("Find the glowing portal. Avoid obstacles.", hudX + 24, hudY + 76);

        g2d.setFont(HUD_FONT);
        g2d.setColor(new Color(201, 233, 255));
        String stats = "Moves: " + moveCount + "   Obstacles: " + obstacleCount + "   Reward: +" + WIN_SCORE;
        int statsWidth = g2d.getFontMetrics().stringWidth(stats);
        g2d.drawString(stats, hudX + hudWidth - statsWidth - 22, hudY + 50);

        g2d.setFont(new Font("Dialog", Font.PLAIN, 16));
        g2d.setColor(new Color(170, 207, 242));
        String controls = "Controls: Arrow Keys / WASD    ESC: Back to Dashboard";
        int controlsWidth = g2d.getFontMetrics().stringWidth(controls);
        g2d.drawString(controls, hudX + hudWidth - controlsWidth - 22, hudY + 76);
    }

    private void drawBoardFrame(Graphics2D g2d, BoardLayout layout) {
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
        g2d.setColor(new Color(255, 106, 192));
        g2d.fillRoundRect(
            layout.frameX + 8,
            layout.frameY + 10,
            layout.frameWidth - 16,
            layout.frameHeight - 14,
            32,
            32
        );
        g2d.setComposite(AlphaComposite.SrcOver);

        GradientPaint frameGradient = new GradientPaint(
            layout.frameX, layout.frameY, new Color(10, 26, 58, 235),
            layout.frameX, layout.frameY + layout.frameHeight, new Color(6, 14, 40, 235)
        );
        g2d.setPaint(frameGradient);
        g2d.fillRoundRect(layout.frameX, layout.frameY, layout.frameWidth, layout.frameHeight, 30, 30);

        g2d.setColor(new Color(99, 235, 255));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawRoundRect(layout.frameX, layout.frameY, layout.frameWidth, layout.frameHeight, 30, 30);

        g2d.setColor(new Color(8, 16, 38));
        g2d.fillRoundRect(layout.boardX, layout.boardY, layout.boardWidth, layout.boardHeight, 16, 16);
    }

    private void drawMaze(Graphics2D g2d, BoardLayout layout) {
        int[][] maze = gameLogic.getMaze();
        int rows = maze.length;
        int cols = maze[0].length;
        int inset = Math.max(2, layout.cellSize / 7);

        GradientPaint floor = new GradientPaint(
            layout.boardX, layout.boardY, new Color(14, 28, 60),
            layout.boardX, layout.boardY + layout.boardHeight, new Color(8, 18, 44)
        );
        g2d.setPaint(floor);
        g2d.fillRoundRect(
            layout.boardX + 2,
            layout.boardY + 2,
            layout.boardWidth - 4,
            layout.boardHeight - 4,
            12,
            12
        );

        int dotSize = Math.max(2, layout.cellSize / 10);
        g2d.setColor(new Color(110, 176, 232, 42));
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int dotX = layout.boardX + (col * layout.cellSize) + (layout.cellSize / 2) - (dotSize / 2);
                int dotY = layout.boardY + (row * layout.cellSize) + (layout.cellSize / 2) - (dotSize / 2);
                g2d.fillOval(dotX, dotY, dotSize, dotSize);
            }
        }

        float wallStroke = Math.max(3f, layout.cellSize / 5.7f);
        g2d.setStroke(new BasicStroke(wallStroke + 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(53, 174, 255, 78));
        drawMazeWallLines(g2d, maze, layout, rows, cols, inset);

        g2d.setStroke(new BasicStroke(wallStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(138, 224, 255, 225));
        drawMazeWallLines(g2d, maze, layout, rows, cols, inset);

    }

    private void drawMazeWallLines(
        Graphics2D g2d,
        int[][] maze,
        BoardLayout layout,
        int rows,
        int cols,
        int inset
    ) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (maze[row][col] != MazeGenerator.PATH) {
                    continue;
                }

                int x0 = layout.boardX + (col * layout.cellSize) + inset;
                int y0 = layout.boardY + (row * layout.cellSize) + inset;
                int x1 = layout.boardX + ((col + 1) * layout.cellSize) - inset;
                int y1 = layout.boardY + ((row + 1) * layout.cellSize) - inset;

                if (isBlockedCell(maze, row - 1, col)) {
                    g2d.drawLine(x0, y0, x1, y0);
                }
                if (isBlockedCell(maze, row + 1, col)) {
                    g2d.drawLine(x0, y1, x1, y1);
                }
                if (isBlockedCell(maze, row, col - 1)) {
                    g2d.drawLine(x0, y0, x0, y1);
                }
                if (isBlockedCell(maze, row, col + 1)) {
                    g2d.drawLine(x1, y0, x1, y1);
                }
            }
        }
    }

    private boolean isBlockedCell(int[][] maze, int row, int col) {
        if (row < 0 || row >= maze.length || col < 0 || col >= maze[0].length) {
            return true;
        }
        return maze[row][col] != MazeGenerator.PATH;
    }

    private void drawExit(Graphics2D g2d, BoardLayout layout) {
        int cellX = layout.boardX + (gameLogic.getExitCol() * layout.cellSize);
        int cellY = layout.boardY + (gameLogic.getExitRow() * layout.cellSize);
        int size = Math.round(layout.cellSize * 0.68f);
        int drawX = cellX + (layout.cellSize - size) / 2;
        int drawY = cellY + (layout.cellSize - size) / 2;

        float pulse = 0.5f + 0.5f * (float) Math.sin(glowPhase * 1.6f);
        int glowSize = size + Math.round(layout.cellSize * (0.20f + pulse * 0.16f));
        int glowX = cellX + (layout.cellSize - glowSize) / 2;
        int glowY = cellY + (layout.cellSize - glowSize) / 2;

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g2d.setColor(new Color(77, 236, 172));
        g2d.fillOval(glowX, glowY, glowSize, glowSize);
        g2d.setComposite(AlphaComposite.SrcOver);

        g2d.setColor(new Color(54, 194, 141));
        g2d.fillRoundRect(drawX, drawY, size, size, Math.max(8, size / 3), Math.max(8, size / 3));
        g2d.setColor(new Color(220, 255, 236));
        g2d.setStroke(new BasicStroke(Math.max(2.2f, layout.cellSize / 10f)));
        g2d.drawRoundRect(drawX, drawY, size, size, Math.max(8, size / 3), Math.max(8, size / 3));

        int arrowMidX = drawX + (size / 2);
        int arrowTop = drawY + Math.round(size * 0.20f);
        int arrowBottom = drawY + Math.round(size * 0.80f);
        int wing = Math.max(4, size / 4);
        g2d.drawLine(arrowMidX, arrowTop, arrowMidX, arrowBottom);
        g2d.drawLine(arrowMidX, arrowBottom, arrowMidX - wing, arrowBottom - wing);
        g2d.drawLine(arrowMidX, arrowBottom, arrowMidX + wing, arrowBottom - wing);
    }

    private void drawPlayer(Graphics2D g2d, BoardLayout layout) {
        Player player = gameLogic.getPlayer();
        int cellX = layout.boardX + (player.getCol() * layout.cellSize);
        int cellY = layout.boardY + (player.getRow() * layout.cellSize);

        int shake = getShakeOffsetX(layout.cellSize);
        int bounce = getBounceOffsetY(layout.cellSize);
        int sway = getIdleSwayOffsetX(layout.cellSize);
        int bob = getIdleBobOffsetY(layout.cellSize);

        int size = Math.round(layout.cellSize * 0.64f);
        int drawX = cellX + ((layout.cellSize - size) / 2) + shake + sway;
        int drawY = cellY + ((layout.cellSize - size) / 2) - bounce + bob;

        g2d.setColor(new Color(9, 14, 30, 95));
        g2d.fillOval(
            cellX + (layout.cellSize / 5) + shake,
            cellY + layout.cellSize - Math.max(6, layout.cellSize / 6),
            Math.max(10, layout.cellSize - (layout.cellSize / 3)),
            Math.max(4, layout.cellSize / 7)
        );

        float pulse = 0.5f + 0.5f * (float) Math.sin(glowPhase * 1.9f);
        int glowSize = size + Math.round(layout.cellSize * (0.24f + pulse * 0.12f));
        int glowX = drawX + ((size - glowSize) / 2);
        int glowY = drawY + ((size - glowSize) / 2);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.34f));
        g2d.setColor(new Color(92, 231, 255));
        g2d.fillOval(glowX, glowY, glowSize, glowSize);
        g2d.setComposite(AlphaComposite.SrcOver);

        g2d.setColor(new Color(95, 187, 255));
        g2d.fillOval(drawX, drawY, size, size);
        g2d.setColor(new Color(232, 247, 255));
        g2d.fillOval(drawX + Math.max(2, size / 4), drawY + Math.max(2, size / 5), Math.max(3, size / 3), Math.max(3, size / 3));
        g2d.setColor(new Color(32, 69, 115));
        g2d.setStroke(new BasicStroke(Math.max(2f, layout.cellSize / 11f)));
        g2d.drawOval(drawX, drawY, size, size);

        int eyeSize = Math.max(2, size / 9);
        int eyeY = drawY + Math.max(4, size / 2 - eyeSize);
        g2d.setColor(new Color(17, 31, 59));
        g2d.fillOval(drawX + Math.max(4, size / 3), eyeY, eyeSize, eyeSize);
        g2d.fillOval(drawX + Math.max(7, (size * 2) / 3), eyeY, eyeSize, eyeSize);
    }

    private void drawCompletionOverlay(Graphics2D g2d, int width, int height) {
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fillRect(0, 0, width, height);

        int cardW = 620;
        int cardH = 260;
        int cardX = (width - cardW) / 2;
        int cardY = (height - cardH) / 2;

        float pulse = 0.5f + 0.5f * (float) Math.sin((completionFrames * 0.15f));
        Color borderColor = blend(new Color(86, 231, 255), new Color(255, 152, 211), pulse);

        GradientPaint overlayGradient = new GradientPaint(
            cardX, cardY, new Color(7, 19, 47, 235),
            cardX, cardY + cardH, new Color(12, 9, 37, 235)
        );
        g2d.setPaint(overlayGradient);
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 34, 34);

        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 34, 34);

        g2d.setFont(OVERLAY_TITLE_FONT);
        g2d.setColor(new Color(142, 245, 255));
        g2d.drawString("MAZE CLEARED!", cardX + 124, cardY + 94);

        g2d.setFont(OVERLAY_TEXT_FONT);
        g2d.setColor(new Color(251, 244, 255));
        g2d.drawString("Excellent navigation. +" + WIN_SCORE + " points earned.", cardX + 132, cardY + 150);
        g2d.drawString("Returning to dashboard...", cardX + 188, cardY + 193);
    }

    private BoardLayout computeBoardLayout(int width, int height) {
        int[][] maze = gameLogic.getMaze();
        int rows = maze.length;
        int cols = maze[0].length;

        int availableWidth = Math.max(320, width - (OUTER_MARGIN * 2) - (BOARD_FRAME_PADDING * 2));
        int availableHeight = Math.max(
            240,
            height - HUD_HEIGHT - OUTER_MARGIN - (BOARD_FRAME_PADDING * 2) - 18
        );

        int cellSize = Math.max(16, Math.min(availableWidth / cols, availableHeight / rows));
        int boardWidth = cellSize * cols;
        int boardHeight = cellSize * rows;
        int boardX = (width - boardWidth) / 2;
        int boardY = HUD_HEIGHT + Math.max(16, (height - HUD_HEIGHT - boardHeight - OUTER_MARGIN) / 2);

        int frameX = boardX - BOARD_FRAME_PADDING;
        int frameY = boardY - BOARD_FRAME_PADDING;
        int frameWidth = boardWidth + (BOARD_FRAME_PADDING * 2);
        int frameHeight = boardHeight + (BOARD_FRAME_PADDING * 2);

        return new BoardLayout(
            cellSize,
            boardX,
            boardY,
            boardWidth,
            boardHeight,
            frameX,
            frameY,
            frameWidth,
            frameHeight
        );
    }

    private void initializeParticlesIfNeeded(int width, int height) {
        if (particlesInitialized) {
            return;
        }
        for (Particle particle : particles) {
            resetParticle(particle, width, height, true);
        }
        particlesInitialized = true;
    }

    private void updateParticles() {
        int width = getSafeWidth();
        int height = getSafeHeight();
        initializeParticlesIfNeeded(width, height);

        for (Particle particle : particles) {
            particle.y += particle.speed;
            particle.x += Math.sin((glowPhase * 0.8f) + particle.phase) * particle.drift;

            if (particle.y > height + 20 || particle.x < -20 || particle.x > width + 20) {
                resetParticle(particle, width, height, false);
            }
        }
    }

    private void resetParticle(Particle particle, int width, int height, boolean randomY) {
        particle.x = random.nextFloat() * width;
        particle.y = randomY ? random.nextFloat() * height : -random.nextInt(180) - 10;
        particle.size = 1 + random.nextInt(3);
        particle.speed = 0.4f + random.nextFloat() * 1.4f;
        particle.alpha = 45 + random.nextInt(95);
        particle.drift = 0.10f + random.nextFloat() * 0.44f;
        particle.phase = random.nextFloat() * 6.2f;
    }

    private int getShakeOffsetX(int cellSize) {
        if (shakeFramesRemaining <= 0) {
            return 0;
        }
        int direction = (shakeFramesRemaining % 2 == 0) ? 1 : -1;
        return direction * Math.max(2, cellSize / 12);
    }

    private int getBounceOffsetY(int cellSize) {
        if (bounceFramesRemaining <= 0) {
            return 0;
        }
        double progress = (MOVE_BOUNCE_FRAMES - bounceFramesRemaining) / (double) MOVE_BOUNCE_FRAMES;
        return (int) Math.round(Math.sin(progress * Math.PI) * Math.max(4.0, cellSize / 6.0));
    }

    private int getIdleSwayOffsetX(int cellSize) {
        if (bounceFramesRemaining > 0 || shakeFramesRemaining > 0) {
            return 0;
        }
        return (int) Math.round(Math.sin(animationTick * 0.12) * Math.max(1.0, cellSize / 22.0));
    }

    private int getIdleBobOffsetY(int cellSize) {
        if (bounceFramesRemaining > 0 || shakeFramesRemaining > 0) {
            return 0;
        }
        return (int) Math.round(Math.sin(animationTick * 0.09) * Math.max(1.0, cellSize / 24.0));
    }

    private int countObstacles() {
        int[][] maze = gameLogic.getMaze();
        int count = 0;
        for (int row = 0; row < maze.length; row++) {
            for (int col = 0; col < maze[row].length; col++) {
                if (maze[row][col] == MazeGenerator.OBSTACLE) {
                    count++;
                }
            }
        }
        return count;
    }

    private int getSafeWidth() {
        return getWidth() > 0 ? getWidth() : 1280;
    }

    private int getSafeHeight() {
        return getHeight() > 0 ? getHeight() : 720;
    }

    private static Color blend(Color from, Color to, float ratio) {
        float clamped = Math.max(0f, Math.min(1f, ratio));
        int red = Math.round(from.getRed() + ((to.getRed() - from.getRed()) * clamped));
        int green = Math.round(from.getGreen() + ((to.getGreen() - from.getGreen()) * clamped));
        int blue = Math.round(from.getBlue() + ((to.getBlue() - from.getBlue()) * clamped));
        return new Color(red, green, blue);
    }

    private static class BoardLayout {
        private final int cellSize;
        private final int boardX;
        private final int boardY;
        private final int boardWidth;
        private final int boardHeight;
        private final int frameX;
        private final int frameY;
        private final int frameWidth;
        private final int frameHeight;

        private BoardLayout(
            int cellSize,
            int boardX,
            int boardY,
            int boardWidth,
            int boardHeight,
            int frameX,
            int frameY,
            int frameWidth,
            int frameHeight
        ) {
            this.cellSize = cellSize;
            this.boardX = boardX;
            this.boardY = boardY;
            this.boardWidth = boardWidth;
            this.boardHeight = boardHeight;
            this.frameX = frameX;
            this.frameY = frameY;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
        }
    }

    private static class Particle {
        private float x;
        private float y;
        private float speed;
        private float drift;
        private float phase;
        private int size;
        private int alpha;
    }
}
