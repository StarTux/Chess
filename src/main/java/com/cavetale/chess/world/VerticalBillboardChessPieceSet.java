package com.cavetale.chess.world;

import com.cavetale.chess.board.ChessPiece;
import com.cavetale.chess.board.ChessSquare;
import java.util.List;
import org.bukkit.entity.Display.Billboard;

public final class VerticalBillboardChessPieceSet extends BillboardChessPieceSet {
    @Override
    public WorldChessPiece place(WorldChessBoard board, ChessSquare square, ChessPiece piece) {
        final var itemDisplay = spawnItemDisplay(getLocation(board, square), board, square, piece, e -> {
                e.setBillboard(Billboard.VERTICAL);
            });
        return new BillboardChessPiece(piece, List.of(itemDisplay), board);
    }
}
