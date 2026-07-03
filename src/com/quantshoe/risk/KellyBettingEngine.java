package com.quantshoe.risk;

public final class KellyBettingEngine {
    private final double fractionalKellyMultiplier;
    private final double baseHouseEdge;
    private final double edgePerTrueCount;

    /**
     * Default constructor for standard blackjack environments.
     * Uses a conservative "Quarter-Kelly" multiplier to mitigate drawdown variance.
     */
    public KellyBettingEngine() {
        this.fractionalKellyMultiplier = 0.25; // Quarter-Kelly (Standard practice for risk management)
        this.baseHouseEdge = -0.005;           // -0.5% starting edge at a normal table
        this.edgePerTrueCount = 0.005;         // Each True Count point adds +0.5% player equity
    }

    /**
     * Custom constructor to test different sizing constraints and house rule parameters.
     */
    public KellyBettingEngine(double fractionalKellyMultiplier, double baseHouseEdge, double edgePerTrueCount) {
        this.fractionalKellyMultiplier = fractionalKellyMultiplier;
        this.baseHouseEdge = baseHouseEdge;
        this.edgePerTrueCount = edgePerTrueCount;
    }

    /**
     * Calculates the exact dollar amount to wager based on the current mathematical advantage.
     *
     * @param trueCount The current true count extracted from the BlackjackShoe.
     * @param currentBankroll The player's active liquid capital.
     * @return The ideal dollar amount to bet (returns 0.0 if the player has no edge).
     */
    public double calculateOptimalWager(double trueCount, double currentBankroll) {
        // 1. Calculate our immediate player advantage (Edge)
        // Example: At True +3 -> -0.005 + (3 * 0.005) = +0.010 (+1% Edge)
        double playerEdge = baseHouseEdge + (trueCount * edgePerTrueCount);

        // If the edge is negative or neutral, bet 0 (or table minimum if forced to play)
        if (playerEdge <= 0) {
            return 0.0;
        }

        // 2. Standard Kelly formula for even-money payouts (b = 1): f = (Edge / Payout Ratio)
        // In even-money blackjack hands, b = 1, so the formula simplifies exactly to: f = playerEdge
        double fullKellyFraction = playerEdge;

        // 3. Apply the scaling factor (Fractional Kelly) to aggressively smooth the variance curve
        double targetedFraction = fullKellyFraction * fractionalKellyMultiplier;

        // 4. Convert the percentage fraction to a hard currency value
        double rawWager = currentBankroll * targetedFraction;

        // Round to the nearest unit currency (integer rounding) for clean casino execution
        return Math.floor(rawWager);
    }
}