package com.cavetale.chess.world;

import com.cavetale.chess.board.ChessPiece;
import com.cavetale.chess.board.ChessSquare;

public interface ChessPieceSet {
    boolean canSupport(WorldChessBoard board);

    WorldChessPiece place(WorldChessBoard board, ChessSquare square, ChessPiece piece);
}
