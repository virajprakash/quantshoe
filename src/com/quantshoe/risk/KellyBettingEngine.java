package com.quantshoe.risk;

public final class KellyBettingEngine {
    private final double fractionalKellyMultiplier;
    private final double baseHouseEdge;
    private final double edgePerTrueCount;

    /**
     * Default constructor using Half-Kelly sizing with standard 6-deck blackjack assumptions.
     */
    public KellyBettingEngine() {
        this.fractionalKellyMultiplier = 0.5; // Half-Kelly to reduce variance
        this.baseHouseEdge = -0.005;           // -0.5% base house edge
        this.edgePerTrueCount = 0.005;         // +0.5% edge per true count point
    }

    /**
     * Custom constructor for different Kelly fractions or house rule assumptions.
     */
    public KellyBettingEngine(double fractionalKellyMultiplier, double baseHouseEdge, double edgePerTrueCount) {
        this.fractionalKellyMultiplier = fractionalKellyMultiplier;
        this.baseHouseEdge = baseHouseEdge;
        this.edgePerTrueCount = edgePerTrueCount;
    }

    /**
     * Calculates the optimal wager based on estimated edge and bankroll.
     *
     * @param trueCount The current true count.
     * @param currentBankroll The player's current bankroll.
     * @return The dollar amount to bet (0.0 if no edge).
     */
    public double calculateOptimalWager(double trueCount, double currentBankroll) {
        // 1. Estimate player edge from true count
        // e.g., TC +3 -> -0.005 + (3 * 0.005) = +1.0%
        double playerEdge = baseHouseEdge + (trueCount * edgePerTrueCount);

        // If the edge is negative or neutral, bet 0 (or table minimum if forced to play)
        if (playerEdge <= 0) {
            return 0.0;
        }

        // 2. Kelly fraction (simplified for ~even-money payouts): f = edge
        double fullKellyFraction = playerEdge;

        // 3. Apply fractional Kelly scaling
        double targetedFraction = fullKellyFraction * fractionalKellyMultiplier;

        // 4. Convert fraction to dollar amount
        double rawWager = currentBankroll * targetedFraction;

        // Round down to whole dollars
        return Math.floor(rawWager);
    }
}