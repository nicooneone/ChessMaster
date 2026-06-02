package com.chessmaster.model;

/**
 * The full state of a chess position plus the logic to make and un-make moves.
 *
 * <p>The board is an {@code int[64]} of encoded {@link Piece} values. Alongside it
 * we track whose turn it is, the castling rights, the en-passant target square,
 * the half-move clock (for the 50-move rule), the full-move number, and an
 * incrementally-maintained {@link Zobrist} hash.
 *
 * <p>{@link #makeMove(int)} pushes an {@code Undo} record onto an internal stack so
 * {@link #unmakeMove()} can perfectly restore the previous state — this is what
 * lets the search explore millions of positions without copying the board.
 */
public final class Board {

    // ----- Castling-right bits -----
    public static final int CASTLE_WK = 1; // White  O-O
    public static final int CASTLE_WQ = 2; // White  O-O-O
    public static final int CASTLE_BK = 4; // Black  O-O
    public static final int CASTLE_BQ = 8; // Black  O-O-O

    public static final String START_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private final int[] squares = new int[64];
    private int sideToMove = Color.WHITE;
    private int castlingRights = 0;
    private int enPassantSquare = Square.NONE;
    private int halfmoveClock = 0;
    private int fullmoveNumber = 1;
    private long zobristKey = 0L;
    private final int[] kingSquare = {Square.NONE, Square.NONE};

    // Repetition history: Zobrist key after each position reached this game/search.
    private long[] historyKeys = new long[256];
    private int historyCount = 0;

    // Undo stack, indexed by ply. Pre-grown as needed.
    private Undo[] undoStack = new Undo[256];
    private int ply = 0;

    /** Per-square mask AND'd into castling rights whenever that square is touched. */
    private static final int[] CASTLE_MASK = new int[64];

    static {
        for (int sq = 0; sq < 64; sq++) CASTLE_MASK[sq] = 0xF;
        CASTLE_MASK[Square.parse("a1")] &= ~CASTLE_WQ;
        CASTLE_MASK[Square.parse("h1")] &= ~CASTLE_WK;
        CASTLE_MASK[Square.parse("e1")] &= ~(CASTLE_WK | CASTLE_WQ);
        CASTLE_MASK[Square.parse("a8")] &= ~CASTLE_BQ;
        CASTLE_MASK[Square.parse("h8")] &= ~CASTLE_BK;
        CASTLE_MASK[Square.parse("e8")] &= ~(CASTLE_BK | CASTLE_BQ);
    }

    /** A snapshot of the bits that {@link #makeMove} cannot recompute on its own. */
    private static final class Undo {
        int move;
        int capturedPiece;
        int capturedSquare;
        int prevCastling;
        int prevEnPassant;
        int prevHalfmove;
        int prevFullmove;
        long prevZobrist;
    }

    public Board() {
        for (int i = 0; i < undoStack.length; i++) undoStack[i] = new Undo();
    }

    public static Board startPosition() {
        Board b = new Board();
        b.setFromFen(START_FEN);
        return b;
    }

    // ---------------------------------------------------------------------
    //  Accessors
    // ---------------------------------------------------------------------

    public int pieceAt(int sq)        { return squares[sq]; }
    public int sideToMove()           { return sideToMove; }
    public int castlingRights()       { return castlingRights; }
    public int enPassantSquare()      { return enPassantSquare; }
    public int halfmoveClock()        { return halfmoveClock; }
    public int fullmoveNumber()       { return fullmoveNumber; }
    public long zobristKey()          { return zobristKey; }
    public int kingSquare(int color)  { return kingSquare[color]; }

    public boolean canCastle(int rightBit) {
        return (castlingRights & rightBit) != 0;
    }

    // ---------------------------------------------------------------------
    //  FEN
    // ---------------------------------------------------------------------

    public void setFromFen(String fen) {
        java.util.Arrays.fill(squares, Piece.NONE);
        kingSquare[Color.WHITE] = Square.NONE;
        kingSquare[Color.BLACK] = Square.NONE;
        historyCount = 0;
        ply = 0;

        String[] parts = fen.trim().split("\\s+");
        // 1) Piece placement, written from rank 8 down to rank 1.
        int rank = 7, file = 0;
        for (char c : parts[0].toCharArray()) {
            if (c == '/') {
                rank--;
                file = 0;
            } else if (Character.isDigit(c)) {
                file += c - '0';
            } else {
                int piece = Piece.fromFenChar(c);
                int sq = Square.of(file, rank);
                squares[sq] = piece;
                if (Piece.type(piece) == Piece.KING) {
                    kingSquare[Piece.color(piece)] = sq;
                }
                file++;
            }
        }

        // 2) Side to move.
        sideToMove = parts[1].equals("w") ? Color.WHITE : Color.BLACK;

        // 3) Castling rights.
        castlingRights = 0;
        if (parts.length > 2 && !parts[2].equals("-")) {
            for (char c : parts[2].toCharArray()) {
                switch (c) {
                    case 'K' -> castlingRights |= CASTLE_WK;
                    case 'Q' -> castlingRights |= CASTLE_WQ;
                    case 'k' -> castlingRights |= CASTLE_BK;
                    case 'q' -> castlingRights |= CASTLE_BQ;
                    default  -> { /* ignore */ }
                }
            }
        }

        // 4) En-passant target square.
        enPassantSquare = (parts.length > 3 && !parts[3].equals("-"))
                ? Square.parse(parts[3]) : Square.NONE;

        // 5) Half-move clock and 6) full-move number (optional).
        halfmoveClock = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
        fullmoveNumber = parts.length > 5 ? Integer.parseInt(parts[5]) : 1;

        zobristKey = computeZobrist();
        pushHistory(zobristKey);
    }

    public String toFen() {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            int empties = 0;
            for (int file = 0; file < 8; file++) {
                int p = squares[Square.of(file, rank)];
                if (p == Piece.NONE) {
                    empties++;
                } else {
                    if (empties > 0) { sb.append(empties); empties = 0; }
                    sb.append(Piece.toFenChar(p));
                }
            }
            if (empties > 0) sb.append(empties);
            if (rank > 0) sb.append('/');
        }
        sb.append(sideToMove == Color.WHITE ? " w " : " b ");

        StringBuilder rights = new StringBuilder();
        if (canCastle(CASTLE_WK)) rights.append('K');
        if (canCastle(CASTLE_WQ)) rights.append('Q');
        if (canCastle(CASTLE_BK)) rights.append('k');
        if (canCastle(CASTLE_BQ)) rights.append('q');
        sb.append(rights.length() == 0 ? "-" : rights.toString());

        sb.append(' ').append(Square.name(enPassantSquare));
        sb.append(' ').append(halfmoveClock);
        sb.append(' ').append(fullmoveNumber);
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    //  Make / un-make
    // ---------------------------------------------------------------------

    public void makeMove(int move) {
        Undo u = undoStack(ply);
        u.move = move;
        u.prevCastling = castlingRights;
        u.prevEnPassant = enPassantSquare;
        u.prevHalfmove = halfmoveClock;
        u.prevFullmove = fullmoveNumber;
        u.prevZobrist = zobristKey;
        u.capturedPiece = Piece.NONE;
        u.capturedSquare = Square.NONE;

        int from = Move.from(move);
        int to = Move.to(move);
        int flag = Move.flag(move);
        int piece = squares[from];
        int us = sideToMove;
        int them = Color.opposite(us);

        // Remove old en-passant and castling contributions from the hash.
        if (enPassantSquare != Square.NONE) {
            zobristKey ^= Zobrist.EN_PASSANT_FILE[Square.file(enPassantSquare)];
        }
        zobristKey ^= Zobrist.CASTLING[castlingRights];

        boolean pawnMove = Piece.type(piece) == Piece.PAWN;
        boolean capture = false;

        // --- Lift the moving piece off 'from'. ---
        squares[from] = Piece.NONE;
        zobristKey ^= Zobrist.PIECE[piece][from];

        // --- Handle captures (including en passant). ---
        if (flag == Move.FLAG_EN_PASSANT) {
            int capSq = to + (us == Color.WHITE ? -8 : 8);
            u.capturedPiece = squares[capSq];
            u.capturedSquare = capSq;
            squares[capSq] = Piece.NONE;
            zobristKey ^= Zobrist.PIECE[u.capturedPiece][capSq];
            capture = true;
        } else if (squares[to] != Piece.NONE) {
            u.capturedPiece = squares[to];
            u.capturedSquare = to;
            zobristKey ^= Zobrist.PIECE[u.capturedPiece][to];
            capture = true;
        }

        // --- Place the moving piece on 'to' (promoting if needed). ---
        int placed = piece;
        if (flag == Move.FLAG_PROMOTION) {
            placed = Piece.make(us, Move.promotionType(move));
        }
        squares[to] = placed;
        zobristKey ^= Zobrist.PIECE[placed][to];

        // --- Move the rook when castling. ---
        if (flag == Move.FLAG_CASTLE_KING) {
            moveRookForCastle(us == Color.WHITE ? 7 : 63, us == Color.WHITE ? 5 : 61, us);
        } else if (flag == Move.FLAG_CASTLE_QUEEN) {
            moveRookForCastle(us == Color.WHITE ? 0 : 56, us == Color.WHITE ? 3 : 59, us);
        }

        // --- Keep the king-square cache current. ---
        if (Piece.type(piece) == Piece.KING) {
            kingSquare[us] = to;
        }

        // --- Update castling rights (king/rook moved, or a rook was captured). ---
        castlingRights &= CASTLE_MASK[from] & CASTLE_MASK[to];
        zobristKey ^= Zobrist.CASTLING[castlingRights];

        // --- Set a new en-passant square only on a double pawn push. ---
        if (flag == Move.FLAG_DOUBLE_PUSH) {
            enPassantSquare = (from + to) / 2;
            zobristKey ^= Zobrist.EN_PASSANT_FILE[Square.file(enPassantSquare)];
        } else {
            enPassantSquare = Square.NONE;
        }

        // --- Clocks. ---
        halfmoveClock = (pawnMove || capture) ? 0 : halfmoveClock + 1;
        if (us == Color.BLACK) fullmoveNumber++;

        // --- Flip side to move. ---
        sideToMove = them;
        zobristKey ^= Zobrist.SIDE_TO_MOVE;

        ply++;
        pushHistory(zobristKey);
    }

    public void unmakeMove() {
        ply--;
        Undo u = undoStack[ply];
        popHistory();

        int move = u.move;
        int from = Move.from(move);
        int to = Move.to(move);
        int flag = Move.flag(move);

        sideToMove = Color.opposite(sideToMove);
        int us = sideToMove;

        // Restore the moving piece on 'from' (un-promote back to a pawn).
        int moved = squares[to];
        if (flag == Move.FLAG_PROMOTION) {
            moved = Piece.make(us, Piece.PAWN);
        }
        squares[from] = moved;
        squares[to] = Piece.NONE;

        // Put any captured piece back (its square differs from 'to' for en passant).
        if (u.capturedPiece != Piece.NONE) {
            squares[u.capturedSquare] = u.capturedPiece;
        }

        // Undo the rook hop for castling.
        if (flag == Move.FLAG_CASTLE_KING) {
            undoRookForCastle(us == Color.WHITE ? 7 : 63, us == Color.WHITE ? 5 : 61);
        } else if (flag == Move.FLAG_CASTLE_QUEEN) {
            undoRookForCastle(us == Color.WHITE ? 0 : 56, us == Color.WHITE ? 3 : 59);
        }

        if (Piece.type(moved) == Piece.KING) {
            kingSquare[us] = from;
        }

        castlingRights = u.prevCastling;
        enPassantSquare = u.prevEnPassant;
        halfmoveClock = u.prevHalfmove;
        fullmoveNumber = u.prevFullmove;
        zobristKey = u.prevZobrist;
    }

    /** Make a "null move" (pass the turn) — used by the engine's null-move pruning. */
    public void makeNullMove() {
        Undo u = undoStack(ply);
        u.move = Move.NONE;
        u.prevCastling = castlingRights;
        u.prevEnPassant = enPassantSquare;
        u.prevHalfmove = halfmoveClock;
        u.prevFullmove = fullmoveNumber;
        u.prevZobrist = zobristKey;
        u.capturedPiece = Piece.NONE;
        u.capturedSquare = Square.NONE;

        if (enPassantSquare != Square.NONE) {
            zobristKey ^= Zobrist.EN_PASSANT_FILE[Square.file(enPassantSquare)];
        }
        enPassantSquare = Square.NONE;
        sideToMove = Color.opposite(sideToMove);
        zobristKey ^= Zobrist.SIDE_TO_MOVE;
        ply++;
        pushHistory(zobristKey);
    }

    public void unmakeNullMove() {
        ply--;
        Undo u = undoStack[ply];
        popHistory();
        sideToMove = Color.opposite(sideToMove);
        castlingRights = u.prevCastling;
        enPassantSquare = u.prevEnPassant;
        halfmoveClock = u.prevHalfmove;
        fullmoveNumber = u.prevFullmove;
        zobristKey = u.prevZobrist;
    }

    private void moveRookForCastle(int rookFrom, int rookTo, int us) {
        int rook = squares[rookFrom];
        squares[rookFrom] = Piece.NONE;
        zobristKey ^= Zobrist.PIECE[rook][rookFrom];
        squares[rookTo] = rook;
        zobristKey ^= Zobrist.PIECE[rook][rookTo];
    }

    private void undoRookForCastle(int rookFrom, int rookTo) {
        int rook = squares[rookTo];
        squares[rookTo] = Piece.NONE;
        squares[rookFrom] = rook;
    }

    // ---------------------------------------------------------------------
    //  Attack / check detection
    // ---------------------------------------------------------------------

    public boolean inCheck(int color) {
        return isSquareAttacked(kingSquare[color], Color.opposite(color));
    }

    /** Convenience: is the side to move currently in check? */
    public boolean inCheck() {
        return inCheck(sideToMove);
    }

    private static final int[][] KNIGHT_DELTAS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };
    private static final int[][] KING_DELTAS = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
    };
    private static final int[][] BISHOP_DIRS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] ROOK_DIRS   = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** @return true if {@code sq} is attacked by any piece of color {@code by}. */
    public boolean isSquareAttacked(int sq, int by) {
        int f = Square.file(sq);
        int r = Square.rank(sq);

        // Pawns: a 'by' pawn attacks diagonally toward us.
        int pawnRankDir = (by == Color.WHITE) ? -1 : 1; // where the attacker sits, relative to sq
        int pawnPiece = Piece.make(by, Piece.PAWN);
        if (Square.onBoard(f - 1, r + pawnRankDir)
                && squares[Square.of(f - 1, r + pawnRankDir)] == pawnPiece) return true;
        if (Square.onBoard(f + 1, r + pawnRankDir)
                && squares[Square.of(f + 1, r + pawnRankDir)] == pawnPiece) return true;

        // Knights.
        int knightPiece = Piece.make(by, Piece.KNIGHT);
        for (int[] d : KNIGHT_DELTAS) {
            int nf = f + d[0], nr = r + d[1];
            if (Square.onBoard(nf, nr) && squares[Square.of(nf, nr)] == knightPiece) return true;
        }

        // King (adjacent).
        int kingPiece = Piece.make(by, Piece.KING);
        for (int[] d : KING_DELTAS) {
            int nf = f + d[0], nr = r + d[1];
            if (Square.onBoard(nf, nr) && squares[Square.of(nf, nr)] == kingPiece) return true;
        }

        // Bishops / queens along diagonals.
        if (raySliderHits(f, r, BISHOP_DIRS, by, Piece.BISHOP)) return true;
        // Rooks / queens along files and ranks.
        if (raySliderHits(f, r, ROOK_DIRS, by, Piece.ROOK)) return true;

        return false;
    }

    /**
     * Walk each direction from (f,r); the first piece encountered is checked. It
     * counts as an attacker if it is a {@code straightType} slider (ROOK or BISHOP)
     * or a QUEEN of color {@code by}.
     */
    private boolean raySliderHits(int f, int r, int[][] dirs, int by, int straightType) {
        for (int[] d : dirs) {
            int nf = f + d[0], nr = r + d[1];
            while (Square.onBoard(nf, nr)) {
                int p = squares[Square.of(nf, nr)];
                if (p != Piece.NONE) {
                    if (Piece.color(p) == by) {
                        int t = Piece.type(p);
                        if (t == straightType || t == Piece.QUEEN) return true;
                    }
                    break; // blocked by the first piece either way
                }
                nf += d[0];
                nr += d[1];
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------
    //  Zobrist + repetition history
    // ---------------------------------------------------------------------

    public long computeZobrist() {
        long key = 0L;
        for (int sq = 0; sq < 64; sq++) {
            int p = squares[sq];
            if (p != Piece.NONE) key ^= Zobrist.PIECE[p][sq];
        }
        key ^= Zobrist.CASTLING[castlingRights];
        if (enPassantSquare != Square.NONE) {
            key ^= Zobrist.EN_PASSANT_FILE[Square.file(enPassantSquare)];
        }
        if (sideToMove == Color.BLACK) key ^= Zobrist.SIDE_TO_MOVE;
        return key;
    }

    /** @return how many times the current position has occurred (1 = first time). */
    public int repetitionCount() {
        int count = 0;
        // Positions repeat only between irreversible moves; scan back over the
        // reversible window (bounded by the half-move clock).
        int limit = Math.min(historyCount, halfmoveClock + 1);
        for (int i = 1; i <= limit; i++) {
            if (historyKeys[historyCount - i] == zobristKey) count++;
        }
        return count;
    }

    public boolean isThreefoldRepetition() {
        return repetitionCount() >= 3;
    }

    public boolean isFiftyMoveRule() {
        return halfmoveClock >= 100; // 100 half-moves = 50 full moves by each side
    }

    private void pushHistory(long key) {
        if (historyCount == historyKeys.length) {
            historyKeys = java.util.Arrays.copyOf(historyKeys, historyKeys.length * 2);
        }
        historyKeys[historyCount++] = key;
    }

    private void popHistory() {
        historyCount--;
    }

    private Undo undoStack(int index) {
        if (index >= undoStack.length) {
            int old = undoStack.length;
            undoStack = java.util.Arrays.copyOf(undoStack, old * 2);
            for (int i = old; i < undoStack.length; i++) undoStack[i] = new Undo();
        }
        return undoStack[index];
    }

    /** A clone with its own arrays — useful for analysis on a background thread. */
    public Board copy() {
        Board b = new Board();
        b.setFromFen(this.toFen());
        return b;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            sb.append(rank + 1).append(' ');
            for (int file = 0; file < 8; file++) {
                sb.append(Piece.toFenChar(squares[Square.of(file, rank)])).append(' ');
            }
            sb.append('\n');
        }
        sb.append("  a b c d e f g h\n");
        sb.append(Color.name(sideToMove)).append(" to move");
        return sb.toString();
    }
}
