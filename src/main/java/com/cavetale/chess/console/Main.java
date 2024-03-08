package com.cavetale.chess.console;

import com.cavetale.chess.board.ChessGame;
import com.cavetale.chess.board.ChessColor;
import com.cavetale.chess.board.ChessMove;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class Main {
    ChessGame game = new ChessGame();
    final Random random = new Random();

    public static void main(String[] args) throws IOException {
        String fenString = null;
        boolean whiteCPU = false;
        boolean blackCPU = false;
        for (int i = 0; i < args.length; i += 1) {
            final String arg = args[i];
            switch (arg) {
            case "-f": case "--fen":
                fenString = args[i++];
                break;
            case "-w": case "--whitecpu":
                whiteCPU = true;
                break;
            case "-b": case "--blackcpu":
                blackCPU = true;
                break;
            default:
                System.err.println("Illegal argument: " + arg);
                System.exit(1);
            }
        }
        new Main().run(fenString, whiteCPU, blackCPU);
    }

    private void run(final String fen, final boolean whiteCPU, final boolean blackCPU) throws IOException {
        if (fen == null) {
            game.initialize();
        } else {
            game.loadFenString(fen);
        }
        final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println(game.getCurrentBoard().getFullMoveClock()
                               + (game.getCurrentBoard().getActiveColor() == ChessColor.WHITE ? "." : "..."));
            System.out.println(game.getCurrentBoard().toAsciiBoard());
            final var turn = game.getCurrentTurn();
            final var board = turn.getBoard();
            final var color = board.getActiveColor();
            if (turn.isGameOver()) {
                System.out.println(game.toPgnString());
                if (turn.isCheckmate()) {
                    System.out.println(color.other().getHumanName() + " wins by checkmate!");
                } else if (turn.isStalemate()) {
                    System.out.println("Stalemate!");
                } else if (turn.isDrawByRepetition()) {
                    System.out.println("Draw by repetition!");
                } else if (turn.isDrawByInsufficientMaterial()) {
                    System.out.println("Draw by insufficient material");
                } else if (turn.isDrawByFiftyMoveRule()) {
                    System.out.println("Draw by fifty move rule");
                } else if (turn.isTimeoutDraw()) {
                    System.out.println("Draw by timeout");
                } else if (turn.isTimeout()) {
                    System.out.println(color.other().getHumanName() + " wins by timeout!");
                } else {
                    System.out.println("???");
                }
                break;
            }
            final boolean cpu = (color == ChessColor.WHITE && whiteCPU)
                || (color == ChessColor.BLACK && blackCPU);
            if (!cpu) {
                // Player
                while (true) {
                    String line = stdin.readLine();
                    if (line.isEmpty()) {
                        final var moves = new ArrayList<>(turn.getMoveTexts().keySet());
                        Collections.sort(moves);
                        System.out.println(moves.size() + " " + moves);
                        continue;
                    }
                    if (line.equals("fen")) {
                        System.out.println(board.toFenString());
                        continue;
                    } else if (line.equals("loadfen")) {
                        final StringBuilder sb = new StringBuilder();
                        while (true) {
                            final String line2 = stdin.readLine();
                            if (line2 == null) break;
                            sb.append(line2);
                        }
                        final ChessGame newGame = new ChessGame();
                        try {
                            newGame.loadFenString(sb.toString());
                            game = newGame;
                            break;
                        } catch (IllegalArgumentException iae) {
                            System.err.println("Error: " + iae.getMessage());
                        }
                        continue;
                    } else if (line.equals("pgn")) {
                        System.out.println(game.toPgnString());
                        continue;
                    } else if (line.equals("loadpgn")) {
                        final StringBuilder sb = new StringBuilder();
                        while (true) {
                            final String line2 = stdin.readLine();
                            if (line2 == null) break;
                            sb.append(line2);
                            sb.append("\n");
                        }
                        final ChessGame newGame = new ChessGame();
                        try {
                            newGame.loadPgnString(sb.toString());
                            game = newGame;
                            break;
                        } catch (IllegalArgumentException iae) {
                            System.err.println("Error: " + iae.getMessage());
                        }
                        continue;
                    } else if (line.equals("board")) {
                        System.out.println(board.toAsciiBoard());
                        continue;
                    } else if (line.equals("moves")) {
                        final var moves = new ArrayList<>(turn.getMoveTexts().keySet());
                        Collections.sort(moves);
                        System.out.println(moves.size() + " " + moves);
                        continue;
                    } else if (line.equals("random")) {
                        List<ChessMove> list = List.copyOf(turn.getLegalMoves().keySet());
                        move(list.get(random.nextInt(list.size())));
                        break;
                    }
                    ChessMove move = turn.getMoveTexts().get(line);
                    if (move != null) {
                        move(move);
                        break;
                    }
                    // Try partial commands
                    if (line.length() >= 2) {
                        for (var entry : turn.getMoveTexts().entrySet()) {
                            if (!entry.getKey().startsWith(line)) continue;
                            if (move != null) {
                                move = null;
                                break;
                            }
                            move = entry.getValue();
                        }
                        if (move != null) {
                            move(move);
                            break;
                        }
                    }
                    // Guess options
                    final var options = new ArrayList<String>();
                    for (var entry : turn.getMoveTexts().entrySet()) {
                        if (!entry.getKey().contains(line)) continue;
                        options.add(entry.getKey());
                    }
                    if (!options.isEmpty()) {
                        Collections.sort(options);
                        System.out.println(options.size() + " " + options);
                    } else {
                        System.err.println("Invalid command: " + line);
                    }
                }
            } else {
                assert cpu;
                try {
                    Thread.sleep(250L);
                } catch (InterruptedException ie) { }
                List<ChessMove> list = List.copyOf(turn.getLegalMoves().keySet());
                move(list.get(random.nextInt(list.size())));
            }
        }
    }

    private void move(ChessMove move) {
        final var board = game.getCurrentBoard();
        final var piece = board.getPieceAt(move.from());
        final var taken = board.getPieceAt(move.to());
        final var color = board.getActiveColor();
        final var check = board.isKingInCheck();
        game.move(move);
        System.out.println(color.getHumanName()
                           + " moves " + piece.getType().getHumanName()
                           + " from " + move.from().getName()
                           + " to " + move.to().getName()
                           + (taken != null
                              ? " and takes " + taken.getType().getHumanName()
                              : "")
                           + (move.promotion() != null
                              ? " and promotes to " + move.promotion().getHumanName()
                              : "")
                           + "."
                           + (check
                              ? " Check!"
                              : ""));
    }
}
