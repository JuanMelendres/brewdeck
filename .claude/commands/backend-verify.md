# Backend Verify

Run or suggest the BrewDeck backend verification flow.

## Verification Flow

Run or suggest the following steps (append `.cmd` to `./mvnw` if explicitly on Windows CMD):

Steps:

1. Apply formatting:

```bash
./mvnw spotless:apply
```

2. Run focused tests (Optional): If a specific area changed and you want rapid feedback before a full build:

```bash
./mvnw -Dtest=ClassName test
```

3. Run full verification:

This executes all unit and integration tests, and generates JaCoCo coverage reports.

```bash
./mvnw clean verify
```

4. If failures occur:

- Identify the failing test
- Read the relevant file
- Explain the root cause
- Propose the smallest fix
- Update tests if needed
- Suggest a Conventional Commit

5. Post-Success Actions:

If clean verify passes successfully:

    - Acknowledge that the build is stable.

    - (Optional) Remind the user to check JaCoCo reports or SonarCloud analysis if this completes a major feature.