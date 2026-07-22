package com.quantshoe.risk;

public final class FixedSpreadBettingEngine implements BettingStrategy {
    private final double tableMin;
    private final double tableMax;
    private final int spreadRatio;

    /**
     * Creates a fixed bet spread that ramps linearly from table min to table max.
     * At TC <= +1, bets table min (1 unit).
     * At each TC above +1, the bet increases by one unit until it hits the max.
     *
     * @param tableMin The minimum bet (1 unit).
     * @param tableMax The maximum bet (top of the spread).
     */
    public FixedSpreadBettingEngine(double tableMin, double tableMax) {
        this.tableMin = tableMin;
        this.tableMax = tableMax;
        this.spreadRatio = (int) Math.round(tableMax / tableMin);
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

    public double getTableMin() { return tableMin; }
    public double getTableMax() { return tableMax; }
    public int getSpreadRatio() { return spreadRatio; }
}
