package com.chessmaster.model;

/**
 * Generates moves for a {@link Board}. The public entry point
 * {@link #generateLegal(Board, MoveList)} produces only fully-legal moves: it
 * builds the pseudo-legal set first, then discards any move that would leave the
 * mover's king in check (by actually making and un-making it). That is the
 * simplest provably-correct strategy, and it is fast enough for a strong
 * club-level engine.
 */
public final class MoveGenerator {

    private MoveGenerator() {}

    private static final int[][] KNIGHT_DELTAS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };
    private static final int[][] KING_DELTAS = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
    };
    private static final int[][] BISHOP_DIRS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] ROOK_DIRS   = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[] PROMOTION_TYPES = {Piece.QUEEN, Piece.ROOK, Piece.BISHOP, Piece.KNIGHT};

    /** Fill {@code out} with every legal move for the side to move. */
    public static void generateLegal(Board board, MoveList out) {
        MoveList pseudo = new MoveList();
        generatePseudoLegal(board, pseudo);
        int us = board.sideToMove();
        for (int i = 0; i < pseudo.size(); i++) {
            int move = pseudo.get(i);
            board.makeMove(move);
            if (!board.inCheck(us)) out.add(move);
            board.unmakeMove();
        }
    }

    /** Convenience wrapper returning a fresh list. */
    public static MoveList generateLegal(Board board) {
        MoveList list = new MoveList();
        generateLegal(board, list);
        return list;
    }

    /**
     * Generate captures (and promotions) only — used by the engine's quiescence
     * search. These are still pseudo-legal; the caller filters by king safety.
     */
    public static void generateCaptures(Board board, MoveList out) {
        MoveList pseudo = new MoveList();
        generatePseudoLegal(board, pseudo);
        int us = board.sideToMove();
        for (int i = 0; i < pseudo.size(); i++) {
            int move = pseudo.get(i);
            int to = Move.to(move);
            boolean isCapture = board.pieceAt(to) != Piece.NONE || Move.isEnPassant(move);
            boolean isPromo = Move.isPromotion(move);
            if (!isCapture && !isPromo) continue;
            board.makeMove(move);
            if (!board.inCheck(us)) out.add(move);
            board.unmakeMove();
        }
    }

    public static void generatePseudoLegal(Board board, MoveList out) {
        int us = board.sideToMove();
        for (int sq = 0; sq < 64; sq++) {
            int p = board.pieceAt(sq);
            if (p == Piece.NONE || Piece.color(p) != us) continue;
            switch (Piece.type(p)) {
                case Piece.PAWN   -> genPawn(board, sq, us, out);
                case Piece.KNIGHT -> genJumper(board, sq, us, KNIGHT_DELTAS, out);
                case Piece.KING   -> { genJumper(board, sq, us, KING_DELTAS, out); genCastles(board, sq, us, out); }
                case Piece.BISHOP -> genSlider(board, sq, us, BISHOP_DIRS, out);
                case Piece.ROOK   -> genSlider(board, sq, us, ROOK_DIRS, out);
                case Piece.QUEEN  -> { genSlider(board, sq, us, BISHOP_DIRS, out); genSlider(board, sq, us, ROOK_DIRS, out); }
                default           -> { /* impossible */ }
            }
        }
    }

    private static void genPawn(Board board, int sq, int us, MoveList out) {
        int file = Square.file(sq);
        int rank = Square.rank(sq);
        int dir = (us == Color.WHITE) ? 1 : -1;       // rank direction
        int startRank = (us == Color.WHITE) ? 1 : 6;
        int promoRank = (us == Color.WHITE) ? 7 : 0;

        // Single push.
        int oneRank = rank + dir;
        if (oneRank >= 0 && oneRank < 8) {
            int oneSq = Square.of(file, oneRank);
            if (board.pieceAt(oneSq) == Piece.NONE) {
                if (oneRank == promoRank) addPromotions(sq, oneSq, out);
                else out.add(Move.make(sq, oneSq, Move.FLAG_NORMAL));

                // Double push (only from the start rank, both squares empty).
                if (rank == startRank) {
                    int twoSq = Square.of(file, rank + 2 * dir);
                    if (board.pieceAt(twoSq) == Piece.NONE) {
                        out.add(Move.make(sq, twoSq, Move.FLAG_DOUBLE_PUSH));
                    }
                }
            }
        }

        // Captures (left and right diagonals), including en passant.
        for (int df = -1; df <= 1; df += 2) {
            int nf = file + df;
            int nr = rank + dir;
            if (!Square.onBoard(nf, nr)) continue;
            int target = Square.of(nf, nr);
            int tp = board.pieceAt(target);
            if (tp != Piece.NONE && Piece.color(tp) != us) {
                if (nr == promoRank) addPromotions(sq, target, out);
                else out.add(Move.make(sq, target, Move.FLAG_NORMAL));
            } else if (target == board.enPassantSquare() && tp == Piece.NONE) {
                out.add(Move.make(sq, target, Move.FLAG_EN_PASSANT));
            }
        }
    }

    private static void addPromotions(int from, int to, MoveList out) {
        for (int t : PROMOTION_TYPES) out.add(Move.makePromotion(from, to, t));
    }

    private static void genJumper(Board board, int sq, int us, int[][] deltas, MoveList out) {
        int file = Square.file(sq);
        int rank = Square.rank(sq);
        for (int[] d : deltas) {
            int nf = file + d[0], nr = rank + d[1];
            if (!Square.onBoard(nf, nr)) continue;
            int target = Square.of(nf, nr);
            int tp = board.pieceAt(target);
            if (tp == Piece.NONE || Piece.color(tp) != us) {
                out.add(Move.make(sq, target, Move.FLAG_NORMAL));
            }
        }
    }

    private static void genSlider(Board board, int sq, int us, int[][] dirs, MoveList out) {
        int file = Square.file(sq);
        int rank = Square.rank(sq);
        for (int[] d : dirs) {
            int nf = file + d[0], nr = rank + d[1];
            while (Square.onBoard(nf, nr)) {
                int target = Square.of(nf, nr);
                int tp = board.pieceAt(target);
                if (tp == Piece.NONE) {
                    out.add(Move.make(sq, target, Move.FLAG_NORMAL));
                } else {
                    if (Piece.color(tp) != us) out.add(Move.make(sq, target, Move.FLAG_NORMAL));
                    break;
                }
                nf += d[0];
                nr += d[1];
            }
        }
    }

    private static void genCastles(Board board, int kingSq, int us, MoveList out) {
        int them = Color.opposite(us);
        // Castling is illegal while in check.
        if (board.isSquareAttacked(kingSq, them)) return;

        if (us == Color.WHITE) {
            if (board.canCastle(Board.CASTLE_WK)
                    && empty(board, 5) && empty(board, 6)
                    && !board.isSquareAttacked(5, them) && !board.isSquareAttacked(6, them)) {
                out.add(Move.make(4, 6, Move.FLAG_CASTLE_KING));
            }
            if (board.canCastle(Board.CASTLE_WQ)
                    && empty(board, 3) && empty(board, 2) && empty(board, 1)
                    && !board.isSquareAttacked(3, them) && !board.isSquareAttacked(2, them)) {
                out.add(Move.make(4, 2, Move.FLAG_CASTLE_QUEEN));
            }
        } else {
            if (board.canCastle(Board.CASTLE_BK)
                    && empty(board, 61) && empty(board, 62)
                    && !board.isSquareAttacked(61, them) && !board.isSquareAttacked(62, them)) {
                out.add(Move.make(60, 62, Move.FLAG_CASTLE_KING));
            }
            if (board.canCastle(Board.CASTLE_BQ)
                    && empty(board, 59) && empty(board, 58) && empty(board, 57)
                    && !board.isSquareAttacked(59, them) && !board.isSquareAttacked(58, them)) {
                out.add(Move.make(60, 58, Move.FLAG_CASTLE_QUEEN));
            }
        }
    }

    private static boolean empty(Board board, int sq) {
        return board.pieceAt(sq) == Piece.NONE;
    }
}
