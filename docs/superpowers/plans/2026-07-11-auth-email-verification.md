# Email Verification (Slice C.3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Soft-gate email verification — issue a hashed single-use token on registration, email a verify link, expose `emailVerified` on the user, and let unverified users resend; nothing is blocked.

**Architecture:** New `auth.verification` package parallel to `auth.reset`. An `email_verification_tokens` table (stores only `SHA-256(token)` hex, single-use `used_at`, 24h `expires_at`) plus a boolean `email_verified` on `users` (existing rows backfilled true). `EmailVerificationService` issues/verifies/resends; `AuthService.register` calls `issueFor` after saving. Delivery goes through a new `EmailVerificationMailPort` (logging default, SMTP stub) that reuses the C.2 `MailProperties` / `brewdeck.mail` toggle. Public `POST /api/auth/verify-email`, authenticated `POST /api/auth/resend-verification`. Frontend adds a warning banner and a public `/verify-email` page.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Data JPA, Flyway, PostgreSQL, JUnit 5, Mockito, MockMvc, Testcontainers; Next.js App Router, React 19, TypeScript, MUI, React Hook Form + Zod, Vitest.

## Global Constraints

- Package base `com.brewdeck.brewdeck_api`; new backend code under `auth.verification`.
- **Soft gate:** login and all routes are unaffected by verification status. Only `/me` exposes the flag + the frontend shows a banner.
- **Store only the token hash** (hex `SHA-256`, `VARCHAR(64)`, UNIQUE); the raw token leaves the backend only via the mail port.
- **Single-use** (`used_at`) + **24-hour TTL** (`expires_at`). Unknown/used/expired all throw `InvalidVerificationTokenException` → one generic `400` "Verification token is invalid or has expired".
- Token generation mirrors `PasswordResetService`: `SecureRandom` 32 bytes → `Base64.getUrlEncoder().withoutPadding()`; hash via `MessageDigest` SHA-256 → `HexFormat.of().formatHex(...)`.
- **Backfill:** V10 adds `email_verified BOOLEAN NOT NULL DEFAULT false` then `UPDATE users SET email_verified = true` (existing/seed users grandfathered). New registrations start `false` (entity default).
- **Resend does not enumerate:** `resend-verification` is authenticated (acts on the principal only); already-verified is a silent no-op returning the same 200.
- **Registration must not fail if mail sending throws:** `issueFor` persists the token, then sends best-effort (mail exception caught + logged). The logging adapter never throws.
- Reuse the existing `MailProperties` bean from `com.brewdeck.brewdeck_api.auth.reset` — import it, do not duplicate. Reuse the existing `brewdeck.mail` config block (no application.yaml change needed).
- Validation messages: no special characters.
- Commits: Conventional Commits. Backend `api`, frontend `web`, docs `docs`.
- Verify backend: `./mvnw spotless:apply && ./mvnw clean verify` then `sh mvnw pmd:check`. The wrapper is not executable via `./mvnw` in this shell — run `sh mvnw ...` from inside `brewdeck-api/`.
- Verify frontend (in `brewdeck-web/`): `npm run test`, `npm run type-check`, `npm run lint`, `npm run build`.
- `@ConfigurationPropertiesScan` is already on `BrewdeckApiApplication`.

---

### Task 1: `email_verified` on users, token table, entity, repository, and `UserResponse`

**Files:**
- Create: `brewdeck-api/src/main/resources/db/migration/V10__add_email_verification.sql`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/User.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/UserResponse.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationToken.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationTokenRepository.java`
- Modify: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/AuthControllerTest.java` (the `new UserResponse(...)` call gains the boolean arg)
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationTokenRepositoryTest.java`

**Interfaces:**
- Produces: `User.isEmailVerified()` / `setEmailVerified(boolean)`; `UserResponse(Long id, String email, String displayName, boolean emailVerified, LocalDateTime createdAt)`; `EmailVerificationToken` (fields `id, userId, tokenHash, expiresAt, usedAt, createdAt`, Lombok builder); `EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long>` with `Optional<EmailVerificationToken> findByTokenHash(String)` and `List<EmailVerificationToken> findByUserIdAndUsedAtIsNull(Long)`.

- [ ] **Step 1: Write the migration**

`V10__add_email_verification.sql`:
```sql
-- Slice C.3: email verification.
-- Existing rows (and the seed/test users) predate verification, so grandfather them verified.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false;
UPDATE users SET email_verified = true;

-- Single-use, time-limited verification tokens. Only the SHA-256 hash is stored.
CREATE TABLE email_verification_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens (user_id);
```

- [ ] **Step 2: Add the `emailVerified` field to `User`**

In `User.java`, after the `displayName` column, add:
```java
  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;
```

- [ ] **Step 3: Add `emailVerified` to `UserResponse`**

Replace the record in `UserResponse.java` with:
```java
public record UserResponse(
    Long id, String email, String displayName, boolean emailVerified, LocalDateTime createdAt) {
  public static UserResponse fromEntity(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getDisplayName(),
        user.isEmailVerified(),
        user.getCreatedAt());
  }
}
```

- [ ] **Step 4: Fix the one existing test that constructs `UserResponse` directly**

In `AuthControllerTest.java`, the `updateProfile_returns200WithUpdatedName` test builds a
`UserResponse`. Add the `emailVerified` arg (position 4, before `createdAt`):
```java
        .thenReturn(
            new UserResponse(
                1L,
                "brewer@example.com",
                "Barista Bob",
                true,
                LocalDateTime.parse("2026-07-09T00:00")));
```

- [ ] **Step 5: Write the entity**

`EmailVerificationToken.java`:
```java
package com.brewdeck.brewdeck_api.auth.verification;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "email_verification_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken {

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

- [ ] **Step 6: Write the repository**

`EmailVerificationTokenRepository.java`:
```java
package com.brewdeck.brewdeck_api.auth.verification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationToken, Long> {

  Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

  List<EmailVerificationToken> findByUserIdAndUsedAtIsNull(Long userId);
}
```

- [ ] **Step 7: Write the failing repository test**

`EmailVerificationTokenRepositoryTest.java`:
```java
package com.brewdeck.brewdeck_api.auth.verification;

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
class EmailVerificationTokenRepositoryTest extends PostgresIntegrationTest {

  @Autowired private EmailVerificationTokenRepository tokenRepository;
  @Autowired private UserRepository userRepository;

  private User persistUser() {
    return userRepository.save(
        User.builder()
            .email("verify-" + System.nanoTime() + "@example.com")
            .passwordHash("hash")
            .createdAt(LocalDateTime.now())
            .build());
  }

  @Test
  void findByTokenHash_returnsToken() {
    User user = persistUser();
    tokenRepository.save(
        EmailVerificationToken.builder()
            .userId(user.getId())
            .tokenHash("hash-" + System.nanoTime())
            .expiresAt(LocalDateTime.now().plusHours(24))
            .createdAt(LocalDateTime.now())
            .build());
    String hash = tokenRepository.findAll().get(0).getTokenHash();

    assertThat(tokenRepository.findByTokenHash(hash)).isPresent();
    assertThat(tokenRepository.findByTokenHash("missing")).isEmpty();
  }

  @Test
  void findByUserIdAndUsedAtIsNull_excludesUsedTokens() {
    User user = persistUser();
    tokenRepository.save(
        EmailVerificationToken.builder()
            .userId(user.getId())
            .tokenHash("unused-" + System.nanoTime())
            .expiresAt(LocalDateTime.now().plusHours(24))
            .createdAt(LocalDateTime.now())
            .build());
    tokenRepository.save(
        EmailVerificationToken.builder()
            .userId(user.getId())
            .tokenHash("used-" + System.nanoTime())
            .expiresAt(LocalDateTime.now().plusHours(24))
            .usedAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build());

    assertThat(tokenRepository.findByUserIdAndUsedAtIsNull(user.getId())).hasSize(1);
  }
}
```

- [ ] **Step 8: Run the test**

Run: `cd brewdeck-api && sh mvnw -Dtest=EmailVerificationTokenRepositoryTest test`
Expected: PASS (2 tests; Flyway applies V10 on the Testcontainer).

- [ ] **Step 9: Spotless + commit**

```bash
cd brewdeck-api && sh mvnw spotless:apply && cd ..
git add brewdeck-api/src/main/resources/db/migration/V10__add_email_verification.sql \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/User.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/UserResponse.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationToken.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationTokenRepository.java \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/AuthControllerTest.java \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationTokenRepositoryTest.java
git commit -m "feat(api): add email_verified flag and verification token table (Slice C.3)"
```

---

### Task 2: Email-verification mail port with logging + stub SMTP adapters

**Files:**
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationMailPort.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/LoggingEmailVerificationMailAdapter.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/SmtpEmailVerificationMailAdapter.java`
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/verification/LoggingEmailVerificationMailAdapterTest.java`

**Interfaces:**
- Consumes: existing `com.brewdeck.brewdeck_api.auth.reset.MailProperties` (record `(boolean enabled, String frontendBaseUrl)`).
- Produces: `EmailVerificationMailPort` with `void sendVerificationLink(String email, String rawToken)`.

- [ ] **Step 1: Write the port**

`EmailVerificationMailPort.java`:
```java
package com.brewdeck.brewdeck_api.auth.verification;

/** Delivers an email-verification link. Swap adapters via {@code brewdeck.mail.enabled}. */
public interface EmailVerificationMailPort {
  void sendVerificationLink(String email, String rawToken);
}
```

- [ ] **Step 2: Write the default logging adapter**

`LoggingEmailVerificationMailAdapter.java`:
```java
package com.brewdeck.brewdeck_api.auth.verification;

import com.brewdeck.brewdeck_api.auth.reset.MailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default adapter used whenever {@code brewdeck.mail.enabled} is false or absent. Logs the
 * verification link instead of sending an email, so the flow works with no SMTP dependency.
 */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "brewdeck.mail",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class LoggingEmailVerificationMailAdapter implements EmailVerificationMailPort {

  private final MailProperties properties;

  public LoggingEmailVerificationMailAdapter(MailProperties properties) {
    this.properties = properties;
  }

  @Override
  public void sendVerificationLink(String email, String rawToken) {
    log.info(
        "Email verification link for {}: {}/verify-email?token={}",
        email,
        properties.frontendBaseUrl(),
        rawToken);
  }
}
```

- [ ] **Step 3: Write the stub SMTP adapter**

`SmtpEmailVerificationMailAdapter.java`:
```java
package com.brewdeck.brewdeck_api.auth.verification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Activated by {@code brewdeck.mail.enabled=true}. Placeholder for a real JavaMailSender
 * integration; wiring an actual SMTP provider is a documented follow-up.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "brewdeck.mail", name = "enabled", havingValue = "true")
public class SmtpEmailVerificationMailAdapter implements EmailVerificationMailPort {

  @Override
  public void sendVerificationLink(String email, String rawToken) {
    // TODO(C.3 follow-up): send a real transactional email via JavaMailSender.
    log.warn("SMTP mail adapter enabled but not implemented; verification link for {} not sent",
        email);
    throw new UnsupportedOperationException("SMTP verification delivery is not implemented yet");
  }
}
```

- [ ] **Step 4: Write the failing adapter test**

`LoggingEmailVerificationMailAdapterTest.java`:
```java
package com.brewdeck.brewdeck_api.auth.verification;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.brewdeck.brewdeck_api.auth.reset.MailProperties;
import org.junit.jupiter.api.Test;

class LoggingEmailVerificationMailAdapterTest {

  @Test
  void sendVerificationLink_logsWithoutThrowing() {
    LoggingEmailVerificationMailAdapter adapter =
        new LoggingEmailVerificationMailAdapter(new MailProperties(false, "http://localhost:3000"));

    assertThatCode(() -> adapter.sendVerificationLink("brewer@example.com", "raw-token"))
        .doesNotThrowAnyException();
  }
}
```

- [ ] **Step 5: Run the test**

Run: `cd brewdeck-api && sh mvnw -Dtest=LoggingEmailVerificationMailAdapterTest test`
Expected: PASS.

- [ ] **Step 6: Spotless + commit**

```bash
cd brewdeck-api && sh mvnw spotless:apply && cd ..
git add brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationMailPort.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/LoggingEmailVerificationMailAdapter.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/SmtpEmailVerificationMailAdapter.java \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/verification/LoggingEmailVerificationMailAdapterTest.java
git commit -m "feat(api): add email-verification mail port with logging and stub SMTP adapters (Slice C.3)"
```

---

### Task 3: Request record and invalid-token exception mapping

**Files:**
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/VerifyEmailRequest.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/InvalidVerificationTokenException.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/error/GlobalExceptionHandler.java`

**Interfaces:**
- Produces: `VerifyEmailRequest(String token)`; `InvalidVerificationTokenException(String)` → HTTP 400.

- [ ] **Step 1: Write the request record**

`VerifyEmailRequest.java`:
```java
package com.brewdeck.brewdeck_api.auth.verification;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(@NotBlank String token) {}
```

- [ ] **Step 2: Write the exception**

`InvalidVerificationTokenException.java`:
```java
package com.brewdeck.brewdeck_api.auth.verification;

/** Raised when a verification token is unknown, already used, or expired. */
public class InvalidVerificationTokenException extends RuntimeException {
  public InvalidVerificationTokenException(String message) {
    super(message);
  }
}
```

- [ ] **Step 3: Map the exception to 400**

In `GlobalExceptionHandler.java`, add the import next to the other `auth` imports:
```java
import com.brewdeck.brewdeck_api.auth.verification.InvalidVerificationTokenException;
```
Add this handler immediately above the `handleInvalidResetToken` handler:
```java
  @ExceptionHandler(InvalidVerificationTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidVerificationToken(
      InvalidVerificationTokenException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Verification token is invalid or has expired",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.badRequest().body(errorResponse);
  }
```

- [ ] **Step 4: Compile**

Run: `cd brewdeck-api && sh mvnw -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Spotless + commit**

```bash
cd brewdeck-api && sh mvnw spotless:apply && cd ..
git add brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/VerifyEmailRequest.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/InvalidVerificationTokenException.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/error/GlobalExceptionHandler.java
git commit -m "feat(api): add verify-email request record and invalid-token mapping (Slice C.3)"
```

---

### Task 4: `EmailVerificationService` (issue / verify / resend)

**Files:**
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationService.java`
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationServiceTest.java`

**Interfaces:**
- Consumes: `EmailVerificationTokenRepository`, `EmailVerificationMailPort`, existing `UserRepository`, `EmailVerificationToken`, `InvalidVerificationTokenException`, `User`.
- Produces: `EmailVerificationService` with `void issueFor(User user)`, `void verify(String rawToken)`, `void resendFor(String email)`.

- [ ] **Step 1: Write the service**

`EmailVerificationService.java`:
```java
package com.brewdeck.brewdeck_api.auth.verification;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class EmailVerificationService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int TOKEN_BYTES = 32;
  private static final int TTL_HOURS = 24;

  private final EmailVerificationTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final EmailVerificationMailPort mailPort;

  public EmailVerificationService(
      EmailVerificationTokenRepository tokenRepository,
      UserRepository userRepository,
      EmailVerificationMailPort mailPort) {
    this.tokenRepository = tokenRepository;
    this.userRepository = userRepository;
    this.mailPort = mailPort;
  }

  /** Issues a fresh verification token for the user and sends the link best-effort. */
  @Transactional
  public void issueFor(User user) {
    List<EmailVerificationToken> outstanding =
        tokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
    LocalDateTime now = LocalDateTime.now();
    outstanding.forEach(token -> token.setUsedAt(now));
    tokenRepository.saveAll(outstanding);

    String rawToken = generateRawToken();
    tokenRepository.save(
        EmailVerificationToken.builder()
            .userId(user.getId())
            .tokenHash(hash(rawToken))
            .expiresAt(now.plusHours(TTL_HOURS))
            .createdAt(now)
            .build());

    // Best-effort: a mail failure must not fail registration or resend.
    try {
      mailPort.sendVerificationLink(user.getEmail(), rawToken);
    } catch (RuntimeException e) {
      log.warn("Failed to send verification email for user id={}: {}", user.getId(), e.toString());
    }
    log.info("Issued email verification token for user id={}", user.getId());
  }

  @Transactional
  public void verify(String rawToken) {
    EmailVerificationToken token =
        tokenRepository
            .findByTokenHash(hash(rawToken))
            .orElseThrow(() -> new InvalidVerificationTokenException("Unknown verification token"));

    if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new InvalidVerificationTokenException("Verification token used or expired");
    }

    User user =
        userRepository
            .findById(token.getUserId())
            .orElseThrow(
                () -> new InvalidVerificationTokenException("Verification token has no user"));

    user.setEmailVerified(true);
    userRepository.save(user);

    token.setUsedAt(LocalDateTime.now());
    tokenRepository.save(token);
    log.info("Email verified for user id={}", user.getId());
  }

  @Transactional
  public void resendFor(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    if (user.isEmailVerified()) {
      log.info("Resend requested for already-verified user id={}; no-op", user.getId());
      return;
    }
    issueFor(user);
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

`EmailVerificationServiceTest.java`:
```java
package com.brewdeck.brewdeck_api.auth.verification;

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

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

  @Mock private EmailVerificationTokenRepository tokenRepository;
  @Mock private UserRepository userRepository;
  @Mock private EmailVerificationMailPort mailPort;

  private EmailVerificationService service;

  @BeforeEach
  void setUp() {
    service = new EmailVerificationService(tokenRepository, userRepository, mailPort);
  }

  private User user(boolean verified) {
    return User.builder()
        .id(1L)
        .email("brewer@example.com")
        .passwordHash("hash")
        .emailVerified(verified)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  void issueFor_persistsHashedTokenAndSendsLink() {
    when(tokenRepository.findByUserIdAndUsedAtIsNull(1L)).thenReturn(List.of());

    service.issueFor(user(false));

    ArgumentCaptor<EmailVerificationToken> tokenCaptor =
        ArgumentCaptor.forClass(EmailVerificationToken.class);
    verify(tokenRepository).save(tokenCaptor.capture());
    ArgumentCaptor<String> rawCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailPort).sendVerificationLink(eq("brewer@example.com"), rawCaptor.capture());

    EmailVerificationToken saved = tokenCaptor.getValue();
    assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo(rawCaptor.getValue());
    assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));
  }

  @Test
  void issueFor_stillPersistsTokenWhenMailThrows() {
    when(tokenRepository.findByUserIdAndUsedAtIsNull(1L)).thenReturn(List.of());
    org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
        .when(mailPort)
        .sendVerificationLink(anyString(), anyString());

    // Must not propagate — registration/resend rely on this.
    service.issueFor(user(false));

    verify(tokenRepository).save(any(EmailVerificationToken.class));
  }

  @Test
  void verify_validToken_setsEmailVerifiedAndStampsUsed() {
    EmailVerificationToken token =
        EmailVerificationToken.builder()
            .id(5L)
            .userId(1L)
            .tokenHash("hash")
            .expiresAt(LocalDateTime.now().plusHours(1))
            .createdAt(LocalDateTime.now())
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
    User user = user(false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    service.verify("any-raw-token");

    assertThat(user.isEmailVerified()).isTrue();
    assertThat(token.getUsedAt()).isNotNull();
  }

  @Test
  void verify_unknownToken_throws() {
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.verify("nope"))
        .isInstanceOf(InvalidVerificationTokenException.class);
  }

  @Test
  void verify_expiredToken_throws() {
    EmailVerificationToken token =
        EmailVerificationToken.builder()
            .id(6L)
            .userId(1L)
            .tokenHash("hash")
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .createdAt(LocalDateTime.now().minusHours(25))
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> service.verify("raw"))
        .isInstanceOf(InvalidVerificationTokenException.class);
  }

  @Test
  void verify_usedToken_throws() {
    EmailVerificationToken token =
        EmailVerificationToken.builder()
            .id(7L)
            .userId(1L)
            .tokenHash("hash")
            .expiresAt(LocalDateTime.now().plusHours(1))
            .usedAt(LocalDateTime.now().minusMinutes(1))
            .createdAt(LocalDateTime.now())
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> service.verify("raw"))
        .isInstanceOf(InvalidVerificationTokenException.class);
  }

  @Test
  void resendFor_alreadyVerified_isNoOp() {
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user(true)));

    service.resendFor("brewer@example.com");

    verify(tokenRepository, never()).save(any());
    verify(mailPort, never()).sendVerificationLink(anyString(), anyString());
  }

  @Test
  void resendFor_unverified_issuesToken() {
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user(false)));
    when(tokenRepository.findByUserIdAndUsedAtIsNull(1L)).thenReturn(List.of());

    service.resendFor("brewer@example.com");

    verify(tokenRepository).save(any(EmailVerificationToken.class));
    verify(mailPort).sendVerificationLink(eq("brewer@example.com"), anyString());
  }
}
```

- [ ] **Step 3: Run the test**

Run: `cd brewdeck-api && sh mvnw -Dtest=EmailVerificationServiceTest test`
Expected: PASS (8 tests).

- [ ] **Step 4: Spotless + commit**

```bash
cd brewdeck-api && sh mvnw spotless:apply && cd ..
git add brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationService.java \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationServiceTest.java
git commit -m "feat(api): add EmailVerificationService with hashed single-use tokens (Slice C.3)"
```

---

### Task 5: Register hook — issue a verification token on sign-up

**Files:**
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/AuthService.java` (add `EmailVerificationService` dependency + call `issueFor` in `register`)
- Modify: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/AuthServiceTest.java` (constructor now takes the new dependency; add a mock + two assertions)

**Interfaces:**
- Consumes: `EmailVerificationService.issueFor(User)` (Task 4).
- Produces: `AuthService` constructor signature `AuthService(UserRepository, JwtService, PasswordEncoder, EmailVerificationService)`.

- [ ] **Step 1: Add the dependency and hook to `AuthService`**

In `AuthService.java`, add the import:
```java
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationService;
```
Add the field and constructor param (keep the existing three), replacing the field block and constructor:
```java
  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationService emailVerificationService;

  public AuthService(
      UserRepository userRepository,
      JwtService jwtService,
      PasswordEncoder passwordEncoder,
      EmailVerificationService emailVerificationService) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
    this.emailVerificationService = emailVerificationService;
  }
```
In `register(...)`, after `User saved = userRepository.save(user);` and its log line, add:
```java
    emailVerificationService.issueFor(saved);
```
(so it reads: save → `log.info("Registered user id=...")` → `emailVerificationService.issueFor(saved)` → `return tokenResponse(saved);`)

- [ ] **Step 2: Update `AuthServiceTest` for the new constructor + register behavior**

In `AuthServiceTest.java`:
1. Add a mock field alongside the others:
```java
  @Mock private com.brewdeck.brewdeck_api.auth.verification.EmailVerificationService
      emailVerificationService;
```
2. Update `setUp()` to pass it:
```java
    authService =
        new AuthService(userRepository, jwtService, encoder, emailVerificationService);
```
3. In `register_persistsHashedPasswordAndReturnsToken`, after the existing assertions, add:
```java
    org.mockito.Mockito.verify(emailVerificationService)
        .issueFor(org.mockito.ArgumentMatchers.any(User.class));
```
4. Add a new test proving registration survives a verification failure:
```java
  @Test
  void register_succeedsEvenWhenVerificationIssueThrows() {
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
    org.mockito.Mockito.doThrow(new RuntimeException("verification down"))
        .when(emailVerificationService)
        .issueFor(any(User.class));

    // The account is created and a token returned even if verification issuance fails.
    AuthResponse response =
        authService.register(new RegisterRequest("new@example.com", "password1"));

    assertThat(response.token()).isEqualTo("jwt-token");
  }
```

Note: for the failure test to pass, `register` must not let `issueFor` propagate. `issueFor` already
swallows mail errors internally (Task 4), but this test injects a throw from the mock itself, so
`register` must tolerate it. Wrap the call:
```java
    try {
      emailVerificationService.issueFor(saved);
    } catch (RuntimeException e) {
      log.warn("Failed to issue verification token for user id={}: {}", saved.getId(), e.toString());
    }
```
Use this wrapped form in Step 1 instead of a bare call.

- [ ] **Step 3: Run the test**

Run: `cd brewdeck-api && sh mvnw -Dtest=AuthServiceTest test`
Expected: PASS (existing tests + the two register assertions).

- [ ] **Step 4: Spotless + commit**

```bash
cd brewdeck-api && sh mvnw spotless:apply && cd ..
git add brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/AuthService.java \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/AuthServiceTest.java
git commit -m "feat(api): issue a verification token on registration (Slice C.3)"
```

---

### Task 6: `EmailVerificationController` + public verify-email matcher

**Files:**
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationController.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/config/SecurityConfig.java` (add `/api/auth/verify-email` to the permitAll list)
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationControllerTest.java`

**Interfaces:**
- Consumes: `EmailVerificationService`, `VerifyEmailRequest`, `InvalidVerificationTokenException`, `GlobalExceptionHandler`.
- Produces: `POST /api/auth/verify-email` (204), `POST /api/auth/resend-verification` (200, body `{ "message": ... }`).

- [ ] **Step 1: Write the controller**

`EmailVerificationController.java`:
```java
package com.brewdeck.brewdeck_api.auth.verification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, and account management")
public class EmailVerificationController {

  private static final String RESENT_MESSAGE = "Verification email sent.";

  private final EmailVerificationService emailVerificationService;

  @PostMapping("/verify-email")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Verify an email address using a verification token")
  public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    emailVerificationService.verify(request.token());
  }

  @PostMapping("/resend-verification")
  @Operation(summary = "Resend the verification email for the current user")
  public Map<String, String> resendVerification(Principal principal) {
    emailVerificationService.resendFor(principal.getName());
    return Map.of("message", RESENT_MESSAGE);
  }
}
```

- [ ] **Step 2: Open verify-email in SecurityConfig**

In `SecurityConfig.java`, add `"/api/auth/verify-email"` to the existing register/login/reset
permitAll matcher list (resend-verification stays authenticated — do NOT add it):
```java
                    .requestMatchers(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        "/api/auth/verify-email")
                    .permitAll()
```

- [ ] **Step 3: Write the failing controller test**

`EmailVerificationControllerTest.java`:
```java
package com.brewdeck.brewdeck_api.auth.verification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EmailVerificationControllerTest {

  @Mock private EmailVerificationService emailVerificationService;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new EmailVerificationController(emailVerificationService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void verifyEmail_returns204() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new VerifyEmailRequest("raw-token"))))
        .andExpect(status().isNoContent());
  }

  @Test
  void verifyEmail_invalidTokenReturns400() throws Exception {
    doThrow(new InvalidVerificationTokenException("bad"))
        .when(emailVerificationService)
        .verify(any());

    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new VerifyEmailRequest("raw-token"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void verifyEmail_blankTokenReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new VerifyEmailRequest(""))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void resendVerification_returns200WithMessage() throws Exception {
    Principal principal = () -> "brewer@example.com";

    mockMvc
        .perform(post("/api/auth/resend-verification").principal(principal))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Verification email sent."));
  }
}
```

- [ ] **Step 4: Run the test**

Run: `cd brewdeck-api && sh mvnw -Dtest=EmailVerificationControllerTest test`
Expected: PASS (4 tests).

- [ ] **Step 5: Spotless + commit**

```bash
cd brewdeck-api && sh mvnw spotless:apply && cd ..
git add brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationController.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/config/SecurityConfig.java \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/verification/EmailVerificationControllerTest.java
git commit -m "feat(api): add verify-email and resend-verification endpoints (Slice C.3)"
```

---

### Task 7: End-to-end integration test (Testcontainers)

**Files:**
- Create: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/integration/EmailVerificationIntegrationTest.java`

**Interfaces:**
- Consumes: full Spring context, `EmailVerificationMailPort` (spied), `UserRepository`, `EmailVerificationTokenRepository`, `PostgresIntegrationTest` base, existing auth endpoints.

- [ ] **Step 1: Write the integration test**

`EmailVerificationIntegrationTest.java`:
```java
package com.brewdeck.brewdeck_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationMailPort;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationToken;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationTokenRepository;
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
class EmailVerificationIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private EmailVerificationTokenRepository tokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @MockitoSpyBean private EmailVerificationMailPort mailPort;

  @Test
  void registerThenVerify_flipsEmailVerified() throws Exception {
    String email = "verify-flow-" + System.nanoTime() + "@example.com";

    String registerResponse =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType("application/json")
                    .content("{\"email\":\"" + email + "\",\"password\":\"password1\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = com.jayway.jsonpath.JsonPath.read(registerResponse, "$.token");

    // Freshly registered user starts unverified.
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(false));

    // The verification token was mailed on registration; capture it.
    ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(mailPort)
        .sendVerificationLink(org.mockito.ArgumentMatchers.eq(email), rawToken.capture());
    String verifyToken = rawToken.getValue();

    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType("application/json")
                .content("{\"token\":\"" + verifyToken + "\"}"))
        .andExpect(status().isNoContent());

    // Now verified.
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(true));

    // Single-use: replay -> 400.
    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType("application/json")
                .content("{\"token\":\"" + verifyToken + "\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void resendVerification_authenticated_issuesNewToken() throws Exception {
    String email = "verify-resend-" + System.nanoTime() + "@example.com";
    String token =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(
                    post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.token");

    mockMvc
        .perform(post("/api/auth/resend-verification").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Verification email sent."));
  }

  @Test
  void verifyEmail_expiredToken_returns400() throws Exception {
    String email = "verify-exp-" + System.nanoTime() + "@example.com";
    User user =
        userRepository.save(
            User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("password1"))
                .createdAt(LocalDateTime.now())
                .build());
    String rawExpired = "expired-verify-token";
    tokenRepository.save(
        EmailVerificationToken.builder()
            .userId(user.getId())
            .tokenHash(sha256Hex(rawExpired))
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .createdAt(LocalDateTime.now().minusHours(25))
            .build());

    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType("application/json")
                .content("{\"token\":\"" + rawExpired + "\"}"))
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

Run: `cd brewdeck-api && sh mvnw -Dtest=EmailVerificationIntegrationTest test`
Expected: PASS (3 tests).

- [ ] **Step 3: Full backend gate**

Run: `cd brewdeck-api && sh mvnw spotless:apply && sh mvnw clean verify && sh mvnw pmd:check`
Expected: BUILD SUCCESS, all tests pass, PMD clean.

Note: if any pre-existing integration test asserts the `/me` body shape or seeds users and now sees
the `emailVerified` field, it should already pass (the field is additive and backfilled true for
seeded users). If a `PostgresIntegrationTest`-seeded user is asserted, `emailVerified` will be
`true` for rows created before this test issues tokens.

- [ ] **Step 4: Commit**

```bash
git add brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/integration/EmailVerificationIntegrationTest.java
git commit -m "test(api): cover email-verification flow end-to-end (Slice C.3)"
```

---

### Task 8: Backend docs — API reference, OpenAPI, Postman

**Files:**
- Modify: `docs/api/README.md` (Auth block)
- Modify: `docs/api/openapi.yaml` (two new paths after `/api/auth/reset-password`)
- Modify: `docs/api/postman/brewdeck.postman_collection.json` (two Auth requests)

**Interfaces:**
- Consumes: nothing; documentation only.

- [ ] **Step 1: Update the API README Auth block**

Replace the Auth code block in `docs/api/README.md` with:
```
POST  /api/auth/register            201
POST  /api/auth/login               200
GET   /api/auth/me                  200 (401 without token; includes emailVerified)
PATCH /api/auth/me                  200 (update display name)
POST  /api/auth/change-password     204 (400 if current password wrong)
POST  /api/auth/forgot-password     200 (always; no user enumeration)
POST  /api/auth/reset-password      204 (400 if token invalid/expired/used)
POST  /api/auth/verify-email        204 (400 if token invalid/expired/used)
POST  /api/auth/resend-verification 200 (authenticated; no-op if already verified)
```

- [ ] **Step 2: Update OpenAPI**

In `docs/api/openapi.yaml`, add after the `/api/auth/reset-password` path:
```yaml
  /api/auth/verify-email:
    post:
      tags: [Auth]
      security: []
      summary: Verify an email address using a verification token
      responses:
        "204": { description: Email verified }
        "400": { description: Invalid/expired/used token or validation error, content: { application/json: { schema: { $ref: "#/components/schemas/ErrorResponse" } } } }
  /api/auth/resend-verification:
    post:
      tags: [Auth]
      summary: Resend the verification email for the authenticated user (no-op if already verified)
      responses:
        "200": { description: OK }
        "401": { description: Unauthenticated }
```

- [ ] **Step 3: Add the two Postman requests**

Run this from the repo root:
```bash
python3 - <<'PY'
import json
p='docs/api/postman/brewdeck.postman_collection.json'
c=json.load(open(p))
auth=[f for f in c['item'] if f['name']=='Auth'][0]
verify={
  "name":"POST - Verify email",
  "request":{
    "auth":{"type":"noauth"},
    "method":"POST",
    "header":[{"key":"Content-Type","value":"application/json"}],
    "body":{"mode":"raw","raw":"{\n  \"token\": \"paste-token-from-logs\"\n}"},
    "url":{"raw":"{{baseURL}}/api/auth/verify-email","host":["{{baseURL}}"],"path":["api","auth","verify-email"]}
  },
  "response":[],
  "event":[{"listen":"test","script":{"type":"text/javascript","exec":[
    "pm.test('Status code is 204', function () {",
    "  pm.response.to.have.status(204);",
    "});"
  ]}}]
}
resend={
  "name":"POST - Resend verification",
  "request":{
    "method":"POST",
    "header":[{"key":"Authorization","value":"Bearer {{authToken}}"}],
    "url":{"raw":"{{baseURL}}/api/auth/resend-verification","host":["{{baseURL}}"],"path":["api","auth","resend-verification"]}
  },
  "response":[],
  "event":[{"listen":"test","script":{"type":"text/javascript","exec":[
    "pm.test('Status code is 200', function () {",
    "  pm.response.to.have.status(200);",
    "});"
  ]}}]
}
auth['item'].extend([verify,resend])
json.dump(c,open(p,'w'),indent=2)
open(p,'a').write('\n')
print('added:',[i['name'] for i in auth['item']])
PY
python3 -c "import json; json.load(open('docs/api/postman/brewdeck.postman_collection.json')); print('JSON_OK')"
```
Expected: prints the request list then `JSON_OK`.

- [ ] **Step 4: Commit**

```bash
git add docs/api/README.md docs/api/openapi.yaml docs/api/postman/brewdeck.postman_collection.json
git commit -m "docs: document email-verification endpoints (Slice C.3)"
```

---

### Task 9: Frontend API client, `emailVerified` type, and AuthProvider refresh

**Files:**
- Modify: `brewdeck-web/src/lib/api/types.ts` (`UserResponse` gains `emailVerified`)
- Modify: `brewdeck-web/src/lib/api/auth.ts` (add `verifyEmail`, `resendVerification`)
- Modify: `brewdeck-web/src/lib/auth/AuthProvider.tsx` (add `refreshUser`)
- Modify: `brewdeck-web/src/lib/auth/AuthProvider.test.tsx` (fixtures gain `emailVerified`)

**Interfaces:**
- Produces: `UserResponse.emailVerified: boolean`; `verifyEmail(token: string): Promise<void>`; `resendVerification(): Promise<{ message: string }>`; `useAuth().refreshUser(): Promise<void>`.

- [ ] **Step 1: Add `emailVerified` to the type**

In `brewdeck-web/src/lib/api/types.ts`, update `UserResponse`:
```typescript
export type UserResponse = {
  id: number;
  email: string;
  displayName: string | null;
  emailVerified: boolean;
  createdAt: string;
};
```

- [ ] **Step 2: Add the API client functions**

In `brewdeck-web/src/lib/api/auth.ts`, append:
```typescript
export function verifyEmail(token: string): Promise<void> {
  return apiFetch<void>('/api/auth/verify-email', {
    method: 'POST',
    body: JSON.stringify({ token }),
  });
}

export function resendVerification(): Promise<{ message: string }> {
  return apiFetch<{ message: string }>('/api/auth/resend-verification', {
    method: 'POST',
  });
}
```

- [ ] **Step 3: Add `refreshUser` to AuthProvider**

In `AuthProvider.tsx`, add `refreshUser` to the context type (after `updateProfile`):
```typescript
  refreshUser: () => Promise<void>;
```
And in the `useMemo` value (after `updateProfile`):
```typescript
      refreshUser: async () => {
        if (!getToken()) {
          return;
        }
        try {
          const me = await getMe();
          setUser(me);
        } catch {
          // Ignore: a failed refresh leaves the existing user state untouched.
        }
      },
```
(`getToken` and `getMe` are already imported in this file.)

- [ ] **Step 4: Fix AuthProvider test fixtures**

In `AuthProvider.test.tsx`, every `getMe` mock returns a user object; add `emailVerified: true` next
to the existing `displayName: null` lines (there are three):
```typescript
      displayName: null,
      emailVerified: true,
      createdAt: '2026-07-01T00:00:00Z',
```

- [ ] **Step 5: Type-check**

Run: `cd brewdeck-web && npm run type-check`
Expected: no errors. (If other test fixtures construct a full `UserResponse` without `emailVerified`,
add `emailVerified: true` to them — search: `grep -rn "displayName:" src` and fix any object literal
typed as `UserResponse` that the compiler flags.)

- [ ] **Step 6: Commit**

```bash
git add brewdeck-web/src/lib/api/types.ts brewdeck-web/src/lib/api/auth.ts \
  brewdeck-web/src/lib/auth/AuthProvider.tsx brewdeck-web/src/lib/auth/AuthProvider.test.tsx
git commit -m "feat(web): add email-verification client, emailVerified type, and refreshUser (Slice C.3)"
```

---

### Task 10: Email-verification banner in the app shell

**Files:**
- Create: `brewdeck-web/src/components/auth/EmailVerificationBanner.tsx`
- Modify: `brewdeck-web/src/components/layout/AppShell.tsx` (render the banner above the main content)
- Test: `brewdeck-web/src/components/auth/EmailVerificationBanner.test.tsx`

**Interfaces:**
- Consumes: `useAuth()` (`user`, `resendVerification` via the api client).
- Produces: `EmailVerificationBanner` component (named export).

- [ ] **Step 1: Write the banner**

`EmailVerificationBanner.tsx`:
```tsx
'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import { useState } from 'react';
import { resendVerification } from '@/lib/api/auth';
import { useAuth } from '@/lib/auth/AuthProvider';

export function EmailVerificationBanner() {
  const { user } = useAuth();
  const [dismissed, setDismissed] = useState(false);
  const [status, setStatus] = useState<'idle' | 'sending' | 'sent' | 'error'>('idle');

  if (!user || user.emailVerified || dismissed) {
    return null;
  }

  const onResend = async () => {
    setStatus('sending');
    try {
      await resendVerification();
      setStatus('sent');
    } catch {
      setStatus('error');
    }
  };

  return (
    <Alert
      severity="warning"
      onClose={() => setDismissed(true)}
      action={
        status === 'sent' ? undefined : (
          <Button color="inherit" size="small" onClick={onResend} disabled={status === 'sending'}>
            Resend link
          </Button>
        )
      }
      sx={{ mb: 2 }}
    >
      {status === 'sent'
        ? 'Verification email sent. Check your inbox.'
        : status === 'error'
          ? 'Could not resend the verification email. Please try again.'
          : 'Your email is not verified. Please check your inbox for the verification link.'}
    </Alert>
  );
}
```

- [ ] **Step 2: Render the banner in AppShell**

In `AppShell.tsx`, import it and render it at the top of the main content. Add the import with the
other internal imports:
```tsx
import { EmailVerificationBanner } from '@/components/auth/EmailVerificationBanner';
```
Then inside the `<Box component="main" ...>`, immediately after the spacer `<Toolbar />`, add the
banner before `{children}`:
```tsx
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Toolbar />
        <EmailVerificationBanner />
        {children}
      </Box>
```

- [ ] **Step 3: Write the failing test**

`EmailVerificationBanner.test.tsx`:
```tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { UserResponse } from '@/lib/api/types';
import { EmailVerificationBanner } from './EmailVerificationBanner';

const resendMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({ resendVerification: () => resendMock() }));

let mockUser: UserResponse | null = null;
vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => ({ user: mockUser }) }));

const unverified: UserResponse = {
  id: 1,
  email: 'brewer@example.com',
  displayName: null,
  emailVerified: false,
  createdAt: '',
};

describe('EmailVerificationBanner', () => {
  afterEach(() => {
    vi.clearAllMocks();
    mockUser = null;
  });

  it('renders nothing when the user is verified', () => {
    mockUser = { ...unverified, emailVerified: true };
    const { container } = render(<EmailVerificationBanner />);
    expect(container).toBeEmptyDOMElement();
  });

  it('shows the warning when unverified', () => {
    mockUser = unverified;
    render(<EmailVerificationBanner />);
    expect(screen.getByText(/email is not verified/i)).toBeInTheDocument();
  });

  it('resends and shows confirmation', async () => {
    mockUser = unverified;
    resendMock.mockResolvedValue({ message: 'ok' });
    render(<EmailVerificationBanner />);
    await userEvent.click(screen.getByRole('button', { name: /resend link/i }));
    await waitFor(() => expect(resendMock).toHaveBeenCalled());
    expect(await screen.findByText(/verification email sent/i)).toBeInTheDocument();
  });

  it('hides when dismissed', async () => {
    mockUser = unverified;
    render(<EmailVerificationBanner />);
    await userEvent.click(screen.getByRole('button', { name: /close/i }));
    expect(screen.queryByText(/email is not verified/i)).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 4: Run the test**

Run: `cd brewdeck-web && npm run test -- src/components/auth/EmailVerificationBanner.test.tsx`
Expected: 4 passing.

- [ ] **Step 5: Commit**

```bash
git add brewdeck-web/src/components/auth/EmailVerificationBanner.tsx \
  brewdeck-web/src/components/layout/AppShell.tsx \
  brewdeck-web/src/components/auth/EmailVerificationBanner.test.tsx
git commit -m "feat(web): add email-verification banner to the app shell (Slice C.3)"
```

---

### Task 11: Public verify-email page + onPublic allowlist

**Files:**
- Create: `brewdeck-web/src/components/auth/VerifyEmailView.tsx`
- Create: `brewdeck-web/src/app/verify-email/page.tsx`
- Modify: `brewdeck-web/src/lib/api/client.ts` (add `/verify-email` to the `onPublic` guard)
- Test: `brewdeck-web/src/components/auth/VerifyEmailView.test.tsx`

**Interfaces:**
- Consumes: `verifyEmail` (Task 9), `ApiError`, `useAuth().refreshUser` (Task 9), Next.js `useSearchParams`.
- Produces: `VerifyEmailView` component; page at `/verify-email`.

- [ ] **Step 1: Write the view**

`VerifyEmailView.tsx`:
```tsx
'use client';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import { useSearchParams } from 'next/navigation';
import { useEffect, useRef, useState } from 'react';
import { verifyEmail } from '@/lib/api/auth';
import { useAuth } from '@/lib/auth/AuthProvider';
import { Spinner } from '@/components/ui/Spinner';

export function VerifyEmailView() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const { refreshUser } = useAuth();
  const [state, setState] = useState<'verifying' | 'success' | 'error'>('verifying');
  const started = useRef(false);

  useEffect(() => {
    if (started.current) {
      return;
    }
    started.current = true;
    if (!token) {
      setState('error');
      return;
    }
    verifyEmail(token)
      .then(async () => {
        setState('success');
        await refreshUser();
      })
      .catch(() => setState('error'));
  }, [token, refreshUser]);

  return (
    <Box sx={{ maxWidth: 400, mx: 'auto', mt: 8 }}>
      {state === 'verifying' ? <Spinner /> : null}
      {state === 'success' ? (
        <Alert severity="success">
          Your email has been verified. <a href="/dashboard">Continue to the app</a>
        </Alert>
      ) : null}
      {state === 'error' ? (
        <Alert severity="error">This verification link is invalid or has expired.</Alert>
      ) : null}
    </Box>
  );
}
```
(Confirm the `Spinner` import path — it lives under `src/components/ui/`. If the export differs,
match the existing usage in `LoginForm`/other views.)

- [ ] **Step 2: Write the page**

`brewdeck-web/src/app/verify-email/page.tsx`:
```tsx
import { Suspense } from 'react';
import { VerifyEmailView } from '@/components/auth/VerifyEmailView';

export default function VerifyEmailPage() {
  return (
    <Suspense>
      <VerifyEmailView />
    </Suspense>
  );
}
```

- [ ] **Step 3: Add `/verify-email` to the public guard**

In `brewdeck-web/src/lib/api/client.ts`, extend the `onPublic` check:
```typescript
      const onPublic =
        p === '/login' ||
        p === '/register' ||
        p === '/forgot-password' ||
        p === '/reset-password' ||
        p === '/verify-email' ||
        p.startsWith('/share');
```

- [ ] **Step 4: Write the failing test**

`VerifyEmailView.test.tsx`:
```tsx
import { render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { VerifyEmailView } from './VerifyEmailView';

const verifyEmailMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({ verifyEmail: (t: string) => verifyEmailMock(t) }));

const refreshUserMock = vi.fn().mockResolvedValue(undefined);
vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => ({ refreshUser: refreshUserMock }) }));

let tokenValue: string | null = 'valid-token';
vi.mock('next/navigation', () => ({ useSearchParams: () => ({ get: () => tokenValue }) }));

describe('VerifyEmailView', () => {
  afterEach(() => {
    vi.clearAllMocks();
    tokenValue = 'valid-token';
  });

  it('verifies the token and shows success', async () => {
    verifyEmailMock.mockResolvedValue(undefined);
    render(<VerifyEmailView />);
    await waitFor(() => expect(verifyEmailMock).toHaveBeenCalledWith('valid-token'));
    expect(await screen.findByText(/your email has been verified/i)).toBeInTheDocument();
  });

  it('shows the invalid-link message on failure', async () => {
    verifyEmailMock.mockRejectedValue(new Error('bad'));
    render(<VerifyEmailView />);
    expect(await screen.findByText(/invalid or has expired/i)).toBeInTheDocument();
  });

  it('shows the invalid-link message when no token is present', async () => {
    tokenValue = null;
    render(<VerifyEmailView />);
    expect(await screen.findByText(/invalid or has expired/i)).toBeInTheDocument();
    expect(verifyEmailMock).not.toHaveBeenCalled();
  });
});
```

- [ ] **Step 5: Run the test**

Run: `cd brewdeck-web && npm run test -- src/components/auth/VerifyEmailView.test.tsx`
Expected: 3 passing.

- [ ] **Step 6: Commit**

```bash
git add brewdeck-web/src/components/auth/VerifyEmailView.tsx \
  brewdeck-web/src/app/verify-email/page.tsx \
  brewdeck-web/src/lib/api/client.ts \
  brewdeck-web/src/components/auth/VerifyEmailView.test.tsx
git commit -m "feat(web): add public verify-email page (Slice C.3)"
```

---

### Task 12: Full verification, project-state, and roadmap

**Files:**
- Modify: `.claude/project-state.md`
- Modify: `.claude/roadmap.md`

**Interfaces:**
- Consumes: nothing; bookkeeping.

- [ ] **Step 1: Run the full frontend suite**

Run: `cd brewdeck-web && npm run test && npm run type-check && npm run lint && npm run build`
Expected: all green. A shared file (`AppShell.tsx`, `AuthProvider.tsx`, `client.ts`, `types.ts`)
changed, so run the WHOLE vitest suite, not just the new files. If any sibling test that renders
`AppShell` now fails for a missing `useAuth` field, mock `resendVerification` / provide
`emailVerified` in that test's user fixture.

- [ ] **Step 2: Run the full backend verify + PMD**

Run: `cd brewdeck-api && sh mvnw spotless:apply && sh mvnw clean verify && sh mvnw pmd:check`
Expected: BUILD SUCCESS, PMD clean.

- [ ] **Step 3: Update roadmap**

In `.claude/roadmap.md`, change the C.3 line to Done:
```
  - C.3 (email verification) — email_verified flag (Flyway V10, existing backfilled verified), hashed single-use 24h tokens (email_verification_tokens), EmailVerificationMailPort reusing the C.2 mail toggle, register-time issue hook, public POST /api/auth/verify-email (204) + authenticated POST /api/auth/resend-verification (200), soft gate (login unaffected; /me exposes emailVerified), frontend banner + /verify-email page — Done
```

- [ ] **Step 4: Update project-state**

In `.claude/project-state.md`: bump `Last Updated`, add a "Recently Worked On" bullet summarizing
C.3 (flag + backfill, token model, mail port reuse, register hook, endpoints, soft gate, frontend
banner + verify page, test counts), and change the Immediate Next Steps item to point at Slice C.4
(refresh tokens).

- [ ] **Step 5: Commit**

```bash
git add .claude/project-state.md .claude/roadmap.md
git commit -m "docs: record Slice C.3 email verification in project-state and roadmap"
```

- [ ] **Step 6: Push and open the PR**

```bash
git push -u origin feature/auth-email-verification
gh pr create --base develop --title "feat: email verification (Slice C.3)" --body "Slice C.3 — soft-gate email verification. See docs/superpowers/plans/2026-07-11-auth-email-verification.md and the design spec."
```

---

## Self-Review Notes

- **Spec coverage:** flag + backfill → Task 1; mail port reuse → Task 2; exception/request → Task 3; service (issue/verify/resend, invariants) → Task 4; register hook + mail-failure-tolerance → Task 5; endpoints + public matcher → Task 6; e2e (verify flips flag, single-use, resend, expired, fresh-vs-backfilled) → Task 7; docs/Postman → Task 8; FE type/client/refresh → Task 9; banner → Task 10; verify page + onPublic → Task 11; state/roadmap → Task 12. No spec requirement without a task.
- **Ripple fixes called out:** `UserResponse` new field breaks `AuthControllerTest` (fixed Task 1 Step 4) and frontend fixtures (Task 9 Step 4 + Task 12 Step 1 sweep); `AuthService` new constructor param breaks `AuthServiceTest` (fixed Task 5 Step 2).
- **Type consistency:** `issueFor(User)`, `verify(String)`, `resendFor(String)` defined in Task 4 and consumed identically in Tasks 5–7; `EmailVerificationMailPort.sendVerificationLink(String,String)` used in adapters (Task 2), service (Task 4), and the spied integration test (Task 7); `UserResponse.emailVerified` (Task 1) surfaced on `/me` and asserted in Task 7 + consumed by the frontend type (Task 9) and banner (Task 10); frontend `verifyEmail`/`resendVerification`/`refreshUser` defined in Task 9, consumed in Tasks 10–11.
- **Security/soft-gate:** login untouched (no task modifies `login`); only `verify-email` added to permitAll (Task 6), `resend-verification` stays authenticated; unknown/used/expired unified 400 (Tasks 3–4); token hash-only storage + entropy mirror C.2.
- **Known gotchas pre-empted:** anchored intent via distinct label text in the banner (no "new password" ambiguity here); `useSearchParams` wrapped in `<Suspense>` (Task 11) for `next build`; verify runs once via a `useRef` guard to avoid double-fire in React strict mode.
```
