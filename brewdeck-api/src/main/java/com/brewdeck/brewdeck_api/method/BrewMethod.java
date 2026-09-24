package com.brewdeck.brewdeck_api.method;

import com.brewdeck.brewdeck_api.auth.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "brew_methods")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrewMethod {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Null for the shared catalog; set for a user's private method. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private User owner;

  // Unique per tier (shared catalog / per owner) via partial indexes in V16.
  @Column(nullable = false, length = 80)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public boolean isShared() {
    return owner == null;
  }

  @PrePersist
  void onCreate() {
    this.createdAt = LocalDateTime.now();
  }
}
