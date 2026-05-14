package com.brewdeck.brewdeck_api.recipe;

import org.springframework.data.jpa.domain.Specification;

public final class RecipeSpecification {

  private RecipeSpecification() {}

  public static Specification<Recipe> hasCoffeeId(Long coffeeId) {
    return (root, query, criteriaBuilder) ->
        coffeeId == null
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.equal(root.get("coffee").get("id"), coffeeId);
  }

  public static Specification<Recipe> hasMethodId(Long methodId) {
    return (root, query, criteriaBuilder) ->
        methodId == null
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.equal(root.get("method").get("id"), methodId);
  }

  public static Specification<Recipe> isFavorite(Boolean favorite) {
    return (root, query, criteriaBuilder) ->
        favorite == null
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.equal(root.get("favorite"), favorite);
  }

  public static Specification<Recipe> nameContains(String name) {
    return (root, query, criteriaBuilder) ->
        name == null || name.isBlank()
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.like(
                criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
  }
}
