import javax.swing.SwingUtilities;

public class GameLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameLoginWindow::new);
    }
}   