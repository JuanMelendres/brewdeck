package com.brewdeck.brewdeck_api.session;

/** Aggregated rating ranking row for a single recipe. */
public interface TopRatedRecipe {

  Long getRecipeId();

  String getRecipeName();

  Double getAverageRating();

  long getTotalSessions();
}
