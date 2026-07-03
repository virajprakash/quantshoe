package com.quantshoe;
import com.quantshoe.pipeline.DataExporter;
import com.quantshoe.pipeline.SimulationResult;
import com.quantshoe.pipeline.SimulationWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Main {

    public static void main(String[] args) {
        // 1. Define Global Simulation Parameters
        int simulationRuns = 100;           // Number of parallel timeline tests (one per task)
        int handsPerRun = 100_000;         // Depth of each historical timeline test
        int totalDecks = 6;                // Standard casino shoe depth

        double startingBankroll = 10_000.0; // Your seed capital
        double tableMin = 10.0;             // Table minimum bet limits
        double tableMax = 1000.0;           // Table maximum bet limits

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
        printConsoleSummaryReport(globalResults, startingBankroll);
    }

    /**
     * Aggregates multi-threaded datasets into single performance metrics.
     */
    private static void printConsoleSummaryReport(List<SimulationResult> results, double initialCap) {
        double aggregatePnL = 0.0;
        double aggregateMaxDrawdown = 0.0;
        int activeRuns = results.size();
        int bankruptcyCount = 0;

        for (SimulationResult res : results) {
            aggregatePnL += res.getNetProfit();

            if (res.getMaxDrawdown() > aggregateMaxDrawdown) {
                aggregateMaxDrawdown = res.getMaxDrawdown();
            }
            // If the worker terminated because the bankroll fell below table minimums, flag it
            if (res.getFinalBankroll() <= 10.0) {
                bankruptcyCount++;
            }
        }

        double averagePnL = aggregatePnL / activeRuns;
        double riskOfRuin = ((double) bankruptcyCount / activeRuns) * 100.0;

        System.out.println("\n============== RISK AUDIT SYSTEM EXECUTIVE REPORT ==============");
        System.out.println("Total Simulated Timelines : " + activeRuns);
        System.out.println("Average Expected Net P&L  : $" + String.format("%.2f", averagePnL));
        System.out.println("Worst-Case Absolute MDD   : $" + String.format("%.2f", aggregateMaxDrawdown));
        System.out.println("Calculated Risk of Ruin   : " + String.format("%.2f", riskOfRuin) + "%");
        System.out.println("================================================================");
    }
}