package games.flappybird;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;

public class GameFrame extends JFrame {

    private final GraphicsDevice graphicsDevice;
    private final GamePanel gamePanel;

    public GameFrame() {
        setTitle("Flappy Pig");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        gamePanel = new GamePanel();
        setContentPane(gamePanel);

        registerExitShortcut();
        launchFullscreen();
        SwingUtilities.invokeLater(gamePanel::requestFocusInWindow);
    }

    private void registerExitShortcut() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "exit-fullscreen");
        getRootPane().getActionMap().put("exit-fullscreen", new AbstractAction() {
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
            validate();
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setVisible(true);
        }
    }

    @Override
    public void dispose() {
        if (graphicsDevice != null && graphicsDevice.getFullScreenWindow() == this) {
            graphicsDevice.setFullScreenWindow(null);
        }
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameFrame::new);
    }
}
