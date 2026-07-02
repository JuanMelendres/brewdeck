package com.brewdeck.brewdeck_api.method;

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
@RequestMapping("/api/brew-methods")
@RequiredArgsConstructor
@Tag(name = "Brew Methods", description = "Manage brew methods")
public class BrewMethodController {

  private final BrewMethodService brewMethodService;

  @GetMapping
  @Operation(
      summary = "List brew methods",
      description = "Returns a paginated list of brew methods.")
  public ResponseEntity<PageResponse<BrewMethodResponse>> findAll(
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(brewMethodService.findAll(pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get brew method by id")
  public ResponseEntity<BrewMethodResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(brewMethodService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Create brew method")
  public ResponseEntity<BrewMethodResponse> create(@Valid @RequestBody BrewMethodRequest request) {
    BrewMethodResponse response = brewMethodService.create(request);

    URI location = URI.create("/api/brew-methods/" + response.id());

    return ResponseEntity.created(location).body(response);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update brew method")
  public ResponseEntity<BrewMethodResponse> update(
      @PathVariable Long id, @Valid @RequestBody BrewMethodRequest request) {
    return ResponseEntity.ok(brewMethodService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete brew method")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    brewMethodService.delete(id);

    return ResponseEntity.noContent().build();
  }
}
