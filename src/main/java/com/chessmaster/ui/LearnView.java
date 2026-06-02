package com.chessmaster.ui;

import com.chessmaster.model.*;
import com.chessmaster.training.OpeningLessons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Guided opening trainer. The learner is walked through an opening one move at a
 * time: the next move is highlighted on the 3D board (orange) with an explanation
 * of the idea, and only that move advances the lesson. It reuses the same 3D
 * board and the same click-to-move interaction as the Play screen.
 */
public final class LearnView implements BoardView.InputListener {

    private final Navigator nav;
    private final Board3D board;

    private Game game = new Game();
    private OpeningLessons.Lesson lesson;
    private int step = 0;
    private int selected = Square.NONE;

    private final ComboBox<OpeningLessons.Lesson> lessonBox = new ComboBox<>();
    private final Label titleLabel = new Label();
    private final Label descLabel = new Label();
    private final Label progressLabel = new Label();
    private final Label explanationLabel = new Label();

    private final BorderPane root = new BorderPane();

    public LearnView(Navigator nav) {
        this.nav = nav;
        this.board = new Board3D(Board.startPosition(), Settings.get());
        this.board.setListener(this);

        root.setTop(topBar());
        root.setCenter(board.node());
        root.setRight(sidePanel());

        loadLesson(OpeningLessons.all().get(0));
    }

    public Parent getView() { return root; }

    public void dispose() { /* no background threads to release */ }

    // ------------------------------------------------------------------
    //  Lesson flow
    // ------------------------------------------------------------------

    private void loadLesson(OpeningLessons.Lesson l) {
        this.lesson = l;
        titleLabel.setText(l.name());
        descLabel.setText(l.description());
        game = new Game();
        board.setBoard(game.board());
        selected = Square.NONE;
        step = 0;
        board.setLastMove(Square.NONE, Square.NONE);
        board.clearSelection();
        board.refresh();
        showStep();
    }

    private void showStep() {
        if (step >= lesson.steps().size()) {
            explanationLabel.setText("✅ Lesson complete! Pick another lesson, or press Restart to run it again.");
            progressLabel.setText("Done — " + lesson.steps().size() + " moves");
            board.clearHint();
            return;
        }
        OpeningLessons.Step st = lesson.steps().get(step);
        explanationLabel.setText(st.explanation());
        progressLabel.setText("Move " + (step + 1) + " / " + lesson.steps().size()
                + "   ·   " + Color.name(game.board().sideToMove()) + " to play");
        int expected = expectedMove();
        if (expected != Move.NONE) board.setHint(Move.from(expected), Move.to(expected));
    }

    private int expectedMove() {
        if (step >= lesson.steps().size()) return Move.NONE;
        return findUci(game.board(), lesson.steps().get(step).uci());
    }

    private void playStep(int move) {
        int from = Move.from(move), to = Move.to(move);
        game.play(move);
        board.setLastMove(from, to);
        selected = Square.NONE;
        board.clearSelection();
        board.refresh();
        step++;
        showStep();
    }

    // ------------------------------------------------------------------
    //  Board interaction (click-to-move, gated to the lesson move)
    // ------------------------------------------------------------------

    @Override public void onPress(int square) {
        if (step >= lesson.steps().size()) return;
        int p = game.board().pieceAt(square);
        if (p != Piece.NONE && Piece.color(p) == game.board().sideToMove()) {
            selected = square;
            board.setSelection(square, game.legalMovesFrom(square));
        }
    }

    @Override public void onRelease(int square) {
        if (selected == Square.NONE || square == selected) return;

        String expectedUci = step < lesson.steps().size() ? lesson.steps().get(step).uci() : "";
        int chosen = Move.NONE;
        boolean legalToTarget = false;
        MoveList legal = game.legalMoves();
        for (int i = 0; i < legal.size(); i++) {
            int m = legal.get(i);
            if (Move.from(m) == selected && Move.to(m) == square) {
                legalToTarget = true;
                if (Move.toUci(m).equals(expectedUci)) { chosen = m; break; }
            }
        }

        if (!legalToTarget) {
            int p = game.board().pieceAt(square);
            if (p != Piece.NONE && Piece.color(p) == game.board().sideToMove()) {
                selected = square;
                board.setSelection(square, game.legalMovesFrom(square));
            } else {
                selected = Square.NONE;
                board.clearSelection();
            }
            return;
        }

        if (chosen != Move.NONE) {
            playStep(chosen);
        } else {
            explanationLabel.setText("That move is legal, but the lesson move is the one highlighted in orange — give it a try!");
            selected = Square.NONE;
            board.clearSelection();
        }
    }

    private static int findUci(Board board, String uci) {
        MoveList legal = MoveGenerator.generateLegal(board);
        for (int i = 0; i < legal.size(); i++) {
            if (Move.toUci(legal.get(i)).equals(uci)) return legal.get(i);
        }
        return Move.NONE;
    }

    // ------------------------------------------------------------------
    //  Layout
    // ------------------------------------------------------------------

    private HBox topBar() {
        Button back = new Button("← Menu");
        back.getStyleClass().add("ghost");
        back.setOnAction(e -> nav.showMenu());
        Label t = new Label("Learn Openings");
        t.getStyleClass().add("top-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(14, back, t, spacer);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 16, 12, 16));
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    private VBox sidePanel() {
        lessonBox.getItems().addAll(OpeningLessons.all());
        // Show just the opening's name, not the whole record's toString().
        lessonBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(OpeningLessons.Lesson l) { return l == null ? "" : l.name(); }
            @Override public OpeningLessons.Lesson fromString(String s) { return null; }
        });
        lessonBox.setValue(OpeningLessons.all().get(0));
        lessonBox.setMaxWidth(Double.MAX_VALUE);
        lessonBox.setOnAction(e -> { if (lessonBox.getValue() != null) loadLesson(lessonBox.getValue()); });

        titleLabel.getStyleClass().add("title");
        descLabel.getStyleClass().add("best-line");
        descLabel.setWrapText(true);
        progressLabel.getStyleClass().add("status");
        explanationLabel.getStyleClass().add("lesson-text");
        explanationLabel.setWrapText(true);

        VBox explanationCard = new VBox(explanationLabel);
        explanationCard.getStyleClass().add("lesson-card");
        explanationCard.setPadding(new Insets(12));
        VBox.setVgrow(explanationCard, Priority.ALWAYS);

        Button showMove = new Button("💡 Show Move");
        showMove.setMaxWidth(Double.MAX_VALUE);
        showMove.setOnAction(e -> { int m = expectedMove(); if (m != Move.NONE) board.setHint(Move.from(m), Move.to(m)); });
        Button restart = new Button("↺ Restart");
        restart.setMaxWidth(Double.MAX_VALUE);
        restart.setOnAction(e -> loadLesson(lesson));
        HBox actions = new HBox(8, showMove, restart);
        for (var n : actions.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        VBox box = new VBox(12,
                new Label("Choose an opening:"),
                lessonBox, titleLabel, descLabel,
                new Separator(),
                progressLabel, explanationCard, actions);
        box.setPadding(new Insets(14));
        box.setPrefWidth(320);
        box.getStyleClass().add("side-panel");
        return box;
    }
}
