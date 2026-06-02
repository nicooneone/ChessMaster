package com.chessmaster.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Credits / about screen. Edit {@link #CREATOR} to your own name. */
public final class CreditsView {

    // The team behind ChessMaster.
    private static final String[] CREATORS = {
            "Nicholas de Hombre",
            "Maximus Elmorry",
            "Anthony Masciale",
    };

    private final Navigator nav;

    public CreditsView(Navigator nav) {
        this.nav = nav;
    }

    public Parent getView() {
        Label title = new Label("★  Credits");
        title.getStyleClass().add("screen-title");

        VBox body = new VBox(10,
                big("Created by"),
                big(String.join("  ·  ", CREATORS)),
                line("Design, chess engine, and 3D interface — all built from scratch in Java."),
                section("Built with"),
                line("• Java + JavaFX (3D scene graph, perspective camera, PhongMaterial)"),
                line("• A custom move generator, verified with perft"),
                line("• A negamax + alpha-beta search with a transposition table"),
                section("Acknowledgements"),
                line("• PeSTO evaluation tables by Ronald Friederich"),
                line("• Perft reference counts from the Chess Programming Wiki"),
                section("Version"),
                line("ChessMaster 2.0 — 3D edition")
        );
        body.setAlignment(Pos.CENTER_LEFT);

        Button back = new Button("Back");
        back.getStyleClass().add("primary");
        back.setOnAction(e -> nav.showMenu());

        VBox card = new VBox(18, title, body, back);
        card.getStyleClass().add("card");
        card.setMaxWidth(620);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("screen-root");
        StackPane.setMargin(card, new Insets(40));
        return root;
    }

    private Label big(String t)     { Label l = new Label(t); l.getStyleClass().add("credits-big"); return l; }
    private Label section(String t) { Label l = new Label(t); l.getStyleClass().add("credits-section"); return l; }
    private Label line(String t)    { Label l = new Label(t); l.getStyleClass().add("credits-line"); l.setWrapText(true); return l; }
}
