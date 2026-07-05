package com.brewdeck.brewdeck_api.session;

import java.time.LocalDateTime;

/** Aggregated brew-session statistics for a single recipe. */
public interface RecipeSessionStats {

  long getTotalSessions();

  Double getAverageRating();

  LocalDateTime getLastBrewedAt();
}
