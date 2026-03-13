package games.responsetime;

import java.util.Random;

public class GameLogic {

    public static final int MIN_DELAY_MS = 2000;
    public static final int MAX_DELAY_MS = 5000;

    public enum State {
        IDLE,
        WAITING_FOR_SIGNAL,
        SIGNAL_VISIBLE
    }

    public enum AttemptType {
        IGNORE,
        TOO_EARLY,
        SUCCESS
    }

    public static class AttemptResult {
        private final AttemptType type;
        private final long reactionTimeMillis;

        private AttemptResult(AttemptType type, long reactionTimeMillis) {
            this.type = type;
            this.reactionTimeMillis = reactionTimeMillis;
        }

        public AttemptType getType() {
            return type;
        }

        public long getReactionTimeMillis() {
            return reactionTimeMillis;
        }
    }

    private final Random random;

    private State state;
    private long signalShownAtMillis;
    private long bestReactionMillis;

    public GameLogic(Random random) {
        this.random = random;
        this.state = State.IDLE;
        this.signalShownAtMillis = -1L;
        this.bestReactionMillis = Long.MAX_VALUE;
    }

    public int startRound() {
        state = State.WAITING_FOR_SIGNAL;
        signalShownAtMillis = -1L;
        return MIN_DELAY_MS + random.nextInt((MAX_DELAY_MS - MIN_DELAY_MS) + 1);
    }

    public void showSignal(long nowMillis) {
        if (state != State.WAITING_FOR_SIGNAL) {
            return;
        }
        state = State.SIGNAL_VISIBLE;
        signalShownAtMillis = nowMillis;
    }

    public AttemptResult onUserAction(long nowMillis) {
        if (state == State.IDLE) {
            return new AttemptResult(AttemptType.IGNORE, -1L);
        }

        if (state == State.WAITING_FOR_SIGNAL) {
            state = State.IDLE;
            return new AttemptResult(AttemptType.TOO_EARLY, -1L);
        }

        long reaction = Math.max(0L, nowMillis - signalShownAtMillis);
        state = State.IDLE;

        if (reaction < bestReactionMillis) {
            bestReactionMillis = reaction;
        }

        return new AttemptResult(AttemptType.SUCCESS, reaction);
    }

    public State getState() {
        return state;
    }

    public long getBestReactionMillis() {
        return bestReactionMillis == Long.MAX_VALUE ? -1L : bestReactionMillis;
    }
}
