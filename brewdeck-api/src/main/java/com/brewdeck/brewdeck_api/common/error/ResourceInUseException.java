package com.brewdeck.brewdeck_api.common.error;

/**
 * A delete was refused because other rows still reference the resource (e.g. a coffee used by
 * recipes). Mapped to 409 with a message that says what is in the way, instead of the generic "Data
 * integrity violation" a foreign-key failure would produce.
 */
public class ResourceInUseException extends RuntimeException {

  public ResourceInUseException(String message) {
    super(message);
  }

  /** "Coffee is used by 1 recipe. Delete or change it first." / "... by 3 recipes ..." */
  public static ResourceInUseException of(
      String resource, long count, String dependentSingular, String dependentPlural) {
    String dependents = count == 1 ? dependentSingular : dependentPlural;
    String pronoun = count == 1 ? "it" : "them";
    return new ResourceInUseException(
        resource
            + " is used by "
            + count
            + " "
            + dependents
            + ". Delete or change "
            + pronoun
            + " first.");
  }
}
