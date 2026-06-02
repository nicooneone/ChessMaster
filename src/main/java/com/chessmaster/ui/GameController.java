package com.chessmaster.ui;

import com.chessmaster.engine.Engine;
import com.chessmaster.engine.Search;
import com.chessmaster.model.*;
import com.chessmaster.training.Analyzer;
import com.chessmaster.training.Puzzle;
import com.chessmaster.training.PuzzleBank;
import javafx.application.Platform;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The glue between the model, the engine, and the JavaFX views. It owns the
 * {@link Game}, drives the {@link Engine} on a background thread (always against a
 * <i>copy</i> of the board so the engine never mutates the position the UI is
 * drawing), and pushes updates into the {@link BoardCanvas} and {@link ControlPanel}.
 */
public final class GameController implements BoardView.InputListener {

    private final BoardView boardView;
    private GamePanel panel;
    private final Engine engine = new Engine();

    private Game game = new Game();
    private Engine.Level level = Engine.Level.INTERMEDIATE;
    private boolean vsAI = true;
    private int humanColor = Color.WHITE;

    private int selected = Square.NONE;
    private volatile boolean thinking = false;

    private final PuzzleBank puzzleBank = new PuzzleBank();
    private boolean puzzleMode = false;
    private Puzzle currentPuzzle;
    private int lastEvalWhiteCp = 0;

    /** Bumped on every state-changing action; background results with a stale
     *  version are discarded (so undo/new-game can't be clobbered by a late search). */
    private volatile int stateVersion = 0;

    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "engine");
        t.setDaemon(true);
        return t;
    });

    public GameController(BoardView boardView) {
        this.boardView = boardView;
        this.boardView.setListener(this);
    }

    public void setPanel(GamePanel panel) {
        this.panel = panel;
    }

    /** Release the engine thread when leaving the screen. */
    public void dispose() {
        engine.stop();
        exec.shutdownNow();
    }

    public Engine engine() {
        return engine;
    }

    // ------------------------------------------------------------------
    //  Public actions (wired to the control panel)
    // ------------------------------------------------------------------

    public void newGame(Engine.Level level, int humanColor, boolean vsAI) {
        this.level = level;
        this.humanColor = humanColor;
        this.vsAI = vsAI;
        this.puzzleMode = false;
        this.currentPuzzle = null;
        if (panel != null) panel.setGoal("");
        startNewGame(Board.START_FEN);
    }

    /** Start a fresh game / puzzle from any FEN. */
    public void startNewGame(String fen) {
        engine.stop();
        stateVersion++;
        thinking = false;
        game = new Game(fen);
        engine.newGame();
        lastEvalWhiteCp = 0;
        boardView.setBoard(game.board());
        boardView.clearSelection();
        boardView.clearHint();
        boardView.setLastMove(Square.NONE, Square.NONE);
        selected = Square.NONE;
        refreshAll();
        kickEvalBar();
        maybeEngineMove();
    }

    public void setLevel(Engine.Level level) {
        this.level = level;
    }

    public Engine.Level level() {
        return level;
    }

    public Game game() {
        return game;
    }

    public void flipBoard() {
        boardView.setFlipped(!boardView.isFlipped());
    }

    public void undo() {
        engine.stop();
        stateVersion++;
        thinking = false;
        boolean undone = game.undo();
        // In a game vs the AI, also take back the engine's reply so it's the human's turn.
        if (undone && vsAI && !game.history().isEmpty() && game.board().sideToMove() != humanColor) {
            game.undo();
        }
        boardView.setBoard(game.board());
        boardView.clearSelection();
        boardView.clearHint();
        selected = Square.NONE;
        setLastMoveFromHistory();
        refreshAll();
        kickEvalBar();
    }

    /** Compute the engine's recommended move and show it as an arrow + text. */
    public void requestHint() {
        if (game.status().isGameOver()) return;
        final int v = stateVersion;
        Board snap = game.board().copy();
        if (panel != null) panel.setStatus("Thinking about a hint…");
        exec.submit(() -> {
            Search.Result r = engine.analyze(snap, Search.Limits.time(800));
            Platform.runLater(() -> {
                if (v != stateVersion || r.bestMove == Move.NONE) return;
                boardView.setHint(Move.from(r.bestMove), Move.to(r.bestMove));
                String san = sanOf(game.board(), r.bestMove);
                if (panel != null) {
                    panel.setStatus("Hint: try " + san + "  " + scoreText(r));
                }
            });
        });
    }

    // ------------------------------------------------------------------
    //  Training: puzzles
    // ------------------------------------------------------------------

    /** Load a random tactics puzzle (any difficulty). */
    public void loadRandomPuzzle() {
        loadPuzzle(null);
    }

    /** Load a random puzzle of the given difficulty (or any difficulty if {@code null}). */
    public void loadPuzzle(Puzzle.Difficulty difficulty) {
        startPuzzle(puzzleBank.random(difficulty));
    }

    private void startPuzzle(Puzzle p) {
        currentPuzzle = p;
        puzzleMode = true;
        vsAI = false;
        humanColor = new Game(p.fen()).board().sideToMove();
        startNewGame(p.fen());
        if (panel != null) {
            panel.setGoal("🧩 " + p.goal());
            panel.setStatus("Your move — find the solution.");
        }
    }

    /** Reload the current puzzle from the start (the "try again" button). */
    public void retryPuzzle() {
        if (currentPuzzle != null) startPuzzle(currentPuzzle);
    }

    /**
     * Reveal and play the solving move (and the engine's reply) for the current
     * puzzle — the "show solution" button.
     */
    public void showPuzzleSolution() {
        if (!puzzleMode || game.status().isGameOver()) return;
        final int v = stateVersion;
        Board snap = game.board().copy();
        if (panel != null) panel.setStatus("Revealing the solution…");
        exec.submit(() -> {
            Search.Result r = engine.analyze(snap, Search.Limits.time(900));
            Platform.runLater(() -> {
                if (v != stateVersion || r.bestMove == Move.NONE || !game.isLegal(r.bestMove)) return;
                String san = sanOf(game.board(), r.bestMove);
                applyMove(r.bestMove);
                if (panel != null) panel.setStatus("Solution: " + san + "  " + scoreText(r));
            });
        });
    }

    /**
     * Validate the solver's move with the engine: accept it if it is the engine's
     * top choice or essentially as good, then auto-play the best reply. Detect when
     * the puzzle is effectively won (mate or a decisive material edge).
     */
    private void handlePuzzleMove(int userMove) {
        thinking = true;
        if (panel != null) { panel.setThinking(true); panel.setStatus("Checking your move…"); }
        final int v = stateVersion;
        final Board pre = game.board().copy();

        exec.submit(() -> {
            Search.Result best = engine.analyze(pre, Search.Limits.time(600));
            Board after = pre.copy();
            after.makeMove(userMove);
            boolean userMated = MoveGenerator.generateLegal(after).isEmpty()
                    && after.inCheck(after.sideToMove());
            Search.Result resp = engine.analyze(after, Search.Limits.time(600));
            final int userScore = userMated ? Search.MATE : -resp.score; // solver POV
            final boolean correct = Move.toUci(userMove).equals(Move.toUci(best.bestMove))
                    || userScore >= best.score - 40;
            final int reply = resp.bestMove;

            // Look one move further to decide whether the puzzle is essentially solved.
            boolean replyLegal = reply != Move.NONE && MoveGenerator.generateLegal(after).contains(reply);
            int afterReplySolverScore = userScore;
            boolean afterReplyMate = userMated;
            if (replyLegal) {
                Board afterReply = after.copy();
                afterReply.makeMove(reply);
                if (!MoveGenerator.generateLegal(afterReply).isEmpty()) {
                    Search.Result n = engine.analyze(afterReply, Search.Limits.time(300));
                    afterReplySolverScore = n.score;
                    afterReplyMate = n.isMate();
                }
            }
            final boolean fReplyLegal = replyLegal;
            final int fProgress = afterReplySolverScore;
            final boolean fProgressMate = afterReplyMate;
            final boolean fUserMated = userMated;

            Platform.runLater(() -> {
                thinking = false;
                if (panel != null) panel.setThinking(false);
                if (v != stateVersion) return;

                if (!correct) {
                    if (panel != null) panel.setWarning("❌ Not the best move — try again. (Hint can help.)");
                    return;
                }

                applyMove(userMove);
                if (fUserMated || game.status() == GameStatus.CHECKMATE) { puzzleSolved("Checkmate!"); return; }

                if (fReplyLegal && game.isLegal(reply)) {
                    game.play(reply);
                    stateVersion++;
                    boardView.setLastMove(Move.from(reply), Move.to(reply));
                    refreshAll();
                    kickEvalBar();
                }
                if (game.status().isGameOver()) { puzzleSolved("Game over."); return; }

                if (fProgressMate || fProgress >= 300) {
                    puzzleSolved("You're winning!");
                } else if (panel != null) {
                    panel.setStatus("✅ Correct — keep going.");
                }
            });
        });
    }

    private void puzzleSolved(String why) {
        if (panel != null) {
            panel.setStatus("✅ Puzzle solved — " + why + "  Press “Next puzzle”.");
            panel.puzzleSolved();
        }
    }

    // ------------------------------------------------------------------
    //  Training: full-game analysis
    // ------------------------------------------------------------------

    /** Review the whole game with the engine and pop up the analysis window. */
    public void analyzeGame() {
        if (game.history().isEmpty()) {
            if (panel != null) panel.setStatus("Play some moves first, then analyze.");
            return;
        }
        final int total = game.history().size();
        final Game snapshot = snapshotGame();
        if (panel != null) panel.setStatus("Analyzing game… 0/" + total);
        exec.submit(() -> {
            Analyzer analyzer = new Analyzer(engine, 250);
            Analyzer.Review review = analyzer.analyze(snapshot,
                    (done, t) -> Platform.runLater(() -> {
                        if (panel != null) panel.setStatus("Analyzing game… " + done + "/" + t);
                    }));
            Platform.runLater(() -> {
                if (panel != null) panel.setStatus(statusText());
                AnalysisView.show(review);
            });
        });
    }

    /** Build an independent copy of the current game (so analysis can't disturb play). */
    private Game snapshotGame() {
        Game copy = new Game(game.startFen());
        for (Game.PlayedMove pm : game.history()) copy.play(pm.move());
        return copy;
    }

    // ------------------------------------------------------------------
    //  Input from the board
    // ------------------------------------------------------------------

    @Override
    public void onPress(int square) {
        if (!isHumanTurn()) return;
        int p = game.board().pieceAt(square);
        if (p != Piece.NONE && Piece.color(p) == game.board().sideToMove()) {
            select(square);
        }
    }

    @Override
    public void onRelease(int square) {
        if (!isHumanTurn() || selected == Square.NONE) return;
        if (square == selected) return; // just a select-click; wait for the target click
        int move = findMove(selected, square);
        if (move != Move.NONE) {
            playHumanMove(move);
        } else {
            int p = game.board().pieceAt(square);
            if (p != Piece.NONE && Piece.color(p) == game.board().sideToMove()) {
                select(square);
            } else {
                clearSelection();
            }
        }
    }

    private void select(int square) {
        selected = square;
        boardView.clearHint();
        boardView.setSelection(square, game.legalMovesFrom(square));
    }

    private void clearSelection() {
        selected = Square.NONE;
        boardView.clearSelection();
    }

    // ------------------------------------------------------------------
    //  Move application
    // ------------------------------------------------------------------

    private void playHumanMove(int move) {
        if (puzzleMode) {
            clearSelection();
            handlePuzzleMove(move);
            return;
        }
        applyMove(move);
        clearSelection();
        maybeEngineMove();
    }

    private void applyMove(int move) {
        int from = Move.from(move), to = Move.to(move);
        game.play(move);
        stateVersion++;
        boardView.clearHint();
        boardView.setLastMove(from, to);
        refreshAll();
        kickEvalBar();
    }

    private void maybeEngineMove() {
        if (!vsAI || game.status().isGameOver()) return;
        if (game.board().sideToMove() == humanColor) return;

        thinking = true;
        final int v = stateVersion;
        final Engine.Level lvl = level;
        Board snap = game.board().copy();
        if (panel != null) { panel.setThinking(true); panel.setStatus(lvl + " is thinking…"); }

        exec.submit(() -> {
            int move = engine.chooseMove(snap, lvl);
            Platform.runLater(() -> {
                thinking = false;
                if (panel != null) panel.setThinking(false);
                if (v != stateVersion) return;            // user changed something meanwhile
                if (move == Move.NONE || !game.isLegal(move)) { refreshAll(); return; }
                applyMove(move);
            });
        });
    }

    /** Background, low-budget analysis to drive the evaluation bar + best-line tip. */
    private void kickEvalBar() {
        if (panel == null) return;
        final int v = stateVersion;
        if (game.status().isGameOver()) { return; }
        Board snap = game.board().copy();
        exec.submit(() -> {
            Search.Result r = engine.analyze(snap, Search.Limits.time(350));
            Platform.runLater(() -> {
                if (v != stateVersion) return;
                int whiteCp = (game.board().sideToMove() == Color.WHITE) ? r.score : -r.score;
                panel.setEval(whiteCp, r.isMate() ? signedMate(r) : 0, pvToSan(game.board(), r.pv));
                detectBlunder(whiteCp, r.isMate());
                lastEvalWhiteCp = whiteCp;
            });
        });
    }

    /** Warn the human if their last move dropped a lot of evaluation. */
    private void detectBlunder(int whiteCp, boolean isMate) {
        if (puzzleMode || isMate || game.history().isEmpty()) return;
        int mover = Color.opposite(game.board().sideToMove()); // the side that just moved
        boolean warnFor = !vsAI || mover == humanColor;
        if (!warnFor) return;
        int prevMover = (mover == Color.WHITE) ? lastEvalWhiteCp : -lastEvalWhiteCp;
        int nowMover = (mover == Color.WHITE) ? whiteCp : -whiteCp;
        int delta = nowMover - prevMover;
        if (delta <= -150) {
            panel.setWarning(String.format("⚠ Possible blunder — that dropped %.1f", -delta / 100.0));
        }
    }

    private int signedMate(Search.Result r) {
        // Convert "mate in N for side to move" into a White-relative signed value.
        int m = r.mateIn();
        return (game.board().sideToMove() == Color.WHITE) ? m : -m;
    }

    // ------------------------------------------------------------------
    //  UI refresh helpers
    // ------------------------------------------------------------------

    private void refreshAll() {
        boardView.refresh();
        if (panel == null) return;
        panel.setMoveList(game.history());
        panel.clearWarning();
        if (!puzzleMode) panel.setStatus(statusText());
    }

    private String statusText() {
        GameStatus s = game.status();
        return switch (s) {
            case CHECKMATE -> Color.name(Color.opposite(game.board().sideToMove())) + " wins by checkmate";
            case STALEMATE -> "Draw — stalemate";
            case DRAW_FIFTY_MOVE -> "Draw — 50-move rule";
            case DRAW_REPETITION -> "Draw — threefold repetition";
            case DRAW_INSUFFICIENT -> "Draw — insufficient material";
            case ONGOING -> {
                String side = Color.name(game.board().sideToMove());
                String chk = game.board().inCheck(game.board().sideToMove()) ? " — check!" : "";
                yield side + " to move" + chk;
            }
        };
    }

    private boolean isHumanTurn() {
        if (thinking || game.status().isGameOver()) return false;
        return !vsAI || game.board().sideToMove() == humanColor;
    }

    private int findMove(int from, int to) {
        MoveList legal = game.legalMoves();
        int match = Move.NONE;
        boolean promo = false;
        for (int i = 0; i < legal.size(); i++) {
            int m = legal.get(i);
            if (Move.from(m) == from && Move.to(m) == to) {
                if (Move.isPromotion(m)) promo = true;
                match = m;
            }
        }
        if (promo) return promotionChoice(from, to);
        return match;
    }

    /** Ask the user which piece to promote to (defaults to queen). */
    private int promotionChoice(int from, int to) {
        List<String> choices = List.of("Queen", "Rook", "Bishop", "Knight");
        javafx.scene.control.ChoiceDialog<String> dialog =
                new javafx.scene.control.ChoiceDialog<>("Queen", choices);
        dialog.setTitle("Pawn promotion");
        dialog.setHeaderText("Promote your pawn to:");
        dialog.setContentText("Piece:");
        Optional<String> result = dialog.showAndWait();
        int type = switch (result.orElse("Queen")) {
            case "Rook" -> Piece.ROOK;
            case "Bishop" -> Piece.BISHOP;
            case "Knight" -> Piece.KNIGHT;
            default -> Piece.QUEEN;
        };
        return Move.makePromotion(from, to, type);
    }

    private void setLastMoveFromHistory() {
        List<Game.PlayedMove> h = game.history();
        if (h.isEmpty()) { boardView.setLastMove(Square.NONE, Square.NONE); return; }
        int last = h.get(h.size() - 1).move();
        boardView.setLastMove(Move.from(last), Move.to(last));
    }

    private String scoreText(Search.Result r) {
        if (r.isMate()) return "(mate in " + Math.abs(r.mateIn()) + ")";
        return String.format("(%+.2f)", r.score / 100.0);
    }

    /** Convert a SAN for a single move on the given board (without mutating it permanently). */
    private String sanOf(Board board, int move) {
        return Notation.toSan(board, move);
    }

    /** Convert a principal-variation move list into readable SAN by replaying on a copy. */
    private String pvToSan(Board board, List<Integer> pv) {
        if (pv == null || pv.isEmpty()) return "";
        Board b = board.copy();
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (int m : pv) {
            if (!MoveGenerator.generateLegal(b).contains(m)) break;
            sb.append(Notation.toSan(b, m)).append(' ');
            b.makeMove(m);
            if (++shown >= 6) break;
        }
        return sb.toString().trim();
    }
}
