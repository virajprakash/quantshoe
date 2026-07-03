package com.quantshoe.pipeline;

public final class SimulationResult {
    private final int totalHandsPlayed;
    private final double finalBankroll;
    private final double netProfit;
    private final double maxDrawdown;

    public SimulationResult(int totalHandsPlayed, double finalBankroll, double netProfit, double maxDrawdown) {
        this.totalHandsPlayed = totalHandsPlayed;
        this.finalBankroll = finalBankroll;
        this.netProfit = netProfit;
        this.maxDrawdown = maxDrawdown;
    }

    public int getTotalHandsPlayed() { return totalHandsPlayed; }
    public double getFinalBankroll() { return finalBankroll; }
    public double getNetProfit() { return netProfit; }
    public double getMaxDrawdown() { return maxDrawdown; }
}