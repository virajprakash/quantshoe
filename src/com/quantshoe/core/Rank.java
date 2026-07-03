package com.quantshoe.core;

public enum Rank {
    TWO(2, 1),
    THREE(3, 1),
    FOUR(4, 1),
    FIVE(5, 1),
    SIX(6, 1),
    SEVEN(7, 0),
    EIGHT(8, 0),
    NINE(9, 0),
    TEN(10, -1),
    JACK(10, -1),
    QUEEN(10, -1),
    KING(10, -1),
    ACE(11, -1); // Soft 11 by default, game engine handles soft/hard conversion

    private final int value;
    private final int countWeight;

    Rank(int value, int countWeight) {
        this.value = value;
        this.countWeight = countWeight;
    }

    public int getValue() {
        return this.value;
    }

    public int getCountWeight() {
        return this.countWeight;
    }
}