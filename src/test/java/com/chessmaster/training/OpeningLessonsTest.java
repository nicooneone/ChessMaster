package com.chessmaster.training;

import com.chessmaster.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Every guided lesson must be a fully legal sequence of moves from the start. */
class OpeningLessonsTest {

    @Test
    void everyLessonIsLegal() {
        for (OpeningLessons.Lesson lesson : OpeningLessons.all()) {
            Board b = Board.startPosition();
            for (OpeningLessons.Step step : lesson.steps()) {
                int move = findUci(b, step.uci());
                assertNotEquals(Move.NONE, move,
                        "Illegal move " + step.uci() + " in lesson '" + lesson.name() + "' at " + b.toFen());
                b.makeMove(move);
            }
        }
    }

    private int findUci(Board board, String uci) {
        MoveList legal = MoveGenerator.generateLegal(board);
        for (int i = 0; i < legal.size(); i++) {
            if (Move.toUci(legal.get(i)).equals(uci)) return legal.get(i);
        }
        return Move.NONE;
    }
}
