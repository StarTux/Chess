package com.cavetale.chess.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChessFile {
    FILE_A('a'),
    FILE_B('b'),
    FILE_C('c'),
    FILE_D('d'),
    FILE_E('e'),
    FILE_F('f'),
    FILE_G('g'),
    FILE_H('h');

    public final char letter;

    public static ChessFile get(int x) {
        return values()[x];
    }
}
