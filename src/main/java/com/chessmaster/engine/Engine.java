package com.chessmaster.engine;

import com.chessmaster.model.Board;
import com.chessmaster.model.Move;
import com.chessmaster.model.MoveGenerator;
import com.chessmaster.model.MoveList;

import java.util.Random;

/**
 * High-level engine the application talks to. It wraps the {@link Search} with:
 * <ul>
 *   <li><b>Difficulty levels</b> — each level sets a search depth/time budget and,
 *       for the weaker levels, a chance to play a deliberately sub-optimal move so
 *       a human can win.</li>
 *   <li>An {@link OpeningBook} for fast, varied, theory-sound openings.</li>
 * </ul>
 * The same {@link #analyze} method powers both the opponent and the hint/eval-bar
 * features, so the analysis the user sees is exactly what the engine plays on.
 */
public final class Engine {

    /** Selectable strength. Time is per-move; randomness is the chance of a weaker move. */
    public enum Level {
        BEGINNER    ("Beginner",     2,   150,  0.55),
        CASUAL      ("Casual",       4,   300,  0.30),
        INTERMEDIATE("Intermediate", 6,   600,  0.12),
        ADVANCED    ("Advanced",     10,  1200, 0.03),
        EXPERT      ("Expert",       16,  2500, 0.0),
        MASTER      ("Master",       64,  5000, 0.0);

        public final String displayName;
        public final int maxDepth;
        public final long timeMs;
        public final double randomness;

        Level(String displayName, int maxDepth, long timeMs, double randomness) {
            this.displayName = displayName;
            this.maxDepth = maxDepth;
            this.timeMs = timeMs;
            this.randomness = randomness;
        }

        public Search.Limits limits() {
            return new Search.Limits(maxDepth, timeMs);
        }

        @Override public String toString() { return displayName; }
    }

    private final Search search = new Search(128);
    private final OpeningBook book = new OpeningBook();
    private final Random random = new Random();
    private boolean useBook = true;

    public void newGame() {
        search.newGame();
    }

    public void setUseBook(boolean useBook) {
        this.useBook = useBook;
    }

    /** Stop the current search early (e.g. the user pressed "move now" or undid). */
    public void stop() {
        search.stop();
    }

    /**
     * Choose the move to play at the given level. Consults the opening book first,
     * then searches. Weaker levels sometimes substitute a near-random legal move.
     */
    public int chooseMove(Board board, Level level) {
        if (useBook) {
            int bookMove = book.probe(board);
            if (bookMove != Move.NONE) return bookMove;
        }
        if (level.randomness > 0 && random.nextDouble() < level.randomness) {
            int weak = weakerMove(board, level);
            if (weak != Move.NONE) return weak;
        }
        return search.search(board, level.limits()).bestMove;
    }

    /**
     * Full-strength analysis of a position — best move, score, principal
     * variation. Used by the hint, blunder-check, and eval-bar features.
     */
    public Search.Result analyze(Board board, Search.Limits limits) {
        return search.search(board, limits);
    }

    public Search.Result analyze(Board board, int depth) {
        return search.search(board, Search.Limits.depth(depth));
    }

    public OpeningBook book() {
        return book;
    }

    /**
     * For weak levels: do a shallow search and pick randomly among the legal
     * moves, but never throw away a free piece blunder-free play would avoid by
     * still preferring the very shallow search result half the time. Simple and
     * "human-like enough" for casual play.
     */
    private int weakerMove(Board board, Level level) {
        MoveList legal = MoveGenerator.generateLegal(board);
        if (legal.isEmpty()) return Move.NONE;
        // Bias toward a 1-ply best move so it still grabs hanging pieces sometimes.
        if (random.nextBoolean()) {
            return search.search(board, Search.Limits.depth(1)).bestMove;
        }
        return legal.get(random.nextInt(legal.size()));
    }
}
