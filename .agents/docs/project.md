# Project context

- Status: backend foundation; no business-domain controllers, services, entities, or repositories yet.
- Architecture: no agreed domain architecture; do not assume one.

## Sources of truth

- `build.gradle`: Java 21, Spring Boot 3.5.16, MVC, JPA, validation, Actuator,
  springdoc, Lombok, PostgreSQL, and JUnit Platform; dependency versions live here.
- `src/main/java/com/team3/BackendApplication.java`: application entry point.
- `src/main/java/com/team3/common/ApiExceptionHandler.java`: shared ProblemDetail
  handling, field validation errors, and generic unexpected-error responses.
- `src/main/resources/application*.yml`: shared defaults and local/test/prod profiles.
- `src/test/java/com/team3/`: MVC error-contract tests, profile tests, context smoke
  test, and PostgreSQL connectivity test.
- `config/`, `.editorconfig`: formatting and Checkstyle configuration.
- `.github/workflows/ci.yml`: CI checks and PostgreSQL 17 service.
- `README.md`, `.env.example`, `docker-compose.yml`: development setup.

## Runtime boundaries

- Persistence: Open Session in View disabled; automatic schema changes disabled (`ddl-auto: none`).
- Migrations: no migration tool configured.
- Local profile: optional `.env` import; API docs enabled.
- Shared defaults: API docs disabled; only Actuator health exposed; health/error internals hidden.
- `bootRun`: local profile by default.
- Compose: development setup; local app profile and PostgreSQL 17.

## Verification

- Working directory: repository root.
- Runner: checked-in Gradle wrapper.

| Command                                                       | Coverage / prerequisite                                                       |
| ------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `./gradlew test`                                              | Tests excluding the `integration` tag; current tests need no running database |
| `./gradlew test --tests 'com.team3.ApiExceptionHandlerTests'` | Focused error-contract regression tests                                       |
| `./gradlew spotlessCheck`                                     | Java formatting verification                                                  |
| `./gradlew spotlessApply`                                     | Rewrites Java formatting; inspect the resulting diff                          |
| `./gradlew check`                                             | Unit tests, Checkstyle, and Spotless; excludes integration tests              |
| `./gradlew integrationTest`                                   | Tagged integration tests; requires a reachable PostgreSQL 17 test database    |
| `./gradlew check integrationTest bootJar`                     | CI command, including executable archive build                                |

- Test defaults: database/user/password `backend_test`; address `localhost:5432`.
- Overrides: environment variables; no `.env` import in the test profile.
- Compose database: uses `.env`; may differ from the test database.
- Isolation: use a dedicated test database.
- Coverage gap: neither `check` nor the pre-push hook runs integration tests.
