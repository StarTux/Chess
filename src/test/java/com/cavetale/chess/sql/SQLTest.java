package com.cavetale.chess.sql;

import org.junit.Test;
import com.winthier.sql.SQLDatabase;

public final class SQLTest {
    @Test
    public void test() {
        System.out.println(SQLDatabase.testTableCreation(SQLChessGame.class));
    }
}
