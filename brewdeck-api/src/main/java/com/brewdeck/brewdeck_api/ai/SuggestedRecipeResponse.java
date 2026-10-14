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
    String rationale,
    // Provenance (ADR-016): the web app saves these with a recipe built from this suggestion.
    String aiModel,
    String aiPromptVersion) {}
