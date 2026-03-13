package games.cakemaker;

import games.ScoreService;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class GamePanel extends JPanel {

    private static final String GAME_NAME = "Cake Maker";
    private static final int ARCADE_STAR_COUNT = 140;
    private static final Font TITLE_FONT = new Font("Dialog", Font.BOLD, 34);
    private static final Font SECTION_FONT = new Font("Dialog", Font.BOLD, 22);
    private static final Font BODY_FONT = new Font("Dialog", Font.BOLD, 18);
    private static final Font SMALL_FONT = new Font("Dialog", Font.PLAIN, 15);

    private final GameLogic gameLogic;
    private final OrderGenerator orderGenerator;

    private final JLabel scoreLabel;
    private final JLabel timerLabel;
    private final JLabel roundLabel;
    private final JLabel progressLabel;
    private final JLabel orderSummaryLabel;
    private final JLabel feedbackLabel;
    private final JLabel titleLabel;

    private final CakeCanvas orderCanvas;
    private final CakeCanvas buildCanvas;

    private final Timer roundTimer;
    private final Timer animationTimer;
    private final Random random;
    private Timer feedbackTimer;

    private final List<ConfettiPiece> confettiPieces;
    private final List<ArcadeStar> arcadeStars;
    private boolean celebrationActive;
    private int celebrationFrames;

    private int animatedLayerIndex;
    private double stackAnimationProgress;
    private boolean roundLocked;
    private boolean scorePersisted;
    private float neonPhase;
    private float gridShift;
    private boolean arcadeBackgroundInitialized;

    public GamePanel() {
        this.orderGenerator = new OrderGenerator();
        this.gameLogic = new GameLogic(orderGenerator, new ScoreManager());
        this.random = new Random();
        this.confettiPieces = new ArrayList<>();
        this.arcadeStars = new ArrayList<>();
        this.celebrationActive = false;
        this.celebrationFrames = 0;
        this.animatedLayerIndex = -1;
        this.stackAnimationProgress = 1.0;
        this.roundLocked = false;
        this.scorePersisted = false;
        this.feedbackTimer = null;
        this.neonPhase = 0f;
        this.gridShift = 0f;
        this.arcadeBackgroundInitialized = false;

        setLayout(new BorderLayout(18, 18));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setOpaque(false);

        titleLabel = new JLabel("CAKE MAKER ARCADE", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(new Color(122, 240, 255));
        add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(16, 16));
        centerPanel.setOpaque(false);
        add(centerPanel, BorderLayout.CENTER);

        JPanel orderPanel = createCardPanel();
        orderPanel.setPreferredSize(new Dimension(340, 540));
        orderPanel.setLayout(new BorderLayout(10, 10));
        orderPanel.add(createSectionLabel("Customer Order"), BorderLayout.NORTH);
        orderCanvas = new CakeCanvas(true);
        orderPanel.add(orderCanvas, BorderLayout.CENTER);
        orderSummaryLabel = new JLabel("", SwingConstants.CENTER);
        orderSummaryLabel.setFont(SMALL_FONT);
        orderSummaryLabel.setForeground(new Color(176, 210, 255));
        orderSummaryLabel.setVerticalAlignment(SwingConstants.TOP);
        orderPanel.add(orderSummaryLabel, BorderLayout.SOUTH);

        JPanel buildPanel = createCardPanel();
        buildPanel.setLayout(new BorderLayout(10, 10));
        buildPanel.add(createSectionLabel("Cake Building Area"), BorderLayout.NORTH);
        buildCanvas = new CakeCanvas(false);
        buildPanel.add(buildCanvas, BorderLayout.CENTER);
        JPanel buildInfoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        buildInfoPanel.setOpaque(false);
        JLabel hintLabel = new JLabel("Pick ingredients, then press Serve Cake", SwingConstants.CENTER);
        hintLabel.setFont(SMALL_FONT);
        hintLabel.setForeground(new Color(171, 199, 242));
        feedbackLabel = new JLabel(" ", SwingConstants.CENTER);
        feedbackLabel.setFont(SMALL_FONT);
        feedbackLabel.setForeground(new Color(121, 255, 187));
        buildInfoPanel.add(hintLabel);
        buildInfoPanel.add(feedbackLabel);
        buildPanel.add(buildInfoPanel, BorderLayout.SOUTH);

        JPanel ingredientPanel = createIngredientPanel();
        ingredientPanel.setPreferredSize(new Dimension(340, 540));

        centerPanel.add(orderPanel, BorderLayout.WEST);
        centerPanel.add(buildPanel, BorderLayout.CENTER);
        centerPanel.add(ingredientPanel, BorderLayout.EAST);

        JPanel statusPanel = createCardPanel();
        statusPanel.setLayout(new GridLayout(1, 4, 12, 0));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        scoreLabel = createStatusValueLabel();
        timerLabel = createStatusValueLabel();
        roundLabel = createStatusValueLabel();
        progressLabel = createStatusValueLabel();
        statusPanel.add(scoreLabel);
        statusPanel.add(timerLabel);
        statusPanel.add(roundLabel);
        statusPanel.add(progressLabel);
        add(statusPanel, BorderLayout.SOUTH);

        roundTimer = new Timer(1000, this::handleRoundTick);
        animationTimer = new Timer(16, this::handleAnimationTick);
        roundTimer.start();
        animationTimer.start();

        updateAllDisplays();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();
        initializeArcadeBackgroundIfNeeded(width, height);
        drawArcadeBackground(g2d, width, height);
        g2d.dispose();
    }

    private void initializeArcadeBackgroundIfNeeded(int width, int height) {
        if (arcadeBackgroundInitialized || width <= 0 || height <= 0) {
            return;
        }

        arcadeStars.clear();
        for (int i = 0; i < ARCADE_STAR_COUNT; i++) {
            ArcadeStar star = new ArcadeStar();
            resetArcadeStar(star, width, height, true);
            arcadeStars.add(star);
        }
        arcadeBackgroundInitialized = true;
    }

    private void drawArcadeBackground(Graphics2D g2d, int width, int height) {
        GradientPaint base = new GradientPaint(
            0, 0, new Color(7, 11, 30),
            0, height, new Color(17, 8, 44)
        );
        g2d.setPaint(base);
        g2d.fillRect(0, 0, width, height);

        int pinkRadius = 200 + (int) (Math.sin(neonPhase * 0.85f) * 22f);
        int cyanRadius = 168 + (int) (Math.cos(neonPhase * 1.1f) * 20f);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.24f));
        g2d.setColor(new Color(255, 72, 180));
        g2d.fillOval((width / 2) - pinkRadius, (height / 2) - pinkRadius - 34, pinkRadius * 2, pinkRadius * 2);
        g2d.setColor(new Color(75, 230, 255));
        g2d.fillOval((width / 2) - cyanRadius, (height / 2) - cyanRadius - 12, cyanRadius * 2, cyanRadius * 2);
        g2d.setComposite(AlphaComposite.SrcOver);

        drawArcadeStars(g2d);
        drawArcadeGrid(g2d, width, height);
        drawArcadeScanlines(g2d, width, height);
    }

    private void drawArcadeStars(Graphics2D g2d) {
        for (ArcadeStar star : arcadeStars) {
            g2d.setColor(new Color(178, 228, 255, star.alpha));
            g2d.fillOval(Math.round(star.x), Math.round(star.y), star.size, star.size);
        }
    }

    private void drawArcadeGrid(Graphics2D g2d, int width, int height) {
        int horizonY = Math.max(height / 3, 170);
        g2d.setStroke(new BasicStroke(1.2f));

        for (int i = 0; i <= 11; i++) {
            float progress = i / 11f;
            int y = horizonY + (int) (Math.pow(progress, 1.75) * (height - horizonY));
            int alpha = 50 + (int) (progress * 100);
            int wave = (int) Math.round(Math.sin((gridShift + (i * 6.7f)) * 0.1f) * 9.0);
            g2d.setColor(new Color(89, 238, 255, alpha));
            g2d.drawLine(wave, y, width + wave, y);
        }

        for (int x = -width; x < width * 2; x += 78) {
            int bottomX = x + Math.round(gridShift);
            int topX = (width / 2) + (int) ((bottomX - (width / 2.0)) * 0.15);
            g2d.setColor(new Color(255, 88, 204, 120));
            g2d.drawLine(bottomX, height, topX, horizonY);
        }
    }

    private void drawArcadeScanlines(Graphics2D g2d, int width, int height) {
        g2d.setColor(new Color(6, 8, 23, 56));
        for (int y = 0; y < height; y += 4) {
            g2d.drawLine(0, y, width, y);
        }
    }

    private void resetArcadeStar(ArcadeStar star, int width, int height, boolean randomY) {
        star.x = random.nextFloat() * Math.max(1, width);
        star.y = randomY ? random.nextFloat() * Math.max(1, height) : -random.nextInt(160) - 6;
        star.speed = 0.4f + random.nextFloat() * 1.2f;
        star.phase = random.nextFloat() * 6.2f;
        star.size = 1 + random.nextInt(3);
        star.alpha = 60 + random.nextInt(120);
    }

    private JPanel createIngredientPanel() {
        JPanel ingredientPanel = createCardPanel();
        ingredientPanel.setLayout(new BorderLayout(0, 12));

        JLabel ingredientTitle = createSectionLabel("Ingredient Selection");
        ingredientPanel.add(ingredientTitle, BorderLayout.NORTH);

        JPanel optionColumn = new JPanel();
        optionColumn.setOpaque(false);
        optionColumn.setLayout(new BoxLayout(optionColumn, BoxLayout.Y_AXIS));

        JPanel baseSection = createOptionSection(
            "Cake Bases",
            orderGenerator.getBaseOptions(),
            new Color(255, 126, 90),
            this::onBaseSelected
        );
        baseSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionColumn.add(baseSection);
        optionColumn.add(Box.createVerticalStrut(10));

        JPanel creamSection = createOptionSection(
            "Cream Layers",
            orderGenerator.getCreamOptions(),
            new Color(255, 182, 84),
            this::onCreamSelected
        );
        creamSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionColumn.add(creamSection);
        optionColumn.add(Box.createVerticalStrut(10));

        JPanel toppingSection = createOptionSection(
            "Toppings",
            orderGenerator.getToppingOptions(),
            new Color(68, 206, 255),
            this::onToppingSelected
        );
        toppingSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionColumn.add(toppingSection);
        optionColumn.add(Box.createVerticalGlue());

        ingredientPanel.add(optionColumn, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new GridLayout(3, 1, 8, 8));
        controlPanel.setOpaque(false);

        JButton serveButton = createIngredientButton("Serve Cake", new Color(59, 215, 152), e -> onServeCake());
        JButton clearButton = createIngredientButton("Clear Cake", new Color(255, 145, 88), e -> onClearCake());
        JButton exitButton = createIngredientButton("Exit Game (Esc)", new Color(240, 106, 189), e -> closeGame());

        controlPanel.add(serveButton);
        controlPanel.add(clearButton);
        controlPanel.add(exitButton);
        ingredientPanel.add(controlPanel, BorderLayout.SOUTH);
        return ingredientPanel;
    }

    private JPanel createOptionSection(String title, List<String> options, Color color, Consumer<String> consumer) {
        JPanel section = new JPanel(new BorderLayout(6, 6));
        section.setOpaque(false);

        JLabel sectionTitle = new JLabel(title);
        sectionTitle.setFont(new Font("Dialog", Font.BOLD, 18));
        sectionTitle.setForeground(new Color(136, 224, 255));
        section.add(sectionTitle, BorderLayout.NORTH);

        JPanel buttonGrid = new JPanel(new GridLayout(0, 1, 6, 6));
        buttonGrid.setOpaque(false);
        for (String option : options) {
            JButton button = createIngredientButton(option, color, e -> consumer.accept(option));
            buttonGrid.add(button);
        }
        section.add(buttonGrid, BorderLayout.CENTER);

        return section;
    }

    private JButton createIngredientButton(String text, Color bgColor, Consumer<ActionEvent> consumer) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(BODY_FONT);
        button.setBackground(bgColor);
        button.setForeground(new Color(239, 247, 255));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(181, 235, 255, 180), 2, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(consumer::accept);

        Color hoverColor = blend(bgColor, new Color(255, 255, 255), 0.18f);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        return button;
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(SECTION_FONT);
        label.setForeground(new Color(118, 232, 255));
        return label;
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(new Color(8, 18, 46, 218));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(106, 229, 255, 185), 2, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return panel;
    }

    private JLabel createStatusValueLabel() {
        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setFont(BODY_FONT);
        label.setForeground(new Color(200, 226, 255));
        return label;
    }

    private void onBaseSelected(String base) {
        if (roundLocked) {
            return;
        }
        gameLogic.selectBase(base);
        startLayerAnimation(0);
        buildCanvas.repaint();
    }

    private void onCreamSelected(String cream) {
        if (roundLocked) {
            return;
        }
        gameLogic.selectCream(cream);
        startLayerAnimation(1);
        buildCanvas.repaint();
    }

    private void onToppingSelected(String topping) {
        if (roundLocked) {
            return;
        }
        int previousLayers = gameLogic.getPlayerCake().getTotalLayers();
        gameLogic.addTopping(topping);
        int newLayers = gameLogic.getPlayerCake().getTotalLayers();
        if (newLayers > previousLayers) {
            startLayerAnimation(newLayers - 1);
        }
        buildCanvas.repaint();
    }

    private void onClearCake() {
        if (roundLocked) {
            return;
        }
        gameLogic.clearPlayerCake();
        resetStackAnimation();
        buildCanvas.repaint();
    }

    private void onServeCake() {
        if (roundLocked) {
            return;
        }

        roundLocked = true;
        roundTimer.stop();
        Cake expectedOrder = gameLogic.getCurrentOrder();
        GameLogic.ServeResult result = gameLogic.serveCake();

        if (result == GameLogic.ServeResult.SUCCESS) {
            startCelebration();
            Timer pause = new Timer(900, e -> {
                ((Timer) e.getSource()).stop();
                showRoundFeedback(
                    "Perfect order! +" + (10 + expectedOrder.getToppings().size() * 2) + " points",
                    new Color(117, 255, 188),
                    2500
                );
                startNextRound();
            });
            pause.setRepeats(false);
            pause.start();
        } else {
            showRoundFeedback(
                "Oops! Cake did not match. New customer arrived.",
                new Color(255, 118, 118),
                2300
            );
            startNextRound();
        }
    }

    private void handleRoundTick(ActionEvent event) {
        if (roundLocked) {
            return;
        }

        boolean timedOut = gameLogic.tickSecond();
        updateStatusLabels();

        if (!timedOut) {
            return;
        }

        roundLocked = true;
        roundTimer.stop();
        gameLogic.markRoundFailed();
        showRoundFeedback(
            "Time is up! This order was missed.",
            new Color(255, 118, 118),
            2300
        );
        startNextRound();
    }

    private void handleAnimationTick(ActionEvent event) {
        neonPhase += 0.055f;
        gridShift = (gridShift + 1.9f) % 82f;

        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        initializeArcadeBackgroundIfNeeded(width, height);
        for (ArcadeStar star : arcadeStars) {
            star.y += star.speed;
            star.x += Math.sin((neonPhase + star.phase) * 0.7f) * 0.16f;
            if (star.y > height + 12) {
                resetArcadeStar(star, width, height, false);
            }
            if (star.x < -8) {
                star.x = width + 6;
            } else if (star.x > width + 8) {
                star.x = -6;
            }
        }

        if (stackAnimationProgress < 1.0) {
            stackAnimationProgress = Math.min(1.0, stackAnimationProgress + 0.11);
        }

        if (celebrationActive) {
            for (ConfettiPiece piece : confettiPieces) {
                piece.y += piece.speed;
                piece.x += piece.drift;
                if (piece.y > buildCanvas.getHeight() + 20) {
                    piece.y = -random.nextInt(200);
                    piece.x = random.nextInt(Math.max(20, buildCanvas.getWidth() - 20));
                }
            }

            celebrationFrames--;
            if (celebrationFrames <= 0) {
                celebrationActive = false;
                confettiPieces.clear();
            }
        }

        float pulse = 0.5f + 0.5f * (float) Math.sin(neonPhase * 1.3f);
        titleLabel.setForeground(blend(new Color(105, 242, 255), new Color(255, 109, 199), pulse));

        buildCanvas.repaint();
        orderCanvas.repaint();
        repaint();
    }

    private void startLayerAnimation(int layerIndex) {
        animatedLayerIndex = layerIndex;
        stackAnimationProgress = 0.0;
    }

    private void resetStackAnimation() {
        animatedLayerIndex = -1;
        stackAnimationProgress = 1.0;
    }

    private void startCelebration() {
        celebrationActive = true;
        celebrationFrames = 70;
        confettiPieces.clear();

        int width = Math.max(80, buildCanvas.getWidth());
        for (int i = 0; i < 110; i++) {
            ConfettiPiece piece = new ConfettiPiece();
            piece.x = random.nextInt(width);
            piece.y = -random.nextInt(260);
            piece.size = 4 + random.nextInt(6);
            piece.speed = 1.8 + random.nextDouble() * 3.2;
            piece.drift = (random.nextDouble() - 0.5) * 1.3;
            piece.color = new Color(
                85 + random.nextInt(170),
                90 + random.nextInt(166),
                120 + random.nextInt(136),
                210
            );
            confettiPieces.add(piece);
        }
    }

    private void startNextRound() {
        roundLocked = false;
        celebrationActive = false;
        confettiPieces.clear();
        resetStackAnimation();
        gameLogic.startNewRound();
        updateAllDisplays();
        roundTimer.start();
    }

    private void showRoundFeedback(String message, Color color, int durationMs) {
        feedbackLabel.setForeground(color);
        feedbackLabel.setText(message);

        if (feedbackTimer != null && feedbackTimer.isRunning()) {
            feedbackTimer.stop();
        }

        feedbackTimer = new Timer(durationMs, event -> {
            feedbackLabel.setText(" ");
            ((Timer) event.getSource()).stop();
        });
        feedbackTimer.setRepeats(false);
        feedbackTimer.start();
    }

    private void updateAllDisplays() {
        updateOrderDisplay();
        updateStatusLabels();
        buildCanvas.repaint();
        orderCanvas.repaint();
    }

    private void updateOrderDisplay() {
        Cake order = gameLogic.getCurrentOrder();
        String toppings = order.getToppings().isEmpty()
            ? "None"
            : String.join(", ", order.getToppings());
        int steps = order.getTotalLayers();
        orderSummaryLabel.setText(
            "<html><div style='text-align:center;'>"
                + "<b>Steps:</b> " + steps + "<br/>"
                + "<b>Base:</b> " + order.getBase() + "<br/>"
                + "<b>Cream:</b> " + order.getCream() + "<br/>"
                + "<b>Toppings:</b> " + toppings
                + "</div></html>"
        );
        orderCanvas.repaint();
    }

    private void updateStatusLabels() {
        ScoreManager scoreManager = gameLogic.getScoreManager();
        scoreLabel.setText("Score: " + scoreManager.getScore());
        timerLabel.setText("Time: " + gameLogic.getTimeRemaining() + "s");
        roundLabel.setText("Round: " + gameLogic.getCurrentRound());
        progressLabel.setText("Wins: " + scoreManager.getSuccessfulRounds() + "/" + scoreManager.getRoundCount());

        if (gameLogic.getTimeRemaining() <= 10) {
            timerLabel.setForeground(new Color(255, 115, 107));
        } else {
            timerLabel.setForeground(new Color(200, 226, 255));
        }
    }

    private void closeGame() {
        persistScoreIfNeeded();
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }

    @Override
    public void removeNotify() {
        persistScoreIfNeeded();
        if (roundTimer != null) {
            roundTimer.stop();
        }
        if (animationTimer != null) {
            animationTimer.stop();
        }
        if (feedbackTimer != null) {
            feedbackTimer.stop();
        }
        super.removeNotify();
    }

    private void persistScoreIfNeeded() {
        if (scorePersisted) {
            return;
        }

        int score = gameLogic.getScoreManager().getScore();
        if (score > 0) {
            ScoreService.recordScore(GAME_NAME, score);
        }
        scorePersisted = true;
    }

    private void drawCake(Graphics2D g2d, Cake cake, int width, int height, boolean orderPreview) {
        if (cake == null || cake.getTotalLayers() == 0) {
            String message = orderPreview ? "Waiting for order..." : "Start building your cake!";
            g2d.setColor(new Color(175, 213, 255));
            g2d.setFont(new Font("Dialog", Font.BOLD, orderPreview ? 17 : 22));
            int textWidth = g2d.getFontMetrics().stringWidth(message);
            g2d.drawString(message, (width - textWidth) / 2, height / 2);
            return;
        }

        int centerX = width / 2;
        int plateWidth = orderPreview ? 170 : 260;
        int plateHeight = orderPreview ? 22 : 30;
        int plateY = height - (orderPreview ? 58 : 68);
        int plateX = centerX - (plateWidth / 2);

        g2d.setColor(new Color(223, 236, 255));
        g2d.fillRoundRect(plateX, plateY, plateWidth, plateHeight, 22, 22);
        g2d.setColor(new Color(109, 206, 255));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(plateX, plateY, plateWidth, plateHeight, 22, 22);

        int cakeWidth = orderPreview ? 130 : 200;
        int baseHeight = orderPreview ? 34 : 48;
        int creamHeight = orderPreview ? 16 : 24;
        int topY = plateY - 4;

        int layerIndex = 0;
        if (cake.getBase() != null) {
            int y = topY - baseHeight - getAnimationOffset(orderPreview, layerIndex);
            drawBaseLayer(g2d, centerX, y, cakeWidth, baseHeight, cake.getBase());
            topY = y + 8;
            layerIndex++;
        }

        if (cake.getCream() != null) {
            int y = topY - creamHeight - getAnimationOffset(orderPreview, layerIndex);
            drawCreamLayer(g2d, centerX, y, cakeWidth - 12, creamHeight, cake.getCream());
            topY = y + 2;
            layerIndex++;
        }

        if (!cake.getToppings().isEmpty()) {
            int toppingY = topY - 8;
            int[] offsets = {-52, -26, 0, 26, 52};
            for (int i = 0; i < cake.getToppings().size(); i++) {
                int x = centerX + offsets[i % offsets.length];
                int y = toppingY - getAnimationOffset(orderPreview, layerIndex + i);
                drawTopping(g2d, x, y, cake.getToppings().get(i), orderPreview);
            }
        }
    }

    private int getAnimationOffset(boolean orderPreview, int layerIndex) {
        if (orderPreview || animatedLayerIndex != layerIndex || stackAnimationProgress >= 1.0) {
            return 0;
        }
        return (int) Math.round((1.0 - stackAnimationProgress) * 34.0);
    }

    private void drawBaseLayer(Graphics2D g2d, int centerX, int y, int width, int height, String base) {
        Color baseColor = new Color(244, 214, 162);
        if (OrderGenerator.BASE_CHOCOLATE.equals(base)) {
            baseColor = new Color(128, 84, 58);
        } else if (OrderGenerator.BASE_STRAWBERRY.equals(base)) {
            baseColor = new Color(245, 162, 186);
        }

        int x = centerX - (width / 2);
        g2d.setColor(baseColor);
        g2d.fillRoundRect(x, y, width, height, 24, 24);
        g2d.setColor(baseColor.darker());
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(x, y, width, height, 24, 24);
    }

    private void drawCreamLayer(Graphics2D g2d, int centerX, int y, int width, int height, String cream) {
        Color creamColor = new Color(255, 251, 236);
        if (OrderGenerator.CREAM_CHOCOLATE.equals(cream)) {
            creamColor = new Color(173, 124, 99);
        } else if (OrderGenerator.CREAM_STRAWBERRY.equals(cream)) {
            creamColor = new Color(255, 189, 214);
        }

        int x = centerX - (width / 2);
        g2d.setColor(creamColor);
        g2d.fillRoundRect(x, y, width, height, 18, 18);
        g2d.setColor(creamColor.darker());
        g2d.drawRoundRect(x, y, width, height, 18, 18);
    }

    private void drawTopping(Graphics2D g2d, int x, int y, String topping, boolean orderPreview) {
        int size = orderPreview ? 11 : 14;

        if (OrderGenerator.TOPPING_CHERRY.equals(topping)) {
            g2d.setColor(new Color(208, 30, 64));
            g2d.fillOval(x - size / 2, y - size / 2, size, size);
            g2d.setColor(new Color(41, 124, 47));
            g2d.drawLine(x, y - size / 2, x + 3, y - size);
            return;
        }

        if (OrderGenerator.TOPPING_STRAWBERRY.equals(topping)) {
            g2d.setColor(new Color(232, 72, 100));
            g2d.fillOval(x - size / 2, y - size / 2, size, size);
            g2d.setColor(new Color(255, 248, 186));
            g2d.fillOval(x - 2, y - 1, 2, 2);
            g2d.fillOval(x + 1, y + 1, 2, 2);
            g2d.setColor(new Color(46, 145, 73));
            g2d.fillOval(x - 2, y - size / 2 - 1, 4, 3);
            return;
        }

        if (OrderGenerator.TOPPING_CHOC_CHIPS.equals(topping)) {
            g2d.setColor(new Color(92, 55, 39));
            g2d.fillRoundRect(x - size / 2, y - size / 3, size, size / 2 + 1, 5, 5);
            return;
        }

        g2d.setColor(new Color(255, 230, 90));
        g2d.fillOval(x - size / 2, y - size / 2, size / 2, size / 2);
        g2d.setColor(new Color(95, 173, 255));
        g2d.fillOval(x, y - size / 3, size / 2, size / 2);
        g2d.setColor(new Color(255, 122, 168));
        g2d.fillOval(x - 1, y, size / 2, size / 2);
    }

    private class CakeCanvas extends JPanel {

        private final boolean orderPreview;

        private CakeCanvas(boolean orderPreview) {
            this.orderPreview = orderPreview;
            setOpaque(true);
            setBackground(new Color(15, 24, 57));
            setBorder(BorderFactory.createLineBorder(new Color(104, 220, 255), 2, true));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2d = (Graphics2D) graphics.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float phase = neonPhase + (orderPreview ? 0.9f : 0f);
            float pulse = 0.5f + 0.5f * (float) Math.sin(phase);
            Color top = blend(new Color(16, 30, 70), new Color(30, 63, 114), pulse * 0.55f);
            Color bottom = blend(new Color(8, 20, 48), new Color(20, 38, 90), pulse * 0.45f);
            GradientPaint innerGradient = new GradientPaint(0, 0, top, 0, getHeight(), bottom);
            g2d.setPaint(innerGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(new Color(94, 206, 255, orderPreview ? 34 : 42));
            for (int y = 0; y < getHeight(); y += 5) {
                g2d.drawLine(0, y, getWidth(), y);
            }

            Cake cake = orderPreview ? gameLogic.getCurrentOrder() : gameLogic.getPlayerCake();
            drawCake(g2d, cake, getWidth(), getHeight(), orderPreview);

            if (!orderPreview && celebrationActive) {
                drawConfetti(g2d);
            }

            g2d.setColor(new Color(159, 236, 255, 86));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);

            g2d.dispose();
        }
    }

    private void drawConfetti(Graphics2D g2d) {
        for (ConfettiPiece piece : confettiPieces) {
            g2d.setColor(piece.color);
            g2d.fillRoundRect((int) piece.x, (int) piece.y, piece.size, piece.size, 3, 3);
            g2d.setColor(new Color(255, 255, 255, 140));
            g2d.drawRoundRect((int) piece.x, (int) piece.y, piece.size, piece.size, 3, 3);
        }
    }

    private static Color blend(Color from, Color to, float ratio) {
        float clamped = Math.max(0f, Math.min(1f, ratio));
        int red = Math.round(from.getRed() + ((to.getRed() - from.getRed()) * clamped));
        int green = Math.round(from.getGreen() + ((to.getGreen() - from.getGreen()) * clamped));
        int blue = Math.round(from.getBlue() + ((to.getBlue() - from.getBlue()) * clamped));
        return new Color(red, green, blue);
    }

    private static class ArcadeStar {
        private float x;
        private float y;
        private float speed;
        private float phase;
        private int size;
        private int alpha;
    }

    private static class ConfettiPiece {
        private double x;
        private double y;
        private double speed;
        private double drift;
        private int size;
        private Color color;
    }
}
