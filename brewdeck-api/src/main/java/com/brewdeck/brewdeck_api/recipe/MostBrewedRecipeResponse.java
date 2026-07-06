package com.brewdeck.brewdeck_api.recipe;

public record MostBrewedRecipeResponse(Long recipeId, String recipeName, long totalSessions) {}
