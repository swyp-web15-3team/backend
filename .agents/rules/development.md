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

- ORM first: use existing JPA entities and Spring Data JPA repositories for application data access.
- Avoid raw SQL: do not introduce JdbcTemplate, direct JDBC, or native queries when ORM can express the required behavior; shorter code alone is not a reason to bypass ORM.
- Exceptions: use raw SQL only for a demonstrated ORM limitation or measured performance need, and document the concrete reason. Flyway schema migration SQL is allowed.
- Entity loading: account for disabled Open Session in View and transaction boundaries.
- Schema changes: provide an explicit, reviewable application strategy compatible with `ddl-auto: none`.
- Migration immutability: NEVER edit, delete, rename, or consolidate existing versioned migration files. This applies even before deployment and even if the database can be recreated.
- Subsequent schema changes: ALWAYS add a new migration file with a higher version; preserve all previous migration files exactly as written.
- Auto-DDL: do not silently enable it.

## Verification

- Test patterns: reuse JUnit, AssertJ, and MockMvc conventions.
- Nontrivial logic: add a focused regression check.
- During development: run focused tests.
- Java/build changes: run `./gradlew check`.
- Documentation changes: check links, accuracy, and diff formatting.
- Unavailable checks: report skipped checks and their causes explicitly.
- Working tree: preserve unrelated changes; do not reset work for verification.
- Data: do not remove database volumes or modify shared/production data as a verification shortcut.
