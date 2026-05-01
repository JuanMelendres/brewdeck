package com.brewdeck.brewdeck_api.method;

import java.time.LocalDateTime;

public record BrewMethodResponse(
    Long id, String name, String description, LocalDateTime createdAt) {
  public static BrewMethodResponse fromEntity(BrewMethod method) {
    return new BrewMethodResponse(
        method.getId(), method.getName(), method.getDescription(), method.getCreatedAt());
  }
}
