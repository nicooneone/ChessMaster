package com.chessmaster.ui;

import com.chessmaster.engine.Engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * User-adjustable settings, shared app-wide as a single instance and persisted to
 * {@code ~/.chessmaster.properties} so choices survive between runs.
 */
public final class Settings {

    private static final Path FILE = Path.of(System.getProperty("user.home"), ".chessmaster.properties");

    // NOTE: INSTANCE must be declared AFTER FILE — load() reads FILE during this
    // static initialization, and static fields initialize in declaration order.
    private static final Settings INSTANCE = load();
    public static Settings get() { return INSTANCE; }

    /** Named board color themes (light square, dark square). */
    public enum Theme {
        WALNUT ("Walnut",  "#f0d9b5", "#b58863"),
        EMERALD("Emerald", "#eeeed2", "#769656"),
        OCEAN  ("Ocean",   "#dee3e6", "#4b6c8c"),
        MIDNIGHT("Midnight","#9aa0b5", "#3b3f54"),
        ROSE   ("Rose",    "#f3d9dc", "#a05c6b");

        public final String label, light, dark;
        Theme(String label, String light, String dark) { this.label = label; this.light = light; this.dark = dark; }
        @Override public String toString() { return label; }
    }

    public Engine.Level defaultLevel = Engine.Level.INTERMEDIATE;
    public Theme theme = Theme.WALNUT;
    public boolean showEvalBar = true;
    public boolean orbitEnabled = true;
    /** "For fun" cinematic visuals: sun shafts, soft shadows, floating dust. Off by default. */
    public boolean funVisuals = false;
    public int animationMs = 280;
    public String pieceWhite = "#efe6d2";
    public String pieceBlack = "#2b2b30";

    private static Settings load() {
        Settings s = new Settings();
        if (!Files.exists(FILE)) return s;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            s.defaultLevel = parseLevel(p.getProperty("level"), s.defaultLevel);
            s.theme = parseTheme(p.getProperty("theme"), s.theme);
            s.showEvalBar = Boolean.parseBoolean(p.getProperty("showEvalBar", String.valueOf(s.showEvalBar)));
            s.orbitEnabled = Boolean.parseBoolean(p.getProperty("orbitEnabled", String.valueOf(s.orbitEnabled)));
            s.funVisuals = Boolean.parseBoolean(p.getProperty("funVisuals", String.valueOf(s.funVisuals)));
            s.animationMs = parseInt(p.getProperty("animationMs"), s.animationMs);
            s.pieceWhite = p.getProperty("pieceWhite", s.pieceWhite);
            s.pieceBlack = p.getProperty("pieceBlack", s.pieceBlack);
        } catch (IOException ignored) {
            // Fall back to defaults on any read problem.
        }
        return s;
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("level", defaultLevel.name());
        p.setProperty("theme", theme.name());
        p.setProperty("showEvalBar", String.valueOf(showEvalBar));
        p.setProperty("orbitEnabled", String.valueOf(orbitEnabled));
        p.setProperty("funVisuals", String.valueOf(funVisuals));
        p.setProperty("animationMs", String.valueOf(animationMs));
        p.setProperty("pieceWhite", pieceWhite);
        p.setProperty("pieceBlack", pieceBlack);
        try (OutputStream out = Files.newOutputStream(FILE)) {
            p.store(out, "ChessMaster settings");
        } catch (IOException ignored) {
            // Non-fatal: settings just won't persist this run.
        }
    }

    private static Engine.Level parseLevel(String v, Engine.Level def) {
        if (v == null) return def;
        try { return Engine.Level.valueOf(v); } catch (IllegalArgumentException e) { return def; }
    }

    private static Theme parseTheme(String v, Theme def) {
        if (v == null) return def;
        try { return Theme.valueOf(v); } catch (IllegalArgumentException e) { return def; }
    }

    private static int parseInt(String v, int def) {
        if (v == null) return def;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }
}
