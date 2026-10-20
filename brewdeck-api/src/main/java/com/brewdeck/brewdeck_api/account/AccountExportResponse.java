package com.brewdeck.brewdeck_api.account;

import com.brewdeck.brewdeck_api.coffee.CoffeeResponse;
import com.brewdeck.brewdeck_api.method.BrewMethodResponse;
import com.brewdeck.brewdeck_api.recipe.RecipeResponse;
import com.brewdeck.brewdeck_api.session.BrewSessionResponse;
import java.time.Instant;
import java.util.List;

/**
 * Everything BrewDeck stores about a user, in the API's own shapes (ADR-020, ARCO access right).
 * Brew methods are only the user's own; the shared catalog is not personal data.
 */
public record AccountExportResponse(
    Instant exportedAt,
    ProfileExport profile,
    List<CoffeeResponse> coffees,
    List<BrewMethodResponse> brewMethods,
    List<RecipeResponse> recipes,
    List<BrewSessionResponse> brewSessions) {}
