package com.chessmaster.ui;

import com.chessmaster.engine.Engine;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/** Settings screen — edits the shared {@link Settings} and can persist them. */
public final class SettingsView {

    private final Navigator nav;
    private final Settings s = Settings.get();

    public SettingsView(Navigator nav) {
        this.nav = nav;
    }

    public Parent getView() {
        Label title = new Label("⚙  Settings");
        title.getStyleClass().add("screen-title");

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(16);
        grid.setPadding(new Insets(8));
        int r = 0;

        ComboBox<Engine.Level> level = new ComboBox<>();
        level.getItems().addAll(Engine.Level.values());
        level.setValue(s.defaultLevel);
        level.setOnAction(e -> s.defaultLevel = level.getValue());
        addRow(grid, r++, "Default difficulty", level);

        ComboBox<Settings.Theme> theme = new ComboBox<>();
        theme.getItems().addAll(Settings.Theme.values());
        theme.setValue(s.theme);
        theme.setOnAction(e -> s.theme = theme.getValue());
        addRow(grid, r++, "Board theme", theme);

        ColorPicker white = new ColorPicker(Color.web(s.pieceWhite));
        white.setOnAction(e -> s.pieceWhite = toHex(white.getValue()));
        addRow(grid, r++, "White pieces", white);

        ColorPicker black = new ColorPicker(Color.web(s.pieceBlack));
        black.setOnAction(e -> s.pieceBlack = toHex(black.getValue()));
        addRow(grid, r++, "Black pieces", black);

        Slider anim = new Slider(0, 600, s.animationMs);
        anim.setShowTickLabels(true);
        anim.setShowTickMarks(true);
        anim.setMajorTickUnit(150);
        anim.valueProperty().addListener((o, a, b) -> s.animationMs = b.intValue());
        addRow(grid, r++, "Animation (ms)", anim);

        CheckBox evalBar = new CheckBox("Show evaluation bar");
        evalBar.setSelected(s.showEvalBar);
        evalBar.selectedProperty().addListener((o, a, b) -> s.showEvalBar = b);
        addRow(grid, r++, "Evaluation", evalBar);

        CheckBox orbit = new CheckBox("Drag to rotate / scroll to zoom");
        orbit.setSelected(s.orbitEnabled);
        orbit.selectedProperty().addListener((o, a, b) -> s.orbitEnabled = b);
        addRow(grid, r++, "3D camera", orbit);

        CheckBox fun = new CheckBox("Sun shafts, soft shadows & floating dust");
        fun.setSelected(s.funVisuals);
        fun.selectedProperty().addListener((o, a, b) -> s.funVisuals = b);
        addRow(grid, r++, "Fun visuals", fun);

        Button save = new Button("Save");
        save.getStyleClass().add("primary");
        save.setOnAction(e -> { s.save(); nav.showMenu(); });
        Button back = new Button("Back");
        back.setOnAction(e -> nav.showMenu());
        HBox actions = new HBox(12, save, back);
        actions.setAlignment(Pos.CENTER_LEFT);

        Label note = new Label("Theme and piece colors apply the next time you open a board.");
        note.getStyleClass().add("hint-note");

        VBox card = new VBox(18, title, grid, note, actions);
        card.getStyleClass().add("card");
        card.setMaxWidth(560);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("screen-root");
        StackPane.setMargin(card, new Insets(40));
        return root;
    }

    private void addRow(GridPane grid, int row, String label, Control control) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        control.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(control, Priority.ALWAYS);
        grid.add(l, 0, row);
        grid.add(control, 1, row);
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }
}
