package com.chessmaster.ui;

import com.chessmaster.engine.Engine;
import com.chessmaster.model.Color;
import com.chessmaster.model.Game;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * The right-hand control/info panel: game setup controls, an evaluation bar, the
 * engine's best line, a status line, and the running move list.
 */
public final class ControlPanel extends VBox implements GamePanel {

    private final GameController controller;

    private final ComboBox<String> opponentBox = new ComboBox<>();
    private final ComboBox<String> sideBox = new ComboBox<>();
    private final ComboBox<Engine.Level> levelBox = new ComboBox<>();

    private final Label statusLabel = new Label("White to move");
    private final Label bestLineLabel = new Label("");
    private final Label goalLabel = new Label("");
    private final Label warningLabel = new Label("");
    private final ProgressIndicator spinner = new ProgressIndicator();

    private final EvalBar evalBar = new EvalBar();
    private final Label evalText = new Label("0.00");
    private final ListView<String> moveList = new ListView<>();

    public ControlPanel(GameController controller) {
        this.controller = controller;
        setSpacing(10);
        setPadding(new Insets(14));
        setPrefWidth(300);
        getStyleClass().add("side-panel");

        getChildren().addAll(title("♚ ChessMaster"), setupBox(), actionBox(), analysisBox());
        if (Settings.get().showEvalBar) getChildren().add(evalSection());
        getChildren().addAll(statusSection(), moveListSection());
    }

    private Label title(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Sans Serif", FontWeight.BOLD, 22));
        l.getStyleClass().add("title");
        return l;
    }

    private TitledPane setupBox() {
        opponentBox.getItems().addAll("Play vs Computer", "Two Players");
        opponentBox.getSelectionModel().select(0);

        sideBox.getItems().addAll("Play as White", "Play as Black");
        sideBox.getSelectionModel().select(0);

        levelBox.getItems().addAll(Engine.Level.values());
        levelBox.getSelectionModel().select(Engine.Level.INTERMEDIATE);
        levelBox.setOnAction(e -> controller.setLevel(levelBox.getValue()));

        Button newGame = new Button("New Game");
        newGame.setMaxWidth(Double.MAX_VALUE);
        newGame.getStyleClass().add("primary");
        newGame.setOnAction(e -> startNewGame());

        VBox box = new VBox(8, opponentBox, sideBox, new Label("Difficulty:"), levelBox, newGame);
        opponentBox.setMaxWidth(Double.MAX_VALUE);
        sideBox.setMaxWidth(Double.MAX_VALUE);
        levelBox.setMaxWidth(Double.MAX_VALUE);

        TitledPane pane = new TitledPane("Game Setup", box);
        pane.setCollapsible(false);
        return pane;
    }

    private void startNewGame() {
        boolean vsAI = opponentBox.getSelectionModel().getSelectedIndex() == 0;
        int humanColor = sideBox.getSelectionModel().getSelectedIndex() == 0 ? Color.WHITE : Color.BLACK;
        controller.newGame(levelBox.getValue(), humanColor, vsAI);
    }

    private HBox actionBox() {
        Button hint = iconButton("💡 Hint", e -> controller.requestHint());
        Button undo = iconButton("↶ Undo", e -> controller.undo());
        Button flip = iconButton("⇅ Flip", e -> controller.flipBoard());
        Button pgn  = iconButton("⎘ PGN", e -> copyPgn());
        HBox box = new HBox(6, hint, undo, flip, pgn);
        box.setAlignment(Pos.CENTER_LEFT);
        for (var n : box.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        return box;
    }

    private Button iconButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(handler);
        return b;
    }

    private void copyPgn() {
        String pgn = controller.game().toPgn();
        ClipboardContent content = new ClipboardContent();
        content.putString(pgn);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("PGN copied to clipboard");
    }

    private VBox analysisBox() {
        Button analyze = iconButton("📈 Analyze game", e -> controller.analyzeGame());
        goalLabel.setWrapText(true);
        goalLabel.getStyleClass().add("goal");
        VBox box = new VBox(6, analyze, goalLabel);
        return box;
    }

    private VBox evalSection() {
        evalText.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
        HBox row = new HBox(10, evalBar, new VBox(4, new Label("Evaluation"), evalText));
        row.setAlignment(Pos.CENTER_LEFT);

        bestLineLabel.setWrapText(true);
        bestLineLabel.getStyleClass().add("best-line");
        VBox box = new VBox(6, row, bestLineLabel);
        return box;
    }

    private VBox statusSection() {
        statusLabel.setWrapText(true);
        statusLabel.getStyleClass().add("status");
        warningLabel.setWrapText(true);
        warningLabel.getStyleClass().add("warning");
        spinner.setPrefSize(18, 18);
        spinner.setVisible(false);
        HBox row = new HBox(8, spinner, statusLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(4, row, warningLabel);
    }

    private VBox moveListSection() {
        moveList.setPrefHeight(150);
        VBox.setVgrow(moveList, Priority.ALWAYS);
        VBox box = new VBox(4, new Label("Moves"), moveList);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    // ------------------------------------------------------------------
    //  Updates pushed by the controller (always on the FX thread)
    // ------------------------------------------------------------------

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void setGoal(String text) {
        goalLabel.setText(text);
    }

    public void setWarning(String text) {
        warningLabel.setText(text);
    }

    public void clearWarning() {
        warningLabel.setText("");
    }

    public void setThinking(boolean thinking) {
        spinner.setVisible(thinking);
    }

    public void setEval(int whiteCp, int mateSigned, String bestLineSan) {
        evalBar.setEval(whiteCp, mateSigned);
        if (mateSigned != 0) {
            evalText.setText("#" + (mateSigned > 0 ? "" : "-") + Math.abs(mateSigned));
        } else {
            evalText.setText(String.format("%+.2f", whiteCp / 100.0));
        }
        bestLineLabel.setText(bestLineSan == null || bestLineSan.isEmpty()
                ? "" : "Best line: " + bestLineSan);
    }

    public void setMoveList(List<Game.PlayedMove> history) {
        moveList.getItems().clear();
        for (int i = 0; i < history.size(); i += 2) {
            int moveNo = i / 2 + 1;
            String white = history.get(i).san();
            String black = (i + 1 < history.size()) ? history.get(i + 1).san() : "";
            moveList.getItems().add(String.format("%d. %-8s %s", moveNo, white, black));
        }
        if (!moveList.getItems().isEmpty()) {
            moveList.scrollTo(moveList.getItems().size() - 1);
        }
    }

    /**
     * A vertical evaluation bar: a white wedge rising from the bottom over a dark
     * background, sized by a sigmoid of the centipawn score so huge advantages
     * don't peg it instantly.
     */
    private static final class EvalBar extends Pane {
        private static final double W = 26, H = 240;
        private final Rectangle dark = new Rectangle(W, H, javafx.scene.paint.Color.web("#403d39"));
        private final Rectangle white = new Rectangle(W, H / 2, javafx.scene.paint.Color.web("#f4f4f4"));

        EvalBar() {
            // Lock the size so the bar always reserves exactly its space (it won't be
            // squished and overflow onto neighboring text), and clip so its rectangles
            // can never paint outside its bounds.
            setMinSize(W, H);
            setPrefSize(W, H);
            setMaxSize(W, H);
            setClip(new Rectangle(W, H));
            white.setY(H / 2);
            getChildren().addAll(dark, white);
            setEval(0, 0);
        }

        void setEval(int whiteCp, int mateSigned) {
            double frac;
            if (mateSigned != 0) {
                frac = mateSigned > 0 ? 1.0 : 0.0;
            } else {
                frac = 1.0 / (1.0 + Math.exp(-whiteCp / 400.0)); // 0..1, white's share
            }
            double whiteHeight = frac * H;
            white.setHeight(whiteHeight);
            white.setY(H - whiteHeight);
        }
    }
}
