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
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
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
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.chess.ChessPlugin.plugin;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
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
    private BossBar bossBar;
    private ChessColor drawOffered;
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
            final var player = saveTag.getPlayer(game.getCurrentBoard().getActiveColor());
            if (player.getTimeBankMillis() == 0L) {
                game.getCurrentTurn().setTimeout();
                onGameOver();
                return;
            } else if (player.isCpu()) {
                if (player.getMoveSeconds() < 5) return;
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
            moveFrom = null;
            legalTargets.clear();
        } else if (moveFrom == null || !legalTargets.contains(clickedSquare)) {
            if (piece == null || piece.color != color) {
                if (moveFrom == null) {
                    clickResignMenu(player, color);
                    player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, SoundCategory.MASTER, 0.5f, 1.25f);
                }
                moveFrom = null;
                legalTargets.clear();
                return;
            }
            legalTargets.clear();
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
            moveFrom = null;
            legalTargets.clear();
            if (list.isEmpty()) {
                moveFrom = null;
                legalTargets.clear();
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
                    gui.setItem(index, Items.text(Glyph.toGlyph(Character.toLowerCase(type.letter)).mytems.createItemStack(), List.of(text(type.getHumanName(), GREEN))), click -> {
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
            if (old != null) old.remove();
        }
        // Announce
        final var newTurn = game.getCurrentTurn();
        final var newState = newTurn.getState();
        announce(textOfChildren(text(player.getName(), GREEN),
                                text(" plays ", GRAY),
                                text(piece.getType().getHumanName(), GREEN),
                                text(" from ", GRAY),
                                text(move.from().getName(), GREEN),
                                text(" to ", GRAY),
                                text(move.to().getName(), GREEN),
                                (taken != null
                                 ? textOfChildren(text(" and takes ", GRAY),
                                                  text(taken.getType().getHumanName(), GREEN))
                                 : empty()),
                                (move.promotion() != null
                                 ? textOfChildren(text(" and promotes to ", GRAY),
                                                  text(move.promotion().getHumanName(), GREEN))
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
        if (takenPiece != null) takenPiece.remove();
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
                final var placed = pieceSet.place(this, move.to(), newPiece);
                if (placed != null) pieces.put(move.to(), placed);
            }
        }
    }

    private void onGameOver() {
        plugin().getLogger().info(getBoardId() + "\n" + game.toPgnString());
        saveTag.setState(ChessSaveTag.ChessState.WAITING);
        final var turn = game.getCurrentTurn();
        switch (turn.getState()) {
        case CHECKMATE:
            announce(text(turn.getWinner().getHumanName() + " wins by checkmate!", GREEN));
            break;
        case RESIGNATION:
            announce(text(turn.getWinner().getHumanName() + " wins by resignation!", GREEN));
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
            announce(text(turn.getWinner().getHumanName() + " wins by timeout!", GREEN));
            break;
        default: break;
        }
        if (game.getTurns().size() > 3) {
            final String url = game.toLichessAnalysisUrl();
            announce(textOfChildren(text("Review the game here: ", WHITE),
                                    text(url, BLUE, UNDERLINED))
                     .hoverEvent(text(url, GRAY))
                     .clickEvent(openUrl(url)));
        }
    }

    private void updateBossBar() {
        final List<Component> bossBarText = new ArrayList<>();
        float progress = 1f;
        for (var color : ChessColor.values()) {
            if (color == ChessColor.BLACK) {
                bossBarText.add(text(" | ", DARK_GRAY));
            }
            final var player = saveTag.getPlayer(color);
            final int seconds = player.getTimeBankSeconds();
            final int minutes = seconds / 60;
            final boolean playing = player.isPlaying();
            if (color == ChessColor.WHITE) {
                bossBarText.add(text(String.format("%02d", minutes), playing ? WHITE : GRAY).decoration(BOLD, playing));
                bossBarText.add(text(":", GRAY));
                bossBarText.add(text(String.format("%02d", seconds % 60), playing ? WHITE : GRAY).decoration(BOLD, playing));
                bossBarText.add(text(" " + player.getName(), playing ? WHITE : GRAY).decoration(BOLD, playing));
            } else {
                bossBarText.add(text(player.getName() + " ", playing ? WHITE : GRAY).decoration(BOLD, playing));
                bossBarText.add(text(String.format("%02d", minutes), playing ? WHITE : GRAY).decoration(BOLD, playing));
                bossBarText.add(text(":", GRAY));
                bossBarText.add(text(String.format("%02d", seconds % 60), playing ? WHITE : GRAY).decoration(BOLD, playing));
            }
            if (playing) {
                progress = (float) (player.getTimeBankMillis() / TIME_BANK);
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
