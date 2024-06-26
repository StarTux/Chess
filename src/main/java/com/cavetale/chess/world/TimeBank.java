package com.cavetale.chess.world;

import com.cavetale.mytems.Mytems;
import lombok.Getter;

@Getter
public enum TimeBank {
    TEN(Mytems.COLORFALL_HOURGLASS, 10, 5),
    FIFTEEN(Mytems.MOONLIGHT_HOURGLASS, 15, 10),
    THIRTY(Mytems.ATMOSPHERE_HOURGLASS, 30, 15);

    private final Mytems mytems;
    private final int timeBankMinutes;
    private final int incrementSeconds;
    private final long timeBank;
    private final long increment;

    TimeBank(final Mytems mytems, final int timeBankMinutes, final int incrementSeconds) {
        this.mytems = mytems;
        this.timeBankMinutes = timeBankMinutes;
        this.incrementSeconds = incrementSeconds;
        this.timeBank = (long) timeBankMinutes * 1000L * 60L;
        this.increment = (long) incrementSeconds * 1000L;
    }

    @Override
    public String toString() {
        return incrementSeconds == 0
            ? "" + timeBankMinutes
            : "" + timeBankMinutes + "|" + incrementSeconds;
    }
}
