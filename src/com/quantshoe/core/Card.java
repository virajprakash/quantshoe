package com.quantshoe.core;

public final class Card {
    private final Rank rank;

    public Card(Rank rank) {
        if (rank == null) {
            throw new IllegalArgumentException("Card rank cannot be null");
        }
        this.rank = rank;
    }

    public Rank getRank() {
        return this.rank;
    }

    // Direct delegation shortcuts for faster math processing in your engine loops
    public int getValue() {
        return this.rank.getValue();
    }

    public int getCountWeight() {
        return this.rank.getCountWeight();
    }

    @Override
    public String toString() {
        return this.rank.name();
    }
}