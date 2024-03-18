package com.cavetale.chess.world;

import com.cavetale.chess.ai.DummyAI;
import com.cavetale.chess.board.ChessBoard;
import com.cavetale.chess.board.ChessColor;
import com.cavetale.chess.board.ChessGame;
import com.cavetale.chess.board.ChessMove;
import com.cavetale.chess.board.ChessPiece;
import com.cavetale.chess.board.ChessPieceType;
import com.cavetale.chess.board.ChessSquare;
import com.cavetale.chess.board.ChessTurnState;
import com.cavetale.chess.net.LichessImport;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.util.Json;
import com.cavetale.mytems.Mytems;
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
import java.util.logging.Level;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import static com.cavetale.chess.ChessPlugin.plugin;
import static com.cavetale.mytems.util.Entities.setTransient;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.event.ClickEvent.openUrl;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.UNDERLINED;

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
    private final int lengthBoardOrtho;
    private final Cuboid perimeter;
    // Game
    private ChessGame game;
    private final Map<ChessSquare, WorldChessPiece> pieces = new EnumMap<>(ChessSquare.class);
    private final Map<ChessSquare, Cuboid> squares = new EnumMap<>(ChessSquare.class);
    // Wake state
    private Set<Vec2i> chunks = new HashSet<>();
    private boolean awake;
    private ChessPieceSet pieceSet;
    private ChessSaveTag saveTag;
    private BossBar bossBar;
    private ChessColor drawOffered;
    // Move selection
    private ChessSquare moveFrom;
    private final List<ChessSquare> legalTargets = new ArrayList<>();
    private final List<BlockDisplay> highlights = new ArrayList<>();
    private int ticks = 0;
    private int lastInputTicks = 0;

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
            lengthBoardOrtho = a1.getSizeY();
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
            lengthBoardOrtho = a1.getSizeZ();
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
            lengthBoardOrtho = a1.getSizeX();
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
            try {
                game.loadPgnString(saveTag.getPgnString());
            } catch (IllegalArgumentException iae) {
                plugin().getLogger().log(Level.WARNING, "load " + saveTag.getPgnString(), iae);
                game.initialize();
            }
        } else {
            game.initialize();
        }
        pieceSet = saveTag.getPieceSetType() != null
            ? saveTag.getPieceSetType().getChessPieceSet()
            : null;
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

    public String getBoardId() {
        return world.getName() + "/" + name;
    }

    public ChessPieceSet getPieceSet() {
        if (pieceSet == null || !pieceSet.canSupport(this)) {
            pieceSet = null;
            saveTag.setPieceSetType(null);
            for (var it : ChessPieceSetType.values()) {
                if (it.getChessPieceSet().canSupport(this)) {
                    pieceSet = it.getChessPieceSet();
                    saveTag.setPieceSetType(it);
                    break;
                }
            }
            if (pieceSet == null) {
                throw new IllegalStateException(name + ": Not a single piece set can support this");
            }
        }
        return pieceSet;
    }

    public void setPieceSet(ChessPieceSetType type) {
        saveTag.setPieceSetType(type);
        pieceSet = type.getChessPieceSet();
        clearPieces();
        spawnAllPieces();
    }

    public void clearPieces() {
        for (var piece : pieces.values()) {
            piece.remove();
        }
        pieces.clear();
    }

    public void spawnAllPieces() {
        if (!awake) return;
        final var board = game.getCurrentBoard();
        for (var square : ChessSquare.values()) {
            final var old = pieces.remove(square);
            if (old != null) old.remove();
            final var piece = board.getPieceAt(square);
            if (piece == null) continue;
            final var placed = getPieceSet().place(this, square, piece);
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
        clearLegalMoves();
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
        ticks += 1;
        switch (saveTag.getState()) {
        case WAITING:
            if (saveTag.getQueue().isEmpty()) return;
            for (var iter = saveTag.getQueue().iterator(); iter.hasNext();) {
                final var uuid = iter.next();
                final var player = Bukkit.getPlayer(uuid);
                if (player == null || !perimeter.contains(player.getLocation())) {
                    iter.remove();
                }
            }
            break;
        case GAME:
            updateBossBar();
            final var color = game.getCurrentBoard().getActiveColor();
            final var player = saveTag.getPlayer(color);
            if (player.getTimeBankMillis() <= 0L) {
                game.getCurrentTurn().setTimeout();
                onGameOver();
                return;
            } else if (player.isCpu()) {
                if (player.getMoveSeconds() < 5) return;
                final var move = new DummyAI().getBestMove(game);
                move(move);
            }
        default: break;
        }
    }

    public void onPlayerInput(Player player, ChessSquare square) {
        if (ticks == lastInputTicks) return;
        lastInputTicks = ticks;
        switch (saveTag.getState()) {
        case WAITING: {
            clickQueue(player);
            break;
        }
        case GAME: {
            final var color = game.getCurrentBoard().getActiveColor();
            if (saveTag.getPlayer(color).isPlayer(player)) {
                clickMove(player, square);
            } else if (saveTag.getPlayer(color.other()).isPlayer(player)) {
                clickResignMenu(player, color.other());
            }
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
        clearLegalMoves();
        spawnAllPieces();
    }

    protected void onPlayerHud(PlayerHudEvent event) {
        if (bossBar != null) {
            event.bossbar(PlayerHudPriority.DEFAULT, bossBar);
        }
    }

    private void clickQueue(Player player) {
        if (saveTag.getQueue().size() >= 2) {
            player.sendMessage(text("The queue is full", RED));
            return;
        }
        if (saveTag.getQueue().contains(player.getUniqueId())) {
            final int size = 9;
            final var builder = GuiOverlay.BLANK.builder(9, WHITE)
                .title(text("Cancel?", BLACK));
            final Gui gui = new Gui().size(size).title(builder.build());
            gui.setItem(4, Items.text(Mytems.NO.createItemStack(), List.of(text("Cancel Request", RED))), click -> {
                    if (!click.isLeftClick()) return;
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    if (saveTag.getState() != ChessSaveTag.ChessState.WAITING
                        || !saveTag.getQueue().contains(player.getUniqueId())) {
                        player.sendMessage(text("Too late", RED));
                        return;
                    }
                    saveTag.getQueue().remove(player.getUniqueId());
                    player.sendMessage(text("Request cancelled", RED));
                });
            gui.open(player);
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
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    if (saveTag.getState() != ChessSaveTag.ChessState.WAITING
                        || saveTag.getQueue().size() >= 2
                        || saveTag.getQueue().contains(player.getUniqueId())) {
                        player.sendMessage(text("Too late", RED));
                        return;
                    }
                    saveTag.getQueue().add(player.getUniqueId());
                    if (saveTag.getQueue().size() == 2) {
                        startQueue();
                    } else {
                        announce(text(player.getName() + " would like to play. Click the board to accept.", GREEN));
                    }
                });
            gui.setItem(6, Items.text(new ItemStack(Material.COMPARATOR), List.of(text("Play against Computer", GREEN))), click -> {
                    if (!click.isLeftClick()) return;
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
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
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
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
        final var color = game.getCurrentBoard().getActiveColor();
        if (moveFrom == clickedSquare) {
            player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, SoundCategory.MASTER, 0.5f, 0.75f);
            clearLegalMoves();
        } else if (moveFrom == null || !legalTargets.contains(clickedSquare)) {
            if (piece == null || piece.color != color) {
                if (moveFrom == null) {
                    clickResignMenu(player, color);
                    player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, SoundCategory.MASTER, 0.5f, 1.25f);
                }
                clearLegalMoves();
                return;
            }
            clearLegalMoves();
            for (var move : game.getCurrentTurn().getLegalMoves().keySet()) {
                if (move.from() != clickedSquare) continue;
                legalTargets.add(move.to());
                if (!legalTargets.contains(move.to())) {
                    legalTargets.add(move.to());
                }
            }
            if (!legalTargets.isEmpty()) {
                player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, SoundCategory.MASTER, 0.5f, 1.25f);
                moveFrom = clickedSquare;
                fillLegalMoves(player);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, SoundCategory.MASTER, 0.5f, 0.75f);
            }
        } else if (moveFrom != null && legalTargets.contains(clickedSquare)) {
            final var list = new ArrayList<ChessMove>();
            for (var move : game.getCurrentTurn().getLegalMoves().keySet()) {
                if (move.from() == moveFrom && move.to() == clickedSquare) {
                    list.add(move);
                }
            }
            clearLegalMoves();
            if (list.isEmpty()) {
                player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, SoundCategory.MASTER, 0.5f, 0.75f);
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
                    gui.setItem(index, Items.text(ChessPiece.of(color, type).getMytems().createItemStack(), List.of(text(type.getHumanName(), GREEN))), click -> {
                            if (!click.isLeftClick()) return;
                            player.closeInventory();
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
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
                player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, SoundCategory.MASTER, 0.5f, 1.25f);
            }
        }
    }

    public static final Vector3f VECTOR3F_ZERO = new Vector3f(0f, 0f, 0f);
    public static final AxisAngle4f AXISANGLE4F_ZERO = new AxisAngle4f(0f, 0f, 0f, 0f);

    private void fillLegalMoves(Player player) {
        final var translation = new Vector3f((float) Math.abs(faceBoardX.getModX() + faceBoardY.getModX()) * -0.5f,
                                             (float) Math.abs(faceBoardX.getModY() + faceBoardY.getModY()) * -0.5f,
                                             (float) Math.abs(faceBoardX.getModZ() + faceBoardY.getModZ()) * -0.5f);
        final var leftRotation = AXISANGLE4F_ZERO;
        final var rightRotation = AXISANGLE4F_ZERO;
        final var scale = new Vector3f(facingAxis == Axis.X ? 0f : 1f,
                                       facingAxis == Axis.Y ? 0f : 1f,
                                       facingAxis == Axis.Z ? 0f : 1f);
        final var transformation = new Transformation(translation, leftRotation, scale, rightRotation);
        final var brightness = new BlockDisplay.Brightness(15, 15);
        for (var square : legalTargets) {
            final var cuboid = squares.get(square);
            for (int dy = 0; dy < lengthBoardY; dy += 1) {
                for (int dx = 0; dx < lengthBoardX; dx += 1) {
                    final int up = lengthBoardOrtho - 1;
                    final int x = cuboid.ax + Math.abs(dx * faceBoardX.getModX() + dy * faceBoardY.getModX() + up * faceBoardOrtho.getModX());
                    final int y = cuboid.ay + Math.abs(dx * faceBoardX.getModY() + dy * faceBoardY.getModY() + up * faceBoardOrtho.getModY());
                    final int z = cuboid.az + Math.abs(dx * faceBoardX.getModZ() + dy * faceBoardY.getModZ() + up * faceBoardOrtho.getModZ());
                    final var block = world.getBlockAt(x, y, z);
                    final var blockData = block.getBlockData();
                    final var location = block.getLocation()
                        .add(0.5, 0.5, 0.5)
                        .add(faceBoardOrtho.getDirection().multiply(0.5078125));
                    boolean willTake = game.getCurrentBoard().getPieceAt(square) != null;
                    final var display = world.spawn(location, BlockDisplay.class, e -> {
                            e.setPersistent(false);
                            setTransient(e);
                            e.setBlock(blockData);
                            e.setTransformation(transformation);
                            e.setGlowing(true);
                            e.setBrightness(brightness);
                            e.setGlowColorOverride(willTake ? Color.RED : Color.WHITE);
                            e.setVisibleByDefault(false);
                        });
                    highlights.add(display);
                    player.showEntity(plugin(), display);
                }
            }
        }
    }

    private void clearLegalMoves() {
        moveFrom = null;
        legalTargets.clear();
        for (var it : highlights) it.remove();
        highlights.clear();
    }

    private File getSaveFile() {
        File folder = new File(plugin().getDataFolder(), "games");
        folder = new File(folder, world.getName());
        folder.mkdirs();
        return new File(folder, name + ".json");
    }

    public static final long TIME_BANK = 1000L * 60L * 15L;
    public static final long TIME_INCREMENT = 1000L * 10L;

    private void startQueue() {
        if (saveTag.getState() != ChessSaveTag.ChessState.WAITING) return;
        if (saveTag.getQueue().size() != 2) return;
        final ChessColor color = ThreadLocalRandom.current().nextBoolean()
            ? ChessColor.WHITE
            : ChessColor.BLACK;
        saveTag.getPlayer(color).setPlayer(saveTag.getQueue().get(0));
        saveTag.getPlayer(color.other()).setPlayer(saveTag.getQueue().get(1));
        for (var p : saveTag.getPlayers()) {
            p.setTimeBank(TIME_BANK);
            p.setTimeIncrement(TIME_INCREMENT);
        }
        saveTag.setState(ChessSaveTag.ChessState.GAME);
        game = new ChessGame();
        game.initialize();
        game.setSiteName(name);
        game.setWhiteName(saveTag.getPlayer(ChessColor.WHITE).getName());
        game.setBlackName(saveTag.getPlayer(ChessColor.BLACK).getName());
        saveTag.getPlayer(game.getCurrentBoard().getActiveColor()).startMove();
        save();
        clearPieces();
        spawnAllPieces();
        final Player white = saveTag.getWhite().getPlayer();
        if (white != null) {
            white.sendMessage(text("You play as White", GREEN));
        }
        final Player black = saveTag.getBlack().getPlayer();
        if (black != null) {
            black.sendMessage(text("You play as Black", GREEN));
        }
    }

    private void startCPU(Player player) {
        if (saveTag.getState() != ChessSaveTag.ChessState.WAITING) return;
        final ChessColor color = ThreadLocalRandom.current().nextBoolean()
            ? ChessColor.WHITE
            : ChessColor.BLACK;
        saveTag.getPlayer(color).setPlayer(player);
        saveTag.getPlayer(color.other()).setCpu(true);
        for (var p : saveTag.getPlayers()) {
            p.setTimeBank(TIME_BANK);
            p.setTimeIncrement(TIME_INCREMENT);
        }
        saveTag.setState(ChessSaveTag.ChessState.GAME);
        game = new ChessGame();
        game.initialize();
        game.setSiteName(name);
        game.setWhiteName(saveTag.getPlayer(ChessColor.WHITE).getName());
        game.setBlackName(saveTag.getPlayer(ChessColor.BLACK).getName());
        saveTag.getPlayer(game.getCurrentBoard().getActiveColor()).startMove();
        save();
        clearPieces();
        spawnAllPieces();
        player.sendMessage(text("You play as " + color.getHumanName(), GREEN));
    }

    public void move(ChessMove move) {
        final var board = game.getCurrentBoard();
        final var turnNumber = board.getFullMoveClock();
        final var piece = board.getPieceAt(move.from());
        var taken = board.getPieceAt(move.to());
        final var color = board.getActiveColor();
        final var player = saveTag.getPlayer(color);
        final var moveText = game.getCurrentTurn().getMoveText(move);
        if (!game.move(move)) return;
        // Update the board
        final var newBoard = game.getCurrentBoard();
        updateBoard(move, color);
        if (newBoard.getCastleMove() != null) {
            updateBoard(newBoard.getCastleMove(), color);
        }
        if (taken != null) {
            world.playSound(getCenterLocation(move.to()), Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 2.0f, 0.5f);
        } else {
            world.playSound(getCenterLocation(move.to()), Sound.BLOCK_WOOD_FALL, SoundCategory.BLOCKS, 2.0f, 0.8f);
        }
        if (newBoard.getEnPassantTaken() != null) {
            taken = ChessPiece.of(color.other(), ChessPieceType.PAWN);
            final var old = pieces.remove(newBoard.getEnPassantTaken());
            if (old != null) {
                old.explode();
                old.remove();
            }
        }
        // Announce
        final var newTurn = game.getCurrentTurn();
        final var newState = newTurn.getState();
        announce(textOfChildren(text(player.getName(), GREEN),
                                text(" plays ", GRAY),
                                piece.getMytems(),
                                text(" from ", GRAY),
                                text(move.from().getName(), GREEN),
                                text(" to ", GRAY),
                                text(move.to().getName(), GREEN),
                                (taken != null
                                 ? textOfChildren(text(" and takes ", GRAY),
                                                  taken.getMytems())
                                 : empty()),
                                (move.promotion() != null
                                 ? textOfChildren(text(" and promotes to ", GRAY),
                                                  ChessPiece.of(color, move.promotion()).getMytems())
                                 : empty()),
                                text(" (", GRAY),
                                text(turnNumber, GRAY),
                                text((color == ChessColor.WHITE ? ". " : "... "), GRAY),
                                text(moveText, GREEN),
                                text(").", GRAY),
                                (newState == ChessTurnState.CHECKMATE
                                 ? text(" Checkmate!", GREEN, BOLD)
                                 : (newState == ChessTurnState.CHECK
                                    ? text(" Check!", GREEN, BOLD)
                                    : empty()))));
        saveTag.getPlayer(color).stopMove();
        if (newState.isGameOver()) {
            onGameOver();
        } else {
            saveTag.getPlayer(newBoard.getActiveColor()).startMove();
        }
        drawOffered = null;
    }

    private void updateBoard(ChessMove move, ChessColor color) {
        final var takenPiece = pieces.remove(move.to());
        if (takenPiece != null) {
            takenPiece.explode();
            takenPiece.remove();
        }
        if (move.promotion() == null) {
            final var movedPiece = pieces.remove(move.from());
            if (movedPiece != null) {
                movedPiece.move(move.to());
                pieces.put(move.to(), movedPiece);
            }
        } else {
            final var movedPiece = pieces.remove(move.from());
            if (movedPiece != null) {
                movedPiece.remove();
                final var newPiece = ChessPiece.of(color, move.promotion());
                final var placed = getPieceSet().place(this, move.to(), newPiece);
                if (placed != null) pieces.put(move.to(), placed);
            }
        }
    }

    private void onGameOver() {
        plugin().getLogger().info(getBoardId() + "\n" + game.toPgnString());
        final var turn = game.getCurrentTurn();
        final var winner = turn.getWinner();
        switch (turn.getState()) {
        case CHECKMATE:
            announce(text(saveTag.getPlayer(winner).getName() + " (" + winner.getHumanName() + ") wins by checkmate!", GREEN));
            break;
        case RESIGNATION:
            announce(text(saveTag.getPlayer(winner).getName() + " (" + winner.getHumanName() + ") wins by resignation!", GREEN));
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
        case DRAW_BY_AGREEMENT:
            announce(text("Both players agreed to draw", GREEN));
            break;
        case TIMEOUT_DRAW:
            announce(text("Draw by timeout", GREEN));
            break;
        case TIMEOUT:
            announce(text(saveTag.getPlayer(winner).getName() + " (" + winner.getHumanName() + ") wins by timeout!", GREEN));
            break;
        default: break;
        }
        if (game.getTurns().size() > 5) {
            new LichessImport(game, url -> {
                    if (url == null) {
                        url = game.toLichessAnalysisUrl();
                    }
                    announce(textOfChildren(text("Review the game here: ", WHITE),
                                            text(url, BLUE, UNDERLINED))
                             .hoverEvent(text(url, GRAY))
                             .clickEvent(openUrl(url)));
            }).async();
        }
        saveTag.setState(ChessSaveTag.ChessState.WAITING);
    }

    private void updateBossBar() {
        final List<ComponentLike> bossBarText = new ArrayList<>();
        float progress = 1f;
        final var whitePieceCount = game.getCurrentBoard().countPieces(ChessColor.WHITE);
        final var blackPieceCount = game.getCurrentBoard().countPieces(ChessColor.BLACK);
        int whiteScore = 0;
        int blackScore = 0;
        for (var entry : whitePieceCount.entrySet()) {
            whiteScore += entry.getKey().getValue() * entry.getValue();
        }
        for (var entry : blackPieceCount.entrySet()) {
            blackScore += entry.getKey().getValue() * entry.getValue();
        }
        for (var color : ChessColor.values()) {
            if (color == ChessColor.BLACK) {
                bossBarText.add(text(" | ", DARK_GRAY));
            }
            final var player = saveTag.getPlayer(color);
            final int seconds = Math.max(0, player.getTimeBankSeconds());
            final int minutes = seconds / 60;
            final boolean playing = player.isPlaying();
            if (color == ChessColor.WHITE) {
                if (playing) {
                    bossBarText.add(Mytems.COLORFALL_HOURGLASS.getCurrentAnimationFrame());
                    progress = Math.max(0.0f, Math.min(1.0f, (float) player.getTimeBankMillis() / (float) TIME_BANK));
                }
                bossBarText.add(text(String.format("%2d", minutes), playing ? AQUA : GRAY));
                bossBarText.add(text(":", GRAY));
                bossBarText.add(text(String.format("%02d", seconds % 60), playing ? AQUA : GRAY));
                bossBarText.add(space());
                bossBarText.add(text(player.getName(), GRAY).decoration(BOLD, playing));
                if (whiteScore > blackScore) {
                    bossBarText.add(space());
                    bossBarText.add(text(Unicode.superscript("+" + (whiteScore - blackScore)), GRAY));
                }
                for (var type : ChessPieceType.values()) {
                    final int has = blackPieceCount.getOrDefault(type, 0);
                    final int missing = type.getInitialAmount() - has;
                    if (missing == 0) continue;
                    bossBarText.add(ChessPiece.of(ChessColor.BLACK, type).getMytems());
                    if (missing > 1) {
                        bossBarText.add(text(Unicode.subscript(missing), DARK_GRAY));
                    }
                }
            } else {
                final var types = ChessPieceType.values();
                for (int j = types.length - 1; j >= 0; j -= 1) {
                    final var type = types[j];
                    final int has = whitePieceCount.getOrDefault(type, 0);
                    final int missing = type.getInitialAmount() - has;
                    if (missing == 0) continue;
                    bossBarText.add(ChessPiece.of(ChessColor.WHITE, type).getMytems());
                    if (missing > 1) {
                        bossBarText.add(text(Unicode.subscript(missing), GRAY));
                    }
                }
                if (blackScore > whiteScore) {
                    bossBarText.add(text(Unicode.superscript("+" + (blackScore - whiteScore)), DARK_GRAY));
                    bossBarText.add(space());
                }
                bossBarText.add(text(player.getName(), DARK_GRAY).decoration(BOLD, playing));
                bossBarText.add(space());
                bossBarText.add(text(String.format("%2d", minutes), playing ? AQUA : DARK_GRAY));
                bossBarText.add(text(":", DARK_GRAY));
                bossBarText.add(text(String.format("%02d", seconds % 60), playing ? AQUA : DARK_GRAY));
                if (playing) {
                    bossBarText.add(Mytems.COLORFALL_HOURGLASS.getCurrentAnimationFrame());
                    progress = Math.max(0.0f, Math.min(1.0f, (float) player.getTimeBankMillis() / (float) TIME_BANK));
                }
            }
        }
        if (bossBar == null) {
            bossBar = BossBar.bossBar(join(noSeparators(), bossBarText), progress, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_20);
        } else {
            bossBar.name(join(noSeparators(), bossBarText));
            bossBar.progress(progress);
        }
    }

    private void clickResignMenu(Player player, ChessColor color) {
        final int size = 9;
        final var builder = GuiOverlay.BLANK.builder(9, WHITE)
            .title(text("End the Game?", BLACK));
        final Gui gui = new Gui().size(size).title(builder.build());
        gui.setItem(2, Items.text(Mytems.REDO.createItemStack(), List.of(text((drawOffered == color.other() ? "Agree to Draw" : "Offer Draw"), GREEN))), click -> {
                if (!click.isLeftClick()) return;
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                if (saveTag.getState() != ChessSaveTag.ChessState.GAME) return;
                if (game.getCurrentTurn().getState().isGameOver()) return;
                if (!saveTag.getPlayer(color).isPlayer(player)) return;
                if (drawOffered == null) {
                    drawOffered = color;
                    announce(text(player.getName() + " offered a draw", RED));
                } else if (drawOffered == color) {
                    player.sendMessage(text("You already offered this turn", RED));
                } else if (drawOffered == color.other()) {
                    game.getCurrentTurn().setAggreeToDraw();
                    onGameOver();
                }
            });
        gui.setItem(6, Items.text(new ItemStack(Material.WHITE_BANNER), List.of(text("Resign", RED))), click -> {
                if (!click.isLeftClick()) return;
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                if (saveTag.getState() != ChessSaveTag.ChessState.GAME) return;
                if (game.getCurrentTurn().getState().isGameOver()) return;
                if (!saveTag.getPlayer(color).isPlayer(player)) return;
                game.getCurrentTurn().resign(color);
                onGameOver();
            });
        gui.open(player);
    }
}
