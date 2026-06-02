package com.chessmaster.ui;

import com.chessmaster.training.Analyzer;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * A pop-up window that visualizes a {@link Analyzer.Review}: an evaluation curve
 * over the course of the game and a color-coded, move-by-move breakdown.
 */
public final class AnalysisView {

    private AnalysisView() {}

    public static void show(Analyzer.Review review) {
        Stage stage = new Stage();
        stage.setTitle("Game Analysis");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.getStyleClass().add("analysis");

        root.setTop(buildChart(review));
        root.setCenter(buildList(review));
        root.setBottom(buildSummary(review));

        Scene scene = new Scene(root, 760, 640);
        var css = AnalysisView.class.getResource("/com/chessmaster/ui/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private static LineChart<Number, Number> buildChart(Analyzer.Review review) {
        NumberAxis x = new NumberAxis();
        x.setLabel("Ply");
        NumberAxis y = new NumberAxis(-10, 10, 1);
        y.setLabel("Evaluation (pawns, +White)");

        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setTitle("Evaluation over the game");
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(260);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        var curve = review.evalWhiteCp();
        for (int i = 0; i < curve.size(); i++) {
            double pawns = Math.max(-10, Math.min(10, curve.get(i) / 100.0));
            series.getData().add(new XYChart.Data<>(i, pawns));
        }
        chart.getData().add(series);
        return chart;
    }

    private static ListView<String> buildList(Analyzer.Review review) {
        ListView<String> list = new ListView<>();
        for (Analyzer.MoveReview m : review.moves()) {
            int moveNo = m.ply() / 2 + 1;
            String dots = m.whiteMoved() ? "." : "...";
            String note = m.quality() == Analyzer.Quality.BEST
                    ? "Best"
                    : String.format("%s (-%.2f), best was %s", m.quality().label, m.cpLoss() / 100.0, m.bestSan());
            list.getItems().add(String.format("%d%s %-7s  %s", moveNo, dots, m.san(), note));
        }
        list.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if (empty) { setStyle(""); return; }
                String color = "#d6d2cc";
                if (item.contains("Blunder"))         color = "#e54b4b";
                else if (item.contains("Mistake"))    color = "#e8954a";
                else if (item.contains("Inaccuracy")) color = "#e8c34a";
                setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Monospaced';");
            }
        });
        return list;
    }

    private static VBox buildSummary(Analyzer.Review review) {
        int whiteBlunders = 0, blackBlunders = 0, whiteMistakes = 0, blackMistakes = 0;
        for (Analyzer.MoveReview m : review.moves()) {
            if (m.quality() == Analyzer.Quality.BLUNDER) { if (m.whiteMoved()) whiteBlunders++; else blackBlunders++; }
            if (m.quality() == Analyzer.Quality.MISTAKE) { if (m.whiteMoved()) whiteMistakes++; else blackMistakes++; }
        }
        Label summary = new Label(String.format(
                "White: %d blunders, %d mistakes      Black: %d blunders, %d mistakes",
                whiteBlunders, whiteMistakes, blackBlunders, blackMistakes));
        summary.getStyleClass().add("status");
        VBox box = new VBox(summary);
        box.setPadding(new Insets(8, 0, 0, 0));
        return box;
    }
}
