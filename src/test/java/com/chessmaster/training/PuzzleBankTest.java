package com.chessmaster.training;

import com.chessmaster.engine.Engine;
import com.chessmaster.engine.Search;
import com.chessmaster.model.Board;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Every built-in puzzle must be sound: the side to move really does have the
 * advertised decisive resource. We let the engine confirm it.
 */
class PuzzleBankTest {

    @Test
    void allPuzzlesAreSound() {
        Engine engine = new Engine();
        engine.setUseBook(false);
        for (Puzzle p : new PuzzleBank().all()) {
            Board b = new Board();
            b.setFromFen(p.fen());
            Search.Result r = engine.analyze(b, Search.Limits.depth(6));
            if (p.theme() == Puzzle.Theme.MATE) {
                assertTrue(r.isMate() && r.mateIn() > 0,
                        "Expected a forced mate for: " + p.fen() + " but score=" + r.score);
            } else {
                assertTrue(r.score >= 300,
                        "Expected a clearly winning position for: " + p.fen() + " but score=" + r.score);
            }
        }
    }
}
