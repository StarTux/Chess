package com.cavetale.chess.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChessRank {
    RANK_1('1'),
    RANK_2('2'),
    RANK_3('3'),
    RANK_4('4'),
    RANK_5('5'),
    RANK_6('6'),
    RANK_7('7'),
    RANK_8('8');

    public final char digit;

    public static ChessRank get(int y) {
        return values()[y];
    }

    public static ChessRank ofDigit(char c) {
        for (var it : values()) {
            if (it.digit == c) return it;
        }
        return null;
    }
}
