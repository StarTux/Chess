package com.cavetale.chess.world;

import com.cavetale.chess.board.ChessColor;
import com.cavetale.chess.board.ChessPiece;
import com.cavetale.chess.board.ChessSquare;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import static com.cavetale.mytems.util.Entities.setTransient;

public final class ItemFrameChessPieceSet implements ChessPieceSet {
    @Override
    public boolean canSupport(WorldChessBoard board) {
        return true;
    }

    @Override
    public WorldChessPiece place(WorldChessBoard board, ChessSquare square, ChessPiece piece) {
        final var location = getLocation(board, square);
        final var itemFrame = location.getWorld().spawn(location, ItemFrame.class, e -> {
                e.setPersistent(false);
                setTransient(e);
                e.setFixed(true);
                e.setVisible(false);
                e.setItem(piece.getMytems().createIcon());
                e.setFacingDirection(board.getFaceBoardOrtho());
                final var rotation = switch (board.getFaceBoardY()) {
                case EAST -> Rotation.CLOCKWISE;
                case WEST -> Rotation.COUNTER_CLOCKWISE;
                case SOUTH -> Rotation.FLIPPED;
                default -> Rotation.NONE;
                };
                e.setRotation(board.getFacingAxis() == Axis.Y && piece.color == ChessColor.BLACK
                              ? rotation.rotateClockwise().rotateClockwise().rotateClockwise().rotateClockwise()
                              : rotation);
            });
        EntityChessPiece.markAsChessPiece(itemFrame);
        return new ItemFramePiece(piece, itemFrame, board);
    }

    public static Location getLocation(WorldChessBoard board, ChessSquare square) {
        final var face = board.getFaceBoardOrtho();
        final var location = board.getCenterLocation(square);
        location.add(face.getModX() * 0.5, face.getModY() * 0.5, face.getModZ() * 0.5);
        // Remember to set the facing direction when teleporting item
        // frames!
        location.setDirection(face.getDirection());
        return location;
    }

    @Data
    @RequiredArgsConstructor
    private static final class ItemFramePiece implements EntityChessPiece {
        private final ChessPiece chessPiece;
        private final ItemFrame itemFrame;
        private final WorldChessBoard board;

        @Override
        public List<Entity> getEntities() {
            return List.of(itemFrame);
        }

        @Override
        public void move(ChessSquare square) {
            itemFrame.teleport(getLocation(board, square));
        }

        @Override
        public void remove() {
            itemFrame.remove();
        }

        @Override
        public void explode() { }
    }
}
