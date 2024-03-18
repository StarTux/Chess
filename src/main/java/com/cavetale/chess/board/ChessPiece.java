package com.cavetale.chess.board;

import com.cavetale.mytems.Mytems;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChessPiece {
    WHITE_PAWN(Mytems.WHITE_PAWN, ChessColor.WHITE, ChessPieceType.PAWN, 'P', '\u2659'),
    WHITE_KNIGHT(Mytems.WHITE_KNIGHT, ChessColor.WHITE, ChessPieceType.KNIGHT, 'N', '\u2658'),
    WHITE_BISHOP(Mytems.WHITE_BISHOP, ChessColor.WHITE, ChessPieceType.BISHOP, 'B', '\u2657'),
    WHITE_ROOK(Mytems.WHITE_ROOK, ChessColor.WHITE, ChessPieceType.ROOK, 'R', '\u2656'),
    WHITE_QUEEN(Mytems.WHITE_QUEEN, ChessColor.WHITE, ChessPieceType.QUEEN, 'Q', '\u2655'),
    WHITE_KING(Mytems.WHITE_KING, ChessColor.WHITE, ChessPieceType.KING, 'K', '\u2654'),
    BLACK_PAWN(Mytems.BLACK_PAWN, ChessColor.BLACK, ChessPieceType.PAWN, 'p', '\u265F'),
    BLACK_KNIGHT(Mytems.BLACK_KNIGHT, ChessColor.BLACK, ChessPieceType.KNIGHT, 'n', '\u265E'),
    BLACK_BISHOP(Mytems.BLACK_BISHOP, ChessColor.BLACK, ChessPieceType.BISHOP, 'b', '\u265D'),
    BLACK_ROOK(Mytems.BLACK_ROOK, ChessColor.BLACK, ChessPieceType.ROOK, 'r', '\u265C'),
    BLACK_QUEEN(Mytems.BLACK_QUEEN, ChessColor.BLACK, ChessPieceType.QUEEN, 'q', '\u265B'),
    BLACK_KING(Mytems.BLACK_KING, ChessColor.BLACK, ChessPieceType.KING, 'k', '\u265A');

    public final Mytems mytems;
    public final ChessColor color;
    public final ChessPieceType type;
    public final char fenChar;
    public final char unicodeSymbol;

    public static ChessPiece ofFenChar(char c) {
        for (var it : values()) {
            if (c == it.fenChar) return it;
        }
        return null;
    }

    public static ChessPiece of(ChessColor color, ChessPieceType type) {
        for (var it : values()) {
            if (it.color == color && it.type == type) return it;
        }
        return null;
    }
}
