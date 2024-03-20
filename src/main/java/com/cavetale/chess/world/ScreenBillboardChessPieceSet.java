package com.cavetale.chess.world;

import com.cavetale.chess.board.ChessPiece;
import com.cavetale.chess.board.ChessSquare;
import java.util.List;
import org.bukkit.entity.Display.Billboard;

public final class ScreenBillboardChessPieceSet extends BillboardChessPieceSet {
    @Override
    public WorldChessPiece place(WorldChessBoard board, ChessSquare square, ChessPiece piece) {
        final var location = getLocation(board, square).add(0.0, 0.015625, 0.0);
        final var itemDisplay = spawnItemDisplay(location, board, square, piece, e -> {
                e.setBillboard(Billboard.CENTER);
            });
        return new BillboardChessPiece(piece, List.of(itemDisplay), board);
    }
}
