package games.responsetime;

import games.ScoreService;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

public class GamePanel extends JPanel {

    private static final String GAME_NAME = "Response Time Tester";
    private static final int MAX_LEADERBOARD_SCORE = 1000;

    private final GameLogic gameLogic;
    private final SignalPanel signalPanel;
    private final JLabel reactionLabel;
    private final JLabel bestLabel;
    private final JLabel statusLabel;
    private final JButton startButton;

    private Timer signalDelayTimer;
    private Timer restartTimer;

    public GamePanel() {
        gameLogic = new GameLogic(new Random());

        setLayout(new BorderLayout(14, 14));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(new Color(27, 36, 62));
        setFocusable(true);

        reactionLabel = createHeaderLabel("Reaction: -- ms");
        bestLabel = createHeaderLabel("Best: -- ms");
        statusLabel = new JLabel("Press Start Game to begin.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 16));
        statusLabel.setForeground(new Color(229, 237, 254));

        startButton = new JButton("Start Game");
        startButton.setFont(new Font("Dialog", Font.BOLD, 18));
        startButton.setFocusPainted(false);
        startButton.setBackground(new Color(99, 232, 158));
        startButton.setForeground(new Color(18, 52, 34));
        startButton.addActionListener(e -> startRound());

        signalPanel = new SignalPanel();
        signalPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleUserAttempt();
            }
        });

        add(createTopPanel(), BorderLayout.NORTH);
        add(signalPanel, BorderLayout.CENTER);
        registerInputHandlers();
    }

    private JPanel createTopPanel() {
        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);

        JLabel titleLabel = new JLabel("Response Time Tester", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 32));
        titleLabel.setForeground(new Color(252, 227, 133));
        top.add(titleLabel, BorderLayout.NORTH);

        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.CENTER, 22, 0));
        metrics.setOpaque(false);
        metrics.add(reactionLabel);
        metrics.add(bestLabel);
        metrics.add(startButton);
        top.add(metrics, BorderLayout.CENTER);

        top.add(statusLabel, BorderLayout.SOUTH);
        return top;
    }

    private JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Dialog", Font.BOLD, 20));
        label.setForeground(new Color(244, 251, 255));
        return label;
    }

    private void registerInputHandlers() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "reaction-space");
        getActionMap().put("reaction-space", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                handleUserAttempt();
            }
        });
    }

    private void startRound() {
        stopTimer(signalDelayTimer);
        stopTimer(restartTimer);

        int delayMs = gameLogic.startRound();
        signalPanel.setMode(SignalPanel.Mode.WAITING);
        statusLabel.setText("Wait for green...");
        startButton.setEnabled(false);
        startButton.setText("Waiting...");
        requestFocusInWindow();

        signalDelayTimer = new Timer(delayMs, e -> showSignal());
        signalDelayTimer.setRepeats(false);
        signalDelayTimer.start();
    }

    private void showSignal() {
        gameLogic.showSignal(System.currentTimeMillis());
        signalPanel.setMode(SignalPanel.Mode.SIGNAL);
        statusLabel.setText("CLICK NOW or press SPACE!");
    }

    private void handleUserAttempt() {
        GameLogic.AttemptResult result = gameLogic.onUserAction(System.currentTimeMillis());
        if (result.getType() == GameLogic.AttemptType.IGNORE) {
            return;
        }

        if (result.getType() == GameLogic.AttemptType.TOO_EARLY) {
            stopTimer(signalDelayTimer);
            signalPanel.setMode(SignalPanel.Mode.TOO_EARLY);
            statusLabel.setText("Too Early! Wait for the signal.");
            startButton.setEnabled(false);
            startButton.setText("Restarting...");

            restartTimer = new Timer(900, e -> startRound());
            restartTimer.setRepeats(false);
            restartTimer.start();
            return;
        }

        long reaction = result.getReactionTimeMillis();
        long best = gameLogic.getBestReactionMillis();
        reactionLabel.setText("Reaction: " + reaction + " ms");
        bestLabel.setText("Best: " + best + " ms");

        try {
            ScoreService.recordScore(GAME_NAME, toLeaderboardScore(reaction));
        } catch (RuntimeException ignored) {
            // Keep the round responsive even if score persistence fails.
        }

        signalPanel.setMode(SignalPanel.Mode.RESULT);
        statusLabel.setText("Great! " + reaction + " ms (Best: " + best + " ms). Press Start Game to try again.");
        startButton.setEnabled(true);
        startButton.setText("Start Game");
    }

    private int toLeaderboardScore(long reactionMillis) {
        // Faster reactions should rank higher on the existing descending leaderboard.
        return (int) Math.max(0L, MAX_LEADERBOARD_SCORE - reactionMillis);
    }

    private void stopTimer(Timer timer) {
        if (timer != null) {
            timer.stop();
        }
    }

    @Override
    public void removeNotify() {
        stopTimer(signalDelayTimer);
        stopTimer(restartTimer);
        super.removeNotify();
    }

    private static class SignalPanel extends JPanel {

        private enum Mode {
            IDLE,
            WAITING,
            SIGNAL,
            TOO_EARLY,
            RESULT
        }

        private Mode mode;

        private SignalPanel() {
            mode = Mode.IDLE;
            setPreferredSize(new Dimension(640, 360));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }

        private void setMode(Mode mode) {
            this.mode = mode;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2d = (Graphics2D) graphics.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            Color top = colorForMode(mode);
            Color bottom = blend(top, Color.BLACK, 0.22f);
            g2d.setPaint(new GradientPaint(0, 0, top, 0, height, bottom));
            g2d.fillRoundRect(0, 0, width - 1, height - 1, 28, 28);

            g2d.setStroke(new BasicStroke(3f));
            g2d.setColor(new Color(255, 255, 255, 180));
            g2d.drawRoundRect(1, 1, width - 3, height - 3, 28, 28);

            g2d.setColor(new Color(255, 255, 255));
            g2d.setFont(new Font("Dialog", Font.BOLD, 42));
            String text = textForMode(mode);
            int textWidth = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, (width - textWidth) / 2, height / 2);

            g2d.setFont(new Font("Dialog", Font.BOLD, 24));
            String hint = "SPACE or Mouse Click";
            int hintWidth = g2d.getFontMetrics().stringWidth(hint);
            g2d.drawString(hint, (width - hintWidth) / 2, (height / 2) + 46);

            g2d.dispose();
        }

        private static Color colorForMode(Mode mode) {
            if (mode == Mode.WAITING) {
                return new Color(224, 63, 73);
            }
            if (mode == Mode.SIGNAL) {
                return new Color(66, 191, 109);
            }
            if (mode == Mode.TOO_EARLY) {
                return new Color(255, 153, 66);
            }
            if (mode == Mode.RESULT) {
                return new Color(75, 146, 240);
            }
            return new Color(122, 99, 237);
        }

        private static String textForMode(Mode mode) {
            if (mode == Mode.WAITING) {
                return "WAIT FOR GREEN...";
            }
            if (mode == Mode.SIGNAL) {
                return "CLICK NOW!";
            }
            if (mode == Mode.TOO_EARLY) {
                return "TOO EARLY!";
            }
            if (mode == Mode.RESULT) {
                return "GOOD REACTION";
            }
            return "PRESS START";
        }

        private static Color blend(Color from, Color to, float ratio) {
            float clamped = Math.max(0f, Math.min(1f, ratio));
            int red = Math.round(from.getRed() + ((to.getRed() - from.getRed()) * clamped));
            int green = Math.round(from.getGreen() + ((to.getGreen() - from.getGreen()) * clamped));
            int blue = Math.round(from.getBlue() + ((to.getBlue() - from.getBlue()) * clamped));
            return new Color(red, green, blue);
        }
    }
}
