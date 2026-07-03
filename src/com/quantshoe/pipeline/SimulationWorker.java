package com.quantshoe.pipeline;

import com.quantshoe.core.Shoe;
import com.quantshoe.engine.GameEngine;
import com.quantshoe.risk.KellyBettingEngine;

import java.util.concurrent.Callable;

public final class SimulationWorker implements Callable<SimulationResult> {
    private final int handsToSimulate;
    private final int totalDecks;
    private final double startingBankroll;
    private final double tableMin;
    private final double tableMax;

    private final GameEngine engine;
    private final KellyBettingEngine bettingEngine;

    public SimulationWorker(int handsToSimulate, int totalDecks, double startingBankroll, double tableMin, double tableMax) {
        this.handsToSimulate = handsToSimulate;
        this.totalDecks = totalDecks;
        this.startingBankroll = startingBankroll;
        this.tableMin = tableMin;
        this.tableMax = tableMax;

        // Instantiating these inside the thread constructor ensures each thread owns its distinct data structures
        this.engine = new GameEngine(totalDecks, startingBankroll, tableMin, tableMax);
        this.bettingEngine = new KellyBettingEngine();
    }

    @Override
    public SimulationResult call() throws Exception {
        double peakBankroll = startingBankroll;
        double maxDrawdown = 0.0;
        int activeHands = 0;

        for (int i = 0; i < handsToSimulate; i++) {
            // 1. Fetch current game state variables
            double currentBankroll = engine.getCurrentBankroll();
            Shoe activeShoe = engine.getShoe();

            // Risk management safety check: terminate early if we experience ruin (bust)
            if (currentBankroll <= tableMin) {
                break;
            }

            // 2. Structural casino rule check: Shuffle if we hit the deck depth limit (e.g., 75% penetration)
            if (activeShoe.getCardsRemaining() < (totalDecks * 52 * 0.25)) {
                activeShoe.shuffle();
            }

            // 3. Query the risk engine to size our wager based on current True Count
            double trueCount = activeShoe.getTrueCount();
            double wager = bettingEngine.calculateOptimalWager(trueCount, currentBankroll);

            // If count is negative/neutral, Kelly tells us to bet $0. We place table minimum to keep playing.
            if (wager < tableMin) {
                wager = tableMin;
            }

            // 4. Execute the mathematical gameplay logic
            engine.playRound(wager);
            activeHands++;

            // 5. Variance Tracking: Calculate Drawdown Metrics
            double updatedBankroll = engine.getCurrentBankroll();
            if (updatedBankroll > peakBankroll) {
                peakBankroll = updatedBankroll;
            }

            double currentDrawdown = peakBankroll - updatedBankroll;
            if (currentDrawdown > maxDrawdown) {
                maxDrawdown = currentDrawdown;
            }
        }

        // Package up the final metrics to return to the Main orchestrator thread
        double finalCap = engine.getCurrentBankroll();
        double netPnL = finalCap - startingBankroll;

        return new SimulationResult(activeHands, finalCap, netPnL, maxDrawdown);
    }
}