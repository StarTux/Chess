package com.cavetale.chess.world;

import com.cavetale.chess.board.ChessPiece;
import com.cavetale.chess.board.ChessSquare;

public interface WorldChessPiece {
    ChessPiece getChessPiece();

    void remove();

    void move(ChessSquare square);

    void explode();
}
