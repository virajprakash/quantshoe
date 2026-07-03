package com.quantshoe.engine;

import com.quantshoe.risk.DeviationMatrix;
import com.quantshoe.core.Shoe;
import com.quantshoe.strategy.StrategyAction;

public final class GameEngine {

    // ==========================================
// 1. HARD TOTALS MATRIX (Indices: Player Hard Total 4-21 vs Dealer Upcard 2-11)
// ==========================================
    private static final StrategyAction[][] HARD_MATRIX = new StrategyAction[22][12];

    // ==========================================
// 2. SOFT TOTALS MATRIX (Indices: Player Non-Ace Card Value 2-10 vs Dealer Upcard 2-11)
// Example: Ace + 6 is index 6. Ace + 7 is index 7.
// ==========================================
    private static final StrategyAction[][] SOFT_MATRIX = new StrategyAction[11][12];

    // ==========================================
// 3. PAIR SPLITTING MATRIX (Indices: Split Card Value 2-11 vs Dealer Upcard 2-11)
// Example: Pair of 8s is index 8. Pair of Aces is index 11.
// ==========================================
    private static final boolean[][] SPLIT_MATRIX = new boolean[12][12];

    static {
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

        // Example: For Hard 9 vs Dealer 3, 4, 5, 6 -> DOUBLE, else HIT
        for (int d = 3; d <= 6; d++) HARD_MATRIX[9][d] = com.quantshoe.strategy.StrategyAction.DOUBLE_OR_HIT;

        // Example: For Hard 12 vs Dealer 4, 5, 6 -> STAND
        for (int d = 4; d <= 6; d++) HARD_MATRIX[12][d] = com.quantshoe.strategy.StrategyAction.STAND;

        // Example: Hard 16 vs Dealer 9, 10, 11 -> SURRENDER (Commonly used index strategy)
        HARD_MATRIX[16][9] = com.quantshoe.strategy.StrategyAction.SURRENDER_OR_HIT;
        HARD_MATRIX[16][10] = com.quantshoe.strategy.StrategyAction.SURRENDER_OR_HIT;
        HARD_MATRIX[16][11] = com.quantshoe.strategy.StrategyAction.SURRENDER_OR_HIT;

        // Hard 17 and above always stands against everything
        for (int p = 17; p <= 21; p++) {
            for (int d = 2; d <= 11; d++) {
                HARD_MATRIX[p][d] = com.quantshoe.strategy.StrategyAction.STAND;
            }
        }


        // -------------------------------------------------------------
        // ENTER DATA HERE: SOFT TOTALS SKELETON (Ace + Card)
        // -------------------------------------------------------------
        // Index matches the OTHER card. (e.g., SOFT_MATRIX[7] means Ace + 7 = Soft 18)

        // Example: Ace + 7 (Soft 18) vs Dealer 2, 7, 8 -> STAND
        SOFT_MATRIX[7][2] = com.quantshoe.strategy.StrategyAction.STAND;
        SOFT_MATRIX[7][7] = com.quantshoe.strategy.StrategyAction.STAND;
        SOFT_MATRIX[7][8] = com.quantshoe.strategy.StrategyAction.STAND;
        // Ace + 7 vs Dealer 3, 4, 5, 6 -> DOUBLE
        for (int d = 3; d <= 6; d++) SOFT_MATRIX[7][d] = com.quantshoe.strategy.StrategyAction.DOUBLE_OR_STAND;
        // Ace + 7 vs Dealer 9, 10, 11 -> HIT
        for (int d = 9; d <= 11; d++) SOFT_MATRIX[7][d] = com.quantshoe.strategy.StrategyAction.HIT;

        // Soft 19 (Ace + 8) and Soft 20 (Ace + 9) usually stand against everything
        for (int p = 8; p <= 10; p++) {
            for (int d = 2; d <= 11; d++) {
                SOFT_MATRIX[p][d] = com.quantshoe.strategy.StrategyAction.STAND;
            }
        }


        // -------------------------------------------------------------
        // ENTER DATA HERE: SPLITTING SKELETON
        // -------------------------------------------------------------
        // Index matches the face value of ONE of the cards (e.g., index 8 for a pair of 8s, 11 for Aces)

        // Example: Always split Aces (11) and 8s
        for (int d = 2; d <= 11; d++) {
            SPLIT_MATRIX[8][d] = true;
            SPLIT_MATRIX[11][d] = true;
        }

        // Example: Split 4s ONLY against Dealer 5 and 6
        SPLIT_MATRIX[4][5] = true;
        SPLIT_MATRIX[4][6] = true;

        // Example: Never split 5s or 10s/Faces
        // (They remain false by default due to our initialization loop)
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

    private final int[] dealerHand = new int[21];
    private int dealerCardCount;
    private double currentBankroll;

    public GameEngine(int totalDecks, double startingBankroll, double tableMinBet, double tableMaxBet) {
        this.shoe = new Shoe(totalDecks);
        this.currentBankroll = startingBankroll;
        this.tableMinBet = tableMinBet;
        this.tableMaxBet = tableMaxBet;
    }

    public double playRound(double targetBet) {
        double baselineWager = Math.max(tableMinBet, Math.min(targetBet, tableMaxBet));
        if (baselineWager > currentBankroll) {
            baselineWager = currentBankroll;
        }

        resetRoundState();

        // Setup initial primary hand (Index 0)
        playerHandWagers[0] = baselineWager;
        int activePlayerHandsCount = 1;

        // Deal initial cards
        playerHands[0][playerHandCardCounts[0]++] = shoe.dealCard().getValue();
        dealerHand[dealerCardCount++] = shoe.dealCard().getValue();
        playerHands[0][playerHandCardCounts[0]++] = shoe.dealCard().getValue();
        dealerHand[dealerCardCount++] = shoe.dealCard().getValue(); // Dealer upcard is dealerHand[0]

        int dealerUpcard = dealerHand[0];
        int dealerTotal = calculateHandValue(dealerHand, dealerCardCount);

        // 1. --- EXTENSION: INSURANCE PROTOCOL ---
        // If dealer upcard is an Ace (value 11), evaluate insurance entry point
        if (dealerUpcard == 11 && currentBankroll >= (baselineWager * 1.5)) {
            boolean takeInsurance = evaluateInsuranceMatrix(shoe.getTrueCount()); // Quant matrix logic
            if (takeInsurance) {
                double insuranceBet = baselineWager * 0.5;
                if (dealerTotal == 21) {
                    currentBankroll += (insuranceBet * 2.0); // Insurance pays 2:1
                } else {
                    currentBankroll -= insuranceBet;
                }
            }
        }

        // Check for immediate natural Blackjacks
        boolean playerInitialBJ = (calculateHandValue(playerHands[0], playerHandCardCounts[0]) == 21);
        boolean dealerBJ = (dealerTotal == 21);
        if (playerInitialBJ || dealerBJ) {
            return resolveNaturalBlackjacks(playerInitialBJ, dealerBJ, baselineWager);
        }

        // 2. --- EXTENSION: SURRENDER PROTOCOL ---
        // Evaluated immediately on the initial 2-card hand
        if (evaluateSurrenderMatrix(playerHands[0], dealerUpcard, shoe.getTrueCount())) {
            playerHandSurrendered[0] = true;
            currentBankroll -= (baselineWager * 0.5);
            return -(baselineWager * 0.5);
        }

        // 3. --- EXTENSION: DYNAMIC SPLITTING & GAMEPLAY LOOP ---
        // We use a simple pointer tracking index to handle hands sequentially, including newly generated split hands
        for (int h = 0; h < activePlayerHandsCount; h++) {

            // Check for recursive split triggers (e.g., pairs) up to a max table limit of 4 hands
            while (activePlayerHandsCount < 4 && playerHandCardCounts[h] == 2 && playerHands[h][0] == playerHands[h][1]) {
                if (evaluateSplitMatrix(playerHands[h][0], dealerUpcard, shoe.getTrueCount()) && currentBankroll >= (currentBankroll + playerHandWagers[h])) {

                    // Isolate the matching card and push it to a brand new hand index
                    int newHandIdx = activePlayerHandsCount;
                    playerHands[newHandIdx][0] = playerHands[h][1]; // Move 2nd card to new hand
                    playerHandCardCounts[newHandIdx] = 1;
                    playerHandWagers[newHandIdx] = playerHandWagers[h]; // Match original wager size

                    // Truncate original hand back to 1 card
                    playerHandCardCounts[h] = 1;

                    // Immediately hit both newly separated hands to bring them back up to 2 cards
                    playerHands[h][playerHandCardCounts[h]++] = shoe.dealCard().getValue();
                    playerHands[newHandIdx][playerHandCardCounts[newHandIdx]++] = shoe.dealCard().getValue();

                    activePlayerHandsCount++; // Increment global hand scope pointer
                } else {
                    break; // Matrix says don't split, continue to standard decision flow
                }
            }

            // Execute standard tactical decision loop for current active hand 'h'
            boolean handActive = true;
            while (handActive) {
                int currentTotal = calculateHandValue(playerHands[h], playerHandCardCounts[h]);
                if (currentTotal >= 21) break;

                // Query your localized choice index array
                Decision decision = evaluateMainMatrix(playerHands[h], playerHandCardCounts[h], dealerUpcard, shoe.getTrueCount());

                switch (decision) {
                    case DOUBLE:
                        if (playerHandCardCounts[h] == 2 && currentBankroll >= (currentBankroll + playerHandWagers[h])) {
                            playerHandWagers[h] *= 2.0; // Double down wager profile
                            playerHands[h][playerHandCardCounts[h]++] = shoe.dealCard().getValue();
                            handActive = false; // Forced single card ceiling limit on doubles
                        } else {
                            // Fallback to HIT if bankroll constraints or rule states deny double down
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
                currentBankroll -= playerHandWagers[h];
            }
        }

        // 4. --- DEALER EXECUTION ENGINE ---
        // The dealer only hits if at least one player split profile survived the initial rounds without busting/surrendering
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
            //H17 Environment
            while (dealerTotal < 17 || (dealerTotal == 17 && checkIsSoftHand(dealerHand, dealerCardCount))) {
                dealerHand[dealerCardCount++] = shoe.dealCard().getValue();
                dealerTotal = calculateHandValue(dealerHand, dealerCardCount);
            }
        }

        // 5. --- MULTI-HAND CAPITAL DISBURSEMENT SYSTEM ---
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
                currentBankroll -= playerHandWagers[h];
                totalRoundPnL -= playerHandWagers[h];
            }
            // Pushes add 0.0 to PnL
        }

        return totalRoundPnL;
    }

    // --- ENUM FOR MATRIX MAPPING INTEGRATION ---
    public enum Decision {HIT, STAND, DOUBLE}

    // --- MATRIX PLUG-IN HOOK METHODS ---
    // Plug your precise 2D multidimensional decision arrays right into these evaluation filters!
    private boolean evaluateInsuranceMatrix(double trueCount) {
        return trueCount >= 3.0; // Statistical baseline: Insurance is mathematically viable at True +3
    }

    private boolean evaluateSurrenderMatrix(int[] initialHand, int dealerUpcard, double trueCount) {
        // Plug your Surrender Matrix configurations here
        return false;
    }

    private boolean evaluateSplitMatrix(int cardValue, int dealerUpcard, double trueCount) {
        // Plug your Split Matrix configurations here
        return false;
    }

    private Decision evaluateMainMatrix(int[] hand, int count, int dealerUpcard, double trueCount) {
        int total = calculateHandValue(hand, count);
        boolean isSoft = checkIsSoftHand(hand, count); // Your existing tracking logic

        StrategyAction action;
        if (isSoft) {
            int nonAceValue = total - 11;
            if (nonAceValue < 2) nonAceValue = 2;
            if (nonAceValue > 10) return Decision.STAND;
            action = SOFT_MATRIX[nonAceValue][dealerUpcard];
        } else {
            if (total > 21) return Decision.STAND;
            action = HARD_MATRIX[total][dealerUpcard];
        }

        // --- INTERCEPT ACTION VECTOR VIA TRUE COUNT DEVIATION LOOKUPS ---
        double threshold = DeviationMatrix.getThreshold(total, dealerUpcard);

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
            case DOUBLE_OR_STAND:
                if (count == 2) return Decision.DOUBLE;
                return (action == com.quantshoe.strategy.StrategyAction.DOUBLE_OR_HIT) ? Decision.HIT : Decision.STAND;

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
            currentBankroll -= wager;
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