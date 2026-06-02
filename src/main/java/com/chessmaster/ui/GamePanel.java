package com.chessmaster.ui;

import com.chessmaster.model.Game;

import java.util.List;

/**
 * The set of UI updates {@link GameController} pushes to whatever side panel is
 * showing. The play screen ({@link ControlPanel}) and the puzzles screen
 * ({@link PuzzlePanel}) each implement the parts they care about; the rest use
 * the no-op defaults.
 */
public interface GamePanel {
    void setStatus(String text);

    default void setGoal(String text) {}
    default void setWarning(String text) {}
    default void clearWarning() {}
    default void setThinking(boolean thinking) {}
    default void setEval(int whiteCp, int mateSigned, String bestLineSan) {}
    default void setMoveList(List<Game.PlayedMove> history) {}

    /** Called when a puzzle is solved (the puzzle panel uses this to count progress). */
    default void puzzleSolved() {}
}
