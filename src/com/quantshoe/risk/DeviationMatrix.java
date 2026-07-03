package com.quantshoe.risk;

public final class DeviationMatrix {
    // Indices: [Player Total/Card][Dealer Upcard]
    private static final double[][] DEVIATION_THRESHOLDS = new double[22][12];

    static {
        // Initialize everything to an unreachable default threshold
        for (int p = 0; p < 22; p++) {
            for (int d = 0; d < 12; d++) {
                DEVIATION_THRESHOLDS[p][d] = Double.NaN;
            }
        }

        // =============================================================
        // PLUG IN YOUR CHOSEN COUNT SYSTEM DEVIATION INDICES HERE
        // =============================================================

        // Example 1: The Insurance Index
        // Insurance is statistically viable at True +3.0 and above
        DEVIATION_THRESHOLDS[0][11] = 3.0; // Hand index 0 represents insurance rule map

        // Example 2: Illustrious 18 - Hard 16 vs Dealer 10
        // Stand if True Count >= 0, otherwise Hit
        DEVIATION_THRESHOLDS[16][10] = 0.0;

        // Example 3: Illustrious 18 - Hard 15 vs Dealer 10
        // Stand if True Count >= 4.0, otherwise Hit
        DEVIATION_THRESHOLDS[15][10] = 4.0;

        // Example 4: Illustrious 18 - Hard 10 vs Dealer Ace
        // Double down if True Count >= 4.0, otherwise Hit
        DEVIATION_THRESHOLDS[10][11] = 4.0;

        // Example 5: Illustrious 18 - Hard 12 vs Dealer 2
        // Stand if True Count >= 3.0, otherwise Hit
        DEVIATION_THRESHOLDS[12][2] = 3.0;
    }

    public static double getThreshold(int playerTotal, int dealerUpcard) {
        return DEVIATION_THRESHOLDS[playerTotal][dealerUpcard];
    }
}