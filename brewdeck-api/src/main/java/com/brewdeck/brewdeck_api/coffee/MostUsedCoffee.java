package com.brewdeck.brewdeck_api.coffee;

/** Aggregated usage row for a single coffee. */
public interface MostUsedCoffee {

  Long getCoffeeId();

  String getCoffeeName();

  long getRecipeCount();
}
