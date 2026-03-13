package games.syntaxsnake;

import games.ScoreService;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GamePanel extends JPanel {

    private static final String GAME_NAME = "SyntaxSnake";

    private static final int GRID_COLS = 24;
    private static final int GRID_ROWS = 16;
    private static final int MOVE_INTERVAL_MS = 105;
    private static final int ANIMATION_INTERVAL_MS = 16;
    private static final int LETTER_SCORE = 1;
    private static final int WRONG_LETTER_PENALTY = 1;
    private static final int WORD_BONUS = 5;
    private static final int LETTER_TILE_COUNT = 6;
    private static final int START_LENGTH = 4;

    private static final Font TITLE_FONT = new Font("Dialog", Font.BOLD, 44);
    private static final Font INFO_FONT = new Font("Dialog", Font.BOLD, 24);
    private static final Font PROGRESS_FONT = new Font("Dialog", Font.BOLD, 33);
    private static final Font OVERLAY_TITLE_FONT = new Font("Dialog", Font.BOLD, 56);
    private static final Font OVERLAY_TEXT_FONT = new Font("Dialog", Font.BOLD, 26);

    private static final List<String> WORDS = List.of(
        "JAVA", "CODE", "SNAKE", "SWING", "CLASS",
        "ARRAY", "OBJECT", "METHOD", "STRING", "LOOP"
    );

    private final Random random;
    private final Deque<Point> snake;
    private final List<LetterTile> letterTiles;
    private final Timer moveTimer;
    private final Timer animationTimer;

    private Direction direction;
    private Direction queuedDirection;
    private String currentWord;
    private int nextLetterIndex;
    private int score;
    private int completedWords;
    private boolean running;
    private boolean gameOver;
    private boolean scoreSaved;
    private float pulsePhase;

    public GamePanel() {
        random = new Random();
        snake = new ArrayDeque<>();
        letterTiles = new ArrayList<>();
        pulsePhase = 0f;

        setFocusable(true);
        setDoubleBuffered(true);
        setBackground(new Color(8, 14, 35));

        registerControls();

        moveTimer = new Timer(MOVE_INTERVAL_MS, this::onMoveTick);
        animationTimer = new Timer(ANIMATION_INTERVAL_MS, this::onAnimationTick);

        startNewGame();
        moveTimer.start();
        animationTimer.start();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();
        BoardLayout layout = computeBoardLayout(width, height);

        drawBackground(g2d, width, height);
        drawHud(g2d, width);
        drawBoard(g2d, layout);
        drawLetterTiles(g2d, layout);
        drawSnake(g2d, layout);

        if (gameOver) {
            drawGameOverOverlay(g2d, width, height);
        }

        g2d.dispose();
    }

    @Override
    public void removeNotify() {
        if (moveTimer != null) {
            moveTimer.stop();
        }
        if (animationTimer != null) {
            animationTimer.stop();
        }
        super.removeNotify();
    }

    private void registerControls() {
        bindDirectionKey("UP", Direction.UP);
        bindDirectionKey("DOWN", Direction.DOWN);
        bindDirectionKey("LEFT", Direction.LEFT);
        bindDirectionKey("RIGHT", Direction.RIGHT);
        bindDirectionKey("W", Direction.UP);
        bindDirectionKey("S", Direction.DOWN);
        bindDirectionKey("A", Direction.LEFT);
        bindDirectionKey("D", Direction.RIGHT);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("R"), "restart");
        getActionMap().put("restart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (gameOver) {
                    startNewGame();
                }
            }
        });
    }

    private void bindDirectionKey(String key, Direction newDirection) {
        String actionKey = "move_" + key.toLowerCase();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), actionKey);
        getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!running) {
                    return;
                }
                if (!newDirection.isOpposite(direction)) {
                    queuedDirection = newDirection;
                }
            }
        });
    }

    private void startNewGame() {
        snake.clear();

        int startRow = GRID_ROWS / 2;
        int startCol = GRID_COLS / 2;
        for (int i = 0; i < START_LENGTH; i++) {
            snake.addLast(new Point(startCol - i, startRow));
        }

        direction = Direction.RIGHT;
        queuedDirection = Direction.RIGHT;
        score = 0;
        completedWords = 0;
        running = true;
        gameOver = false;
        scoreSaved = false;

        chooseNextWord();
        spawnLetterTiles();
        requestFocusInWindow();
        repaint();
    }

    private void chooseNextWord() {
        String next = WORDS.get(random.nextInt(WORDS.size()));
        if (currentWord != null && WORDS.size() > 1) {
            while (next.equals(currentWord)) {
                next = WORDS.get(random.nextInt(WORDS.size()));
            }
        }
        currentWord = next;
        nextLetterIndex = 0;
    }

    private void onMoveTick(ActionEvent event) {
        if (!running) {
            return;
        }

        if (queuedDirection != null && !queuedDirection.isOpposite(direction)) {
            direction = queuedDirection;
        }

        Point head = snake.peekFirst();
        Point newHead = new Point(head.x + direction.dx, head.y + direction.dy);
        LetterTile collectedTile = findLetterAt(newHead);
        boolean collected = collectedTile != null;
        boolean correctLetter = collected
            && currentWord != null
            && nextLetterIndex < currentWord.length()
            && collectedTile.letter == currentWord.charAt(nextLetterIndex);

        if (hitsWall(newHead) || hitsSnake(newHead, correctLetter)) {
            triggerGameOver();
            return;
        }

        snake.addFirst(newHead);
        if (collected) {
            letterTiles.remove(collectedTile);
            if (correctLetter) {
                score += LETTER_SCORE;
                nextLetterIndex++;
                if (nextLetterIndex >= currentWord.length()) {
                    score += WORD_BONUS;
                    completedWords++;
                    chooseNextWord();
                    spawnLetterTiles();
                } else {
                    refillLetterTiles();
                }
            } else {
                score -= WRONG_LETTER_PENALTY;
                snake.removeLast();
                refillLetterTiles();
            }
        } else {
            snake.removeLast();
        }

        repaint();
    }

    private void onAnimationTick(ActionEvent event) {
        pulsePhase += 0.06f;
        repaint();
    }

    private boolean hitsWall(Point point) {
        return point.x < 0 || point.x >= GRID_COLS || point.y < 0 || point.y >= GRID_ROWS;
    }

    private boolean hitsSnake(Point point, boolean growing) {
        Point tail = snake.peekLast();
        for (Point segment : snake) {
            if (!growing && tail != null && segment.equals(tail) && point.equals(tail)) {
                continue;
            }
            if (segment.equals(point)) {
                return true;
            }
        }
        return false;
    }

    private LetterTile findLetterAt(Point point) {
        for (LetterTile tile : letterTiles) {
            if (tile.position.equals(point)) {
                return tile;
            }
        }
        return null;
    }

    private void spawnLetterTiles() {
        letterTiles.clear();
        if (currentWord == null || currentWord.isBlank() || nextLetterIndex >= currentWord.length()) {
            return;
        }

        Set<Point> occupied = new HashSet<>(snake);

        Point requiredPosition = randomOpenCell(occupied);
        if (requiredPosition == null) {
            triggerGameOver();
            return;
        }

        char required = currentWord.charAt(nextLetterIndex);
        letterTiles.add(new LetterTile(requiredPosition, required));

        int distractorCount = Math.max(1, LETTER_TILE_COUNT - 1);
        for (int i = 0; i < distractorCount; i++) {
            Point distractorPosition = randomOpenCell(occupied);
            if (distractorPosition == null) {
                break;
            }
            letterTiles.add(new LetterTile(distractorPosition, randomDistractorLetter(required)));
        }
    }

    private void refillLetterTiles() {
        if (currentWord == null || currentWord.isBlank() || nextLetterIndex >= currentWord.length()) {
            letterTiles.clear();
            return;
        }

        char required = currentWord.charAt(nextLetterIndex);
        boolean hasRequired = false;
        for (LetterTile tile : letterTiles) {
            if (tile.letter == required) {
                hasRequired = true;
                break;
            }
        }

        Set<Point> occupied = new HashSet<>(snake);
        for (LetterTile tile : letterTiles) {
            occupied.add(tile.position);
        }

        while (letterTiles.size() < LETTER_TILE_COUNT) {
            Point candidate = randomOpenCell(occupied);
            if (candidate == null) {
                triggerGameOver();
                return;
            }

            if (!hasRequired) {
                letterTiles.add(new LetterTile(candidate, required));
                hasRequired = true;
            } else {
                letterTiles.add(new LetterTile(candidate, randomDistractorLetter(required)));
            }
        }
    }

    private Point randomOpenCell(Set<Point> occupied) {
        int attempts = 0;
        while (attempts < 1500) {
            Point candidate = new Point(random.nextInt(GRID_COLS), random.nextInt(GRID_ROWS));
            if (!occupied.contains(candidate)) {
                occupied.add(candidate);
                return candidate;
            }
            attempts++;
        }
        return null;
    }

    private char randomDistractorLetter(char required) {
        char letter;
        do {
            letter = (char) ('A' + random.nextInt(26));
        } while (letter == required);
        return letter;
    }

    private void triggerGameOver() {
        running = false;
        gameOver = true;
        if (!scoreSaved) {
            ScoreService.recordScore(GAME_NAME, score);
            scoreSaved = true;
        }
        repaint();
    }

    private void drawBackground(Graphics2D g2d, int width, int height) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(pulsePhase);
        Color top = blend(new Color(10, 22, 50), new Color(29, 55, 99), pulse * 0.45f);
        Color bottom = blend(new Color(8, 16, 34), new Color(24, 21, 63), pulse * 0.40f);

        g2d.setPaint(new GradientPaint(0, 0, top, 0, height, bottom));
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(new Color(136, 220, 255, 28));
        for (int y = 0; y < height; y += 4) {
            g2d.drawLine(0, y, width, y);
        }
    }

    private void drawHud(Graphics2D g2d, int width) {
        g2d.setFont(TITLE_FONT);
        g2d.setColor(new Color(145, 243, 255));
        drawCenteredString(g2d, "SYNTAXSNAKE", width / 2, 50);

        g2d.setFont(INFO_FONT);
        g2d.setColor(new Color(255, 220, 122));
        drawCenteredString(g2d, "Current Word: " + currentWord, width / 2, 84);

        drawWordProgress(g2d, width / 2, 120);

        g2d.setFont(INFO_FONT);
        g2d.setColor(new Color(227, 240, 255));
        drawCenteredString(
            g2d,
            "Score: " + score + "    Words Completed: " + completedWords + "    Bonus: +" + WORD_BONUS + "    Wrong: -" + WRONG_LETTER_PENALTY,
            width / 2,
            154
        );
    }

    private void drawWordProgress(Graphics2D g2d, int centerX, int baselineY) {
        if (currentWord == null || currentWord.isBlank()) {
            return;
        }

        g2d.setFont(PROGRESS_FONT);
        int spacing = 24;
        int tokenCount = currentWord.length();
        int totalWidth = 0;
        for (int i = 0; i < tokenCount; i++) {
            totalWidth += g2d.getFontMetrics().stringWidth("W");
            if (i < tokenCount - 1) {
                totalWidth += spacing;
            }
        }

        int x = centerX - (totalWidth / 2);
        for (int i = 0; i < tokenCount; i++) {
            boolean collected = i < nextLetterIndex;
            char display = collected ? currentWord.charAt(i) : '_';
            g2d.setColor(collected ? new Color(104, 255, 168) : new Color(215, 228, 255));
            g2d.drawString(String.valueOf(display), x, baselineY);
            x += g2d.getFontMetrics().stringWidth("W") + spacing;
        }
    }

    private void drawBoard(Graphics2D g2d, BoardLayout layout) {
        g2d.setColor(new Color(9, 19, 43, 220));
        g2d.fillRoundRect(layout.boardX, layout.boardY, layout.boardWidth, layout.boardHeight, 22, 22);

        g2d.setColor(new Color(120, 231, 255, 205));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawRoundRect(layout.boardX, layout.boardY, layout.boardWidth, layout.boardHeight, 22, 22);

        g2d.setStroke(new BasicStroke(1f));
        g2d.setColor(new Color(142, 202, 255, 44));
        for (int col = 1; col < GRID_COLS; col++) {
            int x = layout.boardX + (col * layout.cellSize);
            g2d.drawLine(x, layout.boardY, x, layout.boardY + layout.boardHeight);
        }
        for (int row = 1; row < GRID_ROWS; row++) {
            int y = layout.boardY + (row * layout.cellSize);
            g2d.drawLine(layout.boardX, y, layout.boardX + layout.boardWidth, y);
        }
    }

    private void drawLetterTiles(Graphics2D g2d, BoardLayout layout) {
        if (letterTiles.isEmpty()) {
            return;
        }

        char required = currentWord != null && nextLetterIndex < currentWord.length()
            ? currentWord.charAt(nextLetterIndex)
            : 0;

        int inset = Math.max(3, layout.cellSize / 10);
        int size = layout.cellSize - (inset * 2);
        g2d.setFont(new Font("Dialog", Font.BOLD, Math.max(16, layout.cellSize - 8)));

        for (LetterTile tile : letterTiles) {
            int cellX = layout.boardX + (tile.position.x * layout.cellSize);
            int cellY = layout.boardY + (tile.position.y * layout.cellSize);
            float hue = ((tile.letter * 37) % 360) / 360f;
            Color tileColor = Color.getHSBColor(hue, 0.72f, 1.0f);
            Color tileShade = blend(tileColor, new Color(255, 255, 255), 0.35f);

            g2d.setPaint(new GradientPaint(cellX, cellY, tileShade, cellX, cellY + size, tileColor.darker()));
            g2d.fillRoundRect(cellX + inset, cellY + inset, size, size, 12, 12);

            if (tile.letter == required) {
                g2d.setColor(new Color(255, 250, 184, 240));
                g2d.setStroke(new BasicStroke(3f));
            } else {
                g2d.setColor(new Color(255, 255, 255, 210));
                g2d.setStroke(new BasicStroke(2f));
            }
            g2d.drawRoundRect(cellX + inset, cellY + inset, size, size, 12, 12);

            String letterText = String.valueOf(tile.letter);
            int tx = cellX + inset + (size - g2d.getFontMetrics().stringWidth(letterText)) / 2;
            int ty = cellY + inset + (size + g2d.getFontMetrics().getAscent()) / 2 - 4;
            g2d.setColor(new Color(20, 24, 50));
            g2d.drawString(letterText, tx, ty);
        }
    }

    private void drawSnake(Graphics2D g2d, BoardLayout layout) {
        int inset = Math.max(2, layout.cellSize / 10);
        int size = layout.cellSize - (inset * 2);

        int index = 0;
        for (Point segment : snake) {
            int x = layout.boardX + (segment.x * layout.cellSize) + inset;
            int y = layout.boardY + (segment.y * layout.cellSize) + inset;

            if (index == 0) {
                g2d.setColor(new Color(124, 255, 191));
                g2d.fillRoundRect(x, y, size, size, 12, 12);
                g2d.setColor(new Color(29, 70, 55));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(x, y, size, size, 12, 12);
                drawHeadEyes(g2d, x, y, size);
            } else {
                float t = Math.min(1f, index / 12f);
                Color body = blend(new Color(88, 225, 255), new Color(88, 255, 146), t);
                g2d.setColor(body);
                g2d.fillRoundRect(x, y, size, size, 10, 10);
                g2d.setColor(new Color(16, 63, 80, 145));
                g2d.drawRoundRect(x, y, size, size, 10, 10);
            }
            index++;
        }
    }

    private void drawHeadEyes(Graphics2D g2d, int x, int y, int size) {
        int eyeSize = Math.max(3, size / 7);
        int eyeY = y + Math.max(4, size / 3);
        int leftX = x + Math.max(4, size / 4);
        int rightX = x + size - eyeSize - Math.max(4, size / 4);
        g2d.setColor(new Color(19, 30, 51));
        g2d.fillOval(leftX, eyeY, eyeSize, eyeSize);
        g2d.fillOval(rightX, eyeY, eyeSize, eyeSize);
    }

    private void drawGameOverOverlay(Graphics2D g2d, int width, int height) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, width, height);

        int cardW = 560;
        int cardH = 250;
        int cardX = (width - cardW) / 2;
        int cardY = (height - cardH) / 2;

        g2d.setColor(new Color(14, 34, 73, 230));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 30, 30);
        g2d.setColor(new Color(138, 241, 255));
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 30, 30);

        g2d.setFont(OVERLAY_TITLE_FONT);
        g2d.setColor(new Color(255, 239, 159));
        drawCenteredString(g2d, "GAME OVER", width / 2, cardY + 92);

        g2d.setFont(OVERLAY_TEXT_FONT);
        g2d.setColor(new Color(235, 246, 255));
        drawCenteredString(g2d, "Final Score: " + score, width / 2, cardY + 152);
        drawCenteredString(g2d, "Press R to Restart", width / 2, cardY + 197);
    }

    private BoardLayout computeBoardLayout(int width, int height) {
        int topSection = 182;
        int sideMargin = 36;
        int availableWidth = Math.max(360, width - (sideMargin * 2));
        int availableHeight = Math.max(300, height - topSection - 32);

        int cellSize = Math.max(18, Math.min(availableWidth / GRID_COLS, availableHeight / GRID_ROWS));
        int boardWidth = cellSize * GRID_COLS;
        int boardHeight = cellSize * GRID_ROWS;
        int boardX = (width - boardWidth) / 2;
        int boardY = topSection + Math.max(12, (height - topSection - boardHeight) / 2);

        return new BoardLayout(cellSize, boardX, boardY, boardWidth, boardHeight);
    }

    private void drawCenteredString(Graphics2D g2d, String text, int centerX, int baselineY) {
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, centerX - (textWidth / 2), baselineY);
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

        private BoardLayout(int cellSize, int boardX, int boardY, int boardWidth, int boardHeight) {
            this.cellSize = cellSize;
            this.boardX = boardX;
            this.boardY = boardY;
            this.boardWidth = boardWidth;
            this.boardHeight = boardHeight;
        }
    }

    private enum Direction {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0);

        private final int dx;
        private final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        private boolean isOpposite(Direction other) {
            return (dx + other.dx == 0) && (dy + other.dy == 0);
        }
    }

    private static class LetterTile {
        private final Point position;
        private final char letter;

        private LetterTile(Point position, char letter) {
            this.position = position;
            this.letter = letter;
        }
    }
}
