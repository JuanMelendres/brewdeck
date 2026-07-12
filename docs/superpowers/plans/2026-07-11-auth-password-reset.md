# Password Reset (Slice C.2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a user reset a forgotten password via an emailed single-use, time-limited token, with no live SMTP dependency required to ship.

**Architecture:** New `auth.reset` package. `PasswordResetService` orchestrates a `password_reset_tokens` table (stores only `SHA-256(token)` hex, single-use via `used_at`, 30-min TTL) and delivers the raw token through a `PasswordResetMailPort` whose default `LoggingPasswordResetMailAdapter` logs the reset link (SMTP adapter stubbed behind `brewdeck.mail.enabled`). Two public endpoints: `POST /api/auth/forgot-password` (always 200, no user enumeration) and `POST /api/auth/reset-password` (204). Frontend adds public `/forgot-password` and `/reset-password` pages.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Data JPA, Flyway, PostgreSQL, JUnit 5, Mockito, MockMvc, Testcontainers; Next.js App Router, React 19, TypeScript, MUI, React Hook Form + Zod, Vitest.

## Global Constraints

- Package base: `com.brewdeck.brewdeck_api`. New code under `auth.reset`.
- **No user enumeration:** `forgot-password` returns `200` identically for known and unknown emails.
- **Store only the token hash** (hex `SHA-256`, `VARCHAR(64)`); the raw token leaves the backend only via the mail port.
- **Single-use** (`used_at`) and **30-minute TTL** (`expires_at`); invalid/expired/used all map to one `400` with a generic message.
- Token generation mirrors `RecipeService`: `SecureRandom` 32 bytes → `Base64.getUrlEncoder().withoutPadding()`.
- Password validation matches `ChangePasswordRequest`: `@NotBlank @Size(min = 8, max = 100)`.
- Validation messages: no special characters (write "degrees Celsius", never symbols).
- Reset endpoints are `permitAll` in `SecurityConfig`, alongside `register`/`login`.
- Commits: Conventional Commits. Backend scope `api`, frontend `web`, docs `docs`.
- Verify backend with `./mvnw spotless:apply && ./mvnw clean verify` then `sh mvnw pmd:check`.
- Verify frontend (in `brewdeck-web/`) with `npm run test`, `npm run type-check`, `npm run lint`, `npm run build`.
- `@ConfigurationPropertiesScan` is already on `BrewdeckApiApplication`; a `@ConfigurationProperties` record is auto-registered (no manual `@EnableConfigurationProperties`).

---

### Task 1: Reset-token table, entity, and repository

**Files:**
- Create: `brewdeck-api/src/main/resources/db/migration/V9__create_password_reset_tokens.sql`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetToken.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetTokenRepository.java`
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetTokenRepositoryTest.java`

**Interfaces:**
- Produces: `PasswordResetToken` entity (fields `id`, `userId`, `tokenHash`, `expiresAt`, `usedAt`, `createdAt` with Lombok `@Getter/@Setter/@Builder/@NoArgsConstructor/@AllArgsConstructor`); `PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long>` with `Optional<PasswordResetToken> findByTokenHash(String tokenHash)` and `List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId)`.

- [ ] **Step 1: Write the migration**

`V9__create_password_reset_tokens.sql`:
```sql
-- Slice C.2: single-use, time-limited password reset tokens.
-- Only a SHA-256 hash of the token is stored; the raw token lives only in the emailed link.
CREATE TABLE password_reset_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
```

- [ ] **Step 2: Write the entity**

`PasswordResetToken.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

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

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
```

- [ ] **Step 3: Write the repository**

`PasswordResetTokenRepository.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId);
}
```

- [ ] **Step 4: Write the failing repository test**

`PasswordResetTokenRepositoryTest.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class PasswordResetTokenRepositoryTest extends PostgresIntegrationTest {

  @Autowired private PasswordResetTokenRepository tokenRepository;
  @Autowired private UserRepository userRepository;

  @Test
  void findByTokenHash_returnsToken() {
    User user =
        userRepository.save(
            User.builder()
                .email("reset-" + System.nanoTime() + "@example.com")
                .passwordHash("hash")
                .createdAt(LocalDateTime.now())
                .build());
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash("abc123")
            .expiresAt(LocalDateTime.now().plusMinutes(30))
            .createdAt(LocalDateTime.now())
            .build());

    assertThat(tokenRepository.findByTokenHash("abc123")).isPresent();
    assertThat(tokenRepository.findByTokenHash("missing")).isEmpty();
  }

  @Test
  void findByUserIdAndUsedAtIsNull_excludesUsedTokens() {
    User user =
        userRepository.save(
            User.builder()
                .email("reset-" + System.nanoTime() + "@example.com")
                .passwordHash("hash")
                .createdAt(LocalDateTime.now())
                .build());
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash("unused-" + System.nanoTime())
            .expiresAt(LocalDateTime.now().plusMinutes(30))
            .createdAt(LocalDateTime.now())
            .build());
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash("used-" + System.nanoTime())
            .expiresAt(LocalDateTime.now().plusMinutes(30))
            .usedAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build());

    assertThat(tokenRepository.findByUserIdAndUsedAtIsNull(user.getId())).hasSize(1);
  }
}
```

- [ ] **Step 5: Run the test**

Run: `cd brewdeck-api && sh mvnw -Dtest=PasswordResetTokenRepositoryTest test`
Expected: PASS (both tests green; migration applied by Flyway on the Testcontainer).

- [ ] **Step 6: Commit**

```bash
git add brewdeck-api/src/main/resources/db/migration/V9__create_password_reset_tokens.sql \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetToken.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetTokenRepository.java \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetTokenRepositoryTest.java
git commit -m "feat(api): add password_reset_tokens table, entity, and repository (Slice C.2)"
```

---

### Task 2: Mail properties and mail port with logging + stub SMTP adapters

**Files:**
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/MailProperties.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetMailPort.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/LoggingPasswordResetMailAdapter.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/SmtpPasswordResetMailAdapter.java`
- Modify: `brewdeck-api/src/main/resources/application.yaml:42-51` (add `mail` block under `brewdeck`)
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/reset/LoggingPasswordResetMailAdapterTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `PasswordResetMailPort` with `void sendResetLink(String email, String rawToken)`; `MailProperties(boolean enabled, String frontendBaseUrl)` record bound to prefix `brewdeck.mail`.

- [ ] **Step 1: Write the properties record**

`MailProperties.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brewdeck.mail")
public record MailProperties(boolean enabled, String frontendBaseUrl) {}
```

- [ ] **Step 2: Write the port**

`PasswordResetMailPort.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

/** Delivers a password-reset link to the user. Swap adapters via {@code brewdeck.mail.enabled}. */
public interface PasswordResetMailPort {
  void sendResetLink(String email, String rawToken);
}
```

- [ ] **Step 3: Write the default logging adapter**

`LoggingPasswordResetMailAdapter.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default adapter used whenever {@code brewdeck.mail.enabled} is false or absent. Logs the reset
 * link instead of sending an email, so the full flow works with no SMTP dependency.
 */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "brewdeck.mail",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class LoggingPasswordResetMailAdapter implements PasswordResetMailPort {

  private final MailProperties properties;

  public LoggingPasswordResetMailAdapter(MailProperties properties) {
    this.properties = properties;
  }

  @Override
  public void sendResetLink(String email, String rawToken) {
    log.info(
        "Password reset link for {}: {}/reset-password?token={}",
        email,
        properties.frontendBaseUrl(),
        rawToken);
  }
}
```

- [ ] **Step 4: Write the stub SMTP adapter**

`SmtpPasswordResetMailAdapter.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Activated by {@code brewdeck.mail.enabled=true}. Placeholder for a real JavaMailSender
 * integration; wiring an actual SMTP provider is a documented follow-up to Slice C.2.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "brewdeck.mail", name = "enabled", havingValue = "true")
public class SmtpPasswordResetMailAdapter implements PasswordResetMailPort {

  @Override
  public void sendResetLink(String email, String rawToken) {
    // TODO(C.2 follow-up): send a real transactional email via JavaMailSender.
    log.warn("SMTP mail adapter enabled but not implemented; reset link for {} not sent", email);
    throw new UnsupportedOperationException("SMTP password-reset delivery is not implemented yet");
  }
}
```

- [ ] **Step 5: Add the mail config block**

In `application.yaml`, under `brewdeck:` (after the `ai:` block, sibling to `auth:`):
```yaml
  mail:
    enabled: ${BREWDECK_MAIL_ENABLED:false}
    frontend-base-url: ${BREWDECK_MAIL_FRONTEND_BASE_URL:http://localhost:3000}
```

- [ ] **Step 6: Write the failing adapter test**

`LoggingPasswordResetMailAdapterTest.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class LoggingPasswordResetMailAdapterTest {

  @Test
  void sendResetLink_logsWithoutThrowing() {
    LoggingPasswordResetMailAdapter adapter =
        new LoggingPasswordResetMailAdapter(new MailProperties(false, "http://localhost:3000"));

    assertThatCode(() -> adapter.sendResetLink("brewer@example.com", "raw-token"))
        .doesNotThrowAnyException();
  }
}
```

- [ ] **Step 7: Run the test**

Run: `cd brewdeck-api && sh mvnw -Dtest=LoggingPasswordResetMailAdapterTest test`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/MailProperties.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetMailPort.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/LoggingPasswordResetMailAdapter.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/SmtpPasswordResetMailAdapter.java \
  brewdeck-api/src/main/resources/application.yaml \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/reset/LoggingPasswordResetMailAdapterTest.java
git commit -m "feat(api): add password-reset mail port with logging and stub SMTP adapters (Slice C.2)"
```

---

### Task 3: Request records and invalid-token exception mapping

**Files:**
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/ForgotPasswordRequest.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/ResetPasswordRequest.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/InvalidResetTokenException.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/error/GlobalExceptionHandler.java` (import + handler)
- Test: covered by the controller test in Task 5 (no standalone test — these are trivial DTOs/exception).

**Interfaces:**
- Produces: `ForgotPasswordRequest(String email)`; `ResetPasswordRequest(String token, String newPassword)`; `InvalidResetTokenException(String message)` mapped to HTTP 400.

- [ ] **Step 1: Write the request records**

`ForgotPasswordRequest.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank @Email String email) {}
```

`ResetPasswordRequest.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank String token, @NotBlank @Size(min = 8, max = 100) String newPassword) {}
```

- [ ] **Step 2: Write the exception**

`InvalidResetTokenException.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

/** Raised when a reset token is unknown, already used, or expired. */
public class InvalidResetTokenException extends RuntimeException {
  public InvalidResetTokenException(String message) {
    super(message);
  }
}
```

- [ ] **Step 3: Map the exception to 400**

In `GlobalExceptionHandler.java`, add the import next to the other `auth` imports:
```java
import com.brewdeck.brewdeck_api.auth.reset.InvalidResetTokenException;
```
Add this handler immediately above the `InvalidCurrentPasswordException` handler:
```java
  @ExceptionHandler(InvalidResetTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidResetToken(
      InvalidResetTokenException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Reset token is invalid or has expired",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.badRequest().body(errorResponse);
  }
```

- [ ] **Step 4: Compile**

Run: `cd brewdeck-api && sh mvnw -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/ForgotPasswordRequest.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/ResetPasswordRequest.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/InvalidResetTokenException.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/error/GlobalExceptionHandler.java
git commit -m "feat(api): add password-reset request records and invalid-token mapping (Slice C.2)"
```

---

### Task 4: PasswordResetService (forgot + reset orchestration)

**Files:**
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetService.java`
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetServiceTest.java`

**Interfaces:**
- Consumes: `PasswordResetTokenRepository`, `PasswordResetMailPort`, `MailProperties`, existing `UserRepository`, existing `PasswordEncoder` bean; `PasswordResetToken`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `InvalidResetTokenException`.
- Produces: `PasswordResetService` with `void requestReset(ForgotPasswordRequest request)` and `void resetPassword(ResetPasswordRequest request)`.

- [ ] **Step 1: Write the service**

`PasswordResetService.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PasswordResetService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int TOKEN_BYTES = 32;
  private static final int TTL_MINUTES = 30;

  private final PasswordResetTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordResetMailPort mailPort;

  public PasswordResetService(
      PasswordResetTokenRepository tokenRepository,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      PasswordResetMailPort mailPort) {
    this.tokenRepository = tokenRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.mailPort = mailPort;
  }

  @Transactional
  public void requestReset(ForgotPasswordRequest request) {
    Optional<User> maybeUser = userRepository.findByEmail(request.email());
    if (maybeUser.isEmpty()) {
      // No user enumeration: silently succeed for unknown emails.
      log.info("Password reset requested for unknown email; no-op");
      return;
    }
    User user = maybeUser.get();

    // Invalidate any outstanding unused tokens for this user.
    List<PasswordResetToken> outstanding =
        tokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
    LocalDateTime now = LocalDateTime.now();
    outstanding.forEach(token -> token.setUsedAt(now));
    tokenRepository.saveAll(outstanding);

    String rawToken = generateRawToken();
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash(hash(rawToken))
            .expiresAt(now.plusMinutes(TTL_MINUTES))
            .createdAt(now)
            .build());

    mailPort.sendResetLink(user.getEmail(), rawToken);
    log.info("Password reset link issued for user id={}", user.getId());
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    PasswordResetToken token =
        tokenRepository
            .findByTokenHash(hash(request.token()))
            .orElseThrow(() -> new InvalidResetTokenException("Unknown reset token"));

    if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new InvalidResetTokenException("Reset token used or expired");
    }

    User user =
        userRepository
            .findById(token.getUserId())
            .orElseThrow(() -> new InvalidResetTokenException("Reset token has no user"));

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);

    token.setUsedAt(LocalDateTime.now());
    tokenRepository.save(token);
    log.info("Password reset completed for user id={}", user.getId());
  }

  private String generateRawToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
```

- [ ] **Step 2: Write the failing service test**

`PasswordResetServiceTest.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  @Mock private PasswordResetTokenRepository tokenRepository;
  @Mock private UserRepository userRepository;
  @Mock private PasswordResetMailPort mailPort;

  private PasswordResetService service;

  @BeforeEach
  void setUp() {
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    service = new PasswordResetService(tokenRepository, userRepository, encoder, mailPort);
  }

  private User user() {
    return User.builder()
        .id(1L)
        .email("brewer@example.com")
        .passwordHash(new BCryptPasswordEncoder().encode("password1"))
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  void requestReset_unknownEmail_isSilentNoOp() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    service.requestReset(new ForgotPasswordRequest("ghost@example.com"));

    verify(tokenRepository, never()).save(any());
    verify(mailPort, never()).sendResetLink(anyString(), anyString());
  }

  @Test
  void requestReset_knownEmail_persistsHashedTokenAndSendsLink() {
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user()));
    when(tokenRepository.findByUserIdAndUsedAtIsNull(1L)).thenReturn(List.of());

    service.requestReset(new ForgotPasswordRequest("brewer@example.com"));

    ArgumentCaptor<PasswordResetToken> tokenCaptor =
        ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(tokenRepository).save(tokenCaptor.capture());
    ArgumentCaptor<String> rawCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailPort).sendResetLink(eq("brewer@example.com"), rawCaptor.capture());

    PasswordResetToken saved = tokenCaptor.getValue();
    // Stored value is a 64-char hex hash, never the raw token.
    assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo(rawCaptor.getValue());
    assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
  }

  @Test
  void resetPassword_validToken_reencodesAndStampsUsed() {
    PasswordResetToken token =
        PasswordResetToken.builder()
            .id(5L)
            .userId(1L)
            .tokenHash(
                "9d0e410f5e6a3f0e0c3e8f6d6f2b4a0c9d0e410f5e6a3f0e0c3e8f6d6f2b4a0c")
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .createdAt(LocalDateTime.now())
            .build();
    // Match the service's hash of the supplied raw token.
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
    User user = user();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    String originalHash = user.getPasswordHash();

    service.resetPassword(new ResetPasswordRequest("any-raw-token", "newpassword1"));

    assertThat(user.getPasswordHash()).isNotEqualTo(originalHash);
    assertThat(new BCryptPasswordEncoder().matches("newpassword1", user.getPasswordHash())).isTrue();
    assertThat(token.getUsedAt()).isNotNull();
  }

  @Test
  void resetPassword_unknownToken_throws() {
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.resetPassword(new ResetPasswordRequest("nope", "newpassword1")))
        .isInstanceOf(InvalidResetTokenException.class);
  }

  @Test
  void resetPassword_expiredToken_throws() {
    PasswordResetToken token =
        PasswordResetToken.builder()
            .id(6L)
            .userId(1L)
            .tokenHash("hash")
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .createdAt(LocalDateTime.now().minusMinutes(31))
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(
            () -> service.resetPassword(new ResetPasswordRequest("raw", "newpassword1")))
        .isInstanceOf(InvalidResetTokenException.class);
  }

  @Test
  void resetPassword_usedToken_throws() {
    PasswordResetToken token =
        PasswordResetToken.builder()
            .id(7L)
            .userId(1L)
            .tokenHash("hash")
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .usedAt(LocalDateTime.now().minusMinutes(1))
            .createdAt(LocalDateTime.now())
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(
            () -> service.resetPassword(new ResetPasswordRequest("raw", "newpassword1")))
        .isInstanceOf(InvalidResetTokenException.class);
  }
}
```

- [ ] **Step 3: Run the test**

Run: `cd brewdeck-api && sh mvnw -Dtest=PasswordResetServiceTest test`
Expected: PASS (all six tests green).

- [ ] **Step 4: Commit**

```bash
git add brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetService.java \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetServiceTest.java
git commit -m "feat(api): add PasswordResetService with hashed single-use tokens (Slice C.2)"
```

---

### Task 5: PasswordResetController + public security matchers

**Files:**
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetController.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/config/SecurityConfig.java:47-48` (add the two public matchers)
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetControllerTest.java`

**Interfaces:**
- Consumes: `PasswordResetService`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `InvalidResetTokenException`, `GlobalExceptionHandler`.
- Produces: `POST /api/auth/forgot-password` (200, body `{ "message": ... }`), `POST /api/auth/reset-password` (204).

- [ ] **Step 1: Write the controller**

`PasswordResetController.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, and current-user lookup")
public class PasswordResetController {

  private static final String GENERIC_MESSAGE = "If that email exists, a reset link has been sent.";

  private final PasswordResetService passwordResetService;

  @PostMapping("/forgot-password")
  @Operation(summary = "Request a password reset link")
  public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    passwordResetService.requestReset(request);
    return Map.of("message", GENERIC_MESSAGE);
  }

  @PostMapping("/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Reset a password using a reset token")
  public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    passwordResetService.resetPassword(request);
  }
}
```

- [ ] **Step 2: Open the two endpoints in SecurityConfig**

In `SecurityConfig.java`, change the register/login matcher line so both reset routes are public:
```java
                    .requestMatchers(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password")
                    .permitAll()
```

- [ ] **Step 3: Write the failing controller test**

`PasswordResetControllerTest.java`:
```java
package com.brewdeck.brewdeck_api.auth.reset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

  @Mock private PasswordResetService passwordResetService;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new PasswordResetController(passwordResetService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void forgotPassword_returns200GenericMessage() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("a@b.com"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("If that email exists, a reset link has been sent."));
  }

  @Test
  void forgotPassword_invalidEmailReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("not-email"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void resetPassword_returns204() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new ResetPasswordRequest("raw-token", "newpassword1"))))
        .andExpect(status().isNoContent());
  }

  @Test
  void resetPassword_invalidTokenReturns400() throws Exception {
    doThrow(new InvalidResetTokenException("bad"))
        .when(passwordResetService)
        .resetPassword(any());

    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new ResetPasswordRequest("raw-token", "newpassword1"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void resetPassword_shortPasswordReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(new ResetPasswordRequest("raw-token", "short"))))
        .andExpect(status().isBadRequest());
  }
}
```

- [ ] **Step 4: Run the test**

Run: `cd brewdeck-api && sh mvnw -Dtest=PasswordResetControllerTest test`
Expected: PASS (five tests green).

- [ ] **Step 5: Commit**

```bash
git add brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetController.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/config/SecurityConfig.java \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/reset/PasswordResetControllerTest.java
git commit -m "feat(api): add public forgot-password and reset-password endpoints (Slice C.2)"
```

---

### Task 6: End-to-end integration test (Testcontainers)

**Files:**
- Create: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/integration/PasswordResetIntegrationTest.java`

**Interfaces:**
- Consumes: full Spring context, `PasswordResetMailPort` (spied to capture the raw token), `PasswordResetTokenRepository`, `UserRepository`, `PostgresIntegrationTest` base.

- [ ] **Step 1: Write the integration test**

`PasswordResetIntegrationTest.java`:
```java
package com.brewdeck.brewdeck_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetMailPort;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetToken;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetTokenRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordResetTokenRepository tokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @MockitoSpyBean private PasswordResetMailPort mailPort;

  @Test
  void fullFlow_forgotThenResetThenLogin() throws Exception {
    String email = "reset-flow-" + System.nanoTime() + "@example.com";
    userRepository.save(
        User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode("password1"))
            .createdAt(LocalDateTime.now())
            .build());

    // forgot-password returns 200 and issues a link
    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\"}"))
        .andExpect(status().isOk());

    ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(mailPort)
        .sendResetLink(org.mockito.ArgumentMatchers.eq(email), rawToken.capture());
    String token = rawToken.getValue();

    // reset-password succeeds (204)
    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType("application/json")
                .content(
                    "{\"token\":\"" + token + "\",\"newPassword\":\"newpassword1\"}"))
        .andExpect(status().isNoContent());

    // new password logs in, old one does not
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"newpassword1\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"password1\"}"))
        .andExpect(status().isUnauthorized());

    // token is single-use: replay returns 400
    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType("application/json")
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"another12\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void forgotPassword_unknownEmail_returns200AndSendsNothing() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType("application/json")
                .content("{\"email\":\"nobody-" + System.nanoTime() + "@example.com\"}"))
        .andExpect(status().isOk());

    org.mockito.Mockito.verify(mailPort, org.mockito.Mockito.never())
        .sendResetLink(
            org.mockito.ArgumentMatchers.contains("nobody-"),
            org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void resetPassword_expiredToken_returns400() throws Exception {
    String email = "reset-exp-" + System.nanoTime() + "@example.com";
    User user =
        userRepository.save(
            User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("password1"))
                .createdAt(LocalDateTime.now())
                .build());
    // Persist a token whose hash we know, already expired.
    // SHA-256 hex of "expired-raw-token":
    String rawExpired = "expired-raw-token";
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash(sha256Hex(rawExpired))
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .createdAt(LocalDateTime.now().minusMinutes(31))
            .build());

    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType("application/json")
                .content(
                    "{\"token\":\"" + rawExpired + "\",\"newPassword\":\"newpassword1\"}"))
        .andExpect(status().isBadRequest());
  }

  private static String sha256Hex(String value) throws Exception {
    java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
    return java.util.HexFormat.of()
        .formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }
}
```

- [ ] **Step 2: Run the test**

Run: `cd brewdeck-api && sh mvnw -Dtest=PasswordResetIntegrationTest test`
Expected: PASS (three tests green).

- [ ] **Step 3: Full backend verify + PMD**

Run: `cd brewdeck-api && sh mvnw spotless:apply && sh mvnw clean verify && sh mvnw pmd:check`
Expected: BUILD SUCCESS, all tests pass, PMD clean.

- [ ] **Step 4: Commit**

```bash
git add brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/integration/PasswordResetIntegrationTest.java
git commit -m "test(api): cover password-reset flow end-to-end (Slice C.2)"
```

---

### Task 7: Backend docs — API reference, OpenAPI, Postman, .env.example

**Files:**
- Modify: `docs/api/README.md:13-20` (Auth block)
- Modify: `docs/api/openapi.yaml` (add two paths after `/api/auth/change-password`)
- Modify: `docs/api/postman/brewdeck.postman_collection.json` (two requests in the Auth folder)
- Modify/Create: `brewdeck-api/.env.example` and `brewdeck-web/.env.example` (mail vars)

**Interfaces:**
- Consumes: nothing; documentation only.

- [ ] **Step 1: Update the API README Auth block**

Replace the Auth code block in `docs/api/README.md` with:
```
POST  /api/auth/register          201
POST  /api/auth/login             200
GET   /api/auth/me                200 (401 without token)
PATCH /api/auth/me                200 (update display name)
POST  /api/auth/change-password   204 (400 if current password wrong)
POST  /api/auth/forgot-password   200 (always; no user enumeration)
POST  /api/auth/reset-password    204 (400 if token invalid/expired/used)
```

- [ ] **Step 2: Update OpenAPI**

In `docs/api/openapi.yaml`, add after the `/api/auth/change-password` path:
```yaml
  /api/auth/forgot-password:
    post:
      tags: [Auth]
      security: []
      summary: Request a password reset link (always 200, no user enumeration)
      responses:
        "200": { description: OK }
        "400": { description: Validation error, content: { application/json: { schema: { $ref: "#/components/schemas/ErrorResponse" } } } }
  /api/auth/reset-password:
    post:
      tags: [Auth]
      security: []
      summary: Reset password using a reset token
      responses:
        "204": { description: Password reset }
        "400": { description: Invalid/expired/used token or validation error, content: { application/json: { schema: { $ref: "#/components/schemas/ErrorResponse" } } } }
```

- [ ] **Step 3: Add the two Postman requests**

Run this from the repo root to append them to the Auth folder:
```bash
python3 - <<'PY'
import json
p='docs/api/postman/brewdeck.postman_collection.json'
c=json.load(open(p))
auth=[f for f in c['item'] if f['name']=='Auth'][0]
forgot={
  "name":"POST - Forgot password",
  "request":{
    "auth":{"type":"noauth"},
    "method":"POST",
    "header":[{"key":"Content-Type","value":"application/json"}],
    "body":{"mode":"raw","raw":"{\n  \"email\": \"brewer@example.com\"\n}"},
    "url":{"raw":"{{baseURL}}/api/auth/forgot-password","host":["{{baseURL}}"],"path":["api","auth","forgot-password"]}
  },
  "response":[],
  "event":[{"listen":"test","script":{"type":"text/javascript","exec":[
    "pm.test('Status code is 200', function () {",
    "  pm.response.to.have.status(200);",
    "});"
  ]}}]
}
reset={
  "name":"POST - Reset password",
  "request":{
    "auth":{"type":"noauth"},
    "method":"POST",
    "header":[{"key":"Content-Type","value":"application/json"}],
    "body":{"mode":"raw","raw":"{\n  \"token\": \"paste-token-from-logs\",\n  \"newPassword\": \"newpassword1\"\n}"},
    "url":{"raw":"{{baseURL}}/api/auth/reset-password","host":["{{baseURL}}"],"path":["api","auth","reset-password"]}
  },
  "response":[],
  "event":[{"listen":"test","script":{"type":"text/javascript","exec":[
    "pm.test('Status code is 204', function () {",
    "  pm.response.to.have.status(204);",
    "});"
  ]}}]
}
auth['item'].extend([forgot,reset])
json.dump(c,open(p,'w'),indent=2)
open(p,'a').write('\n')
print('added:',[i['name'] for i in auth['item']])
PY
```
Expected output lists the two new requests.

- [ ] **Step 4: Document the mail env vars**

Append to `brewdeck-api/.env.example` (create if missing):
```
# Password-reset email delivery (Slice C.2)
BREWDECK_MAIL_ENABLED=false
BREWDECK_MAIL_FRONTEND_BASE_URL=http://localhost:3000
```

- [ ] **Step 5: Commit**

```bash
git add docs/api/README.md docs/api/openapi.yaml \
  docs/api/postman/brewdeck.postman_collection.json brewdeck-api/.env.example
git commit -m "docs: document password-reset endpoints and mail env vars (Slice C.2)"
```

---

### Task 8: Frontend API client and Zod schemas

**Files:**
- Modify: `brewdeck-web/src/lib/api/auth.ts` (add `forgotPassword`, `resetPassword`)
- Modify: `brewdeck-web/src/lib/validation/authSchema.ts` (add `forgotPasswordSchema`, `resetPasswordSchema`)
- Test: covered by the form tests in Tasks 9–10.

**Interfaces:**
- Produces: `forgotPassword(body: { email: string }): Promise<{ message: string }>`; `resetPassword(body: { token: string; newPassword: string }): Promise<void>`; `forgotPasswordSchema` / `ForgotPasswordFormValues`; `resetPasswordSchema` / `ResetPasswordFormValues`.

- [ ] **Step 1: Add the API client functions**

In `brewdeck-web/src/lib/api/auth.ts`, append:
```typescript
export function forgotPassword(body: { email: string }): Promise<{ message: string }> {
  return apiFetch<{ message: string }>('/api/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function resetPassword(body: { token: string; newPassword: string }): Promise<void> {
  return apiFetch<void>('/api/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}
```

- [ ] **Step 2: Add the Zod schemas**

In `brewdeck-web/src/lib/validation/authSchema.ts`, before the `export type` lines, add:
```typescript
export const forgotPasswordSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
});

export const resetPasswordSchema = z
  .object({
    newPassword: z.string().min(8, 'New password must be at least 8 characters'),
    confirmPassword: z.string().min(1, 'Confirm your new password'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  });
```
And add to the type exports:
```typescript
export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
```

- [ ] **Step 3: Type-check**

Run: `cd brewdeck-web && npm run type-check`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add brewdeck-web/src/lib/api/auth.ts brewdeck-web/src/lib/validation/authSchema.ts
git commit -m "feat(web): add forgot/reset password API client and schemas (Slice C.2)"
```

---

### Task 9: Forgot-password page and form

**Files:**
- Create: `brewdeck-web/src/components/auth/ForgotPasswordForm.tsx`
- Create: `brewdeck-web/src/app/forgot-password/page.tsx`
- Modify: `brewdeck-web/src/components/auth/LoginForm.tsx` (add a "Forgot password?" link)
- Test: `brewdeck-web/src/components/auth/ForgotPasswordForm.test.tsx`

**Interfaces:**
- Consumes: `forgotPassword`, `forgotPasswordSchema`, `ForgotPasswordFormValues`.
- Produces: `ForgotPasswordForm` component; page at `/forgot-password`.

- [ ] **Step 1: Write the form**

`ForgotPasswordForm.tsx`:
```tsx
'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { forgotPassword } from '@/lib/api/auth';
import { forgotPasswordSchema, type ForgotPasswordFormValues } from '@/lib/validation/authSchema';

export function ForgotPasswordForm() {
  const [submitted, setSubmitted] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormValues>({ resolver: zodResolver(forgotPasswordSchema) });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await forgotPassword(values);
      setSubmitted(true);
    } catch {
      setFormError('Could not send the reset link. Please try again.');
    }
  });

  return (
    <Box component="form" onSubmit={onSubmit} sx={{ maxWidth: 400, mx: 'auto', mt: 8 }}>
      <Typography variant="h5" component="h1" gutterBottom>
        Reset your password
      </Typography>
      <Stack spacing={2}>
        {formError ? <Alert severity="error">{formError}</Alert> : null}
        {submitted ? (
          <Alert severity="success">
            If that email exists, a reset link has been sent. Check your inbox.
          </Alert>
        ) : null}
        <TextField
          label="Email"
          type="email"
          {...register('email')}
          error={!!errors.email}
          helperText={errors.email?.message}
        />
        <Button type="submit" variant="contained" disabled={isSubmitting}>
          Send reset link
        </Button>
        <Typography variant="body2">
          Remembered it? <a href="/login">Log in</a>
        </Typography>
      </Stack>
    </Box>
  );
}
```

- [ ] **Step 2: Write the page**

`brewdeck-web/src/app/forgot-password/page.tsx`:
```tsx
import { ForgotPasswordForm } from '@/components/auth/ForgotPasswordForm';

export default function ForgotPasswordPage() {
  return <ForgotPasswordForm />;
}
```

- [ ] **Step 3: Add the login-page link**

In `LoginForm.tsx`, change the trailing register line to also offer the reset link:
```tsx
        <Typography variant="body2">
          No account? <a href="/register">Register</a>
        </Typography>
        <Typography variant="body2">
          <a href="/forgot-password">Forgot password?</a>
        </Typography>
```

- [ ] **Step 4: Write the failing test**

`ForgotPasswordForm.test.tsx`:
```tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ForgotPasswordForm } from './ForgotPasswordForm';

const forgotPasswordMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({
  forgotPassword: (body: unknown) => forgotPasswordMock(body),
}));

describe('ForgotPasswordForm', () => {
  afterEach(() => vi.clearAllMocks());

  it('validates the email field', async () => {
    render(<ForgotPasswordForm />);
    await userEvent.click(screen.getByRole('button', { name: /send reset link/i }));
    expect(await screen.findByText(/email is required/i)).toBeInTheDocument();
    expect(forgotPasswordMock).not.toHaveBeenCalled();
  });

  it('submits the email and shows the generic confirmation', async () => {
    forgotPasswordMock.mockResolvedValue({ message: 'ok' });
    render(<ForgotPasswordForm />);
    await userEvent.type(screen.getByLabelText(/email/i), 'a@b.com');
    await userEvent.click(screen.getByRole('button', { name: /send reset link/i }));
    await waitFor(() => expect(forgotPasswordMock).toHaveBeenCalledWith({ email: 'a@b.com' }));
    expect(await screen.findByText(/if that email exists/i)).toBeInTheDocument();
  });

  it('shows an error alert on failure', async () => {
    forgotPasswordMock.mockRejectedValue(new Error('network'));
    render(<ForgotPasswordForm />);
    await userEvent.type(screen.getByLabelText(/email/i), 'a@b.com');
    await userEvent.click(screen.getByRole('button', { name: /send reset link/i }));
    expect(await screen.findByText(/could not send the reset link/i)).toBeInTheDocument();
  });
});
```

- [ ] **Step 5: Run the test**

Run: `cd brewdeck-web && npm run test -- src/components/auth/ForgotPasswordForm.test.tsx`
Expected: 3 passing.

- [ ] **Step 6: Commit**

```bash
git add brewdeck-web/src/components/auth/ForgotPasswordForm.tsx \
  brewdeck-web/src/app/forgot-password/page.tsx \
  brewdeck-web/src/components/auth/LoginForm.tsx \
  brewdeck-web/src/components/auth/ForgotPasswordForm.test.tsx
git commit -m "feat(web): add forgot-password page and form (Slice C.2)"
```

---

### Task 10: Reset-password page, form, and public-path allowlist

**Files:**
- Create: `brewdeck-web/src/components/auth/ResetPasswordForm.tsx`
- Create: `brewdeck-web/src/app/reset-password/page.tsx`
- Modify: `brewdeck-web/src/lib/api/client.ts:36` (add reset paths to the `onPublic` guard)
- Test: `brewdeck-web/src/components/auth/ResetPasswordForm.test.tsx`

**Interfaces:**
- Consumes: `resetPassword`, `resetPasswordSchema`, `ResetPasswordFormValues`, `ApiError`, Next.js `useSearchParams`.
- Produces: `ResetPasswordForm` component; page at `/reset-password`.

- [ ] **Step 1: Write the form**

`ResetPasswordForm.tsx`:
```tsx
'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useSearchParams } from 'next/navigation';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { resetPassword } from '@/lib/api/auth';
import { ApiError } from '@/lib/api/client';
import { resetPasswordSchema, type ResetPasswordFormValues } from '@/lib/validation/authSchema';

export function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const [done, setDone] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormValues>({ resolver: zodResolver(resetPasswordSchema) });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    if (!token) {
      setFormError('This reset link is invalid or has expired.');
      return;
    }
    try {
      await resetPassword({ token, newPassword: values.newPassword });
      setDone(true);
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        setFormError('This reset link is invalid or has expired.');
        return;
      }
      setFormError('Could not reset your password. Please try again.');
    }
  });

  if (done) {
    return (
      <Box sx={{ maxWidth: 400, mx: 'auto', mt: 8 }}>
        <Alert severity="success">
          Your password has been reset. <a href="/login">Log in</a>
        </Alert>
      </Box>
    );
  }

  return (
    <Box component="form" onSubmit={onSubmit} sx={{ maxWidth: 400, mx: 'auto', mt: 8 }}>
      <Typography variant="h5" component="h1" gutterBottom>
        Choose a new password
      </Typography>
      <Stack spacing={2}>
        {formError ? <Alert severity="error">{formError}</Alert> : null}
        <TextField
          label="New password"
          type="password"
          {...register('newPassword')}
          error={!!errors.newPassword}
          helperText={errors.newPassword?.message}
        />
        <TextField
          label="Confirm new password"
          type="password"
          {...register('confirmPassword')}
          error={!!errors.confirmPassword}
          helperText={errors.confirmPassword?.message}
        />
        <Button type="submit" variant="contained" disabled={isSubmitting}>
          Reset password
        </Button>
      </Stack>
    </Box>
  );
}
```

- [ ] **Step 2: Write the page**

`brewdeck-web/src/app/reset-password/page.tsx` (wrap in `Suspense` — `useSearchParams` requires it):
```tsx
import { Suspense } from 'react';
import { ResetPasswordForm } from '@/components/auth/ResetPasswordForm';

export default function ResetPasswordPage() {
  return (
    <Suspense>
      <ResetPasswordForm />
    </Suspense>
  );
}
```

- [ ] **Step 3: Add reset paths to the public guard**

In `brewdeck-web/src/lib/api/client.ts`, update the `onPublic` line so a 401 does not force-redirect from the reset pages:
```typescript
      const onPublic =
        p === '/login' ||
        p === '/register' ||
        p === '/forgot-password' ||
        p === '/reset-password' ||
        p.startsWith('/share');
```

- [ ] **Step 4: Write the failing test**

`ResetPasswordForm.test.tsx`:
```tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/lib/api/client';
import { ResetPasswordForm } from './ResetPasswordForm';

const resetPasswordMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({
  resetPassword: (body: unknown) => resetPasswordMock(body),
}));

let tokenValue: string | null = 'valid-token';
vi.mock('next/navigation', () => ({
  useSearchParams: () => ({ get: () => tokenValue }),
}));

describe('ResetPasswordForm', () => {
  afterEach(() => {
    vi.clearAllMocks();
    tokenValue = 'valid-token';
  });

  it('requires a new password of at least 8 characters', async () => {
    render(<ResetPasswordForm />);
    await userEvent.type(screen.getByLabelText(/^new password$/i), 'short');
    await userEvent.type(screen.getByLabelText(/^confirm new password$/i), 'short');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));
    expect(await screen.findByText(/at least 8 characters/i)).toBeInTheDocument();
    expect(resetPasswordMock).not.toHaveBeenCalled();
  });

  it('flags mismatched confirmation', async () => {
    render(<ResetPasswordForm />);
    await userEvent.type(screen.getByLabelText(/^new password$/i), 'newpassword1');
    await userEvent.type(screen.getByLabelText(/^confirm new password$/i), 'different1');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));
    expect(await screen.findByText(/passwords do not match/i)).toBeInTheDocument();
  });

  it('submits the token and new password, then shows success', async () => {
    resetPasswordMock.mockResolvedValue(undefined);
    render(<ResetPasswordForm />);
    await userEvent.type(screen.getByLabelText(/^new password$/i), 'newpassword1');
    await userEvent.type(screen.getByLabelText(/^confirm new password$/i), 'newpassword1');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));
    await waitFor(() =>
      expect(resetPasswordMock).toHaveBeenCalledWith({
        token: 'valid-token',
        newPassword: 'newpassword1',
      }),
    );
    expect(await screen.findByText(/your password has been reset/i)).toBeInTheDocument();
  });

  it('shows invalid-link message on a 400 response', async () => {
    resetPasswordMock.mockRejectedValue(new ApiError(400, 'bad', '/api/auth/reset-password'));
    render(<ResetPasswordForm />);
    await userEvent.type(screen.getByLabelText(/^new password$/i), 'newpassword1');
    await userEvent.type(screen.getByLabelText(/^confirm new password$/i), 'newpassword1');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));
    expect(await screen.findByText(/invalid or has expired/i)).toBeInTheDocument();
  });
});
```

- [ ] **Step 5: Run the test**

Run: `cd brewdeck-web && npm run test -- src/components/auth/ResetPasswordForm.test.tsx`
Expected: 4 passing.

- [ ] **Step 6: Commit**

```bash
git add brewdeck-web/src/components/auth/ResetPasswordForm.tsx \
  brewdeck-web/src/app/reset-password/page.tsx \
  brewdeck-web/src/lib/api/client.ts \
  brewdeck-web/src/components/auth/ResetPasswordForm.test.tsx
git commit -m "feat(web): add reset-password page and form (Slice C.2)"
```

---

### Task 11: Full verification, project-state, and roadmap

**Files:**
- Modify: `.claude/project-state.md`
- Modify: `.claude/roadmap.md:Phase 6 C.2 line`

**Interfaces:**
- Consumes: nothing; bookkeeping.

- [ ] **Step 1: Run the full frontend suite**

Run: `cd brewdeck-web && npm run test && npm run type-check && npm run lint && npm run build`
Expected: all green (full vitest run — a shared file, `LoginForm.tsx` and `client.ts`, changed, so run the whole suite, not just the new files).

- [ ] **Step 2: Run the full backend verify + PMD**

Run: `cd brewdeck-api && sh mvnw spotless:apply && sh mvnw clean verify && sh mvnw pmd:check`
Expected: BUILD SUCCESS, PMD clean.

- [ ] **Step 3: Update roadmap**

In `.claude/roadmap.md`, change the C.2 line to Done:
```
  - C.2 (password reset) — hashed single-use tokens (Flyway V9), PasswordResetMailPort (logging default, SMTP stub behind brewdeck.mail.enabled), public POST /api/auth/forgot-password (always 200, no enumeration) + POST /api/auth/reset-password (204), /forgot-password + /reset-password pages — Done
```

- [ ] **Step 4: Update project-state**

In `.claude/project-state.md`, bump `Last Updated` to the implementation date, add a "Recently Worked On" bullet summarizing C.2 (endpoints, mail port, token model, frontend pages, test counts), and change the Immediate Next Steps item to point at C.3 (email verification).

- [ ] **Step 5: Commit**

```bash
git add .claude/project-state.md .claude/roadmap.md
git commit -m "docs: record Slice C.2 password reset in project-state and roadmap"
```

- [ ] **Step 6: Push and open the PR**

```bash
git push -u origin feature/auth-password-reset
gh pr create --base develop --title "feat: password reset (Slice C.2)" --body "Slice C.2 — password reset via hashed single-use tokens and a mail port. See docs/superpowers/plans/2026-07-11-auth-password-reset.md and the design spec."
```

---

## Self-Review Notes

- **Spec coverage:** invariants 1–7 → Tasks 4 (no-enumeration, hash, single-use, TTL, token gen), 3+5 (unified 400, public matchers), 3 (password validation). Mail port + adapters → Task 2. Table → Task 1. Endpoints → Task 5. Frontend pages → Tasks 9–10. Docs/Postman/.env → Task 7. State/roadmap → Task 11. No gaps.
- **Type consistency:** `requestReset`/`resetPassword` (service) match the controller calls; `PasswordResetMailPort.sendResetLink(String, String)` used identically in adapters, service, and the spied integration test; `findByTokenHash`/`findByUserIdAndUsedAtIsNull` defined in Task 1 and consumed in Task 4; frontend `forgotPassword`/`resetPassword` and the two schemas defined in Task 8 and consumed in Tasks 9–10.
- **Anchored label regexes** in the reset-password test avoid the `/new password/` ↔ `/confirm new password/` ambiguity hit in C.1.
- **`useSearchParams` Suspense** boundary added in the reset page (Next.js App Router requirement) to keep `npm run build` green.
