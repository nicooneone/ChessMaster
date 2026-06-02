package com.chessmaster.engine;

import com.chessmaster.model.*;

import java.util.*;

/**
 * A small opening book. Rather than ship a big data file, we list a handful of
 * mainline opening sequences (in UCI move notation) and "replay" them from the
 * start position; every position visited along the way records the move that was
 * played from it. This automatically handles transpositions (two move orders
 * reaching the same position share book moves) and is trivial to extend — just
 * add lines to {@link #LINES}.
 */
public final class OpeningBook {

    // Each line is a space-separated sequence of UCI moves from the start position.
    private static final String[] LINES = {
            // --- 1.e4 e5 ---
            "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6 e1g1",   // Ruy Lopez
            "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 c2c3 g8f6",         // Italian, Giuoco Piano
            "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6 f3g5 d7d5",         // Two Knights
            "e2e4 e7e5 g1f3 b8c6 d2d4 e5d4 f3d4",              // Scotch
            "e2e4 e7e5 g1f3 g8f6 f3e5 d7d6 e5f3 f6e4",         // Petrov
            // --- Sicilian ---
            "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3",    // Najdorf setup
            "e2e4 c7c5 g1f3 b8c6 d2d4 c5d4 f3d4 g8f6 b1c3",    // Classical
            "e2e4 c7c5 b1c3 b8c6 g2g3",                         // Closed Sicilian
            // --- French / Caro-Kann ---
            "e2e4 e7e6 d2d4 d7d5 b1c3 g8f6",                    // French
            "e2e4 c7c6 d2d4 d7d5 b1c3 d5e4 c3e4",               // Caro-Kann
            // --- 1.d4 ---
            "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5",               // Queen's Gambit Declined
            "d2d4 d7d5 c2c4 c7c6 g1f3 g8f6 b1c3",               // Slav
            "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6",          // King's Indian
            "d2d4 g8f6 c2c4 e7e6 g1f3 b7b6",                    // Queen's Indian
            "d2d4 g8f6 c2c4 e7e6 b1c3 f8b4",                    // Nimzo-Indian
            // --- English / Reti ---
            "c2c4 e7e5 b1c3 g8f6 g1f3 b8c6",                    // English
            "g1f3 d7d5 g2g3 g8f6 f1g2",                         // Reti
    };

    /** position key -> list of book moves (as packed ints) playable from it. */
    private final Map<String, List<Integer>> book = new HashMap<>();
    private final Random random = new Random();

    public OpeningBook() {
        for (String line : LINES) addLine(line);
    }

    private void addLine(String line) {
        Board b = Board.startPosition();
        for (String uci : line.trim().split("\\s+")) {
            int move = findUci(b, uci);
            if (move == Move.NONE) break; // malformed line; stop replaying it
            String key = key(b);
            book.computeIfAbsent(key, k -> new ArrayList<>());
            if (!book.get(key).contains(move)) book.get(key).add(move);
            b.makeMove(move);
        }
    }

    /** @return a legal book move for this position, or {@link Move#NONE} if out of book. */
    public int probe(Board board) {
        List<Integer> moves = book.get(key(board));
        if (moves == null || moves.isEmpty()) return Move.NONE;
        // Only return moves that are actually legal in the given position.
        MoveList legal = MoveGenerator.generateLegal(board);
        List<Integer> playable = new ArrayList<>();
        for (int m : moves) if (legal.contains(m)) playable.add(m);
        if (playable.isEmpty()) return Move.NONE;
        return playable.get(random.nextInt(playable.size()));
    }

    public boolean hasMove(Board board) {
        return book.containsKey(key(board));
    }

    /** Position identity for book lookup: placement + side + castling rights. */
    private static String key(Board board) {
        String[] parts = board.toFen().split("\\s+");
        return parts[0] + " " + parts[1] + " " + parts[2];
    }

    private static int findUci(Board board, String uci) {
        MoveList legal = MoveGenerator.generateLegal(board);
        for (int i = 0; i < legal.size(); i++) {
            if (Move.toUci(legal.get(i)).equals(uci)) return legal.get(i);
        }
        return Move.NONE;
    }
}
