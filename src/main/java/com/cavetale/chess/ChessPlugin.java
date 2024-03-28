package com.cavetale.chess;

import com.cavetale.chess.sql.SQLChessGame;
import com.cavetale.chess.world.Worlds;
import com.winthier.sql.SQLDatabase;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class ChessPlugin extends JavaPlugin {
    protected static ChessPlugin instance;
    protected final ChessCommand chessCommand = new ChessCommand(this);
    protected final ChessAdminCommand chessAdminCommand = new ChessAdminCommand(this);
    protected final Worlds worlds = new Worlds();
    protected SQLDatabase database;

    public ChessPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        database = new SQLDatabase(this);
        database.registerTable(SQLChessGame.class);
        database.createAllTables();
        chessCommand.enable();
        chessAdminCommand.enable();
        worlds.enable();
    }

    @Override
    public void onDisable() {
        worlds.disable();
        database.waitForAsyncTask();
    }

    public static ChessPlugin plugin() {
        return instance;
    }
}
