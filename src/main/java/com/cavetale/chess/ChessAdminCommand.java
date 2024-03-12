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
        rootNode.addChild("move").arguments("<move>")
            .description("Make a move")
            .playerCaller(this::move);
        rootNode.addChild("pgn").denyTabCompletion()
            .description("Dump PGN file")
            .playerCaller(this::pgn);
        rootNode.addChild("fen").denyTabCompletion()
            .description("Print FEN string")
            .playerCaller(this::fen);
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

    protected boolean move(Player player, String[] args) {
        if (args.length != 1) return false;
        final var board = worlds().getBoardAtPerimeter(player.getLocation());
        if (board == null) {
            throw new CommandWarn("There is no board nearby");
        }
        final var move = board.getGame().getCurrentTurn().getMoveTexts().get(args[0]);
        if (move == null) {
            throw new CommandWarn("Move not available: " + args[0]);
        }
        player.sendMessage(text("Executing move at " + board.getBoardId() + ": " + move, YELLOW));
        board.move(move);
        return true;
    }

    protected void pgn(Player player) {
        final var board = worlds().getBoardAtPerimeter(player.getLocation());
        if (board == null) {
            throw new CommandWarn("There is no board nearby");
        }
        plugin.getLogger().info(board.getGame().toPgnString());
        player.sendMessage(text("PGN file was dumped to console", YELLOW));
    }

    protected void fen(Player player) {
        final var board = worlds().getBoardAtPerimeter(player.getLocation());
        if (board == null) {
            throw new CommandWarn("There is no board nearby");
        }
        final String fen = board.getGame().getCurrentBoard().toFenString();
        player.sendMessage(text(fen, YELLOW)
                           .hoverEvent(text(fen, GRAY))
                           .insertion(fen));
    }

}
