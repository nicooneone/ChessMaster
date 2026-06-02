package com.chessmaster.engine;

import com.chessmaster.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The search: given a position, find the best move and a score for it.
 *
 * <p>Techniques used (each is a standard, well-documented chess-engine idea):
 * <ul>
 *   <li><b>Negamax + alpha-beta</b> — the core minimax variant; alpha-beta prunes
 *       branches that cannot affect the result.</li>
 *   <li><b>Iterative deepening</b> — search depth 1, then 2, then 3... until time
 *       runs out. Sounds wasteful but the shallow searches fill the transposition
 *       table and order moves, making the deep search far faster.</li>
 *   <li><b>Transposition table</b> — cache results keyed by Zobrist hash.</li>
 *   <li><b>Move ordering</b> — try the best-looking moves first (hash move,
 *       captures by MVV-LVA, killer moves, history heuristic) so alpha-beta cuts
 *       off sooner.</li>
 *   <li><b>Quiescence search</b> — at the leaves, keep searching captures so the
 *       engine doesn't stop counting material in the middle of a trade.</li>
 *   <li><b>Null-move pruning</b> — if passing the turn is still good enough to
 *       beat beta, prune; cheap and strong (disabled in check / thin endgames).</li>
 * </ul>
 */
public final class Search {

    public static final int INFINITY = 1_000_000;
    public static final int MATE = 30_000;
    public static final int MATE_IN_MAX = MATE - 1000; // scores beyond this are forced mates
    private static final int MAX_PLY = 128;

    /** What the caller asks for: a depth cap and/or a wall-clock budget. */
    public record Limits(int maxDepth, long maxTimeMs) {
        public static Limits depth(int d)      { return new Limits(d, Long.MAX_VALUE); }
        public static Limits time(long ms)     { return new Limits(MAX_PLY - 1, ms); }
    }

    /** What the search returns. */
    public static final class Result {
        public int bestMove;
        public int score;          // centipawns, from the searched side's perspective
        public int depth;          // deepest fully-completed iteration
        public long nodes;
        public long timeMs;
        public List<Integer> pv = new ArrayList<>(); // principal variation (best line)

        public boolean isMate() { return Math.abs(score) >= MATE_IN_MAX; }

        /** Moves-to-mate (positive = we mate, negative = we get mated), or 0 if none. */
        public int mateIn() {
            if (!isMate()) return 0;
            int plies = MATE - Math.abs(score);
            int moves = (plies + 1) / 2;
            return score > 0 ? moves : -moves;
        }
    }

    private final TranspositionTable tt;
    private final int[][] killers = new int[MAX_PLY][2];
    private final int[][] history = new int[16][64]; // [piece][toSquare]
    private final TranspositionTable.Entry probe = new TranspositionTable.Entry();

    private long startTime;
    private long timeLimitMs;
    private long nodes;
    private volatile boolean stopped;

    private int rootBestMove;
    private int rootBestScore;

    public Search() {
        this(64);
    }

    public Search(int ttSizeMb) {
        this.tt = new TranspositionTable(ttSizeMb);
    }

    /** Ask the search to stop as soon as possible (called from another thread). */
    public void stop() {
        stopped = true;
    }

    public void newGame() {
        tt.clear();
        for (int[] k : killers) { k[0] = 0; k[1] = 0; }
        for (int[] h : history) java.util.Arrays.fill(h, 0);
    }

    public Result search(Board board, Limits limits) {
        this.startTime = System.currentTimeMillis();
        this.timeLimitMs = limits.maxTimeMs();
        this.nodes = 0;
        this.stopped = false;

        // Decay (don't fully clear) history between searches so move ordering carries over.
        for (int[] h : history) for (int i = 0; i < 64; i++) h[i] /= 2;

        Result result = new Result();
        MoveList rootMoves = MoveGenerator.generateLegal(board);
        if (rootMoves.isEmpty()) return result;          // no move (mate/stalemate)
        result.bestMove = rootMoves.get(0);              // always have a legal fallback
        rootBestMove = rootMoves.get(0);

        // Iterative deepening.
        for (int depth = 1; depth <= limits.maxDepth() && depth < MAX_PLY; depth++) {
            rootBestScore = -INFINITY;
            int score = searchRoot(board, depth);

            if (stopped && depth > 1) break; // discard an interrupted, incomplete depth

            result.bestMove = rootBestMove;
            result.score = score;
            result.depth = depth;
            result.nodes = nodes;
            result.timeMs = System.currentTimeMillis() - startTime;

            if (outOfTime()) break;
            if (Math.abs(score) >= MATE_IN_MAX) break; // forced mate found, no need to go deeper
        }

        result.pv = extractPv(board, result.bestMove);
        result.nodes = nodes;
        result.timeMs = System.currentTimeMillis() - startTime;
        return result;
    }

    private int searchRoot(Board board, int depth) {
        MoveList moves = MoveGenerator.generateLegal(board);
        int[] scores = scoreMoves(board, moves, rootBestMove, 0);

        int alpha = -INFINITY, beta = INFINITY;
        int best = -INFINITY;
        int bestMove = moves.get(0);

        for (int i = 0; i < moves.size(); i++) {
            int move = pickNext(moves, scores, i);
            board.makeMove(move);
            int score = -negamax(board, depth - 1, -beta, -alpha, 1);
            board.unmakeMove();

            if (stopped && depth > 1) return best == -INFINITY ? alpha : best;

            if (score > best) {
                best = score;
                bestMove = move;
                if (score > alpha) alpha = score;
            }
        }
        rootBestMove = bestMove;
        rootBestScore = best;
        return best;
    }

    private int negamax(Board board, int depth, int alpha, int beta, int ply) {
        // Draw detection (not at the root).
        if (board.isFiftyMoveRule() || board.isThreefoldRepetition()) return 0;

        if ((nodes & 2047) == 0 && outOfTime()) { stopped = true; return 0; }
        nodes++;

        boolean inCheck = board.inCheck(board.sideToMove());
        if (inCheck) depth++; // check extension: don't drop into qsearch while in check

        if (depth <= 0) return quiescence(board, alpha, beta, ply);

        // --- Transposition table probe. ---
        long key = board.zobristKey();
        int ttMove = 0;
        tt.probe(key, probe);
        if (probe.hit) {
            ttMove = probe.move;
            if (probe.depth >= depth) {
                int s = fromTt(probe.score, ply);
                switch (probe.flag) {
                    case TranspositionTable.FLAG_EXACT -> { return s; }
                    case TranspositionTable.FLAG_LOWER -> { if (s >= beta) return s; }
                    case TranspositionTable.FLAG_UPPER -> { if (s <= alpha) return s; }
                }
            }
        }

        // --- Null-move pruning. Skip when in check or in a likely-zugzwang endgame. ---
        if (!inCheck && depth >= 3 && beta < MATE_IN_MAX && hasNonPawnMaterial(board)) {
            board.makeNullMove();
            int reduction = 2 + depth / 6;
            int nullScore = -negamax(board, depth - 1 - reduction, -beta, -beta + 1, ply + 1);
            board.unmakeNullMove();
            if (stopped) return 0;
            if (nullScore >= beta) return beta;
        }

        MoveList moves = MoveGenerator.generateLegal(board);
        if (moves.isEmpty()) {
            return inCheck ? -MATE + ply : 0; // checkmate (lost) or stalemate (draw)
        }

        int[] scores = scoreMoves(board, moves, ttMove, ply);
        int bestScore = -INFINITY;
        int bestMove = 0;
        byte flag = TranspositionTable.FLAG_UPPER;
        int origAlpha = alpha;

        for (int i = 0; i < moves.size(); i++) {
            int move = pickNext(moves, scores, i);
            board.makeMove(move);
            int score = -negamax(board, depth - 1, -beta, -alpha, ply + 1);
            board.unmakeMove();

            if (stopped) return 0;

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            if (score > alpha) {
                alpha = score;
                flag = TranspositionTable.FLAG_EXACT;
            }
            if (alpha >= beta) {
                flag = TranspositionTable.FLAG_LOWER;
                recordCutoff(board, move, depth, ply); // killer + history
                break;
            }
        }

        tt.store(key, bestMove, depth, toTt(bestScore, ply), flag);
        return bestScore;
    }

    /** Quiescence: only search "noisy" moves (captures/promotions) until quiet. */
    private int quiescence(Board board, int alpha, int beta, int ply) {
        if ((nodes & 2047) == 0 && outOfTime()) { stopped = true; return 0; }
        nodes++;

        int standPat = Evaluation.evaluate(board);
        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;
        if (ply >= MAX_PLY - 1) return standPat;

        MoveList caps = new MoveList();
        MoveGenerator.generateCaptures(board, caps);
        int[] scores = scoreMoves(board, caps, 0, ply);

        for (int i = 0; i < caps.size(); i++) {
            int move = pickNext(caps, scores, i);
            board.makeMove(move);
            int score = -quiescence(board, -beta, -alpha, ply + 1);
            board.unmakeMove();
            if (stopped) return 0;
            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        return alpha;
    }

    // ------------------------------------------------------------------
    //  Move ordering
    // ------------------------------------------------------------------

    private static final int SCORE_TT       = 2_000_000;
    private static final int SCORE_PROMO     = 1_000_000;
    private static final int SCORE_CAPTURE   =   900_000; // base for winning/equal captures
    private static final int SCORE_KILLER_1  =   800_000;
    private static final int SCORE_KILLER_2  =   700_000;

    /** Assign an ordering score to every move so good moves are tried first. */
    private int[] scoreMoves(Board board, MoveList moves, int ttMove, int ply) {
        int[] s = new int[moves.size()];
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            if (move == ttMove) { s[i] = SCORE_TT; continue; }

            int to = Move.to(move);
            int victim = board.pieceAt(to);
            boolean capture = victim != Piece.NONE || Move.isEnPassant(move);

            if (Move.isPromotion(move)) {
                s[i] = SCORE_PROMO + Evaluation.pieceValue(Move.promotionType(move));
            } else if (capture) {
                int attacker = board.pieceAt(Move.from(move));
                int victimVal = Move.isEnPassant(move)
                        ? Evaluation.pieceValue(Piece.PAWN)
                        : Evaluation.pieceValue(Piece.type(victim));
                int attackerVal = Evaluation.pieceValue(Piece.type(attacker));
                // MVV-LVA: most valuable victim, least valuable attacker.
                s[i] = SCORE_CAPTURE + victimVal * 16 - attackerVal;
            } else if (move == killers[ply][0]) {
                s[i] = SCORE_KILLER_1;
            } else if (move == killers[ply][1]) {
                s[i] = SCORE_KILLER_2;
            } else {
                s[i] = history[board.pieceAt(Move.from(move))][to];
            }
        }
        return s;
    }

    /** Selection-sort step: swap the best remaining move into slot {@code i}, return it. */
    private int pickNext(MoveList moves, int[] scores, int i) {
        int bestIdx = i;
        for (int j = i + 1; j < moves.size(); j++) {
            if (scores[j] > scores[bestIdx]) bestIdx = j;
        }
        if (bestIdx != i) {
            int tm = moves.get(i); moves.set(i, moves.get(bestIdx)); moves.set(bestIdx, tm);
            int ts = scores[i]; scores[i] = scores[bestIdx]; scores[bestIdx] = ts;
        }
        return moves.get(i);
    }

    /** On a beta cutoff by a quiet move, remember it as a killer + bump its history. */
    private void recordCutoff(Board board, int move, int depth, int ply) {
        int to = Move.to(move);
        boolean capture = board.pieceAt(to) != Piece.NONE || Move.isEnPassant(move);
        if (capture || Move.isPromotion(move)) return; // killers track quiet moves only
        if (killers[ply][0] != move) {
            killers[ply][1] = killers[ply][0];
            killers[ply][0] = move;
        }
        history[board.pieceAt(Move.from(move))][to] += depth * depth;
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    private boolean hasNonPawnMaterial(Board board) {
        int us = board.sideToMove();
        for (int sq = 0; sq < 64; sq++) {
            int p = board.pieceAt(sq);
            if (p == Piece.NONE || Piece.color(p) != us) continue;
            int t = Piece.type(p);
            if (t != Piece.PAWN && t != Piece.KING) return true;
        }
        return false;
    }

    private boolean outOfTime() {
        return System.currentTimeMillis() - startTime >= timeLimitMs;
    }

    // Mate scores are stored relative to the current node, then re-based on probe.
    private int toTt(int score, int ply) {
        if (score >= MATE_IN_MAX) return score + ply;
        if (score <= -MATE_IN_MAX) return score - ply;
        return score;
    }

    private int fromTt(int score, int ply) {
        if (score >= MATE_IN_MAX) return score - ply;
        if (score <= -MATE_IN_MAX) return score + ply;
        return score;
    }

    /** Walk the transposition table to recover the engine's expected best line. */
    private List<Integer> extractPv(Board board, int firstMove) {
        List<Integer> pv = new ArrayList<>();
        if (firstMove == 0) return pv;
        int made = 0;
        long[] seen = new long[MAX_PLY];
        int move = firstMove;
        while (move != 0 && made < 32) {
            if (!MoveGenerator.generateLegal(board).contains(move)) break;
            // Guard against repetition loops in the PV.
            long key = board.zobristKey();
            boolean loop = false;
            for (int i = 0; i < made; i++) if (seen[i] == key) { loop = true; break; }
            if (loop) break;
            seen[made] = key;

            pv.add(move);
            board.makeMove(move);
            made++;

            tt.probe(board.zobristKey(), probe);
            move = probe.hit ? probe.move : 0;
        }
        for (int i = 0; i < made; i++) board.unmakeMove();
        return pv;
    }
}
