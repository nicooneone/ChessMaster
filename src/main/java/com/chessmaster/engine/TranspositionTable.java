package com.chessmaster.engine;

/**
 * A transposition table: a big hash map from a position's {@link
 * com.chessmaster.model.Zobrist} key to the result of searching it. Different
 * move orders often reach the same position ("transpositions"), so caching the
 * search result avoids re-doing the work and dramatically deepens the search.
 *
 * <p>Entries are stored in flat arrays indexed by {@code key & mask} (the table
 * size is a power of two). We use a simple always-replace scheme.
 */
public final class TranspositionTable {

    /** Bound types: was the stored score exact, or just a lower/upper bound? */
    public static final byte FLAG_EXACT = 0;
    public static final byte FLAG_LOWER = 1; // score is a lower bound (fail-high / beta cutoff)
    public static final byte FLAG_UPPER = 2; // score is an upper bound (fail-low)

    private final long[] keys;
    private final int[] moves;
    private final short[] depths;
    private final int[] scores;
    private final byte[] flags;
    private final int mask;

    /** @param sizeMb approximate table size in megabytes. */
    public TranspositionTable(int sizeMb) {
        // Each slot is ~ 8+4+2+4+1 = 19 bytes; round the slot count down to a power of two.
        long bytes = (long) sizeMb * 1024 * 1024;
        int slots = Integer.highestOneBit((int) Math.max(1024, bytes / 19));
        keys = new long[slots];
        moves = new int[slots];
        depths = new short[slots];
        scores = new int[slots];
        flags = new byte[slots];
        mask = slots - 1;
    }

    public void clear() {
        java.util.Arrays.fill(keys, 0L);
    }

    /** A probe result; {@code hit} is false when nothing usable was stored. */
    public static final class Entry {
        public boolean hit;
        public int move;
        public int depth;
        public int score;
        public byte flag;
    }

    public void probe(long key, Entry out) {
        int i = (int) (key & mask);
        if (keys[i] == key) {
            out.hit = true;
            out.move = moves[i];
            out.depth = depths[i];
            out.score = scores[i];
            out.flag = flags[i];
        } else {
            out.hit = false;
            out.move = 0;
        }
    }

    public void store(long key, int move, int depth, int score, byte flag) {
        int i = (int) (key & mask);
        // Prefer to keep a deeper analysis if it's for the same position.
        if (keys[i] == key && depths[i] > depth && flag != FLAG_EXACT) return;
        keys[i] = key;
        moves[i] = move;
        depths[i] = (short) depth;
        scores[i] = score;
        flags[i] = flag;
    }
}
