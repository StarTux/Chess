package com.cavetale.chess.world;

import com.cavetale.chess.board.ChessColor;
import com.cavetale.core.playercache.PlayerCache;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Data
public final class ChessSaveTag implements Serializable {
    private ChessState state = ChessState.WAITING;
    private long stateStarted;
    private String pgnString;
    private ChessPlayer white = new ChessPlayer();
    private ChessPlayer black = new ChessPlayer();
    private List<UUID> queue = new ArrayList<>();

    public enum ChessState {
        WAITING,
        GAME;
    }

    public long getStateDurationMillis() {
        return System.currentTimeMillis() - stateStarted;
    }

    public int getStateDurationSeconds() {
        return (int) (getStateDurationMillis() / 1000L);
    }

    public void setState(ChessState newState) {
        state = newState;
        stateStarted = System.currentTimeMillis();
        switch (newState) {
        case WAITING:
            white.clear();
            black.clear();
            queue.clear();
            break;
        case GAME:
            queue.clear();
            break;
        default: break;
        }
    }

    public ChessPlayer getPlayer(ChessColor color) {
        return color == ChessColor.WHITE ? white : black;
    }

    @Data
    public static final class ChessPlayer {
        private boolean cpu;
        private UUID player;
        private long moveStarted;

        public boolean isEmpty() {
            return !cpu && player == null;
        }

        public void clear() {
            cpu = false;
            player = null;
        }

        public String getName() {
            if (cpu) return "CPU";
            if (player != null) return PlayerCache.nameForUuid(player);
            return "N/A";
        }

        public boolean isPlayer(Player other) {
            return !cpu && player != null && player.equals(other.getUniqueId());
        }

        public void setPlayer(Player other) {
            cpu = false;
            player = other.getUniqueId();
        }

        public void setPlayer(UUID uuid) {
            cpu = false;
            player = uuid;
        }

        public Player getPlayer() {
            if (player == null) return null;
            return Bukkit.getPlayer(player);
        }

        public void startMove() {
            moveStarted = System.currentTimeMillis();
        }

        public long getMoveMillis() {
            return System.currentTimeMillis() - moveStarted;
        }

        public int getMoveSeconds() {
            return (int) (getMoveMillis() / 1000L);
        }
    }
}
