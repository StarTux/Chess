package com.cavetale.chess.world;

import com.cavetale.chess.ai.ChessEngineType;
import com.cavetale.chess.board.ChessColor;
import com.cavetale.core.font.Unicode;
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
    private ChessColor queueColor = null;
    private ChessPieceSetType pieceSetType;
    private long timeBank;
    private long timeIncrement;
    private long startTime;

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
            white = new ChessPlayer();
            black = new ChessPlayer();
            queue.clear();
            queueColor = null;
            startTime = 0L;
            break;
        case GAME:
            queue.clear();
            queueColor = null;
            startTime = System.currentTimeMillis();
            break;
        default: break;
        }
    }

    public ChessPlayer getPlayer(ChessColor color) {
        return color == ChessColor.WHITE ? white : black;
    }

    public List<ChessPlayer> getPlayers() {
        return List.of(white, black);
    }

    @Data
    public static final class ChessPlayer {
        private ChessEngineType chessEngineType;
        private int stockfishLevel;
        private UUID player;
        private long moveStarted;
        private long timeBank;
        private long timeIncrement;
        private boolean playing;
        private long awaySince;

        public boolean isEmpty() {
            return chessEngineType == null && player == null;
        }

        public String getName() {
            if (chessEngineType != null) {
                if (chessEngineType == ChessEngineType.STOCKFISH) {
                    return "Stockfish" + Unicode.superscript(stockfishLevel);
                }
                return chessEngineType.getDisplayName();
            }
            if (player != null) {
                return PlayerCache.nameForUuid(player);
            }
            return "N/A";
        }

        public String getDatabaseName() {
            if (chessEngineType != null) {
                if (chessEngineType == ChessEngineType.STOCKFISH) {
                    return String.format("Stockfish-%02d", stockfishLevel);
                } else {
                    return chessEngineType.getDisplayName();
                }
            }
            if (player != null) {
                return player.toString();
            }
            return "N/A";
        }

        public boolean isPlayer() {
            return player != null;
        }

        public boolean isPlayer(Player other) {
            return !isCpu() && player != null && player.equals(other.getUniqueId());
        }

        public void setPlayer(Player other) {
            chessEngineType = null;
            player = other.getUniqueId();
        }

        public void setPlayer(UUID uuid) {
            chessEngineType = null;
            player = uuid;
        }

        public Player getPlayer() {
            if (player == null) return null;
            return Bukkit.getPlayer(player);
        }

        public UUID getPlayerUuid() {
            return player;
        }

        public void startMove() {
            playing = true;
            moveStarted = System.currentTimeMillis();
        }

        public void stopMove() {
            playing = false;
            timeBank = Math.max(0, timeBank - getMoveMillis() + timeIncrement);
        }

        public long getMoveMillis() {
            return System.currentTimeMillis() - moveStarted;
        }

        public int getMoveSeconds() {
            return (int) (getMoveMillis() / 1000L);
        }

        public long getTimeBankMillis() {
            return playing
                ? timeBank - getMoveMillis()
                : timeBank;
        }

        public int getTimeBankSeconds() {
            return (int) (getTimeBankMillis() / 1000L);
        }

        public boolean isCpu() {
            return chessEngineType != null;
        }
    }
}
