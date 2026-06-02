package com.chessmaster.ui;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * JavaFX entry point and screen navigator. The app is a stack of full-screen
 * "screens" (main menu, play, learn, settings, credits); {@link #swap} fades
 * between them and disposes the previous screen's background resources.
 *
 * <p>Run with {@code mvn javafx:run}, or the green ▶ on this class in IntelliJ.
 */
public final class ChessApp extends Application implements Navigator {

    private final StackPane rootPane = new StackPane();
    private Runnable disposeCurrent = () -> {};

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(rootPane, 1240, 840);
        var css = getClass().getResource("/com/chessmaster/ui/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setTitle("ChessMaster");
        stage.setMinWidth(1040);
        stage.setMinHeight(760);
        stage.setScene(scene);
        if (System.getProperty("chess.startScreen", "menu").equals("play")) showPlay();
        else showMenu();
        stage.show();
    }

    private void swap(Parent next, Runnable dispose) {
        disposeCurrent.run();
        disposeCurrent = dispose != null ? dispose : () -> {};
        rootPane.getChildren().setAll(next);
        FadeTransition fade = new FadeTransition(Duration.millis(220), next);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    @Override public void showMenu()     { swap(new MainMenuView(this).getView(), null); }
    @Override public void showSettings() { swap(new SettingsView(this).getView(), null); }
    @Override public void showCredits()  { swap(new CreditsView(this).getView(), null); }

    @Override public void showPlay() {
        PlayView view = new PlayView(this);
        swap(view.getView(), view::dispose);
    }

    @Override public void showLearn() {
        LearnView view = new LearnView(this);
        swap(view.getView(), view::dispose);
    }

    @Override public void showPuzzles() {
        PuzzlesView view = new PuzzlesView(this);
        swap(view.getView(), view::dispose);
    }

    @Override public void quit() {
        disposeCurrent.run();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
