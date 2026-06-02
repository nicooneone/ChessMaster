package com.chessmaster.ui;

import com.chessmaster.model.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.VPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Renders a chess board and reports user clicks/drags as square indices. It is a
 * "dumb" view: it draws whatever state the {@link GameController} pushes into it
 * (the position, the selected square, legal-move dots, last-move and check
 * highlights, and an optional hint arrow) and forwards input via a listener.
 */
public final class BoardCanvas extends Canvas implements BoardView {

    // Board colors (chess.com-style green theme).
    private static final Color LIGHT      = Color.web("#EEEED2");
    private static final Color DARK       = Color.web("#769656");
    private static final Color LAST_MOVE  = Color.web("#F6F669", 0.55);
    private static final Color SELECTED   = Color.web("#F6F669", 0.75);
    private static final Color LEGAL_DOT  = Color.web("#202020", 0.22);
    private static final Color CHECK_GLOW = Color.web("#E54B4B", 0.85);
    private static final Color HINT       = Color.web("#FF8C00", 0.85);
    private static final Color COORD_LIGHT= Color.web("#EEEED2");
    private static final Color COORD_DARK = Color.web("#769656");

    private double squareSize = 80;
    private boolean flipped = false;

    private Board board;
    private int selected = Square.NONE;
    private final Set<Integer> legalTargets = new HashSet<>();
    private int lastFrom = Square.NONE, lastTo = Square.NONE;
    private int checkSquare = Square.NONE;
    private int hintFrom = Square.NONE, hintTo = Square.NONE;

    private BoardView.InputListener listener;

    public BoardCanvas(Board board) {
        this.board = board;
        setWidth(squareSize * 8);
        setHeight(squareSize * 8);
        setOnMousePressed(e -> { if (listener != null) listener.onPress(pixelToSquare(e.getX(), e.getY())); });
        setOnMouseReleased(e -> { if (listener != null) listener.onRelease(pixelToSquare(e.getX(), e.getY())); });
        draw();
    }

    @Override
    public void setListener(BoardView.InputListener listener) {
        this.listener = listener;
    }

    @Override
    public javafx.scene.Node node() {
        return this;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        draw();
    }

    public boolean isFlipped() {
        return flipped;
    }

    public void setSelection(int square, MoveList legalFromSquare) {
        this.selected = square;
        legalTargets.clear();
        if (legalFromSquare != null) {
            for (int i = 0; i < legalFromSquare.size(); i++) {
                legalTargets.add(Move.to(legalFromSquare.get(i)));
            }
        }
        draw();
    }

    public void clearSelection() {
        selected = Square.NONE;
        legalTargets.clear();
        draw();
    }

    public void setLastMove(int from, int to) {
        this.lastFrom = from;
        this.lastTo = to;
    }

    public void setHint(int from, int to) {
        this.hintFrom = from;
        this.hintTo = to;
        draw();
    }

    public void clearHint() {
        hintFrom = Square.NONE;
        hintTo = Square.NONE;
        draw();
    }

    public void refresh() {
        // Recompute the king-in-check highlight, then redraw.
        checkSquare = Square.NONE;
        if (board != null) {
            int stm = board.sideToMove();
            if (board.inCheck(stm)) checkSquare = board.kingSquare(stm);
        }
        draw();
    }

    // ------------------------------------------------------------------
    //  Drawing
    // ------------------------------------------------------------------

    public void draw() {
        GraphicsContext g = getGraphicsContext2D();
        double s = squareSize;
        g.clearRect(0, 0, getWidth(), getHeight());

        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                int sq = Square.of(file, rank);
                double x = colOf(file) * s;
                double y = rowOf(rank) * s;

                boolean light = ((file + rank) & 1) == 1;
                g.setFill(light ? LIGHT : DARK);
                g.fillRect(x, y, s, s);

                if (sq == lastFrom || sq == lastTo) { g.setFill(LAST_MOVE); g.fillRect(x, y, s, s); }
                if (sq == selected)                 { g.setFill(SELECTED);  g.fillRect(x, y, s, s); }
                if (sq == checkSquare)              { drawCheckGlow(g, x, y, s); }

                drawCoordinate(g, file, rank, x, y, s, light);
            }
        }

        // Legal-move indicators: a dot on empty squares, a ring on capturable ones.
        for (int target : legalTargets) {
            double x = colOf(Square.file(target)) * s;
            double y = rowOf(Square.rank(target)) * s;
            g.setFill(LEGAL_DOT);
            if (board.pieceAt(target) == Piece.NONE && !isEnPassantTarget(target)) {
                double r = s * 0.16;
                g.fillOval(x + s / 2 - r, y + s / 2 - r, r * 2, r * 2);
            } else {
                g.setStroke(LEGAL_DOT);
                g.setLineWidth(s * 0.08);
                double pad = s * 0.06;
                g.strokeOval(x + pad, y + pad, s - 2 * pad, s - 2 * pad);
            }
        }

        drawPieces(g, s);
        if (hintFrom != Square.NONE && hintTo != Square.NONE) drawArrow(g, hintFrom, hintTo, s);
    }

    private void drawPieces(GraphicsContext g, double s) {
        Font font = Font.font("Sans Serif", FontWeight.NORMAL, s * 0.80);
        g.setFont(font);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setLineWidth(Math.max(1.0, s * 0.02));
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                int p = board.pieceAt(Square.of(file, rank));
                if (p == Piece.NONE) continue;
                String glyph = glyph(Piece.type(p));
                double cx = colOf(file) * s + s / 2;
                double cy = rowOf(rank) * s + s / 2;
                if (Piece.isWhite(p)) {
                    g.setFill(Color.WHITE);
                    g.setStroke(Color.web("#333333"));
                } else {
                    g.setFill(Color.web("#202020"));
                    g.setStroke(Color.web("#dddddd"));
                }
                g.fillText(glyph, cx, cy);
                g.strokeText(glyph, cx, cy);
            }
        }
    }

    private void drawCheckGlow(GraphicsContext g, double x, double y, double s) {
        g.setFill(CHECK_GLOW);
        g.fillRect(x, y, s, s);
    }

    private void drawCoordinate(GraphicsContext g, int file, int rank, double x, double y, double s, boolean light) {
        g.setFont(Font.font("Sans Serif", FontWeight.BOLD, s * 0.16));
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);
        g.setFill(light ? COORD_DARK : COORD_LIGHT);
        // Rank numbers down the left edge, file letters along the bottom edge.
        boolean leftEdge = (colOf(file) == 0);
        boolean bottomEdge = (rowOf(rank) == 7);
        if (leftEdge) g.fillText(String.valueOf(rank + 1), x + s * 0.05, y + s * 0.04);
        if (bottomEdge) {
            g.setTextAlign(TextAlignment.RIGHT);
            g.setTextBaseline(VPos.BOTTOM);
            g.fillText(String.valueOf((char) ('a' + file)), x + s * 0.95, y + s * 0.97);
        }
    }

    private void drawArrow(GraphicsContext g, int from, int to, double s) {
        double x1 = colOf(Square.file(from)) * s + s / 2;
        double y1 = rowOf(Square.rank(from)) * s + s / 2;
        double x2 = colOf(Square.file(to)) * s + s / 2;
        double y2 = rowOf(Square.rank(to)) * s + s / 2;
        g.setStroke(HINT);
        g.setFill(HINT);
        g.setLineWidth(s * 0.14);
        // Shorten the line so the arrowhead sits nicely on the target square.
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double headLen = s * 0.32;
        double ex = x2 - Math.cos(angle) * headLen * 0.6;
        double ey = y2 - Math.sin(angle) * headLen * 0.6;
        g.strokeLine(x1, y1, ex, ey);
        double a1 = angle + Math.toRadians(150);
        double a2 = angle - Math.toRadians(150);
        g.fillPolygon(
                new double[]{x2, x2 + Math.cos(a1) * headLen, x2 + Math.cos(a2) * headLen},
                new double[]{y2, y2 + Math.sin(a1) * headLen, y2 + Math.sin(a2) * headLen}, 3);
    }

    // ------------------------------------------------------------------
    //  Geometry & glyphs
    // ------------------------------------------------------------------

    /** Column drawn for a given file, honoring board flip. */
    private int colOf(int file) { return flipped ? 7 - file : file; }
    /** Row drawn for a given rank (rank 0 is at the bottom when not flipped). */
    private int rowOf(int rank) { return flipped ? rank : 7 - rank; }

    private int pixelToSquare(double px, double py) {
        int col = (int) (px / squareSize);
        int row = (int) (py / squareSize);
        col = Math.max(0, Math.min(7, col));
        row = Math.max(0, Math.min(7, row));
        int file = flipped ? 7 - col : col;
        int rank = flipped ? row : 7 - row;
        return Square.of(file, rank);
    }

    private boolean isEnPassantTarget(int sq) {
        return board.enPassantSquare() == sq;
    }

    private static String glyph(int type) {
        return switch (type) {
            case Piece.KING   -> "♚";
            case Piece.QUEEN  -> "♛";
            case Piece.ROOK   -> "♜";
            case Piece.BISHOP -> "♝";
            case Piece.KNIGHT -> "♞";
            case Piece.PAWN   -> "♟";
            default           -> "";
        };
    }
}
