package com.cavetale.chess.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChessPieceType {
    PAWN("Pawn", 'P', 1, 8),
    KNIGHT("Knight", 'N', 3, 2),
    BISHOP("Bishop", 'B', 3, 2),
    ROOK("Rook", 'R', 5, 2),
    QUEEN("Queen", 'Q', 9, 1),
    KING("King", 'K', 0, 1);

    public final String humanName;
    public final char letter;
    public final int value;
    public final int initialAmount;

    public static ChessPieceType ofLetter(char l) {
        for (var it : values()) {
            if (l == it.letter) return it;
        }
        return null;
    }
}
