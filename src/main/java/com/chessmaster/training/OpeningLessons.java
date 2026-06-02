package com.chessmaster.training;

import java.util.List;

/**
 * Curated, guided opening lessons. Each lesson is a sequence of moves (in UCI
 * notation) with a plain-English explanation of the *idea* behind each move, so
 * the learner understands the plan rather than just memorizing.
 */
public final class OpeningLessons {

    private OpeningLessons() {}

    public record Step(String uci, String explanation) {}
    public record Lesson(String name, String description, List<Step> steps) {}

    public static List<Lesson> all() {
        return List.of(ITALIAN, RUY_LOPEZ, SICILIAN, QUEENS_GAMBIT, LONDON);
    }

    private static final Lesson ITALIAN = new Lesson(
            "Italian Game",
            "A classical, principled opening — develop fast and target f7.",
            List.of(
                    new Step("e2e4", "1.e4 — Claim the center and free your bishop and queen."),
                    new Step("e7e5", "1...e5 — Black mirrors, contesting the center."),
                    new Step("g1f3", "2.Nf3 — Develop a knight and attack the e5-pawn."),
                    new Step("b8c6", "2...Nc6 — Defend e5 while developing."),
                    new Step("f1c4", "3.Bc4 — The 'Italian' bishop eyes f7, Black's weakest square."),
                    new Step("f8c5", "3...Bc5 — Black develops actively: the Giuoco Piano."),
                    new Step("c2c3", "4.c3 — Prepare d4 to build a broad pawn center."),
                    new Step("g8f6", "4...Nf6 — Develop and pressure the e4-pawn.")
            ));

    private static final Lesson RUY_LOPEZ = new Lesson(
            "Ruy López",
            "The 'Spanish' — long-term pressure on Black's center.",
            List.of(
                    new Step("e2e4", "1.e4 — Take the center."),
                    new Step("e7e5", "1...e5 — Black stakes a central claim too."),
                    new Step("g1f3", "2.Nf3 — Develop and hit e5."),
                    new Step("b8c6", "2...Nc6 — Defend e5."),
                    new Step("f1b5", "3.Bb5 — The Ruy López: pin pressure on the defender of e5."),
                    new Step("a7a6", "3...a6 — The Morphy Defense, questioning the bishop."),
                    new Step("b5a4", "4.Ba4 — Keep the bishop on its strong diagonal."),
                    new Step("g8f6", "4...Nf6 — Develop and attack e4."),
                    new Step("e1g1", "5.O-O — Castle: king safety first.")
            ));

    private static final Lesson SICILIAN = new Lesson(
            "Sicilian Defense",
            "Black's most combative answer to 1.e4 — fight for the center asymmetrically.",
            List.of(
                    new Step("e2e4", "1.e4 — White grabs the center."),
                    new Step("c7c5", "1...c5 — The Sicilian: trade a wing pawn for a center pawn later."),
                    new Step("g1f3", "2.Nf3 — Develop toward the center."),
                    new Step("d7d6", "2...d6 — Support a future ...e5/...Nf6 and open the c8-bishop."),
                    new Step("d2d4", "3.d4 — Strike in the center."),
                    new Step("c5d4", "3...cxd4 — Black captures, gaining a central majority."),
                    new Step("f3d4", "4.Nxd4 — Recapture with the knight, well-centralized."),
                    new Step("g8f6", "4...Nf6 — Develop and attack e4."),
                    new Step("b1c3", "5.Nc3 — Defend e4 and develop: a standard Open Sicilian.")
            ));

    private static final Lesson QUEENS_GAMBIT = new Lesson(
            "Queen's Gambit",
            "A positional 1.d4 opening — pressure d5 to dominate the center.",
            List.of(
                    new Step("d2d4", "1.d4 — Claim the center with the queen's pawn."),
                    new Step("d7d5", "1...d5 — Black answers symmetrically."),
                    new Step("c2c4", "2.c4 — The Queen's Gambit: offer a pawn to deflect d5."),
                    new Step("e7e6", "2...e6 — The Declined: keep the center solid (frees the f8-bishop)."),
                    new Step("b1c3", "3.Nc3 — Add pressure to d5."),
                    new Step("g8f6", "3...Nf6 — Develop and defend d5."),
                    new Step("c1g5", "4.Bg5 — Pin the f6-knight, increasing pressure on d5.")
            ));

    private static final Lesson LONDON = new Lesson(
            "London System",
            "An easy-to-learn, solid setup you can play against almost anything.",
            List.of(
                    new Step("d2d4", "1.d4 — Take the center."),
                    new Step("d7d5", "1...d5 — A common reply."),
                    new Step("c1f4", "2.Bf4 — The London bishop comes out before locking it in with e3."),
                    new Step("g8f6", "2...Nf6 — Black develops."),
                    new Step("e2e3", "3.e3 — Solidify the center and open the f1-bishop."),
                    new Step("e7e6", "3...e6 — Black mirrors the solid setup."),
                    new Step("g1f3", "4.Nf3 — Develop toward a harmonious London structure."),
                    new Step("f8d6", "4...Bd6 — Black challenges the f4-bishop.")
            ));
}
