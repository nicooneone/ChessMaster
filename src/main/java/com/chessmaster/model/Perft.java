package com.chessmaster.model;

/**
 * "Perft" (performance test) counts the number of leaf nodes reachable in
 * exactly {@code depth} plies. Because the correct counts for many positions are
 * published and well-known, perft is the gold-standard way to prove a move
 * generator is bug-free. It is also a handy raw-speed benchmark.
 */
public final class Perft {

    private Perft() {}

    public static long perft(Board board, int depth) {
        if (depth == 0) return 1;
        MoveList moves = MoveGenerator.generateLegal(board);
        if (depth == 1) return moves.size();
        long nodes = 0;
        for (int i = 0; i < moves.size(); i++) {
            board.makeMove(moves.get(i));
            nodes += perft(board, depth - 1);
            board.unmakeMove();
        }
        return nodes;
    }

    /** Per-root-move breakdown ("divide"), the usual way to localize a perft bug. */
    public static void divide(Board board, int depth) {
        MoveList moves = MoveGenerator.generateLegal(board);
        long total = 0;
        for (int i = 0; i < moves.size(); i++) {
            int m = moves.get(i);
            board.makeMove(m);
            long n = perft(board, depth - 1);
            board.unmakeMove();
            total += n;
            System.out.println(Move.toUci(m) + ": " + n);
        }
        System.out.println("Total: " + total);
    }
}
