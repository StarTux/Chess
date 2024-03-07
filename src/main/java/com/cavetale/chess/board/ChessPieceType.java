package com.cavetale.chess.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChessPieceType {
    PAWN("Pawn", 'P'),
    KNIGHT("Knight", 'N'),
    BISHOP("Bishop", 'B'),
    ROOK("Rook", 'R'),
    QUEEN("Queen", 'Q'),
    KING("King", 'K');

    public final String humanName;
    public final char letter;

    public static ChessPieceType ofLetter(char l) {
        for (var it : values()) {
            if (l == it.letter) return it;
        }
        return null;
    }
}
