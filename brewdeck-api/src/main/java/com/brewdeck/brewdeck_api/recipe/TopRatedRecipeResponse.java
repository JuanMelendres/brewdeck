package com.brewdeck.brewdeck_api.recipe;

public record TopRatedRecipeResponse(
    Long recipeId, String recipeName, Double averageRating, long totalSessions) {}
