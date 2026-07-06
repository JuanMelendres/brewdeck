package com.brewdeck.brewdeck_api.session;

/** Aggregated brew-activity ranking row for a single recipe. */
public interface MostBrewedRecipe {

  Long getRecipeId();

  String getRecipeName();

  long getTotalSessions();
}
