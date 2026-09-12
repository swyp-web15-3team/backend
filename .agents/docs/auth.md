# Token authentication

- Successful JSON responses use `ApiResponse<T>` (`{"data": ...}`). Login and refresh return 200; logout uses `@ResponseStatus(NO_CONTENT)` with no body. Errors retain the unwrapped ProblemDetail format.
- Login and refresh token fields are nested under `data`; clients must read `data.accessToken`, `data.refreshToken`, and (login only) `data.isNewUser`. Clients derive access-token expiry from the JWT `exp` claim.
- Spring Security supplies default cache-prevention headers including `no-store` and the common `Referrer-Policy: no-referrer` header.
- `POST /auth/kakao` accepts JSON `{"code":"..."}` from the Next.js server, verifies the Kakao user, resolves an internal Long user ID, and returns `accessToken`, `refreshToken`, and `isNewUser`. `isNewUser` is true only when this request creates the user; existing users and concurrent creation conflict recovery return false. Code must be nonblank and at most 2048 characters.
- Next.js owns OAuth initiation, browser-bound single-use expiring `state` validation, denial handling, and token cookie setting. Exchange a code only after validating state; the backend creates no HTTP session or cookies.
- Backend `GET /auth/kakao` and `GET /auth/kakao/callback` have been removed. Frontend integration is required before using this flow.
- Kakao setup: set `KAKAO_LOGIN_ENABLED=true`, `KAKAO_CLIENT_ID` (REST API key), and `KAKAO_REDIRECT_URI` matching the URI registered with Kakao. Set `KAKAO_CLIENT_SECRET` if enabled in Kakao.
- Default frontend callback: `http://localhost:3000/auth/kakao/callback`; use HTTPS in production. The authorization request, backend `KAKAO_REDIRECT_URI`, and Kakao registration must use the same URI.
- Integration contract and manual verification: [Kakao login](../../tools/kakao-login/README.md).
- Send access tokens as `Authorization: Bearer <accessToken>`; they expire after 15 minutes.
- `POST /auth/refresh` accepts JSON `{"refreshToken":"..."}` and returns
  `accessToken` and `refreshToken`. Clients may decode JWT `exp` only for expiry metadata;
  this does not replace backend signature verification.
  Save the new refresh token: the previous token is invalidated. Call without an expired access-token header.
- Refresh tokens expire 14 days after initial issuance; rotation does not extend this deadline.
  Only SHA-256 hashes are stored, using JPA. Flyway SQL creates the table.
- `POST /auth/logout` accepts the same JSON and returns 204, revoking that refresh token.
  Existing access tokens remain valid until expiry. Each issuance creates a separate session.
- Tokens are sent in JSON, not cookies; use HTTPS. Cookie-based OAuth/token transport will need CSRF protection.
- Set a random, Base64-encoded `JWT_SECRET` of at least 32 bytes; use the same key on all app instances.
- `JwtProperties` and `KakaoProperties` bind `auth.jwt` and `auth.kakao` via `@ConfigurationProperties`, registered in `BackendApplication`. JWT lifetimes are validated when binding; key validation remains in `JwtConfig`, and enabled Kakao client validation remains in `KakaoClient`.
  Lifetimes and issuer are configured under `auth.jwt` in `application.yml`.
- Health is public; API docs are public when enabled by the profile. Other endpoints require authentication.
- Expired refresh rows are retained; add scheduled cleanup when retention becomes an operational concern.

- Setup: run `openssl rand -base64 32` and place the result in `.env` as `JWT_SECRET`.
- Verification: `./gradlew check`; no integration tests or `integrationTest` task are added in this branch.

- Structure: `auth/AuthController` handles Kakao code exchange, refresh, and logout; `AuthService` coordinates token operations and JPA user lookup/creation.
- `jwt/JwtProvider` issues access tokens; `token/` owns refresh-token persistence and rotation.
- `global/security/SecurityConfig` owns HTTP authorization, CSRF/session policy, and Resource Server JWT authentication.
- `auth/jwt/JwtConfig` provides the clock, signing key, encoder, decoder, and issuer validation.
  `KakaoClient` exchanges codes and retrieves the Kakao user ID; `user/User` and `user/UserRepository` persist the auto-increment Long user ID and unique `(provider, providerId)` pair in `users`. `Provider` is stored by enum name; `providerId` is an opaque string of 1 to 255 characters. Kakao login supplies `Provider.KAKAO` and the Kakao user ID as a string.

- Migrations: immutable V1 creates refresh-token storage; V2 creates users with BIGINT IDENTITY IDs and unique `(provider, provider_id)`; V3 replaces refresh-token `subject` with a non-null BIGINT `user_id` FK and `ON DELETE CASCADE`. Refresh-token row IDs remain UUIDs. JWT `sub` is the decimal string of the internal Long user ID.
- V3 preserves token hashes and expiry by matching existing subjects to `users.id::text`. Unmapped subjects cause migration failure and transaction rollback; investigate their provenance before retrying, without guessing account ownership or deleting data.
- Before deployment: inspect the target DB Flyway history and schema. Databases with different historical V2/V3 files require a separately reviewed reconciliation; do not edit migrations, repair checksums, or recreate databases to bypass a mismatch.
- Deploy the schema and application together with old instances stopped: V3 removes the column used by the old application.
