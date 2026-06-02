# ♚ ChessMaster — 3D Edition

A complete, competitive chess application built from scratch in Java with a
**3D JavaFX interface** — a main menu, an orbiting 3D board with procedurally
modeled pieces, a guided "learn openings" mode, plus a **from-scratch chess
engine** (no external chess libraries), live hints, blunder warnings, tactics
puzzles, and full-game analysis with an evaluation graph.

Everything in the rules layer, the engine, and the 3D pieces is *your own code* —
which is exactly what makes this an impressive project to show off.

---

## Screens

- **Main Menu** — Play · Learn Openings · Settings · Credits.
- **Play** — a full game vs the computer on the 3D board.
- **Learn Openings** — guided, move-by-move opening trials with explanations.
- **Settings** — board theme, piece colors, animation speed, default difficulty,
  evaluation-bar and camera toggles (saved to `~/.chessmaster.properties`).
- **Credits** — about the project (edit your name in `CreditsView.java`).

## Controls (3D board)

- **Click** a piece, then **click** a destination to move (legal targets are dotted).
- **Drag** empty space to **orbit** the camera; **scroll** to **zoom**.
- Pieces glide between squares; captures fade out.

## Features

- **3D board & pieces** rendered with JavaFX's 3D engine: perspective camera,
  lighting, materials, and smooth move animations. Pieces are built procedurally
  from primitives — no model files required.
- **Play vs the computer** at six strength levels (Beginner → Master).
- **A real chess engine** you wrote: minimax with alpha-beta pruning, iterative
  deepening, a transposition table (Zobrist hashing), move ordering, quiescence
  search, and null-move pruning, with a tuned (PeSTO) evaluation.
- **Opening book** for fast, varied, theory-sound openings.
- **Live evaluation bar** + the engine's best line, updated as you play.
- **Hints** — press 💡 to get an arrow showing a strong move and why.
- **Blunder warnings** — a heads-up when your last move dropped a lot of ground.
- **Tactics puzzles** — engine-validated, so they're always sound.
- **Full-game analysis** — every move graded (Best/Good/Inaccuracy/Mistake/
  Blunder) with an evaluation graph, like an online "game review".
- **Two-player (hotseat)** mode, board flip, undo, and **PGN export**.
- Full legal-move rules: castling, en passant, promotion, check/checkmate/
  stalemate, the 50-move rule, threefold repetition, and insufficient material.

---

## Running it

### In IntelliJ IDEA (recommended)
1. **File → Open…** and select this folder (`ChessMaster`). IntelliJ detects the
   Maven project and downloads JavaFX automatically.
2. Open `src/main/java/com/chessmaster/ui/ChessApp.java` and click the green
   **▶ Run** arrow next to `main`.
   - If you hit a JavaFX module error running it that way, instead run the Maven
     goal: open the **Maven** tool window → **Plugins → javafx → javafx:run**.

### From the command line
```bash
mvn javafx:run      # launch the app
mvn test            # run the test suite (rules + engine + puzzles)
```

Requires **JDK 24+** (developed on JDK 26) and Maven. JavaFX itself is pulled in
by Maven — you don't install it separately.

---

## How it's organized

```
src/main/java/com/chessmaster/
├── model/      The rules of chess (no AI, no UI)
│   ├── Board, Piece, Move, Square, Color   core representation
│   ├── MoveGenerator                       all legal moves
│   ├── Game, GameStatus, Notation          game state, results, SAN/PGN
│   ├── Zobrist                             position hashing
│   └── Perft                               move-generation correctness test
├── engine/     The AI
│   ├── Evaluation        scores a position (PeSTO tapered piece-square tables)
│   ├── Search            negamax + alpha-beta + all the search tricks
│   ├── TranspositionTable cache of searched positions
│   ├── OpeningBook       mainline opening theory
│   └── Engine            difficulty levels + the public API
├── training/   Puzzles and analysis
│   ├── Puzzle, PuzzleBank
│   └── Analyzer          per-move grading + eval curve
└── ui/         JavaFX front-end
    ├── ChessApp          entry point + screen navigation (fades between screens)
    ├── MainMenuView / SettingsView / CreditsView   menu & info screens
    ├── PlayView / LearnView   the play and guided-lesson screens
    ├── BoardView         interface implemented by both board renderers
    ├── Board3D           the 3D board (camera, lighting, picking, animations)
    ├── PieceFactory3D     procedural 3D piece models
    ├── BoardCanvas        the original 2D board (still available)
    ├── ControlPanel      side panel (controls, eval bar, move list)
    ├── GameController     glue between model, engine, and views
    ├── Settings           persisted user settings
    └── AnalysisView       the game-review window
```

## Why you can trust the engine

The move generator is verified with **perft**, it produces exactly the
published node counts for the standard reference positions (e.g. the "Kiwipete"
position to depth 4 = 4,085,603 nodes). The engine is tested on tactics it must
solve (mate-in-one, winning material), and every built-in puzzle is checked for
soundness by the engine. Run `mvn test` to see all of it pass.

## Ideas to take it further (great for a competition write-up)
- Add bitboard move generation for a big speed boost.
- Tune the evaluation with more terms (king safety, passed pawns, mobility).
- Support loading positions/games from PGN/FEN text.
- Add an opening-name detector and an opening trainer.
- Plug in a UCI engine (e.g. Stockfish) as an optional analysis backend.
- Online play over a network.
