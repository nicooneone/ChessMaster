package com.chessmaster.ui;

import com.chessmaster.training.Puzzle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * The side panel for the dedicated Puzzles screen: a difficulty picker, the
 * current puzzle's goal, status/feedback, a solved counter, and the puzzle
 * actions (next, hint, retry, show solution). Implements {@link GamePanel} so
 * {@link GameController} can drive it.
 */
public final class PuzzlePanel extends VBox implements GamePanel {

    private final GameController controller;

    private final ComboBox<String> difficultyBox = new ComboBox<>();
    private final Label goalLabel = new Label("Press “New puzzle” to begin.");
    private final Label statusLabel = new Label("");
    private final Label warningLabel = new Label("");
    private final Label solvedLabel = new Label("Solved: 0");
    private final ProgressIndicator spinner = new ProgressIndicator();

    private int solvedCount = 0;

    public PuzzlePanel(GameController controller) {
        this.controller = controller;
        setSpacing(12);
        setPadding(new Insets(16));
        setPrefWidth(310);
        getStyleClass().add("side-panel");

        Label title = new Label("🧩 Tactics Puzzles");
        title.setFont(Font.font("Sans Serif", FontWeight.BOLD, 22));
        title.getStyleClass().add("title");

        difficultyBox.getItems().addAll("Any difficulty", "Easy", "Medium", "Hard");
        difficultyBox.getSelectionModel().select(0);
        difficultyBox.setMaxWidth(Double.MAX_VALUE);

        Button next = new Button("New puzzle");
        next.getStyleClass().add("primary");
        next.setMaxWidth(Double.MAX_VALUE);
        next.setOnAction(e -> controller.loadPuzzle(selectedDifficulty()));

        goalLabel.setWrapText(true);
        goalLabel.getStyleClass().add("goal");

        statusLabel.setWrapText(true);
        statusLabel.getStyleClass().add("status");

        warningLabel.setWrapText(true);
        warningLabel.getStyleClass().add("warning");

        spinner.setPrefSize(18, 18);
        spinner.setVisible(false);
        HBox statusRow = new HBox(8, spinner, statusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Button hint = action("💡 Hint", e -> controller.requestHint());
        Button retry = action("↻ Retry", e -> controller.retryPuzzle());
        Button solution = action("👁 Show solution", e -> controller.showPuzzleSolution());
        VBox actions = new VBox(8, hint, retry, solution);

        solvedLabel.getStyleClass().add("credits-section");

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);

        getChildren().addAll(
                title,
                new Label("Difficulty"), difficultyBox, next,
                separator(),
                goalLabel, statusRow, warningLabel,
                separator(),
                actions,
                grow,
                solvedLabel
        );
    }

    private Button action(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(handler);
        return b;
    }

    private Separator separator() {
        return new Separator();
    }

    private Puzzle.Difficulty selectedDifficulty() {
        return switch (difficultyBox.getSelectionModel().getSelectedIndex()) {
            case 1 -> Puzzle.Difficulty.EASY;
            case 2 -> Puzzle.Difficulty.MEDIUM;
            case 3 -> Puzzle.Difficulty.HARD;
            default -> null; // "Any"
        };
    }

    // ----- GamePanel -----

    @Override public void setStatus(String text) { statusLabel.setText(text); }
    @Override public void setGoal(String text)   { goalLabel.setText(text); warningLabel.setText(""); }
    @Override public void setWarning(String text){ warningLabel.setText(text); }
    @Override public void clearWarning()         { warningLabel.setText(""); }
    @Override public void setThinking(boolean t) { spinner.setVisible(t); }

    @Override public void puzzleSolved() {
        solvedCount++;
        solvedLabel.setText("Solved: " + solvedCount);
    }
}
