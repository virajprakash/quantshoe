package com.quantshoe.core;

public enum DeckEstimation {
    FULL(1.0),
    HALF(0.5),
    QUARTER(0.25);

    private final double roundingUnit;

    DeckEstimation(double roundingUnit) {
        this.roundingUnit = roundingUnit;
    }

    public double getRoundingUnit() {
        return roundingUnit;
    }
}
