package com.brewdeck.brewdeck_api.method;

import java.time.LocalDateTime;

/**
 * A brew method as seen by the current user. {@code shared} is true for the admin-managed catalog
 * (read-only to regular users) and false for the user's own private methods (editable by them).
 */
public record BrewMethodResponse(
    Long id, String name, String description, boolean shared, LocalDateTime createdAt) {
  public static BrewMethodResponse fromEntity(BrewMethod method) {
    return new BrewMethodResponse(
        method.getId(),
        method.getName(),
        method.getDescription(),
        method.isShared(),
        method.getCreatedAt());
  }
}
