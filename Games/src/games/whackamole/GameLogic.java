package games.whackamole;

import java.util.Random;

public class GameLogic {

    public static final int GRID_SIZE = 9;
    public static final int GAME_DURATION_SECONDS = 30;

    private final Random random;

    private int activeMoleIndex;
    private int score;
    private int timeRemaining;
    private boolean running;

    public GameLogic(Random random) {
        this.random = random;
        this.activeMoleIndex = -1;
        this.score = 0;
        this.timeRemaining = GAME_DURATION_SECONDS;
        this.running = false;
    }

    public void startGame() {
        score = 0;
        timeRemaining = GAME_DURATION_SECONDS;
        running = true;
        activeMoleIndex = random.nextInt(GRID_SIZE);
    }

    public void moveMole() {
        if (!running) {
            return;
        }
        activeMoleIndex = nextIndexExcluding(activeMoleIndex);
    }

    public boolean hitMole(int index) {
        if (!running) {
            return false;
        }

        if (index != activeMoleIndex) {
            return false;
        }

        score++;
        activeMoleIndex = nextIndexExcluding(activeMoleIndex);
        return true;
    }

    public boolean tickSecond() {
        if (!running) {
            return false;
        }

        if (timeRemaining > 0) {
            timeRemaining--;
        }

        if (timeRemaining <= 0) {
            timeRemaining = 0;
            running = false;
            return true;
        }

        return false;
    }

    public void stopGame() {
        running = false;
    }

    public int getActiveMoleIndex() {
        return activeMoleIndex;
    }

    public int getScore() {
        return score;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public boolean isRunning() {
        return running;
    }

    private int nextIndexExcluding(int excludedIndex) {
        int candidate = random.nextInt(GRID_SIZE);
        if (GRID_SIZE <= 1) {
            return candidate;
        }

        while (candidate == excludedIndex) {
            candidate = random.nextInt(GRID_SIZE);
        }
        return candidate;
    }
}
