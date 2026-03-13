package games.flappybird;

import games.ScoreService;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel {

    private static final String GAME_NAME = "Flappy Pig";

    private static final int TARGET_FPS_DELAY_MS = 16;
    private static final double MAX_FRAME_DELTA_SECONDS = 0.033;

    private static final int PIPE_WIDTH = 108;
    private static final int MIN_PIPE_SEPARATION = 190;
    private static final int MIN_PIPE_GAP_PIXELS = 220;
    private static final int MAX_PIPE_GAP_PIXELS = 280;
    private static final double PIPE_SPAWN_INTERVAL_SECONDS = 1.45;
    private static final double BASE_PIPE_SPEED = 235.0;
    private static final double MAX_ADDED_PIPE_SPEED = 170.0;

    private static final int GROUND_HEIGHT = 110;
    private static final Font SCORE_FONT = new Font("Dialog", Font.BOLD, 42);
    private static final Font OVERLAY_TITLE_FONT = new Font("Dialog", Font.BOLD, 72);
    private static final Font OVERLAY_TEXT_FONT = new Font("Dialog", Font.BOLD, 28);

    private final Timer gameLoopTimer;
    private final List<Pipe> pipes;
    private final List<Cloud> clouds;
    private final Random random;

    private Pig pig;
    private int score;
    private boolean gameOver;
    private boolean scoreSaved;
    private boolean initialized;
    private long lastTickNanos;
    private double spawnTimerSeconds;
    private double elapsedSeconds;
    private double groundShift;
    private double hillShift;
    private double cloudShift;

    public GamePanel() {
        setPreferredSize(new Dimension(1280, 720));
        setDoubleBuffered(true);
        setFocusable(true);

        pipes = new ArrayList<>();
        clouds = new ArrayList<>();
        random = new Random();

        bindKeys();
        bindMouse();

        gameLoopTimer = new Timer(TARGET_FPS_DELAY_MS, this::onGameTick);
        gameLoopTimer.start();
    }

    @Override
    public void removeNotify() {
        if (gameLoopTimer != null) {
            gameLoopTimer.stop();
        }
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getPanelWidth();
        int height = getPanelHeight();
        int groundY = getGroundY(height);

        drawSky(g2d, width, height);
        drawSunAndHills(g2d, width, height, groundY);
        drawClouds(g2d, groundY);

        for (Pipe pipe : pipes) {
            pipe.render(g2d, groundY);
        }

        drawGround(g2d, width, height, groundY);

        if (pig != null) {
            pig.render(g2d);
        }

        drawScore(g2d);

        if (gameOver) {
            drawGameOverOverlay(g2d, width, height);
        }

        g2d.dispose();
    }

    private void onGameTick(ActionEvent event) {
        if (!initialized) {
            initializeGame();
            repaint();
            return;
        }

        long now = System.nanoTime();
        double deltaSeconds = Math.min(
            MAX_FRAME_DELTA_SECONDS,
            Math.max(0.0, (now - lastTickNanos) / 1_000_000_000.0)
        );
        lastTickNanos = now;

        updateBackground(deltaSeconds);
        if (!gameOver) {
            updateGame(deltaSeconds);
        }

        repaint();
    }

    private void updateGame(double deltaSeconds) {
        int width = getPanelWidth();
        int groundY = getGroundY(getPanelHeight());

        elapsedSeconds += deltaSeconds;
        spawnTimerSeconds += deltaSeconds;

        double pipeSpeed = BASE_PIPE_SPEED
            + Math.min(MAX_ADDED_PIPE_SPEED, (elapsedSeconds * 5.0) + (score * 2.2));

        pig.update(deltaSeconds);

        if (spawnTimerSeconds >= PIPE_SPAWN_INTERVAL_SECONDS) {
            spawnTimerSeconds -= PIPE_SPAWN_INTERVAL_SECONDS;
            spawnPipe(width + 80);
        }

        Iterator<Pipe> iterator = pipes.iterator();
        while (iterator.hasNext()) {
            Pipe pipe = iterator.next();
            pipe.update(deltaSeconds, pipeSpeed);

            if (pipe.checkAndMarkPassed(pig)) {
                score++;
            }

            if (pipe.collides(pig, groundY)) {
                triggerGameOver();
            }

            if (pipe.isOffScreen()) {
                iterator.remove();
            }
        }

        if (pig.getBottom() >= groundY) {
            pig.setY(groundY - pig.getHeight());
            triggerGameOver();
        } else if (pig.getY() < pig.getVisibilityMargin()) {
            pig.setY(pig.getVisibilityMargin());
        }
    }

    private void triggerGameOver() {
        if (gameOver) {
            return;
        }

        gameOver = true;
        if (!scoreSaved) {
            ScoreService.recordScore(GAME_NAME, score);
            scoreSaved = true;
        }
    }

    private void initializeGame() {
        int width = getPanelWidth();
        int height = getPanelHeight();
        int groundY = getGroundY(height);

        if (clouds.isEmpty()) {
            initializeClouds(width, height);
        }

        double startX = width * 0.24;
        pig = new Pig(startX, 0);
        double startY = Math.max(
            Math.max(80, pig.getVisibilityMargin()),
            (groundY - pig.getHeight()) * 0.5
        );
        pig.setY(startY);

        pipes.clear();
        score = 0;
        gameOver = false;
        scoreSaved = false;
        elapsedSeconds = 0.0;
        spawnTimerSeconds = 0.0;
        groundShift = 0.0;
        hillShift = 0.0;
        cloudShift = 0.0;

        spawnPipe(width + 120);
        spawnPipe(width + 460);

        initialized = true;
        lastTickNanos = System.nanoTime();
    }

    private void restartGame() {
        initialized = false;
        initializeGame();
    }

    private void spawnPipe(double requestedStartX) {
        double startX = resolvePipeSpawnX(requestedStartX);
        int height = getPanelHeight();
        int groundY = getGroundY(height);

        // Keep enough opening so pipe caps do not create unfairly tiny pass-through spaces.
        int minGap = Math.max(MIN_PIPE_GAP_PIXELS, (height / 4) + 30);
        int maxGap = Math.max(minGap + 24, Math.min(MAX_PIPE_GAP_PIXELS, (height / 3) + 40));
        int gapHeight = minGap + random.nextInt(Math.max(1, (maxGap - minGap) + 1));

        int topMargin = 70;
        int bottomMargin = 90;

        int minCenter = topMargin + (gapHeight / 2);
        int maxCenter = (groundY - bottomMargin) - (gapHeight / 2);
        int gapCenterY = maxCenter <= minCenter
            ? (groundY / 2)
            : minCenter + random.nextInt((maxCenter - minCenter) + 1);

        pipes.add(new Pipe(startX, PIPE_WIDTH, gapCenterY, gapHeight));
    }

    private double resolvePipeSpawnX(double requestedStartX) {
        double spawnX = requestedStartX;
        for (Pipe pipe : pipes) {
            if (pipe.getX() >= spawnX) {
                spawnX = Math.max(spawnX, pipe.getRightX() + MIN_PIPE_SEPARATION);
            }
        }
        return spawnX;
    }

    private void updateBackground(double deltaSeconds) {
        groundShift += deltaSeconds * 185.0;
        hillShift += deltaSeconds * 52.0;
        cloudShift += deltaSeconds * 46.0;

        int width = getPanelWidth();
        for (Cloud cloud : clouds) {
            cloud.x -= cloud.speed * deltaSeconds;
            if (cloud.x < -(240 * cloud.scale)) {
                cloud.x = width + random.nextInt(220);
                cloud.y = 35 + random.nextInt(Math.max(40, getGroundY(getPanelHeight()) / 2));
            }
        }
    }

    private void bindKeys() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "flap");
        getActionMap().put("flap", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                handleFlapInput();
            }
        });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("R"), "restart");
        getActionMap().put("restart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (gameOver) {
                    restartGame();
                }
            }
        });
    }

    private void bindMouse() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                if (event.getButton() == MouseEvent.BUTTON1) {
                    handleFlapInput();
                }
            }
        });
    }

    private void handleFlapInput() {
        if (pig != null && !gameOver) {
            pig.flap();
        }
    }

    private void drawSky(Graphics2D g2d, int width, int height) {
        GradientPaint skyGradient = new GradientPaint(
            0, 0, new Color(74, 196, 255),
            0, height, new Color(168, 229, 255)
        );
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, width, height);
    }

    private void drawSunAndHills(Graphics2D g2d, int width, int height, int groundY) {
        g2d.setColor(new Color(255, 233, 123, 180));
        g2d.fillOval(width - 240, 40, 140, 140);

        int hillBase = groundY - 60;
        int shift = ((int) Math.round(hillShift)) % 280;
        g2d.setColor(new Color(92, 190, 128));
        for (int x = -280 - shift; x < width + 280; x += 280) {
            g2d.fillOval(x, hillBase - 60, 300, 170);
        }

        g2d.setColor(new Color(67, 166, 103));
        for (int x = -220 - (shift / 2); x < width + 220; x += 220) {
            g2d.fillOval(x, hillBase - 28, 250, 150);
        }
    }

    private void drawClouds(Graphics2D g2d, int groundY) {
        g2d.setStroke(new BasicStroke(1.5f));
        for (Cloud cloud : clouds) {
            int cx = (int) Math.round(cloud.x);
            int cy = (int) Math.round(cloud.y + Math.sin((cloudShift * 0.02) + cloud.phase) * 7.0);
            int w = (int) Math.round(120 * cloud.scale);
            int h = (int) Math.round(58 * cloud.scale);

            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.fillOval(cx, cy, w, h);
            g2d.fillOval(cx + (w / 3), cy - (h / 3), w / 2, h / 2);
            g2d.fillOval(cx + (w / 2), cy + 4, w / 2, h / 2);

            g2d.setColor(new Color(225, 242, 255, 170));
            g2d.drawOval(cx, cy, w, h);

            if (cy > groundY - 20) {
                cloud.y = groundY - 60;
            }
        }
    }

    private void drawGround(Graphics2D g2d, int width, int height, int groundY) {
        GradientPaint groundGradient = new GradientPaint(
            0, groundY, new Color(144, 104, 58),
            0, height, new Color(114, 73, 38)
        );
        g2d.setPaint(groundGradient);
        g2d.fillRect(0, groundY, width, height - groundY);

        int stripeOffset = ((int) Math.round(groundShift)) % 72;
        for (int x = -72 + stripeOffset; x < width + 72; x += 72) {
            g2d.setColor(new Color(172, 124, 72));
            g2d.fillRect(x, groundY + 16, 32, 28);
            g2d.setColor(new Color(98, 64, 35));
            g2d.drawRect(x, groundY + 16, 32, 28);
        }

        g2d.setColor(new Color(198, 156, 83));
        g2d.fillRect(0, groundY, width, 14);
    }

    private void drawScore(Graphics2D g2d) {
        g2d.setFont(SCORE_FONT);
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.drawString(String.valueOf(score), 43, 69);
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.valueOf(score), 40, 66);
    }

    private void drawGameOverOverlay(Graphics2D g2d, int width, int height) {
        g2d.setColor(new Color(0, 0, 0, 132));
        g2d.fillRect(0, 0, width, height);

        int cardWidth = 520;
        int cardHeight = 260;
        int cardX = (width - cardWidth) / 2;
        int cardY = (height - cardHeight) / 2;

        g2d.setColor(new Color(24, 46, 86, 220));
        g2d.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 34, 34);
        g2d.setColor(new Color(141, 226, 255));
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawRoundRect(cardX, cardY, cardWidth, cardHeight, 34, 34);

        g2d.setFont(OVERLAY_TITLE_FONT);
        g2d.setColor(new Color(255, 243, 180));
        g2d.drawString("GAME OVER", cardX + 64, cardY + 92);

        g2d.setFont(OVERLAY_TEXT_FONT);
        g2d.setColor(Color.WHITE);
        g2d.drawString("Score: " + score, cardX + 182, cardY + 148);
        g2d.drawString("Press R to Restart", cardX + 132, cardY + 196);
    }

    private void initializeClouds(int width, int height) {
        clouds.clear();
        int cloudCount = 8;
        int skyLimit = Math.max(90, height - GROUND_HEIGHT - 120);

        for (int i = 0; i < cloudCount; i++) {
            Cloud cloud = new Cloud();
            cloud.x = random.nextInt(Math.max(1, width + 300));
            cloud.y = 28 + random.nextInt(Math.max(40, skyLimit));
            cloud.speed = 24 + random.nextDouble() * 22;
            cloud.scale = 0.8 + random.nextDouble() * 0.8;
            cloud.phase = random.nextDouble() * Math.PI * 2;
            clouds.add(cloud);
        }
    }

    private int getGroundY(int height) {
        return height - GROUND_HEIGHT;
    }

    private int getPanelWidth() {
        return getWidth() > 0 ? getWidth() : 1280;
    }

    private int getPanelHeight() {
        return getHeight() > 0 ? getHeight() : 720;
    }

    private static class Cloud {
        private double x;
        private double y;
        private double speed;
        private double scale;
        private double phase;
    }
}
