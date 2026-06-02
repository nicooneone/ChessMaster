package com.chessmaster.model;

import java.util.Arrays;

/**
 * A growable list of packed-int moves. Using a primitive {@code int[]} (instead
 * of {@code List<Integer>}) avoids boxing in the engine's hottest loops.
 */
public final class MoveList {

    private int[] moves;
    private int size;

    public MoveList() {
        this(64);
    }

    public MoveList(int capacity) {
        moves = new int[Math.max(8, capacity)];
        size = 0;
    }

    public void add(int move) {
        if (size == moves.length) {
            moves = Arrays.copyOf(moves, moves.length * 2);
        }
        moves[size++] = move;
    }

    public int get(int i) {
        return moves[i];
    }

    public void set(int i, int move) {
        moves[i] = move;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        size = 0;
    }

    /** Direct access to the backing array; only the first {@link #size()} entries are valid. */
    public int[] array() {
        return moves;
    }

    public boolean contains(int move) {
        for (int i = 0; i < size; i++) {
            if (moves[i] == move) return true;
        }
        return false;
    }
}
