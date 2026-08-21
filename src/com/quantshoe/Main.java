package com.quantshoe;
import com.quantshoe.core.DeckEstimation;
import com.quantshoe.pipeline.DataExporter;
import com.quantshoe.pipeline.SimulationResult;
import com.quantshoe.pipeline.SimulationWorker;
import com.quantshoe.engine.GameEngine;
import com.quantshoe.engine.S17GameEngine;
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
        // 1. Simulation parameters (defaults, overridable via command-line args)
        int simulationRuns = 1000;
        int handsPerRun = 100_000;
        int totalDecks = 6;

        double startingBankroll = 25000;
        double tableMin = 25;
        double tableMax = 3000;
        double deckPenetration = 1.5;

        boolean lateSurrenderAllowed = true;
        boolean resplitAcesAllowed = false;

        String gameMode = "H17";
        String bettingMode = "fixed";
        String deckEstimationMode = "full";
        String rampStr = "1,2x1,6x1,10x1,12x1";
        String outputFilePath = "quant_blackjack_results.csv";

        // Parse command-line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--simRuns":
                    simulationRuns = Integer.parseInt(args[++i]);
                    break;
                case "--handsPerRun":
                    handsPerRun = Integer.parseInt(args[++i]);
                    break;
                case "--startingBankroll":
                    startingBankroll = Double.parseDouble(args[++i]);
                    break;
                case "--tableMin":
                    tableMin = Double.parseDouble(args[++i]);
                    break;
                case "--tableMax":
                    tableMax = Double.parseDouble(args[++i]);
                    break;
                case "--deckPen":
                    deckPenetration = Double.parseDouble(args[++i]);
                    break;
                case "--gameMode":
                    gameMode = args[++i];
                    break;
                case "--bettingMode":
                    bettingMode = args[++i];
                    break;
                case "--deckEstimation":
                    deckEstimationMode = args[++i];
                    break;
                case "--ramp":
                    rampStr = args[++i];
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    System.err.println("Usage: java com.quantshoe.Main [options]");
                    System.err.println("  --simRuns <int>            Number of simulation runs (default: 1000)");
                    System.err.println("  --handsPerRun <int>        Hands per run (default: 100000)");
                    System.err.println("  --startingBankroll <double> Starting bankroll (default: 10000)");
                    System.err.println("  --tableMin <double>        Table minimum bet (default: 25)");
                    System.err.println("  --tableMax <double>        Table maximum bet (default: 3000)");
                    System.err.println("  --deckPen <double>         Deck penetration (default: 1.5)");
                    System.err.println("  --gameMode <S17|H17>       Dealer soft-17 rule (default: S17)");
                    System.err.println("  --bettingMode <kelly|fixed> Bet sizing strategy (default: fixed)");
                    System.err.println("  --deckEstimation <full|half|quarter> Deck estimation granularity (default: full)");
                    System.err.println("  --ramp <units>             Comma-separated bet ramp in units (default: 1,2x2,6x2,10x2,12x2)");
                    System.exit(1);
            }
        }

        // 2. Set up thread pool
        int availableCores = Runtime.getRuntime().availableProcessors();
        System.out.println("System Hardware Detected: " + availableCores + " physical cores available.");
        System.out.println("Allocating Fixed Thread Pool...");

        ExecutorService threadPool = Executors.newFixedThreadPool(availableCores);
        List<SimulationWorker> tasks = new ArrayList<>();

        // 3. Create betting strategy
        // Parse ramp — supports optional xN hand-count suffix per level, e.g. "1,2x2,6x2,10x2,12x2"
        String[] rampParts = rampStr.split(",");
        int[] ramp = new int[rampParts.length];
        int[] handsPerLevel = new int[rampParts.length];
        for (int i = 0; i < rampParts.length; i++) {
            String part = rampParts[i].trim();
            if (part.contains("x")) {
                String[] tokens = part.split("x");
                ramp[i] = Integer.parseInt(tokens[0].trim());
                handsPerLevel[i] = Integer.parseInt(tokens[1].trim());
            } else {
                ramp[i] = Integer.parseInt(part);
                handsPerLevel[i] = 1;
            }
        }

        BettingStrategy bettingStrategy;
        if (bettingMode.equals("kelly")) {
            bettingStrategy = new KellyBettingEngine();
            System.out.println("Betting Mode: Half-Kelly Criterion");
        } else {
            bettingStrategy = new FixedSpreadBettingEngine(tableMin, tableMax, ramp, handsPerLevel);
            System.out.println("Betting Mode: Fixed Spread ($" + (int) tableMin + "-$" + (int) tableMax + "), Ramp: " + rampStr);
        }

        // 3b. Resolve deck estimation
        DeckEstimation deckEstimation;
        switch (deckEstimationMode.toLowerCase()) {
            case "half":
                deckEstimation = DeckEstimation.HALF;
                break;
            case "quarter":
                deckEstimation = DeckEstimation.QUARTER;
                break;
            default:
                deckEstimation = DeckEstimation.FULL;
                break;
        }

        System.out.println("Game Engine: " + (gameMode.equalsIgnoreCase("s17") ? "S17 (Stand on Soft 17)" : "H17 (Hit on Soft 17)"));
        System.out.println("Deck Estimation: " + deckEstimation.name().charAt(0) + deckEstimation.name().substring(1).toLowerCase() + " deck (rounding to nearest " + deckEstimation.getRoundingUnit() + " deck)");

        // 4. Create simulation tasks
        for (int i = 0; i < simulationRuns; i++) {
            GameEngine engine;
            if (gameMode.equalsIgnoreCase("s17")) {
                engine = new S17GameEngine(totalDecks, startingBankroll, tableMin, tableMax, lateSurrenderAllowed, resplitAcesAllowed, deckEstimation);
            } else {
                engine = new GameEngine(totalDecks, startingBankroll, tableMin, tableMax, lateSurrenderAllowed, resplitAcesAllowed, true, deckEstimation);
            }
            tasks.add(new SimulationWorker(handsPerRun, totalDecks, startingBankroll, tableMin, tableMax, deckPenetration, lateSurrenderAllowed, resplitAcesAllowed, bettingStrategy, engine));
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
                    String.format("%.0f", effectiveWager) + " x " + bettingStrategy.getNumHands(tc)));
        }
        System.out.println("======================================================================");
    }
}