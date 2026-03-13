package games.cakemaker;

public class ScoreManager {

    private int score;
    private int roundCount;
    private int successfulRounds;

    public void recordSuccess(int toppingCount) {
        roundCount++;
        successfulRounds++;
        score += 10 + (toppingCount * 2);
    }

    public void recordFailure() {
        roundCount++;
    }

    public int getScore() {
        return score;
    }

    public int getRoundCount() {
        return roundCount;
    }

    public int getSuccessfulRounds() {
        return successfulRounds;
    }

    public int getFailedRounds() {
        return Math.max(0, roundCount - successfulRounds);
    }
}
