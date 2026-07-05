package com.quantshoe.risk;

public final class DeviationMatrix {
    // Indices: [Player Total/Card][Dealer Upcard]
    private static final double[][] DEVIATION_THRESHOLDS = new double[22][12];
    private static final double[][] SOFT_DEVIATION_THRESHOLDS = new double[11][12];

    public static final double SURRENDER_16_VS_8_INDEX = 4.0;

    public static final double SURRENDER_16_VS_9_INDEX = -1.0;
    public static final double STAND_16_VS_9_INDEX = 4.0;

    public static final double STAND_16_VS_ACE_INDEX = 3.0;

    public static final double SURRENDER_15_VS_ACE_INDEX = -1.0;
    public static final double STAND_15_VS_ACE_INDEX = 5.0;

    public static final double SURRENDER_15_VS_9_INDEX = 2.0;

    public static final double STAND_15_VS_10_INDEX = 4.0;
    static {
        // Initialize everything to an unreachable default threshold
        for (int p = 0; p < 22; p++) {
            for (int d = 0; d < 12; d++) {
                if (p < 11) SOFT_DEVIATION_THRESHOLDS[p][d] = Double.NaN;
                DEVIATION_THRESHOLDS[p][d] = Double.NaN;
            }
        }

        // =============================================================
        // PLUG IN YOUR CHOSEN COUNT SYSTEM DEVIATION INDICES HERE
        // =============================================================

        // Example 1: The Insurance Index
        // Insurance is statistically viable at True +3.0 and above
        DEVIATION_THRESHOLDS[8][6] = 2.0;
        DEVIATION_THRESHOLDS[0][11] = 3.0; // Hand index 0 represents insurance rule map

        // Example 2: Illustrious 18 - Hard 16 vs Dealer 10
        // Stand if Running Count >= 0, otherwise Hit
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
        DEVIATION_THRESHOLDS[12][3] = 2.0;
        DEVIATION_THRESHOLDS[12][4] = 0;

        // Illustrious 18 - Hard 13 vs Dealer 2: Stand if TC >= -1
        DEVIATION_THRESHOLDS[13][2] = -1.0;
        // Illustrious 18 - Hard 13 vs Dealer 3: Stand if TC >= -2
        DEVIATION_THRESHOLDS[13][3] = -2.0;

        // Illustrious 18 - Hard 10 vs Dealer 10: Double if TC >= 4, Hard 10 vs Dealer Ace: Double if TC >= 3
        DEVIATION_THRESHOLDS[10][10] = 4.0;
        DEVIATION_THRESHOLDS[10][11] = 3.0;

        // Illustrious 18 - Hard 9 vs Dealer 2: Double if TC >= 1
        DEVIATION_THRESHOLDS[9][2] = 1.0;
        // Hard 9 vs Dealer 7: Double if TC >= 3
        DEVIATION_THRESHOLDS[9][7] = 3.0;

        // Soft deviations: Soft 17 (A+6) vs Dealer 2: Double if TC >= 1
        SOFT_DEVIATION_THRESHOLDS[6][2] = 1.0;
        // Soft 19 (A+8) vs Dealer 4: Double if TC >= 3
        SOFT_DEVIATION_THRESHOLDS[8][4] = 3.0;
        // Soft 19 (A+8) vs Dealer 5: Double if TC >= 1
        SOFT_DEVIATION_THRESHOLDS[8][5] = 1.0;
        // Soft 19 (A+8) vs Dealer 6: Double if TC >= 0
        SOFT_DEVIATION_THRESHOLDS[8][6] = 0.0;

    }

    public static double getHardThreshold(int playerTotal, int dealerUpcard) {
        return DEVIATION_THRESHOLDS[playerTotal][dealerUpcard];
    }

    public static double getSoftThreshold(int nonAceCardValue, int dealerUpcard) {
        return SOFT_DEVIATION_THRESHOLDS[nonAceCardValue][dealerUpcard];
    }

}