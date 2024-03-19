package com.cavetale.chess.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChessEngineType {
    DUMMY("Dummy"),
    STOCKFISH("Stockfish");

    private final String displayName;
}
