package com.chessmaster.ui;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** The landing screen: title and the main navigation buttons. */
public final class MainMenuView {

    private final Navigator nav;

    public MainMenuView(Navigator nav) {
        this.nav = nav;
    }

    public Parent getView() {
        Label title = new Label("♚ ChessMaster");
        title.getStyleClass().add("menu-title");

        Label subtitle = new Label("A 3D chess studio — play, learn, and improve");
        subtitle.getStyleClass().add("menu-subtitle");

        VBox buttons = new VBox(14,
                menuButton("▶  Play", "Play a full game against the computer", nav::showPlay, true),
                menuButton("🧩  Puzzles", "Solve tactics puzzles by difficulty", nav::showPuzzles, false),
                menuButton("🎓  Learn Openings", "Guided, move-by-move opening trials", nav::showLearn, false),
                menuButton("⚙  Settings", "Themes, difficulty, and more", nav::showSettings, false),
                menuButton("★  Credits", "About this project", nav::showCredits, false),
                menuButton("⏻  Quit", "Exit ChessMaster", nav::quit, false)
        );
        buttons.setAlignment(Pos.CENTER);
        buttons.setMaxWidth(420);

        VBox content = new VBox(10, title, subtitle, spacer(), buttons);
        content.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(content);
        root.getStyleClass().add("menu-root");
        return root;
    }

    private Button menuButton(String text, String tooltip, Runnable action, boolean primary) {
        Button b = new Button(text);
        b.getStyleClass().add("menu-button");
        if (primary) b.getStyleClass().add("menu-button-primary");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        b.setOnAction(e -> action.run());
        return b;
    }

    private Label spacer() {
        Label s = new Label("");
        s.setMinHeight(24);
        return s;
    }
}
