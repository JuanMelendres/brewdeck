package com.brewdeck.brewdeck_api.session;

import org.springframework.data.jpa.domain.Specification;

public final class BrewSessionSpecification {

  private BrewSessionSpecification() {}

  public static Specification<BrewSession> hasRecipeId(Long recipeId) {
    return (root, query, criteriaBuilder) ->
        recipeId == null
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.equal(root.get("recipe").get("id"), recipeId);
  }

  public static Specification<BrewSession> hasRating(Integer rating) {
    return (root, query, criteriaBuilder) ->
        rating == null
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.equal(root.get("rating"), rating);
  }
}
