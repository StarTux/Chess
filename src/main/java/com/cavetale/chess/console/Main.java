package com.cavetale.chess.console;

import com.cavetale.chess.board.ChessBoard;
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
    final ChessBoard board = new ChessBoard();
    final Random random = new Random();

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            new Main().run(String.join(" ", args));
        } else {
            new Main().run(null);
        }
    }

    private void run(String fen) throws IOException {
        if (fen == null) {
            board.loadStartingPosition();
        } else {
            board.loadFenString(fen);
        }
        final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            final var legalMoves = board.getLegalMoves();
            final var moveTexts = board.getMoveTexts(legalMoves);
            System.out.println(board.toAsciiBoard());
            if (legalMoves.isEmpty()) {
                if (board.isKingInCheck()) {
                    System.out.println("Checkmate for " + board.getActiveColor().other().getHumanName());
                } else {
                    System.out.println("Stalemate!");
                }
                break;
            } else if (board.isKingInCheck()) {
                System.out.println(board.getActiveColor().getHumanName() + " is in check!");
            }
            if (board.getActiveColor() == ChessColor.WHITE) {
                // Player
                while (true) {
                    String line = stdin.readLine();
                    if (line.isEmpty()) continue;
                    if (line.equals("fen")) {
                        System.out.println(board.toFenString());
                        continue;
                    } else if (line.equals("board")) {
                        System.out.println(board.toAsciiBoard());
                        continue;
                    } else if (line.equals("moves")) {
                        final var moves = new ArrayList<>(moveTexts.keySet());
                        Collections.sort(moves);
                        System.out.println(moves.size() + " " + moves);
                        continue;
                    }
                    ChessMove move = moveTexts.get(line);
                    if (move != null) {
                        move(move);
                        break;
                    }
                    // Try partial commands
                    if (line.length() >= 2) {
                        for (var entry : moveTexts.entrySet()) {
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
                    for (var entry : moveTexts.entrySet()) {
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
                List<ChessMove> list = List.copyOf(legalMoves.keySet());
                final ChessMove move = list.get(random.nextInt(list.size()));
                move(move);
            }
        }
    }

    private void move(ChessMove move) {
        final var piece = board.getPieceAt(move.from());
        final var taken = board.getPieceAt(move.to());
        final var color = board.getActiveColor();
        board.move(move);
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
                           + (board.isKingInCheck()
                              ? " Check!"
                              : ""));
    }
}
