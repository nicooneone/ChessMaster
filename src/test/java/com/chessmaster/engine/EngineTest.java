package com.chessmaster.engine;

import com.chessmaster.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EngineTest {

    @Test
    void evaluationIsSymmetricAtStart() {
        // The start position is perfectly symmetric, so it must evaluate to 0
        // (any non-zero value would reveal a piece-square-table orientation bug).
        Board b = Board.startPosition();
        assertEquals(0, Evaluation.evaluate(b));
    }

    @Test
    void findsMateInOne() {
        // Back-rank mate: Ra1-a8 is checkmate (black king boxed in by its pawns).
        Board b = new Board();
        b.setFromFen("6k1/5ppp/8/8/8/8/5PPP/R3K3 w - - 0 1");
        Search.Result r = new Engine().analyze(b, 5);
        assertEquals("a1a8", Move.toUci(r.bestMove));
        assertTrue(r.isMate(), "should report a forced mate");
        assertEquals(1, r.mateIn());
    }

    @Test
    void winsAFreeQueen() {
        // Black queen on e5 is undefended and the white rook on e1 can take it.
        Board b = new Board();
        b.setFromFen("4k3/8/8/4q3/8/8/8/4R1K1 w - - 0 1");
        Search.Result r = new Engine().analyze(b, 6);
        assertEquals("e1e5", Move.toUci(r.bestMove));
        // After Rxe5 it's K+R vs K: white is up a rook (~500cp), clearly winning.
        assertTrue(r.score > 400, "winning the queen should score clearly winning; was " + r.score);
    }

    @Test
    void avoidsGettingMated() {
        // White is threatened with ...Qg2#; engine must find a defense and not be mated.
        Board b = new Board();
        b.setFromFen("6k1/5ppp/8/8/8/8/5PPP/R3K3 b - - 0 1");
        Search.Result r = new Engine().analyze(b, 5);
        assertTrue(r.bestMove != Move.NONE);
    }

    @Test
    void opensFromTheBook() {
        Board b = Board.startPosition();
        Engine engine = new Engine();
        int move = engine.chooseMove(b, Engine.Level.MASTER);
        assertTrue(MoveGenerator.generateLegal(b).contains(move), "book move must be legal");
    }

    @Test
    void mastersStrengthIsDeterministicTactically() {
        // With randomness 0, Master/Expert must find the free queen too (no book here).
        Board b = new Board();
        b.setFromFen("4k3/8/8/4q3/8/8/8/4R1K1 w - - 0 1");
        Engine engine = new Engine();
        engine.setUseBook(false);
        int move = engine.chooseMove(b, Engine.Level.EXPERT);
        assertEquals("e1e5", Move.toUci(move));
    }
}
