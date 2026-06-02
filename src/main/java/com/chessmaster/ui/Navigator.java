package com.chessmaster.ui;

/** Screen navigation, implemented by {@link ChessApp} and used by each screen. */
public interface Navigator {
    void showMenu();
    void showPlay();
    void showLearn();
    void showPuzzles();
    void showSettings();
    void showCredits();
    void quit();
}
