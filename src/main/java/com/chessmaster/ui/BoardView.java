package com.chessmaster.ui;

import com.chessmaster.model.Board;
import com.chessmaster.model.MoveList;
import javafx.scene.Node;

/**
 * The contract the {@link GameController} relies on for "a chess board the user
 * can see and click". Both the 2D {@link BoardCanvas} and the 3D {@link Board3D}
 * implement it, so the controller — and all the game/engine logic — is identical
 * regardless of which renderer is on screen.
 */
public interface BoardView {

    /** Reports user interaction in terms of board squares (0..63). */
    interface InputListener {
        void onPress(int square);
        void onRelease(int square);
    }

    void setListener(InputListener listener);

    /** Point the view at the position it should display. */
    void setBoard(Board board);

    void setFlipped(boolean flipped);
    boolean isFlipped();

    /** Highlight {@code square} as selected and show its legal destinations. */
    void setSelection(int square, MoveList legalFromSquare);
    void clearSelection();

    void setLastMove(int from, int to);

    void setHint(int from, int to);
    void clearHint();

    /** Re-sync the rendering with the current board state (and re-evaluate check). */
    void refresh();

    /** The JavaFX node to place in the scene. */
    Node node();
}
