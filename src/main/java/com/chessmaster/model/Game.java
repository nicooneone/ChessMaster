package com.chessmaster.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A higher-level wrapper around {@link Board} that the UI and training tools talk
 * to. It keeps the running list of moves (with SAN text for display/PGN),
 * detects game-ending conditions, and supports undo.
 */
public final class Game {

    /** One played move plus the SAN string and the FEN of the position before it. */
    public record PlayedMove(int move, String san, String fenBefore) {}

    private final Board board;
    private final List<PlayedMove> history = new ArrayList<>();
    private final String startFen;

    public Game() {
        this(Board.START_FEN);
    }

    public Game(String fen) {
        this.startFen = fen;
        this.board = new Board();
        this.board.setFromFen(fen);
    }

    public Board board() {
        return board;
    }

    public List<PlayedMove> history() {
        return history;
    }

    public MoveList legalMoves() {
        return MoveGenerator.generateLegal(board);
    }

    /** Legal moves originating from a given square (for highlighting in the UI). */
    public MoveList legalMovesFrom(int square) {
        MoveList all = legalMoves();
        MoveList out = new MoveList();
        for (int i = 0; i < all.size(); i++) {
            if (Move.from(all.get(i)) == square) out.add(all.get(i));
        }
        return out;
    }

    public boolean isLegal(int move) {
        return legalMoves().contains(move);
    }

    /** Play a move, recording SAN. Returns false if the move is not legal. */
    public boolean play(int move) {
        if (!isLegal(move)) return false;
        String san = Notation.toSan(board, move);
        String fenBefore = board.toFen();
        board.makeMove(move);
        history.add(new PlayedMove(move, san, fenBefore));
        return true;
    }

    /** Undo the last played move. Returns false if there is nothing to undo. */
    public boolean undo() {
        if (history.isEmpty()) return false;
        history.remove(history.size() - 1);
        board.unmakeMove();
        return true;
    }

    public GameStatus status() {
        boolean noMoves = legalMoves().isEmpty();
        if (noMoves) {
            return board.inCheck(board.sideToMove()) ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
        }
        if (board.isFiftyMoveRule())          return GameStatus.DRAW_FIFTY_MOVE;
        if (board.isThreefoldRepetition())    return GameStatus.DRAW_REPETITION;
        if (isInsufficientMaterial())         return GameStatus.DRAW_INSUFFICIENT;
        return GameStatus.ONGOING;
    }

    /**
     * Conservative insufficient-material check covering the cases that can never
     * be mated: K vs K, K+minor vs K, and K+B vs K+B with both bishops on the
     * same color complex.
     */
    public boolean isInsufficientMaterial() {
        int whiteBishops = 0, blackBishops = 0, knights = 0, others = 0;
        int bishopSquareColorMask = 0; // bit 0 = a bishop on a light sq, bit 1 = dark sq
        for (int sq = 0; sq < 64; sq++) {
            int p = board.pieceAt(sq);
            if (p == Piece.NONE) continue;
            switch (Piece.type(p)) {
                case Piece.KING -> { /* ignore */ }
                case Piece.KNIGHT -> knights++;
                case Piece.BISHOP -> {
                    if (Piece.isWhite(p)) whiteBishops++; else blackBishops++;
                    boolean light = ((Square.file(sq) + Square.rank(sq)) & 1) == 1;
                    bishopSquareColorMask |= light ? 1 : 2;
                }
                default -> others++; // pawn, rook, or queen -> sufficient material
            }
        }
        if (others > 0) return false;
        int minors = knights + whiteBishops + blackBishops;
        if (minors <= 1) return true;                              // KvK, KNvK, KBvK
        if (knights == 0 && bishopSquareColorMask != 3) return true; // all bishops same color
        return false;
    }

    public String startFen() {
        return startFen;
    }

    /** Export the game as a minimal but valid PGN movetext. */
    public String toPgn() {
        StringBuilder sb = new StringBuilder();
        if (!startFen.equals(Board.START_FEN)) {
            sb.append("[SetUp \"1\"]\n[FEN \"").append(startFen).append("\"]\n");
        }
        sb.append("[Event \"ChessMaster Game\"]\n\n");
        // Determine the move number of the first ply from the start FEN.
        Board tmp = new Board();
        tmp.setFromFen(startFen);
        int moveNumber = tmp.fullmoveNumber();
        boolean whiteToMove = tmp.sideToMove() == Color.WHITE;
        for (int i = 0; i < history.size(); i++) {
            if (whiteToMove) sb.append(moveNumber).append(". ");
            else if (i == 0) sb.append(moveNumber).append("... ");
            sb.append(history.get(i).san()).append(' ');
            if (!whiteToMove) moveNumber++;
            whiteToMove = !whiteToMove;
        }
        sb.append(resultTag());
        return sb.toString().trim();
    }

    private String resultTag() {
        GameStatus s = status();
        if (s == GameStatus.CHECKMATE) {
            return board.sideToMove() == Color.WHITE ? "0-1" : "1-0";
        }
        if (s.isDraw()) return "1/2-1/2";
        return "*";
    }
}
