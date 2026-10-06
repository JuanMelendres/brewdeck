package com.brewdeck.brewdeck_api.common.error;

/**
 * A delete was refused because other rows still reference the resource (e.g. a coffee used by
 * recipes). Mapped to 409 with a message that says what is in the way, instead of the generic "Data
 * integrity violation" a foreign-key failure would produce. The response text comes from
 * messages*.properties ({@code error.inUse.*}) in the request's language; {@link #getMessage()}
 * stays English for logs.
 */
public class ResourceInUseException extends RuntimeException {

  /** What could not be deleted, with its message key and English wording for logs. */
  public enum Resource {
    COFFEE("error.inUse.coffee", "Coffee", "recipe", "recipes"),
    BREW_METHOD("error.inUse.brewMethod", "Brew method", "recipe", "recipes"),
    RECIPE("error.inUse.recipe", "Recipe", "brew session", "brew sessions");

    private final String messageKey;
    private final String name;
    private final String dependentSingular;
    private final String dependentPlural;

    Resource(String messageKey, String name, String dependentSingular, String dependentPlural) {
      this.messageKey = messageKey;
      this.name = name;
      this.dependentSingular = dependentSingular;
      this.dependentPlural = dependentPlural;
    }
  }

  private final transient Resource resource;
  private final long count;

  private ResourceInUseException(Resource resource, long count) {
    super(englishMessage(resource, count));
    this.resource = resource;
    this.count = count;
  }

  /** "Coffee is used by 1 recipe. Delete or change it first." / "... by 3 recipes ..." */
  public static ResourceInUseException of(Resource resource, long count) {
    return new ResourceInUseException(resource, count);
  }

  public String getMessageKey() {
    return resource.messageKey;
  }

  public long getCount() {
    return count;
  }

  private static String englishMessage(Resource resource, long count) {
    String dependents = count == 1 ? resource.dependentSingular : resource.dependentPlural;
    String pronoun = count == 1 ? "it" : "them";
    return resource.name
        + " is used by "
        + count
        + " "
        + dependents
        + ". Delete or change "
        + pronoun
        + " first.";
  }
}
