package com.brewdeck.brewdeck_api.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.common.PostgresRepositoryTest;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RecipeSpecificationRepositoryTest extends PostgresRepositoryTest {

  @Autowired private RecipeRepository recipeRepository;

  @Autowired
  private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

  @Test
  void search_shouldFilterByCoffeeIdMethodIdFavoriteAndName() {
    Coffee coffee = persistCoffee("Mezcla Veracruz");
    Coffee otherCoffee = persistCoffee("Mezcla Chiapas");

    BrewMethod method = persistBrewMethod("AeroPress");
    BrewMethod otherMethod = persistBrewMethod("V60");

    Recipe matchingRecipe =
        Recipe.builder()
            .coffee(coffee)
            .method(method)
            .name("Mezcla Veracruz AeroPress")
            .favorite(true)
            .build();

    Recipe otherRecipe =
        Recipe.builder()
            .coffee(otherCoffee)
            .method(otherMethod)
            .name("Chiapas V60")
            .favorite(false)
            .build();

    entityManager.persist(matchingRecipe);
    entityManager.persist(otherRecipe);
    entityManager.flush();
    entityManager.clear();

    Specification<Recipe> specification =
        RecipeSpecification.hasCoffeeId(coffee.getId())
            .and(RecipeSpecification.hasMethodId(method.getId()))
            .and(RecipeSpecification.isFavorite(true))
            .and(RecipeSpecification.nameContains("AeroPress"));

    List<Recipe> result = recipeRepository.findAll(specification);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getName()).isEqualTo("Mezcla Veracruz AeroPress");
    assertThat(result.getFirst().getCoffee().getId()).isEqualTo(coffee.getId());
    assertThat(result.getFirst().getMethod().getId()).isEqualTo(method.getId());
    assertThat(result.getFirst().getFavorite()).isTrue();
  }

  @Test
  void search_shouldReturnAllRecipes_whenFiltersAreNullOrBlank() {
    Coffee coffee = persistCoffee("Mezcla Veracruz");
    BrewMethod method = persistBrewMethod("AeroPress");

    Recipe recipeOne =
        Recipe.builder().coffee(coffee).method(method).name("Recipe One").favorite(true).build();

    Recipe recipeTwo =
        Recipe.builder().coffee(coffee).method(method).name("Recipe Two").favorite(false).build();

    entityManager.persist(recipeOne);
    entityManager.persist(recipeTwo);
    entityManager.flush();
    entityManager.clear();

    Specification<Recipe> specification =
        RecipeSpecification.hasCoffeeId(null)
            .and(RecipeSpecification.hasMethodId(null))
            .and(RecipeSpecification.isFavorite(null))
            .and(RecipeSpecification.nameContains(""));

    List<Recipe> result = recipeRepository.findAll(specification);

    assertThat(result).extracting(Recipe::getName).contains("Recipe One", "Recipe Two");
  }

  @Test
  void hasOwner_shouldReturnOnlyOwnedRecipes() {
    Coffee coffee = persistCoffee("Mezcla Veracruz");
    BrewMethod method = persistBrewMethod("AeroPress");
    User owner = persistUser("owner@brewdeck.test");
    User other = persistUser("other@brewdeck.test");

    Recipe ownedRecipe =
        Recipe.builder()
            .coffee(coffee)
            .method(method)
            .name("Owned")
            .favorite(false)
            .owner(owner)
            .build();

    Recipe foreignRecipe =
        Recipe.builder()
            .coffee(coffee)
            .method(method)
            .name("Foreign")
            .favorite(false)
            .owner(other)
            .build();

    entityManager.persist(ownedRecipe);
    entityManager.persist(foreignRecipe);
    entityManager.flush();
    entityManager.clear();

    List<Recipe> result = recipeRepository.findAll(RecipeSpecification.hasOwner(owner.getId()));

    assertThat(result).extracting(Recipe::getName).containsExactly("Owned");
  }

  private User persistUser(String email) {
    User user =
        User.builder()
            .email(email)
            .passwordHash("hashed-password")
            .createdAt(LocalDateTime.now())
            .build();

    return entityManager.persistAndFlush(user);
  }

  private Coffee persistCoffee(String name) {
    Coffee coffee =
        Coffee.builder()
            .name(name + " " + System.nanoTime())
            .brand("Café local")
            .origin("Veracruz")
            .roastLevel("Medio")
            .process("Lavado")
            .build();

    return entityManager.persistAndFlush(coffee);
  }

  private BrewMethod persistBrewMethod(String name) {
    BrewMethod method =
        BrewMethod.builder()
            .name("Test Method " + name + " " + System.nanoTime())
            .description("Brew method created for specification tests.")
            .build();

    return entityManager.persistAndFlush(method);
  }
}
