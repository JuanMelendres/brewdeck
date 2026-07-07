package com.brewdeck.brewdeck_api.ai;

import java.math.BigDecimal;
import java.util.List;

public record ImprovementContext(
    String coffeeName,
    String origin,
    String roastLevel,
    String process,
    Integer acidityScore,
    Integer bodyScore,
    Integer sweetnessScore,
    Integer bitternessScore,
    String methodName,
    String methodDescription,
    BigDecimal currentCoffeeGrams,
    BigDecimal currentWaterGrams,
    String currentRatio,
    String currentGrindSetting,
    Integer currentWaterTemp,
    String currentBrewTime,
    String currentSteps,
    List<BrewHistoryEntry> history) {}
