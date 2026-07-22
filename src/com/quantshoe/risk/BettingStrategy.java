package com.quantshoe.risk;

public interface BettingStrategy {
    /**
     * Calculates the wager for the current hand.
     *
     * @param trueCount The current true count.
     * @param currentBankroll The player's current bankroll.
     * @return The dollar amount to bet (0.0 if no edge or sitting out).
     */
    double calculateOptimalWager(double trueCount, double currentBankroll);
}
