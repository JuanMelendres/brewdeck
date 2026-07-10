package com.brewdeck.brewdeck_api.coffee;

import com.brewdeck.brewdeck_api.auth.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "coffees")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coffee {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private User owner;

  @Column(nullable = false, length = 120)
  private String name;

  private String brand;
  private String origin;
  private String region;
  private String farm;
  private String producer;
  private String variety;
  private String process;

  @Column(name = "roast_level")
  private String roastLevel;

  @Column(name = "notes_primary", columnDefinition = "TEXT")
  private String notesPrimary;

  @Column(name = "notes_secondary", columnDefinition = "TEXT")
  private String notesSecondary;

  @Column(name = "acidity_score")
  private Integer acidityScore;

  @Column(name = "body_score")
  private Integer bodyScore;

  @Column(name = "sweetness_score")
  private Integer sweetnessScore;

  @Column(name = "bitterness_score")
  private Integer bitternessScore;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
