package com.cavetale.chess.world;

import com.cavetale.chess.ai.DummyAI;
import com.cavetale.chess.board.ChessBoard;
import com.cavetale.chess.board.ChessColor;
import com.cavetale.chess.board.ChessGame;
import com.cavetale.chess.board.ChessMove;
import com.cavetale.chess.board.ChessPiece;
import com.cavetale.chess.board.ChessSquare;
import com.cavetale.chess.board.ChessTurnState;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.util.Json;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.font.Glyph;
import com.cavetale.mytems.util.Gui;
import com.cavetale.mytems.util.Items;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Axis;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.chess.ChessPlugin.plugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * Represent one board in a world.  Boards can be asleep if their
 * chunks are not loaded.  The Worlds class manages instances of
 * WorldChessBoard and helps determining loaded or unloaded chunks.
 */
@Data
@RequiredArgsConstructor
public final class WorldChessBoard {
    // Identifiers
    private final World world;
    private final String name;
    // Place and orientation
    private final Cuboid boardArea;
    private final Cuboid a1;
    private final Axis facingAxis;
    private final BlockFace faceBoardX; // Right on the board
    private final int lengthBoardX;
    private final BlockFace faceBoardY; // Up on the board
    private final int lengthBoardY;
    private final BlockFace faceBoardOrtho; // Orthogonal to the board surface
    private final Cuboid perimeter;
    // Game
    private ChessGame game;
    private final Map<ChessSquare, WorldChessPiece> pieces = new EnumMap<>(ChessSquare.class);
    private final Map<ChessSquare, Cuboid> squares = new EnumMap<>(ChessSquare.class);
    // Wake state
    private Set<Vec2i> chunks = new HashSet<>();
    private boolean awake;
    private final DefaultEntityChessPieceSet pieceSet = new DefaultEntityChessPieceSet();
    private ChessSaveTag saveTag;
    // Move selection
    private ChessSquare moveFrom;
    private final List<ChessSquare> legalTargets = new ArrayList<>();

    public WorldChessBoard(final World world, final String name, final Cuboid boardArea, final Cuboid a1) {
        this.world = world;
        this.name = name;
        this.boardArea = boardArea;
        this.a1 = a1;
        final boolean matchNorth = boardArea.az == a1.az;
        final boolean matchSouth = boardArea.bz == a1.bz;
        final boolean matchDown = boardArea.ay == a1.ay;
        final boolean matchUp = boardArea.by == a1.by;
        final boolean matchWest = boardArea.ax == a1.ax;
        final boolean matchEast = boardArea.bx == a1.bx;
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
                lengthBoardX = a1.getSizeZ();
                lengthBoardY = a1.getSizeX();
            } else if (matchSouth && matchWest) {
                faceBoardX = BlockFace.EAST;
                faceBoardY = BlockFace.NORTH;
                lengthBoardX = a1.getSizeX();
                lengthBoardY = a1.getSizeZ();
            } else if (matchSouth && matchEast) {
                faceBoardX = BlockFace.NORTH;
                faceBoardY = BlockFace.WEST;
                lengthBoardX = a1.getSizeZ();
                lengthBoardY = a1.getSizeX();
            } else if (matchNorth && matchEast) {
                faceBoardX = BlockFace.WEST;
                faceBoardY = BlockFace.SOUTH;
                lengthBoardX = a1.getSizeX();
                lengthBoardY = a1.getSizeZ();
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
            lengthBoardX = a1.getSizeX();
            lengthBoardY = a1.getSizeY();
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
            lengthBoardX = a1.getSizeZ();
            lengthBoardY = a1.getSizeY();
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
        // Chunks
        final var chunkCuboid = boardArea.blockToChunk();
        for (int z = chunkCuboid.az; z <= chunkCuboid.bz; z += 1) {
            for (int x = chunkCuboid.ax; x <= chunkCuboid.bx; x += 1) {
                chunks.add(new Vec2i(x, z));
            }
        }
        // Squares
        for (var square : ChessSquare.values()) {
            final int dx = faceBoardX.getModX() * lengthBoardX * square.x
                + faceBoardY.getModX() * lengthBoardY * square.y;
            final int dy = faceBoardX.getModY() * lengthBoardX * square.x
                + faceBoardY.getModY() * lengthBoardY * square.y;
            final int dz = faceBoardX.getModZ() * lengthBoardX * square.x
                + faceBoardY.getModZ() * lengthBoardY * square.y;
            squares.put(square, a1.shift(dx, dy, dz));
        }
        // Finish
        this.perimeter = boardArea.outset(16, 16, 16);
        plugin().getLogger().info("Board created: " + getBoardId()
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
                                  + " u:" + matchUp
                                  + " c:" + chunks);
    }

    public List<Player> getPlayersInPerimeter() {
        final var list = new ArrayList<Player>();
        for (Player player : world.getPlayers()) {
            if (!perimeter.contains(player.getLocation())) continue;
            list.add(player);
        }
        return list;
    }

    public void announce(Component text) {
        for (var player : getPlayersInPerimeter()) {
            player.sendMessage(text);
        }
    }

    public void load() {
        saveTag = Json.load(getSaveFile(), ChessSaveTag.class, ChessSaveTag::new);
        game = new ChessGame();
        if (saveTag.getPgnString() != null) {
            game.loadPgnString(saveTag.getPgnString());
        } else {
            game.initialize();
        }
    }

    public void save() {
        if (saveTag == null) return;
        saveTag.setPgnString(game.toPgnString());
        Json.save(getSaveFile(), saveTag, true);
    }

    public Location getCenterLocation(ChessSquare square) {
        final Cuboid squareCuboid = squares.get(square);
        final var vector = squareCuboid.getFaceCenterExact(faceBoardOrtho);
        return vector.toLocation(world);
    }

    public void clearPieces() {
        for (var piece : pieces.values()) {
            piece.remove();
        }
        pieces.clear();
    }

    public String getBoardId() {
        return world.getName() + "/" + name;
    }

    public void spawnAllPieces() {
        if (!awake) return;
        final var board = game.getCurrentBoard();
        for (var square : ChessSquare.values()) {
            final var old = pieces.remove(square);
            if (old != null) old.remove();
            final var piece = board.getPieceAt(square);
            if (piece == null) continue;
            final var placed = pieceSet.place(this, square, piece);
            if (placed == null) continue;
            pieces.put(square, placed);
        }
    }

    /**
     * Wake up if all chunks are loaded.
     * @return true if we are now awake, regardless of original state
     */
    public boolean tryToWakeUp() {
        if (awake) return true;
        if (!allChunksAreLoaded()) return false;
        awake = true;
        load();
        spawnAllPieces();
        plugin().getLogger().info("[WorldChessBoard] " + getBoardId() + " woke up");
        return true;
    }

    /**
     * Fall asleep if not already asleep.
     * @return true if we were asleep and are now sleeping, false
     *   otherwise.
     */
    public boolean fallAsleep() {
        if (!awake) return false;
        awake = false;
        save();
        clearPieces();
        plugin().getLogger().info("[WorldChessBoard] " + getBoardId() + " fell asleep");
        return true;
    }

    /**
     * Fall asleep if any chunks are not loaded.
     * @return true if we did fall asleep, false otherwise
     */
    public boolean fallAsleepIfNecessary() {
        if (!awake) return false;
        if (allChunksAreLoaded()) return false;
        fallAsleep();
        return true;
    }

    public boolean allChunksAreLoaded() {
        for (var vec : chunks) {
            if (!world.isChunkLoaded(vec.x, vec.z)) return false;
            final var chunk = world.getChunkAt(vec.x, vec.z);
            if (chunk.getLoadLevel() != Chunk.LoadLevel.ENTITY_TICKING) return false;
        }
        return true;
    }

    /**
     * Tick this board.  Called by Worlds if this board is awake.
     */
    protected void tick() {
        switch (saveTag.getState()) {
        case GAME:
            final var player = saveTag.getPlayer(game.getCurrentBoard().getActiveColor());
            if (player.isCpu()) {
                if (player.getMoveSeconds() < 3) return;
                final var move = new DummyAI().getBestMove(game);
                move(move);
            } else {
                final var entity = player.getPlayer();
                if (entity != null && perimeter.contains(entity.getLocation()) && !legalTargets.isEmpty()) {
                    for (var target : legalTargets) {
                        Location location = getCenterLocation(target);
                        entity.spawnParticle(Particle.REDSTONE, location, 2, 0.25, 0.25, 0.25, new Particle.DustOptions(Color.RED, 1.0f));
                    }
                }
            }
        default: break;
        }
    }

    public void onPlayerInput(Player player, ChessSquare square) {
        final ChessPiece piece = game.getCurrentBoard().getPieceAt(square);
        switch (saveTag.getState()) {
        case WAITING: {
            if (piece == null) return;
            clickQueue(player);
            break;
        }
        case GAME: {
            if (!saveTag.getPlayer(game.getCurrentBoard().getActiveColor()).isPlayer(player)) {
                return;
            }
            clickMove(player, square);
            break;
        }
        default: break;
        }
    }

    public void reset() {
        game = new ChessGame();
        game.initialize();
        saveTag.setState(ChessSaveTag.ChessState.WAITING);
        clearPieces();
        spawnAllPieces();
    }

    private void clickQueue(Player player) {
        if (saveTag.getPlayer(ChessColor.WHITE).isPlayer(player)) {
            return;
        }
        if (saveTag.getPlayer(ChessColor.BLACK).isPlayer(player)) {
            return;
        }
        if (saveTag.getQueue().size() >= 2) {
            player.sendMessage(text("The queue is full", RED));
            return;
        }
        if (saveTag.getQueue().contains(player.getUniqueId())) {
            player.sendMessage(text("Waiting for other player", GREEN));
            return;
        }
        if (saveTag.getQueue().isEmpty()) {
            final int size = 9;
            final var builder = GuiOverlay.BLANK.builder(9, WHITE)
                .title(text("Play Chess?", BLACK));
            final Gui gui = new Gui().size(size).title(builder.build());
            gui.setItem(2, Items.text(Mytems.OK.createItemStack(), List.of(text("Play against other player", GREEN))), click -> {
                    if (!click.isLeftClick()) return;
                    player.closeInventory();
                    if (saveTag.getState() != ChessSaveTag.ChessState.WAITING
                        || saveTag.getQueue().size() >= 2
                        || saveTag.getQueue().contains(player.getUniqueId())) {
                        player.sendMessage(text("Too late", RED));
                        return;
                    }
                    saveTag.getQueue().add(player.getUniqueId());
                    if (saveTag.getQueue().size() == 2) {
                        startQueue();
                    }
                });
            gui.setItem(6, Items.text(new ItemStack(Material.COMPARATOR), List.of(text("Play against Computer", GREEN))), click -> {
                    if (!click.isLeftClick()) return;
                    player.closeInventory();
                    if (saveTag.getState() != ChessSaveTag.ChessState.WAITING || !saveTag.getQueue().isEmpty()) {
                        player.sendMessage(text("Too late", RED));
                        return;
                    }
                    startCPU(player);
                });
            gui.open(player);
        } else if (saveTag.getQueue().size() == 1) {
            final int size = 9;
            final var builder = GuiOverlay.BLANK.builder(9, WHITE)
                .title(text("Play Chess?", BLACK));
            final Gui gui = new Gui().size(size).title(builder.build());
            final var otherName = PlayerCache.nameForUuid(saveTag.getQueue().get(0));
            gui.setItem(4, Items.text(Mytems.OK.createItemStack(), List.of(text("Play against " + otherName, GREEN))), click -> {
                    if (!click.isLeftClick()) return;
                    player.closeInventory();
                    if (saveTag.getState() != ChessSaveTag.ChessState.WAITING
                        || saveTag.getQueue().size() != 1
                        || saveTag.getQueue().contains(player.getUniqueId())) {
                        player.sendMessage(text("Too late", RED));
                        return;
                    }
                    saveTag.getQueue().add(player.getUniqueId());
                    if (saveTag.getQueue().size() == 2) {
                        startQueue();
                    }
                });
            gui.open(player);
        }
    }

    private void clickMove(Player player, ChessSquare clickedSquare) {
        final var piece = game.getCurrentBoard().getPieceAt(clickedSquare);
        if (moveFrom == null || !legalTargets.contains(clickedSquare)) {
            if (piece == null || piece.color != game.getCurrentBoard().getActiveColor()) return;
            moveFrom = clickedSquare;
            legalTargets.clear();
            for (var move : game.getCurrentTurn().getLegalMoves().keySet()) {
                if (move.from() != clickedSquare) continue;
                legalTargets.add(move.to());
                if (!legalTargets.contains(move.to())) {
                    legalTargets.add(move.to());
                }
            }
        } else if (moveFrom == clickedSquare) {
            // Clicks tend to spam
            return;
        } else if (moveFrom != null && legalTargets.contains(clickedSquare)) {
            final var list = new ArrayList<ChessMove>();
            for (var move : game.getCurrentTurn().getLegalMoves().keySet()) {
                if (move.from() == moveFrom && move.to() == clickedSquare) {
                    list.add(move);
                }
            }
            moveFrom = null;
            legalTargets.clear();
            if (list.isEmpty()) {
                return;
            } else if (list.size() == 1) {
                move(list.get(0));
            } else {
                // Promotion
                final int size = 9;
                final var builder = GuiOverlay.BLANK.builder(9, WHITE)
                    .title(text("Promotion", BLACK));
                final Gui gui = new Gui().size(size).title(builder.build());
                int index = 1;
                for (var type : ChessBoard.PROMOTION_PIECES) {
                    gui.setItem(index, Items.text(Glyph.toGlyph(type.letter).mytems.createItemStack(), List.of(text(type.getHumanName(), GREEN))), click -> {
                            if (!click.isLeftClick()) return;
                            player.closeInventory();
                            for (var move : list) {
                                if (move.promotion() == type) {
                                    move(move);
                                    return;
                                }
                            }
                        });
                    index += 2;
                }
                gui.open(player);
            }
        }
    }

    private File getSaveFile() {
        File folder = new File(plugin().getDataFolder(), "games");
        folder = new File(folder, world.getName());
        folder.mkdirs();
        return new File(folder, name + ".json");
    }

    private void startQueue() {
        if (saveTag.getState() != ChessSaveTag.ChessState.WAITING) return;
        if (saveTag.getQueue().size() != 2) return;
        final ChessColor color = ThreadLocalRandom.current().nextBoolean()
            ? ChessColor.WHITE
            : ChessColor.BLACK;
        saveTag.getPlayer(color).setPlayer(saveTag.getQueue().get(0));
        saveTag.getPlayer(color.other()).setPlayer(saveTag.getQueue().get(1));
        saveTag.setState(ChessSaveTag.ChessState.GAME);
        game = new ChessGame();
        game.initialize();
        game.setSiteName(name);
        game.setWhiteName(saveTag.getPlayer(ChessColor.WHITE).getName());
        game.setBlackName(saveTag.getPlayer(ChessColor.BLACK).getName());
        save();
        clearPieces();
        spawnAllPieces();
        final Player black = saveTag.getBlack().getPlayer();
        if (black != null) {
            black.sendMessage(text("You play as Black", GREEN));
        }
        final Player white = saveTag.getWhite().getPlayer();
        if (white != null) {
            black.sendMessage(text("You play as White", GREEN));
        }
    }

    private void startCPU(Player player) {
        if (saveTag.getState() != ChessSaveTag.ChessState.WAITING) return;
        final ChessColor color = ThreadLocalRandom.current().nextBoolean()
            ? ChessColor.WHITE
            : ChessColor.BLACK;
        saveTag.getPlayer(color).setPlayer(player);
        saveTag.getPlayer(color.other()).setCpu(true);
        saveTag.setState(ChessSaveTag.ChessState.GAME);
        game = new ChessGame();
        game.initialize();
        game.setSiteName(name);
        game.setWhiteName(saveTag.getPlayer(ChessColor.WHITE).getName());
        game.setBlackName(saveTag.getPlayer(ChessColor.BLACK).getName());
        save();
        clearPieces();
        spawnAllPieces();
        player.sendMessage(text("You play as " + color.getHumanName(), GREEN));
    }

    private void move(ChessMove move) {
        final var board = game.getCurrentBoard();
        final var piece = board.getPieceAt(move.from());
        final var taken = board.getPieceAt(move.to());
        final var color = board.getActiveColor();
        final var moveText = game.getCurrentTurn().getMoveText(move);
        if (!game.move(move)) return;
        // Update the board
        updateBoard(move);
        if (game.getCurrentBoard().getCastleMove() != null) {
            updateBoard(game.getCurrentBoard().getCastleMove());
        }
        // Announce
        final var turn = game.getCurrentTurn();
        final var state = turn.getState();
        announce(text(color.getHumanName()
                      + " moves " + piece.getType().getHumanName()
                      + " from " + move.from().getName()
                      + " to " + move.to().getName()
                      + (taken != null
                         ? " and takes " + taken.getType().getHumanName()
                         : "")
                      + (move.promotion() != null
                         ? " and promotes to " + move.promotion().getHumanName()
                         : "")
                      + " (" + moveText + ")"
                      + "."
                      + (state == ChessTurnState.CHECKMATE
                         ? " Checkmate!"
                         : (state == ChessTurnState.CHECK
                            ? " Check!"
                            : "")), GREEN));
        if (state.isGameOver()) {
            saveTag.setState(ChessSaveTag.ChessState.WAITING);
            switch (state) {
            case CHECKMATE:
                announce(text(turn.getWinner().getHumanName() + " wins by checkmate!", GREEN));
                break;
            case STALEMATE:
                announce(text("Stalemate!", GREEN));
                break;
            case DRAW_BY_REPETITION:
                announce(text("Draw by repetition!", GREEN));
                break;
            case DRAW_BY_INSUFFICIENT_MATERIAL:
                announce(text("Draw by insufficient material", GREEN));
                break;
            case DRAW_BY_FIFTY_MOVE_RULE:
                announce(text("Draw by fifty move rule", GREEN));
                break;
            case TIMEOUT_DRAW:
                announce(text("Draw by timeout", GREEN));
                break;
            case TIMEOUT:
                announce(text(turn.getWinner().getHumanName() + " wins by timeout!", GREEN));
                break;
            default: break;
            }
        } else {
            saveTag.getPlayer(game.getCurrentBoard().getActiveColor()).startMove();
        }
    }

    private void updateBoard(ChessMove move) {
        final var takenPiece = pieces.remove(move.to());
        if (takenPiece != null) takenPiece.remove();
        final var movedPiece = pieces.remove(move.from());
        if (movedPiece != null) {
            movedPiece.move(move.to());
            pieces.put(move.to(), movedPiece);
        }
    }
}
