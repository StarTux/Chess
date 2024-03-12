package com.cavetale.chess;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static com.cavetale.chess.world.Worlds.worlds;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class ChessAdminCommand extends AbstractCommand<ChessPlugin> {
    protected ChessAdminCommand(final ChessPlugin plugin) {
        super(plugin, "chessadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload boards")
            .senderCaller(this::reload);
        rootNode.addChild("reset").denyTabCompletion()
            .description("Reset this board")
            .playerCaller(this::reset);
    }

    protected void reload(CommandSender sender) {
        worlds().unloadAll();
        worlds().loadAll();
        sender.sendMessage(text("Boards reloaded", YELLOW));
    }

    protected void reset(Player player) {
        final var board = worlds().getBoardAtPerimeter(player.getLocation());
        if (board == null) {
            throw new CommandWarn("There is no board nearby");
        }
        board.reset();
        player.sendMessage(text("Board was reset: " + board.getBoardId(), YELLOW));
    }
}
