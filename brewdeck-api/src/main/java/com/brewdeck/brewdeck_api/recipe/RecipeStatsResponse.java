package com.brewdeck.brewdeck_api.recipe;

import java.time.LocalDateTime;

public record RecipeStatsResponse(
    Long recipeId, long totalSessions, Double averageRating, LocalDateTime lastBrewedAt) {}
