package com.cavetale.chess.sql;

import com.cavetale.chess.board.ChessColor;
import com.cavetale.chess.world.WorldChessBoard;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow;
import java.util.Date;
import lombok.Data;

@Name("games")
@NotNull
@Data
public final class SQLChessGame implements SQLRow {
    @Id private Integer id;
    private Date startTime;
    private Date endTime;
    @VarChar(15) private String timeBank;
    private int whiteType;
    @VarChar(40) private String whiteName;
    private int blackType;
    @VarChar(40) private String blackName;
    private int moves;
    @Text private String pgn;
    @VarChar(40) private String result;
    private int winner;
    @Nullable private String lichessUrl;

    public SQLChessGame() { }

    public SQLChessGame(final WorldChessBoard wcb) {
        startTime = new Date(wcb.getSaveTag().getStartTime());
        endTime = new Date();
        timeBank = "" + (wcb.getSaveTag().getTimeBank() / 1000L / 60L)
            + (wcb.getSaveTag().getTimeIncrement() != 0
               ? "|" + (wcb.getSaveTag().getTimeIncrement() / 1000L)
               : "");
        final var white = wcb.getSaveTag().getWhite();
        whiteType = white.isPlayer() ? 0 : 1;
        whiteName = white.getDatabaseName();
        final var black = wcb.getSaveTag().getBlack();
        blackType = black.isPlayer() ? 0 : 1;
        blackName = black.getDatabaseName();
        moves = wcb.getGame().getMoveCount();
        pgn = wcb.getGame().toPgnString();
        result = wcb.getGame().getCurrentTurn().getState().name().toLowerCase();
        final var theWinner = wcb.getGame().getCurrentTurn().getWinner();
        if (theWinner == null) {
            winner = 0;
        } else if (theWinner == ChessColor.WHITE) {
            winner = 1;
        } else if (theWinner == ChessColor.BLACK) {
            winner = 2;
        }
    }
}
