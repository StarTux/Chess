package com.cavetale.chess.world;

import lombok.RequiredArgsConstructor;
import java.util.function.Supplier;

/**
 * Enumerate all the available sets of piece types, that is different
 * ways and looks to represent chess pieces in the world.
 */
@RequiredArgsConstructor
public enum ChessPieceSetType {
    DEFAULT_ENTITY(DefaultEntityChessPieceSet::new),
    ITEM_FRAME(ItemFrameChessPieceSet::new),
    ;

    private final Supplier<? extends ChessPieceSet> chessPieceSetSupplier;
    private ChessPieceSet chessPieceSet;

    public ChessPieceSet getChessPieceSet() {
        if (chessPieceSet == null) {
            chessPieceSet = chessPieceSetSupplier.get();
        }
        return chessPieceSet;
    }
}
