# SECURITY

Authentication and authorization are now fully wired up. Summary of what's in place:

New com.example.urlshortener.auth package:

- `AppUser` / `AppUserRepository` — user accounts backed by Postgres/H2 (`V2__create_users_table.sql`)
- `CustomUserDetailsService` — loads users for Spring Security
- `JwtService` — issues/validates HS512 JWTs (via jjwt, Apache 2.0 licensed) carrying username + role, signed with `JWT_SECRET`
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
