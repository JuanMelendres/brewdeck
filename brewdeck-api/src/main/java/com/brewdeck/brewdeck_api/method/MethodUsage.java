package com.brewdeck.brewdeck_api.method;

/** Aggregated usage row for a single brew method. */
public interface MethodUsage {

  Long getMethodId();

  String getMethodName();

  long getRecipeCount();
}
