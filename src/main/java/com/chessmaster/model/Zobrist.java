package com.chessmaster.model;

import java.util.Random;

/**
 * Zobrist hashing turns a board position into a single 64-bit number. Each
 * (piece, square) combination, each castling right, each en-passant file, and
 * the side to move gets a fixed random key; the position's hash is the XOR of
 * all the keys that apply. Because XOR is its own inverse, the hash can be
 * updated incrementally as pieces move — that is what makes the transposition
 * table and threefold-repetition detection fast.
 */
public final class Zobrist {

    /** [piece value 0..15][square 0..63]. Index 0 (empty) is left as zero/unused. */
    public static final long[][] PIECE = new long[16][64];
    /** Indexed by the 4-bit castling-rights mask (0..15). */
    public static final long[] CASTLING = new long[16];
    /** One key per file for the en-passant target square (its file is all that matters). */
    public static final long[] EN_PASSANT_FILE = new long[8];
    /** XOR'd in when it is black's turn. */
    public static final long SIDE_TO_MOVE;

    static {
        // Fixed seed -> reproducible keys across runs (handy for debugging & tests).
        Random r = new Random(0x5DEECE66DL ^ 0xC0FFEEL);
        for (int p = 1; p < 16; p++) {
            for (int sq = 0; sq < 64; sq++) {
                PIECE[p][sq] = r.nextLong();
            }
        }
        for (int i = 0; i < 16; i++) CASTLING[i] = r.nextLong();
        for (int i = 0; i < 8; i++) EN_PASSANT_FILE[i] = r.nextLong();
        SIDE_TO_MOVE = r.nextLong();
    }

    private Zobrist() {}
}
