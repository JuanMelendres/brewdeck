package com.brewdeck.brewdeck_api.method;

import com.brewdeck.brewdeck_api.common.pagination.PageResponse;
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
public class BrewMethodController {

  private final BrewMethodService brewMethodService;

  @GetMapping
  public ResponseEntity<PageResponse<BrewMethodResponse>> findAll(
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(brewMethodService.findAll(pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<BrewMethodResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(brewMethodService.findById(id));
  }

  @PostMapping
  public ResponseEntity<BrewMethodResponse> create(@Valid @RequestBody BrewMethodRequest request) {
    BrewMethodResponse response = brewMethodService.create(request);

    URI location = URI.create("/api/brew-methods/" + response.id());

    return ResponseEntity.created(location).body(response);
  }

  @PutMapping("/{id}")
  public ResponseEntity<BrewMethodResponse> update(
      @PathVariable Long id, @Valid @RequestBody BrewMethodRequest request) {
    return ResponseEntity.ok(brewMethodService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    brewMethodService.delete(id);

    return ResponseEntity.noContent().build();
  }
}
