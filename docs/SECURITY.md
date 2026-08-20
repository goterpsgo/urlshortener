# SECURITY

Authentication and authorization are now fully wired up. Summary of what's in place:

New com.example.urlshortener.auth package:

- `AppUser` / `AppUserRepository` — user accounts backed by Postgres/H2 (`V2__create_users_table.sql`)
- `CustomUserDetailsService` — loads users for Spring Security
- `JwtService` — issues/validates HMAC-signed JWTs (via jjwt, Apache 2.0 licensed) carrying username + role, signed with `JWT_SECRET`. The exact algorithm (HS256/HS384/HS512) is chosen by jjwt's `Keys.hmacShaKeyFor()` based on the byte length of `JWT_SECRET` — it isn't hardcoded, so it only lands on HS512 if the configured secret is at least 64 bytes.
- `JwtAuthenticationFilter` — reads Authorization: Bearer `<token>`, populates the security context per-request (no DB hit needed per request)
- `RegisterRequest`/`LoginRequest`/`AuthResponse` DTOs + AuthController — `POST /auth/register`, `POST /auth/login`

Rewired SecurityConfig: stateless sessions, CSRF disabled (correct for token-based auth, no cookies involved), PasswordEncoder (BCrypt) + AuthenticationManager beans, JWT filter registered ahead of the standard auth filter.

Authorization: every user gets role USER today, and the role is embedded in the JWT as a claim → mapped to `ROLE_USER` as a Spring Security authority. There's no admin-gated endpoint yet, but the plumbing is live — `.hasRole("ADMIN")` in `SecurityConfig`, or `@PreAuthorize("hasRole('ADMIN')")` on a controller method, works today whenever you add one.

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
