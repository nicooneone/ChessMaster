package com.chessmaster.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Move-generation correctness via perft. The expected node counts below are the
 * canonical published values (chessprogramming.org). If any of these fail, the
 * move generator, make/unmake, or check detection has a bug.
 */
class PerftTest {

    private long perft(String fen, int depth) {
        Board b = new Board();
        b.setFromFen(fen);
        return Perft.perft(b, depth);
    }

    @Test
    void startingPosition() {
        String fen = Board.START_FEN;
        assertEquals(20, perft(fen, 1));
        assertEquals(400, perft(fen, 2));
        assertEquals(8902, perft(fen, 3));
        assertEquals(197281, perft(fen, 4));
        assertEquals(4865609, perft(fen, 5));
    }

    @Test
    void kiwipete() {
        String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
        assertEquals(48, perft(fen, 1));
        assertEquals(2039, perft(fen, 2));
        assertEquals(97862, perft(fen, 3));
        assertEquals(4085603, perft(fen, 4));
    }

    @Test
    void position3() {
        String fen = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1";
        assertEquals(14, perft(fen, 1));
        assertEquals(191, perft(fen, 2));
        assertEquals(2812, perft(fen, 3));
        assertEquals(43238, perft(fen, 4));
        assertEquals(674624, perft(fen, 5));
    }

    @Test
    void position4() {
        String fen = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";
        assertEquals(6, perft(fen, 1));
        assertEquals(264, perft(fen, 2));
        assertEquals(9467, perft(fen, 3));
        assertEquals(422333, perft(fen, 4));
    }

    @Test
    void position5() {
        String fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        assertEquals(44, perft(fen, 1));
        assertEquals(1486, perft(fen, 2));
        assertEquals(62379, perft(fen, 3));
        assertEquals(2103487, perft(fen, 4));
    }

    @Test
    void zobristIsIncrementallyConsistent() {
        // After making and un-making moves, the incrementally-updated hash must
        // always match a from-scratch recomputation.
        Board b = new Board();
        b.setFromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        MoveList moves = MoveGenerator.generateLegal(b);
        for (int i = 0; i < moves.size(); i++) {
            b.makeMove(moves.get(i));
            assertEquals(b.computeZobrist(), b.zobristKey(), "hash mismatch after move");
            b.unmakeMove();
        }
    }
}
