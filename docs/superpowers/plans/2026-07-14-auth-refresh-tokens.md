# Auth Refresh Tokens Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add short-lived access JWTs plus long-lived, single-use rotating refresh tokens with reuse detection, giving real logout/revocation and a seamless silent re-auth on the frontend.

**Architecture:** New backend package `auth/refresh/` mirroring `auth/reset/`. Refresh tokens are opaque 256-bit values stored only as SHA-256 hashes (Flyway V11), single-use with rotation; presenting an already-used/revoked token revokes every active token for that user. `login`/`register` return both tokens; public `POST /api/auth/refresh` rotates; authenticated `POST /api/auth/logout` revokes. Frontend stores both tokens in `localStorage` and transparently refreshes on a 401 via a single-flight guard, retrying the original request once.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, Flyway, PostgreSQL, JUnit 5 + Mockito + MockMvc + Testcontainers; Next.js 15 + React 19 + TypeScript, Vitest + React Testing Library.

## Global Constraints

- Refresh token: 256-bit random, base64url; **only** its lowercase hex SHA-256 is persisted. Reuse `com.brewdeck.brewdeck_api.common.security.SecureTokens` (`newToken()`, `sha256Hex(raw)`) — do not reimplement.
- Access-token TTL → **`PT15M`**; refresh-token TTL → **`P7D`** (7 days). Both env-overridable via `application.yaml`.
- Reuse detection revokes **all active** refresh tokens for the user (active = `used_at IS NULL AND revoked_at IS NULL AND expires_at > now`).
- Logout revokes **only** the presented token, scoped to the authenticated user's id.
- `InvalidRefreshTokenException` → HTTP **401**, single generic message (no not-found/used/expired oracle).
- `/api/auth/refresh` is `permitAll`; `/api/auth/logout` stays authenticated. The terminal `.anyRequest().authenticated()` in `SecurityConfig` must remain intact.
- Timestamps use `java.time.LocalDateTime` (match `PasswordResetToken`/`EmailVerificationToken`).
- Backend package base: `com.brewdeck.brewdeck_api`. Frontend uses the `@/` import alias.
- Verification gate per task — Backend: `./mvnw spotless:apply && ./mvnw clean verify && sh mvnw pmd:check` (run from `brewdeck-api/`). Frontend: `pnpm test && pnpm type-check && pnpm build && pnpm lint:fix -- <changed files>` (run from `brewdeck-web/`).
- Conventional Commits; scopes `api`/`web`; sign-off trailers per repo convention are added by the commit tooling.

## File Structure

**Backend (create)** under `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/refresh/`:
- `RefreshToken.java` — JPA entity
- `RefreshTokenRepository.java` — repo (`findByTokenHash`, `revokeAllActiveForUser`)
- `RefreshTokenService.java` — issue / rotate / revoke + `RotationResult`
- `InvalidRefreshTokenException.java` — RuntimeException
- `RefreshRequest.java` — `record RefreshRequest(@NotBlank String refreshToken)`

**Backend (create, migration):** `brewdeck-api/src/main/resources/db/migration/V11__create_refresh_tokens.sql`

**Backend (modify):**
- `auth/AuthResponse.java` — add `refreshToken` field
- `auth/AuthService.java` — inject `RefreshTokenService`; issue on register/login; add `refresh`, `logout`
- `auth/AuthController.java` — add `POST /refresh`, `POST /logout`
- `common/error/GlobalExceptionHandler.java` — map `InvalidRefreshTokenException` → 401
- `common/config/SecurityConfig.java` — permitAll `/api/auth/refresh`
- `src/main/resources/application.yaml` — `token-ttl` → `PT15M`; add `refresh-ttl`

**Backend (create, tests):** `RefreshTokenServiceTest`, `RefreshTokenRepositoryTest` under `src/test/.../auth/refresh/`; modify `auth/AuthControllerTest`; extend the auth integration test.

**Frontend (modify)** under `brewdeck-web/src/`:
- `lib/auth/tokenStore.ts` — refresh-token key, `clearTokens()`
- `lib/api/types.ts` — `AuthResponse.refreshToken`
- `lib/api/auth.ts` — `refresh`, `logout`
- `lib/api/client.ts` — single-flight refresh on 401
- `lib/auth/AuthProvider.tsx` — store refresh token; async `logout`

**Frontend (modify, tests):** `lib/api/client.test.ts`, `lib/api/auth.test.ts`, `lib/auth/AuthProvider.test.tsx`.

**Docs (modify):** `.env.example`, `docs/api/README.md`, `docs/api/openapi.yaml`, `docs/api/postman/brewdeck.postman_collection.json`, `.claude/project-state.md`, `.claude/roadmap.md`.

---

## Task 1: Persistence — migration, entity, repository

**Files:**
- Create: `brewdeck-api/src/main/resources/db/migration/V11__create_refresh_tokens.sql`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/refresh/RefreshToken.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/refresh/RefreshTokenRepository.java`
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/refresh/RefreshTokenRepositoryTest.java`

**Interfaces:**
- Produces:
  - `RefreshToken` entity — Lombok `@Getter/@Setter/@Builder/@NoArgsConstructor/@AllArgsConstructor`; fields `Long id`, `Long userId`, `String tokenHash`, `LocalDateTime expiresAt`, `LocalDateTime usedAt`, `LocalDateTime revokedAt`, `LocalDateTime createdAt`.
  - `RefreshTokenRepository extends JpaRepository<RefreshToken, Long>` with `Optional<RefreshToken> findByTokenHash(String tokenHash)` and `int revokeAllActiveForUser(Long userId, LocalDateTime now)`.

- [ ] **Step 1: Write the migration**

Create `V11__create_refresh_tokens.sql`:

```sql
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    used_at     TIMESTAMP,
    revoked_at  TIMESTAMP,
    created_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
```

- [ ] **Step 2: Write the entity**

Create `RefreshToken.java`:

```java
package com.brewdeck.brewdeck_api.auth.refresh;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "used_at")
  private LocalDateTime usedAt;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
```

- [ ] **Step 3: Write the repository**

Create `RefreshTokenRepository.java`:

```java
package com.brewdeck.brewdeck_api.auth.refresh;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /** Revokes every still-active refresh token for the user. Returns the number of rows updated. */
  @Modifying
  @Query(
      "UPDATE RefreshToken t SET t.revokedAt = :now "
          + "WHERE t.userId = :userId AND t.usedAt IS NULL "
          + "AND t.revokedAt IS NULL AND t.expiresAt > :now")
  int revokeAllActiveForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
```

- [ ] **Step 4: Write the failing repository test**

Create `RefreshTokenRepositoryTest.java`. It follows the existing `@DataJpaTest` pattern (real Postgres via the project's test datasource; check a sibling repo test such as `PasswordResetTokenRepositoryTest` for the exact annotations — use `@DataJpaTest` with `@AutoConfigureTestDatabase(replace = NONE)` if the siblings do, plus `@Testcontainers`/container config from the shared test base). Persist a user first (via `UserRepository`) so the FK holds.

```java
package com.brewdeck.brewdeck_api.auth.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
// Class-level annotations: mirror the sibling reset/verification repository test
// (@DataJpaTest + @AutoConfigureTestDatabase(replace = NONE) + Testcontainers config).

class RefreshTokenRepositoryTest {

  // @Autowired RefreshTokenRepository refreshTokenRepository;
  // @Autowired UserRepository userRepository;

  private Long persistUser() {
    User user =
        User.builder()
            .email("refresh-repo-" + System.nanoTime() + "@example.com")
            .passwordHash("x")
            .createdAt(LocalDateTime.now())
            .build();
    return userRepository.save(user).getId();
  }

  private RefreshToken persistToken(Long userId, String hash, LocalDateTime expiresAt) {
    return refreshTokenRepository.save(
        RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash)
            .expiresAt(expiresAt)
            .createdAt(LocalDateTime.now())
            .build());
  }

  @Test
  void findByTokenHashReturnsTheStoredToken() {
    Long userId = persistUser();
    persistToken(userId, "hash-a", LocalDateTime.now().plusDays(7));

    assertThat(refreshTokenRepository.findByTokenHash("hash-a")).isPresent();
    assertThat(refreshTokenRepository.findByTokenHash("missing")).isEmpty();
  }

  @Test
  void revokeAllActiveForUserRevokesOnlyActiveRowsOfThatUser() {
    Long userId = persistUser();
    Long otherUserId = persistUser();
    LocalDateTime now = LocalDateTime.now();

    RefreshToken active = persistToken(userId, "active", now.plusDays(7));
    RefreshToken othersActive = persistToken(otherUserId, "other-active", now.plusDays(7));

    int updated = refreshTokenRepository.revokeAllActiveForUser(userId, now);

    assertThat(updated).isEqualTo(1);
    // Re-read to see the flushed update.
    assertThat(refreshTokenRepository.findByTokenHash("active").orElseThrow().getRevokedAt())
        .isNotNull();
    assertThat(refreshTokenRepository.findByTokenHash("other-active").orElseThrow().getRevokedAt())
        .isNull();
  }
}
```

> Note: a `@Modifying` bulk update does not flush the persistence context automatically for entities already loaded. Re-fetch via `findByTokenHash` after the update (as above) rather than reusing the in-memory reference. If the sibling repo tests inject `TestEntityManager`, call `entityManager.clear()` before re-reading.

- [ ] **Step 5: Run the repository test — expect FAIL, then PASS**

Run: `cd brewdeck-api && ./mvnw -Dtest=RefreshTokenRepositoryTest test`
First run before wiring annotations may fail to compile; once the class-level annotations match the sibling test, Expected: PASS (2 tests).

- [ ] **Step 6: Verify migration + full build**

Run: `cd brewdeck-api && ./mvnw spotless:apply && ./mvnw clean verify && sh mvnw pmd:check`
Expected: BUILD SUCCESS; Flyway applies V11 during the integration tests.

- [ ] **Step 7: Commit**

```bash
git add brewdeck-api/src/main/resources/db/migration/V11__create_refresh_tokens.sql \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/refresh/RefreshToken.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/refresh/RefreshTokenRepository.java \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/refresh/RefreshTokenRepositoryTest.java
git commit -m "feat(api): add refresh_tokens table, entity, and repository"
```

---

## Task 2: RefreshTokenService + exception + config

**Files:**
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/refresh/InvalidRefreshTokenException.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/refresh/RefreshTokenService.java`
- Modify: `brewdeck-api/src/main/resources/application.yaml` (lines 48-50, `brewdeck.auth`)
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/refresh/RefreshTokenServiceTest.java`

**Interfaces:**
- Consumes: `RefreshToken`, `RefreshTokenRepository` (Task 1); `User`, `UserRepository`; `SecureTokens`.
- Produces:
  - `class InvalidRefreshTokenException extends RuntimeException` (single `String` ctor).
  - `RefreshTokenService` with:
    - `String issue(User user)`
    - `RotationResult rotate(String rawToken)`
    - `void revoke(String rawToken, Long userId)`
  - `record RotationResult(User user, String rawToken)` (public static nested in `RefreshTokenService`).

- [ ] **Step 1: Write the exception**

Create `InvalidRefreshTokenException.java`:

```java
package com.brewdeck.brewdeck_api.auth.refresh;

public class InvalidRefreshTokenException extends RuntimeException {
  public InvalidRefreshTokenException(String message) {
    super(message);
  }
}
```

- [ ] **Step 2: Add config**

In `application.yaml`, edit the `brewdeck.auth` block (currently lines 48-50):

```yaml
  auth:
    secret: ${BREWDECK_JWT_SECRET:dev-only-insecure-secret-change-me-at-least-32-bytes}
    token-ttl: ${AUTH_TOKEN_TTL:PT15M}
    refresh-ttl: ${AUTH_REFRESH_TTL:P7D}
```

(Only `token-ttl` value changes from `PT24H` to `PT15M`; `refresh-ttl` is new.)

- [ ] **Step 3: Write the service**

Create `RefreshTokenService.java`:

```java
package com.brewdeck.brewdeck_api.auth.refresh;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.security.SecureTokens;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class RefreshTokenService {

  private final RefreshTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final Duration refreshTtl;

  public RefreshTokenService(
      RefreshTokenRepository tokenRepository,
      UserRepository userRepository,
      @Value("${brewdeck.auth.refresh-ttl}") Duration refreshTtl) {
    this.tokenRepository = tokenRepository;
    this.userRepository = userRepository;
    this.refreshTtl = refreshTtl;
  }

  /** Issues a fresh refresh token for the user and returns the raw (unhashed) value. */
  @Transactional
  public String issue(User user) {
    return issueInternal(user, LocalDateTime.now());
  }

  /**
   * Rotates a refresh token: validates it, marks it used, and issues a replacement. Presenting an
   * already-used or revoked token is treated as theft and revokes every active token for that user.
   *
   * <p>{@code noRollbackFor} is essential: the reuse path revokes the user's active tokens and then
   * throws. Without it the throw would roll back the very revocation we rely on for containment.
   */
  @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
  public RotationResult rotate(String rawToken) {
    RefreshToken token =
        tokenRepository
            .findByTokenHash(SecureTokens.sha256Hex(rawToken))
            .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));

    LocalDateTime now = LocalDateTime.now();

    if (token.getUsedAt() != null || token.getRevokedAt() != null) {
      tokenRepository.revokeAllActiveForUser(token.getUserId(), now);
      log.warn(
          "Refresh token reuse detected for user id={}; revoked all active tokens",
          token.getUserId());
      throw new InvalidRefreshTokenException("Refresh token already used");
    }

    if (token.getExpiresAt().isBefore(now)) {
      throw new InvalidRefreshTokenException("Refresh token expired");
    }

    User user =
        userRepository
            .findById(token.getUserId())
            .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token has no user"));

    token.setUsedAt(now);
    tokenRepository.save(token);

    String newRawToken = issueInternal(user, now);
    return new RotationResult(user, newRawToken);
  }

  /** Revokes the presented token if it belongs to the user and is still active. Idempotent. */
  @Transactional
  public void revoke(String rawToken, Long userId) {
    tokenRepository
        .findByTokenHash(SecureTokens.sha256Hex(rawToken))
        .filter(
            t ->
                t.getUserId().equals(userId)
                    && t.getUsedAt() == null
                    && t.getRevokedAt() == null)
        .ifPresent(
            t -> {
              t.setRevokedAt(LocalDateTime.now());
              tokenRepository.save(t);
              log.info("Refresh token revoked for user id={}", userId);
            });
  }

  private String issueInternal(User user, LocalDateTime now) {
    String rawToken = SecureTokens.newToken();
    tokenRepository.save(
        RefreshToken.builder()
            .userId(user.getId())
            .tokenHash(SecureTokens.sha256Hex(rawToken))
            .expiresAt(now.plus(refreshTtl))
            .createdAt(now)
            .build());
    return rawToken;
  }

  public record RotationResult(User user, String rawToken) {}
}
```

- [ ] **Step 4: Write the failing service test**

Create `RefreshTokenServiceTest.java` (Mockito, no Spring context; TTL passed via constructor):

```java
package com.brewdeck.brewdeck_api.auth.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.security.SecureTokens;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private RefreshTokenRepository tokenRepository;
  @Mock private UserRepository userRepository;

  private RefreshTokenService service;

  private final User user = User.builder().id(7L).email("u@example.com").build();

  @BeforeEach
  void setUp() {
    service = new RefreshTokenService(tokenRepository, userRepository, Duration.ofDays(7));
    when(tokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  private RefreshToken activeToken(String rawToken) {
    return RefreshToken.builder()
        .id(1L)
        .userId(user.getId())
        .tokenHash(SecureTokens.sha256Hex(rawToken))
        .expiresAt(LocalDateTime.now().plusDays(7))
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  void issuePersistsHashedTokenAndReturnsRawValue() {
    String raw = service.issue(user);

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(tokenRepository).save(captor.capture());
    assertThat(captor.getValue().getTokenHash()).isEqualTo(SecureTokens.sha256Hex(raw));
    assertThat(captor.getValue().getUserId()).isEqualTo(7L);
    assertThat(raw).isNotBlank();
  }

  @Test
  void rotateMarksOldUsedAndIssuesNewToken() {
    String raw = "raw-valid";
    RefreshToken stored = activeToken(raw);
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(stored));
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));

    RefreshTokenService.RotationResult result = service.rotate(raw);

    assertThat(stored.getUsedAt()).isNotNull();
    assertThat(result.user()).isEqualTo(user);
    assertThat(result.rawToken()).isNotBlank().isNotEqualTo(raw);
    // saved twice: the rotated (used) token + the new one.
    verify(tokenRepository, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
  }

  @Test
  void rotateOnUsedTokenRevokesAllActiveAndThrows() {
    String raw = "raw-used";
    RefreshToken used = activeToken(raw);
    used.setUsedAt(LocalDateTime.now().minusMinutes(1));
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(used));

    assertThatThrownBy(() -> service.rotate(raw))
        .isInstanceOf(InvalidRefreshTokenException.class);
    verify(tokenRepository).revokeAllActiveForUser(eq(7L), any(LocalDateTime.class));
    verify(userRepository, never()).findById(any());
  }

  @Test
  void rotateOnExpiredTokenThrowsWithoutRevokingAll() {
    String raw = "raw-expired";
    RefreshToken expired = activeToken(raw);
    expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(expired));

    assertThatThrownBy(() -> service.rotate(raw))
        .isInstanceOf(InvalidRefreshTokenException.class);
    verify(tokenRepository, never()).revokeAllActiveForUser(any(), any());
  }

  @Test
  void rotateOnUnknownTokenThrows() {
    when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.rotate("nope"))
        .isInstanceOf(InvalidRefreshTokenException.class);
    verify(tokenRepository, never()).revokeAllActiveForUser(any(), any());
  }

  @Test
  void revokeSetsRevokedAtWhenOwnedAndActive() {
    String raw = "raw-logout";
    RefreshToken stored = activeToken(raw);
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(stored));

    service.revoke(raw, 7L);

    assertThat(stored.getRevokedAt()).isNotNull();
    verify(tokenRepository).save(stored);
  }

  @Test
  void revokeIsNoOpWhenTokenBelongsToAnotherUser() {
    String raw = "raw-other";
    RefreshToken stored = activeToken(raw);
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(stored));

    service.revoke(raw, 999L);

    assertThat(stored.getRevokedAt()).isNull();
    verify(tokenRepository, never()).save(any());
  }

  @Test
  void revokeIsNoOpWhenTokenUnknown() {
    when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

    service.revoke("missing", 7L);

    verify(tokenRepository, never()).save(any());
  }
}
```

- [ ] **Step 5: Run the service test**

Run: `cd brewdeck-api && ./mvnw -Dtest=RefreshTokenServiceTest test`
Expected: PASS (8 tests).

- [ ] **Step 6: Commit**

```bash
git add brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/refresh/InvalidRefreshTokenException.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/refresh/RefreshTokenService.java \
  brewdeck-api/src/main/resources/application.yaml \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/refresh/RefreshTokenServiceTest.java
git commit -m "feat(api): add RefreshTokenService with rotation and reuse detection"
```

---

## Task 3: Endpoints — AuthResponse, AuthService wiring, controller, security, exception mapping

**Files:**
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/refresh/RefreshRequest.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/AuthResponse.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/AuthService.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/AuthController.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/error/GlobalExceptionHandler.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/config/SecurityConfig.java`
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/AuthControllerTest.java`

**Interfaces:**
- Consumes: `RefreshTokenService` (`issue`, `rotate`, `revoke`, `RotationResult`), `InvalidRefreshTokenException` (Task 2).
- Produces:
  - `record RefreshRequest(@NotBlank String refreshToken)`
  - `record AuthResponse(String token, Instant expiresAt, String email, String refreshToken)`
  - `AuthService.refresh(RefreshRequest)` → `AuthResponse`; `AuthService.logout(String email, RefreshRequest)` → `void`.
  - Endpoints `POST /api/auth/refresh` (200), `POST /api/auth/logout` (204).

- [ ] **Step 1: Add `RefreshRequest`**

```java
package com.brewdeck.brewdeck_api.auth.refresh;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {}
```

- [ ] **Step 2: Extend `AuthResponse`**

```java
package com.brewdeck.brewdeck_api.auth;

import java.time.Instant;

public record AuthResponse(String token, Instant expiresAt, String email, String refreshToken) {}
```

- [ ] **Step 3: Wire `AuthService`**

Inject `RefreshTokenService`; issue a refresh token on register/login; add `refresh` and `logout`. Edit `AuthService.java`:

Add the field + constructor param:

```java
  private final RefreshTokenService refreshTokenService;

  public AuthService(
      UserRepository userRepository,
      JwtService jwtService,
      PasswordEncoder passwordEncoder,
      EmailVerificationService emailVerificationService,
      RefreshTokenService refreshTokenService) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
    this.emailVerificationService = emailVerificationService;
    this.refreshTokenService = refreshTokenService;
  }
```

Add the import: `import com.brewdeck.brewdeck_api.auth.refresh.RefreshRequest;` and `import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenService;`.

Change `register`'s final line from `return tokenResponse(saved);` to:

```java
    return tokenResponse(saved, refreshTokenService.issue(saved));
```

Change `login`'s final line from `return tokenResponse(user);` to:

```java
    return tokenResponse(user, refreshTokenService.issue(user));
```

Replace the `tokenResponse` helper:

```java
  private AuthResponse tokenResponse(User user, String refreshToken) {
    String token = jwtService.generateToken(user);
    return new AuthResponse(token, jwtService.expiryFor(Instant.now()), user.getEmail(), refreshToken);
  }
```

Add the two new methods (after `changePassword`):

```java
  @Transactional
  public AuthResponse refresh(RefreshRequest request) {
    RefreshTokenService.RotationResult result = refreshTokenService.rotate(request.refreshToken());
    return tokenResponse(result.user(), result.rawToken());
  }

  @Transactional
  public void logout(String email, RefreshRequest request) {
    User user = requireByEmail(email);
    refreshTokenService.revoke(request.refreshToken(), user.getId());
  }
```

> Note: `login` is currently `@Transactional(readOnly = true)`. Issuing a refresh token performs a write. Change `login`'s annotation to `@Transactional` (drop `readOnly = true`).

- [ ] **Step 4: Add controller endpoints**

In `AuthController.java`, add the import `import com.brewdeck.brewdeck_api.auth.refresh.RefreshRequest;` and these methods:

```java
  @PostMapping("/refresh")
  @Operation(summary = "Exchange a refresh token for a new access + refresh token pair")
  public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return authService.refresh(request);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Revoke the presented refresh token")
  public void logout(Principal principal, @Valid @RequestBody RefreshRequest request) {
    authService.logout(principal.getName(), request);
  }
```

- [ ] **Step 5: Map the exception → 401**

In `GlobalExceptionHandler.java`, add the import `import com.brewdeck.brewdeck_api.auth.refresh.InvalidRefreshTokenException;` and this handler (place beside `handleBadCredentials`):

```java
  @ExceptionHandler(InvalidRefreshTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
      InvalidRefreshTokenException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "Refresh token is invalid or has expired",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }
```

- [ ] **Step 6: permitAll `/api/auth/refresh`**

In `SecurityConfig.java`, add `"/api/auth/refresh"` to the first `.requestMatchers(...)` permitAll group (alongside `register`, `login`, `forgot-password`, `reset-password`, `verify-email`). Do **not** add `/api/auth/logout`. Leave `.anyRequest().authenticated()` unchanged.

```java
                    .requestMatchers(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        "/api/auth/verify-email")
                    .permitAll()
```

- [ ] **Step 7: Write the failing controller tests**

In `AuthControllerTest.java`, add a `@Mock RefreshTokenService` is **not** needed (the controller depends on `AuthService` only). Add these tests (the `@BeforeEach` already builds `standaloneSetup(new AuthController(authService))` with the real `GlobalExceptionHandler`). Add `import com.brewdeck.brewdeck_api.auth.refresh.InvalidRefreshTokenException;` and `import com.brewdeck.brewdeck_api.auth.refresh.RefreshRequest;`:

```java
  @Test
  void refreshReturns200WithNewPair() throws Exception {
    when(authService.refresh(any(RefreshRequest.class)))
        .thenReturn(new AuthResponse("new-jwt", Instant.parse("2026-07-14T00:15:00Z"),
            "u@example.com", "new-refresh"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType("application/json")
                .content("{\"refreshToken\":\"old-refresh\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("new-jwt"))
        .andExpect(jsonPath("$.refreshToken").value("new-refresh"))
        .andExpect(jsonPath("$.email").value("u@example.com"));
  }

  @Test
  void refreshReturns401OnInvalidToken() throws Exception {
    when(authService.refresh(any(RefreshRequest.class)))
        .thenThrow(new InvalidRefreshTokenException("bad"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType("application/json")
                .content("{\"refreshToken\":\"bad\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Refresh token is invalid or has expired"));
  }

  @Test
  void refreshReturns400OnBlankToken() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType("application/json")
                .content("{\"refreshToken\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void logoutReturns204() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/logout")
                .principal(() -> "u@example.com")
                .contentType("application/json")
                .content("{\"refreshToken\":\"some-refresh\"}"))
        .andExpect(status().isNoContent());

    verify(authService).logout(eq("u@example.com"), any(RefreshRequest.class));
  }
```

Also update any existing `register`/`login` controller test that asserts the JSON body to include `.andExpect(jsonPath("$.refreshToken")...)` if it constructs an `AuthResponse` — the added constructor arg means existing `new AuthResponse(...)` calls in the test must pass a 4th argument. Search the test for `new AuthResponse(` and add a refresh-token value.

> `verify` and `eq` are already imported in this test file. Add `import static org.mockito.Mockito.verify;` only if absent.

- [ ] **Step 8: Fix other `new AuthResponse(` call sites**

Run: `cd brewdeck-api && grep -rn "new AuthResponse(" src`
For every match (production `AuthService.tokenResponse` is already updated; check tests and any mapper), ensure a 4th `refreshToken` argument is supplied. Expected remaining call sites are in tests only.

- [ ] **Step 9: Run controller tests + full verify**

Run: `cd brewdeck-api && ./mvnw -Dtest=AuthControllerTest test`
Expected: PASS.
Then: `./mvnw spotless:apply && ./mvnw clean verify && sh mvnw pmd:check`
Expected: BUILD SUCCESS.

- [ ] **Step 10: Commit**

```bash
git add brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/refresh/RefreshRequest.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/AuthResponse.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/AuthService.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/AuthController.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/error/GlobalExceptionHandler.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/config/SecurityConfig.java \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/AuthControllerTest.java
git commit -m "feat(api): add refresh and logout endpoints returning a rotated token pair"
```

---

## Task 4: Backend integration test (full rotation + reuse + logout flow)

**Files:**
- Modify: the auth security integration test (`brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/**/AuthSecurityIntegrationTest.java` — confirm exact path with `find src/test -name 'AuthSecurityIntegrationTest.java'`).

**Interfaces:**
- Consumes: the live endpoints from Task 3 through MockMvc/Testcontainers.

- [ ] **Step 1: Locate the integration test and its helpers**

Run: `cd brewdeck-api && find src/test -name 'AuthSecurityIntegrationTest.java'`
Read it to reuse its existing register/login helpers, `ObjectMapper`, and MockMvc setup (it already exercises register → PATCH → change-password flows).

- [ ] **Step 2: Add the rotation + reuse + logout flow test**

Add one `@Test` that, using the existing helpers, performs this sequence. Adapt the helper names to those already in the file (e.g. a `register(...)`/`login(...)` helper that returns the parsed `AuthResponse` JSON or a token string). Extract the `token` and `refreshToken` from response JSON via the file's existing `ObjectMapper`/`JsonNode` approach.

```java
  @Test
  void refreshRotatesAndReuseOfOldTokenRevokesTheChain() throws Exception {
    // register a fresh user and capture both tokens from the response body
    String email = "refresh-flow-" + System.nanoTime() + "@example.com";
    JsonNode registered = registerAndRead(email, "password123"); // helper: POST /register, parse body
    String refresh1 = registered.get("refreshToken").asText();

    // 1) rotate: refresh1 -> (access2, refresh2)
    JsonNode rotated = refreshAndRead(refresh1); // helper: POST /api/auth/refresh {refreshToken}
    String refresh2 = rotated.get("refreshToken").asText();
    assertThat(refresh2).isNotEqualTo(refresh1);

    // 2) the OLD refresh (refresh1) is now used -> presenting it again is 401 (reuse)
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refresh1))))
        .andExpect(status().isUnauthorized());

    // 3) reuse revoked the whole active set, so refresh2 (issued during the rotation) is dead too
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refresh2))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutRevokesThePresentedRefreshToken() throws Exception {
    String email = "logout-flow-" + System.nanoTime() + "@example.com";
    JsonNode registered = registerAndRead(email, "password123");
    String access = registered.get("token").asText();
    String refresh = registered.get("refreshToken").asText();

    // logout requires a valid access token (authenticated endpoint)
    mockMvc
        .perform(
            post("/api/auth/logout")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refresh))))
        .andExpect(status().isNoContent());

    // the logged-out refresh token can no longer be rotated
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refresh))))
        .andExpect(status().isUnauthorized());
  }
```

Add imports as needed: `com.brewdeck.brewdeck_api.auth.refresh.RefreshRequest`, `com.fasterxml.jackson.databind.JsonNode`, `org.springframework.http.MediaType`, `static org.assertj.core.api.Assertions.assertThat`. If the file lacks `registerAndRead`/`refreshAndRead` helpers, write them inline in the test class following the existing register/login helper style already present in the file.

- [ ] **Step 3: Run + full verify**

Run: `cd brewdeck-api && ./mvnw -Dtest=AuthSecurityIntegrationTest test`
Expected: PASS (the reuse case confirms `noRollbackFor` persisted the revocation).
Then: `./mvnw spotless:apply && ./mvnw clean verify && sh mvnw pmd:check`
Expected: BUILD SUCCESS, full suite green.

- [ ] **Step 4: Commit**

```bash
git add brewdeck-api/src/test
git commit -m "test(api): cover refresh rotation, reuse revocation, and logout flow"
```

---

## Task 5: Frontend token store + auth client + types

**Files:**
- Modify: `brewdeck-web/src/lib/auth/tokenStore.ts`
- Modify: `brewdeck-web/src/lib/api/types.ts` (lines 135-139, `AuthResponse`)
- Modify: `brewdeck-web/src/lib/api/auth.ts`
- Test: `brewdeck-web/src/lib/auth/tokenStore.test.ts`, `brewdeck-web/src/lib/api/auth.test.ts`

**Interfaces:**
- Produces:
  - `tokenStore`: `getRefreshToken()`, `setRefreshToken(token: string)`, `clearRefreshToken()`, `clearTokens()` (clears both keys). Keep existing `getToken/setToken/clearToken`.
  - `AuthResponse` TS type gains `refreshToken: string`.
  - `api/auth`: `refresh(refreshToken: string): Promise<AuthResponse>`, `logout(refreshToken: string): Promise<void>`.

- [ ] **Step 1: Extend `tokenStore.ts`**

Append refresh-token helpers and a combined clear (keep the existing access-token functions unchanged):

```ts
const REFRESH_TOKEN_KEY = 'brewdeck.refreshToken';

export function getRefreshToken(): string | null {
  if (typeof window === 'undefined') {
    return null;
  }
  return window.localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setRefreshToken(token: string): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(REFRESH_TOKEN_KEY, token);
}

export function clearRefreshToken(): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function clearTokens(): void {
  clearToken();
  clearRefreshToken();
}
```

- [ ] **Step 2: Extend `AuthResponse` type**

In `types.ts`, add the field:

```ts
export type AuthResponse = {
  token: string;
  expiresAt: string;
  email: string;
  refreshToken: string;
};
```

- [ ] **Step 3: Add `refresh` + `logout` to `api/auth.ts`**

Append:

```ts
export function refresh(refreshToken: string): Promise<AuthResponse> {
  return apiFetch<AuthResponse>('/api/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  });
}

export function logout(refreshToken: string): Promise<void> {
  return apiFetch<void>('/api/auth/logout', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  });
}
```

- [ ] **Step 4: Write failing tests**

Add to `tokenStore.test.ts` (follow the file's existing structure):

```ts
it('stores, reads, and clears the refresh token', () => {
  setRefreshToken('r-1');
  expect(getRefreshToken()).toBe('r-1');
  clearRefreshToken();
  expect(getRefreshToken()).toBeNull();
});

it('clearTokens clears both the access and refresh tokens', () => {
  setToken('a-1');
  setRefreshToken('r-1');
  clearTokens();
  expect(getToken()).toBeNull();
  expect(getRefreshToken()).toBeNull();
});
```

Add to `auth.test.ts` (mirror an existing call-shape test — mock `apiFetch`/`fetch` as the file already does):

```ts
it('refresh posts the refresh token to /api/auth/refresh', async () => {
  // arrange fetch mock returning an AuthResponse; act: await refresh('r-old');
  // assert the request URL ends with /api/auth/refresh, method POST, body { refreshToken: 'r-old' }
});

it('logout posts the refresh token to /api/auth/logout', async () => {
  // arrange fetch mock returning 204; act: await logout('r-1');
  // assert URL /api/auth/logout, method POST, body { refreshToken: 'r-1' }
});
```

Fill the arrange/act/assert bodies using the exact mocking helper already present in `auth.test.ts` (match how `login`/`forgotPassword` tests assert URL, method, and JSON body).

- [ ] **Step 5: Run**

Run: `cd brewdeck-web && pnpm test -- src/lib/auth/tokenStore.test.ts src/lib/api/auth.test.ts`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add brewdeck-web/src/lib/auth/tokenStore.ts brewdeck-web/src/lib/auth/tokenStore.test.ts \
  brewdeck-web/src/lib/api/types.ts brewdeck-web/src/lib/api/auth.ts brewdeck-web/src/lib/api/auth.test.ts
git commit -m "feat(web): add refresh-token storage and refresh/logout api calls"
```

---

## Task 6: Frontend single-flight refresh interceptor

**Files:**
- Modify: `brewdeck-web/src/lib/api/client.ts`
- Test: `brewdeck-web/src/lib/api/client.test.ts`

**Interfaces:**
- Consumes: `getToken`, `setToken`, `getRefreshToken`, `setRefreshToken`, `clearTokens` (Task 5); `AuthResponse` type.
- Produces: `apiFetch<T>(path, init?, allowRefresh = true)` — on a 401 with a stored refresh token (and not already refreshing/looping), transparently refreshes once via a module-level single-flight promise, then retries the original request once.

- [ ] **Step 1: Rewrite `client.ts`**

Replace the file with the refresh-aware version. Key rules: skip refresh when `allowRefresh` is false, when the path is `/api/auth/refresh`, or when there is no stored refresh token; share one in-flight refresh across concurrent 401s; on refresh success store the new pair and retry once (`allowRefresh = false`); on failure fall through to the existing clear + redirect.

```ts
import { API_BASE_URL } from '@/config/env';
import {
  clearTokens,
  getRefreshToken,
  getToken,
  setRefreshToken,
  setToken,
} from '@/lib/auth/tokenStore';
import type { AuthResponse, ErrorResponse } from './types';

export class ApiError extends Error {
  status: number;
  path?: string;
  validationErrors?: Record<string, string>;

  constructor(
    status: number,
    message: string,
    path?: string,
    validationErrors?: Record<string, string>,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.path = path;
    this.validationErrors = validationErrors;
  }
}

let refreshInFlight: Promise<AuthResponse> | null = null;

function isOnPublicPath(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }
  const p = window.location.pathname;
  return (
    p === '/login' ||
    p === '/register' ||
    p === '/forgot-password' ||
    p === '/reset-password' ||
    p === '/verify-email' ||
    p.startsWith('/share')
  );
}

async function runRefresh(refreshToken: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });
  if (!response.ok) {
    throw new ApiError(response.status, 'Refresh failed');
  }
  return (await response.json()) as AuthResponse;
}

// Collapses concurrent 401s into a single rotation so we never double-fire /refresh
// (a second rotation would present an already-used token and trip reuse detection).
function attemptRefresh(refreshToken: string): Promise<AuthResponse> {
  if (!refreshInFlight) {
    refreshInFlight = runRefresh(refreshToken).finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

export async function apiFetch<T>(
  path: string,
  init?: RequestInit,
  allowRefresh = true,
): Promise<T> {
  const token = getToken();
  const authHeader: Record<string, string> = token ? { Authorization: `Bearer ${token}` } : {};
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...authHeader, ...init?.headers },
  });

  if (response.status === 401) {
    const refreshToken = getRefreshToken();
    const canRefresh = allowRefresh && path !== '/api/auth/refresh' && refreshToken !== null;

    if (canRefresh) {
      try {
        const auth = await attemptRefresh(refreshToken as string);
        setToken(auth.token);
        setRefreshToken(auth.refreshToken);
        // Retry the original request once; disallow a further refresh to avoid loops.
        return await apiFetch<T>(path, init, false);
      } catch {
        // Refresh failed — fall through to clear + redirect below.
      }
    }

    clearTokens();
    if (typeof window !== 'undefined' && !isOnPublicPath()) {
      window.location.assign('/login');
    }
  }

  if (!response.ok) {
    let body: Partial<ErrorResponse> = {};
    try {
      body = (await response.json()) as ErrorResponse;
    } catch {
      // non-JSON error body; fall back to status text
    }
    throw new ApiError(
      response.status,
      body.message ?? response.statusText,
      body.path,
      body.validationErrors,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
```

- [ ] **Step 2: Verify existing client tests still pass**

The existing 401 tests set only an access token (no refresh token), so `getRefreshToken()` is `null` → `canRefresh` false → the original clear + redirect path runs, and `clearTokens()` still nulls the access token.

Run: `cd brewdeck-web && pnpm test -- src/lib/api/client.test.ts`
Expected: PASS (existing tests unchanged in behavior).

- [ ] **Step 3: Add single-flight + retry tests**

Append to `client.test.ts`. Use a URL-routing fetch mock so the refresh endpoint and the retried original resolve differently. Reset `refreshInFlight` between tests by re-importing is unnecessary (module state resets per file run, but each test must fully drain the in-flight promise — the `finally` handles that).

```ts
import { setRefreshToken } from '@/lib/auth/tokenStore';

function routedFetch(handlers: Record<string, () => { ok: boolean; status: number; body: unknown }>) {
  return vi.fn((url: string) => {
    const key = Object.keys(handlers).find((k) => String(url).includes(k));
    const res = key ? handlers[key]() : { ok: false, status: 404, body: {} };
    return Promise.resolve({
      ok: res.ok,
      status: res.status,
      statusText: 'Status',
      json: () => Promise.resolve(res.body),
    });
  });
}

it('refreshes once and retries the original request on 401', async () => {
  setToken('stale-access');
  setRefreshToken('r-1');
  let coffeesCalls = 0;
  const fetchMock = routedFetch({
    '/api/auth/refresh': () => ({
      ok: true,
      status: 200,
      body: { token: 'fresh-access', refreshToken: 'r-2', email: 'u@e.com', expiresAt: 'x' },
    }),
    '/api/coffees': () => {
      coffeesCalls += 1;
      return coffeesCalls === 1
        ? { ok: false, status: 401, body: { message: 'expired' } }
        : { ok: true, status: 200, body: { value: 1 } };
    },
  });
  vi.stubGlobal('fetch', fetchMock);

  const result = await apiFetch<{ value: number }>('/api/coffees');

  expect(result).toEqual({ value: 1 });
  expect(getToken()).toBe('fresh-access');
  const refreshCalls = fetchMock.mock.calls.filter((c) => String(c[0]).includes('/api/auth/refresh'));
  expect(refreshCalls).toHaveLength(1);
});

it('shares a single refresh across concurrent 401s', async () => {
  setToken('stale-access');
  setRefreshToken('r-1');
  const okAfterRefresh: Record<string, number> = {};
  const fetchMock = routedFetch({
    '/api/auth/refresh': () => ({
      ok: true,
      status: 200,
      body: { token: 'fresh-access', refreshToken: 'r-2', email: 'u@e.com', expiresAt: 'x' },
    }),
    '/api/a': () => {
      okAfterRefresh.a = (okAfterRefresh.a ?? 0) + 1;
      return okAfterRefresh.a === 1
        ? { ok: false, status: 401, body: {} }
        : { ok: true, status: 200, body: { ok: 'a' } };
    },
    '/api/b': () => {
      okAfterRefresh.b = (okAfterRefresh.b ?? 0) + 1;
      return okAfterRefresh.b === 1
        ? { ok: false, status: 401, body: {} }
        : { ok: true, status: 200, body: { ok: 'b' } };
    },
  });
  vi.stubGlobal('fetch', fetchMock);

  await Promise.all([apiFetch('/api/a'), apiFetch('/api/b')]);

  const refreshCalls = fetchMock.mock.calls.filter((c) => String(c[0]).includes('/api/auth/refresh'));
  expect(refreshCalls).toHaveLength(1);
});

it('clears tokens and redirects when the refresh itself fails', async () => {
  setToken('stale-access');
  setRefreshToken('r-1');
  const assignMock = vi.fn();
  vi.stubGlobal('location', { pathname: '/', assign: assignMock });
  vi.stubGlobal(
    'fetch',
    routedFetch({
      '/api/auth/refresh': () => ({ ok: false, status: 401, body: {} }),
      '/api/coffees': () => ({ ok: false, status: 401, body: { message: 'expired' } }),
    }),
  );

  await expect(apiFetch('/api/coffees')).rejects.toThrow();
  expect(getToken()).toBeNull();
  expect(getRefreshToken()).toBeNull();
  expect(assignMock).toHaveBeenCalledWith('/login');
});
```

Add `getRefreshToken` and `setRefreshToken` to the existing tokenStore import in the test file, and ensure `afterEach` also calls `clearTokens()` (or `clearRefreshToken()`), so refresh-token state does not leak between tests.

- [ ] **Step 4: Run**

Run: `cd brewdeck-web && pnpm test -- src/lib/api/client.test.ts`
Expected: PASS (existing + 3 new tests).

- [ ] **Step 5: Commit**

```bash
git add brewdeck-web/src/lib/api/client.ts brewdeck-web/src/lib/api/client.test.ts
git commit -m "feat(web): auto-refresh access token on 401 with single-flight retry"
```

---

## Task 7: AuthProvider — store refresh token, real logout

**Files:**
- Modify: `brewdeck-web/src/lib/auth/AuthProvider.tsx`
- Test: `brewdeck-web/src/lib/auth/AuthProvider.test.tsx`

**Interfaces:**
- Consumes: `setRefreshToken`, `clearTokens` (Task 5); `logout as logoutApi` from `@/lib/api/auth` (Task 5).
- Produces: `AuthContextValue.logout` becomes `() => Promise<void>`; login/register persist the refresh token.

- [ ] **Step 1: Update `AuthProvider.tsx`**

Change imports:

```ts
import {
  getMe,
  login as loginApi,
  logout as logoutApi,
  register as registerApi,
  updateProfile as updateProfileApi,
} from '@/lib/api/auth';
```

```ts
import { clearTokens, getToken, setRefreshToken, setToken } from './tokenStore';
```

Change the `logout` type in `AuthContextValue`:

```ts
  logout: () => Promise<void>;
```

In `login` and `register`, after `setToken(response.token);` add:

```ts
        setRefreshToken(response.refreshToken);
```

Replace `logout`:

```ts
      logout: async () => {
        const refreshToken =
          typeof window !== 'undefined' ? window.localStorage.getItem('brewdeck.refreshToken') : null;
        if (refreshToken) {
          try {
            await logoutApi(refreshToken);
          } catch {
            // Best-effort: proceed to local sign-out even if the revoke call fails.
          }
        }
        clearTokens();
        setUser(null);
        setStatus('anonymous');
      },
```

> Prefer importing `getRefreshToken` from `./tokenStore` instead of reading `localStorage` directly — use `import { clearTokens, getRefreshToken, getToken, setRefreshToken, setToken } from './tokenStore';` and `const refreshToken = getRefreshToken();`. Do not leave a raw `localStorage` string key in the component.

Also update the effect's catch and any other `clearToken()` calls in this file to `clearTokens()` so a failed `/me` clears both tokens.

- [ ] **Step 2: Update the tests**

In `AuthProvider.test.tsx`:
- Existing `login`/`register` mocks that resolve an `AuthResponse` must include `refreshToken: 'r-1'` (the type now requires it — this is also a `type-check`/`build` failure if omitted).
- `logout` is now async; tests that call it must `await` it (wrap in `act(async () => { await result.current.logout(); })`).
- Add a test: after `login`, `localStorage.getItem('brewdeck.refreshToken')` is the returned refresh token.
- Add a test: `logout()` calls the mocked `logout` api with the stored refresh token, then clears both tokens and sets status `anonymous`. Mock `@/lib/api/auth`'s `logout`. Add a test that logout still clears locally when the mocked `logout` rejects.

Follow the file's existing mocking style (`vi.mock('@/lib/api/auth', ...)`). Ensure the mock factory now also exports `logout`.

- [ ] **Step 3: Run**

Run: `cd brewdeck-web && pnpm test -- src/lib/auth/AuthProvider.test.tsx`
Expected: PASS.

- [ ] **Step 4: Full frontend gate**

Run: `cd brewdeck-web && pnpm test && pnpm type-check && pnpm build`
Expected: all green. Then scope-lint the changed files:
`pnpm lint:fix -- src/lib/auth/tokenStore.ts src/lib/api/types.ts src/lib/api/auth.ts src/lib/api/client.ts src/lib/auth/AuthProvider.tsx`

- [ ] **Step 5: Commit**

```bash
git add brewdeck-web/src/lib/auth/AuthProvider.tsx brewdeck-web/src/lib/auth/AuthProvider.test.tsx
git commit -m "feat(web): persist refresh token and revoke it on logout"
```

---

## Task 8: Docs, Postman, config sample, roadmap/state

**Files:**
- Modify: `.env.example` (repo root or `brewdeck-api/.env.example` — check which exists)
- Modify: `docs/api/README.md`
- Modify: `docs/api/openapi.yaml`
- Modify: `docs/api/postman/brewdeck.postman_collection.json`
- Modify: `.claude/project-state.md`, `.claude/roadmap.md`

**Interfaces:** none (documentation).

- [ ] **Step 1: `.env.example`**

Run: `find . -name '.env.example' -not -path '*/node_modules/*'` to locate it. Add/adjust:

```bash
# Access-token lifetime (ISO-8601 duration). Default 15 minutes.
AUTH_TOKEN_TTL=PT15M
# Refresh-token lifetime (ISO-8601 duration). Default 7 days.
AUTH_REFRESH_TTL=P7D
```

- [ ] **Step 2: `docs/api/README.md` + `openapi.yaml`**

Document under the Auth section:
- `POST /api/auth/refresh` — public. Body `{ "refreshToken": "..." }`. 200 → `AuthResponse` (`token`, `expiresAt`, `email`, `refreshToken`); 401 invalid/expired/used; 400 blank.
- `POST /api/auth/logout` — authenticated (Bearer). Body `{ "refreshToken": "..." }`. 204; 400 blank; 401 no/invalid access token.
- Note the new `refreshToken` field on the `register`/`login` responses.

Mirror the existing style of the reset/verify entries in both files. In `openapi.yaml` add the two paths and the `RefreshRequest` schema, and add `refreshToken` to the `AuthResponse` schema.

- [ ] **Step 3: Postman collection**

In `docs/api/postman/brewdeck.postman_collection.json`, add two requests under the Auth folder:
- "Refresh token" → `POST {{baseUrl}}/api/auth/refresh`, body `{ "refreshToken": "{{refreshToken}}" }`, with a test script that saves `pm.collectionVariables.set('refreshToken', pm.response.json().refreshToken)` and the access token similarly.
- "Logout" → `POST {{baseUrl}}/api/auth/logout`, Bearer `{{authToken}}`, body `{ "refreshToken": "{{refreshToken}}" }`.
- Update the existing Login/Register requests' test scripts to also capture `refreshToken` into the `{{refreshToken}}` collection variable.

Add a `refreshToken` collection variable (empty default). No real credentials.

- [ ] **Step 4: Roadmap + project-state**

In `.claude/roadmap.md`, change the Slice C.4 line from `Pending` to `Done` with a one-line summary, and mark Phase 6 Status `Completed`. In `.claude/project-state.md`, update "Current Phase", add a "Recently Worked On" entry for C.4, and update "Immediate Next Steps" (drop C.4; keep the deferred-Minors sweep + JaCoCo/Sonar review). Mirror the same status in `docs/product/roadmap.md` if it tracks phases.

- [ ] **Step 5: Sanity-check the collection JSON parses**

Run: `cat docs/api/postman/brewdeck.postman_collection.json | python3 -m json.tool > /dev/null && echo OK`
Expected: `OK`.

- [ ] **Step 6: Commit**

```bash
git add .env.example docs/api .claude/project-state.md .claude/roadmap.md docs/product/roadmap.md
git commit -m "docs: document refresh/logout endpoints and mark Phase 6 Slice C.4 done"
```

---

## Final verification (before opening the PR)

- [ ] Backend full gate: `cd brewdeck-api && ./mvnw spotless:apply && ./mvnw clean verify && sh mvnw pmd:check` → BUILD SUCCESS.
- [ ] Frontend full gate: `cd brewdeck-web && pnpm test && pnpm type-check && pnpm build && pnpm lint` → all green.
- [ ] Manual smoke (optional): with `AUTH_TOKEN_TTL=PT10S`, log in, wait 10s, trigger an API call from the UI, confirm it silently refreshes (no bounce to `/login`) and a network tab shows one `/api/auth/refresh` call.
- [ ] Open PR `feature/auth-refresh-tokens` → `develop`.

---

## Self-Review Notes

- **Spec coverage:** transport/storage (Tasks 5-7), Flyway V11 + entity/repo (Task 1), issue/rotate/reuse-revoke/revoke service (Task 2), endpoints + AuthResponse + SecurityConfig + 401 mapping (Task 3), integration reuse+logout proof (Task 4), single-flight client (Task 6), AuthProvider logout (Task 7), config/docs/postman/roadmap (Tasks 2 & 8). All spec sections mapped.
- **Reuse-revoke rollback gotcha:** `rotate` is `@Transactional(noRollbackFor = InvalidRefreshTokenException.class)` so the reuse-path revocation commits despite the thrown 401 — Task 2 documents it, Task 4 proves it end-to-end.
- **Backward compat:** `AuthResponse` gains a trailing field (non-breaking JSON); every `new AuthResponse(` call site is swept in Task 3 Step 8. `login` annotation switched off `readOnly` because issuing a refresh token writes.
- **No footguns:** client refresh is gated on `allowRefresh`, non-`/refresh` path, and presence of a stored refresh token, so existing 401 tests keep their clear+redirect behavior and no infinite loop is possible.
