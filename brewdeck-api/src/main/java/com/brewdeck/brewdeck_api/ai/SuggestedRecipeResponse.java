package com.brewdeck.brewdeck_api.ai;

import java.math.BigDecimal;

public record SuggestedRecipeResponse(
    BigDecimal coffeeGrams,
    BigDecimal waterGrams,
    String ratio,
    String grindSetting,
    Integer waterTemp,
    String brewTime,
    String steps,
    String rationale) {}
