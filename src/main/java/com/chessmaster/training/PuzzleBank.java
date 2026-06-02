package com.chessmaster.training;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.chessmaster.training.Puzzle.Difficulty;
import com.chessmaster.training.Puzzle.Theme;

/**
 * The built-in set of tactics puzzles, grouped by difficulty. Each is a legal
 * position where the side to move has a clear, decisive resource. The engine
 * validates the solver's moves at run time, and {@code PuzzleBankTest} confirms
 * every position here is actually winning, so the set stays sound as it grows.
 */
public final class PuzzleBank {

    private final List<Puzzle> puzzles = new ArrayList<>();
    private final Random random = new Random();

    public PuzzleBank() {
        // ===================== EASY =====================
        // Mate in one (back-rank, ladder, and queen patterns).
        add("6k1/5ppp/8/8/8/8/8/R6K w - - 0 1",        "White to move — mate in 1.",            Theme.MATE, Difficulty.EASY);
        add("6k1/5ppp/8/8/8/8/8/3R3K w - - 0 1",       "White to move — mate in 1.",            Theme.MATE, Difficulty.EASY);
        add("7k/5ppp/8/8/8/8/8/1R5K w - - 0 1",        "White to move — back-rank mate in 1.",  Theme.MATE, Difficulty.EASY);
        add("7k/6pp/8/8/8/8/8/5R1K w - - 0 1",         "White to move — mate in 1.",            Theme.MATE, Difficulty.EASY);
        add("k7/7R/1R6/8/8/8/8/6K1 w - - 0 1",         "White to move — ladder mate in 1.",     Theme.MATE, Difficulty.EASY);
        add("7k/5Q2/6K1/8/8/8/8/8 w - - 0 1",          "White to move — mate the lone king.",   Theme.MATE, Difficulty.EASY);
        // Win a hanging piece.
        add("4k3/8/8/4q3/8/8/8/4R1K1 w - - 0 1",       "White to move — win the queen.",        Theme.WIN_MATERIAL, Difficulty.EASY);
        add("4k3/8/8/4q3/8/8/8/4RK2 w - - 0 1",        "White to move — win the queen.",        Theme.WIN_MATERIAL, Difficulty.EASY);
        add("r3k3/8/8/8/8/8/8/R3K3 w - - 0 1",         "White to move — win the rook.",         Theme.WIN_MATERIAL, Difficulty.EASY);
        add("2q1k3/8/8/8/8/8/8/2R1K3 w - - 0 1",       "White to move — win the queen.",        Theme.WIN_MATERIAL, Difficulty.EASY);
        add("4k3/8/8/3n4/8/8/8/3RK3 w - - 0 1",        "White to move — win the knight.",       Theme.WIN_MATERIAL, Difficulty.EASY);
        add("4k3/8/8/2b5/8/8/8/2R1K3 w - - 0 1",       "White to move — win the bishop.",       Theme.WIN_MATERIAL, Difficulty.EASY);

        // ===================== MEDIUM =====================
        // Rook skewers: check the king on a rank, win the piece behind it.
        add("r3k3/8/8/8/8/8/8/4K2R w - - 0 1",         "White to move — skewer to win the rook.",  Theme.WIN_MATERIAL, Difficulty.MEDIUM);
        add("4k2r/8/8/8/8/8/8/R3K3 w - - 0 1",         "White to move — skewer to win the rook.",  Theme.WIN_MATERIAL, Difficulty.MEDIUM);
        add("q3k3/8/8/8/8/8/8/4K2R w - - 0 1",         "White to move — skewer to win the queen.", Theme.WIN_MATERIAL, Difficulty.MEDIUM);
        add("4k2q/8/8/8/8/8/8/R3K3 w - - 0 1",         "White to move — skewer to win the queen.", Theme.WIN_MATERIAL, Difficulty.MEDIUM);
        // Win the queen for a bishop with a pin against the king.
        add("7k/6q1/8/8/3B4/8/PPPP4/6K1 w - - 0 1",    "White to move — win the queen.",           Theme.WIN_MATERIAL, Difficulty.MEDIUM);

        // ===================== HARD =====================
        // Two-rook ladder mates that begin with a quiet (non-checking) move.
        add("7k/8/8/8/8/8/1R6/R6K w - - 0 1",          "White to move — mate in 2 (find the quiet rook move).", Theme.MATE, Difficulty.HARD);
        add("7k/8/8/8/8/8/3R4/R6K w - - 0 1",          "White to move — mate in 2.",                            Theme.MATE, Difficulty.HARD);
        // King-and-queen mate in two.
        add("7k/8/5K2/8/8/8/8/1Q6 w - - 0 1",          "White to move — mate in 2 with the queen.",             Theme.MATE, Difficulty.HARD);
    }

    private void add(String fen, String goal, Theme theme, Difficulty difficulty) {
        puzzles.add(new Puzzle(fen, goal, theme, difficulty));
    }

    public List<Puzzle> all() {
        return puzzles;
    }

    public List<Puzzle> byDifficulty(Difficulty difficulty) {
        if (difficulty == null) return puzzles;
        List<Puzzle> out = new ArrayList<>();
        for (Puzzle p : puzzles) if (p.difficulty() == difficulty) out.add(p);
        return out;
    }

    /** A random puzzle of the given difficulty (or any difficulty if {@code null}). */
    public Puzzle random(Difficulty difficulty) {
        List<Puzzle> pool = byDifficulty(difficulty);
        if (pool.isEmpty()) pool = puzzles;
        return pool.get(random.nextInt(pool.size()));
    }
}
