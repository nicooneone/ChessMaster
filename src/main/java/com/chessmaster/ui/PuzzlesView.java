package com.chessmaster.ui;

import com.chessmaster.model.Board;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** The dedicated "Tactics Puzzles" screen: top bar, 3D board, and puzzle panel. */
public final class PuzzlesView {

    private final Navigator nav;
    private final GameController controller;
    private final Board3D board;
    private final BorderPane root;

    public PuzzlesView(Navigator nav) {
        this.nav = nav;
        this.board = new Board3D(Board.startPosition(), Settings.get());
        this.controller = new GameController(board);
        PuzzlePanel panel = new PuzzlePanel(controller);
        controller.setPanel(panel);

        this.root = new BorderPane();
        root.setTop(topBar("Tactics Puzzles"));
        root.setCenter(board.node());
        root.setRight(panel);

        // Start the player on a puzzle right away.
        controller.loadRandomPuzzle();
    }

    public Parent getView() {
        return root;
    }

    public void dispose() {
        controller.dispose();
        board.dispose();
    }

    private HBox topBar(String titleText) {
        Button back = new Button("← Menu");
        back.getStyleClass().add("ghost");
        back.setOnAction(e -> nav.showMenu());

        Label title = new Label(titleText);
        title.getStyleClass().add("top-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(14, back, title, spacer);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 16, 12, 16));
        bar.getStyleClass().add("top-bar");
        return bar;
    }
}
