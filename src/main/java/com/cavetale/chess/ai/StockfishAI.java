package com.cavetale.chess.ai;

import com.cavetale.chess.board.ChessMove;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import static com.cavetale.chess.ChessPlugin.plugin;

@Data
@RequiredArgsConstructor
public final class StockfishAI {
    private final String fenString;
    private final Consumer<ChessMove> callback;
    private int seconds = 5;
    private int skillLevel = 20;
    private ChessMove bestMove;
    private static final List<StockfishAI> QUEUE = new ArrayList<>();
    private static BukkitTask backgroundTask;

    public void schedule() {
        assert Bukkit.isPrimaryThread();
        QUEUE.add(this);
        scheduleNext();
    }

    private static void scheduleNext() {
        assert Bukkit.isPrimaryThread();
        if (backgroundTask != null) return;
        if (QUEUE.isEmpty()) return;
        final StockfishAI ai = QUEUE.remove(0);
        backgroundTask = Bukkit.getScheduler().runTaskAsynchronously(plugin(), ai::thread);
    }

    private static void finished() {
        assert Bukkit.isPrimaryThread();
        if (backgroundTask != null) {
            backgroundTask.cancel();
            backgroundTask = null;
        }
        scheduleNext();
    }

    private void task() throws Exception {
        final Process process = Runtime.getRuntime().exec("/home/mc/public/stockfish/stockfish");
        final BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final PrintStream in = new PrintStream(process.getOutputStream());
        in.println("uci");
        in.println("position fen " + fenString);
        in.println("setoption name Skill Level value " + skillLevel);
        in.println("go");
        in.flush();
        Thread.sleep((long) seconds * 1000L);
        in.println("stop");
        in.println("quit");
        in.flush();
        while (true) {
            final String line = out.readLine();
            if (line == null) break;
            if (line.startsWith("bestmove ")) {
                String[] tokens = line.split(" ");
                if (tokens.length < 2) continue;
                bestMove = ChessMove.fromString(tokens[1]);
            }
        }
        if (process.waitFor(5L, TimeUnit.SECONDS)) return;
        process.destroy();
        if (process.waitFor(5L, TimeUnit.SECONDS)) return;
        process.destroyForcibly();
    }

    private void thread() {
        try {
            task();
        } catch (Exception e) {
            plugin().getLogger().log(Level.SEVERE, "Stockfish task", e);
        }
        Bukkit.getScheduler().runTask(plugin(), () -> {
                callback.accept(bestMove);
                finished();
            });
    }
}
