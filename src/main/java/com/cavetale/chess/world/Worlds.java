package com.cavetale.chess.world;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.chess.board.ChessSquare;
import com.cavetale.core.event.hud.PlayerHudEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;
import static com.cavetale.chess.ChessPlugin.plugin;

/**
 * Worlds and world chess board manager.
 */
public final class Worlds implements Listener {
    private final List<WorldChessBoard> boards = new ArrayList<>();
    private BukkitTask task;
    @Setter private boolean allowVehicleEnter = false;

    public void enable() {
        task = Bukkit.getScheduler().runTaskTimer(plugin(), this::tick, 1L, 1L);
        Bukkit.getPluginManager().registerEvents(this, plugin());
        loadAll();
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        unloadAll();
    }

    public WorldChessBoard getBoard(World world, String name) {
        for (var board : boards) {
            if (world.equals(board.getWorld()) && name.equals(board.getName())) {
                return board;
            }
        }
        return null;
    }

    public List<WorldChessBoard> getBoardsIn(World world) {
        final var result = new ArrayList<WorldChessBoard>();
        for (var board : boards) {
            if (world.equals(board.getWorld())) {
                result.add(board);
            }
        }
        return result;
    }

    public WorldChessBoard getBoardAtPerimeter(Location location) {
        final var world = location.getWorld();
        for (var board : boards) {
            if (!world.equals(board.getWorld())) continue;
            if (!board.getPerimeter().contains(location)) continue;
            return board;
        }
        return null;
    }

    public void enable(World world) {
        Bukkit.getScheduler().runTask(plugin(), () -> loadAllIn(world));
    }

    public void disable(World world) {
        for (var iter = boards.iterator(); iter.hasNext();) {
            final var board = iter.next();
            if (world.equals(board.getWorld())) {
                board.fallAsleep();
                iter.remove();
            }
        }
    }

    public void loadAll() {
        for (var world : Bukkit.getWorlds()) {
            enable(world);
        }
    }

    public void unloadAll() {
        for (var board : boards) {
            board.fallAsleep();
        }
        boards.clear();
    }

    private void loadAllIn(World world) {
        final var areasFile = AreasFile.load(world, "Chess");
        if (areasFile == null) return;
        for (var boardName : areasFile.getAreas().keySet()) {
            if (getBoard(world, boardName) != null) continue;
            load(world, boardName, areasFile.getAreas().get(boardName));
        }
    }

    private WorldChessBoard load(World world, String boardName, List<Area> areaList) {
        final var boardId = world.getName() + "/" + boardName;
        Area boardArea = null;
        Area a1 = null;
        for (var area : areaList) {
            if (area.name == null) {
                plugin().getLogger().warning(boardId + ": Unnamed area: " + area);
                continue;
            }
            switch (area.name) {
            case "board":
                boardArea = area;
                break;
            case "a1":
                a1 = area;
                break;
            default: break;
            }
        }
        if (boardArea == null || a1 == null) {
            plugin().getLogger().warning(boardId + ": Missing area:"
                                         + " board=" + boardArea
                                         + " a1=" + a1);
            return null;
        }
        final WorldChessBoard worldChessBoard;
        try {
            worldChessBoard = new WorldChessBoard(world, boardName, boardArea.toCuboid(), a1.toCuboid());
        } catch (IllegalArgumentException iae) {
            plugin().getLogger().log(Level.WARNING, "Cannot created board " + boardId, iae);
            return null;
        }
        worldChessBoard.load();
        boards.add(worldChessBoard);
        worldChessBoard.tryToWakeUp();
        return worldChessBoard;
    }

    public static Worlds worlds() {
        return plugin().getWorlds();
    }

    private void tick() {
        for (var board : boards) {
            if (board.isAwake()) {
                if (board.fallAsleepIfNecessary()) continue;
                board.tick();
            } else {
                board.tryToWakeUp();
            }
        }
    }

    @EventHandler
    private void onWorldLoad(WorldLoadEvent event) {
        worlds().enable(event.getWorld());
    }

    @EventHandler
    private void onWorldUnload(WorldUnloadEvent event) {
        worlds().disable(event.getWorld());
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGH)
    private void onEntityDamage(EntityDamageEvent event) {
        final var entity = event.getEntity();
        if (!EntityChessPiece.isChessPiece(entity)) return;
        event.setCancelled(true);
        if (event instanceof EntityDamageByEntityEvent event2 && event2.getDamager() instanceof Player player) {
            for (var board : boards) {
                if (!board.isAwake()) continue;
                for (var square : ChessSquare.values()) {
                    final var worldChessPiece = board.getPieces().get(square);
                    if (!(worldChessPiece instanceof EntityChessPiece entityChessPiece)) continue;
                    if (!entityChessPiece.getEntities().contains(entity)) continue;
                    board.onPlayerInput(player, square);
                }
            }
        }
    }

    /**
     * Player interacted with unknown chess piece.
     */
    private void onPlayerInput(Player player, Entity entity) {
        for (var board : boards) {
            if (!board.isAwake()) continue;
            for (var square : ChessSquare.values()) {
                final var worldChessPiece = board.getPieces().get(square);
                if (!(worldChessPiece instanceof EntityChessPiece entityChessPiece)) continue;
                if (!entityChessPiece.getEntities().contains(entity)) continue;
                board.onPlayerInput(player, square);
            }
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGH)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        final var player = event.getPlayer();
        final var entity = event.getRightClicked();
        if (!EntityChessPiece.isChessPiece(entity)) return;
        event.setCancelled(true);
        onPlayerInput(player, entity);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGH)
    private void onPlayerInteract(PlayerInteractEvent event) {
        final var player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        switch (event.getAction()) {
        case RIGHT_CLICK_BLOCK:
        case LEFT_CLICK_BLOCK: {
            final var block = event.getClickedBlock();
            final var world = block.getWorld();
            for (var board : boards) {
                if (!board.isAwake()) continue;
                if (!world.equals(board.getWorld())) continue;
                if (!board.getBoardArea().contains(block)) continue;
                event.setCancelled(true);
                for (var square : ChessSquare.values()) {
                    if (!board.getSquares().get(square).contains(block)) continue;
                    board.onPlayerInput(player, square);
                    break;
                }
            }
            break;
        }
        case RIGHT_CLICK_AIR:
        case LEFT_CLICK_AIR: {
            final var location = player.getLocation();
            final var world = location.getWorld();
            for (var board : boards) {
                if (!board.isAwake()) continue;
                if (!world.equals(board.getWorld())) continue;
                if (!board.getPerimeter().contains(location)) continue;
                final var block = player.getTargetBlockExact(64);
                if (block == null) continue;
                for (var square : ChessSquare.values()) {
                    if (!board.getSquares().get(square).contains(block)) continue;
                    if (board.onPlayerRemoteInput(player, square)) {
                        event.setCancelled(true);
                    }
                    break;
                }
            }
            break;
        }
        default: return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onHangingBreak(HangingBreakEvent event) {
        final var hanging = event.getEntity();
        if (!EntityChessPiece.isChessPiece(hanging)) return;
        event.setCancelled(true);
        if (event instanceof HangingBreakByEntityEvent event2 && event2.getRemover() instanceof Player player) {
            onPlayerInput(player, hanging);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGH)
    private void onEntityTarget(EntityTargetEvent event) {
        final var target = event.getTarget();
        if (target == null) return;
        if (EntityChessPiece.isChessPiece(target)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        final var player = event.getPlayer();
        final var location = player.getLocation();
        final var world = location.getWorld();
        for (var board : boards) {
            if (!board.isAwake()) continue;
            if (!world.equals(board.getWorld())) continue;
            if (!board.getPerimeter().contains(location)) continue;
            board.onPlayerHud(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onVehicleEnter(VehicleEnterEvent event) {
        if (allowVehicleEnter) return;
        if (EntityChessPiece.isChessPiece(event.getEntered())) {
            event.setCancelled(true);
        }
    }
}
