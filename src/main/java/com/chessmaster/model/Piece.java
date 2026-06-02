package com.chessmaster.model;

/**
 * Pieces are stored on the board as small integers for speed. This class holds
 * the encoding constants and tiny helper methods to read them back out.
 *
 * <p>Encoding: a piece is {@code type | (colorBit)} where the color bit is 8 for
 * black. So:
 * <pre>
 *   empty = 0
 *   white: PAWN=1 KNIGHT=2 BISHOP=3 ROOK=4 QUEEN=5 KING=6
 *   black: PAWN=9 KNIGHT=10 BISHOP=11 ROOK=12 QUEEN=13 KING=14
 * </pre>
 * The "type" (1..6) lives in the low 3 bits; the color lives in bit 3.
 */
public final class Piece {

    private Piece() {}

    // ----- Piece types (1..6). 0 is reserved for "empty". -----
    public static final int NONE   = 0;
    public static final int PAWN   = 1;
    public static final int KNIGHT = 2;
    public static final int BISHOP = 3;
    public static final int ROOK   = 4;
    public static final int QUEEN  = 5;
    public static final int KING   = 6;

    /** Bit set on a piece value when it is black. */
    public static final int BLACK_FLAG = 8;

    // ----- Fully-encoded piece values, handy as named constants. -----
    public static final int WHITE_PAWN   = PAWN;
    public static final int WHITE_KNIGHT = KNIGHT;
    public static final int WHITE_BISHOP = BISHOP;
    public static final int WHITE_ROOK   = ROOK;
    public static final int WHITE_QUEEN  = QUEEN;
    public static final int WHITE_KING   = KING;

    public static final int BLACK_PAWN   = PAWN   | BLACK_FLAG;
    public static final int BLACK_KNIGHT = KNIGHT | BLACK_FLAG;
    public static final int BLACK_BISHOP = BISHOP | BLACK_FLAG;
    public static final int BLACK_ROOK   = ROOK   | BLACK_FLAG;
    public static final int BLACK_QUEEN  = QUEEN  | BLACK_FLAG;
    public static final int BLACK_KING   = KING   | BLACK_FLAG;

    /** @return the piece value for {@code type} owned by {@code color}. */
    public static int make(int color, int type) {
        return type | (color == Color.BLACK ? BLACK_FLAG : 0);
    }

    /** @return the type (1..6) of an encoded piece, or {@link #NONE} for empty. */
    public static int type(int piece) {
        return piece & 7;
    }

    /** @return {@link Color#WHITE} or {@link Color#BLACK} (undefined for empty squares). */
    public static int color(int piece) {
        return (piece & BLACK_FLAG) != 0 ? Color.BLACK : Color.WHITE;
    }

    public static boolean isWhite(int piece) {
        return piece != NONE && (piece & BLACK_FLAG) == 0;
    }

    public static boolean isBlack(int piece) {
        return (piece & BLACK_FLAG) != 0;
    }

    public static boolean isEmpty(int piece) {
        return piece == NONE;
    }

    public static boolean isSlider(int piece) {
        int t = type(piece);
        return t == BISHOP || t == ROOK || t == QUEEN;
    }

    /** Single-character symbol used by FEN: uppercase for white, lowercase for black. */
    public static char toFenChar(int piece) {
        char c = switch (type(piece)) {
            case PAWN   -> 'p';
            case KNIGHT -> 'n';
            case BISHOP -> 'b';
            case ROOK   -> 'r';
            case QUEEN  -> 'q';
            case KING   -> 'k';
            default     -> '.';
        };
        return isWhite(piece) ? Character.toUpperCase(c) : c;
    }

    /** Parse a FEN piece character (e.g. 'N' or 'q') into an encoded piece value. */
    public static int fromFenChar(char c) {
        int color = Character.isUpperCase(c) ? Color.WHITE : Color.BLACK;
        int type = switch (Character.toLowerCase(c)) {
            case 'p' -> PAWN;
            case 'n' -> KNIGHT;
            case 'b' -> BISHOP;
            case 'r' -> ROOK;
            case 'q' -> QUEEN;
            case 'k' -> KING;
            default  -> throw new IllegalArgumentException("Bad FEN piece char: " + c);
        };
        return make(color, type);
    }
}
