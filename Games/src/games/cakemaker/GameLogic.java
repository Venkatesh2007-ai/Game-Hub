package games.cakemaker;

public class GameLogic {

    public enum ServeResult {
        SUCCESS,
        MISMATCH
    }

    private static final int ROUND_DURATION_SECONDS = 40;

    private final OrderGenerator orderGenerator;
    private final ScoreManager scoreManager;

    private Cake currentOrder;
    private Cake playerCake;
    private int timeRemaining;
    private int currentRound;

    public GameLogic(OrderGenerator orderGenerator, ScoreManager scoreManager) {
        this.orderGenerator = orderGenerator;
        this.scoreManager = scoreManager;
        startNewRound();
    }

    public void startNewRound() {
        currentOrder = orderGenerator.generateOrder();
        playerCake = new Cake();
        timeRemaining = ROUND_DURATION_SECONDS;
        currentRound++;
    }

    public void selectBase(String base) {
        playerCake.setBase(base);
    }

    public void selectCream(String cream) {
        playerCake.setCream(cream);
    }

    public void addTopping(String topping) {
        playerCake.addTopping(topping);
    }

    public void clearPlayerCake() {
        playerCake.clear();
    }

    public boolean tickSecond() {
        if (timeRemaining > 0) {
            timeRemaining--;
        }
        return timeRemaining <= 0;
    }

    public ServeResult serveCake() {
        if (playerCake.matches(currentOrder)) {
            scoreManager.recordSuccess(currentOrder.getToppings().size());
            return ServeResult.SUCCESS;
        }

        scoreManager.recordFailure();
        return ServeResult.MISMATCH;
    }

    public void markRoundFailed() {
        scoreManager.recordFailure();
    }

    public Cake getCurrentOrder() {
        return currentOrder.copy();
    }

    public Cake getPlayerCake() {
        return playerCake.copy();
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }
}
