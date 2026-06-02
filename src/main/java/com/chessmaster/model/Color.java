package com.chessmaster.model;

/**
 * Side to move / piece ownership. Stored as a plain int (0 = white, 1 = black)
 * because it is used as an array index in many hot paths inside the engine.
 */
public final class Color {

    private Color() {}

    public static final int WHITE = 0;
    public static final int BLACK = 1;

    /** @return the opposing color. */
    public static int opposite(int color) {
        return color ^ 1;
    }

    public static String name(int color) {
        return color == WHITE ? "White" : "Black";
    }
}
