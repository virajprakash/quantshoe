package com.quantshoe.engine;

import com.quantshoe.core.DeckEstimation;
import com.quantshoe.core.Shoe;
import com.quantshoe.strategy.StrategyAction;

public class GameEngine {

    // ==========================================
// 1. HARD TOTALS MATRIX (Indices: Player Hard Total 4-21 vs Dealer Upcard 2-11)
// ==========================================
    protected final StrategyAction[][] HARD_MATRIX = new StrategyAction[22][12];

    // ==========================================
// 2. SOFT TOTALS MATRIX (Indices: Player Non-Ace Card Value 2-10 vs Dealer Upcard 2-11)
// Example: Ace + 6 is index 6. Ace + 7 is index 7.
// ==========================================
    protected final StrategyAction[][] SOFT_MATRIX = new StrategyAction[11][12];

    // ==========================================
// 3. PAIR SPLITTING MATRIX (Indices: Split Card Value 2-11 vs Dealer Upcard 2-11)
// Example: Pair of 8s is index 8. Pair of Aces is index 11.
// ==========================================
    protected final boolean[][] SPLIT_MATRIX = new boolean[12][12];

    // ==========================================
// 4. DEVIATION THRESHOLDS (Indices: [Player Total/Card][Dealer Upcard])
// ==========================================
    protected final double[][] DEVIATION_THRESHOLDS = new double[22][12];
    protected final double[][] SOFT_DEVIATION_THRESHOLDS = new double[11][12];
    protected final double[][] SPLIT_DEVIATION_THRESHOLDS = new double[12][12];

    // Surrender/stand deviation indices
    protected double SURRENDER_16_VS_8_INDEX = 4.0;
    protected double SURRENDER_16_VS_9_INDEX = -1.0;
    protected double STAND_16_VS_9_INDEX = 4.0;
    protected double STAND_16_VS_ACE_INDEX = 3.0;
    protected double SURRENDER_15_VS_ACE_INDEX = -1.0;
    protected double STAND_15_VS_ACE_INDEX = 5.0;
    protected double SURRENDER_15_VS_9_INDEX = 2.0;
    protected double STAND_15_VS_10_INDEX = 4.0;

    {
        // Fill every cell with a safe default action (HIT) first to avoid NullPointerExceptions
        for (int p = 0; p < 22; p++) {
            for (int d = 0; d < 12; d++) {
                HARD_MATRIX[p][d] = com.quantshoe.strategy.StrategyAction.HIT;
                if (p < 11) SOFT_MATRIX[p][d] = com.quantshoe.strategy.StrategyAction.HIT;
                if (p < 12) SPLIT_MATRIX[p][d] = false;
            }
        }

        // -------------------------------------------------------------
        // ENTER DATA HERE: HARD TOTALS SKELETON
        // -------------------------------------------------------------
        // Loop/Set values for Player Hard Totals 4 through 21 vs Dealer Upcards (2 to 11)

        for (int p = 17; p <= 21; p++) {
            for (int d = 2; d <= 11; d++) {
                HARD_MATRIX[p][d] = StrategyAction.STAND;
            }
        }

// 2. Hard 12 through 16: STAND against low dealer upcards (2 through 6)
// (Note: Hard 12 vs 2 or 3 is a HIT in basic strategy, handled below)
        for (int p = 13; p <= 16; p++) {
            for (int d = 2; d <= 6; d++) {
                HARD_MATRIX[p][d] = StrategyAction.STAND;
            }
        }
        for (int d = 4; d <= 6; d++) {
            HARD_MATRIX[12][d] = StrategyAction.STAND; // 12 stands only vs 4, 5, 6
        }

// 3. Hard 11: Always DOUBLE against 2 through 10, HIT against Ace (S17 baseline)
        for (int d = 2; d <= 11; d++) {
            HARD_MATRIX[11][d] = StrategyAction.DOUBLE_OR_HIT;
        }

// 4. Hard 10: DOUBLE against 2 through 9, HIT against 10 and Ace
        for (int d = 2; d <= 9; d++) {
            HARD_MATRIX[10][d] = StrategyAction.DOUBLE_OR_HIT;
        }

// 5. Hard 9: DOUBLE against 3 through 6, HIT against everything else
        for (int d = 3; d <= 6; d++) {
            HARD_MATRIX[9][d] = StrategyAction.DOUBLE_OR_HIT;
        }
        // Illustrious 18 deviations: Stand on stiff hands when count is high
        HARD_MATRIX[15][10] = StrategyAction.DEVIATE_STAND_IF_ABOVE;
        HARD_MATRIX[16][10] = StrategyAction.SURRENDER_16_VS_10_RUNNING_COUNT;
        HARD_MATRIX[12][2] = StrategyAction.DEVIATE_STAND_IF_ABOVE;
        HARD_MATRIX[12][3] = StrategyAction.DEVIATE_STAND_IF_ABOVE;

        // Illustrious 18 deviations: Double on hard totals when count is high
        HARD_MATRIX[10][10] = StrategyAction.DEVIATE_DOUBLE_IF_ABOVE;
        HARD_MATRIX[10][11] = StrategyAction.DEVIATE_DOUBLE_IF_ABOVE;
        HARD_MATRIX[9][2] = StrategyAction.DEVIATE_DOUBLE_IF_ABOVE;
        HARD_MATRIX[9][7] = StrategyAction.DEVIATE_DOUBLE_IF_ABOVE;
        HARD_MATRIX[8][6] = StrategyAction.DEVIATE_DOUBLE_IF_ABOVE;

        //Surrender hard 17 against dealer ace
        HARD_MATRIX[17][11] = StrategyAction.SURRENDER_OR_HIT;

        HARD_MATRIX[16][8]  = StrategyAction.SURRENDER_16_VS_8_ABOVE_4;
        HARD_MATRIX[16][9]  = StrategyAction.SURRENDER_16_VS_9_ABOVE_MINUS_1_ELSE_STAND_ABOVE_4;
        HARD_MATRIX[16][10] = StrategyAction.SURRENDER_16_VS_10_RUNNING_COUNT;
        HARD_MATRIX[16][11] = StrategyAction.SURRENDER_16_VS_ACE_ELSE_STAND_ABOVE_3; // Index 11 is Dealer Ace

        HARD_MATRIX[15][9]  = StrategyAction.SURRENDER_15_VS_9_ABOVE_2_ELSE_HIT;
        HARD_MATRIX[15][10] = StrategyAction.SURRENDER_15_VS_10_WHEN_RUNNING_NON_NEGATIVE_ELSE_STAND_ABOVE_4;
        HARD_MATRIX[15][11] = StrategyAction.SURRENDER_15_VS_ACE_ABOVE_MINUS_1_ELSE_STAND_ABOVE_5;

        // -------------------------------------------------------------
        // ENTER DATA HERE: SOFT TOTALS SKELETON (Ace + Card)
        // -------------------------------------------------------------
        // Index matches the OTHER card. (e.g., SOFT_MATRIX[7] means Ace + 7 = Soft 18)

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

        // Example: Ace + 7 (Soft 18) vs Dealer 7, 8 -> STAND
        SOFT_MATRIX[7][7] = com.quantshoe.strategy.StrategyAction.STAND;
        SOFT_MATRIX[7][8] = com.quantshoe.strategy.StrategyAction.STAND;
        // Ace + 7 vs Dealer 2, 3, 4, 5, 6 -> DOUBLE
        for (int d = 2; d <= 6; d++) SOFT_MATRIX[7][d] = com.quantshoe.strategy.StrategyAction.DOUBLE_OR_STAND;
        // Ace + 7 vs Dealer 9, 10, 11 -> HIT
        for (int d = 9; d <= 11; d++) SOFT_MATRIX[7][d] = com.quantshoe.strategy.StrategyAction.HIT;

        // Soft 19 (Ace + 8) and Soft 20 (Ace + 9) usually stand against everything
        for (int p = 8; p <= 10; p++) {
            for (int d = 2; d <= 11; d++) {
                SOFT_MATRIX[p][d] = com.quantshoe.strategy.StrategyAction.STAND;
            }
        }
        SOFT_MATRIX[8][4] = StrategyAction.DEVIATE_DOUBLE_IF_STAND;
        SOFT_MATRIX[8][5] = StrategyAction.DEVIATE_DOUBLE_IF_STAND;
        SOFT_MATRIX[8][6] = StrategyAction.DEVIATE_DOUBLE_IF_STAND;


        // -------------------------------------------------------------
        // ENTER DATA HERE: SPLITTING SKELETON
        // -------------------------------------------------------------
        // Index matches the face value of ONE of the cards (e.g., index 8 for a pair of 8s, 11 for Aces)
// 1. Always split Aces (11) and 8s against any dealer upcard
        for (int d = 2; d <= 11; d++) {
            SPLIT_MATRIX[11][d] = true;
            SPLIT_MATRIX[8][d] = true;
        }

// 2. Split 9s against 2 through 9, EXCEPT against a 7. STAND against 10 and Ace.
        for (int d = 2; d <= 9; d++) {
            if (d != 7) {
                SPLIT_MATRIX[9][d] = true;
            }
        }

// 3. Split 7s and 3s against 2 through 7. Otherwise false (HIT).
// 4. Split 2s and 6s against 2 through 6. Otherwise false (HIT).
        for (int d = 2; d <= 7; d++) {
            SPLIT_MATRIX[7][d] = true;
            SPLIT_MATRIX[3][d] = true;
        }
        for (int d = 2; d <= 6; d++) {
            SPLIT_MATRIX[6][d] = true;
            SPLIT_MATRIX[2][d] = true;
        }

// 5. Split 4s ONLY against dealer 5 and 6
        SPLIT_MATRIX[4][5] = true;
        SPLIT_MATRIX[4][6] = true;
        // Example: Never split 5s or 10s/Faces
        // (They remain false by default due to our initialization loop)

        // -------------------------------------------------------------
        // DEVIATION THRESHOLDS (H17)
        // -------------------------------------------------------------
        for (int p = 0; p < 22; p++) {
            for (int d = 0; d < 12; d++) {
                if (p < 11) SOFT_DEVIATION_THRESHOLDS[p][d] = Double.NaN;
                DEVIATION_THRESHOLDS[p][d] = Double.NaN;
            }
        }

        DEVIATION_THRESHOLDS[8][6] = 2.0;
        DEVIATION_THRESHOLDS[0][11] = 3.0;
        DEVIATION_THRESHOLDS[16][10] = 0.0;
        DEVIATION_THRESHOLDS[15][10] = 4.0;
        DEVIATION_THRESHOLDS[10][11] = 4.0;
        DEVIATION_THRESHOLDS[12][2] = 3.0;
        DEVIATION_THRESHOLDS[12][3] = 2.0;
        DEVIATION_THRESHOLDS[12][4] = 0;
        DEVIATION_THRESHOLDS[13][2] = -1.0;
        DEVIATION_THRESHOLDS[13][3] = -2.0;
        DEVIATION_THRESHOLDS[10][10] = 4.0;
        DEVIATION_THRESHOLDS[10][11] = 3.0;
        DEVIATION_THRESHOLDS[9][2] = 1.0;
        DEVIATION_THRESHOLDS[9][7] = 3.0;

        SOFT_DEVIATION_THRESHOLDS[6][2] = 1.0;
        SOFT_DEVIATION_THRESHOLDS[8][4] = 3.0;
        SOFT_DEVIATION_THRESHOLDS[8][5] = 1.0;
        SOFT_DEVIATION_THRESHOLDS[8][6] = 0.0;

        // -------------------------------------------------------------
        // SPLIT DEVIATION THRESHOLDS (H17)
        // -------------------------------------------------------------
        for (int p = 0; p < 12; p++) {
            for (int d = 0; d < 12; d++) {
                SPLIT_DEVIATION_THRESHOLDS[p][d] = Double.NaN;
            }
        }

        // Split 10s vs dealer 4 at TC >= 6, vs dealer 5 at TC >= 5, vs dealer 6 at TC >= 4
        SPLIT_DEVIATION_THRESHOLDS[10][4] = 6.0;
        SPLIT_DEVIATION_THRESHOLDS[10][5] = 5.0;
        SPLIT_DEVIATION_THRESHOLDS[10][6] = 4.0;
    }

    private final Shoe shoe;
    private final double tableMinBet;
    private final double tableMaxBet;

    // --- QUANT PERFORMANCE IMPROVEMENT: MEMORY-FLAT MULTI-HAND TRACKING ---
    // A single round can split up to 4 distinct hands.
    // Row 0-3 = Hand Index. Column 0-20 = Pre-allocated space for card values.
    private final int[][] playerHands = new int[4][21];
    private final int[] playerHandCardCounts = new int[4];
    private final double[] playerHandWagers = new double[4];
    private final boolean[] playerHandBusted = new boolean[4];
    private final boolean[] playerHandSurrendered = new boolean[4];
    private final boolean[] playerHandFromSplitAces = new boolean[4];

    private final int[] dealerHand = new int[21];
    private int dealerCardCount;
    private double currentBankroll;
    private final boolean allowLateSurrender;
    private final boolean allowResplitAces;
    protected final boolean dealerHitsSoft17;

    public GameEngine(int totalDecks, double startingBankroll, double tableMinBet, double tableMaxBet) {
        this(totalDecks, startingBankroll, tableMinBet, tableMaxBet, true, true, true, DeckEstimation.FULL);
    }

    public GameEngine(int totalDecks, double startingBankroll, double tableMinBet, double tableMaxBet, boolean allowLateSurrender, boolean allowResplitAces) {
        this(totalDecks, startingBankroll, tableMinBet, tableMaxBet, allowLateSurrender, allowResplitAces, true, DeckEstimation.FULL);
    }

    public GameEngine(int totalDecks, double startingBankroll, double tableMinBet, double tableMaxBet, boolean allowLateSurrender, boolean allowResplitAces, boolean dealerHitsSoft17) {
        this(totalDecks, startingBankroll, tableMinBet, tableMaxBet, allowLateSurrender, allowResplitAces, dealerHitsSoft17, DeckEstimation.FULL);
    }

    public GameEngine(int totalDecks, double startingBankroll, double tableMinBet, double tableMaxBet, boolean allowLateSurrender, boolean allowResplitAces, boolean dealerHitsSoft17, DeckEstimation deckEstimation) {
        this.shoe = new Shoe(totalDecks, deckEstimation);
        this.currentBankroll = startingBankroll;
        this.tableMinBet = tableMinBet;
        this.tableMaxBet = tableMaxBet;
        this.allowLateSurrender = allowLateSurrender;
        this.allowResplitAces = allowResplitAces;
        this.dealerHitsSoft17 = dealerHitsSoft17;
    }

    public double playRound(int numHands, double targetBet) {
        double baselineWager = Math.max(tableMinBet, Math.min(targetBet, tableMaxBet));
        // Ensure total exposure across all hands doesn't exceed bankroll
        if (baselineWager * numHands > currentBankroll) {
            baselineWager = Math.floor(currentBankroll / numHands);
            if (baselineWager < tableMinBet) {
                // Can't afford multiple hands at table min — reduce to as many hands as possible
                numHands = Math.max(1, (int) (currentBankroll / tableMinBet));
                baselineWager = tableMinBet;
            }
            if (baselineWager > currentBankroll) {
                baselineWager = currentBankroll;
            }
        }

        resetRoundState();

        // Setup wagers and deal cards for all initial hands
        int activePlayerHandsCount = numHands;
        for (int i = 0; i < numHands; i++) {
            playerHandWagers[i] = baselineWager;
            // Deal initial cards
            playerHands[i][playerHandCardCounts[i]++] = shoe.dealCard().getValue();
            playerHands[i][playerHandCardCounts[i]++] = shoe.dealCard().getValue();
        }
        dealerHand[dealerCardCount++] = shoe.dealCard().getValue();
        dealerHand[dealerCardCount++] = shoe.dealCard().getValue(); // Dealer upcard is dealerHand[0]

        int dealerUpcard = dealerHand[0];
        int dealerTotal = calculateHandValue(dealerHand, dealerCardCount);

        // 1. Insurance check (dealer shows Ace)
        if (dealerUpcard == 11 && currentBankroll >= (baselineWager * 1.5)) {
            boolean takeInsurance = evaluateInsuranceMatrix(shoe.getTrueCount());
            if (takeInsurance) {
                double insuranceBet = baselineWager * 0.5;
                if (dealerTotal == 21) {
                    currentBankroll += (insuranceBet * 2.0); // Insurance pays 2:1
                } else {
                    currentBankroll = Math.max(0, currentBankroll - insuranceBet);
                }
            }
        }

        // Check for immediate natural Blackjacks
        boolean playerInitialBJ = (calculateHandValue(playerHands[0], playerHandCardCounts[0]) == 21);
        boolean dealerBJ = (dealerTotal == 21);
        if (playerInitialBJ || dealerBJ) {
            return resolveNaturalBlackjacks(playerInitialBJ, dealerBJ, baselineWager);
        }

        // 2. Surrender check (initial 2-card hand only)
        if (allowLateSurrender && evaluateSurrenderMatrix(playerHands[0], dealerUpcard, shoe.getTrueCount(), shoe.getRunningCount())) {
            playerHandSurrendered[0] = true;
            currentBankroll = Math.max(0, currentBankroll - (baselineWager * 0.5));
            return -(baselineWager * 0.5);
        }

        // 3. Splitting and play loop
        for (int h = 0; h < activePlayerHandsCount; h++) {

            // Check for recursive split triggers (e.g., pairs) up to a max table limit of 4 hands
            while (activePlayerHandsCount < 4 && playerHandCardCounts[h] == 2 && playerHands[h][0] == playerHands[h][1]
                    && (!playerHandFromSplitAces[h] || allowResplitAces)) {
                if (evaluateSplitMatrix(playerHands[h][0], dealerUpcard, shoe.getTrueCount()) && currentBankroll >= playerHandWagers[h]) {

                    // Track if we are splitting Aces
                    boolean splittingAces = (playerHands[h][0] == 11);

                    // Move second card to a new hand
                    int newHandIdx = activePlayerHandsCount;
                    playerHands[newHandIdx][0] = playerHands[h][1]; // Move 2nd card to new hand
                    playerHandCardCounts[newHandIdx] = 1;
                    playerHandWagers[newHandIdx] = playerHandWagers[h];

                    // Reset original hand to 1 card
                    playerHandCardCounts[h] = 1;

                    // Split Aces get one card each, no further hitting
                    if (splittingAces) {
                        playerHandFromSplitAces[h] = true;
                        playerHandFromSplitAces[newHandIdx] = true;
                    }

                    // Deal one card to each split hand
                    playerHands[h][playerHandCardCounts[h]++] = shoe.dealCard().getValue();
                    playerHands[newHandIdx][playerHandCardCounts[newHandIdx]++] = shoe.dealCard().getValue();

                    activePlayerHandsCount++;
                } else {
                    break;
                }
            }

            // Split Aces receive one card only — no further hitting allowed
            if (playerHandFromSplitAces[h]) {
                if (calculateHandValue(playerHands[h], playerHandCardCounts[h]) > 21) {
                    playerHandBusted[h] = true;
                    currentBankroll = Math.max(0, currentBankroll - playerHandWagers[h]);
                }
                continue;
            }

            // Play out the hand (hit/stand/double loop)
            boolean handActive = true;
            while (handActive) {
                int currentTotal = calculateHandValue(playerHands[h], playerHandCardCounts[h]);
                if (currentTotal >= 21) break;

                // Look up the correct play
                Decision decision = evaluateMainMatrix(playerHands[h], playerHandCardCounts[h], dealerUpcard, shoe.getTrueCount());

                switch (decision) {
                    case DOUBLE:
                        if (playerHandCardCounts[h] == 2 && currentBankroll >= playerHandWagers[h]) {
                            playerHandWagers[h] *= 2.0;
                            playerHands[h][playerHandCardCounts[h]++] = shoe.dealCard().getValue();
                            handActive = false; // Only one card on a double
                        } else {
                            // Can't double — hit instead
                            playerHands[h][playerHandCardCounts[h]++] = shoe.dealCard().getValue();
                        }
                        break;

                    case HIT:
                        playerHands[h][playerHandCardCounts[h]++] = shoe.dealCard().getValue();
                        break;

                    case STAND:
                    default:
                        handActive = false;
                        break;
                }
            }

            // Flag bust conditions
            if (calculateHandValue(playerHands[h], playerHandCardCounts[h]) > 21) {
                playerHandBusted[h] = true;
                currentBankroll = Math.max(0, currentBankroll - playerHandWagers[h]);
            }
        }

        // 4. Dealer plays (only if at least one player hand is still live)
        boolean dealerMustPlay = false;
        for (int h = 0; h < activePlayerHandsCount; h++) {
            if (!playerHandBusted[h] && !playerHandSurrendered[h]) {
                dealerMustPlay = true;
                break;
            }
        }

        if (dealerMustPlay) {
            //S17 Matrix Environment
//            while (dealerTotal < 17) {
//                dealerHand[dealerCardCount++] = shoe.dealCard().getValue();
//                dealerTotal = calculateHandValue(dealerHand, dealerCardCount);
//            }
            //H17 or S17 Environment (controlled by dealerHitsSoft17 flag)
            while (dealerTotal < 17 || (dealerHitsSoft17 && dealerTotal == 17 && checkIsSoftHand(dealerHand, dealerCardCount))) {
                dealerHand[dealerCardCount++] = shoe.dealCard().getValue();
                dealerTotal = calculateHandValue(dealerHand, dealerCardCount);
            }
        }

        // 5. Settle all hands
        double totalRoundPnL = 0.0;
        for (int h = 0; h < activePlayerHandsCount; h++) {
            if (playerHandSurrendered[h]) {
                totalRoundPnL -= (playerHandWagers[h] * 0.5);
                continue;
            }
            if (playerHandBusted[h]) {
                totalRoundPnL -= playerHandWagers[h];
                continue;
            }

            int pTotal = calculateHandValue(playerHands[h], playerHandCardCounts[h]);
            if (dealerTotal > 21 || pTotal > dealerTotal) {
                currentBankroll += playerHandWagers[h];
                totalRoundPnL += playerHandWagers[h];
            } else if (pTotal < dealerTotal) {
                currentBankroll = Math.max(0, currentBankroll - playerHandWagers[h]);
                totalRoundPnL -= playerHandWagers[h];
            }
            // Pushes add 0.0 to PnL
        }

        return totalRoundPnL;
    }

    public enum Decision {HIT, STAND, DOUBLE}

    protected boolean evaluateInsuranceMatrix(double trueCount) {
        return trueCount >= 3.0; // Insurance is +EV at true count >= +3
    }

    /**
     * Evaluates whether the initial 2-card player hand qualifies for a baseline,
     * true-count, or running-count dependent surrender deviation.
     */
    protected boolean evaluateSurrenderMatrix(int[] initialHand, int dealerUpcard, double trueCount, int runningCount) {
        int total = initialHand[0] + initialHand[1]; // Ensure pulling from initial 2-card indices

        // Safety boundary constraints
        boolean isSoft = (initialHand[0] == 11 || initialHand[1] == 11);
        if (isSoft || total > 21 || total < 4) {
            return false;
        }

        StrategyAction action = HARD_MATRIX[total][dealerUpcard];

        // Scenario A: Static Basic Strategy Surrender (Always Surrender)
        if (action == StrategyAction.SURRENDER_16_VS_10_RUNNING_COUNT) {
            // Pair of 8's shouldn't surrender vs a 10
            return initialHand[0] != 8 || initialHand[1] != 8;
        }

        if (action == StrategyAction.SURRENDER_16_VS_8_ABOVE_4) {
            return trueCount >= SURRENDER_16_VS_8_INDEX;
        }

        if (action == StrategyAction.SURRENDER_16_VS_9_ABOVE_MINUS_1_ELSE_STAND_ABOVE_4) {
            return trueCount >= SURRENDER_16_VS_9_INDEX;
        }

        if (action == StrategyAction.SURRENDER_16_VS_ACE_ELSE_STAND_ABOVE_3) {
            return true; // Basic strategy: always surrender 16 vs Ace
        }

        if (action == StrategyAction.SURRENDER_OR_HIT) {
            return true; // Basic strategy surrender (e.g., hard 17 vs Ace in some rulesets)
        }

        if (action == StrategyAction.SURRENDER_15_VS_ACE_ABOVE_MINUS_1_ELSE_STAND_ABOVE_5) {
            return trueCount >= SURRENDER_15_VS_ACE_INDEX;
        }

        if (action == StrategyAction.SURRENDER_15_VS_10_WHEN_RUNNING_NON_NEGATIVE_ELSE_STAND_ABOVE_4) {
            return runningCount >= 0;
        }

        if (action == StrategyAction.SURRENDER_15_VS_9_ABOVE_2_ELSE_HIT) {
            return trueCount >= SURRENDER_15_VS_9_INDEX;
        }

        // Pull assigned threshold bounds from instance deviation data
        double threshold = DEVIATION_THRESHOLDS[total][dealerUpcard];
        if (Double.isNaN(threshold)) {
            return false; // Safe fallback if data missing
        }

        // Scenario B: True Count Verification Layers
        if (action == StrategyAction.DEVIATE_SURRENDER_IF_TRUE_COUNT_ABOVE) {
            return trueCount >= threshold;
        }
        if (action == StrategyAction.DEVIATE_SURRENDER_IF_TRUE_COUNT_BELOW) {
            return trueCount <= threshold;
        }

        // Scenario C: Running Count Verification Layers
        if (action == StrategyAction.DEVIATE_SURRENDER_IF_RUNNING_COUNT_ABOVE) {
            return runningCount >= threshold;
        }
        if (action == StrategyAction.DEVIATE_SURRENDER_IF_RUNNING_COUNT_BELOW) {
            return runningCount <= threshold;
        }

        return false;
    }

    protected boolean evaluateSplitMatrix(int cardValue, int dealerUpcard, double trueCount) {
        if (cardValue < 2 || cardValue > 11 || dealerUpcard < 2 || dealerUpcard > 11) {
            return false;
        }
        // Check for split deviation: if a threshold exists, split only when TC >= threshold
        double splitThreshold = SPLIT_DEVIATION_THRESHOLDS[cardValue][dealerUpcard];
        if (!Double.isNaN(splitThreshold)) {
            return trueCount >= splitThreshold;
        }
        return SPLIT_MATRIX[cardValue][dealerUpcard];
    }

    protected Decision evaluateMainMatrix(int[] hand, int count, int dealerUpcard, double trueCount) {
        int total = calculateHandValue(hand, count);
        if (total > 21) return Decision.STAND; // Safety catch

        boolean isSoft = checkIsSoftHand(hand, count);
        StrategyAction action;
        double threshold; // Variable to store our cleanly routed threshold number

        if (isSoft) {
            // Isolate the card next to your Ace (e.g., Soft 18 -> nonAceValue = 7)
            int nonAceValue = total - 11;
            if (nonAceValue < 2) nonAceValue = 2;
            if (nonAceValue > 10) return Decision.STAND;

            action = SOFT_MATRIX[nonAceValue][dealerUpcard];

            // ROUTING VECTOR A: Fetch exclusively from instance soft threshold grid
            threshold = SOFT_DEVIATION_THRESHOLDS[nonAceValue][dealerUpcard];
        } else {
            action = HARD_MATRIX[total][dealerUpcard];

            // ROUTING VECTOR B: Fetch exclusively from instance hard threshold grid
            threshold = DEVIATION_THRESHOLDS[total][dealerUpcard];
        }

        switch (action) {
            case DEVIATE_STAND_IF_ABOVE:
                return (trueCount >= threshold) ? Decision.STAND : Decision.HIT;

            case DEVIATE_STAND_IF_BELOW:
                return (trueCount <= threshold) ? Decision.STAND : Decision.HIT;

            case DEVIATE_DOUBLE_IF_ABOVE:
                if (count == 2) {
                    return (trueCount >= threshold) ? Decision.DOUBLE : Decision.HIT;
                }
                return Decision.HIT;

            case DEVIATE_DOUBLE_IF_STAND:
                if (count == 2) {
                    return (trueCount >= threshold) ? Decision.DOUBLE : Decision.STAND;
                }
                return Decision.STAND;

            case STAND:
                return Decision.STAND;

            case DOUBLE_OR_HIT:
                return count == 2 ? Decision.DOUBLE : Decision.HIT;
            case DOUBLE_OR_STAND:
                if (count == 2) return Decision.DOUBLE;
                return Decision.STAND;

            // Overlap surrender cases: if we reach the main matrix, surrender was already
            // evaluated and declined, so fall through to the secondary action
            case SURRENDER_16_VS_10_RUNNING_COUNT:
                // After surrender declined: stand if running count >= 0, else hit
                return (getShoe().getRunningCount() >= 0) ? Decision.STAND : Decision.HIT;

            case SURRENDER_16_VS_9_ABOVE_MINUS_1_ELSE_STAND_ABOVE_4:
                return (trueCount >= STAND_16_VS_9_INDEX) ? Decision.STAND : Decision.HIT;

            case SURRENDER_16_VS_8_ABOVE_4:
                return Decision.HIT;

            case SURRENDER_16_VS_ACE_ELSE_STAND_ABOVE_3:
                return (trueCount >= STAND_16_VS_ACE_INDEX) ? Decision.STAND : Decision.HIT;

            case SURRENDER_OR_HIT:
                return Decision.HIT;

            case SURRENDER_15_VS_ACE_ABOVE_MINUS_1_ELSE_STAND_ABOVE_5:
                return (trueCount >= STAND_15_VS_ACE_INDEX) ? Decision.STAND : Decision.HIT;

            case SURRENDER_15_VS_10_WHEN_RUNNING_NON_NEGATIVE_ELSE_STAND_ABOVE_4:
                return (trueCount >= STAND_15_VS_10_INDEX) ? Decision.STAND : Decision.HIT;

            case SURRENDER_15_VS_9_ABOVE_2_ELSE_HIT:
                return Decision.HIT;

            case HIT:
            default:
                return Decision.HIT;
        }
    }

    private int calculateHandValue(int[] hand, int count) {
        int total = 0;
        int aceCount = 0;
        for (int i = 0; i < count; i++) {
            total += hand[i];
            if (hand[i] == 11) aceCount++;
        }

        while (total > 21 && aceCount > 0) {
            total -= 10;
            aceCount--;
        }
        return total;
    }

    /**
     * Evaluates whether a hand possesses an active "Soft" Ace (valued at 11).
     *
     * @param hand  The primitive array containing the current hand's card values.
     * @param count The exact number of active cards held in this hand array buffer.
     * @return True if the hand contains a usable Ace counted as 11, false if it is a Hard total.
     */
    private boolean checkIsSoftHand(int[] hand, int count) {
        int total = 0;
        int aceCount = 0;

        // 1. Sum up raw values and track the quantity of Aces
        for (int i = 0; i < count; i++) {
            total += hand[i];
            if (hand[i] == 11) {
                aceCount++;
            }
        }

        // 2. Mirror the exact same loop the table uses to down-convert busted Aces
        while (total > 21 && aceCount > 0) {
            total -= 10;
            aceCount--;
        }

        // 3. If we still have at least one Ace valued at 11, it is mathematically a Soft Hand
        return aceCount > 0;
    }

    private double resolveNaturalBlackjacks(boolean playerBJ, boolean dealerBJ, double wager) {
        if (playerBJ && !dealerBJ) {
            double payout = wager * 1.5;
            currentBankroll += payout;
            return payout;
        } else if (!playerBJ && dealerBJ) {
            currentBankroll = Math.max(0, currentBankroll - wager);
            return -wager;
        }
        return 0; // Push
    }

    private void resetRoundState() {
        this.dealerCardCount = 0;
        for (int i = 0; i < 4; i++) {
            this.playerHandCardCounts[i] = 0;
            this.playerHandBusted[i] = false;
            this.playerHandSurrendered[i] = false;
            this.playerHandFromSplitAces[i] = false;
            this.playerHandWagers[i] = 0.0;
        }
    }

    public double getCurrentBankroll() {
        return this.currentBankroll;
    }

    public Shoe getShoe() {
        return this.shoe;
    }
}