package com.quantshoe.pipeline;

import com.quantshoe.core.Shoe;
import com.quantshoe.engine.GameEngine;
import com.quantshoe.risk.BettingStrategy;

import java.util.concurrent.Callable;

public final class SimulationWorker implements Callable<SimulationResult> {
    private final int handsToSimulate;
    private final int totalDecks;
    private final double startingBankroll;
    private final double tableMin;
    private final double tableMax;
    private final double deckPenetration;
    private boolean lateSurrenderAllowed = true;
    private boolean resplitAcesAllowed = false;
    private final GameEngine engine;
    private final BettingStrategy bettingEngine;

    public SimulationWorker(int handsToSimulate, int totalDecks, double startingBankroll, double tableMin, double tableMax, double deckPenetration, boolean lateSurrenderAllowed, boolean resplitAcesAllowed, BettingStrategy bettingStrategy) {
        this(handsToSimulate, totalDecks, startingBankroll, tableMin, tableMax, deckPenetration, lateSurrenderAllowed, resplitAcesAllowed, bettingStrategy,
                new GameEngine(totalDecks, startingBankroll, tableMin, tableMax, lateSurrenderAllowed, resplitAcesAllowed));
    }

    public SimulationWorker(int handsToSimulate, int totalDecks, double startingBankroll, double tableMin, double tableMax, double deckPenetration, boolean lateSurrenderAllowed, boolean resplitAcesAllowed, BettingStrategy bettingStrategy, GameEngine engine) {
        this.handsToSimulate = handsToSimulate;
        this.totalDecks = totalDecks;
        this.startingBankroll = startingBankroll;
        this.tableMin = tableMin;
        this.tableMax = tableMax;
        this.deckPenetration = deckPenetration;
        this.lateSurrenderAllowed = lateSurrenderAllowed;
        this.resplitAcesAllowed = resplitAcesAllowed;
        this.engine = engine;
        this.bettingEngine = bettingStrategy;
    }

    @Override
    public SimulationResult call() throws Exception {
        double peakBankroll = startingBankroll;
        double maxDrawdown = 0.0;
        int activeHands = 0;

        for (int i = 0; i < handsToSimulate; i++) {
            // 1. Get current state
            double currentBankroll = engine.getCurrentBankroll();
            Shoe activeShoe = engine.getShoe();

            // Stop if bankroll is too low to continue
            if (currentBankroll <= tableMin) {
                break;
            }

            // 2. Reshuffle at appropriate level of deck penetration (# of decks cut off)
            if (activeShoe.getCardsRemaining() < ((int) (52 * deckPenetration))) {
                activeShoe.shuffle();
            }

            // 3. Size the bet using Kelly criterion
            double trueCount = activeShoe.getTrueCount();
            double wager = bettingEngine.calculateOptimalWager(trueCount, currentBankroll);

            // Negative/neutral count = no edge, so bet the minimum
            if (wager < tableMin) {
                wager = tableMin;
            }

            // 4. Play the hand
            int numHands = bettingEngine.getNumHands(trueCount);
            engine.playRound(numHands, wager);
            activeHands += numHands;

            // 5. Track drawdown
            double updatedBankroll = engine.getCurrentBankroll();
            if (updatedBankroll > peakBankroll) {
                peakBankroll = updatedBankroll;
                // stop / win loss experimentation
//                if (updatedBankroll >= startingBankroll * 2.5) {
//                    break;
//                }
            }

            double currentDrawdown = peakBankroll - updatedBankroll;
            if (currentDrawdown > maxDrawdown) {
                maxDrawdown = currentDrawdown;
                //stop/win loss experimentation
//                if (currentDrawdown >= 10000) {
//                    break;
//                }
            }
        }

        // Return results
        double finalCap = engine.getCurrentBankroll();
        double netPnL = finalCap - startingBankroll;

        return new SimulationResult(activeHands, finalCap, netPnL, maxDrawdown);
    }
}