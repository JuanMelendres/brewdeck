package com.brewdeck.brewdeck_api.session;

import com.brewdeck.brewdeck_api.recipe.Recipe;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "brew_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrewSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipe_id", nullable = false)
  private Recipe recipe;

  @Column(name = "brewed_at", nullable = false)
  private LocalDateTime brewedAt;

  @Column(name = "actual_grind")
  private String actualGrind;

  @Column(name = "actual_temp")
  private Integer actualTemp;

  @Column(name = "actual_time")
  private String actualTime;

  @Column(name = "taste_result", columnDefinition = "TEXT")
  private String tasteResult;

  private Integer rating;

  @Column(name = "adjustment_notes", columnDefinition = "TEXT")
  private String adjustmentNotes;

  @PrePersist
  void onCreate() {
    if (this.brewedAt == null) {
      this.brewedAt = LocalDateTime.now();
    }
  }
}
