package com.brewdeck.brewdeck_api.recipe;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/recipes")
@RequiredArgsConstructor
@Tag(name = "Public Recipes", description = "Read-only public recipe access via share token")
public class PublicRecipeController {

  private final RecipeService recipeService;

  @GetMapping("/{token}")
  @Operation(summary = "Get a shared recipe by its public token")
  public ResponseEntity<PublicRecipeResponse> getByToken(@PathVariable String token) {
    return ResponseEntity.ok(recipeService.getByShareToken(token));
  }
}
