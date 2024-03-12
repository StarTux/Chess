package com.cavetale.chess.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Every possible turn state.
 * (false, ...) -> Game carries on
 * (true, true) -> draw
 * (true, false) -> other player wins
 */
@Getter
@RequiredArgsConstructor
public enum ChessTurnState {
    PLAY(false, false),
    CHECK(false, false),
    CHECKMATE(true, false),
    STALEMATE(true, true),
    DRAW_BY_FIFTY_MOVE_RULE(true, true),
    DRAW_BY_INSUFFICIENT_MATERIAL(true, true),
    DRAW_BY_REPETITION(true, true),
    DRAW_BY_AGREEMENT(true, true),
    TIMEOUT_DRAW(true, true),
    TIMEOUT(true, false),
    RESIGNATION(true, false),
    ;

    public final boolean gameOver;
    public final boolean draw;
}
