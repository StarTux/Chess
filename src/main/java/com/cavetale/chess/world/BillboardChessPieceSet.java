package com.cavetale.chess.world;

import com.cavetale.chess.board.ChessPiece;
import com.cavetale.chess.board.ChessPieceType;
import com.cavetale.chess.board.ChessSquare;
import java.util.List;
import java.util.function.Consumer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import static com.cavetale.mytems.util.Entities.setTransient;

public abstract class BillboardChessPieceSet implements ChessPieceSet {
    @Override
    public final boolean canSupport(WorldChessBoard board) {
        return board.getFacingAxis() == Axis.Y;
    }

    protected final ItemDisplay spawnItemDisplay(Location location, WorldChessBoard board, ChessSquare square, ChessPiece piece, Consumer<ItemDisplay> callback) {
        final var itemDisplay = location.getWorld().spawn(location, ItemDisplay.class, e -> {
                e.setPersistent(false);
                setTransient(e);
                e.setItemStack(piece.getMytems().createIcon());
                final float sy = piece.type == ChessPieceType.KING || piece.type == ChessPieceType.QUEEN
                    ? 1f
                    : (piece.type == ChessPieceType.PAWN
                       ? 0.5f
                       : 0.75f);
                final float scalef = sy * (float) Math.min(board.getLengthBoardX(), board.getLengthBoardY());
                final var translation = new Vector3f(0f, 0.5f * scalef, 0f);
                final var leftRotation = WorldChessBoard.AXISANGLE4F_ZERO;
                final var rightRotation = WorldChessBoard.AXISANGLE4F_FLIP;
                final var scale = new Vector3f(scalef, scalef, 0f);
                e.setTransformation(new Transformation(translation, leftRotation, scale, rightRotation));
                callback.accept(e);
            });
        EntityChessPiece.markAsChessPiece(itemDisplay);
        return itemDisplay;
    }

    public static Location getLocation(WorldChessBoard board, ChessSquare square) {
        return board.getCenterLocation(square).add(0.0, 0.015625, 0.0);
    }

    @Data
    @RequiredArgsConstructor
    protected static final class BillboardChessPiece implements EntityChessPiece {
        private final ChessPiece chessPiece;
        private final List<ItemDisplay> itemDisplays;
        private final WorldChessBoard board;

        @Override
        public List<ItemDisplay> getEntities() {
            return itemDisplays;
        }

        @Override
        public void move(ChessSquare square) {
            final var location = getLocation(board, square);
            for (var it : itemDisplays) {
                it.teleport(location);
            }
        }

        @Override
        public void remove() {
            for (var it : itemDisplays) {
                it.remove();
            }
        }

        @Override
        public void explode() { }
    }
}
