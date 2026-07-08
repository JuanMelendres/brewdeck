# Auth Foundation (Slice A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce stateless JWT authentication to BrewDeck — users self-register and log in, receive a bearer token, and every existing `/api/**` endpoint requires it (public share links and auth endpoints excepted); no per-row ownership yet.

**Architecture:** New backend `auth` domain (`User`, `UserRepository`, `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `AuthController`) adds Spring Security as a stateless filter chain. Frontend gains a `tokenStore`, an `AuthProvider` (Context for DI), `apiFetch` bearer injection + 401 handling, `/login` + `/register` pages, and a `RequireAuth` guard on an `(app)` route-group layout — leaving `/login`, `/register`, and `/share/[token]` public.

**Tech Stack:** Java 21 / Spring Boot 3.5 / Spring Security / jjwt 0.12.x / Spring Data JPA / Flyway / PostgreSQL 16 / Testcontainers / JUnit 5 / Mockito / MockMvc / spring-security-test — Next.js 16 App Router / React 19 / TypeScript / MUI / TanStack Query / React Hook Form + Zod / Vitest + RTL.

## Global Constraints

- **Backend:** organize by domain (`auth`); never return entities from controllers — map to records; Bean Validation on inputs; no special symbols like `degrees Celsius` written as `°C`; single-resource GET returns the DTO directly (not `PageResponse`); stateless security (no server session).
- **Token:** HMAC-SHA256 via jjwt 0.12.x; secret from `BREWDECK_JWT_SECRET` (dev/test/local default present; **prod profile has no default → fail-fast**); default TTL 24h (`brewdeck.auth.token-ttl`, ISO-8601 Duration `PT24H`). Secret must be ≥ 32 bytes for HS256.
- **Passwords:** BCrypt; never log, return, or expose `passwordHash`.
- **Public endpoints:** `/api/public/**`, `/api/auth/register`, `/api/auth/login` stay open (`/api/auth/me` is authenticated); the `/share/[token]` page stays outside the route guard.
- **Frontend:** strict TypeScript, no `any`; import domain types from `@/lib/api`; named exports everywhere except Next.js `page.tsx`/`layout.tsx` (`export default`); absolute `@/` imports; TanStack Query for server state (login/register mutations, `getMe` query); React Context only for DI (`AuthProvider`); handle loading/error/empty visibly (no `console.error`); the raw token lives only in `tokenStore`.
- **Coverage parity:** `*Request`/`*Response` records and `**/config/**` are already excluded from JaCoCo + Sonar — put DTOs and `SecurityConfig`/`OpenApiConfig` under those patterns (records in the `auth` package match `*Request`/`*Response`; `SecurityConfig` must live in `common/config` to inherit the `**/config/**` exclusion).
- **Commands:** frontend from `brewdeck-web/` (`npm run test`, `npm run type-check`, `npm run build`; scope `lint:fix` to changed files). Backend `./mvnw spotless:apply` then `./mvnw clean verify` from `brewdeck-api/`.
- **Full `vitest run`** after Tasks 3 and 4 — `apiFetch`, `Providers`, and the root layout are shared; sibling tests mount them (see the sibling-test regression in project memory).
- Conventional Commits; scopes `api` (backend), `web` (frontend), `docs`.

---

### Task 1: Backend identity core (no web security)

Adds token + password + persistence primitives **without** `spring-boot-starter-security`, so Boot does not auto-enable its default gate and the existing suite stays green. `spring-security-crypto` (BCrypt) and `jjwt` are standalone libraries that do **not** trigger web-security auto-configuration.

**Files:**
- Modify: `brewdeck-api/pom.xml` (add `jjwt` 0.12.x trio + `spring-security-crypto`)
- Create: `brewdeck-api/src/main/resources/db/migration/V5__create_users.sql`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/User.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/UserRepository.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/JwtService.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/RegisterRequest.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/LoginRequest.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/AuthResponse.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/UserResponse.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/EmailAlreadyUsedException.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/AuthService.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/config/PasswordEncoderConfig.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/error/GlobalExceptionHandler.java`
- Modify: `brewdeck-api/src/main/resources/application.yaml` (add `brewdeck.auth.*`)
- Modify: `brewdeck-api/src/main/resources/application-prod.yml` (secret, no default)
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/JwtServiceTest.java` (create)
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/AuthServiceTest.java` (create)

**Interfaces:**
- Consumes: `ErrorResponse`, `GlobalExceptionHandler` (add a `@ExceptionHandler` for `EmailAlreadyUsedException`), the Lombok entity pattern from `coffee/Coffee.java`.
- Produces (later tasks rely on these exact signatures):
  - `User` entity — Lombok `@Getter/@Setter/@Builder/@NoArgsConstructor/@AllArgsConstructor`; fields `Long id`, `String email`, `String passwordHash`, `LocalDateTime createdAt`.
  - `UserRepository extends JpaRepository<User, Long>` — `Optional<User> findByEmail(String email)`, `boolean existsByEmail(String email)`.
  - `JwtService` — `String generateToken(User user)`, `String validateAndGetSubject(String token)` (throws `io.jsonwebtoken.JwtException` on invalid/expired), `Instant expiryFor(Instant issuedAt)`.
  - `AuthService` — `AuthResponse register(RegisterRequest request)`, `AuthResponse login(LoginRequest request)`, `UserResponse me(String email)`.
  - `RegisterRequest(String email, String password)`, `LoginRequest(String email, String password)`, `AuthResponse(String token, Instant expiresAt, String email)`, `UserResponse(Long id, String email, LocalDateTime createdAt)`.
  - `EmailAlreadyUsedException extends RuntimeException`.

- [ ] **Step 1: Add dependencies to `pom.xml`**

In the `<dependencies>` block (next to `spring-boot-starter-validation`), add:

```xml
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-crypto</artifactId>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-api</artifactId>
			<version>0.12.6</version>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-impl</artifactId>
			<version>0.12.6</version>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-jackson</artifactId>
			<version>0.12.6</version>
			<scope>runtime</scope>
		</dependency>
```

> `spring-security-crypto` has a managed version via the Boot BOM — no `<version>` needed. It pulls no servlet-filter auto-config, so the existing suite stays open/green.

- [ ] **Step 2: Confirm the project still builds (no gate introduced)**

Run: `./mvnw -q -DskipTests compile`
Expected: BUILD SUCCESS. (No `spring-boot-starter-security` yet, so no default gate.)

- [ ] **Step 3: Write the migration**

Create `brewdeck-api/src/main/resources/db/migration/V5__create_users.sql`:

```sql
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);
```

- [ ] **Step 4: Create the `User` entity**

Create `auth/User.java`:

```java
package com.brewdeck.brewdeck_api.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
```

- [ ] **Step 5: Create the repository**

Create `auth/UserRepository.java`:

```java
package com.brewdeck.brewdeck_api.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}
```

- [ ] **Step 6: Add config properties**

In `application.yaml`, under the existing `brewdeck:` key (beside `ai:`), add:

```yaml
  auth:
    secret: ${BREWDECK_JWT_SECRET:dev-only-insecure-secret-change-me-at-least-32-bytes}
    token-ttl: ${AUTH_TOKEN_TTL:PT24H}
```

In `application-prod.yml`, add (no default → fail-fast in prod):

```yaml
brewdeck:
  auth:
    secret: ${BREWDECK_JWT_SECRET}
```

- [ ] **Step 7: Write the failing `JwtService` test**

Create `auth/JwtServiceTest.java`:

```java
package com.brewdeck.brewdeck_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "test-secret-value-that-is-definitely-long-enough-1234";

  private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(24));

  private User user() {
    return User.builder().id(1L).email("brewer@example.com").passwordHash("x").build();
  }

  @Test
  void generateThenValidate_returnsSubjectEmail() {
    String token = jwtService.generateToken(user());

    assertThat(jwtService.validateAndGetSubject(token)).isEqualTo("brewer@example.com");
  }

  @Test
  void validate_rejectsTamperedToken() {
    String token = jwtService.generateToken(user());
    String tampered = token.substring(0, token.length() - 2) + (token.endsWith("a") ? "b" : "a");

    assertThatThrownBy(() -> jwtService.validateAndGetSubject(tampered))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void validate_rejectsTokenSignedWithDifferentSecret() {
    JwtService other = new JwtService("a-completely-different-secret-key-32bytes-minimum", Duration.ofHours(24));
    String token = other.generateToken(user());

    assertThatThrownBy(() -> jwtService.validateAndGetSubject(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void validate_rejectsExpiredToken() {
    JwtService expiring = new JwtService(SECRET, Duration.ofSeconds(-1));
    String token = expiring.generateToken(user());

    assertThatThrownBy(() -> jwtService.validateAndGetSubject(token))
        .isInstanceOf(JwtException.class);
  }
}
```

- [ ] **Step 8: Run the test to verify it fails**

Run: `./mvnw -Dtest=JwtServiceTest test`
Expected: FAIL — `JwtService` does not exist.

- [ ] **Step 9: Implement `JwtService`**

Create `auth/JwtService.java`:

```java
package com.brewdeck.brewdeck_api.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey key;
  private final Duration ttl;

  public JwtService(
      @Value("${brewdeck.auth.secret}") String secret,
      @Value("${brewdeck.auth.token-ttl}") Duration ttl) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.ttl = ttl;
  }

  public String generateToken(User user) {
    Instant issuedAt = Instant.now();
    return Jwts.builder()
        .subject(user.getEmail())
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiryFor(issuedAt)))
        .signWith(key)
        .compact();
  }

  public String validateAndGetSubject(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  public Instant expiryFor(Instant issuedAt) {
    return issuedAt.plus(ttl);
  }
}
```

- [ ] **Step 10: Run the test to verify it passes**

Run: `./mvnw -Dtest=JwtServiceTest test`
Expected: PASS.

- [ ] **Step 11: Create the DTOs and exception**

Create `auth/RegisterRequest.java`:

```java
package com.brewdeck.brewdeck_api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email String email, @NotBlank @Size(min = 8, max = 100) String password) {}
```

Create `auth/LoginRequest.java`:

```java
package com.brewdeck.brewdeck_api.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
```

Create `auth/AuthResponse.java`:

```java
package com.brewdeck.brewdeck_api.auth;

import java.time.Instant;

public record AuthResponse(String token, Instant expiresAt, String email) {}
```

Create `auth/UserResponse.java`:

```java
package com.brewdeck.brewdeck_api.auth;

import java.time.LocalDateTime;

public record UserResponse(Long id, String email, LocalDateTime createdAt) {
  public static UserResponse fromEntity(User user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt());
  }
}
```

Create `auth/EmailAlreadyUsedException.java`:

```java
package com.brewdeck.brewdeck_api.auth;

public class EmailAlreadyUsedException extends RuntimeException {
  public EmailAlreadyUsedException(String message) {
    super(message);
  }
}
```

- [ ] **Step 12: Create the `PasswordEncoder` bean**

Create `common/config/PasswordEncoderConfig.java` (kept in `common/config` so it is covered by the `**/config/**` coverage exclusion):

```java
package com.brewdeck.brewdeck_api.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
```

- [ ] **Step 13: Write the failing `AuthService` test**

Create `auth/AuthServiceTest.java`:

```java
package com.brewdeck.brewdeck_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    authService = new AuthService(userRepository, jwtService, encoder);
  }

  private User stored(String email, String rawPassword) {
    return User.builder()
        .id(1L)
        .email(email)
        .passwordHash(new BCryptPasswordEncoder().encode(rawPassword))
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  void register_persistsHashedPasswordAndReturnsToken() {
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

    AuthResponse response = authService.register(new RegisterRequest("new@example.com", "password1"));

    assertThat(response.token()).isEqualTo("jwt-token");
    assertThat(response.email()).isEqualTo("new@example.com");
  }

  @Test
  void register_throwsWhenEmailAlreadyUsed() {
    when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

    assertThatThrownBy(
            () -> authService.register(new RegisterRequest("taken@example.com", "password1")))
        .isInstanceOf(EmailAlreadyUsedException.class);
  }

  @Test
  void login_returnsTokenWhenPasswordMatches() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

    AuthResponse response = authService.login(new LoginRequest("brewer@example.com", "password1"));

    assertThat(response.token()).isEqualTo("jwt-token");
  }

  @Test
  void login_throwsWhenPasswordWrong() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));

    assertThatThrownBy(() -> authService.login(new LoginRequest("brewer@example.com", "wrong")))
        .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
  }

  @Test
  void login_throwsWhenEmailUnknown() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "password1")))
        .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
  }

  @Test
  void me_returnsUserWhenPresent() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));

    UserResponse response = authService.me("brewer@example.com");

    assertThat(response.email()).isEqualTo("brewer@example.com");
  }

  @Test
  void me_throwsWhenMissing() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.me("ghost@example.com"))
        .isInstanceOf(EntityNotFoundException.class);
  }
}
```

- [ ] **Step 14: Run the test to verify it fails**

Run: `./mvnw -Dtest=AuthServiceTest test`
Expected: FAIL — `AuthService` does not exist.

- [ ] **Step 15: Implement `AuthService`**

Create `auth/AuthService.java`:

```java
package com.brewdeck.brewdeck_api.auth;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AuthService {

  private static final String INVALID_CREDENTIALS = "Invalid email or password";

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new EmailAlreadyUsedException("Email is already registered");
    }
    User user =
        User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .createdAt(LocalDateTime.now())
            .build();
    User saved = userRepository.save(user);
    log.info("Registered user id={}", saved.getId());
    return tokenResponse(saved);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BadCredentialsException(INVALID_CREDENTIALS);
    }
    return tokenResponse(user);
  }

  @Transactional(readOnly = true)
  public UserResponse me(String email) {
    return userRepository
        .findByEmail(email)
        .map(UserResponse::fromEntity)
        .orElseThrow(() -> new EntityNotFoundException("User not found"));
  }

  private AuthResponse tokenResponse(User user) {
    String token = jwtService.generateToken(user);
    return new AuthResponse(token, jwtService.expiryFor(Instant.now()), user.getEmail());
  }
}
```

- [ ] **Step 16: Run the test to verify it passes**

Run: `./mvnw -Dtest=AuthServiceTest test`
Expected: PASS.

- [ ] **Step 17: Map `EmailAlreadyUsedException` → 409**

In `GlobalExceptionHandler.java`, add the import
`import com.brewdeck.brewdeck_api.auth.EmailAlreadyUsedException;` and this handler (place beside `handleDataIntegrityViolationException`):

```java
  @ExceptionHandler(EmailAlreadyUsedException.class)
  public ResponseEntity<ErrorResponse> handleEmailAlreadyUsed(
      EmailAlreadyUsedException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.CONFLICT,
            sanitize(exception.getMessage()),
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }
```

- [ ] **Step 18: Format, full verify, commit**

```bash
./mvnw spotless:apply
./mvnw clean verify
```
Expected: BUILD SUCCESS; the existing suite is untouched (no gate yet). Then:

```bash
git add brewdeck-api/pom.xml \
  brewdeck-api/src/main/resources/db/migration/V5__create_users.sql \
  brewdeck-api/src/main/resources/application.yaml \
  brewdeck-api/src/main/resources/application-prod.yml \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/ \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/config/PasswordEncoderConfig.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/error/GlobalExceptionHandler.java \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/
git commit -m "feat(api): add auth identity core (user, jwt, bcrypt, register/login service)"
```

---

### Task 2: Backend security gate + auth endpoints

Introduces `spring-boot-starter-security` (the moment this lands, `/api/**` is gated), the JWT filter chain, the `AuthController`, the Swagger bearer scheme, and the migration of every existing MockMvc test so the whole suite is green again in the same commit.

**Files:**
- Modify: `brewdeck-api/pom.xml` (add `spring-boot-starter-security`, `spring-security-test` test-scope)
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/JwtAuthenticationFilter.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/config/SecurityConfig.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/config/RestAuthenticationEntryPoint.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/AuthController.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/config/OpenApiConfig.java`
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/auth/AuthControllerTest.java` (create)
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/integration/AuthSecurityIntegrationTest.java` (create)
- Test (migrate — add `@WithMockUser`): the 8 `@WebMvcTest` controller tests and the `@SpringBootTest` integration tests that call protected endpoints (enumerated in Step 9).

**Interfaces:**
- Consumes: `JwtService.validateAndGetSubject`, `UserRepository.findByEmail`, `AuthService` (all from Task 1), `RestAuthenticationEntryPoint`.
- Produces:
  - `POST /api/auth/register` → 201 `AuthResponse`
  - `POST /api/auth/login` → 200 `AuthResponse`
  - `GET /api/auth/me` → 200 `UserResponse` (401 without a valid token)
  - Every other `/api/**` endpoint now requires `Authorization: Bearer <token>`; `/api/public/**` stays open.
  - Security context principal name = the authenticated user's email.

- [ ] **Step 1: Add the security dependencies**

In `pom.xml`, add (starter next to `spring-boot-starter-web`; test dep next to `spring-boot-starter-test`):

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
```
```xml
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-test</artifactId>
			<scope>test</scope>
		</dependency>
```

- [ ] **Step 2: Create the authentication entry point (401 JSON)**

Create `common/config/RestAuthenticationEntryPoint.java`:

```java
package com.brewdeck.brewdeck_api.common.config;

import com.brewdeck.brewdeck_api.common.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    ErrorResponse body =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            "Authentication required",
            request.getRequestURI(),
            null);
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), body);
  }
}
```

- [ ] **Step 3: Create the JWT authentication filter**

Create `auth/JwtAuthenticationFilter.java`:

```java
package com.brewdeck.brewdeck_api.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final UserRepository userRepository;

  public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
    this.jwtService = jwtService;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null
        && header.startsWith(BEARER_PREFIX)
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      String token = header.substring(BEARER_PREFIX.length());
      try {
        String email = jwtService.validateAndGetSubject(token);
        userRepository
            .findByEmail(email)
            .ifPresent(
                user -> {
                  var authentication =
                      new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of());
                  authentication.setDetails(
                      new WebAuthenticationDetailsSource().buildDetails(request));
                  SecurityContextHolder.getContext().setAuthentication(authentication);
                });
      } catch (JwtException ignored) {
        // invalid/expired token -> stay anonymous; authorization rules reject protected routes
      }
    }
    filterChain.doFilter(request, response);
  }
}
```

- [ ] **Step 4: Create `SecurityConfig`**

Create `common/config/SecurityConfig.java`:

```java
package com.brewdeck.brewdeck_api.common.config;

import com.brewdeck.brewdeck_api.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RestAuthenticationEntryPoint authenticationEntryPoint;
  private final CorsConfigurationSource corsConfigurationSource;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      CorsConfigurationSource corsConfigurationSource) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.corsConfigurationSource = corsConfigurationSource;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/api/auth/register", "/api/auth/login")
                    .permitAll()
                    .requestMatchers("/api/public/**")
                    .permitAll()
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
```

> `CorsConfigurationSource` is not currently a bean — the app uses `WebConfig` (an MVC `CorsRegistry`). Spring Security needs its own CORS source. Add the bean in Step 5.

- [ ] **Step 5: Expose a `CorsConfigurationSource` bean for the security chain**

In `common/config/WebConfig.java`, keep the existing MVC mapping and **add** a `CorsConfigurationSource` bean (same allowed origins) so the security chain and MVC agree:

```java
  @org.springframework.context.annotation.Bean
  public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
    org.springframework.web.cors.CorsConfiguration config =
        new org.springframework.web.cors.CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(
        java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(java.util.List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
        new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
```

- [ ] **Step 6: Create `AuthController`**

Create `auth/AuthController.java`:

```java
package com.brewdeck.brewdeck_api.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, and current-user lookup")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Register a new account")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  @Operation(summary = "Log in and receive a bearer token")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @GetMapping("/me")
  @Operation(summary = "Get the currently authenticated user")
  public ResponseEntity<UserResponse> me(Principal principal) {
    return ResponseEntity.ok(authService.me(principal.getName()));
  }
}
```

> `BadCredentialsException` from `login` must return 401. Add its handler in Step 7 (it flows through `GlobalExceptionHandler`, not the entry point, because it is thrown inside the controller call).

- [ ] **Step 7: Map `BadCredentialsException` → 401**

In `GlobalExceptionHandler.java`, add
`import org.springframework.security.authentication.BadCredentialsException;` and:

```java
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentials(
      BadCredentialsException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "Invalid email or password",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }
```

- [ ] **Step 8: Add the Swagger bearer scheme**

Replace the body of `OpenApiConfig.brewDeckOpenAPI()` so it registers a bearer scheme:

```java
package com.brewdeck.brewdeck_api.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String BEARER = "bearerAuth";

  @Bean
  public OpenAPI brewDeckOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("BrewDeck API")
                .description(
                    "REST API for managing coffees, brew methods, recipes and brew sessions.")
                .version("v1")
                .contact(new Contact().name("BrewDeck"))
                .license(new License().name("Apache 2.0")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
```

- [ ] **Step 9: Migrate the existing MockMvc tests to authenticate**

The gate now rejects unauthenticated `/api/**` calls. Add a class-level `@org.springframework.security.test.context.support.WithMockUser` to every existing test that exercises a protected endpoint, so the pre-populated `SecurityContext` satisfies `.authenticated()`. Add the import and the annotation (directly above the class declaration) to each of these files — **do not** change their request logic:

`@WebMvcTest` controller tests (8):
`coffee/CoffeeControllerTest.java`, `recipe/RecipeControllerTest.java`,
`method/BrewMethodControllerTest.java`, `session/BrewSessionControllerTest.java`,
`dashboard/DashboardControllerTest.java`, `ai/RecipeSuggestionControllerTest.java`,
`ai/RecipeImprovementControllerTest.java`.

> **`recipe/PublicRecipeControllerTest.java` is the exception — do NOT add `@WithMockUser`.** It targets a `permitAll` endpoint and must keep passing unauthenticated. For `@WebMvcTest` slices, also add `@AutoConfigureMockMvc(addFilters = true)` is unnecessary; but each `@WebMvcTest` slice must not fail to load the real `SecurityConfig`. `@WebMvcTest` does **not** import `SecurityConfig` (it is a `@Configuration`, not imported by the slice), so the slice uses Boot's default chain; `@WithMockUser` satisfies it. If any slice fails to start because it cannot find `JwtAuthenticationFilter`/`RestAuthenticationEntryPoint` beans, add `@WebMvcTest(controllers = X.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {com.brewdeck.brewdeck_api.auth.JwtAuthenticationFilter.class, com.brewdeck.brewdeck_api.common.config.SecurityConfig.class, com.brewdeck.brewdeck_api.common.config.RestAuthenticationEntryPoint.class}))` to that slice.

`@SpringBootTest` integration tests that call protected endpoints — add class-level `@WithMockUser` to each:
`integration/DashboardSummaryIntegrationTest.java`,
`integration/BrewMethodSeedIntegrationTest.java`,
`integration/BrewingWorkflowIntegrationTest.java`,
`integration/MostBrewedRecipesIntegrationTest.java`,
`integration/MostUsedCoffeesIntegrationTest.java`,
`integration/TopRatedRecipesIntegrationTest.java`,
`integration/RecipeStatsIntegrationTest.java`,
`integration/MethodUsageIntegrationTest.java`,
`integration/InvalidSortIntegrationTest.java`.

> **`integration/HealthEndpointIntegrationTest.java`** hits `/actuator/health` (permitAll) — leave unauthenticated.
> **`integration/OpenApiDocsIntegrationTest.java`** hits `/v3/api-docs` (permitAll) — leave unauthenticated.
> **`integration/CorsIntegrationTest.java`** — its preflight `OPTIONS` tests still pass (OPTIONS is permitAll). Its `actualRequest_fromAllowedOrigin_shouldEchoAllowOrigin` performs a real `GET /api/coffees`; add `@WithMockUser` at class level so that GET returns 200. The disallowed-origin preflight still expects 403 from the CORS layer.

- [ ] **Step 10: Write the `AuthController` slice test**

Create `auth/AuthControllerTest.java`:

```java
package com.brewdeck.brewdeck_api.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// Uses a standalone MockMvc wired with the real GlobalExceptionHandler so status mapping is exercised.
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private AuthService authService;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new AuthController(authService))
            .setControllerAdvice(new com.brewdeck.brewdeck_api.common.error.GlobalExceptionHandler())
            .build();
  }

  @Test
  void register_returns201WithToken() throws Exception {
    when(authService.register(any()))
        .thenReturn(new AuthResponse("jwt", Instant.parse("2026-07-09T00:00:00Z"), "new@example.com"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new RegisterRequest("new@example.com", "password1"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").value("jwt"))
        .andExpect(jsonPath("$.email").value("new@example.com"));
  }

  @Test
  void register_duplicateReturns409() throws Exception {
    when(authService.register(any())).thenThrow(new EmailAlreadyUsedException("Email is already registered"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new RegisterRequest("taken@example.com", "password1"))))
        .andExpect(status().isConflict());
  }

  @Test
  void register_invalidEmailReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new RegisterRequest("not-an-email", "short"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_badCredentialsReturns401() throws Exception {
    when(authService.login(any())).thenThrow(new BadCredentialsException("Invalid email or password"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new LoginRequest("brewer@example.com", "wrong"))))
        .andExpect(status().isUnauthorized());
  }
}
```

> This uses `standaloneSetup` (no security chain) to unit-test the controller + advice mapping. The **filter-chain** behavior (401 without a token, 200 with) is covered by the integration test in Step 11.

- [ ] **Step 11: Write the security integration test**

Create `integration/AuthSecurityIntegrationTest.java`:

```java
package com.brewdeck.brewdeck_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void protectedEndpoint_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/coffees").param("page", "0").param("size", "10").param("sort", "id,asc"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void publicShareEndpoint_withoutToken_isReachable() throws Exception {
    // Unknown token -> 404 (reachable, i.e. not blocked by 401).
    mockMvc.perform(get("/api/public/recipes/unknown-token")).andExpect(status().isNotFound());
  }

  @Test
  void registerThenLoginThenCallProtected_succeeds() throws Exception {
    String email = "flow-" + System.nanoTime() + "@example.com";
    String body = "{\"email\":\"" + email + "\",\"password\":\"password1\"}";

    String registerResponse =
        mockMvc
            .perform(post("/api/auth/register").contentType("application/json").content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String token = com.jayway.jsonpath.JsonPath.read(registerResponse, "$.token");

    mockMvc
        .perform(
            get("/api/coffees")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "10")
                .param("sort", "id,asc"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email));
  }

  @Test
  void me_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }
}
```

- [ ] **Step 12: Run the auth tests**

Run: `./mvnw -Dtest=AuthControllerTest,AuthSecurityIntegrationTest test`
Expected: PASS.

- [ ] **Step 13: Format, full verify, commit**

```bash
./mvnw spotless:apply
./mvnw clean verify
```
Expected: BUILD SUCCESS with the **entire** suite green — the gate is live and every migrated test authenticates. If any migrated slice fails to load, apply the `excludeFilters` fallback from Step 9. Then:

```bash
git add brewdeck-api/pom.xml \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/JwtAuthenticationFilter.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/auth/AuthController.java \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/config/ \
  brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/error/GlobalExceptionHandler.java \
  brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/
git commit -m "feat(api): gate /api/** behind stateless JWT auth with register/login/me"
```

---

### Task 3: Frontend auth client + state

Adds the token store, API module, bearer injection + 401 handling in `apiFetch`, and the `AuthProvider`. No screens yet.

**Files:**
- Modify: `brewdeck-web/src/lib/api/types.ts` (add `AuthResponse`, `UserResponse`)
- Create: `brewdeck-web/src/lib/api/auth.ts`
- Create: `brewdeck-web/src/lib/auth/tokenStore.ts`
- Modify: `brewdeck-web/src/lib/api/client.ts` (bearer header + 401 handling)
- Create: `brewdeck-web/src/lib/auth/AuthProvider.tsx`
- Modify: `brewdeck-web/src/app/providers.tsx` (wrap children in `AuthProvider`)
- Test: `brewdeck-web/src/lib/auth/tokenStore.test.ts` (create)
- Test: `brewdeck-web/src/lib/api/auth.test.ts` (create)
- Test: `brewdeck-web/src/lib/api/client.test.ts` (modify — bearer + 401 cases)
- Test: `brewdeck-web/src/lib/auth/AuthProvider.test.tsx` (create)

**Interfaces:**
- Consumes: backend `POST /api/auth/register|login` → `AuthResponse`, `GET /api/auth/me` → `UserResponse`; existing `apiFetch`, `ApiError`.
- Produces (later task relies on these):
  - `AuthResponse = { token: string; expiresAt: string; email: string }`, `UserResponse = { id: number; email: string; createdAt: string }` in `@/lib/api/types`.
  - `src/lib/api/auth.ts`: `register(body: { email: string; password: string }): Promise<AuthResponse>`, `login(body: { email: string; password: string }): Promise<AuthResponse>`, `getMe(): Promise<UserResponse>`.
  - `src/lib/auth/tokenStore.ts`: `getToken(): string | null`, `setToken(token: string): void`, `clearToken(): void` (key `brewdeck.token`, SSR-guarded).
  - `src/lib/auth/AuthProvider.tsx`: `AuthProvider` component + `useAuth(): { user: UserResponse | null; status: 'loading' | 'authenticated' | 'anonymous'; login(body): Promise<void>; register(body): Promise<void>; logout(): void }`.

- [ ] **Step 1: Add the domain types**

In `src/lib/api/types.ts`, append:

```ts
export type AuthResponse = {
  token: string;
  expiresAt: string;
  email: string;
};

export type UserResponse = {
  id: number;
  email: string;
  createdAt: string;
};
```

- [ ] **Step 2: Write the failing `tokenStore` test**

Create `src/lib/auth/tokenStore.test.ts`:

```ts
import { afterEach, describe, expect, it } from 'vitest';
import { clearToken, getToken, setToken } from './tokenStore';

describe('tokenStore', () => {
  afterEach(() => clearToken());

  it('returns null when no token is set', () => {
    expect(getToken()).toBeNull();
  });

  it('stores and reads a token', () => {
    setToken('abc.def.ghi');
    expect(getToken()).toBe('abc.def.ghi');
  });

  it('clears a stored token', () => {
    setToken('abc.def.ghi');
    clearToken();
    expect(getToken()).toBeNull();
  });
});
```

- [ ] **Step 3: Run it to verify it fails**

Run (from `brewdeck-web/`): `npm run test -- src/lib/auth/tokenStore.test.ts`
Expected: FAIL — module not found.

- [ ] **Step 4: Implement `tokenStore`**

Create `src/lib/auth/tokenStore.ts`:

```ts
const TOKEN_KEY = 'brewdeck.token';

export function getToken(): string | null {
  if (typeof window === 'undefined') {
    return null;
  }
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(TOKEN_KEY);
}
```

- [ ] **Step 5: Run it to verify it passes**

Run: `npm run test -- src/lib/auth/tokenStore.test.ts`
Expected: PASS.

- [ ] **Step 6: Write the failing `auth` API test**

Create `src/lib/api/auth.test.ts`:

```ts
import { afterEach, describe, expect, it, vi } from 'vitest';
import { getMe, login, register } from './auth';
import * as client from './client';

describe('auth api', () => {
  afterEach(() => vi.restoreAllMocks());

  it('POSTs registration', async () => {
    const body = { token: 't', expiresAt: '2026-07-09T00:00:00Z', email: 'a@b.com' };
    const spy = vi.spyOn(client, 'apiFetch').mockResolvedValue(body as never);

    const result = await register({ email: 'a@b.com', password: 'password1' });

    expect(spy).toHaveBeenCalledWith('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email: 'a@b.com', password: 'password1' }),
    });
    expect(result).toEqual(body);
  });

  it('POSTs login', async () => {
    const body = { token: 't', expiresAt: '2026-07-09T00:00:00Z', email: 'a@b.com' };
    const spy = vi.spyOn(client, 'apiFetch').mockResolvedValue(body as never);

    await login({ email: 'a@b.com', password: 'password1' });

    expect(spy).toHaveBeenCalledWith('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: 'a@b.com', password: 'password1' }),
    });
  });

  it('GETs the current user', async () => {
    const spy = vi
      .spyOn(client, 'apiFetch')
      .mockResolvedValue({ id: 1, email: 'a@b.com', createdAt: '2026-07-01T00:00:00Z' } as never);

    await getMe();

    expect(spy).toHaveBeenCalledWith('/api/auth/me');
  });
});
```

- [ ] **Step 7: Run it to verify it fails, then implement**

Run: `npm run test -- src/lib/api/auth.test.ts` → FAIL (module not found).

Create `src/lib/api/auth.ts`:

```ts
import { apiFetch } from './client';
import type { AuthResponse, UserResponse } from './types';

export function register(body: { email: string; password: string }): Promise<AuthResponse> {
  return apiFetch<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function login(body: { email: string; password: string }): Promise<AuthResponse> {
  return apiFetch<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function getMe(): Promise<UserResponse> {
  return apiFetch<UserResponse>('/api/auth/me');
}
```

Run again → PASS.

- [ ] **Step 8: Add the failing `apiFetch` bearer + 401 tests**

In `src/lib/api/client.test.ts`, add these cases (adapt to the file's existing `fetch` mock helper; the shape below assumes a `global.fetch` spy — match the file's convention). Import the token store and mock `next/navigation`:

```ts
import { setToken, clearToken, getToken } from '@/lib/auth/tokenStore';

const redirectMock = vi.fn();
vi.mock('next/navigation', () => ({ redirect: (url: string) => redirectMock(url) }));

// ...inside describe:

it('adds the Authorization header when a token is present', async () => {
  setToken('jwt-token');
  const fetchSpy = vi
    .spyOn(global, 'fetch')
    .mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }));

  await apiFetch('/api/coffees');

  const init = fetchSpy.mock.calls[0][1];
  expect((init?.headers as Record<string, string>).Authorization).toBe('Bearer jwt-token');
  clearToken();
});

it('omits the Authorization header when no token is present', async () => {
  clearToken();
  const fetchSpy = vi
    .spyOn(global, 'fetch')
    .mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }));

  await apiFetch('/api/coffees');

  const init = fetchSpy.mock.calls[0][1];
  expect((init?.headers as Record<string, string>).Authorization).toBeUndefined();
});

it('clears the token and redirects to /login on 401', async () => {
  setToken('jwt-token');
  vi.spyOn(global, 'fetch').mockResolvedValue(
    new Response(JSON.stringify({ message: 'Authentication required' }), { status: 401 }),
  );
  // jsdom default pathname is "/"
  await expect(apiFetch('/api/coffees')).rejects.toThrow();
  expect(getToken()).toBeNull();
  expect(redirectMock).toHaveBeenCalledWith('/login');
});
```

- [ ] **Step 9: Run to verify failure, then modify `apiFetch`**

Run: `npm run test -- src/lib/api/client.test.ts` → the new cases FAIL.

In `src/lib/api/client.ts`, modify `apiFetch` to inject the bearer header and handle 401. Add imports at the top:

```ts
import { getToken, clearToken } from '@/lib/auth/tokenStore';
```

Change the fetch call + error handling so the header is added and 401 triggers a redirect (guarded against `/login`, `/register`, `/share`):

```ts
export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const authHeader = token ? { Authorization: `Bearer ${token}` } : {};
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...authHeader, ...init?.headers },
  });

  if (response.status === 401) {
    clearToken();
    if (typeof window !== 'undefined') {
      const p = window.location.pathname;
      const onPublic = p === '/login' || p === '/register' || p.startsWith('/share');
      if (!onPublic) {
        window.location.assign('/login');
      }
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

> Use `window.location.assign('/login')` (not `next/navigation`'s `redirect`, which only works in Server Components / route handlers). In the test, spy on `window.location.assign` instead of mocking `next/navigation` — adjust the Step 8 test accordingly: replace the `next/navigation` mock with `const assignMock = vi.fn(); Object.defineProperty(window, 'location', { value: { pathname: '/', assign: assignMock }, writable: true });` and assert `assignMock` was called with `/login`.

- [ ] **Step 10: Run to verify the client tests pass**

Run: `npm run test -- src/lib/api/client.test.ts`
Expected: PASS.

- [ ] **Step 11: Write the failing `AuthProvider` test**

Create `src/lib/auth/AuthProvider.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from './AuthProvider';
import { setToken, clearToken } from './tokenStore';
import * as authApi from '@/lib/api/auth';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient();
  return (
    <QueryClientProvider client={client}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  );
}

function Probe() {
  const { status, user, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="status">{status}</span>
      <span data-testid="email">{user?.email ?? 'none'}</span>
      <button onClick={() => login({ email: 'a@b.com', password: 'password1' })}>login</button>
      <button onClick={logout}>logout</button>
    </div>
  );
}

describe('AuthProvider', () => {
  afterEach(() => {
    clearToken();
    vi.restoreAllMocks();
  });

  it('is anonymous when no token exists', async () => {
    render(<Probe />, { wrapper });
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
  });

  it('hydrates the user from getMe when a token exists', async () => {
    setToken('jwt');
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 1,
      email: 'brewer@example.com',
      createdAt: '2026-07-01T00:00:00Z',
    });
    render(<Probe />, { wrapper });
    await waitFor(() => expect(screen.getByTestId('email')).toHaveTextContent('brewer@example.com'));
    expect(screen.getByTestId('status')).toHaveTextContent('authenticated');
  });

  it('logs in and stores the token', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      token: 'jwt',
      expiresAt: '2026-07-09T00:00:00Z',
      email: 'a@b.com',
    });
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 2,
      email: 'a@b.com',
      createdAt: '2026-07-01T00:00:00Z',
    });
    render(<Probe />, { wrapper });
    await userEvent.click(screen.getByRole('button', { name: 'login' }));
    await waitFor(() => expect(screen.getByTestId('email')).toHaveTextContent('a@b.com'));
  });
});
```

- [ ] **Step 12: Run to verify it fails, then implement `AuthProvider`**

Run: `npm run test -- src/lib/auth/AuthProvider.test.tsx` → FAIL.

Create `src/lib/auth/AuthProvider.tsx`:

```tsx
'use client';

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { getMe, login as loginApi, register as registerApi } from '@/lib/api/auth';
import type { UserResponse } from '@/lib/api/types';
import { clearToken, getToken, setToken } from './tokenStore';

type AuthStatus = 'loading' | 'authenticated' | 'anonymous';

type Credentials = { email: string; password: string };

type AuthContextValue = {
  user: UserResponse | null;
  status: AuthStatus;
  login: (body: Credentials) => Promise<void>;
  register: (body: Credentials) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [status, setStatus] = useState<AuthStatus>('loading');

  useEffect(() => {
    if (!getToken()) {
      setStatus('anonymous');
      return;
    }
    getMe()
      .then((me) => {
        setUser(me);
        setStatus('authenticated');
      })
      .catch(() => {
        clearToken();
        setUser(null);
        setStatus('anonymous');
      });
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      login: async (body) => {
        const response = await loginApi(body);
        setToken(response.token);
        const me = await getMe();
        setUser(me);
        setStatus('authenticated');
      },
      register: async (body) => {
        const response = await registerApi(body);
        setToken(response.token);
        const me = await getMe();
        setUser(me);
        setStatus('authenticated');
      },
      logout: () => {
        clearToken();
        setUser(null);
        setStatus('anonymous');
      },
    }),
    [user, status],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
```

Run again → PASS.

- [ ] **Step 13: Wire `AuthProvider` into `Providers`**

In `src/app/providers.tsx`, import and wrap children inside `QueryProvider` (so `useAuth`'s data fetching has a client available):

```tsx
import { AuthProvider } from '@/lib/auth/AuthProvider';
```
```tsx
        <QueryProvider>
          <AuthProvider>{children}</AuthProvider>
        </QueryProvider>
```

- [ ] **Step 14: Full suite + checks, commit**

Run the **full** suite (shared `apiFetch`/`Providers`):

```bash
npm run test
npm run type-check
npm run lint:fix -- src/lib/auth/tokenStore.ts src/lib/auth/AuthProvider.tsx src/lib/api/auth.ts src/lib/api/client.ts src/lib/api/types.ts src/app/providers.tsx
npm run build
```
Expected: all green. Then:

```bash
git add brewdeck-web/src/lib/api/types.ts brewdeck-web/src/lib/api/auth.ts \
  brewdeck-web/src/lib/api/auth.test.ts brewdeck-web/src/lib/auth/ \
  brewdeck-web/src/lib/api/client.ts brewdeck-web/src/lib/api/client.test.ts \
  brewdeck-web/src/app/providers.tsx
git commit -m "feat(web): add auth token store, api client, bearer injection, and AuthProvider"
```

---

### Task 4: Frontend screens + route guard

Adds the Zod schema, `/login` + `/register` pages, `RequireAuth`, the `(app)` route-group restructure so the guard + `AppShell` wrap only authenticated routes, and a logout control.

**Files:**
- Create: `brewdeck-web/src/lib/validation/authSchema.ts`
- Create: `brewdeck-web/src/components/auth/RequireAuth.tsx`
- Create: `brewdeck-web/src/app/login/page.tsx`
- Create: `brewdeck-web/src/app/register/page.tsx`
- Create: `brewdeck-web/src/app/(app)/layout.tsx`
- Move: `brewdeck-web/src/app/page.tsx` → `brewdeck-web/src/app/(app)/page.tsx`
- Move: `dashboard/`, `coffees/`, `recipes/`, `brew-sessions/`, `brew-methods/` from `src/app/` into `src/app/(app)/`
- Modify: `brewdeck-web/src/app/layout.tsx` (remove `AppShell`; it moves into the `(app)` layout)
- Modify: `brewdeck-web/src/components/layout/AppShell.tsx` (logout button + user email)
- Test: `brewdeck-web/src/components/auth/RequireAuth.test.tsx` (create)
- Test: `brewdeck-web/src/app/login/LoginForm` behavior — tested via `src/components/auth/LoginForm.test.tsx` (create; extract a `LoginForm` component so the page stays a thin `export default`)

**Interfaces:**
- Consumes: `useAuth()` from `@/lib/auth/AuthProvider`; `Spinner` from `@/components/ui/Spinner`; `ApiError` from `@/lib/api/client`.
- Produces: `RequireAuth` (guards children); `LoginForm`, `RegisterForm` components; `/login`, `/register` routes; `(app)` guarded layout.

- [ ] **Step 1: Create the Zod schema**

Create `src/lib/validation/authSchema.ts`:

```ts
import { z } from 'zod';

export const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
});

export const registerSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
});

export type LoginFormValues = z.infer<typeof loginSchema>;
export type RegisterFormValues = z.infer<typeof registerSchema>;
```

- [ ] **Step 2: Write the failing `RequireAuth` test**

Create `src/components/auth/RequireAuth.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RequireAuth } from './RequireAuth';

const replaceMock = vi.fn();
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace: replaceMock }) }));

const useAuthMock = vi.fn();
vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => useAuthMock() }));

describe('RequireAuth', () => {
  afterEach(() => vi.clearAllMocks());

  it('shows a spinner while loading', () => {
    useAuthMock.mockReturnValue({ status: 'loading' });
    render(
      <RequireAuth>
        <div>secret</div>
      </RequireAuth>,
    );
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('redirects to /login when anonymous', () => {
    useAuthMock.mockReturnValue({ status: 'anonymous' });
    render(
      <RequireAuth>
        <div>secret</div>
      </RequireAuth>,
    );
    expect(replaceMock).toHaveBeenCalledWith('/login');
    expect(screen.queryByText('secret')).not.toBeInTheDocument();
  });

  it('renders children when authenticated', () => {
    useAuthMock.mockReturnValue({ status: 'authenticated' });
    render(
      <RequireAuth>
        <div>secret</div>
      </RequireAuth>,
    );
    expect(screen.getByText('secret')).toBeInTheDocument();
  });
});
```

- [ ] **Step 3: Run to verify it fails, then implement `RequireAuth`**

Run: `npm run test -- src/components/auth/RequireAuth.test.tsx` → FAIL.

Create `src/components/auth/RequireAuth.tsx`:

```tsx
'use client';

import { useRouter } from 'next/navigation';
import { useEffect, type ReactNode } from 'react';
import { Spinner } from '@/components/ui/Spinner';
import { useAuth } from '@/lib/auth/AuthProvider';

export function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === 'anonymous') {
      router.replace('/login');
    }
  }, [status, router]);

  if (status === 'authenticated') {
    return <>{children}</>;
  }
  return <Spinner />;
}
```

Run again → PASS.

- [ ] **Step 4: Write the failing `LoginForm` test**

Create `src/components/auth/LoginForm.test.tsx`:

```tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { LoginForm } from './LoginForm';

const pushMock = vi.fn();
vi.mock('next/navigation', () => ({ useRouter: () => ({ push: pushMock, replace: vi.fn() }) }));

const loginMock = vi.fn();
vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => ({ login: loginMock }) }));

describe('LoginForm', () => {
  afterEach(() => vi.clearAllMocks());

  it('validates required fields', async () => {
    render(<LoginForm />);
    await userEvent.click(screen.getByRole('button', { name: /log in/i }));
    expect(await screen.findByText(/email is required/i)).toBeInTheDocument();
  });

  it('submits credentials and redirects on success', async () => {
    loginMock.mockResolvedValue(undefined);
    render(<LoginForm />);
    await userEvent.type(screen.getByLabelText(/email/i), 'a@b.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'password1');
    await userEvent.click(screen.getByRole('button', { name: /log in/i }));
    await waitFor(() => expect(loginMock).toHaveBeenCalledWith({ email: 'a@b.com', password: 'password1' }));
    expect(pushMock).toHaveBeenCalledWith('/dashboard');
  });

  it('shows an error alert on 401', async () => {
    loginMock.mockRejectedValue(new Error('bad creds'));
    render(<LoginForm />);
    await userEvent.type(screen.getByLabelText(/email/i), 'a@b.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'password1');
    await userEvent.click(screen.getByRole('button', { name: /log in/i }));
    expect(await screen.findByText(/could not log in/i)).toBeInTheDocument();
  });
});
```

- [ ] **Step 5: Run to verify it fails, then implement `LoginForm`**

Run: `npm run test -- src/components/auth/LoginForm.test.tsx` → FAIL.

Create `src/components/auth/LoginForm.tsx`:

```tsx
'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useAuth } from '@/lib/auth/AuthProvider';
import { loginSchema, type LoginFormValues } from '@/lib/validation/authSchema';

export function LoginForm() {
  const { login } = useAuth();
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await login(values);
      router.push('/dashboard');
    } catch {
      setFormError('Could not log in. Check your email and password.');
    }
  });

  return (
    <Box component="form" onSubmit={onSubmit} sx={{ maxWidth: 400, mx: 'auto', mt: 8 }}>
      <Typography variant="h5" component="h1" gutterBottom>
        Log in
      </Typography>
      <Stack spacing={2}>
        {formError ? <Alert severity="error">{formError}</Alert> : null}
        <TextField
          label="Email"
          type="email"
          {...register('email')}
          error={!!errors.email}
          helperText={errors.email?.message}
        />
        <TextField
          label="Password"
          type="password"
          {...register('password')}
          error={!!errors.password}
          helperText={errors.password?.message}
        />
        <Button type="submit" variant="contained" disabled={isSubmitting}>
          Log in
        </Button>
        <Typography variant="body2">
          No account? <a href="/register">Register</a>
        </Typography>
      </Stack>
    </Box>
  );
}
```

Run again → PASS.

- [ ] **Step 6: Create `RegisterForm` (mirror of `LoginForm`)**

Create `src/components/auth/RegisterForm.tsx` — identical structure to `LoginForm` with these differences: import `registerSchema`/`RegisterFormValues`; destructure `register: registerAccount` from `useAuth`; heading "Register"; button "Create account"; error copy "Could not register. That email may already be in use."; on success `router.push('/dashboard')`; footer link "Already have an account? Log in" → `/login`. Full code:

```tsx
'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useAuth } from '@/lib/auth/AuthProvider';
import { registerSchema, type RegisterFormValues } from '@/lib/validation/authSchema';

export function RegisterForm() {
  const { register: registerAccount } = useAuth();
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema) });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await registerAccount(values);
      router.push('/dashboard');
    } catch {
      setFormError('Could not register. That email may already be in use.');
    }
  });

  return (
    <Box component="form" onSubmit={onSubmit} sx={{ maxWidth: 400, mx: 'auto', mt: 8 }}>
      <Typography variant="h5" component="h1" gutterBottom>
        Register
      </Typography>
      <Stack spacing={2}>
        {formError ? <Alert severity="error">{formError}</Alert> : null}
        <TextField
          label="Email"
          type="email"
          {...register('email')}
          error={!!errors.email}
          helperText={errors.email?.message}
        />
        <TextField
          label="Password"
          type="password"
          {...register('password')}
          error={!!errors.password}
          helperText={errors.password?.message}
        />
        <Button type="submit" variant="contained" disabled={isSubmitting}>
          Create account
        </Button>
        <Typography variant="body2">
          Already have an account? <a href="/login">Log in</a>
        </Typography>
      </Stack>
    </Box>
  );
}
```

- [ ] **Step 7: Create the page files**

Create `src/app/login/page.tsx`:

```tsx
import { LoginForm } from '@/components/auth/LoginForm';

export default function LoginPage() {
  return <LoginForm />;
}
```

Create `src/app/register/page.tsx`:

```tsx
import { RegisterForm } from '@/components/auth/RegisterForm';

export default function RegisterPage() {
  return <RegisterForm />;
}
```

- [ ] **Step 8: Restructure into the `(app)` route group**

Move the authenticated routes under a new group so the guard wraps only them. Run from `brewdeck-web/`:

```bash
mkdir -p "src/app/(app)"
git mv src/app/page.tsx "src/app/(app)/page.tsx"
git mv src/app/dashboard "src/app/(app)/dashboard"
git mv src/app/coffees "src/app/(app)/coffees"
git mv src/app/recipes "src/app/(app)/recipes"
git mv src/app/brew-sessions "src/app/(app)/brew-sessions"
git mv src/app/brew-methods "src/app/(app)/brew-methods"
```

> `login/`, `register/`, and `share/` stay at `src/app/` — outside the group, so they get neither the guard nor `AppShell`. Route-group folders do not change URL paths, and components are imported via the `@/` alias, so no import breaks.

- [ ] **Step 9: Create the guarded `(app)` layout**

Create `src/app/(app)/layout.tsx`:

```tsx
import type { ReactNode } from 'react';
import { RequireAuth } from '@/components/auth/RequireAuth';
import { AppShell } from '@/components/layout/AppShell';

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <RequireAuth>
      <AppShell>{children}</AppShell>
    </RequireAuth>
  );
}
```

- [ ] **Step 10: Remove `AppShell` from the root layout**

In `src/app/layout.tsx`, drop the `AppShell` import and wrapper so the root only provides context (login/register/share render bare):

```tsx
import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import { Providers } from './providers';

export const metadata: Metadata = {
  title: 'BrewDeck',
  description: 'Coffee brewing companion',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
```

- [ ] **Step 11: Add logout + user email to `AppShell`**

Make `AppShell` render the current user's email and a Logout button in the `AppBar`. Add imports and use `useAuth`:

```tsx
import Button from '@mui/material/Button';
import { useAuth } from '@/lib/auth/AuthProvider';
```

Replace the `<Toolbar>` inside `<AppBar>` with:

```tsx
        <Toolbar sx={{ justifyContent: 'space-between' }}>
          <Typography variant="h6" noWrap>
            BrewDeck
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {user ? (
              <Typography variant="body2" noWrap>
                {user.email}
              </Typography>
            ) : null}
            <Button color="inherit" onClick={onLogout}>
              Logout
            </Button>
          </Box>
        </Toolbar>
```

Inside the `AppShell` component body (top), add:

```tsx
  const { user, logout } = useAuth();
  const router = useRouter();
  const onLogout = () => {
    logout();
    router.replace('/login');
  };
```

Add `import { useRouter } from 'next/navigation';` at the top.

> `AppShell` now consumes `useAuth`, so any test that renders it must provide an `AuthProvider` (or mock `@/lib/auth/AuthProvider`). Check for an existing `AppShell.test.tsx`; if present, mock `useAuth` there: `vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => ({ user: { email: 'a@b.com' }, logout: vi.fn() }) }))` and mock `next/navigation`'s `useRouter`.

- [ ] **Step 12: Full suite + checks, commit**

Run the **full** suite (shared root layout + `AppShell`; sibling-test trap):

```bash
npm run test
npm run type-check
npm run lint:fix -- src/lib/validation/authSchema.ts src/components/auth/RequireAuth.tsx src/components/auth/LoginForm.tsx src/components/auth/RegisterForm.tsx "src/app/(app)/layout.tsx" src/app/login/page.tsx src/app/register/page.tsx src/app/layout.tsx src/components/layout/AppShell.tsx
npm run build
```
Expected: all green; `build` confirms the `(app)` group and new routes compile. Then:

```bash
git add -A brewdeck-web/src
git commit -m "feat(web): add login/register screens and route guard on the app segment"
```

---

### Task 5: Docs — roadmap, project-state, Postman

**Files:**
- Modify: `.claude/roadmap.md`
- Modify: `.claude/project-state.md`
- Modify: `docs/postman/brewdeck.postman_collection.json`

**Interfaces:** none (documentation only).

- [ ] **Step 1: Open Phase 6 in the roadmap**

In `.claude/roadmap.md`, append a Phase 6 section after Phase 5:

```markdown
## Phase 6 — Auth & Multi-User

Status: In progress

- Auth foundation (Slice A) — self-registration, JWT login, gate all /api/** (public share + auth endpoints open) — Done
- Per-user ownership (Slice B) — owner FK on coffees/recipes/sessions, per-user filtering + data migration — Pending
- Account UX (Slice C) — email verification, password reset, refresh tokens, profile — Pending
```

- [ ] **Step 2: Update project-state**

In `.claude/project-state.md`, set the Current Phase to Phase 6 in progress and add a Recently Worked On entry:

```markdown
- Auth foundation (Slice A, full-stack) — backend: users table (Flyway V5), stateless JWT (jjwt) filter chain gating all /api/** except /api/public/** and /api/auth/{register,login}; BCrypt passwords; POST /api/auth/register (201), POST /api/auth/login (200), GET /api/auth/me (401 without token); RestAuthenticationEntryPoint returns 401 JSON; existing MockMvc suite migrated with @WithMockUser. Frontend: tokenStore (localStorage), auth api, apiFetch bearer injection + 401 redirect, AuthProvider (Context DI), /login + /register screens, RequireAuth on the (app) route-group layout; /login, /register, /share/[token] stay public. Ownership deferred to Slice B.
```

Also update the `## Current Phase` line and `## Immediate Next Steps` to point at Slice B (ownership).

- [ ] **Step 3: Add Postman auth requests**

In `docs/postman/brewdeck.postman_collection.json`:
1. Add a collection variable `{ "key": "authToken", "value": "", "type": "string" }` beside the existing `shareToken` variable.
2. Add an "Auth" folder with three requests using `{{baseURL}}`:
   - `POST {{baseURL}}/api/auth/register` — JSON body `{"email":"brewer@example.com","password":"password1"}`; test script asserts status is 201 and saves the token: `pm.collectionVariables.set('authToken', pm.response.json().token);`.
   - `POST {{baseURL}}/api/auth/login` — same body; test asserts 200 and saves `authToken` the same way.
   - `GET {{baseURL}}/api/auth/me` — header `Authorization: Bearer {{authToken}}`; test asserts status is 200.
3. Add a collection-level Bearer auth (`"auth": { "type": "bearer", "bearer": [{ "key": "token", "value": "{{authToken}}", "type": "string" }] }`) so existing protected requests send the token; keep the "Public Recipes" and "Auth/register", "Auth/login" requests on `"auth": { "type": "noauth" }`.

- [ ] **Step 4: Validate JSON and commit**

```bash
python3 -m json.tool docs/postman/brewdeck.postman_collection.json > /dev/null && echo JSON_OK
git add .claude/roadmap.md .claude/project-state.md docs/postman/brewdeck.postman_collection.json
git commit -m "docs: open Phase 6 and document auth foundation (Slice A)"
```

---

## Notes for the executor

- **Green per commit:** Task 1 deliberately avoids `spring-boot-starter-security` (only `jjwt` + `spring-security-crypto`) so no gate appears mid-plan. Task 2 introduces the gate **and** migrates the existing suite in the same commit — never split those.
- **Frontend sibling-test trap:** Tasks 3 and 4 touch shared modules (`apiFetch`, `Providers`, root layout, `AppShell`). Always run the **full** `vitest run`, not just the changed file.
- **`useAuth` outside a provider throws** — any test rendering `AppShell`, `RequireAuth`, or a form must mock `@/lib/auth/AuthProvider` or wrap in `AuthProvider`.
