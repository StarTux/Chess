package com.cavetale.chess.net;

import com.cavetale.chess.board.ChessGame;
import com.cavetale.core.util.Json;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import static com.cavetale.chess.ChessPlugin.plugin;

@RequiredArgsConstructor
public final class LichessImport {
    private final ChessGame game;
    private final Consumer<String> callback;

    public HttpResponse request() {
        final String post = "pgn=" + URLEncoder.encode(game.toPgnString().replace("\n", "\r\n"),
                                                       StandardCharsets.UTF_8);
        final var client = HttpClient.newHttpClient();
        final var request = HttpRequest.newBuilder()
            .uri(URI.create("https://lichess.org/api/import"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(post))
            .build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ioe) {
            plugin().getLogger().log(Level.SEVERE, request.toString(), ioe);
            return null;
        } catch (InterruptedException ie) {
            plugin().getLogger().log(Level.SEVERE, request.toString(), ie);
            return null;
        }
    }

    private String sync() {
        final var response = request();
        if (response == null) return null;
        switch (response.statusCode()) {
        case 200:
            final String json = response.body().toString();
            final Map<?, ?> map = Json.deserialize(json, Map.class);
            final var object = map.get("url");
            if (object == null) {
                plugin().getLogger().severe("Unexpected LichessImport 200"
                                          + " body=" + response.body());
                return null;
            }
            return object.toString();
        case 303:
            final var list = response.headers().map().get("location");
            if (list == null || list.isEmpty()) {
                plugin().getLogger().severe("Unexpected LichessImport 303"
                                          + " header=" + response.headers());
                return null;
            }
            return "https://lichess.org" + list.get(0);
        default:
            plugin().getLogger().severe("Unexpected LichessImport HTTP"
                                      + " response=" + response
                                      + " header=" + response.headers()
                                      + " body=" + response.body());
            return null;
        }
    }

    public void async() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin(), () -> {
                final String result = sync();
                Bukkit.getScheduler().runTask(plugin(), () -> callback.accept(result));
            });
    }
}
