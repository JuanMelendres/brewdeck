package com.brewdeck.brewdeck_api.session;

import com.brewdeck.brewdeck_api.common.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brew-sessions")
@RequiredArgsConstructor
@Tag(name = "Brew Sessions", description = "Manage brew sessions")
public class BrewSessionController {

  private final BrewSessionService brewSessionService;

  @GetMapping
  @Operation(
      summary = "List brew sessions",
      description = "Returns a paginated list of brew sessions.")
  public ResponseEntity<PageResponse<BrewSessionResponse>> findAll(
      @ModelAttribute BrewSessionFilter filter,
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(brewSessionService.search(filter, pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get brew session by id")
  public ResponseEntity<BrewSessionResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(brewSessionService.findById(id));
  }

  @GetMapping("/recipe/{recipeId}")
  @Operation(summary = "List brew sessions by recipe")
  public ResponseEntity<PageResponse<BrewSessionResponse>> findByRecipeId(
      @PathVariable Long recipeId,
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(brewSessionService.findByRecipeId(recipeId, pageable));
  }

  @PostMapping
  @Operation(summary = "Create brew session")
  public ResponseEntity<BrewSessionResponse> create(
      @Valid @RequestBody BrewSessionRequest request) {
    BrewSessionResponse response = brewSessionService.create(request);

    URI location = URI.create("/api/brew-sessions/" + response.id());

    return ResponseEntity.created(location).body(response);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update brew session")
  public ResponseEntity<BrewSessionResponse> update(
      @PathVariable Long id, @Valid @RequestBody BrewSessionRequest request) {
    return ResponseEntity.ok(brewSessionService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete brew session")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    brewSessionService.delete(id);

    return ResponseEntity.noContent().build();
  }
}
