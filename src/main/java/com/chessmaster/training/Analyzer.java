package com.chessmaster.training;

import com.chessmaster.engine.Engine;
import com.chessmaster.engine.Search;
import com.chessmaster.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-game analysis. For each move that was played, it compares the move to the
 * engine's preferred move and measures the "centipawn loss" (how much worse the
 * played move is). From that it labels each move and builds an evaluation curve
 * for a graph — the same kind of "game review" big chess sites provide.
 */
public final class Analyzer {

    /** Quality buckets, by how many centipawns the move threw away. */
    public enum Quality {
        BEST("Best", 0),
        GOOD("Good", 20),
        INACCURACY("Inaccuracy", 50),
        MISTAKE("Mistake", 120),
        BLUNDER("Blunder", 250);

        public final String label;
        public final int threshold; // a move losing >= the next bucket's threshold drops a tier
        Quality(String label, int threshold) { this.label = label; this.threshold = threshold; }

        static Quality of(int cpLoss) {
            if (cpLoss >= BLUNDER.threshold) return BLUNDER;
            if (cpLoss >= MISTAKE.threshold) return MISTAKE;
            if (cpLoss >= INACCURACY.threshold) return INACCURACY;
            if (cpLoss >= GOOD.threshold) return GOOD;
            return BEST;
        }
    }

    /** One reviewed move. */
    public record MoveReview(int ply, boolean whiteMoved, String san, Quality quality,
                             int cpLoss, String bestSan) {}

    /** The whole review: per-move notes plus an eval-after-each-ply curve (White's POV). */
    public record Review(List<MoveReview> moves, List<Integer> evalWhiteCp) {}

    public interface Progress { void update(int done, int total); }

    private final Engine engine;
    private final long perMoveMs;

    public Analyzer(Engine engine, long perMoveMs) {
        this.engine = engine;
        this.perMoveMs = perMoveMs;
    }

    public Review analyze(Game game, Progress progress) {
        List<MoveReview> reviews = new ArrayList<>();
        List<Integer> curve = new ArrayList<>();

        Board board = new Board();
        board.setFromFen(game.startFen());

        List<Game.PlayedMove> history = game.history();
        int total = history.size();

        // Initial evaluation (White's perspective).
        curve.add(whiteCp(engine.analyze(board.copy(), Search.Limits.time(perMoveMs)).score, board.sideToMove()));

        for (int i = 0; i < total; i++) {
            int moverColor = board.sideToMove();
            Game.PlayedMove pm = history.get(i);

            // Engine's view of the position BEFORE the move.
            Search.Result best = engine.analyze(board.copy(), Search.Limits.time(perMoveMs));
            int bestScore = best.score; // mover's perspective
            String bestSan = best.bestMove != Move.NONE ? Notation.toSan(board, best.bestMove) : "";

            // Score of the move actually played: evaluate the resulting position
            // from the opponent's view, then negate to get the mover's view.
            Board after = board.copy();
            after.makeMove(pm.move());
            int playedScore = -engine.analyze(after, Search.Limits.time(perMoveMs)).score;

            int cpLoss = Math.max(0, clampMate(bestScore) - clampMate(playedScore));
            Quality q = Quality.of(cpLoss);
            reviews.add(new MoveReview(i, moverColor == Color.WHITE, pm.san(), q, cpLoss, bestSan));

            board.makeMove(pm.move());
            curve.add(whiteCp(playedScore, moverColor)); // playedScore is mover POV
            if (progress != null) progress.update(i + 1, total);
        }
        return new Review(reviews, curve);
    }

    /** Convert a side-to-move score into a White-relative score, clamped for display. */
    private int whiteCp(int scoreSideToMove, int sideToMove) {
        int white = (sideToMove == Color.WHITE) ? scoreSideToMove : -scoreSideToMove;
        return Math.max(-2000, Math.min(2000, white));
    }

    /** Treat mate scores as a large but finite value so centipawn-loss math behaves. */
    private int clampMate(int score) {
        if (score >= Search.MATE_IN_MAX) return 10000;
        if (score <= -Search.MATE_IN_MAX) return -10000;
        return score;
    }
}
