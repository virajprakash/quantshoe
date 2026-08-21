package com.quantshoe.risk;

public final class FixedSpreadBettingEngine implements BettingStrategy {
    private final double tableMin;
    private final double tableMax;
    private final int spreadRatio;
    private final int multiHandThreshold;
    private final int multiHandCount;

    /**
     * Creates a fixed bet spread that ramps linearly from table min to table max.
     * At TC <= +1, bets table min (1 unit).
     * At each TC above +1, the bet increases by one unit until it hits the max.
     *
     * @param tableMin The minimum bet (1 unit).
     * @param tableMax The maximum bet (top of the spread).
     */
    public FixedSpreadBettingEngine(double tableMin, double tableMax) {
        this(tableMin, tableMax, 2, 2);
    }

    /**
     * Creates a fixed bet spread with configurable multi-hand settings.
     *
     * @param tableMin          The minimum bet (1 unit).
     * @param tableMax          The maximum bet (top of the spread).
     * @param multiHandThreshold The true count at or above which multiple hands are played.
     * @param multiHandCount     The number of hands to play when the threshold is met.
     */
    public FixedSpreadBettingEngine(double tableMin, double tableMax, int multiHandThreshold, int multiHandCount) {
        this.tableMin = tableMin;
        this.tableMax = tableMax;
        this.spreadRatio = (int) Math.round(tableMax / tableMin);
        this.multiHandThreshold = multiHandThreshold;
        this.multiHandCount = multiHandCount;
    }

    @Override
    public double calculateOptimalWager(double trueCount, double currentBankroll) {
        // CVCX-matched ramp (1-12 spread)
        if (trueCount < 2) return tableMin;                          // TC <= +1: 1 unit
        if (trueCount < 3) return Math.min(tableMin * 2, tableMax);  // TC +2:    2 units
        if (trueCount < 4) return Math.min(tableMin * 6, tableMax);  // TC +3:    6 units
        if (trueCount < 5) return Math.min(tableMin * 10, tableMax); // TC +4:    10 units
        return Math.min(tableMin * 12, tableMax);                                              // TC +5+:   12 units
    }

    @Override
    public int getNumHands(double trueCount) {
        return trueCount >= multiHandThreshold ? multiHandCount : 1;
    }

    public double getTableMin() { return tableMin; }
    public double getTableMax() { return tableMax; }
    public int getSpreadRatio() { return spreadRatio; }
}
