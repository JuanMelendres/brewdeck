package com.brewdeck.brewdeck_api.auth.emailchange;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/** A pending, confirmed or cancelled email change (ADR-019). Tokens are stored as hashes only. */
@Entity
@Table(name = "email_change_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailChange {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "new_email", nullable = false)
  private String newEmail;

  @Column(name = "confirm_token_hash", nullable = false, unique = true, length = 64)
  private String confirmTokenHash;

  @Column(name = "cancel_token_hash", nullable = false, unique = true, length = 64)
  private String cancelTokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
