package com.cavetale.chess.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChessPieceType {
    PAWN("Pawn", 'P', 1),
    KNIGHT("Knight", 'N', 3),
    BISHOP("Bishop", 'B', 3),
    ROOK("Rook", 'R', 5),
    QUEEN("Queen", 'Q', 9),
    KING("King", 'K', 0);

    public final String humanName;
    public final char letter;
    public final int value;

    public static ChessPieceType ofLetter(char l) {
        for (var it : values()) {
            if (l == it.letter) return it;
        }
        return null;
    }
}
