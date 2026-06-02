package com.chessmaster.ui;

import com.chessmaster.model.Piece;
import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

/**
 * Builds chess pieces procedurally from JavaFX 3D primitives — no model files
 * needed. Each piece is a {@link Group} centered on the origin in the X/Z plane,
 * resting on the board surface at {@code y = 0} and rising "up" (negative Y in
 * JavaFX's coordinate system). The shapes are stylized Staunton silhouettes:
 * a tiered base, a body, and a type-specific top.
 */
public final class PieceFactory3D {

    private PieceFactory3D() {}

    /** One board tile is this many world units; piece proportions are relative to it. */
    public static final double TILE = 10.0;

    public static Group create(int piece, PhongMaterial material) {
        Group g = new Group();
        switch (Piece.type(piece)) {
            case Piece.PAWN   -> pawn(g, material);
            case Piece.ROOK   -> rook(g, material);
            case Piece.KNIGHT -> knight(g, material);
            case Piece.BISHOP -> bishop(g, material);
            case Piece.QUEEN  -> queen(g, material);
            case Piece.KING   -> king(g, material);
            default -> {}
        }
        return g;
    }

    // A running "height above the board" cursor makes stacking parts readable.
    private static final class Stack {
        final Group g;
        final PhongMaterial mat;
        double h = 0; // current height above the board surface
        Stack(Group g, PhongMaterial mat) { this.g = g; this.mat = mat; }

        /** Add a cylinder sitting on top of the stack; advance the cursor. */
        void cyl(double radius, double height) {
            Cylinder c = new Cylinder(radius, height);
            c.setMaterial(mat);
            c.setTranslateY(-(h + height / 2));
            g.getChildren().add(c);
            h += height;
        }

        /** Add a sphere whose bottom touches the top of the stack. */
        Sphere ball(double radius) {
            Sphere s = new Sphere(radius);
            s.setMaterial(mat);
            s.setTranslateY(-(h + radius));
            g.getChildren().add(s);
            h += 2 * radius;
            return s;
        }
    }

    private static void base(Stack s, double r) {
        s.cyl(r, 0.10 * TILE);            // wide foot
        s.cyl(r * 0.78, 0.10 * TILE);     // step
    }

    private static void pawn(Group g, PhongMaterial m) {
        Stack s = new Stack(g, m);
        base(s, 0.34 * TILE);
        s.cyl(0.18 * TILE, 0.22 * TILE);  // neck
        s.cyl(0.24 * TILE, 0.06 * TILE);  // collar
        s.ball(0.24 * TILE);              // head
    }

    private static void rook(Group g, PhongMaterial m) {
        Stack s = new Stack(g, m);
        base(s, 0.36 * TILE);
        s.cyl(0.24 * TILE, 0.42 * TILE);  // body
        s.cyl(0.32 * TILE, 0.12 * TILE);  // top ring (battlements suggested by the wide cap)
        // Four merlons around the top.
        double top = s.h;
        for (int i = 0; i < 4; i++) {
            Box merlon = new Box(0.12 * TILE, 0.16 * TILE, 0.12 * TILE);
            merlon.setMaterial(m);
            double angle = Math.toRadians(i * 90);
            merlon.setTranslateX(Math.cos(angle) * 0.22 * TILE);
            merlon.setTranslateZ(Math.sin(angle) * 0.22 * TILE);
            merlon.setTranslateY(-(top + 0.08 * TILE));
            g.getChildren().add(merlon);
        }
    }

    private static void knight(Group g, PhongMaterial m) {
        Stack s = new Stack(g, m);
        base(s, 0.36 * TILE);
        s.cyl(0.22 * TILE, 0.28 * TILE);  // short neck

        // Stylized horse head: an angled block with a snout.
        double y = s.h;
        Box head = new Box(0.20 * TILE, 0.42 * TILE, 0.26 * TILE);
        head.setMaterial(m);
        head.getTransforms().add(new Rotate(-28, Rotate.X_AXIS));
        head.setTranslateY(-(y + 0.26 * TILE));
        head.setTranslateZ(-0.04 * TILE);
        g.getChildren().add(head);

        Box snout = new Box(0.16 * TILE, 0.14 * TILE, 0.22 * TILE);
        snout.setMaterial(m);
        snout.getTransforms().add(new Rotate(-28, Rotate.X_AXIS));
        snout.setTranslateY(-(y + 0.30 * TILE));
        snout.setTranslateZ(-0.18 * TILE);
        g.getChildren().add(snout);

        Box ears = new Box(0.16 * TILE, 0.10 * TILE, 0.06 * TILE);
        ears.setMaterial(m);
        ears.setTranslateY(-(y + 0.50 * TILE));
        ears.setTranslateZ(0.08 * TILE);
        g.getChildren().add(ears);
    }

    private static void bishop(Group g, PhongMaterial m) {
        Stack s = new Stack(g, m);
        base(s, 0.34 * TILE);
        // Tapered body via decreasing cylinders.
        s.cyl(0.22 * TILE, 0.16 * TILE);
        s.cyl(0.18 * TILE, 0.18 * TILE);
        s.cyl(0.13 * TILE, 0.18 * TILE);
        s.cyl(0.20 * TILE, 0.06 * TILE);  // collar
        s.ball(0.17 * TILE);              // mitre
        s.cyl(0.04 * TILE, 0.10 * TILE);  // finial
    }

    private static void queen(Group g, PhongMaterial m) {
        Stack s = new Stack(g, m);
        base(s, 0.38 * TILE);
        s.cyl(0.24 * TILE, 0.18 * TILE);
        s.cyl(0.19 * TILE, 0.22 * TILE);
        s.cyl(0.15 * TILE, 0.20 * TILE);
        s.cyl(0.28 * TILE, 0.06 * TILE);  // crown base
        double crownY = s.h;
        s.ball(0.12 * TILE);
        // Ring of points (crown).
        for (int i = 0; i < 7; i++) {
            Sphere pt = new Sphere(0.05 * TILE);
            pt.setMaterial(m);
            double a = Math.toRadians(i * (360.0 / 7));
            pt.setTranslateX(Math.cos(a) * 0.20 * TILE);
            pt.setTranslateZ(Math.sin(a) * 0.20 * TILE);
            pt.setTranslateY(-(crownY + 0.04 * TILE));
            g.getChildren().add(pt);
        }
    }

    private static void king(Group g, PhongMaterial m) {
        Stack s = new Stack(g, m);
        base(s, 0.40 * TILE);
        s.cyl(0.25 * TILE, 0.20 * TILE);
        s.cyl(0.20 * TILE, 0.26 * TILE);
        s.cyl(0.16 * TILE, 0.22 * TILE);
        s.cyl(0.30 * TILE, 0.06 * TILE);  // collar
        s.ball(0.14 * TILE);
        // Cross on top.
        double y = s.h;
        Box vert = new Box(0.06 * TILE, 0.26 * TILE, 0.06 * TILE);
        vert.setMaterial(m);
        vert.setTranslateY(-(y + 0.13 * TILE));
        Box horiz = new Box(0.18 * TILE, 0.06 * TILE, 0.06 * TILE);
        horiz.setMaterial(m);
        horiz.setTranslateY(-(y + 0.17 * TILE));
        g.getChildren().addAll(vert, horiz);
    }
}
