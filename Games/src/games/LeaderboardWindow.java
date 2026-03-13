package games;

import javax.swing.BorderFactory;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class LeaderboardWindow extends JFrame {

    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DarkNeonLeaderboardPanel rootPanel;
    private final GraphicsDevice graphicsDevice;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JComboBox<String> gameFilterCombo;
    private final JLabel statusLabel;
    private final NeonActionButton refreshButton;
    private final Timer autoRefreshTimer;
    private final Timer uiAnimationTimer;

    private List<ScoreService.LeaderboardEntry> allEntries;
    private float buttonPhase;

    public LeaderboardWindow() {
        setTitle("Leaderboard");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        allEntries = new ArrayList<>();
        buttonPhase = 0f;
        graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        rootPanel = new DarkNeonLeaderboardPanel();
        rootPanel.setLayout(new BorderLayout(14, 14));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setContentPane(rootPanel);

        JPanel titlePanel = new JPanel(new BorderLayout(2, 2));
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel("GAMEHUB LEADERBOARD", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 38));
        titleLabel.setForeground(new Color(105, 255, 240));
        titlePanel.add(titleLabel, BorderLayout.NORTH);

        JLabel subtitleLabel = new JLabel("Live Scores Across Every Arcade Challenge", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Dialog", Font.PLAIN, 18));
        subtitleLabel.setForeground(new Color(255, 124, 201));
        titlePanel.add(subtitleLabel, BorderLayout.CENTER);
        rootPanel.add(titlePanel, BorderLayout.NORTH);

        JPanel centerContent = new JPanel(new BorderLayout(10, 10));
        centerContent.setOpaque(false);
        rootPanel.add(centerContent, BorderLayout.CENTER);

        JPanel boardCard = new JPanel(new BorderLayout(14, 14));
        boardCard.setOpaque(true);
        boardCard.setBackground(new Color(8, 14, 34, 220));
        boardCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 244, 255), 2, true),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        centerContent.add(boardCard, BorderLayout.CENTER);

        JPanel topControls = new JPanel(new GridLayout(1, 2, 12, 0));
        topControls.setOpaque(false);
        topControls.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        gameFilterCombo = new JComboBox<>();
        gameFilterCombo.setFont(new Font("Dialog", Font.BOLD, 15));
        gameFilterCombo.setBackground(new Color(18, 27, 57));
        gameFilterCombo.setForeground(new Color(222, 246, 255));
        gameFilterCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(73, 214, 255), 2, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        gameFilterCombo.addActionListener(e -> renderTableRows());
        topControls.add(gameFilterCombo);

        refreshButton = new NeonActionButton(
            "Refresh Leaderboard",
            new Color(69, 223, 255),
            new Color(93, 146, 255),
            new Color(52, 244, 255),
            0f
        );
        refreshButton.setFont(new Font("Dialog", Font.BOLD, 16));
        refreshButton.setForeground(new Color(242, 250, 255));
        refreshButton.addActionListener(e -> loadLeaderboardData());
        topControls.add(refreshButton);
        boardCard.add(topControls, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
            new Object[]{"Rank", "Player Name", "Game", "Total Score", "Last Updated"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(34);
        table.setFont(new Font("Dialog", Font.PLAIN, 15));
        table.setBackground(new Color(11, 19, 46));
        table.setForeground(new Color(220, 239, 255));
        table.setGridColor(new Color(45, 70, 126));
        table.setSelectionBackground(new Color(92, 224, 255));
        table.setSelectionForeground(new Color(9, 22, 46));
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new LeaderboardRenderer());

        table.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 16));
        table.getTableHeader().setBackground(new Color(17, 43, 86));
        table.getTableHeader().setForeground(new Color(201, 240, 255));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 2, true),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        scrollPane.getViewport().setBackground(new Color(11, 19, 46));
        boardCard.add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Loading leaderboard...", SwingConstants.LEFT);
        statusLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        statusLabel.setForeground(new Color(197, 231, 255));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 4, 0, 4));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        rootPanel.add(statusPanel, BorderLayout.SOUTH);

        autoRefreshTimer = new Timer(7000, e -> loadLeaderboardData());
        autoRefreshTimer.start();

        uiAnimationTimer = new Timer(33, this::animateControls);
        uiAnimationTimer.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                stopTimers();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                stopTimers();
            }
        });

        registerExitShortcut();
        loadLeaderboardData();
        launchFullscreen();
    }

    private void animateControls(ActionEvent event) {
        buttonPhase += 0.06f;
        refreshButton.setAnimationPhase(buttonPhase);
    }

    private void registerExitShortcut() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "exit-leaderboard");
        getRootPane().getActionMap().put("exit-leaderboard", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
    }

    private void launchFullscreen() {
        if (graphicsDevice != null && graphicsDevice.isFullScreenSupported()) {
            setUndecorated(true);
            graphicsDevice.setFullScreenWindow(this);
            setVisible(true);
            validate();
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setVisible(true);
        }
    }

    private void stopTimers() {
        autoRefreshTimer.stop();
        uiAnimationTimer.stop();
        rootPanel.stopAnimation();
    }

    @Override
    public void dispose() {
        stopTimers();
        if (graphicsDevice != null && graphicsDevice.getFullScreenWindow() == this) {
            graphicsDevice.setFullScreenWindow(null);
        }
        super.dispose();
    }

    private void loadLeaderboardData() {
        String previousSelection = gameFilterCombo.getSelectedItem() == null
            ? "All Games"
            : gameFilterCombo.getSelectedItem().toString();

        allEntries = ScoreService.getLeaderboardEntries();
        rebuildFilterOptions(previousSelection);
        renderTableRows();
    }

    private void rebuildFilterOptions(String desiredSelection) {
        List<String> games = new ArrayList<>();
        games.add("All Games");

        Set<String> gameNames = new TreeSet<>(ScoreService.getRegisteredGames());
        allEntries.stream()
            .map(ScoreService.LeaderboardEntry::getGame)
            .forEach(gameNames::add);
        games.addAll(gameNames);

        gameFilterCombo.removeAllItems();
        for (String game : games) {
            gameFilterCombo.addItem(game);
        }

        boolean selectionFound = false;
        for (int i = 0; i < gameFilterCombo.getItemCount(); i++) {
            if (gameFilterCombo.getItemAt(i).equals(desiredSelection)) {
                gameFilterCombo.setSelectedIndex(i);
                selectionFound = true;
                break;
            }
        }

        if (!selectionFound && gameFilterCombo.getItemCount() > 0) {
            gameFilterCombo.setSelectedIndex(0);
        }
    }

    private void renderTableRows() {
        tableModel.setRowCount(0);
        String selectedGame = gameFilterCombo.getSelectedItem() == null
            ? "All Games"
            : gameFilterCombo.getSelectedItem().toString();

        int rank = 1;
        for (ScoreService.LeaderboardEntry entry : allEntries) {
            if (!"All Games".equals(selectedGame) && !selectedGame.equals(entry.getGame())) {
                continue;
            }

            tableModel.addRow(new Object[]{
                rank++,
                entry.getUsername(),
                entry.getGame(),
                entry.getTotalScore(),
                formatTimestamp(entry.getLastPlayed())
            });
        }

        if (tableModel.getRowCount() == 0) {
            statusLabel.setText("No scores available for this filter.");
        } else {
            statusLabel.setText("Showing " + tableModel.getRowCount() + " leaderboard rows. Auto-refresh: ON");
        }
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return timestamp.toLocalDateTime().format(TIME_FORMAT);
    }

    private static Color blend(Color from, Color to, float ratio) {
        float clamped = Math.max(0f, Math.min(1f, ratio));
        int r = Math.round(from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int g = Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int b = Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        return new Color(r, g, b);
    }

    private static class NeonActionButton extends JButton {

        private final Color primaryColor;
        private final Color secondaryColor;
        private final Color glowColor;
        private final float phaseOffset;

        private float animationPhase;
        private boolean hovered;

        private NeonActionButton(
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
            this.animationPhase = phaseOffset;
            this.hovered = false;

            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(240, 46));

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
            int arc = 20;

            float pulse = 0.5f + 0.5f * (float) Math.sin(animationPhase);
            float hoverBoost = hovered ? 0.20f : 0f;

            Color topColor = blend(new Color(10, 16, 37), primaryColor, 0.42f + pulse * 0.25f);
            Color bottomColor = blend(new Color(4, 8, 24), secondaryColor, 0.36f + pulse * 0.2f);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.24f + hoverBoost));
            g2d.setColor(glowColor);
            g2d.fillRoundRect(5, 8, width - 10, height - 11, arc + 2, arc + 2);

            g2d.setComposite(AlphaComposite.SrcOver);
            g2d.setPaint(new GradientPaint(0, 0, topColor, 0, height, bottomColor));
            g2d.fillRoundRect(0, 0, width - 1, height - 1, arc, arc);

            int sheenWidth = Math.max(30, width / 6);
            int travel = width + sheenWidth + 36;
            int sheenX = -sheenWidth - 18 + (int) ((animationPhase % (Math.PI * 2)) / (Math.PI * 2) * travel);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
            g2d.setColor(new Color(255, 255, 255, 190));
            g2d.fillRoundRect(sheenX, 5, sheenWidth, height - 10, 16, 16);
            g2d.setComposite(AlphaComposite.SrcOver);

            g2d.setStroke(new BasicStroke(2.4f));
            g2d.setColor(blend(glowColor, Color.WHITE, 0.34f + pulse * 0.3f + hoverBoost));
            g2d.drawRoundRect(2, 2, width - 5, height - 5, arc, arc);

            super.paintComponent(g2d);
            g2d.dispose();
        }
    }

    private static class DarkNeonLeaderboardPanel extends JPanel {

        private static final int STAR_COUNT = 150;

        private final Random random;
        private final Star[] stars;
        private final Timer animationTimer;

        private float gridShift;
        private float glowPhase;
        private boolean initialized;

        private DarkNeonLeaderboardPanel() {
            random = new Random();
            stars = new Star[STAR_COUNT];
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

            initialized = true;
        }

        private void advanceAnimation(ActionEvent event) {
            int width = Math.max(1, getWidth());
            int height = Math.max(1, getHeight());
            initializeIfNeeded(width, height);

            for (Star star : stars) {
                star.y += star.speed;
                star.x += Math.sin((glowPhase + star.phase) * 0.7f) * 0.18f;
                if (star.y > height + 15 || star.x < -20 || star.x > width + 20) {
                    resetStar(star, width, height, false);
                }
            }

            gridShift = (gridShift + 2.0f) % 86f;
            glowPhase += 0.048f;
            repaint();
        }

        private void resetStar(Star star, int width, int height, boolean randomY) {
            star.x = random.nextFloat() * width;
            star.y = randomY ? random.nextFloat() * height : -random.nextInt(140) - 10;
            star.size = 1 + random.nextInt(3);
            star.speed = 0.45f + random.nextFloat() * 1.65f;
            star.alpha = 70 + random.nextInt(130);
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
            int mainRadius = 220 + (int) (Math.sin(glowPhase) * 22);
            int mainX = (width / 2) - mainRadius;
            int mainY = (height / 2) - mainRadius - 34;

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.21f));
            g2d.setColor(new Color(255, 52, 162));
            g2d.fillOval(mainX, mainY, mainRadius * 2, mainRadius * 2);

            int secondRadius = 168 + (int) (Math.cos(glowPhase * 1.4f) * 20);
            g2d.setColor(new Color(45, 233, 255));
            g2d.fillOval((width / 2) - secondRadius, (height / 2) - secondRadius - 18, secondRadius * 2, secondRadius * 2);

            int cornerRadius = 100 + (int) (Math.cos(glowPhase * 1.7f) * 12);
            g2d.setColor(new Color(150, 90, 255));
            g2d.fillOval(width - cornerRadius - 70, 54, cornerRadius, cornerRadius);
            g2d.fillOval(70 - cornerRadius / 2, height - 130, cornerRadius, cornerRadius);

            g2d.setComposite(AlphaComposite.SrcOver);
        }

        private void drawNeonGrid(Graphics2D g2d, int width, int height) {
            int horizonY = Math.max(height / 3, 180);
            g2d.setStroke(new BasicStroke(1.3f));

            for (int i = 0; i <= 10; i++) {
                float progress = i / 10f;
                int y = horizonY + (int) (Math.pow(progress, 1.75) * (height - horizonY));
                int alpha = 44 + (int) (progress * 102);
                int wave = (int) Math.round(Math.sin((gridShift + (i * 7.3f)) * 0.11) * 10);
                g2d.setColor(new Color(80, 244, 255, alpha));
                g2d.drawLine(wave, y, width + wave, y);
            }

            for (int x = -width; x < width * 2; x += 82) {
                int bottomX = x + Math.round(gridShift);
                int topX = (width / 2) + (int) ((bottomX - (width / 2.0)) * 0.15);
                g2d.setColor(new Color(255, 96, 200, 108));
                g2d.drawLine(bottomX, height, topX, horizonY);
            }
        }

        private void drawStars(Graphics2D g2d) {
            for (Star star : stars) {
                g2d.setColor(new Color(188, 228, 255, star.alpha));
                g2d.fillOval(Math.round(star.x), Math.round(star.y), star.size, star.size);
            }
        }

        private void drawScanlines(Graphics2D g2d, int width, int height) {
            g2d.setColor(new Color(4, 6, 19, 60));
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
    }

    private static class LeaderboardRenderer extends DefaultTableCellRenderer {
        private final Font baseFont = new Font("Dialog", Font.PLAIN, 15);
        private final Font rankFont = new Font("Dialog", Font.BOLD, 15);

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        ) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);

            if (isSelected) {
                setBackground(new Color(92, 224, 255));
                setForeground(new Color(9, 22, 46));
            } else {
                setBackground(row % 2 == 0 ? new Color(12, 21, 50) : new Color(10, 18, 43));
                setForeground(new Color(218, 236, 255));
            }

            setFont(baseFont);
            if (!isSelected && column == 0 && value instanceof Integer) {
                int rank = (Integer) value;
                setFont(rankFont);
                if (rank == 1) {
                    setForeground(new Color(255, 198, 78));
                } else if (rank == 2) {
                    setForeground(new Color(189, 208, 236));
                } else if (rank == 3) {
                    setForeground(new Color(218, 150, 104));
                }
            }

            return this;
        }
    }
}
