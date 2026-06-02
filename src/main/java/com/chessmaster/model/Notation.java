package com.chessmaster.model;

/**
 * Converts moves to Standard Algebraic Notation (SAN) — the "Nf3", "exd5",
 * "O-O", "e8=Q+" form used on score sheets and in PGN files. SAN needs the
 * surrounding position to add capture marks, disambiguation, and check/mate
 * suffixes, so every method takes the {@link Board} the move is played from.
 */
public final class Notation {

    private Notation() {}

    public static String toSan(Board board, int move) {
        int flag = Move.flag(move);
        if (flag == Move.FLAG_CASTLE_KING)  return withCheckSuffix(board, move, "O-O");
        if (flag == Move.FLAG_CASTLE_QUEEN) return withCheckSuffix(board, move, "O-O-O");

        int from = Move.from(move);
        int to = Move.to(move);
        int piece = board.pieceAt(from);
        int type = Piece.type(piece);
        boolean capture = board.pieceAt(to) != Piece.NONE || Move.isEnPassant(move);

        StringBuilder sb = new StringBuilder();
        if (type == Piece.PAWN) {
            if (capture) sb.append((char) ('a' + Square.file(from))).append('x');
            sb.append(Square.name(to));
            if (Move.isPromotion(move)) {
                sb.append('=').append(Character.toUpperCase(Piece.toFenChar(Move.promotionType(move))));
            }
        } else {
            sb.append(Character.toUpperCase(Piece.toFenChar(piece)));
            sb.append(disambiguation(board, move, type, from, to));
            if (capture) sb.append('x');
            sb.append(Square.name(to));
        }
        return withCheckSuffix(board, move, sb.toString());
    }

    /** Figure out the minimal file/rank hint needed to identify the moving piece. */
    private static String disambiguation(Board board, int move, int type, int from, int to) {
        MoveList legal = MoveGenerator.generateLegal(board);
        boolean ambiguous = false, sameFile = false, sameRank = false;
        for (int i = 0; i < legal.size(); i++) {
            int m = legal.get(i);
            if (m == move) continue;
            if (Move.to(m) != to) continue;
            int other = Move.from(m);
            if (Piece.type(board.pieceAt(other)) != type) continue;
            ambiguous = true;
            if (Square.file(other) == Square.file(from)) sameFile = true;
            if (Square.rank(other) == Square.rank(from)) sameRank = true;
        }
        if (!ambiguous) return "";
        if (!sameFile) return String.valueOf((char) ('a' + Square.file(from)));
        if (!sameRank) return String.valueOf((char) ('1' + Square.rank(from)));
        return Square.name(from);
    }

    private static String withCheckSuffix(Board board, int move, String base) {
        board.makeMove(move);
        String suffix = "";
        if (board.inCheck(board.sideToMove())) {
            suffix = MoveGenerator.generateLegal(board).isEmpty() ? "#" : "+";
        }
        board.unmakeMove();
        return base + suffix;
    }
}
