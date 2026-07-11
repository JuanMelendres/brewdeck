package com.brewdeck.brewdeck_api.coffee;

import org.springframework.data.jpa.domain.Specification;

public final class CoffeeSpecification {

  private CoffeeSpecification() {}

  public static Specification<Coffee> hasOrigin(String origin) {
    return (root, query, criteriaBuilder) ->
        origin == null || origin.isBlank()
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("origin")), origin.toLowerCase());
  }

  public static Specification<Coffee> hasRoastLevel(String roastLevel) {
    return (root, query, criteriaBuilder) ->
        roastLevel == null || roastLevel.isBlank()
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("roastLevel")), roastLevel.toLowerCase());
  }

  public static Specification<Coffee> hasProcess(String process) {
    return (root, query, criteriaBuilder) ->
        process == null || process.isBlank()
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("process")), process.toLowerCase());
  }

  public static Specification<Coffee> nameContains(String name) {
    return (root, query, criteriaBuilder) ->
        name == null || name.isBlank()
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.like(
                criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
  }

  public static Specification<Coffee> hasOwner(Long ownerId) {
    return (root, query, criteriaBuilder) ->
        ownerId == null
            ? criteriaBuilder.disjunction()
            : criteriaBuilder.equal(root.get("owner").get("id"), ownerId);
  }
}
