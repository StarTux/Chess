package com.cavetale.chess.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChessPieceType {
    PAWN("Pawn", 'P', 'p', 1, 8),
    KNIGHT("Knight", 'N', 'n', 3, 2),
    BISHOP("Bishop", 'B', 'b', 3, 2),
    ROOK("Rook", 'R', 'r', 5, 2),
    QUEEN("Queen", 'Q', 'q', 9, 1),
    KING("King", 'K', 'k', 0, 1);

    public final String humanName;
    public final char letter;
    public final char lowerLetter;
    public final int value;
    public final int initialAmount;

    public static ChessPieceType ofLetter(char l) {
        for (var it : values()) {
            if (l == it.letter) return it;
            if (l == it.lowerLetter) return it;
        }
        return null;
    }
}
