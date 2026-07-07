package com.brewdeck.brewdeck_api.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
@Tag(name = "AI Recipe Improvements", description = "AI-improved brewing parameters from history")
public class RecipeImprovementController {

  private final RecipeImprovementService recipeImprovementService;

  @PostMapping("/{id}/improve")
  @Operation(
      summary = "Improve a recipe from its brew history",
      description =
          "Generates improved AI brewing parameters from the recipe's recent rated brews. The"
              + " result is not persisted; the client uses it to pre-fill the recipe form.")
  public ResponseEntity<SuggestedRecipeResponse> improve(@PathVariable Long id) {
    return ResponseEntity.ok(recipeImprovementService.improve(id));
  }
}
