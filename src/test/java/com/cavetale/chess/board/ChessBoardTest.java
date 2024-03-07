package com.cavetale.chess.board;

import org.junit.Assert;
import org.junit.Test;

public final class ChessBoardTest {
    @Test
    public void test() {
        ChessBoard board = new ChessBoard();
        board.loadFenString(ChessBoard.FEN_START);
        Assert.assertEquals(board.toFenString(), ChessBoard.FEN_START);
        //
        board.move(ChessSquare.E2, ChessSquare.E4, null);
        board.move(ChessSquare.C7, ChessSquare.C5, null);
        board.move(ChessSquare.G1, ChessSquare.F3, null);
        final String fen2 = "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2";
        Assert.assertEquals(board.toFenString(), fen2);
    }
}
