package com.brewdeck.brewdeck_api.session;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brew-sessions")
@RequiredArgsConstructor
public class BrewSessionController {

  private final BrewSessionService brewSessionService;

  @GetMapping
  public List<BrewSessionResponse> findAll() {
    return brewSessionService.findAll();
  }

  @GetMapping("/{id}")
  public BrewSessionResponse findById(@PathVariable Long id) {
    return brewSessionService.findById(id);
  }

  @GetMapping("/recipe/{recipeId}")
  public List<BrewSessionResponse> findByRecipeId(@PathVariable Long recipeId) {
    return brewSessionService.findByRecipeId(recipeId);
  }

  @PostMapping
  public BrewSessionResponse create(@Valid @RequestBody BrewSessionRequest request) {
    return brewSessionService.create(request);
  }

  @PutMapping("/{id}")
  public BrewSessionResponse update(
      @PathVariable Long id, @Valid @RequestBody BrewSessionRequest request) {
    return brewSessionService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    brewSessionService.delete(id);
  }
}
