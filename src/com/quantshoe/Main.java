package com.quantshoe;
import com.quantshoe.pipeline.DataExporter;
import com.quantshoe.pipeline.SimulationResult;
import com.quantshoe.pipeline.SimulationWorker;
import com.quantshoe.risk.KellyBettingEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Main {

    public static void main(String[] args) {
        // 1. Define Global Simulation Parameters
        int simulationRuns = 1000;           // Number of parallel timeline tests (one per task)
        int handsPerRun = 100_000;         // Depth of each historical timeline test
        int totalDecks = 6;                // Standard casino shoe depth

        double startingBankroll = 100000; // Your seed capital
        double tableMin = 100;             // Table minimum bet limits
        double tableMax = 10000.0;           // Table maximum bet limits

        String outputFilePath = "quant_blackjack_results.csv";

        // 2. Dynamically Scale Resources to the Host Hardware
        int availableCores = Runtime.getRuntime().availableProcessors();
        System.out.println("System Hardware Detected: " + availableCores + " physical cores available.");
        System.out.println("Allocating Fixed Thread Pool...");

        ExecutorService threadPool = Executors.newFixedThreadPool(availableCores);
        List<SimulationWorker> tasks = new ArrayList<>();

        // 3. Queue Up the Parallel Timelines
        for (int i = 0; i < simulationRuns; i++) {
            tasks.add(new SimulationWorker(handsPerRun, totalDecks, startingBankroll, tableMin, tableMax));
        }

        System.out.println("Executing " + simulationRuns + " parallel tests ("
                + (simulationRuns * handsPerRun) + " cumulative rounds)...");

        List<SimulationResult> globalResults = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try {
            // 4. Fire the threads simultaneously across all CPU cores
            List<Future<SimulationResult>> futures = threadPool.invokeAll(tasks);

            // 5. Gather and block-collate the results as they finish processing
            for (Future<SimulationResult> future : futures) {
                globalResults.add(future.get()); // Blocks safely until individual thread concludes
            }

        } catch (Exception e) {
            System.err.println("[Thread Pool Failure] Execution crash: " + e.getMessage());
        } finally {
            // Always shut down the thread pool to release OS hardware resources
            threadPool.shutdown();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Computation complete! Processing execution time: " + (endTime - startTime) + "ms");

        // 6. Push Datasets to External Spreadsheet File Engine
        DataExporter.exportToCSV(outputFilePath, globalResults);

        // 7. Generate Real-Time High-Level Analytics Console Report
        printConsoleSummaryReport(globalResults, startingBankroll, tableMin, tableMax, handsPerRun);
    }

    /**
     * Aggregates multi-threaded datasets into single performance metrics.
     */
    private static void printConsoleSummaryReport(List<SimulationResult> results, double initialCap, double tableMin, double tableMax, int handsPerRun) {
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

        System.out.println("\n============== RISK AUDIT SYSTEM EXECUTIVE REPORT ==============");
        System.out.println("Total Simulated Timelines : " + activeRuns);
        System.out.println("Average Expected Net P&L  : $" + String.format("%.2f", averagePnL));
        System.out.println("Worst-Case Absolute MDD   : $" + String.format("%.2f", aggregateMaxDrawdown));
        System.out.println("Calculated Risk of Ruin   : " + String.format("%.2f", riskOfRuin) + "%");
        System.out.println("================================================================");

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
        KellyBettingEngine kellyEngine = new KellyBettingEngine();
        System.out.println("\n=================== BET SPREAD (Starting Bankroll) ===================");
        System.out.println(String.format("%-18s %-18s", "True Count", "Wager"));
        System.out.println("--------------------------------------");
        for (int tc = -5; tc <= 10; tc++) {
            double rawWager = kellyEngine.calculateOptimalWager(tc, initialCap);
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