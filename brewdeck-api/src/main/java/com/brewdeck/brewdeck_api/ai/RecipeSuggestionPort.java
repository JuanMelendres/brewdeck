package com.brewdeck.brewdeck_api.ai;

public interface RecipeSuggestionPort {
  SuggestedRecipe suggest(SuggestionContext context);
}
