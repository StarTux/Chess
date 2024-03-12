package com.cavetale.chess;

import com.cavetale.chess.world.Worlds;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class ChessPlugin extends JavaPlugin {
    protected static ChessPlugin instance;
    protected final ChessCommand chessCommand = new ChessCommand(this);
    protected final ChessAdminCommand chessAdminCommand = new ChessAdminCommand(this);
    protected final Worlds worlds = new Worlds();

    public ChessPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        chessCommand.enable();
        chessAdminCommand.enable();
        worlds.enable();
    }

    @Override
    public void onDisable() {
        worlds.disable();
    }

    public static ChessPlugin plugin() {
        return instance;
    }
}
