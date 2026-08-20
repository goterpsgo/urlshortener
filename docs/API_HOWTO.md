# API How-To

A quick guide to using the URL Shortener API: register, log in, create short links, and follow redirects.

Examples assume the app is running locally on the default port:

```
http://localhost:8080
```

## 1. Register an account

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "correcthorsebattery"}'
```

Requirements:
- `username`: 3–100 characters, required
- `password`: minimum 8 characters, required

Response — `201 Created`:

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

If the username is already taken, you'll get `409 Conflict`.

Save the `token` — you'll need it for every request below.

## 2. Log in (existing account)

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "correcthorsebattery"}'
```

Response — `200 OK`:

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

Wrong username or password returns `401 Unauthorized`.

The token is a JWT signed with `JWT_SECRET` and expires after `app.jwt.expiration-minutes` (60 minutes by default) — log in again to get a fresh one.

## 3. Create a short link

This endpoint requires authentication. Pass the token from step 1 or 2 as a Bearer token:

```bash
curl -X POST http://localhost:8080/api/links \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com/some/very/long/path"}'
```

`originalUrl` must be a valid, non-blank URL.

Response — `201 Created`:

```json
{
  "shortCode": "aZ3x9Q",
  "shortUrl": "http://localhost:8080/aZ3x9Q",
  "originalUrl": "https://example.com/some/very/long/path"
}
```

Missing or invalid token → `403 Forbidden`. (`SecurityConfig` doesn't configure a custom authentication entry point, so Spring Security falls back to its default `Http403ForbiddenEntryPoint` rather than `401 Unauthorized`.)

## 4. Follow a short link

Redirects are public — no token needed:

```bash
curl -i http://localhost:8080/aZ3x9Q
```

Response — `302 Found` with a `Location` header pointing at the original URL. In a browser, just visiting `http://localhost:8080/aZ3x9Q` redirects you straight there.

An unknown short code returns `404 Not Found`.

## Quick end-to-end example

```bash
# Register and capture the token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "correcthorsebattery"}' | jq -r .token)

# Create a short link
curl -s -X POST http://localhost:8080/api/links \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com"}'

# Follow it
curl -i http://localhost:8080/<shortCode-from-response>
```

## Endpoint summary

| Method | Path | Auth required | Purpose |
|---|---|---|---|
| POST | `/auth/register` | No | Create an account, get a token |
| POST | `/auth/login` | No | Get a token for an existing account |
| POST | `/api/links` | Yes (Bearer token) | Shorten a URL |
| GET | `/{shortCode}` | No | Redirect to the original URL |

## Notes

- All authenticated requests use `Authorization: Bearer <token>` — no cookies or sessions are involved (the API is stateless).
- Every new account currently gets the `USER` role; there's no admin-only endpoint yet.
- This is a development/testing guide — treat any token or credential used here as a draft, and don't reuse example credentials for real accounts.
