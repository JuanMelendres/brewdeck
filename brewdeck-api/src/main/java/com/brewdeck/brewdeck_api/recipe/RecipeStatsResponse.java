package com.brewdeck.brewdeck_api.recipe;

import java.time.Instant;

public record RecipeStatsResponse(
    Long recipeId, long totalSessions, Double averageRating, Instant lastBrewedAt) {}
