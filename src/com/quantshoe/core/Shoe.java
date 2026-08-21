package com.quantshoe.core;

import java.util.Random;

public final class Shoe {
    private final Card[] cards;
    private final int totalDecks;
    private final DeckEstimation deckEstimation;
    private final Random random;

    private int topCardIndex;
    private int runningCount;

    public Shoe(int totalDecks) {
        this(totalDecks, DeckEstimation.FULL);
    }

    public Shoe(int totalDecks, DeckEstimation deckEstimation) {
        if (totalDecks <= 0) {
            throw new IllegalArgumentException("Shoe must contain at least 1 deck.");
        }
        this.totalDecks = totalDecks;
        this.deckEstimation = deckEstimation;
        this.cards = new Card[totalDecks * 52];
        this.random = new Random();

        initializeShoeStructure();
        shuffle();
    }

    /**
     * Pre-populates the fixed array structure with immutable Card references.
     * This happens once per object instantiation to optimize memory cache.
     */
    private void initializeShoeStructure() {
        int index = 0;
        Rank[] ranks = Rank.values();

        for (int deck = 0; deck < totalDecks; deck++) {
            for (int suit = 0; suit < 4; suit++) {
                for (Rank rank : ranks) {
                    cards[index++] = new Card(rank);
                }
            }
        }
    }

    /**
     * Resets execution pointers and performs an in-place Fisher-Yates shuffle.
     * O(N) time complexity with zero temporary object allocations.
     */
    public void shuffle() {
        this.runningCount = 0;
        this.topCardIndex = 0;

        for (int i = cards.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Card temp = cards[i];
            cards[i] = cards[j];
            cards[j] = temp;
        }
    }

    /**
     * Deals the next card from the shoe and immediately updates the system's running count.
     */
    public Card dealCard() {
        if (topCardIndex >= cards.length) {
            throw new IllegalStateException("The shoe is empty. Implement shuffle check before dealing.");
        }

        Card card = cards[topCardIndex++];
        this.runningCount += card.getCountWeight();
        return card;
    }

    /**
     * Converts the internal running count to the precise True Count metric.
     * Crucial for Kelly Criterion precision calculations.
     */
    public double getTrueCount() {
        int cardsRemaining = cards.length - topCardIndex;

        double exactDecksRemaining = cardsRemaining / 52.0;
        double unit = deckEstimation.getRoundingUnit();
        // Floor to the nearest unit — models a player eyeballing the discard tray
        // and rounding up the remaining decks (conservative estimate).
        // With full-deck estimation this systematically underestimates TC,
        // while quarter-deck estimation stays close to the true value.
        double decksRemaining = Math.ceil(exactDecksRemaining / unit) * unit;

        // Floor the minimum decks remaining to 0.5 to mitigate extreme edge spikes at the cut card
        return this.runningCount / Math.max(decksRemaining, 0.5);
    }

    // High-utility diagnostic getters
    public int getRunningCount() {
        return this.runningCount;
    }

    public int getCardsRemaining() {
        return cards.length - topCardIndex;
    }
}