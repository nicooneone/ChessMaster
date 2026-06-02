package com.chessmaster.training;

/**
 * A tactics puzzle: a starting position, a one-line goal, a theme, and a
 * difficulty. The "solution" is not hard-coded — the
 * {@link com.chessmaster.engine.Engine} validates the solver's moves and plays
 * the opponent's replies, so puzzles are always internally consistent and easy
 * to add (drop a FEN into {@link PuzzleBank}).
 */
public record Puzzle(String fen, String goal, Theme theme, Difficulty difficulty) {

    public enum Theme {
        MATE,           // forced checkmate
        WIN_MATERIAL    // win decisive material
    }

    public enum Difficulty {
        EASY("Easy"), MEDIUM("Medium"), HARD("Hard");

        public final String label;
        Difficulty(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
}
