package com.chessmaster.model;

/**
 * Squares are numbered 0..63. Square 0 is a1, 1 is b1, ... 7 is h1, 8 is a2, and
 * so on up to 63 = h8. So {@code file = sq & 7} (0=a..7=h) and
 * {@code rank = sq >> 3} (0=rank1..7=rank8).
 */
public final class Square {

    private Square() {}

    public static final int NONE = -1;

    public static int of(int file, int rank) {
        return rank * 8 + file;
    }

    public static int file(int sq) {
        return sq & 7;
    }

    public static int rank(int sq) {
        return sq >> 3;
    }

    public static boolean onBoard(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    /** Algebraic name such as "e4". */
    public static String name(int sq) {
        if (sq == NONE) return "-";
        return "" + (char) ('a' + file(sq)) + (char) ('1' + rank(sq));
    }

    /** Parse algebraic coordinates such as "e4" into a square index. */
    public static int parse(String s) {
        int file = s.charAt(0) - 'a';
        int rank = s.charAt(1) - '1';
        return of(file, rank);
    }
}
