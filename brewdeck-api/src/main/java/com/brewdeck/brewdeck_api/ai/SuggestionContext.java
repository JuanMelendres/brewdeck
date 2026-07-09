package com.brewdeck.brewdeck_api.ai;

public record SuggestionContext(
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
    String notes) {}
