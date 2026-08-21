package com.quantshoe.engine;

import com.quantshoe.strategy.StrategyAction;

/**
 * S17 (Stand on Soft 17) game engine.
 * The dealer stands on all 17s, including soft 17.
 * This engine uses its own independent decision matrices, deviation thresholds,
 * and split matrices that are completely separate from the H17 GameEngine.
 *
 * To configure S17 strategy: edit the instance initializer block below.
 */
public final class S17GameEngine extends GameEngine {

    // S17 instance initializer — overwrites all H17 matrices with S17-specific data
    {
        // =============================================
        // RESET ALL MATRICES TO DEFAULTS
        // =============================================
        for (int p = 0; p < 22; p++) {
            for (int d = 0; d < 12; d++) {
                HARD_MATRIX[p][d] = StrategyAction.HIT;
                if (p < 11) SOFT_MATRIX[p][d] = StrategyAction.HIT;
                if (p < 12) SPLIT_MATRIX[p][d] = false;
                DEVIATION_THRESHOLDS[p][d] = Double.NaN;
                if (p < 11) SOFT_DEVIATION_THRESHOLDS[p][d] = Double.NaN;
            }
        }

        // =============================================
        // S17 HARD TOTALS MATRIX
        // =============================================

        // Hard 17-21: Always STAND
        for (int p = 17; p <= 21; p++) {
            for (int d = 2; d <= 11; d++) {
                HARD_MATRIX[p][d] = StrategyAction.STAND;
            }
        }

        // Hard 13-16: STAND against dealer 2-6
        for (int p = 13; p <= 16; p++) {
            for (int d = 2; d <= 6; d++) {
                HARD_MATRIX[p][d] = StrategyAction.STAND;
            }
        }

        // Hard 12: STAND only vs. 4, 5, 6
        for (int d = 4; d <= 6; d++) {
            HARD_MATRIX[12][d] = StrategyAction.STAND;
        }

        // Hard 11: DOUBLE against everything (S17: includes vs Ace)
        for (int d = 2; d <= 10; d++) {
            HARD_MATRIX[11][d] = StrategyAction.DOUBLE_OR_HIT;
        }
        HARD_MATRIX[11][11] = StrategyAction.DEVIATE_DOUBLE_IF_ABOVE;

        // Hard 10: DOUBLE against 2-9
        for (int d = 2; d <= 9; d++) {
            HARD_MATRIX[10][d] = StrategyAction.DOUBLE_OR_HIT;
        }

        // Hard 9: DOUBLE against 3-6
        for (int d = 3; d <= 6; d++) {
            HARD_MATRIX[9][d] = StrategyAction.DOUBLE_OR_HIT;
        }

        // =============================================
        // S17 HARD DEVIATIONS (Illustrious 18 for S17)
        // =============================================
        // TODO: Replace these with your S17-specific deviation indices
        HARD_MATRIX[15][10] = StrategyAction.DEVIATE_STAND_IF_ABOVE;
        HARD_MATRIX[16][10] = StrategyAction.SURRENDER_16_VS_10_RUNNING_COUNT;
        HARD_MATRIX[12][2] = StrategyAction.DEVIATE_STAND_IF_ABOVE;
        HARD_MATRIX[12][3] = StrategyAction.DEVIATE_STAND_IF_ABOVE;
        HARD_MATRIX[12][4] = StrategyAction.DEVIATE_STAND_IF_RUNNING_ABOVE_ZERO;

        HARD_MATRIX[10][10] = StrategyAction.DEVIATE_DOUBLE_IF_ABOVE;
        HARD_MATRIX[10][11] = StrategyAction.DEVIATE_DOUBLE_IF_ABOVE;
        HARD_MATRIX[9][2] = StrategyAction.DEVIATE_DOUBLE_IF_ABOVE;
        HARD_MATRIX[9][7] = StrategyAction.DEVIATE_DOUBLE_IF_ABOVE;
        HARD_MATRIX[8][6] = StrategyAction.DEVIATE_DOUBLE_IF_ABOVE;

        // S17 Surrender entries
        // TODO: Replace with S17-specific surrender rules
        HARD_MATRIX[16][8]  = StrategyAction.SURRENDER_16_VS_8_ABOVE_4;
        HARD_MATRIX[16][9]  = StrategyAction.SURRENDER_16_VS_9_ABOVE_MINUS_1_ELSE_STAND_ABOVE_4;
        HARD_MATRIX[16][10] = StrategyAction.SURRENDER_16_VS_10_RUNNING_COUNT;
        HARD_MATRIX[16][11] = StrategyAction.SURRENDER_16_VS_ACE_ELSE_STAND_ABOVE_3;

        HARD_MATRIX[15][9]  = StrategyAction.SURRENDER_15_VS_9_ABOVE_2_ELSE_HIT;
        HARD_MATRIX[15][10] = StrategyAction.SURRENDER_15_VS_10_WHEN_RUNNING_NON_NEGATIVE_ELSE_STAND_ABOVE_4;
        HARD_MATRIX[15][11] = StrategyAction.SURRENDER_15_VS_ACE_ABOVE_MINUS_1_ELSE_STAND_ABOVE_5;

        // S17: Do NOT surrender hard 17 vs Ace (H17-only rule)
        // HARD_MATRIX[17][11] stays as STAND from the loop above

        // =============================================
        // S17 SOFT TOTALS MATRIX
        // =============================================

        SOFT_MATRIX[2][5] = StrategyAction.DOUBLE_OR_HIT;
        SOFT_MATRIX[2][6] = StrategyAction.DOUBLE_OR_HIT;

        SOFT_MATRIX[3][5] = StrategyAction.DOUBLE_OR_HIT;
        SOFT_MATRIX[3][6] = StrategyAction.DOUBLE_OR_HIT;

        SOFT_MATRIX[4][4] = StrategyAction.DOUBLE_OR_HIT;
        SOFT_MATRIX[4][5] = StrategyAction.DOUBLE_OR_HIT;
        SOFT_MATRIX[4][6] = StrategyAction.DOUBLE_OR_HIT;

        SOFT_MATRIX[5][4] = StrategyAction.DOUBLE_OR_HIT;
        SOFT_MATRIX[5][5] = StrategyAction.DOUBLE_OR_HIT;
        SOFT_MATRIX[5][6] = StrategyAction.DOUBLE_OR_HIT;

        SOFT_MATRIX[6][2] = StrategyAction.DEVIATE_DOUBLE_IF_ABOVE;
        SOFT_MATRIX[6][3] = StrategyAction.DOUBLE_OR_HIT;
        SOFT_MATRIX[6][4] = StrategyAction.DOUBLE_OR_HIT;
        SOFT_MATRIX[6][5] = StrategyAction.DOUBLE_OR_HIT;
        SOFT_MATRIX[6][6] = StrategyAction.DOUBLE_OR_HIT;

        // Soft 18 (A+7)
        SOFT_MATRIX[7][7] = StrategyAction.STAND;
        SOFT_MATRIX[7][8] = StrategyAction.STAND;
        for (int d = 2; d <= 6; d++) SOFT_MATRIX[7][d] = StrategyAction.DOUBLE_OR_STAND;
        for (int d = 9; d <= 11; d++) SOFT_MATRIX[7][d] = StrategyAction.HIT;

        // Soft 19-21: STAND
        for (int p = 8; p <= 10; p++) {
            for (int d = 2; d <= 11; d++) {
                SOFT_MATRIX[p][d] = StrategyAction.STAND;
            }
        }

        // S17 soft deviations
        // TODO: Replace with S17-specific soft deviation actions
        SOFT_MATRIX[8][4] = StrategyAction.DEVIATE_DOUBLE_IF_STAND;
        SOFT_MATRIX[8][5] = StrategyAction.DEVIATE_DOUBLE_IF_STAND;
        SOFT_MATRIX[8][6] = StrategyAction.DEVIATE_DOUBLE_IF_STAND;

        // =============================================
        // S17 SPLIT MATRIX
        // =============================================

        // Always split Aces and 8s
        for (int d = 2; d <= 11; d++) {
            SPLIT_MATRIX[11][d] = true;
            SPLIT_MATRIX[8][d] = true;
        }

        // Split 9s against 2-9 except 7
        for (int d = 2; d <= 9; d++) {
            if (d != 7) SPLIT_MATRIX[9][d] = true;
        }

        // Split 7s and 3s and 2s against 2-7
        for (int d = 2; d <= 7; d++) {
            SPLIT_MATRIX[7][d] = true;
            SPLIT_MATRIX[3][d] = true;
            SPLIT_MATRIX[2][d] = true;
        }

        // Split 6s against 2-6
        for (int d = 2; d <= 6; d++) {
            SPLIT_MATRIX[6][d] = true;
        }

        // Split 4s against 5 and 6
        SPLIT_MATRIX[4][5] = true;
        SPLIT_MATRIX[4][6] = true;

        // =============================================
        // S17 DEVIATION THRESHOLDS
        // =============================================
        // TODO: Replace with S17-specific deviation threshold indices
        DEVIATION_THRESHOLDS[8][6] = 2.0;
        // DEVIATION_THRESHOLDS[0][11] = 3.0; Insurance
        DEVIATION_THRESHOLDS[16][9] = 4.0;
        DEVIATION_THRESHOLDS[16][10] = 0.0;
        DEVIATION_THRESHOLDS[15][10] = 4.0;
        DEVIATION_THRESHOLDS[12][2] = 3.0;
        DEVIATION_THRESHOLDS[12][3] = 2.0;
        DEVIATION_THRESHOLDS[12][4] = 0;
        DEVIATION_THRESHOLDS[13][2] = -1.0;
        DEVIATION_THRESHOLDS[10][10] = 4.0;
        DEVIATION_THRESHOLDS[10][11] = 4.0;
        DEVIATION_THRESHOLDS[9][2] = 1.0;
        DEVIATION_THRESHOLDS[9][7] = 3.0;

        SOFT_DEVIATION_THRESHOLDS[6][2] = 1.0;
        SOFT_DEVIATION_THRESHOLDS[8][4] = 3.0;
        SOFT_DEVIATION_THRESHOLDS[8][5] = 1.0;
        SOFT_DEVIATION_THRESHOLDS[8][6] = 1.0;

        // S17 SPLIT DEVIATION THRESHOLDS
        for (int p = 0; p < 12; p++) {
            for (int d = 0; d < 12; d++) {
                SPLIT_DEVIATION_THRESHOLDS[p][d] = Double.NaN;
            }
        }

        // Split 10s vs dealer 4 at TC >= 6, vs dealer 5 at TC >= 5, vs dealer 6 at TC >= 4
        SPLIT_DEVIATION_THRESHOLDS[10][4] = 6.0;
        SPLIT_DEVIATION_THRESHOLDS[10][5] = 5.0;
        SPLIT_DEVIATION_THRESHOLDS[10][6] = 4.0;

        // S17 surrender/stand deviation indices
        // TODO: Replace with S17-specific values
        SURRENDER_16_VS_8_INDEX = 4.0;
        SURRENDER_16_VS_9_INDEX = -1.0;
        STAND_16_VS_9_INDEX = 4.0;
        STAND_16_VS_ACE_INDEX = 3.0;
        SURRENDER_15_VS_ACE_INDEX = -1.0;
        STAND_15_VS_ACE_INDEX = 5.0;
        SURRENDER_15_VS_9_INDEX = 2.0;
        STAND_15_VS_10_INDEX = 4.0;
    }

    public S17GameEngine(int totalDecks, double startingBankroll, double tableMinBet, double tableMaxBet) {
        super(totalDecks, startingBankroll, tableMinBet, tableMaxBet, true, true, false);
    }

    public S17GameEngine(int totalDecks, double startingBankroll, double tableMinBet, double tableMaxBet, boolean allowLateSurrender, boolean allowResplitAces) {
        super(totalDecks, startingBankroll, tableMinBet, tableMaxBet, allowLateSurrender, allowResplitAces, false);
    }
}
