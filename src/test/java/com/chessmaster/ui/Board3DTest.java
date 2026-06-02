package com.chessmaster.ui;

import com.chessmaster.model.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the 3D board's construction and its reconcile/animation path on the
 * real JavaFX thread. If the JavaFX toolkit can't start (e.g. a headless CI box),
 * the test is skipped rather than failed.
 */
class Board3DTest {

    @Test
    void buildsAndReconcilesWithoutError() throws Exception {
        CountDownLatch toolkitReady = new CountDownLatch(1);
        try {
            javafx.application.Platform.startup(toolkitReady::countDown);
        } catch (IllegalStateException alreadyStarted) {
            javafx.application.Platform.runLater(toolkitReady::countDown);
        } catch (Throwable noToolkit) {
            Assumptions.abort("JavaFX toolkit unavailable: " + noToolkit.getMessage());
        }
        Assumptions.assumeTrue(toolkitReady.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");

        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        javafx.application.Platform.runLater(() -> {
            try {
                Board board = Board.startPosition();
                Board3D view = new Board3D(board, Settings.get());
                view.refresh();                                   // initial snap-build

                // Make a real move on the same board instance, then reconcile (animates).
                int e4 = findUci(board, "e2e4");
                board.makeMove(e4);
                view.setLastMove(Move.from(e4), Move.to(e4));
                view.refresh();

                // Selection, hint, and flip should all be exception-free.
                view.setSelection(Square.parse("d7"), MoveGenerator.generateLegal(board));
                view.clearSelection();
                view.setHint(Square.parse("g1"), Square.parse("f3"));
                view.clearHint();
                view.setFlipped(true);

                assertNotNull(view.node());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });

        assertTrue(done.await(10, TimeUnit.SECONDS), "FX work did not finish");
        if (error.get() != null) fail("3D board threw: " + error.get(), error.get());
    }

    private static int findUci(Board board, String uci) {
        MoveList legal = MoveGenerator.generateLegal(board);
        for (int i = 0; i < legal.size(); i++) {
            if (Move.toUci(legal.get(i)).equals(uci)) return legal.get(i);
        }
        return Move.NONE;
    }
}
