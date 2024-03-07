package com.cavetale.chess.board;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

@Getter
@RequiredArgsConstructor
public enum ChessColor {
    WHITE("White", 'w'),
    BLACK("Black", 'b');

    public final String humanName;
    public final char fenChar;

    public static ChessColor ofFenChar(char c) {
        for (var it : values()) {
            if (it.fenChar == c) return it;
        }
        return null;
    }

    public ChessColor other() {
        return this == WHITE
            ? BLACK
            : WHITE;
    }
}
