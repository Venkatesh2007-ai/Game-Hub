package games;

import javax.swing.JPasswordField;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScoreService {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/gamehub";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    private static final String[] REGISTERED_GAMES = {
        "Maze Escape",
        "Cake Maker",
        "Whack-A-Mole",
        "Response Time Tester",
        "Flappy Pig",
        "SyntaxSnake"
    };

    private static final Set<String> HIDDEN_LEADERBOARD_GAMES = Set.of(
        "flappypig",
        "flappybird",
        "quizgame",
        "tictactoe",
        "memorygame",
        "rockpaperscissor",
        "rockpaperscissors"
    );

    private static final List<ScoreRecord> MEMORY_SCORES = new ArrayList<>();
    private static final List<String> MEMORY_GAMES = new ArrayList<>();
    private static final Map<String, String> MEMORY_USERS = new HashMap<>();

    private static String username;
    private static boolean databaseAvailable;

    static {
        init();
    }

    private static void init() {
        seedMemoryGames();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            databaseAvailable = false;
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS scores (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "username VARCHAR(100) NOT NULL, " +
                    "game VARCHAR(100) NOT NULL, " +
                    "score INT NOT NULL, " +
                    "totalscore INT NOT NULL DEFAULT 0, " +
                    "played_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")"
                );
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS games (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(100) NOT NULL UNIQUE" +
                    ")"
                );
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "username VARCHAR(100) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL" +
                    ")"
                );
            }

            ensureScoresTableColumns(conn);

            try (PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO games (name) VALUES (?)")) {
                for (String game : REGISTERED_GAMES) {
                    ps.setString(1, game);
                    ps.executeUpdate();
                }
            }

            databaseAvailable = true;
        } catch (SQLException e) {
            databaseAvailable = false;
        }
    }

    public static String ensureUsername(Component parent) {
        if (username != null && !username.isBlank()) {
            return username;
        }

        JTextField usernameField = new JTextField(16);
        JPasswordField passwordField = new JPasswordField(16);

        JPanel loginPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        loginPanel.add(new javax.swing.JLabel("Username"));
        loginPanel.add(usernameField);
        loginPanel.add(new javax.swing.JLabel("Password"));
        loginPanel.add(passwordField);

        Object[] options = {"Login", "Sign Up", "Cancel"};
        int option = JOptionPane.showOptionDialog(
            parent,
            loginPanel,
            "Player Login",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE
            ,
            null,
            options,
            options[0]
        );

        if (option == 0) {
            if (!login(usernameField.getText(), new String(passwordField.getPassword()))) {
                JOptionPane.showMessageDialog(parent, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return getUsername();
        }

        if (option == 1) {
            if (!signup(usernameField.getText(), new String(passwordField.getPassword()))) {
                JOptionPane.showMessageDialog(parent, "Username already exists.", "Sign Up Failed", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return getUsername();
        }

        if (option != 0 && option != 1) {
            return null;
        }
        return null;
    }

    public static synchronized boolean login(String usernameInput, String passwordInput) {
        String sanitizedUsername = sanitize(usernameInput);
        String sanitizedPassword = sanitize(passwordInput);

        if (sanitizedUsername.isBlank() || sanitizedPassword.isBlank()) {
            return false;
        }

        if (databaseAvailable) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                try (PreparedStatement findUser = conn.prepareStatement(
                    "SELECT password FROM users WHERE username = ?")) {
                    findUser.setString(1, sanitizedUsername);
                    try (ResultSet rs = findUser.executeQuery()) {
                        if (!rs.next()) {
                            return false;
                        }
                        String storedPassword = rs.getString("password");
                        if (!sanitizedPassword.equals(storedPassword)) {
                            return false;
                        }
                    }
                }

                username = sanitizedUsername;
                return true;
            } catch (SQLException e) {
                databaseAvailable = false;
            }
        }

        String existingPassword = MEMORY_USERS.get(sanitizedUsername);
        if (existingPassword == null) {
            return false;
        }
        if (!existingPassword.equals(sanitizedPassword)) {
            return false;
        }
        username = sanitizedUsername;
        return true;
    }

    public static synchronized boolean signup(String usernameInput, String passwordInput) {
        String sanitizedUsername = sanitize(usernameInput);
        String sanitizedPassword = sanitize(passwordInput);

        if (sanitizedUsername.isBlank() || sanitizedPassword.isBlank()) {
            return false;
        }

        if (databaseAvailable) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                try (PreparedStatement checkUser = conn.prepareStatement(
                    "SELECT 1 FROM users WHERE username = ?")) {
                    checkUser.setString(1, sanitizedUsername);
                    try (ResultSet rs = checkUser.executeQuery()) {
                        if (rs.next()) {
                            return false;
                        }
                    }
                }

                try (PreparedStatement createUser = conn.prepareStatement(
                    "INSERT INTO users (username, password) VALUES (?, ?)")) {
                    createUser.setString(1, sanitizedUsername);
                    createUser.setString(2, sanitizedPassword);
                    createUser.executeUpdate();
                }

                username = sanitizedUsername;
                return true;
            } catch (SQLException e) {
                databaseAvailable = false;
            }
        }

        if (MEMORY_USERS.containsKey(sanitizedUsername)) {
            return false;
        }

        MEMORY_USERS.put(sanitizedUsername, sanitizedPassword);
        username = sanitizedUsername;
        return true;
    }

    public static void setUsername(String name) {
        if (name == null) {
            return;
        }
        username = name.trim();
    }

    public static String getUsername() {
        return username;
    }

    public static void recordScore(String game, int score) {
        if (score < 0) {
            return;
        }

        String user = ensureUsername(null);
        if (user == null) {
            return;
        }

        if (databaseAvailable) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO scores (username, game, score, totalscore) VALUES (?, ?, ?, ?)")) {
                int totalScore = getDatabaseCurrentTotal(conn, user) + score;
                ps.setString(1, user);
                ps.setString(2, game);
                ps.setInt(3, score);
                ps.setInt(4, totalScore);
                ps.executeUpdate();
                return;
            } catch (SQLException e) {
                databaseAvailable = false;
            }
        }

        recordMemoryScore(user, game, score);
    }

    public static int getTotalScore(String user) {
        if (user == null || user.isBlank()) {
            return 0;
        }

        if (databaseAvailable) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT COALESCE(SUM(score), 0) FROM scores WHERE username = ?")) {
                ps.setString(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                databaseAvailable = false;
            }
        }

        return getMemoryTotalScore(user);
    }

    public static List<LeaderboardEntry> getLeaderboardEntries() {
        if (databaseAvailable) {
            List<LeaderboardEntry> entries = new ArrayList<>();
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT username, game, COALESCE(SUM(score), 0) AS total_score, MAX(played_at) AS last_played " +
                     "FROM scores " +
                     "GROUP BY username, game " +
                     "ORDER BY total_score DESC, last_played DESC");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String game = rs.getString("game");
                    if (shouldHideFromLeaderboard(game)) {
                        continue;
                    }
                    entries.add(new LeaderboardEntry(
                        rs.getString("username"),
                        game,
                        rs.getInt("total_score"),
                        rs.getTimestamp("last_played")
                    ));
                }
                return entries;
            } catch (SQLException e) {
                databaseAvailable = false;
            }
        }

        return getMemoryLeaderboardEntries();
    }

    public static List<String> getRegisteredGames() {
        if (databaseAvailable) {
            List<String> games = new ArrayList<>();
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT name FROM games ORDER BY name");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String game = rs.getString("name");
                    if (shouldHideFromLeaderboard(game)) {
                        continue;
                    }
                    games.add(game);
                }
                return games;
            } catch (SQLException e) {
                databaseAvailable = false;
            }
        }

        return getMemoryRegisteredGames();
    }

    private static synchronized void seedMemoryGames() {
        for (String game : REGISTERED_GAMES) {
            addMemoryGameIfAbsent(game);
        }
    }

    private static synchronized void recordMemoryScore(String user, String game, int score) {
        addMemoryGameIfAbsent(game);
        MEMORY_SCORES.add(new ScoreRecord(user, game, score, new Timestamp(System.currentTimeMillis())));
    }

    private static synchronized int getMemoryTotalScore(String user) {
        int total = 0;
        for (ScoreRecord record : MEMORY_SCORES) {
            if (user.equals(record.username)) {
                total += record.score;
            }
        }
        return total;
    }

    private static synchronized List<LeaderboardEntry> getMemoryLeaderboardEntries() {
        Map<String, LeaderboardAccumulator> grouped = new HashMap<>();

        for (ScoreRecord record : MEMORY_SCORES) {
            if (shouldHideFromLeaderboard(record.game)) {
                continue;
            }

            String key = record.username + "\u0000" + record.game;
            LeaderboardAccumulator accumulator = grouped.computeIfAbsent(
                key, ignored -> new LeaderboardAccumulator(record.username, record.game)
            );

            accumulator.totalScore += record.score;
            if (accumulator.lastPlayed == null || record.playedAt.after(accumulator.lastPlayed)) {
                accumulator.lastPlayed = record.playedAt;
            }
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (LeaderboardAccumulator accumulator : grouped.values()) {
            entries.add(new LeaderboardEntry(
                accumulator.username,
                accumulator.game,
                accumulator.totalScore,
                accumulator.lastPlayed
            ));
        }

        entries.sort(
            Comparator.comparingInt(LeaderboardEntry::getTotalScore).reversed()
                .thenComparing(
                    entry -> entry.getLastPlayed() == null ? new Timestamp(0L) : entry.getLastPlayed(),
                    Comparator.reverseOrder()
                )
        );

        return entries;
    }

    private static synchronized List<String> getMemoryRegisteredGames() {
        List<String> games = new ArrayList<>();
        for (String game : MEMORY_GAMES) {
            if (!shouldHideFromLeaderboard(game)) {
                games.add(game);
            }
        }
        games.sort(String::compareToIgnoreCase);
        return games;
    }

    private static void addMemoryGameIfAbsent(String game) {
        if (game == null || game.isBlank()) {
            return;
        }

        for (String existing : MEMORY_GAMES) {
            if (existing.equalsIgnoreCase(game)) {
                return;
            }
        }

        MEMORY_GAMES.add(game.trim());
    }

    private static boolean shouldHideFromLeaderboard(String gameName) {
        if (gameName == null || gameName.isBlank()) {
            return false;
        }
        String normalized = gameName.toLowerCase().replaceAll("[^a-z0-9]", "");
        return HIDDEN_LEADERBOARD_GAMES.contains(normalized);
    }

    private static void ensureScoresTableColumns(Connection conn) throws SQLException {
        if (!scoreColumnExists(conn, "totalscore")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE scores ADD COLUMN totalscore INT NOT NULL DEFAULT 0");
            }
        }
    }

    private static boolean scoreColumnExists(Connection conn, String columnName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scores' AND COLUMN_NAME = ?")) {
            ps.setString(1, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int getDatabaseCurrentTotal(Connection conn, String user) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT COALESCE(SUM(score), 0) FROM scores WHERE username = ?")) {
            ps.setString(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private static class ScoreRecord {
        private final String username;
        private final String game;
        private final int score;
        private final Timestamp playedAt;

        private ScoreRecord(String username, String game, int score, Timestamp playedAt) {
            this.username = username;
            this.game = game;
            this.score = score;
            this.playedAt = playedAt;
        }
    }

    private static class LeaderboardAccumulator {
        private final String username;
        private final String game;
        private int totalScore;
        private Timestamp lastPlayed;

        private LeaderboardAccumulator(String username, String game) {
            this.username = username;
            this.game = game;
        }
    }

    public static class LeaderboardEntry {
        private final String username;
        private final String game;
        private final int totalScore;
        private final Timestamp lastPlayed;

        public LeaderboardEntry(String username, String game, int totalScore, Timestamp lastPlayed) {
            this.username = username;
            this.game = game;
            this.totalScore = totalScore;
            this.lastPlayed = lastPlayed;
        }

        public String getUsername() {
            return username;
        }

        public String getGame() {
            return game;
        }

        public int getTotalScore() {
            return totalScore;
        }

        public Timestamp getLastPlayed() {
            return lastPlayed;
        }
    }
}
