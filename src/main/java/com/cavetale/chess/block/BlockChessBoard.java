package com.cavetale.chess.block;

import com.cavetale.chess.board.ChessBoard;
import com.cavetale.core.struct.Cuboid;
import lombok.RequiredArgsConstructor;
import org.bukkit.Axis;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import static com.cavetale.chess.ChessPlugin.plugin;

@RequiredArgsConstructor
public final class BlockChessBoard {
    // Identifiers
    private final World world;
    private final String name;
    // Place and orientation
    private final Cuboid boardArea;
    private final Cuboid a1;
    private final Axis facingAxis;
    private final BlockFace faceBoardX; // Right on the board
    private final BlockFace faceBoardY; // Up on the board
    private final BlockFace faceBoardOrtho; // Orthogonal to the board surface
    // Game
    private final ChessBoard board = new ChessBoard();

    public BlockChessBoard(final World world, final String name, final Cuboid boardArea, final Cuboid a1) {
        this.world = world;
        this.name = name;
        this.boardArea = boardArea;
        this.a1 = a1;
        final boolean matchNorth = boardArea.ax == a1.ax;
        final boolean matchSouth = boardArea.bx == a1.bx;
        final boolean matchDown = boardArea.ay == a1.ay;
        final boolean matchUp = boardArea.by == a1.by;
        final boolean matchWest = boardArea.az == a1.az;
        final boolean matchEast = boardArea.bz == a1.bz;
        if (matchUp && matchDown) {
            // Horizontal boards always face up.
            // Unless we want hanging boards some day...
            facingAxis = Axis.Y;
            faceBoardOrtho = BlockFace.UP;
            // Based on the a1 square, we can determine the 2
            // remaining directions, although it is a bit of a mind
            // game.
            if (matchNorth && matchWest) {
                faceBoardX = BlockFace.SOUTH;
                faceBoardY = BlockFace.EAST;
            } else if (matchSouth && matchWest) {
                faceBoardX = BlockFace.EAST;
                faceBoardY = BlockFace.NORTH;
            } else if (matchSouth && matchEast) {
                faceBoardX = BlockFace.NORTH;
                faceBoardY = BlockFace.WEST;
            } else if (matchNorth && matchEast) {
                faceBoardX = BlockFace.WEST;
                faceBoardY = BlockFace.SOUTH;
            } else {
                throw new IllegalArgumentException("Illegal horizontal board and a1 area configuration:"
                                                   + " " + boardArea
                                                   + " " + a1
                                                   + " n:" + matchNorth
                                                   + " s:" + matchSouth
                                                   + " w:" + matchWest
                                                   + " e:" + matchEast);
            }
        } else if (matchNorth && matchSouth) {
            // Up against a wall, facing North or South
            facingAxis = Axis.Z;
            faceBoardY = BlockFace.UP;
            if (matchDown && matchWest) {
                faceBoardX = BlockFace.EAST;
                faceBoardOrtho = BlockFace.SOUTH;
            } else if (matchDown && matchEast) {
                faceBoardX = BlockFace.WEST;
                faceBoardOrtho = BlockFace.NORTH;
            } else {
                throw new IllegalArgumentException("Illegal vertical Z-facing board and a1 area configuration:"
                                                   + " " + boardArea
                                                   + " " + a1
                                                   + " w:" + matchWest
                                                   + " e:" + matchEast
                                                   + " d:" + matchDown
                                                   + " u:" + matchUp);
            }
        } else if (matchWest && matchEast) {
            // Up against a wall, facing West or East
            facingAxis = Axis.X;
            faceBoardY = BlockFace.UP;
            if (matchDown && matchNorth) {
                faceBoardX = BlockFace.SOUTH;
                faceBoardOrtho = BlockFace.WEST;
            } else if (matchDown && matchSouth) {
                faceBoardX = BlockFace.NORTH;
                faceBoardOrtho = BlockFace.EAST;
            } else {
                throw new IllegalArgumentException("Illegal vertical X-facing board and a1 area configuration:"
                                                   + " " + boardArea
                                                   + " " + a1
                                                   + " n:" + matchNorth
                                                   + " S:" + matchSouth
                                                   + " d:" + matchDown
                                                   + " u:" + matchUp);
            }
        } else {
                throw new IllegalArgumentException("Illegal board and a1 area configuration:"
                                                   + " " + boardArea
                                                   + " " + a1
                                                   + " n:" + matchNorth
                                                   + " s:" + matchSouth
                                                   + " w:" + matchWest
                                                   + " e:" + matchEast
                                                   + " d:" + matchDown
                                                   + " u:" + matchUp);
        }
        plugin().getLogger().info("Board created: " + world.getName() + "/" + name
                                  + " " + boardArea
                                  + " " + a1
                                  + " a:" + facingAxis
                                  + " x:" + faceBoardX
                                  + " y:" + faceBoardY
                                  + " o:" + faceBoardOrtho
                                  + " n:" + matchNorth
                                  + " s:" + matchSouth
                                  + " w:" + matchWest
                                  + " e:" + matchEast
                                  + " d:" + matchDown
                                  + " u:" + matchUp);
    }
}
