package com.quantshoe.pipeline;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public final class DataExporter {

    /**
     * Iterates through all thread outputs and flushes them into a clean CSV file.
     *
     * @param filePath The destination file pathway on your hard drive (e.g., "results.csv").
     * @param results A list containing the compiled results from all executed threads.
     */
    public static void exportToCSV(String filePath, List<SimulationResult> results) {
        if (results == null || results.isEmpty()) {
            System.err.println("[Exporter Error] No data fields available to extract.");
            return;
        }

        System.out.println("Processing data export pipelines to: " + filePath);

        // Try-with-resources blocks automatically close file handles, preventing memory leaks
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

            // 1. Write the column headers for spreadsheet engines to parse
            writer.write("Thread_ID,Total_Hands_Played,Final_Bankroll,Net_Profit,Max_Drawdown");
            writer.newLine();

            // 2. Stream through every result packet and format the rows
            int threadId = 1;
            for (SimulationResult result : results) {
                StringBuilder row = new StringBuilder();

                row.append(threadId++).append(",")
                        .append(result.getTotalHandsPlayed()).append(",")
                        .append(String.format("%.2f", result.getFinalBankroll())).append(",")
                        .append(String.format("%.2f", result.getNetProfit())).append(",")
                        .append(String.format("%.2f", result.getMaxDrawdown()));

                writer.write(row.toString());
                writer.newLine();
            }

            System.out.println("Data extraction complete! File finalized successfully.");

        } catch (IOException e) {
            System.err.println("[Exporter Failure] Critical error writing to filesystem: " + e.getMessage());
        }
    }
}