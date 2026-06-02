package com.chessmaster.model;

/**
 * A move is packed into a single {@code int} so move lists are cheap to create
 * and store (important for a fast search). Layout:
 *
 * <pre>
 *   bits  0.. 5 : from square (0..63)
 *   bits  6..11 : to square   (0..63)
 *   bits 12..14 : flag        (see constants below)
 *   bits 15..17 : promotion piece type (KNIGHT..QUEEN), only when flag == PROMOTION
 * </pre>
 *
 * This class is a bag of static helpers; a "move" is just the int it returns.
 */
public final class Move {

    private Move() {}

    // ----- Move flags -----
    public static final int FLAG_NORMAL        = 0; // quiet move or ordinary capture
    public static final int FLAG_DOUBLE_PUSH   = 1; // pawn two squares forward
    public static final int FLAG_EN_PASSANT    = 2; // en-passant capture
    public static final int FLAG_CASTLE_KING   = 3; // O-O
    public static final int FLAG_CASTLE_QUEEN  = 4; // O-O-O
    public static final int FLAG_PROMOTION     = 5; // pawn promotion (capture or not)

    /** Sentinel for "no move". */
    public static final int NONE = 0;

    public static int make(int from, int to, int flag) {
        return from | (to << 6) | (flag << 12);
    }

    public static int makePromotion(int from, int to, int promoType) {
        return from | (to << 6) | (FLAG_PROMOTION << 12) | (promoType << 15);
    }

    public static int from(int move) {
        return move & 0x3F;
    }

    public static int to(int move) {
        return (move >> 6) & 0x3F;
    }

    public static int flag(int move) {
        return (move >> 12) & 0x7;
    }

    public static int promotionType(int move) {
        return (move >> 15) & 0x7;
    }

    public static boolean isPromotion(int move) {
        return flag(move) == FLAG_PROMOTION;
    }

    public static boolean isCastle(int move) {
        int f = flag(move);
        return f == FLAG_CASTLE_KING || f == FLAG_CASTLE_QUEEN;
    }

    public static boolean isEnPassant(int move) {
        return flag(move) == FLAG_EN_PASSANT;
    }

    /** Long-algebraic / UCI notation, e.g. "e2e4" or "e7e8q". */
    public static String toUci(int move) {
        if (move == NONE) return "0000";
        StringBuilder sb = new StringBuilder();
        sb.append(Square.name(from(move)));
        sb.append(Square.name(to(move)));
        if (isPromotion(move)) {
            sb.append(Character.toLowerCase(Piece.toFenChar(promotionType(move))));
        }
        return sb.toString();
    }
}
