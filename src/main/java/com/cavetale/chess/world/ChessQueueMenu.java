package com.cavetale.chess.world;

import com.cavetale.chess.ai.ChessEngineType;
import com.cavetale.chess.board.ChessColor;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Gui;
import com.cavetale.mytems.util.Items;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class ChessQueueMenu {
    private final Player player;
    private final WorldChessBoard worldChessBoard;
    private ChessColor color = null;
    private ChessEngineType chessEngineType = null;
    private int stockfishLevel = 0;

    private static final TextColor HIGHLIGHT = BLUE;

    public void open() {
        final int size = 6 * 9;
        final var builder = GuiOverlay.BLANK.builder(size, DARK_GRAY)
            .title(text("Play Chess?", WHITE));
        final Gui gui = new Gui().size(size);
        // Color selection
        final int whiteIndex = 3;
        final int randomIndex = 4;
        final int blackIndex = 5;
        if (color == ChessColor.WHITE) {
            builder.highlightSlot(whiteIndex, HIGHLIGHT);
        } else if (color == ChessColor.BLACK) {
            builder.highlightSlot(blackIndex, HIGHLIGHT);
        } else {
            builder.highlightSlot(randomIndex, HIGHLIGHT);
        }
        gui.setItem(whiteIndex, Mytems.WHITE_QUEEN.createIcon(List.of(text("Play as White", GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f);
                    color = ChessColor.WHITE;
                    open();
                });
        gui.setItem(randomIndex, Mytems.DICE.createIcon(List.of(text("Play Random Color", GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f);
                    color = null;
                    open();
                });
        gui.setItem(blackIndex, Mytems.BLACK_QUEEN.createIcon(List.of(text("Play as Black", GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f);
                    color = ChessColor.BLACK;
                    open();
                });
        // Player Opponent
        final int playerIndex = 10;
        if (chessEngineType == null) {
            builder.highlightSlot(playerIndex, HIGHLIGHT);
        }
        gui.setItem(playerIndex, Items.text(new ItemStack(Material.PLAYER_HEAD), List.of(text("Challenge another player", GRAY))), click -> {
                if (!click.isLeftClick()) return;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f);
                chessEngineType = null;
                open();
            });
        // Dummy AI
        final int dummyIndex = 16;
        if (chessEngineType == ChessEngineType.DUMMY) {
            builder.highlightSlot(dummyIndex, HIGHLIGHT);
        }
        gui.setItem(dummyIndex, Items.text(new ItemStack(Material.COMPARATOR), List.of(text("Play against easy Computer", GRAY))), click -> {
                    if (!click.isLeftClick()) return;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f);
                    chessEngineType = ChessEngineType.DUMMY;
                    open();
            });
        // Stockfish AI
        int opponentIndex = 19;
        for (int level = 0; level <= 20; level += 1) {
            final var icon = new ItemStack(level == 0 ? Material.COD : Material.COOKED_COD);
            if (level >= 1) icon.setAmount(level);
            final List<Component> tooltip = List.of(text("Play against Stockfish", GRAY),
                                                    text("Level " + level, DARK_GRAY));
            if (chessEngineType == ChessEngineType.STOCKFISH && stockfishLevel == level) {
                builder.highlightSlot(opponentIndex, HIGHLIGHT);
            }
            final int finalLevel = level;
            gui.setItem(opponentIndex++, Items.text(icon, tooltip), click -> {
                    if (!click.isLeftClick()) return;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f);
                    chessEngineType = ChessEngineType.STOCKFISH;
                    stockfishLevel = finalLevel;
                    open();
                });
            if (opponentIndex % 9 == 8) opponentIndex += 2;
        }
        gui.setItem(49, Mytems.OK.createIcon(List.of(text("Start the Game", GREEN))), click -> {
                if (!click.isLeftClick()) return;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f);
                clickOk();
            });
        gui.title(builder.build());
        gui.open(player);
    }

    private void clickOk() {
        player.closeInventory();
        if (worldChessBoard.getSaveTag().getState() != ChessSaveTag.ChessState.WAITING) {
            player.sendMessage(text("A game has already started", RED));
            return;
        }
        if (chessEngineType != null) {
            if (!worldChessBoard.getSaveTag().getQueue().isEmpty()) {
                player.sendMessage(text("A player is now challenging you", RED));
                return;
            }
            worldChessBoard.startCPU(player, color, chessEngineType, p -> p.setStockfishLevel(stockfishLevel));
        } else {
            worldChessBoard.addToQueue(player, color);
        }
    }
}