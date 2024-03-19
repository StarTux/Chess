package com.cavetale.chess.board;

public record ChessMove(ChessSquare from, ChessSquare to, ChessPieceType promotion) {
    public ChessMove(final ChessSquare from, final ChessSquare to) {
        this(from, to, null);
    }

    @Override
    public String toString() {
        return from.name + to.name + (promotion != null ? "" + promotion.lowerLetter : "");
    }

    public static ChessMove fromString(String in) {
        if (in.length() != 4 && in.length() != 5) return null;
        final ChessSquare a = ChessSquare.ofName(in.substring(0, 2));
        if (a == null) return null;
        final ChessSquare b = ChessSquare.ofName(in.substring(2, 4));
        if (b == null) return null;
        final ChessPieceType p;
        if (in.length() >= 5) {
            p = ChessPieceType.ofLetter(in.charAt(4));
            if (p == null) return null;
        } else {
            p = null;
        }
        return new ChessMove(a, b, p);
    }
}
