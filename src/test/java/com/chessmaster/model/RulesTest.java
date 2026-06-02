package com.chessmaster.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RulesTest {

    private int findMove(Game game, String uci) {
        MoveList legal = game.legalMoves();
        for (int i = 0; i < legal.size(); i++) {
            if (Move.toUci(legal.get(i)).equals(uci)) return legal.get(i);
        }
        throw new AssertionError("No legal move " + uci + " in " + game.board().toFen());
    }

    @Test
    void foolsMateIsCheckmate() {
        Game g = new Game();
        assertTrue(g.play(findMove(g, "f2f3")));
        assertTrue(g.play(findMove(g, "e7e5")));
        assertTrue(g.play(findMove(g, "g2g4")));
        assertTrue(g.play(findMove(g, "d8h4"))); // Qh4#
        assertEquals(GameStatus.CHECKMATE, g.status());
        assertEquals("Qh4#", g.history().get(g.history().size() - 1).san());
    }

    @Test
    void stalemateIsDetected() {
        // Classic king+queen stalemate: black to move, no legal move, not in check.
        Game g = new Game("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        assertEquals(GameStatus.STALEMATE, g.status());
        assertTrue(g.status().isDraw());
    }

    @Test
    void insufficientMaterialKingVsKing() {
        Game g = new Game("8/8/4k3/8/8/3K4/8/8 w - - 0 1");
        assertEquals(GameStatus.DRAW_INSUFFICIENT, g.status());
    }

    @Test
    void promotionSanAndCheck() {
        Game g = new Game("8/P6k/8/8/8/8/8/6K1 w - - 0 1");
        assertTrue(g.play(findMove(g, "a7a8q")));
        assertEquals("a8=Q", g.history().get(0).san());
    }

    @Test
    void enPassantCapture() {
        Game g = new Game("rnbqkbnr/ppp1pppp/8/8/3pP3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        int ep = findMove(g, "d4e3");
        assertTrue(Move.isEnPassant(ep));
        assertTrue(g.play(ep));
        assertEquals(Piece.NONE, g.board().pieceAt(Square.parse("e4"))); // captured pawn gone
    }

    @Test
    void undoRestoresPosition() {
        Game g = new Game();
        String fen0 = g.board().toFen();
        g.play(findMove(g, "e2e4"));
        g.undo();
        assertEquals(fen0, g.board().toFen());
        assertTrue(g.history().isEmpty());
    }
}
