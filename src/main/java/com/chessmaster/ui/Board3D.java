package com.chessmaster.ui;

import com.chessmaster.model.Board;
import com.chessmaster.model.Color;
import com.chessmaster.model.Move;
import com.chessmaster.model.MoveList;
import com.chessmaster.model.Piece;
import com.chessmaster.model.Square;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.scene.*;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.Set;

import static com.chessmaster.ui.PieceFactory3D.TILE;

/**
 * A real 3D chess board rendered with JavaFX's built-in 3D engine: a perspective
 * camera the user can orbit and zoom, lit materials, procedurally-modeled pieces,
 * and smooth move animations. It implements {@link BoardView}, so the rest of the
 * app drives it exactly like the 2D board.
 */
public final class Board3D implements BoardView {

    private static final double THICK = 2.0;          // square slab thickness
    private static final double SURFACE_LIFT = 0.6;   // markers float just above the board

    private final Settings settings;
    private Board board;
    private boolean flipped = false;
    private BoardView.InputListener listener;

    private final Pane container = new Pane();
    private final SubScene subScene;

    private final Group root = new Group();
    private final Group tableGroup = new Group();     // board + pieces + markers (this is what "flips")
    private final Group squaresGroup = new Group();
    private final Group shadowsGroup = new Group();   // fake blob shadows under pieces (fun visuals)
    private final Group piecesGroup = new Group();
    private final Group markersGroup = new Group();
    private final Rotate flipRotate = new Rotate(0, Rotate.Y_AXIS);

    // "Fun visuals" (off by default): warm sun light, blob shadows, and a 2D
    // god-ray + floating-dust overlay drawn on top of the 3D view.
    private final boolean fun;
    private javafx.scene.canvas.Canvas funOverlay;
    private javafx.animation.AnimationTimer funTimer;
    private double[][] dust; // [n][x, y, radius, fallSpeed, driftAmp, phase]
    private final java.util.Random rng = new java.util.Random(7);

    private final Box[] squareBox = new Box[64];
    private final Group[] pieceNode = new Group[64];
    private final int[] codeAt = new int[64];

    private PhongMaterial lightMat, darkMat, whiteMat, blackMat;
    private PhongMaterial selMat, lastMat, checkMat, hintMat;

    // Highlight state.
    private int selected = Square.NONE;
    private final Set<Integer> legalTargets = new HashSet<>();
    private int lastFrom = Square.NONE, lastTo = Square.NONE;
    private int checkSquare = Square.NONE;
    private int hintFrom = Square.NONE, hintTo = Square.NONE;

    private boolean snapNext = true; // next refresh() snaps instead of animating

    // Camera rig.
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate camYaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate camPitch = new Rotate(-42, Rotate.X_AXIS);
    private final Translate camDist = new Translate(0, 0, -200);

    // Drag tracking (orbit vs click).
    private double pressX, pressY, lastX, lastY;
    private int pressSquare = Square.NONE;
    private boolean dragging = false;

    public Board3D(Board board, Settings settings) {
        this.board = board;
        this.settings = settings;
        this.fun = settings.funVisuals;
        buildMaterials();

        buildSquares();
        buildLights();
        buildCamera();

        tableGroup.getTransforms().add(flipRotate);
        tableGroup.getChildren().addAll(squaresGroup, shadowsGroup, piecesGroup, markersGroup);
        root.getChildren().add(tableGroup);

        subScene = new SubScene(root, 640, 640, true, SceneAntialiasing.BALANCED);
        // IMPORTANT: a SubScene only clears its back buffer when its fill is a solid
        // opaque Color. A gradient/transparent fill leaves "trails" of moving pieces
        // and the orbiting board. Keep this a plain Color.
        subScene.setFill(javafx.scene.paint.Color.web("#1a1c22"));
        subScene.setCamera(camera);

        container.getChildren().add(subScene);
        container.setMinSize(420, 420);
        subScene.widthProperty().bind(container.widthProperty());
        subScene.heightProperty().bind(container.heightProperty());

        if (fun) buildFunOverlay();

        installInput();
        rebuildPieces();
        applyHighlights();
    }

    // ------------------------------------------------------------------
    //  Construction
    // ------------------------------------------------------------------

    private void buildMaterials() {
        lightMat = matte(settings.theme.light);
        darkMat  = matte(settings.theme.dark);
        whiteMat = glossy(settings.pieceWhite);
        blackMat = glossy(settings.pieceBlack);
        selMat   = glossy("#f6f669");
        lastMat  = glossy("#d8c24a");
        checkMat = glossy("#e54b4b");
        hintMat  = glossy("#ff8c00");
    }

    private static PhongMaterial matte(String hex) {
        PhongMaterial m = new PhongMaterial(javafx.scene.paint.Color.web(hex));
        m.setSpecularColor(javafx.scene.paint.Color.web(hex).deriveColor(0, 1, 1.15, 1));
        return m;
    }

    private static PhongMaterial glossy(String hex) {
        javafx.scene.paint.Color c = javafx.scene.paint.Color.web(hex);
        PhongMaterial m = new PhongMaterial(c);
        m.setSpecularColor(c.deriveColor(0, 1, 1.6, 1));
        m.setSpecularPower(28);
        return m;
    }

    private void buildSquares() {
        // A border frame, kept strictly BELOW the squares so their top faces are not
        // coplanar (coplanar faces Z-fight badly when the camera moves).
        Box frame = new Box(8 * TILE + 0.8 * TILE, 4.0, 8 * TILE + 0.8 * TILE);
        frame.setMaterial(glossy("#4a3526"));
        frame.setTranslateY(THICK + 2.5); // squares occupy y in [0, THICK]; frame sits under them
        squaresGroup.getChildren().add(frame);

        for (int sq = 0; sq < 64; sq++) {
            // Shrink each square a hair so neighbors don't share exact faces (Z-fighting),
            // which also draws subtle grid seams.
            Box b = new Box(TILE * 0.985, THICK, TILE * 0.985);
            b.setMaterial(baseMaterial(sq));
            double[] c = coords(sq);
            b.setTranslateX(c[0]);
            b.setTranslateZ(c[1]);
            b.setTranslateY(THICK / 2);
            b.setUserData(sq);
            squareBox[sq] = b;
            squaresGroup.getChildren().add(b);
        }
    }

    private void buildLights() {
        AmbientLight ambient = new AmbientLight(javafx.scene.paint.Color.color(0.42, 0.42, 0.46));
        PointLight key = new PointLight(javafx.scene.paint.Color.color(0.9, 0.9, 0.86));
        key.setTranslateX(60);
        key.setTranslateY(-180);
        key.setTranslateZ(120);
        PointLight fill = new PointLight(javafx.scene.paint.Color.color(0.35, 0.38, 0.45));
        fill.setTranslateX(-120);
        fill.setTranslateY(-120);
        fill.setTranslateZ(-80);
        root.getChildren().addAll(ambient, key, fill);

        if (fun) {
            // A warm "sun" high above and slightly toward the camera, to match the
            // god-ray overlay coming from the top of the screen.
            PointLight sun = new PointLight(javafx.scene.paint.Color.web("#fff3d0"));
            sun.setTranslateX(0);
            sun.setTranslateY(-320);
            sun.setTranslateZ(40);
            root.getChildren().add(sun);
        }
    }

    private void buildCamera() {
        // A small near-clip with a huge far-clip destroys depth-buffer precision and
        // causes Z-fighting ("tearing") on the board. Keep the ratio modest.
        camera.setNearClip(4);
        camera.setFarClip(1400);
        camera.setFieldOfView(42);
        camera.getTransforms().add(camDist);
        Group pivot = new Group(camera);
        pivot.getTransforms().addAll(camYaw, camPitch);
        root.getChildren().add(pivot);
    }

    // ------------------------------------------------------------------
    //  Input: orbit / zoom / click-to-select
    // ------------------------------------------------------------------

    private void installInput() {
        subScene.setOnMousePressed(e -> {
            pressX = lastX = e.getSceneX();
            pressY = lastY = e.getSceneY();
            dragging = false;
            pressSquare = pickSquare(e.getPickResult());
        });
        subScene.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - lastX;
            double dy = e.getSceneY() - lastY;
            lastX = e.getSceneX();
            lastY = e.getSceneY();
            if (Math.hypot(e.getSceneX() - pressX, e.getSceneY() - pressY) > 6) dragging = true;
            if (dragging && settings.orbitEnabled) {
                camYaw.setAngle(camYaw.getAngle() - dx * 0.4);
                camPitch.setAngle(clamp(camPitch.getAngle() - dy * 0.35, -89, -8));
            }
        });
        subScene.setOnMouseReleased(e -> {
            if (!dragging && pressSquare != Square.NONE && listener != null) {
                // A click maps to a press+release on the same square (see GameController).
                listener.onPress(pressSquare);
                listener.onRelease(pressSquare);
            }
        });
        subScene.setOnScroll(e -> {
            double z = camDist.getZ() + e.getDeltaY() * 0.5;
            camDist.setZ(clamp(z, -420, -90));
        });
    }

    private int pickSquare(PickResult pr) {
        if (pr == null) return Square.NONE;
        Node n = pr.getIntersectedNode();
        while (n != null) {
            if (n.getUserData() instanceof Integer i) return i;
            n = n.getParent();
        }
        return Square.NONE;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ------------------------------------------------------------------
    //  Pieces: build & reconcile (with animation)
    // ------------------------------------------------------------------

    private void rebuildPieces() {
        piecesGroup.getChildren().clear();
        for (int sq = 0; sq < 64; sq++) { pieceNode[sq] = null; codeAt[sq] = Piece.NONE; }
        for (int sq = 0; sq < 64; sq++) {
            int p = board.pieceAt(sq);
            if (p == Piece.NONE) continue;
            placeNewPiece(sq, p, false);
        }
        snapNext = false;
    }

    private void placeNewPiece(int sq, int code, boolean fadeIn) {
        Group g = PieceFactory3D.create(code, Piece.isWhite(code) ? whiteMat : blackMat);
        double[] c = coords(sq);
        g.setTranslateX(c[0]);
        g.setTranslateZ(c[1]);
        g.setUserData(sq);
        piecesGroup.getChildren().add(g);
        pieceNode[sq] = g;
        codeAt[sq] = code;
        if (fadeIn) {
            g.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(settings.animationMs), g);
            ft.setToValue(1);
            ft.play();
        }
    }

    /** Animate the rendered pieces from their current squares to match {@code board}. */
    private void reconcile() {
        int[] desired = new int[64];
        for (int sq = 0; sq < 64; sq++) desired[sq] = board.pieceAt(sq);

        boolean[] used = new boolean[64]; // vacated source squares already matched to a target

        // 1) For every square whose contents must change, try to find a source node
        //    of the same piece elsewhere and slide it over.
        for (int t = 0; t < 64; t++) {
            if (desired[t] == codeAt[t]) continue; // unchanged
            if (desired[t] == Piece.NONE) continue; // handled as a removal below
            int from = findSource(desired[t], codeAt, used, t);
            if (from != Square.NONE) {
                used[from] = true;
                Group node = pieceNode[from];
                pieceNode[from] = null; codeAt[from] = Piece.NONE;
                disposeNodeAt(t);             // remove a captured piece on the target
                moveNode(node, t, true);
                node.setUserData(t);
                pieceNode[t] = node; codeAt[t] = desired[t];
            }
        }
        // 2) Remove any leftover pieces whose square no longer holds that piece.
        for (int sq = 0; sq < 64; sq++) {
            if (pieceNode[sq] != null && codeAt[sq] != desired[sq]) disposeNodeAt(sq);
        }
        // 3) Create pieces that appeared with no matching source (e.g. a promotion).
        for (int sq = 0; sq < 64; sq++) {
            if (desired[sq] != Piece.NONE && pieceNode[sq] == null) placeNewPiece(sq, desired[sq], true);
        }
    }

    private int findSource(int code, int[] current, boolean[] used, int notThis) {
        for (int sq = 0; sq < 64; sq++) {
            if (sq == notThis || used[sq]) continue;
            if (current[sq] == code && board.pieceAt(sq) != code) return sq; // vacated source
        }
        return Square.NONE;
    }

    private void disposeNodeAt(int sq) {
        Group node = pieceNode[sq];
        if (node == null) return;
        pieceNode[sq] = null; codeAt[sq] = Piece.NONE;
        FadeTransition ft = new FadeTransition(Duration.millis(Math.max(120, settings.animationMs)), node);
        ft.setToValue(0);
        ft.setOnFinished(ev -> piecesGroup.getChildren().remove(node));
        ft.play();
    }

    private void moveNode(Group n, int to, boolean animate) {
        double[] c = coords(to);
        if (animate && settings.animationMs > 0) {
            TranslateTransition tt = new TranslateTransition(Duration.millis(settings.animationMs), n);
            tt.setToX(c[0]);
            tt.setToZ(c[1]);
            tt.play();
        } else {
            n.setTranslateX(c[0]);
            n.setTranslateZ(c[1]);
        }
    }

    // ------------------------------------------------------------------
    //  Highlights & markers
    // ------------------------------------------------------------------

    private void applyHighlights() {
        for (int sq = 0; sq < 64; sq++) squareBox[sq].setMaterial(baseMaterial(sq));
        if (lastFrom != Square.NONE) squareBox[lastFrom].setMaterial(lastMat);
        if (lastTo != Square.NONE) squareBox[lastTo].setMaterial(lastMat);
        if (hintFrom != Square.NONE) squareBox[hintFrom].setMaterial(hintMat);
        if (hintTo != Square.NONE) squareBox[hintTo].setMaterial(hintMat);
        if (selected != Square.NONE) squareBox[selected].setMaterial(selMat);
        if (checkSquare != Square.NONE) squareBox[checkSquare].setMaterial(checkMat);

        markersGroup.getChildren().clear();
        for (int target : legalTargets) {
            boolean capture = board.pieceAt(target) != Piece.NONE || target == board.enPassantSquare();
            Cylinder dot = new Cylinder(capture ? 0.40 * TILE : 0.15 * TILE, 0.5);
            PhongMaterial mm = new PhongMaterial(capture
                    ? javafx.scene.paint.Color.color(0.9, 0.2, 0.2, 0.55)
                    : javafx.scene.paint.Color.color(0.1, 0.1, 0.1, 0.45));
            dot.setMaterial(mm);
            double[] c = coords(target);
            dot.setTranslateX(c[0]);
            dot.setTranslateZ(c[1]);
            dot.setTranslateY(-SURFACE_LIFT);
            dot.setMouseTransparent(true);
            markersGroup.getChildren().add(dot);
        }

        if (fun) rebuildShadows();
    }

    /** Soft "blob" shadows under each piece — JavaFX 3D can't cast real shadows. */
    private void rebuildShadows() {
        shadowsGroup.getChildren().clear();
        for (int sq = 0; sq < 64; sq++) {
            if (board.pieceAt(sq) == Piece.NONE) continue;
            Cylinder blob = new Cylinder(0.34 * TILE, 0.2);
            PhongMaterial m = new PhongMaterial(javafx.scene.paint.Color.color(0, 0, 0, 0.34));
            blob.setMaterial(m);
            double[] c = coords(sq);
            // Offset slightly away from the overhead sun so it reads as a cast shadow.
            blob.setTranslateX(c[0] + 1.6);
            blob.setTranslateZ(c[1] + 1.6);
            blob.setTranslateY(-0.08);     // just above the board surface
            blob.setScaleX(1.25);          // flatten into an ellipse
            blob.setMouseTransparent(true);
            shadowsGroup.getChildren().add(blob);
        }
    }

    private PhongMaterial baseMaterial(int sq) {
        boolean light = ((Square.file(sq) + Square.rank(sq)) & 1) == 1;
        return light ? lightMat : darkMat;
    }

    private double[] coords(int sq) {
        double x = (Square.file(sq) - 3.5) * TILE;
        double z = (3.5 - Square.rank(sq)) * TILE;
        return new double[]{x, z};
    }

    // ------------------------------------------------------------------
    //  BoardView API
    // ------------------------------------------------------------------

    @Override public void setListener(BoardView.InputListener listener) { this.listener = listener; }

    @Override public void setBoard(Board board) {
        this.board = board;
        snapNext = true;
    }

    @Override public void setFlipped(boolean flipped) {
        if (this.flipped == flipped) return;
        this.flipped = flipped;
        // Smoothly rotate the whole table 180° so the opponent's side faces the camera.
        double target = flipped ? 180 : 0;
        javafx.animation.Timeline tl = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.millis(Math.max(200, settings.animationMs)),
                        new javafx.animation.KeyValue(flipRotate.angleProperty(), target)));
        tl.play();
    }

    @Override public boolean isFlipped() { return flipped; }

    @Override public void setSelection(int square, MoveList legalFromSquare) {
        selected = square;
        legalTargets.clear();
        if (legalFromSquare != null) {
            for (int i = 0; i < legalFromSquare.size(); i++) legalTargets.add(Move.to(legalFromSquare.get(i)));
        }
        applyHighlights();
    }

    @Override public void clearSelection() {
        selected = Square.NONE;
        legalTargets.clear();
        applyHighlights();
    }

    @Override public void setLastMove(int from, int to) { lastFrom = from; lastTo = to; }

    @Override public void setHint(int from, int to) { hintFrom = from; hintTo = to; applyHighlights(); }

    @Override public void clearHint() { hintFrom = Square.NONE; hintTo = Square.NONE; applyHighlights(); }

    @Override public void refresh() {
        if (snapNext) {
            rebuildPieces();
        } else {
            reconcile();
        }
        checkSquare = Square.NONE;
        int stm = board.sideToMove();
        if (board.inCheck(stm)) checkSquare = board.kingSquare(stm);
        applyHighlights();
    }

    @Override public Node node() { return container; }

    // ------------------------------------------------------------------
    //  Fun visuals: 2D god-ray + dust overlay drawn on top of the 3D scene
    // ------------------------------------------------------------------

    private void buildFunOverlay() {
        funOverlay = new javafx.scene.canvas.Canvas();
        funOverlay.setMouseTransparent(true);
        // SCREEN blend makes the warm overlay add light to the board rather than cover it.
        funOverlay.setBlendMode(javafx.scene.effect.BlendMode.SCREEN);
        funOverlay.widthProperty().bind(container.widthProperty());
        funOverlay.heightProperty().bind(container.heightProperty());
        container.getChildren().add(funOverlay);
        funOverlay.widthProperty().addListener((o, a, b) -> initDust());
        funOverlay.heightProperty().addListener((o, a, b) -> initDust());

        funTimer = new javafx.animation.AnimationTimer() {
            private long last = 0;
            @Override public void handle(long now) {
                double dt = (last == 0) ? 0.016 : Math.min(0.05, (now - last) / 1e9);
                last = now;
                drawFun(dt);
            }
        };
        funTimer.start();
    }

    private void initDust() {
        double w = funOverlay.getWidth(), h = funOverlay.getHeight();
        if (w <= 0 || h <= 0) { dust = null; return; }
        int n = 70;
        dust = new double[n][6];
        for (int i = 0; i < n; i++) {
            dust[i][0] = rng.nextDouble() * w;          // x
            dust[i][1] = rng.nextDouble() * h;          // y
            dust[i][2] = 0.6 + rng.nextDouble() * 2.2;  // radius
            dust[i][3] = 6 + rng.nextDouble() * 22;     // fall speed (px/s)
            dust[i][4] = 4 + rng.nextDouble() * 12;     // horizontal drift amplitude
            dust[i][5] = rng.nextDouble() * Math.PI * 2;// phase
        }
    }

    private void drawFun(double dt) {
        if (funOverlay == null) return;
        double w = funOverlay.getWidth(), h = funOverlay.getHeight();
        if (w <= 0 || h <= 0) return;
        if (dust == null) initDust();

        javafx.scene.canvas.GraphicsContext g = funOverlay.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        double t = System.nanoTime() / 1e9;

        // 1) Warm sunlight wash, brightest at the top of the screen.
        LinearGradient wash = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, javafx.scene.paint.Color.web("#fff2c8", 0.22)),
                new Stop(0.5, javafx.scene.paint.Color.web("#ffe6a8", 0.06)),
                new Stop(1, javafx.scene.paint.Color.color(0, 0, 0, 0)));
        g.setFill(wash);
        g.fillRect(0, 0, w, h);

        // 2) A few light shafts streaming from the top, gently swaying.
        int shafts = 4;
        for (int i = 0; i < shafts; i++) {
            double baseX = w * (0.12 + 0.72 * i / (shafts - 1));
            double sway = Math.sin(t * 0.3 + i) * w * 0.03;
            double top = baseX + sway;
            double width = w * 0.09;
            double spread = w * 0.12;
            g.setFill(javafx.scene.paint.Color.web("#fff4d0", 0.05));
            g.fillPolygon(
                    new double[]{top, top + width, baseX + spread + sway + width, baseX + spread + sway},
                    new double[]{0, 0, h, h}, 4);
        }

        // 3) Floating sun dust drifting down.
        for (double[] p : dust) {
            p[1] += p[3] * dt;
            p[0] += Math.sin(t * 0.5 + p[5]) * p[4] * dt;
            if (p[1] - p[2] > h) { p[1] = -p[2]; p[0] = rng.nextDouble() * w; }
            double alpha = 0.5 * (1 - p[1] / h) + 0.12; // brighter near the top
            g.setFill(javafx.scene.paint.Color.web("#fff6da",
                    Math.max(0.05, Math.min(0.6, alpha))));
            g.fillOval(p[0] - p[2], p[1] - p[2], p[2] * 2, p[2] * 2);
        }
    }

    /** Stop background animations when the screen is torn down. */
    public void dispose() {
        if (funTimer != null) funTimer.stop();
    }
}

