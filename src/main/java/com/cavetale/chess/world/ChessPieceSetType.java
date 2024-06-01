package com.cavetale.chess.world;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

/**
 * Enumerate all the available sets of piece types, that is different
 * ways and looks to represent chess pieces in the world.
 */
@RequiredArgsConstructor
public enum ChessPieceSetType {
    DEFAULT_ENTITY(DefaultEntityChessPieceSet::new),
    ITEM_FRAME(ItemFrameChessPieceSet::new),
    VERTICAL_BILLBOARD(VerticalBillboardChessPieceSet::new),
    X_BILLBOARD(XBillboardChessPieceSet::new),
    SCREEN_BILLBOARD(ScreenBillboardChessPieceSet::new),
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
