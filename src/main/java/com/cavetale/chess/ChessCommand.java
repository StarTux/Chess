package com.cavetale.chess;

import com.cavetale.chess.board.ChessGame;
import com.cavetale.chess.sql.SQLChessGame;
import com.cavetale.core.command.AbstractCommand;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import static com.cavetale.chess.ChessPlugin.plugin;
import static com.cavetale.core.util.CamelCase.toCamelCase;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.openUrl;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class ChessCommand extends AbstractCommand<ChessPlugin> {
    private static final ZoneId ZONE_ID = ZoneId.of("UTC-11");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE MMMM dd yyyy HH:mm:ss");

    protected ChessCommand(final ChessPlugin plugin) {
        super(plugin, "chess");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("mygames").denyTabCompletion()
            .description("List your games")
            .playerCaller(this::mygames);
    }

    private void mygames(Player player) {
        final var uuid = player.getUniqueId();
        plugin().getDatabase().find(SQLChessGame.class)
            .eq("whiteName", uuid)
            .or()
            .eq("blackName", uuid)
            .orderByDescending("startTime")
            .findListAsync(list -> mygamesCallback(player, list));
    }

    private void mygamesCallback(Player player, List<SQLChessGame> list) {
        if (list.isEmpty()) {
            player.sendMessage(text("No games to show", RED));
            return;
        }
        final List<Component> lines = new ArrayList<>();
        for (var row : list) {
            final Instant instant = Instant.ofEpochMilli(row.getStartTime().getTime());
            final LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZONE_ID);
            final int winner = row.getWinner();
            final List<Component> tooltip = new ArrayList<>();
            final ChessGame game = new ChessGame();
            game.loadPgnString(row.getPgn());
            final Component names = textOfChildren(text(game.getWhiteName(), GRAY).decoration(BOLD, winner == 1),
                                                   text(" vs "),
                                                   text(game.getBlackName(), DARK_GRAY).decoration(BOLD, winner == 2));
            tooltip.add(names);
            tooltip.add(text(localDateTime.format(DATE_TIME_FORMATTER), LIGHT_PURPLE, ITALIC));
            if (row.getLichessUrl() != null) {
                tooltip.add(text(row.getLichessUrl(), BLUE, ITALIC));
            }
            if (winner == 0) {
                tooltip.add(text("Draw", GOLD));
            } else if (winner == 1) {
                tooltip.add(text(game.getWhiteName() + " (White) wins", GOLD));
            } else if (winner == 2) {
                tooltip.add(text(game.getBlackName() + " (Black) wins", GOLD));
            }
            tooltip.add(text(toCamelCase(" ", List.of(row.getResult().split("_"))), GOLD));
            tooltip.add(text(game.getMoveCount() + " moves", GOLD));
            final var pgn = row.getPgn().split("\n");
            tooltip.add(text(pgn[pgn.length - 1], GRAY));
            if (row.getLichessUrl() != null) {
                lines.add(names.hoverEvent(showText(join(separator(newline()), tooltip)))
                          .clickEvent(openUrl(row.getLichessUrl())));
            } else {
                lines.add(names.hoverEvent(showText(join(separator(newline()), tooltip))));
            }
        }
        final List<Component> pages = new ArrayList<>();
        final int linesPerPage = 4;
        for (int offset = 0; offset < lines.size(); offset += linesPerPage) {
            final List<Component> page = new ArrayList<>();
            for (int i = 0; i < linesPerPage; i += 1) {
                final int linum = offset + i;
                if (linum >= lines.size()) break;
                page.add(lines.get(linum));
            }
            pages.add(join(separator(textOfChildren(newline(), newline())), page));
        }
        final var book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                if (!(m instanceof BookMeta meta)) return;
                meta.author(text("Cavetale"));
                meta.title(text("Chess"));
                meta.pages(pages);
            });
        player.closeInventory();
        player.openBook(book);
    }
}
