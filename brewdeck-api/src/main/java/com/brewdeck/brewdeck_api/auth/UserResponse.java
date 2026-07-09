package com.brewdeck.brewdeck_api.auth;

import java.time.LocalDateTime;

public record UserResponse(Long id, String email, LocalDateTime createdAt) {
  public static UserResponse fromEntity(User user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt());
  }
}
