# ChessMaster 

A full-featured and competitive chess app built entirely in Java using a **3D JavaFX UI**, including: a main menu, an orbiting 3D board with procedurally generated pieces, a guided "learn openings" mode and a **from-scratch chess engine** (no other chess libraries), live hints, blunder warnings, tactics puzzles and full game analysis with an evaluation graph.

---

## Screens

- **Main Menu** — Play, Learn Openings, Settings, Credits.
- **Play** — a full game versus the computer on the 3D board.
- **Learn Openings** — guided, move-by-move opening trials with explanation.
- **Settings** — board theme, piece colors, animation speed, default difficulty level, evaluation-bar and camera toggle options (saved to `~/.chessmaster.properties`).
- **Credits** — about the project and its author.

## Controls (3D board)

- **Click** a piece, then **click** its destination square to make a move (legal target squares are indicated by dots).
- **Drag** empty space to rotate the camera around the board; use the **scroll wheel** to zoom.
- Pieces smoothly glide across squares; captured pieces disappear.

## Features

- **3D board & pieces** rendered with JavaFX's 3D engine: perspective camera, lighting, materials, and smooth move animations. Procedurally generated pieces from primitives — no model files needed.
- **Play against the computer** at six different strength levels (Beginner → Master).
- **A from-scratch chess engine:** minimax with alpha-beta pruning, iterative deepening, a transposition table (Zobrist hashing), move ordering, quiescence search, and null-move pruning, with a tuned (PeSTO) evaluation.
- **Opening book** for fast, varied, theory-sound openings.
- **Live evaluation bar** and the engine's predicted best line, continuously updated as you play.
- **Hints** — click 💡 to see an arrow pointing to a strong move and why.
- **Blunder warnings** — an indication that your last move significantly hurt your position.
- **Tactics puzzles** — verified by the engine to ensure soundness.
- **Full game analysis** — each move evaluated (Best/Good/Inaccuracy/Mistake/Blunder) with an evaluation graph, similar to an online "game review".
- **Two-player (hotseat)** mode, board flip, undo, and **PGN export**.
- Complete legal-move rules: castling, en passant, promotion, check/checkmate/stalemate, 50-move rule, three-fold repetition, and insufficient material.

---

## Running It

### In IntelliJ IDEA (recommended)
1. **File → Open…** and select this folder (`ChessMaster`). IntelliJ recognizes the Maven project and downloads JavaFX automatically.
2. Open `src/main/java/com/chessmaster/ui/ChessApp.java` and click the green **▶ Run** arrow next to `main`.
   - If you experience a JavaFX module error when attempting to run it this way, instead execute the Maven goal: open the **Maven** tool window → **Plugins → javafx → javafx:run**.

### From the command line
```bash
mvn javafx:run      # launches the app
mvn test            # executes the test suite (rules + engine + puzzles)
```

Requires **JDK 24+** (developed on JDK 26) and Maven. JavaFX is pulled in via Maven — you do not need to install it separately.

---

## Organization of the Project

```
src/main/java/com/chessmaster/
├── model/      Rules of Chess (No AI or UI)
│   ├── Board, Piece, Move, Square, Color   Core representation
│   ├── MoveGenerator                       All possible legal moves
│   ├── Game, GameStatus, Notation          Game state, results, SAN/PGN
│   ├── Zobrist                             Hash positions
│   └── Perft                               Test for correct generation of moves
├── engine/     AI
│   ├── Evaluation         Evaluate positions (PeSTO tapered piece-square tables)
│   ├── Search             Negamax with alpha-beta + all search techniques
│   ├── TranspositionTable Cache previously searched positions
│   ├── OpeningBook        Mainstream opening theory
│   └── Engine             Difficulty levels + public interface
├── training/   Puzzles and Analysis
│   ├── Puzzle, PuzzleBank
│   └── Analyzer           Grade moves + create an evaluation curve
└── ui/         JavaFX Front-end
    ├── ChessApp           Entrance point + screen navigation (fade between screens)
    ├── MainMenuView / SettingsView / CreditsView   Menu & Information Screens
    ├── PlayView / LearnView   Play and Lesson Screens
    ├── BoardView          Interface implemented by both board renderers
    ├── Board3D            3D Board (Camera, Lighting, Picking, Animations)
    ├── PieceFactory3D     Procedurally Generated 3D Piece Models
    ├── BoardCanvas        Original 2D Board (Available)
    ├── ControlPanel       Side panel (controls, evaluation bar, move list)
    ├── GameController     Link between Model, Engine and Views
    ├── Settings           User Settings (Persisted)
    └── AnalysisView       Game Review Window
```

## Why You Can Trust The Engine

The move generator is verified using **perft**, which generates exactly the same number of nodes for each of the standard reference positions as published (e.g., the "Kiwipete" position to depth 4 = 4,085,603 nodes). The engine is tested on tactics puzzles it must solve (mate-in-one, winning material), and every built-in puzzle is verified for soundness by the engine. Execute `mvn test` to see all tests pass.

## Ideas for Taking it Further
- Use bitboard move generation for significant speed improvements.
- Refine the evaluation function with additional terms (king safety, passed pawns, mobility).
- Allow loading positions/games from PGN/FEN text files.
- Develop an opening name detector and opening trainer.
- Integrate a UCI engine (e.g., Stockfish) as an optional analysis backend.
- Implement online play over a network.
