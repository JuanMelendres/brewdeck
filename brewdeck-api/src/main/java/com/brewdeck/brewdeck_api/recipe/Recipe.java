package com.brewdeck.brewdeck_api.recipe;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "recipes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coffee_id", nullable = false)
  private Coffee coffee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "method_id", nullable = false)
  private BrewMethod method;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "coffee_grams")
  private BigDecimal coffeeGrams;

  @Column(name = "water_grams")
  private BigDecimal waterGrams;

  private String ratio;

  @Column(name = "grind_setting")
  private String grindSetting;

  @Column(name = "water_temp")
  private Integer waterTemp;

  @Column(name = "brew_time")
  private String brewTime;

  @Column(columnDefinition = "TEXT")
  private String steps;

  @Column(name = "expected_taste", columnDefinition = "TEXT")
  private String expectedTaste;

  @Column(nullable = false)
  private Boolean favorite;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    this.createdAt = LocalDateTime.now();
    if (this.favorite == null) {
      this.favorite = false;
    }
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
