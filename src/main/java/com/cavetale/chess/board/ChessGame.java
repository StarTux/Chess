package com.cavetale.chess.board;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public final class ChessGame {
    private List<ChessTurn> turns;
    private ChessTurn currentTurn;
    private LocalDate startTime = LocalDate.now();
    private String eventName = "Cavetale Chess";
    private String siteName = "cavetale.com";
    private int roundNumber = 1;
    private String whiteName = "Unknown";
    private String blackName = "Unknown";

    public ChessGame() { }

    public void initialize() {
        turns = new ArrayList<>();
        final ChessBoard board = new ChessBoard();
        board.loadStartingPosition();
        currentTurn = new ChessTurn(null, board);
        turns.add(currentTurn);
        currentTurn.fillCache();
    }

    public void loadFenString(String fen) {
        turns = new ArrayList<>();
        final ChessBoard board = new ChessBoard();
        board.loadFenString(fen);
        currentTurn = new ChessTurn(null, board);
        turns.add(currentTurn);
        currentTurn.fillCache();
    }

    public ChessBoard getCurrentBoard() {
        return currentTurn.getBoard();
    }

    public boolean move(final ChessMove move) {
        if (currentTurn.getState().isGameOver()) return false;
        final ChessBoard nextBoard = currentTurn.getLegalMoves().get(move);
        if (nextBoard == null) return false;
        currentTurn.setNextMove(move);
        currentTurn = new ChessTurn(move, nextBoard);
        turns.add(currentTurn);
        currentTurn.fillCache();
        if (!currentTurn.getState().isGameOver()) {
            int repetitionCount = 1;
            for (ChessTurn oldTurn : turns) {
                if (oldTurn == currentTurn) continue;
                if (currentTurn.getBoard().isRepetitionOf(oldTurn.getBoard())) {
                    repetitionCount += 1;
                }
            }
            if (repetitionCount >= 3) {
                currentTurn.setDrawByRepetition();
            }
        }
        return true;
    }

    private static String escape(String in) {
        return in.replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("]", "\\]");
    }

    public String toPgnString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Event \"" + escape(eventName) + "\"]\n");
        sb.append("[Site \"" + escape(siteName) + "\"]\n");
        sb.append(String.format("[Date \"%04d.%02d.%02d\"]\n", startTime.getYear(), startTime.getMonthValue(), startTime.getDayOfMonth()));
        sb.append("[Round \"" + roundNumber + "\"]\n");
        sb.append("[White \"" + escape(whiteName) + "\"]\n");
        sb.append("[Black \"" + escape(blackName) + "\"]\n");
        sb.append("[Result \"" + getResultPgn() + "\"]\n");
        sb.append("\n");
        final var list = new ArrayList<String>();
        for (var turn : turns) {
            if (turn.getNextMove() == null) break;
            if (turn.getBoard().getActiveColor() == ChessColor.WHITE) {
                list.add(turn.getBoard().getFullMoveClock() + ".");
            }
            list.add(turn.getMoveText(turn.getNextMove()));
        }
        list.add(getResultPgn());
        sb.append(String.join(" ", list));
        return sb.toString();
    }

    private String getResultPgn() {
        if (!currentTurn.getState().isGameOver()) {
            return "*";
        } else if (currentTurn.getState().isDraw()) {
            return "1/2-1/2";
        } else {
            return currentTurn.getBoard().getActiveColor() == ChessColor.WHITE
                ? "0-1"
                : "1-0";
        }
    }

    public void loadPgnString(String pgn) {
        turns = new ArrayList<>();
        final ChessBoard board = new ChessBoard();
        board.loadStartingPosition();
        currentTurn = new ChessTurn(null, board);
        turns.add(currentTurn);
        currentTurn.fillCache();
        for (String line : pgn.split("\n")) {
            if (line.startsWith("[")) continue;
            if (line.startsWith(";")) continue;
            if (line.startsWith("{")) continue;
            if (line.isEmpty()) continue;
            loadPgnLine(line);
        }
    }

    private void loadPgnLine(String line) {
        while (!line.isEmpty()) {
            if (line.charAt(0) == ' ') {
                line = line.substring(1);
            } else if (line.charAt(0) == ('{')) {
                line = crawlComment(line);
            } else {
                final int spaceIndex = line.indexOf(' ');
                final String token;
                if (spaceIndex < 0) {
                    token = line;
                    line = "";
                } else {
                    token = line.substring(0, spaceIndex);
                    line = line.substring(spaceIndex + 1);
                }
                if (token.endsWith(".")) {
                    try {
                        final int turnNumber = Integer.parseInt(token.substring(0, token.length() - 1));
                        if (turnNumber != currentTurn.getBoard().getFullMoveClock()) {
                            throw new IllegalArgumentException("Bad turn number: " + turnNumber);
                        }
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException("Invalid movetext: " + token);
                    }
                } else if (token.equals("*")
                           || token.equals("1/2-1/2")
                           || token.equals("1-0")
                           || token.equals("0-1")) {
                    continue;
                } else {
                    parseMoveText(token);
                }
            }
        }
    }

    private void parseMoveText(String token) {
        final var move = currentTurn.getMoveTexts().get(token);
        if (move == null) {
            throw new IllegalArgumentException("Illegal move: "
                                               + getCurrentBoard().getFullMoveClock()
                                               + (getCurrentBoard().getActiveColor() == ChessColor.WHITE
                                                  ? ". "
                                                  : "... ")
                                               + token);
        }
        move(move);
    }

    private String crawlComment(String line) {
        int depth = 0;
        int index;
        for (index = 0; index < line.length(); index += 1) {
            final char c = line.charAt(index);
            switch (c) {
            case '{':
                depth += 1;
                break;
            case '}':
                depth -= 1;
                if (depth == 0) {
                    return line.substring(index + 1);
                }
                break;
            default: break;
            }
        }
        // End of line?
        return "";
    }
}
