package com.cavetale.chess.ai;

import com.cavetale.chess.board.ChessBoard;
import com.cavetale.chess.board.ChessGame;
import com.cavetale.chess.board.ChessMove;
import com.cavetale.chess.board.ChessSquare;
import com.cavetale.chess.board.ChessTurn;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * The dummy AI looks at all legal moves, gives it a score, and
 * chooses the top score.
 * The resulting accuracy according to Stockfish is all over the
 * place, ranging from as low as 35% to over 90%.
 * Most notably, the score encourages early Queen movements.  Perhaps
 * it could be optimized in the future.
 */
public final class DummyAI {
    public ChessMove getBestMove(ChessGame game) {
        final var turn = game.getCurrentTurn();
        final List<Consideration> considerations = new ArrayList<>(turn.getLegalMoves().size());
        for (var entry : turn.getLegalMoves().entrySet()) {
            final var move = entry.getKey();
            final var board = entry.getValue();
            if (board.isKingInCheck() && board.getLegalMoves().isEmpty()) {
                // Checkmate
                return move;
            }
            Consideration consideration = new Consideration(move);
            consideration.compute(turn, board);
            considerations.add(consideration);
            for (var old : game.getTurns()) {
                int repCount = 1;
                if (board.isRepetitionOf(old.getBoard())) {
                    System.out.println(" REP " + repCount + " " + board.toFenString() + " " + consideration);
                    repCount += 1;
                }
                if (repCount >= 3) {
                    consideration.total = Integer.MIN_VALUE;
                }
            }
        }
        Collections.shuffle(considerations);
        Collections.sort(considerations, Comparator.comparing(Consideration::getTotal).reversed());
        return considerations.get(0).getMove();
    }

    @Data
    @RequiredArgsConstructor
    private static final class Consideration {
        final ChessMove move;
        int controlScore;
        int counterControlScore;
        int attackScore;
        int counterAttackScore;
        int pieceScore;
        int counterPieceScore;
        int takeScore;
        int castleScore;
        int clockScore;
        int total;

        private void compute(ChessTurn turn, ChessBoard board) {
            final var color = turn.getBoard().getActiveColor();
            final var piece = turn.getBoard().getPieceAt(move.from());
            // Control score
            for (var square : ChessSquare.values()) {
                if (board.isInCheck(square, color.other())) {
                    controlScore += 1;
                    final var attackedPiece = board.getPieceAt(square);
                    if (attackedPiece != null && attackedPiece.color != color) {
                        attackScore += attackedPiece.type.value;
                    }
                }
                if (board.isInCheck(square, color)) {
                    counterControlScore += 1;
                    final var attackedPiece = board.getPieceAt(square);
                    if (attackedPiece != null && attackedPiece.color == color) {
                        counterAttackScore += attackedPiece.type.value;
                    }
                }
            }
            // Pieces
            for (var entry : board.countPieces(color).entrySet()) {
                pieceScore += entry.getKey().getValue() * entry.getValue();
            }
            for (var entry : board.countPieces(color.other()).entrySet()) {
                counterPieceScore += entry.getKey().getValue() * entry.getValue();
            }
            // Taking
            final var taken = turn.getBoard().getPieceAt(move.to());
            if (taken != null) {
                takeScore += taken.getType().getValue();
            }
            final boolean toInCheck = board.isInCheck(move.to(), color);
            if (toInCheck) {
                takeScore -= piece.getType().getValue();
            }
            // Castle
            castleScore = (board.canCastleKingside(color) ? 1 : 0)
                - (board.canCastleKingside(color.other()) ? 1 : 0)
                + (board.canCastleQueenside(color) ? 1 : 0)
                - (board.canCastleQueenside(color.other()) ? 1 : 0);
            clockScore = board.getHalfMoveClock();
            // Final score
            total = (controlScore - counterControlScore)
                + (attackScore - counterAttackScore)
                + (pieceScore - counterPieceScore) * 10
                + takeScore * 15
                + castleScore * 5
                - clockScore;
        }
    }
}
