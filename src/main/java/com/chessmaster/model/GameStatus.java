package com.chessmaster.model;

/** The outcome/state of a game after the most recent move. */
public enum GameStatus {
    ONGOING,
    CHECKMATE,            // side to move is checkmated (the other side won)
    STALEMATE,            // side to move has no legal move but is not in check
    DRAW_FIFTY_MOVE,
    DRAW_REPETITION,
    DRAW_INSUFFICIENT;    // neither side has mating material

    public boolean isGameOver() {
        return this != ONGOING;
    }

    public boolean isDraw() {
        return this == STALEMATE || this == DRAW_FIFTY_MOVE
                || this == DRAW_REPETITION || this == DRAW_INSUFFICIENT;
    }
}
