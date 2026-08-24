package com.quantshoe.risk;

public final class FixedSpreadBettingEngine implements BettingStrategy {
    private final double tableMin;
    private final double tableMax;
    private final int spreadRatio;
    private final int[] ramp;
    private final int[] handsPerLevel;
    private final int startTC;

    /** Default CVCX-matched ramp: 1-2-6-10-12 units at TC +1/+2/+3/+4/+5+. */
    private static final int[] DEFAULT_RAMP = {1, 2, 6, 10, 12};
    private static final int[] DEFAULT_HANDS = {1, 1, 1, 1, 1};
    private static final int DEFAULT_START_TC = 1;

    /**
     * Creates a fixed bet spread with the default 1-2-6-10-12 ramp (single hand at every level).
     */
    public FixedSpreadBettingEngine(double tableMin, double tableMax) {
        this(tableMin, tableMax, DEFAULT_RAMP, DEFAULT_HANDS, DEFAULT_START_TC);
    }

    /**
     * Creates a fixed bet spread with configurable multi-hand settings applied uniformly
     * at or above a threshold, using the default ramp.
     */
    public FixedSpreadBettingEngine(double tableMin, double tableMax, int multiHandThreshold, int multiHandCount) {
        this(tableMin, tableMax, multiHandThreshold, multiHandCount, DEFAULT_RAMP, DEFAULT_START_TC);
    }

    /**
     * Creates a fixed bet spread with a configurable ramp and uniform multi-hand settings.
     * Multi-hand count is applied at every TC level >= multiHandThreshold.
     */
    public FixedSpreadBettingEngine(double tableMin, double tableMax, int multiHandThreshold, int multiHandCount, int[] ramp) {
        this(tableMin, tableMax, multiHandThreshold, multiHandCount, ramp, DEFAULT_START_TC);
    }

    public FixedSpreadBettingEngine(double tableMin, double tableMax, int multiHandThreshold, int multiHandCount, int[] ramp, int startTC) {
        if (ramp == null || ramp.length == 0) {
            throw new IllegalArgumentException("Ramp must contain at least one entry.");
        }
        this.tableMin = tableMin;
        this.tableMax = tableMax;
        this.spreadRatio = (int) Math.round(tableMax / tableMin);
        this.ramp = ramp.clone();
        this.startTC = startTC;
        // Build per-level hands array from threshold/count
        this.handsPerLevel = new int[ramp.length];
        for (int i = 0; i < ramp.length; i++) {
            int tc = startTC + i;
            this.handsPerLevel[i] = (tc >= multiHandThreshold) ? multiHandCount : 1;
        }
    }

    /**
     * Creates a fixed bet spread with fully configurable per-level ramp and hand counts.
     *
     * @param tableMin      The minimum bet (1 unit).
     * @param tableMax      The maximum bet (top of the spread).
     * @param ramp          Unit multipliers for each TC level starting at TC +1.
     * @param handsPerLevel Number of hands to play at each TC level (same length as ramp).
     */
    public FixedSpreadBettingEngine(double tableMin, double tableMax, int[] ramp, int[] handsPerLevel) {
        this(tableMin, tableMax, ramp, handsPerLevel, DEFAULT_START_TC);
    }

    public FixedSpreadBettingEngine(double tableMin, double tableMax, int[] ramp, int[] handsPerLevel, int startTC) {
        if (ramp == null || ramp.length == 0) {
            throw new IllegalArgumentException("Ramp must contain at least one entry.");
        }
        if (handsPerLevel == null || handsPerLevel.length != ramp.length) {
            throw new IllegalArgumentException("handsPerLevel must have the same length as ramp.");
        }
        this.tableMin = tableMin;
        this.tableMax = tableMax;
        this.spreadRatio = (int) Math.round(tableMax / tableMin);
        this.ramp = ramp.clone();
        this.handsPerLevel = handsPerLevel.clone();
        this.startTC = startTC;
    }

    @Override
    public double calculateOptimalWager(double trueCount, double currentBankroll) {
        int index = (int) Math.floor(trueCount) - startTC;
        if (index < 0) return tableMin;
        if (index >= ramp.length) index = ramp.length - 1;
        return Math.min(tableMin * ramp[index], tableMax);
    }

    @Override
    public int getNumHands(double trueCount) {
        int index = (int) Math.floor(trueCount) - startTC;
        if (index < 0) return 1;
        if (index >= handsPerLevel.length) index = handsPerLevel.length - 1;
        return handsPerLevel[index];
    }

    public double getTableMin() { return tableMin; }
    public double getTableMax() { return tableMax; }
    public int getSpreadRatio() { return spreadRatio; }
}
