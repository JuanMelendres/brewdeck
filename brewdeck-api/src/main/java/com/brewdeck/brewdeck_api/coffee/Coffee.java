package com.brewdeck.brewdeck_api.coffee;

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

  private String acidity;
  private String body;
  private String sweetness;
  private String bitterness;

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
