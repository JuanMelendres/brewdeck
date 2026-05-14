package com.brewdeck.brewdeck_api.recipe;

public record RecipeFilter(Long coffeeId, Long methodId, Boolean favorite, String name) {}
