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
    // Parameters
    private final String fenString;
    private final Consumer<ChessMove> callback;
    private int seconds = 5;
    private int skillLevel = 20;
    // Result
    private transient ChessMove bestMove;
    // Runtime
    private final int id = nextId++;
    private Process process;
    private BufferedReader out;
    private PrintStream in;
    private BukkitTask writerTask;
    private BukkitTask readerTask;
    private long startTime;
    private long endTime;
    // Static
    private static int nextId = 1;
    private static final List<StockfishAI> QUEUE = new ArrayList<>();
    private static StockfishAI current;

    public void schedule() {
        assert Bukkit.isPrimaryThread();
        QUEUE.add(this);
        plugin().getLogger().info("[StockfishAI " + id + "] Scheduled " + fenString);
        scheduleNext();
    }

    private static void scheduleNext() {
        assert Bukkit.isPrimaryThread();
        if (current != null) return;
        if (QUEUE.isEmpty()) return;
        current = QUEUE.remove(0);
        Bukkit.getScheduler().runTaskAsynchronously(plugin(), () -> {
                try {
                    current.start();
                } catch (Exception e) {
                    plugin().getLogger().log(Level.SEVERE, "[StockfishAI " + current.id + "] start", e);
                    current.finishInMainThread();
                }
            });
    }

    private static void finished() {
        assert Bukkit.isPrimaryThread();
        if (current != null) {
            plugin().getLogger().info("[StockfishAI " + current.id + "] Finished");
            current.cancel();
            current = null;
        }
        scheduleNext();
    }

    private void finishInMainThread() {
        Bukkit.getScheduler().runTask(plugin(), () -> {
                callback.accept(bestMove);
                finished();
            });
    }

    private void start() throws Exception {
        process = Runtime.getRuntime().exec(new String[] {"/home/mc/public/stockfish/stockfish"});
        out = new BufferedReader(new InputStreamReader(process.getInputStream()));
        in = new PrintStream(process.getOutputStream());
        startTime = System.currentTimeMillis();
        endTime = startTime + ((long) seconds) * 1000L;
        writerTask = Bukkit.getScheduler().runTaskAsynchronously(plugin(), () -> {
                try {
                    writer();
                } catch (Exception e) {
                    plugin().getLogger().log(Level.SEVERE, "[StockfishAI " + id + "] Writer task", e);
                }
            });
        readerTask = Bukkit.getScheduler().runTaskAsynchronously(plugin(), () -> {
                try {
                    reader();
                } catch (Exception e) {
                    plugin().getLogger().log(Level.SEVERE, "[StockfishAI " + id + "] Reader task", e);
                } finally {
                    finishInMainThread();
                }
            });
        plugin().getLogger().info("[StockfishAI " + id + "] Started");
    }

    private void cancel() {
        if (writerTask != null) {
            writerTask.cancel();
            writerTask = null;
        }
        if (readerTask != null) {
            readerTask.cancel();
            readerTask = null;
        }
    }

    private void writer() throws Exception {
        in.println("uci");
        in.println("position fen " + fenString);
        in.println("setoption name Skill Level value " + skillLevel);
        in.println("go");
        in.flush();
        do {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException ie) { }
        } while (bestMove == null && System.currentTimeMillis() < endTime);
        in.println("stop");
        in.println("quit");
        in.flush();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException ie) { }
        if (process.waitFor(5L, TimeUnit.SECONDS)) return;
        process.destroy();
        if (process.waitFor(5L, TimeUnit.SECONDS)) return;
        process.destroyForcibly();
    }

    private void reader() throws Exception {
        while (true) {
            final String line = out.readLine();
            if (line == null) break;
            if (line.startsWith("bestmove ")) {
                String[] tokens = line.split(" ");
                if (tokens.length < 2) continue;
                bestMove = ChessMove.fromString(tokens[1]);
                final double secondsUsed = (double) (System.currentTimeMillis() - startTime) / 1000.0;
                plugin().getLogger().info("[StockfishAI " + id + "] Found solution after "
                                          + String.format("%.2f", secondsUsed)
                                          + "/" + seconds + " seconds: " + bestMove);
            }
        }
    }
}
