# SECURITY

Authentication and authorization are now fully wired up. Summary of what's in place:

New com.example.urlshortener.auth package:

- `AppUser` / `AppUserRepository` — user accounts backed by Postgres/H2 (`V2__create_users_table.sql`)
- `CustomUserDetailsService` — loads users for Spring Security
- `JwtService` — issues/validates HMAC-signed JWTs (via jjwt, Apache 2.0 licensed) carrying username + role, signed with `JWT_SECRET`. The exact algorithm (HS256/HS384/HS512) is chosen by jjwt's `Keys.hmacShaKeyFor()` based on the byte length of `JWT_SECRET` — it isn't hardcoded, so it only lands on HS512 if the configured secret is at least 64 bytes.
- `JwtAuthenticationFilter` — reads Authorization: Bearer `<token>`, populates the security context per-request (no DB hit needed per request)
- `RegisterRequest`/`LoginRequest`/`AuthResponse` DTOs + AuthController — `POST /auth/register`, `POST /auth/login`

Rewired SecurityConfig: stateless sessions, CSRF disabled (correct for token-based auth, no cookies involved), PasswordEncoder (BCrypt) + AuthenticationManager beans, JWT filter registered ahead of the standard auth filter.

Authorization: every account created via `/auth/register` gets role USER — there's no way to self-register as `ADMIN`. The role is embedded in the JWT as a claim → mapped to `ROLE_<role>` as a Spring Security authority. The first (and currently only) `ADMIN`-gated resource is `/api/admin/**` (see "Admin-gated user management" below), authorized via `.hasRole("ADMIN")` in `SecurityConfig` rather than `@PreAuthorize` — method security (`@EnableMethodSecurity`) isn't enabled in this app, so a future admin-only controller method should follow the same `SecurityConfig` request-matcher pattern rather than reaching for `@PreAuthorize`.

How to use it:

```bash
curl -X POST localhost:8080/auth/register -H "Content-Type: application/json" -d '{"username":"alice","password":"correcthorsebattery"}'
# → {"token": "..."}
curl -X POST localhost:8080/api/links -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"originalUrl":"https://example.com"}'
```

One thing worth deciding next: right now, anyone can hit `/auth/register` and create an account — is that the intent (self-service signup), or should account creation be admin-only/invite-based for this app?

## `JwtAuthenticationFilter` usage

`JwtAuthenticationFilter` (`src/main/java/com/example/urlshortener/auth/JwtAuthenticationFilter.java`) is a Spring `OncePerRequestFilter` that runs once per incoming HTTP request:

- It reads the `Authorization` header and checks for a `Bearer <token>` value.
- If present, and no authentication is already set on the `SecurityContext`, it hands the token to `JwtService.isValid(token)`.
- On a valid token, it extracts the username and role via `JwtService`, builds a `UsernamePasswordAuthenticationToken` with authority `ROLE_<role>`, attaches request details, and sets it on `SecurityContextHolder` — authenticating the request without a DB lookup.
- If the header is missing, malformed, or the token is invalid, the filter does nothing and simply passes the request along; downstream authorization then denies it as unauthenticated.
- The filter always calls `filterChain.doFilter(...)`, so it never short-circuits the chain itself.

It's wired into `SecurityConfig` (`src/main/java/com/example/urlshortener/config/SecurityConfig.java:26,40`), which injects the `JwtAuthenticationFilter` bean and registers it with `http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` — so JWT-based auth is evaluated before Spring Security's standard username/password filter, for every request except the `permitAll()` routes (`/auth/**`, `GET /{shortCode}`, `GET /app/**`, and optionally the H2 console).

## Link ownership (authorization, not just authentication)

Being authenticated is enough to hit `GET /api/links` or `PUT /api/links/{id}`, but not enough to see or edit *anyone's* links — each `Link` row now carries `owner_username` (`V3__add_owner_to_links.sql`), set from `Authentication.getName()` at creation time. `LinkRepository.findByOwnerUsernameOrderByCreatedAtDesc` / `findByIdAndOwnerUsername` do the scoping at the query level, so:

- Listing only ever returns the caller's own links.
- Editing a link that exists but belongs to another user returns `404 Not Found`, identical to editing an id that doesn't exist at all — this is deliberate: it prevents an attacker from using the response code to enumerate which link ids exist (an IDOR/enumeration guard, not just an access-control check).

## Public feature-flag defaults

`GET /api/features` is intentionally on the `permitAll()` list (`SecurityConfig`) — unlike `/api/links`, this is deliberate, not an oversight:

- The response only ever contains the environment-level default (or an authenticated user's own override) — never another user's override, and never anything else about the account. There's nothing in `FeatureFlagsResponse` that's sensitive to expose to a logged-out visitor.
- Pre-login pages (`Login`) need this to render feature-gated UI correctly before a user has a token.
- `FeatureFlagController` distinguishes anonymous from authenticated callers by checking for `AnonymousAuthenticationToken`, not a null `Authentication` — Spring Security's anonymous-authentication filter always populates *some* `Authentication` (principal `"anonymousUser"`), so a null check alone would never trigger and every request would incorrectly try a per-user lookup.

If a future flag's payload ever needs to carry something user-specific beyond a boolean, reconsider this — `permitAll()` here assumes the response is safe for anyone to read.

## Self-service feature-flag overrides

`PUT /api/features` is *not* on the `permitAll()` list — setting an override requires a valid Bearer token, and `FeatureFlagController.updateFeatures` always uses `authentication.getName()` as the target, with no id or username field in the request body. There's no way to set another user's override through this endpoint, self-service by construction rather than by a check that could be bypassed. Setting *someone else's* flag requires the separate, role-gated endpoint below.

## Admin-gated user management

`GET`/`PUT /api/admin/users/{userId}/features` let an admin view or set *any* user's `h1Green` override by numeric id — this is the one place in the app where a user can act on another account, so it's deliberately narrow:

- Gated in `SecurityConfig` with `auth.requestMatchers("/api/admin/**").hasRole("ADMIN")`, evaluated before the catch-all `anyRequest().authenticated()` — a non-admin (or anonymous) caller gets `403`, verified for both cases.
- `AdminFeatureFlagController` never accepts a username in the body, only a path-variable `userId` looked up via `AppUserRepository.findById` — an unknown id returns `404`, not a silent no-op.
- The response (`AdminFeatureFlagsResponse`) includes the target's `username` so the admin UI can confirm which account it's about to change before submitting.
- There's no audit log of who changed what — worth adding if this pattern grows beyond one boolean flag.

## Admin bootstrap

The first `ADMIN` account is seeded by `db/migration/V5__seed_admin_user.sql` rather than created through the API (there's intentionally no "promote to admin" endpoint — that would be a privilege-escalation surface). The migration inserts one row using Flyway placeholders (`${admin-username}` / `${admin-password-hash}`, configured in `application.yml`'s `spring.flyway.placeholders`), resolved from the required `ADMIN_USERNAME` / `ADMIN_PASSWORD_HASH` env vars (see `.env.example`) — same pattern as `JWT_SECRET`: no default, so the app won't start without them, and the actual password never appears in a committed file. `ADMIN_PASSWORD_HASH` must be a bcrypt hash (e.g. produced by the app's own `BCryptPasswordEncoder`, or any bcrypt-compatible tool) — never a plaintext password. Because Flyway only ever runs a given migration once per database, this seed doesn't re-run (or re-insert) on every restart; promoting additional admins today means a direct `UPDATE users SET role = 'ADMIN' WHERE username = ...`, matching the "direct DB edit" pattern already used for per-user feature overrides before the self-service endpoint existed.
