package com.cavetale.chess.world;

import com.cavetale.chess.board.ChessColor;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Gui;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class ChessAcceptMenu {
    private final Player player;
    private final WorldChessBoard worldChessBoard;
    private final UUID otherPlayer;

    private static final TextColor HIGHLIGHT = BLUE;

    public void open() {
        final int size = 9;
        final String otherName = PlayerCache.nameForUuid(otherPlayer);
        final var builder = GuiOverlay.BLANK.builder(size, DARK_GRAY)
            .title(text("Play Chess with " + otherName + "?", WHITE));
        final Gui gui = new Gui().size(size);
        final ChessColor queueColor = worldChessBoard.getSaveTag().getQueueColor();
        if (queueColor != ChessColor.WHITE) {
            gui.setItem(2, Mytems.WHITE_QUEEN.createIcon(List.of(text("Play as White", GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f);
                    clickAccept(ChessColor.WHITE);
                });
        }
        if (queueColor == null) {
            gui.setItem(4, Mytems.DICE.createIcon(List.of(text("Play Random Color", GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f);
                    clickAccept(null);
                });
        }
        if (queueColor != ChessColor.BLACK) {
            gui.setItem(6, Mytems.BLACK_QUEEN.createIcon(List.of(text("Play as Black", GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f);
                    clickAccept(ChessColor.BLACK);
                });
        }
        gui.title(builder.build());
        gui.open(player);
    }

    private void clickAccept(ChessColor color) {
        player.closeInventory();
        if (worldChessBoard.getSaveTag().getState() != ChessSaveTag.ChessState.WAITING) {
            player.sendMessage(text("A game has already started", RED));
            return;
        }
        if (worldChessBoard.getSaveTag().getQueue().size() != 1 || !otherPlayer.equals(worldChessBoard.getSaveTag().getQueue().get(0))) {
            player.sendMessage(text("The challenge has been cancelled", RED));
            return;
        }
        worldChessBoard.addToQueue(player, color);
    }
}
