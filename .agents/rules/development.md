# Development rules

## Implementation

- Before editing: trace the affected flow and all callers of shared functions.
- Reuse: prefer existing code and dependencies; add only required structure.
- Packages: follow nearby code under `com.team3`.
- Java variable declarations: avoid `var`; prefer explicit types.
- Domain layers: no established convention; avoid empty controllers/services/repositories.
- Formatting: follow `.editorconfig`, the Eclipse formatter, and Checkstyle.
- Diff scope: inspect formatter changes; exclude unrelated cleanup.

## API and security

- Validation errors: preserve ProblemDetail `field` and `message`; exclude rejected values.
- Unexpected errors: preserve generic response detail.
- HTTP status: preserve framework statuses; extend the shared handler where appropriate.
- Input: validate external values.
- Sensitive data: exclude credentials, stack traces, SQL, and sensitive request values from responses and added logs.
- Secrets: keep `.env` and real credentials untracked.
- Profiles: preserve isolation, restricted Actuator exposure, and production API-doc defaults unless explicitly changed by the task.

## Persistence

- Entity loading: account for disabled Open Session in View and transaction boundaries.
- Schema changes: provide an explicit, reviewable application strategy compatible with `ddl-auto: none`.
- Auto-DDL: do not silently enable it.

## Verification

- Test patterns: reuse JUnit, AssertJ, and MockMvc conventions.
- Nontrivial logic: add a focused regression check.
- Database tests: use the test profile and `integration` tag.
- PostgreSQL checks: do not assume an in-memory database is equivalent.
- During development: run focused tests.
- Java/build changes: run `./gradlew check`.
- Persistence changes: run `integrationTest` with the test database available.
- Documentation changes: check links, accuracy, and diff formatting.
- Unavailable checks: report skipped checks and their causes explicitly.
- Working tree: preserve unrelated changes; do not reset work for verification.
- Data: do not remove database volumes or modify shared/production data as a verification shortcut.
