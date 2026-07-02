package com.brewdeck.brewdeck_api.dashboard;

public record DashboardSummaryResponse(
    long totalCoffees,
    long totalBrewMethods,
    long totalRecipes,
    long favoriteRecipes,
    long totalBrewSessions,
    Double averageSessionRating) {}
