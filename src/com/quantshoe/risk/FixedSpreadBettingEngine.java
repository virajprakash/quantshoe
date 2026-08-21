package com.quantshoe.risk;

public final class FixedSpreadBettingEngine implements BettingStrategy {
    private final double tableMin;
    private final double tableMax;
    private final int spreadRatio;
    private final int multiHandThreshold;
    private final int multiHandCount;
    private final int[] ramp;

    /** Default CVCX-matched ramp: 1-2-6-10-12 units at TC +1/+2/+3/+4/+5+. */
    private static final int[] DEFAULT_RAMP = {1, 2, 6, 10, 12};

    /**
     * Creates a fixed bet spread with the default 1-2-6-10-12 ramp.
     *
     * @param tableMin The minimum bet (1 unit).
     * @param tableMax The maximum bet (top of the spread).
     */
    public FixedSpreadBettingEngine(double tableMin, double tableMax) {
        this(tableMin, tableMax, 2, 2, DEFAULT_RAMP);
    }

    /**
     * Creates a fixed bet spread with configurable multi-hand settings and the default ramp.
     *
     * @param tableMin          The minimum bet (1 unit).
     * @param tableMax          The maximum bet (top of the spread).
     * @param multiHandThreshold The true count at or above which multiple hands are played.
     * @param multiHandCount     The number of hands to play when the threshold is met.
     */
    public FixedSpreadBettingEngine(double tableMin, double tableMax, int multiHandThreshold, int multiHandCount) {
        this(tableMin, tableMax, multiHandThreshold, multiHandCount, DEFAULT_RAMP);
    }

    /**
     * Creates a fixed bet spread with fully configurable ramp and multi-hand settings.
     *
     * @param tableMin          The minimum bet (1 unit).
     * @param tableMax          The maximum bet (top of the spread).
     * @param multiHandThreshold The true count at or above which multiple hands are played.
     * @param multiHandCount     The number of hands to play when the threshold is met.
     * @param ramp              Unit multipliers for each TC level starting at TC +1.
     *                          E.g. {1, 2, 6, 10, 12} means TC+1=1u, TC+2=2u, TC+3=6u, TC+4=10u, TC+5+=12u.
     */
    public FixedSpreadBettingEngine(double tableMin, double tableMax, int multiHandThreshold, int multiHandCount, int[] ramp) {
        if (ramp == null || ramp.length == 0) {
            throw new IllegalArgumentException("Ramp must contain at least one entry.");
        }
        this.tableMin = tableMin;
        this.tableMax = tableMax;
        this.spreadRatio = (int) Math.round(tableMax / tableMin);
        this.multiHandThreshold = multiHandThreshold;
        this.multiHandCount = multiHandCount;
        this.ramp = ramp.clone();
    }

    @Override
    public double calculateOptimalWager(double trueCount, double currentBankroll) {
        // TC <= +1 always bets 1 unit (ramp[0])
        if (trueCount < 2) return Math.min(tableMin * ramp[0], tableMax);
        // Walk the ramp for TC +2, +3, ... up to the last entry
        int index = (int) trueCount - 1; // TC +2 → index 1, TC +3 → index 2, etc.
        if (index >= ramp.length) index = ramp.length - 1;
        return Math.min(tableMin * ramp[index], tableMax);
    }

    @Override
    public int getNumHands(double trueCount) {
        return trueCount >= multiHandThreshold ? multiHandCount : 1;
    }

    public double getTableMin() { return tableMin; }
    public double getTableMax() { return tableMax; }
    public int getSpreadRatio() { return spreadRatio; }
}
