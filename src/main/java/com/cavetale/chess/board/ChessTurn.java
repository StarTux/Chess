package com.cavetale.chess.board;

import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public final class ChessTurn {
    private final ChessMove previousMove;
    private final ChessBoard board;
    private Map<ChessMove, ChessBoard> legalMoves;
    private Map<String, ChessMove> moveTexts;
    private boolean check;
    private boolean checkmate;
    private boolean stalemate;
    private boolean drawByFiftyMoveRule;
    private boolean drawByInsufficientMaterial;
    // Externally set
    @Setter private boolean drawByRepetition;
    @Setter private boolean timeout;
    @Setter private ChessMove nextMove;

    public void fillCache() {
        if (legalMoves == null) {
            legalMoves = board.getLegalMoves();
        }
        if (moveTexts == null) {
            moveTexts = board.getMoveTexts(legalMoves);
        }
    }

    public void clearCache() {
        legalMoves = null;
        moveTexts = null;
    }

    public void computeState() {
        fillCache();
        check = board.isKingInCheck();
        checkmate = check && legalMoves.isEmpty();
        stalemate = !check && legalMoves.isEmpty();
        drawByFiftyMoveRule = board.getHalfMoveClock() >= 50;
        // May be imprecise. King and 2 Knights is not considered!
        drawByInsufficientMaterial = computeInsufficientMaterial(board.countPieces(ChessColor.WHITE))
            && computeInsufficientMaterial(board.countPieces(ChessColor.BLACK));
    }

    public boolean isGameOver() {
        return checkmate || timeout || isDraw();
    }

    public boolean isDraw() {
        return stalemate || drawByFiftyMoveRule || drawByInsufficientMaterial || drawByRepetition || isTimeoutDraw();
    }

    public ChessTurn move(ChessMove move) {
        return null;
    }

    public boolean isTimeoutDraw() {
        return timeout && computeInsufficientMaterial(board.countPieces(board.getActiveColor().other()));
    }

    public String getMoveText(ChessMove move) {
        for (var it : moveTexts.entrySet()) {
            if (it.getValue().equals(move)) return it.getKey();
        }
        return null;
    }

    private static boolean computeInsufficientMaterial(Map<ChessPieceType, Integer> count) {
        // We assume that the mapping {KING:1} exists.
        return count.size() == 1 // Lone King
            || (count.size() == 2 && count.getOrDefault(ChessPieceType.BISHOP, 0) == 1)
            || (count.size() == 2 && count.getOrDefault(ChessPieceType.KNIGHT, 0) == 1);
    }
}
