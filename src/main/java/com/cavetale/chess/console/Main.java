package com.cavetale.chess.console;

import com.cavetale.chess.board.ChessBoard;
import com.cavetale.chess.board.ChessColor;
import com.cavetale.chess.board.ChessMove;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
                    if (line.equals("fen")) {
                        System.out.println(board.toFenString());
                        continue;
                    } else if (line.equals("board")) {
                        System.out.println(board.toAsciiBoard());
                        continue;
                    } else if (line.equals("moves")) {
                        System.out.println(List.copyOf(legalMoves.keySet()));
                        continue;
                    }
                    final ChessMove move = ChessMove.fromString(line);
                    if (move != null && legalMoves.containsKey(move)) {
                        move(move);
                        break;
                    }
                    final List<String> output = new ArrayList<>();
                    for (ChessMove it : legalMoves.keySet()) {
                        if (it.toString().startsWith(line)) {
                            output.add(it.toString());
                        }
                    }
                    if (!output.isEmpty()) {
                        System.out.println(output);
                        continue;
                    }
                    System.err.println("Invalid command: " + line);
                }
            } else {
                List<ChessMove> list = List.copyOf(legalMoves.keySet());
                final ChessMove move = list.get(random.nextInt(list.size()));
                move(move);
            }
        }
    }

    private void move(ChessMove move) {
        System.out.println(board.getActiveColor().getHumanName()
                           + " moves " + board.getPieceAt(move.from()).getType().getHumanName()
                           + " from " + move.from().getName()
                           + " to " + move.to().getName()
                           + (move.promotion() != null
                              ? " and promotes to " + move.promotion().getHumanName()
                              : ""));
        board.move(move);
    }
}
