package com.cavetale.chess.world;

import com.cavetale.chess.ai.ChessEngineType;
import com.cavetale.chess.ai.DummyAI;
import com.cavetale.chess.ai.StockfishAI;
import com.cavetale.chess.board.ChessBoard;
import com.cavetale.chess.board.ChessColor;
import com.cavetale.chess.board.ChessGame;
import com.cavetale.chess.board.ChessMove;
import com.cavetale.chess.board.ChessPiece;
import com.cavetale.chess.board.ChessPieceType;
import com.cavetale.chess.board.ChessSquare;
import com.cavetale.chess.board.ChessTurnState;
import com.cavetale.chess.net.LichessImport;
import com.cavetale.chess.sql.SQLChessGame;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.util.Json;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Gui;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
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
import org.bukkit.Particle;
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
import static com.cavetale.mytems.util.Items.tooltip;
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
    private boolean cpuRequestScheduled = false;

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
            showPreviousMove();
            for (ChessColor color : ChessColor.values()) {
                final var player = saveTag.getPlayer(color);
                if (!player.isPlayer()) continue;
                final Player entity = player.getPlayer();
                if (entity != null && entity.getWorld().equals(world) && perimeter.contains(entity.getLocation())) {
                    player.setAwaySince(0L);
                } else {
                    if (player.getAwaySince() == 0L) {
                        player.setAwaySince(System.currentTimeMillis());
                    } else {
                        final long awayTime = System.currentTimeMillis() - player.getAwaySince();
                        if (awayTime > 1000L * 60L) {
                            game.getCurrentTurn().abandon(color);
                            onGameOver();
                            return;
                        }
                    }
                }
            }
            final var color = game.getCurrentBoard().getActiveColor();
            final var player = saveTag.getPlayer(color);
            if (player.getTimeBankMillis() <= 0L) {
                game.getCurrentTurn().setTimeout();
                onGameOver();
                return;
            } else if (player.isCpu()) {
                switch (player.getChessEngineType()) {
                case DUMMY: {
                    if (player.getMoveSeconds() < 5) return;
                    final var move = new DummyAI().getBestMove(game);
                    move(move);
                    break;
                }
                case STOCKFISH: {
                    if (cpuRequestScheduled) return;
                    cpuRequestScheduled = true;
                    final String fenString = game.getCurrentBoard().toFenString();
                    final int currentTurnNumber = game.getCurrentBoard().getFullMoveClock();
                    final var ai = new StockfishAI(fenString, move -> {
                            if (!game.getCurrentBoard().toFenString().equals(fenString)) return;
                            if (move == null) {
                                game.getCurrentTurn().resign(color);
                                onGameOver();
                            } else {
                                if (!move(move)) {
                                    plugin().getLogger().warning(getBoardId() + " Stockfish suggests illegal move: "
                                                                 + move + " for " + game.getCurrentBoard().toFenString());
                                    game.getCurrentTurn().resign(color);
                                }
                            }
                    });
                    ai.setSeconds(Math.max(5, Math.min(15, 5 + currentTurnNumber / 2)));
                    ai.setSkillLevel(player.getStockfishLevel());
                    ai.schedule();
                    announce(text("Stockfish is thinking...", GRAY));
                    break;
                }
                default: break;
                }
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

    /**
     * Input is more remote than above, so we return false when they
     * are not playing so it doesn't have to be needlessly cancelled.
     */
    public boolean onPlayerRemoteInput(Player player, ChessSquare square) {
        if (ticks == lastInputTicks) return false;
        lastInputTicks = ticks;
        switch (saveTag.getState()) {
        case GAME: {
            final var color = game.getCurrentBoard().getActiveColor();
            if (saveTag.getPlayer(color).isPlayer(player)) {
                clickMove(player, square);
                return true;
            } else if (saveTag.getPlayer(color.other()).isPlayer(player)) {
                clickResignMenu(player, color.other());
                return true;
            } else {
                return false;
            }
        }
        default: return false;
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
        final Player player = event.getPlayer();
        if (bossBar != null) {
            final PlayerHudPriority prio;
            if (saveTag.getState() == ChessSaveTag.ChessState.GAME) {
                if (saveTag.getWhite().isPlayer(player) || saveTag.getBlack().isPlayer(player)) {
                    prio = PlayerHudPriority.HIGH;
                } else {
                    prio = PlayerHudPriority.LOW;
                }
            } else {
                prio = PlayerHudPriority.LOWEST;
            }
            event.bossbar(prio, bossBar);
        }
    }

    private void clickQueue(Player player) {
        if (saveTag.getQueue().size() >= 2) {
            player.sendMessage(text("The queue is full", RED));
            return;
        }
        if (saveTag.getQueue().contains(player.getUniqueId())) {
            openCancelMenu(player);
            return;
        }
        if (saveTag.getQueue().isEmpty()) {
            openQueueMenu(player);
        } else if (saveTag.getQueue().size() == 1) {
            new ChessAcceptMenu(player, this, saveTag.getQueue().get(0)).open();
        }
    }

    private void openQueueMenu(Player player) {
        new ChessQueueMenu(player, this).open();
    }

    private void openCancelMenu(Player player) {
        final int size = 9;
        final var builder = GuiOverlay.BLANK.builder(9, DARK_GRAY)
            .title(text("Cancel?", WHITE));
        final Gui gui = new Gui().size(size).title(builder.build());
        gui.setItem(4, tooltip(Mytems.NO.createItemStack(), List.of(text("Cancel Request", RED))), click -> {
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
                final var builder = GuiOverlay.BLANK.builder(9, DARK_GRAY)
                    .title(text("Promotion", WHITE));
                final Gui gui = new Gui().size(size).title(builder.build());
                int index = 1;
                for (var type : ChessBoard.PROMOTION_PIECES) {
                    gui.setItem(index, tooltip(ChessPiece.of(color, type).getMytems().createItemStack(), List.of(text(type.getHumanName(), GREEN))), click -> {
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
    public static final AxisAngle4f AXISANGLE4F_FLIP = new org.joml.AxisAngle4f((float) Math.PI, 0f, 1f, 0f);

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

    protected boolean addToQueue(Player player, ChessColor color) {
        if (saveTag.getState() != ChessSaveTag.ChessState.WAITING) return false;
        if (saveTag.getQueue().size() >= 2) return false;
        if (saveTag.getQueue().isEmpty()) {
            saveTag.setQueueColor(color);
        } else if (saveTag.getQueueColor() == null && color != null) {
            saveTag.setQueueColor(color.other());
        }
        saveTag.getQueue().add(player.getUniqueId());
        if (saveTag.getQueue().size() == 2) {
            startQueue();
        } else {
            announce(text(player.getName() + " would like to play. Click the board to accept.", GREEN));
        }
        return true;
    }

    private void startQueue() {
        if (saveTag.getState() != ChessSaveTag.ChessState.WAITING) return;
        if (saveTag.getQueue().size() != 2) return;
        final ChessColor color;
        if (saveTag.getQueueColor() != null) {
            color = saveTag.getQueueColor();
        } else {
            color = ThreadLocalRandom.current().nextBoolean()
                ? ChessColor.WHITE
                : ChessColor.BLACK;
        }
        saveTag.getPlayer(color).setPlayer(saveTag.getQueue().get(0));
        saveTag.getPlayer(color.other()).setPlayer(saveTag.getQueue().get(1));
        for (var p : saveTag.getPlayers()) {
            p.setTimeBank(saveTag.getTimeBank());
            p.setTimeIncrement(saveTag.getTimeIncrement());
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
            white.sendMessage(textOfChildren(Mytems.WHITE_QUEEN, text("You play as White", GRAY)));
        }
        final Player black = saveTag.getBlack().getPlayer();
        if (black != null) {
            black.sendMessage(textOfChildren(Mytems.BLACK_QUEEN, text("You play as Black", DARK_GRAY)));
        }
        cpuRequestScheduled = false;
    }

    protected void startCPU(Player player, ChessColor chosenColor, ChessEngineType type, Consumer<ChessSaveTag.ChessPlayer> callback) {
        if (saveTag.getState() != ChessSaveTag.ChessState.WAITING) return;
        final ChessColor color;
        if (chosenColor != null) {
            color = chosenColor;
        } else {
            color = ThreadLocalRandom.current().nextBoolean()
                ? ChessColor.WHITE
                : ChessColor.BLACK;
        }
        saveTag.getPlayer(color).setPlayer(player);
        saveTag.getPlayer(color.other()).setChessEngineType(type);
        callback.accept(saveTag.getPlayer(color.other()));
        for (var p : saveTag.getPlayers()) {
            p.setTimeBank(saveTag.getTimeBank());
            p.setTimeIncrement(saveTag.getTimeIncrement());
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
        cpuRequestScheduled = false;
        player.sendMessage(textOfChildren(ChessPiece.of(color, ChessPieceType.QUEEN).getMytems(),
                                          text("You play as " + color.getHumanName(), (color == ChessColor.WHITE ? GRAY : DARK_GRAY))));
    }

    public boolean move(ChessMove move) {
        final var board = game.getCurrentBoard();
        final var turnNumber = board.getFullMoveClock();
        final var piece = board.getPieceAt(move.from());
        var taken = board.getPieceAt(move.to());
        final var color = board.getActiveColor();
        final var player = saveTag.getPlayer(color);
        final var moveText = game.getCurrentTurn().getMoveText(move);
        if (!game.move(move)) return false;
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
        cpuRequestScheduled = false;
        return true;
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
        case ABANDONED:
            announce(text(saveTag.getPlayer(winner).getName() + " (" + winner.getHumanName() + ") wins by abandonment!", GREEN));
            break;
        default: break;
        }
        final SQLChessGame row = new SQLChessGame(this);
        if (game.getTurns().size() > 5) {
            new LichessImport(game, url -> {
                    if (url != null) {
                        row.setLichessUrl(url);
                    }
                    plugin().getDatabase().insertAsync(row, i -> plugin().getLogger().info("Game saved with id " + row.getId()));
                    if (url == null) {
                        url = game.toLichessAnalysisUrl();
                    }
                    announce(textOfChildren(text("Review the game here: ", WHITE),
                                            text(url, BLUE, UNDERLINED))
                             .hoverEvent(text(url, GRAY))
                             .clickEvent(openUrl(url)));
                    plugin().getLogger().info(url);
            }).async();
        } else {
            plugin().getDatabase().insertAsync(row, i -> plugin().getLogger().info("Game saved with id " + row.getId()));
        }
        if (winner != null && saveTag.getPlayer(winner).isPlayer() && saveTag.getPlayer(winner.other()).isStockfish()) {
            final var player = saveTag.getPlayer(winner).getPlayer();
            if (player != null) {
                Mytems.KITTY_COIN.giveItemStack(player, 1 + saveTag.getPlayer(winner.other()).getStockfishLevel() / 3);
            }
        }
        saveTag.callMinigameEvent(winner);
        saveTag.setState(ChessSaveTag.ChessState.WAITING);
        cpuRequestScheduled = false;
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
            final var textColor = player.getAwaySince() != 0L
                ? DARK_RED
                : (color == ChessColor.WHITE
                   ? GRAY
                   : DARK_GRAY);
            if (color == ChessColor.WHITE) {
                if (playing) {
                    bossBarText.add(Mytems.COLORFALL_HOURGLASS.getCurrentAnimationFrame());
                    progress = Math.max(0.0f, Math.min(1.0f, (float) player.getTimeBankMillis() / (float) saveTag.getTimeBank()));
                }
                bossBarText.add(text(String.format("%2d", minutes), textColor));
                bossBarText.add(text(":", textColor));
                bossBarText.add(text(String.format("%02d", seconds % 60), textColor));
                bossBarText.add(space());
                bossBarText.add(text(player.getName(), textColor).decoration(BOLD, playing));
                if (whiteScore > blackScore) {
                    bossBarText.add(space());
                    bossBarText.add(text(Unicode.superscript("+" + (whiteScore - blackScore)), textColor));
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
                    bossBarText.add(text(Unicode.superscript("+" + (blackScore - whiteScore)), textColor));
                    bossBarText.add(space());
                }
                bossBarText.add(text(player.getName(), textColor).decoration(BOLD, playing));
                bossBarText.add(space());
                bossBarText.add(text(String.format("%2d", minutes), textColor));
                bossBarText.add(text(":", textColor));
                bossBarText.add(text(String.format("%02d", seconds % 60), textColor));
                if (playing) {
                    bossBarText.add(Mytems.COLORFALL_HOURGLASS.getCurrentAnimationFrame());
                    progress = Math.max(0.0f, Math.min(1.0f, (float) player.getTimeBankMillis() / (float) saveTag.getTimeBank()));
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

    private void showPreviousMove() {
        final ChessMove move = game.getCurrentTurn().getPreviousMove();
        if (move == null) return;
        final var up = faceBoardOrtho.getDirection().multiply(0.125);
        final Location from = getCenterLocation(move.from()).add(up);
        final Location to = getCenterLocation(move.to()).add(up);
        final double d = from.distance(to);
        final int steps = (int) Math.round(d * 2.0);
        if (steps == 0) return; // Impossible
        for (int i = 0; i < steps; i += 1) {
            final double a = (double) i / (double) steps;
            final double b = 1.0 - a;
            final Location location = from.toVector().multiply(b)
                .add(to.toVector().multiply(a))
                .toLocation(world);
            world.spawnParticle(Particle.WAX_ON, location, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void clickResignMenu(Player player, ChessColor color) {
        final int size = 9;
        final var builder = GuiOverlay.BLANK.builder(9, DARK_GRAY)
            .title(text("End the Game?", WHITE));
        final Gui gui = new Gui().size(size).title(builder.build());
        gui.setItem(2, tooltip(Mytems.REDO.createItemStack(), List.of(text((drawOffered == color.other() ? "Agree to Draw" : "Offer Draw"), GREEN))), click -> {
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
        gui.setItem(6, tooltip(new ItemStack(Material.WHITE_BANNER), List.of(text("Resign", RED))), click -> {
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
