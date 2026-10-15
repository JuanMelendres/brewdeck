# Technical Spike: Port BrewDeck's own code to Jackson 3 and drop the `spring-boot-jackson2` shim

## 1. Summary

The Spring Boot 4 migration ([ADR-008](../../decisions/ADR-008-spring-boot-4-migration.md)) kept
Jackson 2 for BrewDeck's own code through the `spring-boot-jackson2` compatibility shim and logged
a real Jackson 3 port as follow-up debt. This spike measures how big that port is, checks whether
behavior changes, and recommends how to land it.

- **Question:** can we remove `spring-boot-jackson2` and move our code to `tools.jackson.*` without
  changing what the API returns?
- **Decision expected:** port now (and how), or keep the shim.

## 2. Background

- Boot 4.1 auto-configures Jackson 3 (`tools.jackson.databind.json.JsonMapper`). The shim brings
  back a second, Jackson 2 `ObjectMapper` bean (`jackson2ObjectMapper`) so classes that inject
  `com.fasterxml.jackson.databind.ObjectMapper` keep working.
- Jackson 2 stays on the classpath no matter what: `jjwt-jackson` 0.13.0, `swagger-core-jakarta`
  (springdoc) and `anthropic-java` 2.71.0 all depend on `com.fasterxml.jackson` 2.21.5. The port is
  about BrewDeck's own code and the shim, not about removing Jackson 2 from the build.
- `pom.xml` pins `jackson-2-bom` 2.21.5 (ADR-008) for those transitive users.

## 3. Problem Statement

The app runs two JSON mappers with separate configuration. Controllers write responses with
Jackson 3; the security filters and handlers that write error bodies by hand, the Ollama adapter,
and 19 test classes use the Jackson 2 bean. A future config change (date format, inclusion rules)
applied to one mapper would silently not apply to the other, so 401/403/429 bodies could drift
from every other error body. The shim is also legacy support that Spring may drop in a later
Boot release.

## 4. Goals

- Inventory every Jackson 2 use in BrewDeck's code.
- Confirm which mapper writes HTTP responses today.
- Prove (or disprove) a port with no behavior change, using the full test suite.
- Recommend scope, PR shape, and checks.

## 5. Non-Goals

- Removing Jackson 2 from the dependency tree (third-party libraries need it).
- Replacing `jjwt-jackson`, springdoc, or the Anthropic SDK.
- Changing any JSON shape, date format, or error contract.

## 6. Key Questions

- Q-001: Which classes and tests use Jackson 2 APIs, and how?
- Q-002: Which mapper serializes controller responses today?
- Q-003: Do error bodies written by the security handlers look the same after the port?
- Q-004: Does anything besides our code need the shim (springdoc, JWT, Claude SDK)?
- Q-005: Is this mechanical, or does it need code redesign?

## 7. Constraints

- Spring Boot 4.1.1, Java 21; Jackson 3.1.5 and 2.21.5 both resolved today.
- No API contract change: the frontend parses `ErrorResponse` (`timestamp`, `status`, `error`,
  `message`, `path`, `validationErrors`, `code`).
- Small, reviewable PRs via branch + PR; `./mvnw clean verify` green.
- Local-only deployment for now, so the risk window is small.

## 8. Options Considered

| Option | Description | Pros | Cons | Complexity | Risk |
|---|---|---|---|---|---|
| A. Keep the shim | Do nothing | Zero effort | Two mappers keep diverging; legacy module may be dropped by Spring; debt stays open | None | Low now, grows over time |
| B. Port everything in one PR | Remove the shim; swap `com.fasterxml.jackson.databind/core` imports for `tools.jackson.*` in 7 main and 19 test classes | One mapper; matches Boot 4 defaults; small diff | Touches 26 files at once | Low (mechanical) | Low (POC green) |
| C. Port in stages | First main classes (inject Jackson 3) with the shim still there, then tests, then drop the shim | Smaller PRs | Three PRs for ~40 changed lines; the middle state still has two mappers | Low | Low |

## 9. Evaluation Criteria

| Criterion | Description | Weight |
|---|---|---|
| Behavioral safety | Same JSON on the wire, all tests green | High |
| Maintainability | One mapper, one configuration | High |
| Effort | Developer time and review load | Medium |
| Reversibility | Easy to revert if something shows up | Medium |

## 10. Research Findings

### Finding 1: Jackson 2 use is small and mechanical (Q-001, Q-005)

Seven main classes, all with simple calls:

| Class | Jackson 2 use |
|---|---|
| `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler` | inject `ObjectMapper`, `writeValue(writer, ErrorResponse)` |
| `EmailVerificationRequiredFilter`, `AuthRateLimitFilter` | same pattern (403 / 429 bodies) |
| `SecurityConfig` | passes the `ObjectMapper` to the handlers and filters above |
| `OllamaRecipeSuggestionAdapter` | `writeValueAsString`, `readTree`, `readValue`, catches `JsonProcessingException` |
| `ClaudeRecipeSuggestionAdapter` | only `@JsonPropertyDescription` from `com.fasterxml.jackson.annotation` |

Tests: 19 classes import `ObjectMapper` (16 via `@Autowired`, 4 via `new ObjectMapper()`),
8 import `JsonNode`, 2 import `ObjectNode`.

Evidence: `grep -rE "com\.fasterxml\.jackson" brewdeck-api/src` on `develop` (2026-10-14).

Impact: no custom serializers, modules, or mapper configuration to port. Annotations
(`com.fasterxml.jackson.annotation`) are shared by Jackson 2 and 3 and need no change.

### Finding 2: controller responses already use Jackson 3 (Q-002)

A throwaway `@SpringBootTest` probe on `develop` listed both beans (`jacksonJsonMapper` →
`tools.jackson.databind.json.JsonMapper`, `jackson2ObjectMapper` →
`com.fasterxml.jackson.databind.ObjectMapper`). Spring MVC's JSON converter is
`JacksonJsonHttpMessageConverter`, which is the Jackson 3 one.

Impact: only the hand-written security and rate-limit bodies, the Ollama adapter, and tests use
Jackson 2. Porting them makes the app consistent with what controllers already do.

### Finding 3: same error JSON before and after (Q-003)

The probe printed the 401 body from `RestAuthenticationEntryPoint` and a 404 body from
`GlobalExceptionHandler`:

```
before: {"timestamp":"2026-10-14T17:48:30.760876Z","status":401,"error":"Unauthorized","message":"Authentication required","path":"/api/coffees","validationErrors":null,"code":null}
after:  {"timestamp":"2026-10-14T17:49:37.258938Z","status":401,"error":"Unauthorized","message":"Authentication required","path":"/api/coffees","validationErrors":null,"code":null}
```

Same field order, ISO-8601 `Instant`, explicit `null`s. The 404 body matched too.

### Finding 4: nothing else needs the shim (Q-004)

- `jjwt-jackson`, `swagger-core`, and `anthropic-java` create their own Jackson 2 mappers; none of them
  reads the shim's Spring bean.
- With the shim removed, `GET /v3/api-docs` returned 200 (34 KB) in the probe (docs enabled for the
  test).
- The `jackson-2-bom` pin in `pom.xml` must stay, because those libraries still resolve Jackson 2.

## 11. Proof of Concept Plan

- Remove `spring-boot-jackson2` from `pom.xml`.
- Change `com.fasterxml.jackson.databind.*` imports to `tools.jackson.databind.*`, and change
  `JsonProcessingException` to `tools.jackson.core.JacksonException`.
- Run `./mvnw clean verify` plus the probe.
- Time-box: 1 hour. Throwaway, not committed in this PR.

## 12. Proof of Concept Results

- **Change:** 26 files, +39 / −48 lines. Every change was an import swap or the exception rename;
  no logic changed. In Jackson 3, `JacksonException` is unchecked, so the existing `catch` blocks
  still compile and still turn bad JSON into `AiUnavailableException`.
- **`./mvnw clean verify`:** BUILD SUCCESS, 591 tests (590 + the probe), 0 failures, JaCoCo
  check passed. Spotless and PMD are not bound to `verify`; the PR must run them separately.
- **Probe after the change:** only the Jackson 3 `jacksonJsonMapper` bean exists; error bodies are
  unchanged (Finding 3); `/v3/api-docs` returns 200.
- **Limitations:**
  - `OllamaRecipeSuggestionAdapterTest` builds the adapter with a plain mapper and mocked HTTP.
    In the app the adapter will receive Boot's configured `JsonMapper`. The tests cannot show that,
    so the implementation PR needs one manual suggestion against a real `ollama serve`.
  - The edits were made with a script, so import order still needs `./mvnw spotless:apply`.

## 13. Trade-Off Analysis

| Trade-Off | Benefit | Cost |
|---|---|---|
| One PR (B) vs staged (C) | One review; no in-between state with two mappers | A 26-file diff, though each change is one line |
| Port now vs wait | Debt closed while the diff is mechanical and the app is local-only | A small amount of time not spent on features |

## 14. Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Different Jackson 3 defaults change a hand-written JSON body | Frontend misreads 401/403/429 | Low (probe identical) | Existing tests assert bodies; compare with the probe output |
| Ollama adapter receives Boot's `JsonMapper` with different settings | AI suggest fails | Low | Manual suggestion with `ollama serve` in the PR |
| Later dependency pulls `spring-boot-jackson2` back in | Two mappers again | Low | Note in ADR; `dependency:tree` check in review |
| Test-only `new ObjectMapper()` diverges from the app mapper | Tests pass, app differs | Low | Prefer the autowired `JsonMapper` where a Spring context exists |

## 15. Recommendation

**Recommended option: B, port everything in one PR.**

- **Why B:** the POC shows a mechanical, behavior-neutral change with the full suite green. It leaves
  one JSON mapper, which controllers already use.
- **Why not A:** keeps two mappers that can drift, and an open debt item.
- **Why not C:** three PRs for about 40 changed lines, and the middle state keeps both mappers.
- **Supporting assumption:** the error-body tests and the probe cover the JSON contract the frontend
  relies on.
- **Still to check in the PR:** one real Ollama suggestion, and `dependency:tree` showing no
  `spring-boot-jackson2`.

## 16. Decision

- Decision: Option B, port everything in one PR (`refactor/api-jackson3`).
- Date: 2026-10-14
- Owner: Juan
- Status: Accepted

## 17. Next Steps

1. If accepted: branch `refactor/api-jackson3`. Apply the POC change, run `./mvnw spotless:apply`
   and `./mvnw clean verify`, and do one manual AI suggestion against `ollama serve`.
2. Update ADR-008 with a note that the shim was removed. Update the CLAUDE.md stack line, the
   `pom.xml` comment, and project state.
3. Close project-state "Immediate Next Steps" item 7.

## 18. ADR Candidate

- ADR needed: No new ADR. This finishes the follow-up that
  [ADR-008](../../decisions/ADR-008-spring-boot-4-migration.md) recorded; add an update note there.
- Reason: no new architectural choice. It moves to the framework default that ADR-008 already
  named as the target.

## 19. Open Questions

- OQ-001: Replace `jjwt-jackson` with a Jackson-3-ready serializer when jjwt ships one?
  Out of scope; track separately.

## 20. Assumptions

- Assumption-001: Spring Boot keeps Jackson 3 as the default JSON mapper for the 4.x line.
- Assumption-002: no client depends on anything in the 401/403/429 bodies beyond the
  `ErrorResponse` fields above.
