package com.quantshoe.strategy;

public enum StrategyAction {
        HIT,
        STAND,
        DOUBLE_OR_HIT,
        DOUBLE_OR_STAND,
        SURRENDER_OR_HIT,
        DEVIATE_STAND_IF_ABOVE, // Stand if True Count >= Index, else Hit
        DEVIATE_STAND_IF_BELOW, // Stand if True Count <= Index, else Hit
        DEVIATE_DOUBLE_IF_ABOVE, // Double if True Count >= Index, else Hit
        DEVIATE_DOUBLE_IF_STAND  // Double if True Count >= Index, else Stand
}

