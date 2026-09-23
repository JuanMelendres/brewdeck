# ADR-008: Migrate to Spring Boot 4.1.1

## Status
Accepted

## Date
2026-09-22

## Context
Three prior rounds of OWASP `dependency-check-maven` remediation (CVSS >= 7.0 gate,
`.github/workflows/security.yml`) closed every fixable finding within the Spring Boot 3.5.x line.
Two findings were left open because no fix exists below Spring Boot 4:

- **CVE-2026-59282** (spring-core, CVSS 7.5, gate-blocking) — a data-binding property-path DoS. The
  NVD-published fixed range starts at Spring Framework 7.0.9/7.1.1, which only ships under Spring
  Boot 4. BrewDeck is reachable: `CoffeeFilter`, `RecipeFilter`, and `BrewSessionFilter` are all bound
  via `@ModelAttribute` on collection GET endpoints.
- **CVE-2026-47834** (spring-data-jpa, CVSS 4.8, below the gate but tracked) — a `Sort` validation
  bypass. No fixed 3.x release exists; the fix ships only in spring-data-jpa releases aligned with
  Spring Boot 4.

Both were documented as open, gate-relevant risk in `pom.xml` and `dependency-check-suppressions.xml`
across rounds 1-3, with this migration flagged each time as the real fix path. This ADR records that
migration: `spring-boot-starter-parent` 3.5.16 -> **4.1.1**.

## Decision
Bump the parent POM to `spring-boot-starter-parent:4.1.1` and carry the following coordinated
changes, each forced by a genuine breaking change in Boot 4 (not cosmetic):

1. **Removed the `jackson-bom.version` and `tomcat.version` property overrides.** Both existed only
   to force CVE fixes ahead of what Boot 3.5.16 managed by default. Boot 4.1.1 manages
   `jackson-2-bom` (the classic Jackson 2.x property was renamed; `jackson-bom.version` now refers to
   the *new* `tools.jackson:jackson-bom` for Jackson 3) at 2.21.5 and Tomcat at 11.0.24 by default —
   both already at or ahead of the versions we were forcing. Keeping the old property name would have
   silently done nothing (or worse, misconfigured the wrong BOM).
2. **Renamed Testcontainers artifacts**: `org.testcontainers:postgresql` ->
   `org.testcontainers:testcontainers-postgresql`, `org.testcontainers:junit-jupiter` ->
   `org.testcontainers:testcontainers-junit-jupiter`, tracking the Testcontainers 2.x rename that
   Boot 4.1.1's dependency management (`testcontainers.version=2.0.5`) now expects.
3. **`GlobalExceptionHandler`**: `org.springframework.data.mapping.PropertyReferenceException` moved
   to `org.springframework.data.core.PropertyReferenceException` in spring-data-commons 4.1.1. This is
   the exact exception BrewDeck's `@ModelAttribute`-bound sort/filter handling relies on, so it is
   directly on the CVE-2026-59282 surface — verified with a full test run, not just a successful
   compile.
4. **Rewrote 40 test files'** Boot-4-modularized test-slice annotation imports (`@WebMvcTest`,
   `@AutoConfigureMockMvc`, `@DataJpaTest`, `TestEntityManager`, `@AutoConfigureTestDatabase`) via a
   scripted `sed` pass across `src/test/java`, then fixed the resulting compile/format state (Spotless
   re-sorted imports in 18 of those files because the new package names sort differently). Swapped
   `spring-boot-starter-test` -> **`spring-boot-starter-test-classic`**, Boot's own aggregate covering
   the same technology set (JUnit 5, Mockito, AssertJ, MockMvc, JSONassert) that the plain starter
   covered under Boot 3.
5. **`@MockBean` -> `@MockitoBean`** in `PublicRecipeControllerTest` had already landed on `develop`
   via a separate, earlier PR before this migration branched — confirmed via `grep` before editing;
   no changes needed here.

## The Jackson 2 vs Jackson 3 decision
Boot 4's `spring-boot-starter-web` now pulls `spring-boot-jackson`, which only autoconfigures a
`tools.jackson.databind.ObjectMapper` bean (Jackson 3). BrewDeck's `RestAuthenticationEntryPoint`
(production code, on the authentication error path) and 17 test classes explicitly depend on the
classic `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2) — without a shim, autowiring breaks
at real application startup, not just in tests.

**Decision: add `org.springframework.boot:spring-boot-jackson2` (compile scope, Boot-managed
version) as a deliberate compatibility shim, and do not port to the Jackson 3 API in this change.**
Porting to Jackson 3 (`tools.jackson.databind.*`) is a separate, larger effort with its own risk
surface — it touches every controller/test that serializes JSON, is orthogonal to closing
CVE-2026-59282/47834, and deserves its own reviewed change. This is intentionally called out here so
nobody mistakes the shim for a permanent architecture choice; **revisit and plan a real Jackson 3
port as a follow-up**, at which point `spring-boot-jackson2` should be removed.

## The Flyway starter gotcha
Boot 4 carved `FlywayAutoConfiguration` out of the Boot 3-style automatic wiring into its own
`spring-boot-flyway` module, gated behind the new `spring-boot-starter-flyway` starter. A bare
`org.flywaydb:flyway-core` dependency (BrewDeck's prior setup, matching Boot 3's behavior of shipping
Flyway unconditionally) **no longer triggers Flyway autoconfiguration under Boot 4 — silently, with
no error and no log line.** The failure mode this produces is confusing: Hibernate's
`ddl-auto: validate` then fails at context startup with a "missing table" error that looks unrelated
to Flyway at all.

**This is worth flagging explicitly so nobody "cleans up" this dependency change later thinking it's
redundant**: `org.flywaydb:flyway-core` was replaced with `org.springframework.boot:spring-boot-starter-flyway`
(keeping `flyway-database-postgresql` as-is). Verified post-migration by reading the Testcontainers
integration test logs directly, not just trusting a green build: every Testcontainers-backed context
in the `./mvnw clean verify` run logs `Successfully applied 14 migrations to schema "public", now at
version v14`, and the security-profile run additionally logs `Successfully validated 14 migrations`
from a separate `DbValidate` pass — real Flyway execution, not an absence-of-failure inference.

## springdoc-openapi bump (mandatory, not optional)
`springdoc-openapi-starter-webmvc-ui` 2.8.17 compiles under Boot 4 but fails at runtime
(`NoClassDefFoundError` on a Boot package that Boot 4 relocated, surfacing in
`SwaggerConfig.swaggerWelcome`'s `@ConditionalOnMissingBean` introspection). Bumped to **3.1.1**,
Boot-4-compatible and the same target Dependabot PR #97 already proposed — this migration supersedes
that PR; PR #97 itself was left untouched.

Re-validated the `org.webjars:swagger-ui` `<dependencyManagement>` override (pinned to 5.32.15 for the
DOMPurify CVE-2026-75838 fix) against springdoc 3.1.1's own transitive default:
`mvn dependency:tree -Dverbose=true -Dincludes=org.webjars:swagger-ui` shows springdoc 3.1.1 manages
swagger-ui at **5.32.14** by default — one patch behind our pin. The override remains necessary
post-migration; it does not need a further version bump yet.

## Consequences
- **Positive:** closes the migration path for CVE-2026-59282 and CVE-2026-47834, the two gate-relevant
  findings that could not be patched within Spring Boot 3.5.x.
- **Positive:** `./mvnw spotless:apply` then `./mvnw clean verify` is green — 377 tests, 0 failures,
  0 errors, JaCoCo coverage gate met, with real Testcontainers-backed PostgreSQL 16 containers and
  real Flyway migrations (not a silently-skipped Flyway masked by a permissive `ddl-auto`).
- **Negative:** the Jackson 2 compatibility shim (`spring-boot-jackson2`) is deliberate technical debt;
  a future Jackson 3 port is now a tracked follow-up, not an open-ended deferral.
- **Negative:** the `dependency-check-maven` `-Psecurity` run to confirm CVE-2026-59282/47834 are
  actually gone from the report is a separate, network-dependent step from the rest of this migration
  (NVD data refresh); see the accompanying PR description for its outcome, since it can be
  time-sensitive and should not be assumed to have completed from this ADR alone.
- **Negative:** any future contributor bumping `spring-boot-starter-parent` again should re-check this
  ADR's list of coordinated changes (Flyway starter, Jackson 2 shim, Testcontainers artifact names,
  `PropertyReferenceException` package) rather than assuming a parent-version bump alone is safe.

## Alternatives Considered
- **Stay on Spring Boot 3.5.x and rely on `dependency-check-suppressions.xml`** — rejected: both CVEs
  are reachable in BrewDeck's actual code paths (query filters, JPA sorting), not suppressible as
  false positives, and CVE-2026-59282 is above the CI gate threshold.
- **Port straight to Jackson 3 in the same change** — rejected: bundles an unrelated, larger-risk
  refactor (every controller/test touching JSON) into a security-driven migration; done separately so
  each change can be reviewed and rolled back independently.
- **Wait for a later Boot 4.x patch release** — rejected: 4.1.1 is the version verified against this
  codebase now; there is no indication a later patch changes any of the breaking changes documented
  above, and deferring leaves the two open CVEs unresolved longer than necessary.

## Notes
See `pom.xml` inline comments for the per-dependency rationale (Flyway starter, Jackson 2 shim,
swagger-ui override) and `.claude/project-state.md` "Recently Worked On" for the round-by-round CVE
remediation history this migration concludes.
