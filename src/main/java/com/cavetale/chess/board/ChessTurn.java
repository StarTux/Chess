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
    private ChessTurnState state;
    // Externally set
    @Setter private ChessMove nextMove;

    public void fillCache() {
        if (legalMoves == null) {
            legalMoves = board.getLegalMoves();
        }
        if (moveTexts == null) {
            moveTexts = board.getMoveTexts(legalMoves);
        }
        state = computeState();
    }

    public void clearCache() {
        legalMoves = null;
        moveTexts = null;
    }

    public void setDrawByRepetition() {
        state = ChessTurnState.DRAW_BY_REPETITION;
    }

    public void setTimeout() {
        state = isTimeoutDraw() ? ChessTurnState.TIMEOUT_DRAW : ChessTurnState.TIMEOUT;
    }

    public void setAggreeToDraw() {
        state = ChessTurnState.DRAW_BY_AGREEMENT;
    }

    public String getMoveText(ChessMove move) {
        for (var it : moveTexts.entrySet()) {
            if (it.getValue().equals(move)) return it.getKey();
        }
        return null;
    }

    public ChessColor getWinner() {
        if (!state.gameOver || state.draw) return null;
        return board.getActiveColor().other();
    }

    private ChessTurnState computeState() {
        final boolean check = board.isKingInCheck();
        if (check && legalMoves.isEmpty()) return ChessTurnState.CHECKMATE;
        if (!check && legalMoves.isEmpty()) return ChessTurnState.STALEMATE;
        if (board.getHalfMoveClock() >= 50) return ChessTurnState.DRAW_BY_FIFTY_MOVE_RULE;
        final var whiteCounts = board.countPieces(ChessColor.WHITE);
        final var blackCounts = board.countPieces(ChessColor.BLACK);
        if (isKingVsKing(whiteCounts, blackCounts)
            || isKingVsKingAndBishopOrKnight(whiteCounts, blackCounts)
            || isKingVsKingAndBishopOrKnight(blackCounts, whiteCounts)
            || isBothKingAndBishopOfSameSquareColor(whiteCounts, blackCounts)) {
            return ChessTurnState.DRAW_BY_INSUFFICIENT_MATERIAL;
        }
        return check ? ChessTurnState.CHECK : ChessTurnState.PLAY;
    }

    /**
     * This is called once.
     */
    private static boolean isKingVsKing(Map<ChessPieceType, Integer> white, Map<ChessPieceType, Integer> black) {
        return white.size() == 1 && black.size() == 1;
    }

    /**
     * This is called twice.
     */
    private static boolean isKingVsKingAndBishopOrKnight(Map<ChessPieceType, Integer> a, Map<ChessPieceType, Integer> b) {
        return a.size() == 1 && b.size() == 2
            && (b.getOrDefault(ChessPieceType.BISHOP, 0) == 1
                || b.getOrDefault(ChessPieceType.KNIGHT, 0) == 1);
    }

    /**
     * This is called once.
     */
    private boolean isBothKingAndBishopOfSameSquareColor(Map<ChessPieceType, Integer> white, Map<ChessPieceType, Integer> black) {
        if (white.size() != 2 || white.getOrDefault(ChessPieceType.BISHOP, 0) != 1
            || black.size() != 2 || black.getOrDefault(ChessPieceType.BISHOP, 0) != 1) {
            return false;
        }
        // King and Bishop vs King and Bishop of same colored square
        final ChessSquare w = board.findFirstPiece(ChessPiece.WHITE_BISHOP);
        final ChessSquare b = board.findFirstPiece(ChessPiece.BLACK_BISHOP);
        return w != null && b != null && w.getColor() == b.getColor();
    }

    /**
     * Active color ran out of time.  It's a draw if the other player
     * has insufficient material.
     */
    private boolean isTimeoutDraw() {
        final ChessColor color = board.getActiveColor().other();
        final var counts = board.countPieces(color);
        return counts.size() == 1
            || (counts.size() == 2
                && counts.getOrDefault(ChessPieceType.BISHOP, 0) == 1
                && counts.getOrDefault(ChessPieceType.KNIGHT, 0) == 1);
    }
}
