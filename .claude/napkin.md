# Napkin Runbook

## Curation Rules
- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

## Execution & Validation (Highest Priority)
1. **[2026-07-03] `mvnw` tracked as non-executable (100644)**
   Do instead: run Maven via `sh ./mvnw ...`. Never `chmod +x mvnw` — it flips the tracked mode to 100755 and dirties the diff; if done, `git checkout brewdeck-api/mvnw` to revert.
2. **[2026-07-03] Spotless check fails the build if unformatted**
   Do instead: always `sh ./mvnw -q spotless:apply` before `sh ./mvnw clean verify`.
3. **[2026-07-03] `develop` merges can leave conflict markers committed (broke CoffeeServiceTest compile)**
   Do instead: before verify, `grep -rn '^<<<<<<<\|^>>>>>>>\|^=======$' brewdeck-api/src`; resolve then build.
4. **[2026-07-03] JaCoCo coverage gate enforced in `verify`**
   Do instead: add tests for new code (service+controller+integration) or build fails at jacoco:check.

## Domain Behavior Guardrails
1. **[2026-07-03] Collection GET endpoints must return `PageResponse<T>`**
   Do instead: use `@PageableDefault(size=10, sort="id", ASC)`; detail GET returns DTO directly.
2. **[2026-07-03] Integration tests run on a shared Testcontainers DB**
   Do instead: never assert single-record counts; assert `greaterThanOrEqualTo` or fully control the dataset. Never assert `$[0]` on paginated bodies — use `$.content[0]`.
3. **[2026-07-03] Validation/response messages get HTML-sanitized**
   Do instead: no special symbols (write "degrees Celsius", not °C).
4. **[2026-07-03] New DTOs/requests/filters use Java records; controllers thin, logic in services**
   Do instead: match existing package style (coffee/method/recipe/session/common), Lombok `@RequiredArgsConstructor`, `@Slf4j` for logs.

## User Directives
1. **[2026-07-03] Commit workflow**
   Do instead: implement + verify, then create commits ONLY when asked; when told "just give the message", do not commit. Never push unless explicitly asked. Split logical changes into separate commits (disjoint file sets).
2. **[2026-07-03] User commits/merges between turns**
   Do instead: re-check `git status`/`git log` at task start; prior CORS/dashboard/CI landed via user between turns — don't assume worktree state carries over.
3. **[2026-07-03] Commit trailers required**
   Do instead: end commit body with the `Co-Authored-By: Claude Opus 4.8 (1M context)` and `Claude-Session:` lines.
4. **[2026-07-03] Session task flow**
   Do instead: on "next task", consult roadmap order + pick smallest safe change; use task-template (goal/DoD/risks/files/steps/tests/verify/commit). Update `.claude/project-state.md` + `roadmap.md` when a phase/task completes.
5. **[2026-07-05] Frontend PRs target `develop`, NOT `master`**
   Do instead: feature branches (e.g. `feature/recipe-crud`) PR into `origin/develop`; `develop` → `master` later. `gh pr create --base develop`. User merges/advances develop between turns (PRs #33/#34/#35), so `git fetch` + re-check `origin/develop` before opening PRs or assuming branch is ahead.
6. **[2026-07-05] Don't keep pushing to a branch that has an open PR the user may merge**
   Do instead: a PR merges the branch tip AT MERGE TIME, and the user merges between turns — so commits pushed after the PR opens can be orphaned if they merge early (PR #35 merged an early tip; 8 later commits stranded, needed follow-up PR #36). Keep one branch = one PR scope; once a PR is up, branch fresh off `develop` for further work, or confirm before piling more commits on. On "merge PR", first `gh pr view <n> --json state` — it may already be MERGED.
