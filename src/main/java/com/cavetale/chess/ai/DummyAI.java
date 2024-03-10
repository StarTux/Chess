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
@Data
public final class DummyAI {
    private boolean debug;

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
            int repCount = 1;
            for (var old : game.getTurns()) {
                if (board.isRepetitionOf(old.getBoard())) {
                    repCount += 1;
                }
            }
            if (repCount >= 3) {
                consideration.total = Integer.MIN_VALUE;
            }
        }
        Collections.shuffle(considerations);
        Collections.sort(considerations, Comparator.comparing(Consideration::getTotal).reversed());
        if (debug) {
            for (int i = 0; i < 4 && i < considerations.size(); i += 1) {
                System.out.println(i + ". " + turn.getMoveText(considerations.get(i).getMove()) + " " + considerations.get(i));
            }
        }
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
        int coverScore;
        int takeScore;
        int castleScore;
        int total;

        private void compute(ChessTurn turn, ChessBoard board) {
            final var color = turn.getBoard().getActiveColor();
            final var enemy = color.other();
            final var piece = turn.getBoard().getPieceAt(move.from());
            // Control score
            for (var square : ChessSquare.values()) {
                if (board.isInCheck(square, enemy)) {
                    final var attackedPiece = board.getPieceAt(square);
                    if (attackedPiece == null || attackedPiece.color == enemy) {
                        controlScore += 1;
                    }
                    if (attackedPiece != null && attackedPiece.color != color) {
                        attackScore += attackedPiece.type.value;
                    }
                }
                if (board.isInCheck(square, color)) {
                    final var attackedPiece = board.getPieceAt(square);
                    if (attackedPiece == null || attackedPiece.color == color) {
                        counterControlScore += 1;
                    }
                    if (attackedPiece != null && attackedPiece.color == color) {
                        counterAttackScore += attackedPiece.type.value;
                    }
                }
            }
            // Pieces
            for (var entry : board.countPieces(color).entrySet()) {
                pieceScore += entry.getKey().getValue() * entry.getValue();
            }
            for (var entry : board.countPieces(enemy).entrySet()) {
                counterPieceScore += entry.getKey().getValue() * entry.getValue();
            }
            // Cover
            for (var square : ChessSquare.values()) {
                final var coveredPiece = board.getPieceAt(square);
                if (coveredPiece == null || coveredPiece.color != color) continue;
                if (board.isInCheck(square, enemy)) {
                    coverScore += coveredPiece.type.value;
                } else {
                    coverScore -= coveredPiece.type.value;
                }
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
                - (board.canCastleKingside(enemy) ? 1 : 0)
                + (board.canCastleQueenside(color) ? 1 : 0)
                - (board.canCastleQueenside(enemy) ? 1 : 0);
            // Final score
            total = controlScore * 3 - counterControlScore
                + (attackScore - counterAttackScore) * 10
                + (pieceScore - counterPieceScore) * 10
                + takeScore * 15
                + coverScore
                + castleScore * 5;
        }
    }
}
