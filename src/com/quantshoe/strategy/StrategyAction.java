package com.quantshoe.strategy;

public enum StrategyAction {
        // --- 1. BASELINE CRITERIA ---
        HIT,
        STAND,
        DOUBLE_OR_HIT,
        DOUBLE_OR_STAND,
        SURRENDER_OR_HIT,

        // --- 2. TRUE COUNT DEVIATION CRITERIA ---
        DEVIATE_STAND_IF_ABOVE,                  // Stand if True Count >= Index, else Hit
        DEVIATE_STAND_IF_BELOW,                  // Stand if True Count <= Index, else Hit
        DEVIATE_DOUBLE_IF_ABOVE,                 // Double if True Count >= Index, else Hit
        DEVIATE_DOUBLE_IF_STAND,                 // Double if True Count >= Index, else Stand
        DEVIATE_SURRENDER_IF_TRUE_COUNT_ABOVE,   // Surrender if True Count >= Index, else Hit
        DEVIATE_SURRENDER_IF_TRUE_COUNT_BELOW,   // Surrender if True Count <= Index, else Hit

        // --- 3. RUNNING COUNT DEVIATION CRITERIA ---
        DEVIATE_SURRENDER_IF_RUNNING_COUNT_ABOVE, // Surrender if Running Count >= Index, else Hit/Stand
        DEVIATE_SURRENDER_IF_RUNNING_COUNT_BELOW,  // Surrender if Running Count <= Index, else Hit/Stand
        DEVIATE_STAND_IF_RUNNING_ABOVE_ZERO, // Stand if Running count >= Zero, else Hit
        DEVIATE_STAND_IF_RUNNING_BELOW_ZERO,                  // Stand if True Count <= Zero, else Hit

        //Overlap cases
        SURRENDER_16_VS_10_RUNNING_COUNT,
        SURRENDER_16_VS_9_ABOVE_MINUS_1_ELSE_STAND_ABOVE_4,
        SURRENDER_16_VS_8_ABOVE_4,
        SURRENDER_16_VS_ACE_ELSE_STAND_ABOVE_3,
        SURRENDER_15_VS_ACE_ABOVE_MINUS_1_ELSE_STAND_ABOVE_5,
        SURRENDER_15_VS_10_WHEN_RUNNING_NON_NEGATIVE_ELSE_STAND_ABOVE_4,
        SURRENDER_15_VS_9_ABOVE_2_ELSE_HIT,

}