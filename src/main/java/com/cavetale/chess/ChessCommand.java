package com.cavetale.chess;

import com.cavetale.core.command.AbstractCommand;
import org.bukkit.command.CommandSender;

public final class ChessCommand extends AbstractCommand<ChessPlugin> {
    protected ChessCommand(final ChessPlugin plugin) {
        super(plugin, "chess");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("info").denyTabCompletion()
            .description("Info Command")
            .senderCaller(this::info);
    }

    protected boolean info(CommandSender sender, String[] args) {
        return false;
    }
}
