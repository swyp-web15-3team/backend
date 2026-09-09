# Token authentication

- After Kakao OAuth verifies the user, call `AuthService.issue(internalUserId)` from the server.
  No public token-issuance endpoint is exposed.
- Send access tokens as `Authorization: Bearer <accessToken>`; they expire after 15 minutes.
- `POST /auth/refresh` accepts JSON `{"refreshToken":"..."}` and returns
  `accessToken`, `refreshToken`, and `expiresIn` (access-token lifetime in seconds).
  Save the new refresh token: the previous token is invalidated. Call without an expired access-token header.
- Refresh tokens expire 14 days after initial issuance; rotation does not extend this deadline.
  Only SHA-256 hashes are stored, using JPA. Flyway SQL creates the table.
- `POST /auth/logout` accepts the same JSON and returns 204, revoking that refresh token.
  Existing access tokens remain valid until expiry. Each issuance creates a separate session.
- Tokens are sent in JSON, not cookies; use HTTPS. Cookie-based OAuth/token transport will need CSRF protection.
- Set a random, Base64-encoded `JWT_SECRET` of at least 32 bytes; use the same key on all app instances.
  Lifetimes and issuer are configured under `auth.jwt` in `application.yml`.
- Health is public; API docs are public when enabled by the profile. Other endpoints require authentication.
- Expired refresh rows are retained; add scheduled cleanup when retention becomes an operational concern.

- Setup: run `openssl rand -base64 32` and place the result in `.env` as `JWT_SECRET`.
- Verification: `./gradlew check`; no integration tests or `integrationTest` task are added in this branch.

- Structure: `AuthController` and `AuthService` expose and coordinate authentication flows;
  `jwt/JwtProvider` issues access tokens; `token/` owns refresh-token persistence and rotation.
- `TokenConfiguration` wires Spring Security’s built-in bearer-token filter and JWT decoder.
  Add `KakaoClient` when implementing Kakao OAuth; no placeholder client or custom JWT filter is present.
