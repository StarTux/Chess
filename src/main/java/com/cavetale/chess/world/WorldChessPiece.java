package com.cavetale.chess.world;

import com.cavetale.chess.board.ChessSquare;

public interface WorldChessPiece {
    void remove();

    void move(ChessSquare square);
}
