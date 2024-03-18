package com.cavetale.chess.world;

import com.cavetale.chess.board.ChessPiece;
import com.cavetale.chess.board.ChessSquare;
import java.util.List;

public final class XBillboardChessPieceSet extends BillboardChessPieceSet {
    @Override
    public WorldChessPiece place(WorldChessBoard board, ChessSquare square, ChessPiece piece) {
        final var location = getLocation(board, square);
        final var a = spawnItemDisplay(location, board, square, piece, e -> { });
        location.setYaw(90f);
        final var b = spawnItemDisplay(location, board, square, piece, e -> { });
        return new BillboardChessPiece(piece, List.of(a, b), board);
    }
}
