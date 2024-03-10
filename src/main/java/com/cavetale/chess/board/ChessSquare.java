package com.cavetale.chess.board;

import lombok.Getter;

@Getter
public enum ChessSquare {
    A1(ChessFile.FILE_A, ChessRank.RANK_1),
    B1(ChessFile.FILE_B, ChessRank.RANK_1),
    C1(ChessFile.FILE_C, ChessRank.RANK_1),
    D1(ChessFile.FILE_D, ChessRank.RANK_1),
    E1(ChessFile.FILE_E, ChessRank.RANK_1),
    F1(ChessFile.FILE_F, ChessRank.RANK_1),
    G1(ChessFile.FILE_G, ChessRank.RANK_1),
    H1(ChessFile.FILE_H, ChessRank.RANK_1),
    A2(ChessFile.FILE_A, ChessRank.RANK_2),
    B2(ChessFile.FILE_B, ChessRank.RANK_2),
    C2(ChessFile.FILE_C, ChessRank.RANK_2),
    D2(ChessFile.FILE_D, ChessRank.RANK_2),
    E2(ChessFile.FILE_E, ChessRank.RANK_2),
    F2(ChessFile.FILE_F, ChessRank.RANK_2),
    G2(ChessFile.FILE_G, ChessRank.RANK_2),
    H2(ChessFile.FILE_H, ChessRank.RANK_2),
    A3(ChessFile.FILE_A, ChessRank.RANK_3),
    B3(ChessFile.FILE_B, ChessRank.RANK_3),
    C3(ChessFile.FILE_C, ChessRank.RANK_3),
    D3(ChessFile.FILE_D, ChessRank.RANK_3),
    E3(ChessFile.FILE_E, ChessRank.RANK_3),
    F3(ChessFile.FILE_F, ChessRank.RANK_3),
    G3(ChessFile.FILE_G, ChessRank.RANK_3),
    H3(ChessFile.FILE_H, ChessRank.RANK_3),
    A4(ChessFile.FILE_A, ChessRank.RANK_4),
    B4(ChessFile.FILE_B, ChessRank.RANK_4),
    C4(ChessFile.FILE_C, ChessRank.RANK_4),
    D4(ChessFile.FILE_D, ChessRank.RANK_4),
    E4(ChessFile.FILE_E, ChessRank.RANK_4),
    F4(ChessFile.FILE_F, ChessRank.RANK_4),
    G4(ChessFile.FILE_G, ChessRank.RANK_4),
    H4(ChessFile.FILE_H, ChessRank.RANK_4),
    A5(ChessFile.FILE_A, ChessRank.RANK_5),
    B5(ChessFile.FILE_B, ChessRank.RANK_5),
    C5(ChessFile.FILE_C, ChessRank.RANK_5),
    D5(ChessFile.FILE_D, ChessRank.RANK_5),
    E5(ChessFile.FILE_E, ChessRank.RANK_5),
    F5(ChessFile.FILE_F, ChessRank.RANK_5),
    G5(ChessFile.FILE_G, ChessRank.RANK_5),
    H5(ChessFile.FILE_H, ChessRank.RANK_5),
    A6(ChessFile.FILE_A, ChessRank.RANK_6),
    B6(ChessFile.FILE_B, ChessRank.RANK_6),
    C6(ChessFile.FILE_C, ChessRank.RANK_6),
    D6(ChessFile.FILE_D, ChessRank.RANK_6),
    E6(ChessFile.FILE_E, ChessRank.RANK_6),
    F6(ChessFile.FILE_F, ChessRank.RANK_6),
    G6(ChessFile.FILE_G, ChessRank.RANK_6),
    H6(ChessFile.FILE_H, ChessRank.RANK_6),
    A7(ChessFile.FILE_A, ChessRank.RANK_7),
    B7(ChessFile.FILE_B, ChessRank.RANK_7),
    C7(ChessFile.FILE_C, ChessRank.RANK_7),
    D7(ChessFile.FILE_D, ChessRank.RANK_7),
    E7(ChessFile.FILE_E, ChessRank.RANK_7),
    F7(ChessFile.FILE_F, ChessRank.RANK_7),
    G7(ChessFile.FILE_G, ChessRank.RANK_7),
    H7(ChessFile.FILE_H, ChessRank.RANK_7),
    A8(ChessFile.FILE_A, ChessRank.RANK_8),
    B8(ChessFile.FILE_B, ChessRank.RANK_8),
    C8(ChessFile.FILE_C, ChessRank.RANK_8),
    D8(ChessFile.FILE_D, ChessRank.RANK_8),
    E8(ChessFile.FILE_E, ChessRank.RANK_8),
    F8(ChessFile.FILE_F, ChessRank.RANK_8),
    G8(ChessFile.FILE_G, ChessRank.RANK_8),
    H8(ChessFile.FILE_H, ChessRank.RANK_8);

    public final ChessFile file;
    public final ChessRank rank;
    public final int x;
    public final int y;
    public final String name;
    public final ChessColor color;

    ChessSquare(final ChessFile file, final ChessRank rank) {
        this.file = file;
        this.rank = rank;
        this.x = file.ordinal();
        this.y = rank.ordinal();
        this.name = "" + file.letter + rank.digit;
        this.color = (x & 1) == (y & 1) ? ChessColor.BLACK : ChessColor.WHITE;
    }

    public static ChessSquare ofName(String n) {
        for (var it : values()) {
            if (n.equals(it.name)) return it;
        }
        return null;
    }

    public static ChessSquare of(ChessFile f, ChessRank r) {
        return values()[f.ordinal() + 8 * r.ordinal()];
    }

    public static ChessSquare at(int x, int y) {
        if (x < 0 || x > 7) {
            throw new IllegalArgumentException("x=" + x);
        }
        if (y < 0 || y > 7) {
            throw new IllegalArgumentException("y=" + x);
        }
        return values()[x + 8 * y];
    }

    public ChessSquare relative(int dx, int dy) {
        return at(x + dx, y + dy);
    }
}
