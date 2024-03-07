package com.cavetale.chess;

import org.bukkit.plugin.java.JavaPlugin;

public final class ChessPlugin extends JavaPlugin {
    protected static ChessPlugin instance;
    protected final ChessCommand chessCommand = new ChessCommand(this);
    protected final EventListener eventListener = new EventListener();

    public ChessPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        chessCommand.enable();
        eventListener.enable();
    }

    @Override
    public void onDisable() {
    }

    public static ChessPlugin plugin() {
        return instance;
    }
}
