package com.quantshoe;
import com.quantshoe.pipeline.DataExporter;
import com.quantshoe.pipeline.SimulationResult;
import com.quantshoe.pipeline.SimulationWorker;
import com.quantshoe.risk.BettingStrategy;
import com.quantshoe.risk.FixedSpreadBettingEngine;
import com.quantshoe.risk.KellyBettingEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Main {

    public static void main(String[] args) {
        // 1. Simulation parameters
        int simulationRuns = 1000;           // Number of simulation runs
        int handsPerRun = 100_000;         // Hands per run
        int totalDecks = 6;                // Decks in the shoe

        double startingBankroll = 25000; // Starting bankroll
        double tableMin = 25;             // Table minimum bet
        double tableMax = 3000;           // Table maximum bet (1-12 spread: $15 x 12)
        double deckPenetration = 1.5;

        boolean lateSurrenderAllowed = true;
        boolean resplitAcesAllowed = false;


        // Betting mode: "kelly" for Half-Kelly sizing, "fixed" for fixed spread
        String bettingMode = "fixed";

        String outputFilePath = "quant_blackjack_results.csv";

        // 2. Set up thread pool
        int availableCores = Runtime.getRuntime().availableProcessors();
        System.out.println("System Hardware Detected: " + availableCores + " physical cores available.");
        System.out.println("Allocating Fixed Thread Pool...");

        ExecutorService threadPool = Executors.newFixedThreadPool(availableCores);
        List<SimulationWorker> tasks = new ArrayList<>();

        // 3. Create betting strategy
        BettingStrategy bettingStrategy;
        if (bettingMode.equals("kelly")) {
            bettingStrategy = new KellyBettingEngine();
            System.out.println("Betting Mode: Half-Kelly Criterion");
        } else {
            bettingStrategy = new FixedSpreadBettingEngine(tableMin, tableMax);
            System.out.println("Betting Mode: Fixed Spread ($" + (int) tableMin + "-$" + (int) tableMax + ")");
        }

        // 4. Create simulation tasks
        for (int i = 0; i < simulationRuns; i++) {
            tasks.add(new SimulationWorker(handsPerRun, totalDecks, startingBankroll, tableMin, tableMax, deckPenetration, lateSurrenderAllowed, resplitAcesAllowed, bettingStrategy));
        }

        System.out.println("Executing " + simulationRuns + " parallel tests ("
                + (simulationRuns * handsPerRun) + " cumulative rounds)...");

        List<SimulationResult> globalResults = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try {
            // 5. Run all simulations in parallel
            List<Future<SimulationResult>> futures = threadPool.invokeAll(tasks);

            // 6. Collect results
            for (Future<SimulationResult> future : futures) {
                globalResults.add(future.get());
            }

        } catch (Exception e) {
            System.err.println("Simulation failed: " + e.getMessage());
        } finally {
            // Shut down the thread pool
            threadPool.shutdown();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Computation complete! Processing execution time: " + (endTime - startTime) + "ms");

        // 7. Export results to CSV
        DataExporter.exportToCSV(outputFilePath, globalResults);

        // 8. Print summary report
        printConsoleSummaryReport(globalResults, startingBankroll, tableMin, tableMax, handsPerRun, bettingStrategy);
    }

    /**
     * Aggregates multi-threaded datasets into single performance metrics.
     */
    private static void printConsoleSummaryReport(List<SimulationResult> results, double initialCap, double tableMin, double tableMax, int handsPerRun, BettingStrategy bettingStrategy) {
        double aggregatePnL = 0.0;
        double aggregateMaxDrawdown = 0.0;
        int activeRuns = results.size();
        int bankruptcyCount = 0;
        long totalHandsPlayed = 0;

        for (SimulationResult res : results) {
            aggregatePnL += res.getNetProfit();
            totalHandsPlayed += res.getTotalHandsPlayed();

            if (res.getMaxDrawdown() > aggregateMaxDrawdown) {
                aggregateMaxDrawdown = res.getMaxDrawdown();
            }
            // If the worker terminated because the bankroll fell below table minimums, flag it
            if (res.getFinalBankroll() <= tableMin) {
                bankruptcyCount++;
            }
        }

        double averagePnL = aggregatePnL / activeRuns;
        double riskOfRuin = ((double) bankruptcyCount / activeRuns) * 100.0;

        // Calculate average EV per hand across all runs
        double averageHandsPlayed = (double) totalHandsPlayed / activeRuns;
        double evPerHand = aggregatePnL / totalHandsPlayed;

        System.out.println("\n============== SIMULATION SUMMARY ==============");
        System.out.println("Total Runs              : " + activeRuns);
        System.out.println("Average Net P&L         : $" + String.format("%.2f", averagePnL));
        System.out.println("Worst-Case Max Drawdown : $" + String.format("%.2f", aggregateMaxDrawdown));
        System.out.println("Risk of Ruin            : " + String.format("%.2f", riskOfRuin) + "%");
        System.out.println("=================================================");

        // Expected Value Summary by Rounds Per Hour
        int[] roundsPerHourOptions = {50, 60, 70, 80, 100, 120, 150, 200};

        System.out.println("\n========== EXPECTED VALUE BY ROUNDS PER HOUR ==========");
        System.out.println(String.format("%-20s %-18s %-18s %-18s %-18s", "Rounds/Hour", "EV/Hour", "EV/8hr Session", "Hours/Thread", "8hr Sessions/Thread"));
        System.out.println("-----------------------------------------------------------------------------------------------");
        for (int rph : roundsPerHourOptions) {
            double evPerHour = evPerHand * rph;
            double evPerSession = evPerHour * 8.0;
            double hoursPerThread = averageHandsPlayed / rph;
            double sessionsPerThread = hoursPerThread / 8.0;
            System.out.println(String.format("%-20d $%-17s $%-17s %-18s %-18s",
                    rph,
                    String.format("%.2f", evPerHour),
                    String.format("%.2f", evPerSession),
                    String.format("%.1f", hoursPerThread),
                    String.format("%.1f", sessionsPerThread)));
        }
        System.out.println("===============================================================================================");

        // Bet Spread Summary
        System.out.println("\n=================== BET SPREAD (Starting Bankroll) ===================");
        System.out.println(String.format("%-18s %-18s", "True Count", "Wager"));
        System.out.println("--------------------------------------");
        for (int tc = -5; tc <= 10; tc++) {
            double rawWager = bettingStrategy.calculateOptimalWager(tc, initialCap);
            double effectiveWager = Math.max(tableMin, Math.min(rawWager, tableMax));
            if (rawWager < tableMin) {
                effectiveWager = tableMin;
            }
            System.out.println(String.format("%-18s $%-17s",
                    (tc >= 0 ? "+" : "") + tc,
                    String.format("%.0f", effectiveWager)));
        }
        System.out.println("======================================================================");
    }
}