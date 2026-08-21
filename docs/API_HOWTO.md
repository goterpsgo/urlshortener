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

## 4. List your links

Also requires authentication, and only returns links created by the calling user:

```bash
curl http://localhost:8080/api/links \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

Response — `200 OK`, newest first:

```json
[
  {
    "id": 1,
    "shortCode": "aZ3x9Q",
    "shortUrl": "http://localhost:8080/aZ3x9Q",
    "originalUrl": "https://example.com/some/very/long/path"
  }
]
```

## 5. Edit a link

Updates the destination URL of a link you own, identified by its `id` (from create or list):

```bash
curl -X PUT http://localhost:8080/api/links/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com/a-different-path"}'
```

`originalUrl` has the same validation as create. The `shortCode` and `shortUrl` are unchanged — only the redirect target updates.

Response — `200 OK` with the updated link, same shape as list/create.

Editing a link you don't own — or one that doesn't exist — returns `404 Not Found` (not `403`), so ownership can't be probed by id.

## 6. Check your feature flags

Public — no token required, so pre-login pages can read it too. Include a Bearer token to get your own per-user override instead of the environment default:

```bash
# Anonymous — environment default only
curl http://localhost:8080/api/features

# Authenticated — per-user override if set, otherwise the environment default
curl http://localhost:8080/api/features \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

Response — `200 OK`:

```json
{
  "h1Green": false,
  "h1GreenOverride": null
}
```

`h1Green` is the effective value (what the UI should actually do); `h1GreenOverride` is the raw per-user override behind it — `null` means "no override, using the environment default", otherwise `true`/`false`. Anonymous requests always get `h1GreenOverride: null`. `h1Green` controls whether the Dashboard's `h1` renders in green. The environment default comes from `FEATURE_H1_GREEN` (see `.env.example`).

## 7. Set your own feature-flag override

Requires authentication — this always sets *your own* override, there's no way to set another user's:

```bash
curl -X PUT http://localhost:8080/api/features \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"h1Green": true}'
```

`h1Green` accepts `true` (force on), `false` (force off), or `null` (clear the override, fall back to the environment default). Response — `200 OK` with the same shape as the `GET` above, reflecting the new state immediately. There's a UI for this at `/toggles` in the frontend (the "Feature Toggles" page, linked from the Dashboard and Links nav).

## 8. Check who you are

Requires authentication. Mainly useful for a client (like the frontend) deciding whether to show admin-only UI:

```bash
curl http://localhost:8080/api/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

Response — `200 OK`:

```json
{
  "username": "alice",
  "isAdmin": false
}
```

## 9. Admin: view or set another user's feature flag by id

Requires an `ADMIN` account (see `docs/SECURITY.md`'s "Admin bootstrap" section — there's no self-service way to become an admin). A non-admin or anonymous request gets `403 Forbidden`; an unknown `userId` gets `404 Not Found`.

```bash
# View
curl http://localhost:8080/api/admin/users/2/features \
  -H "Authorization: Bearer <admin-token>"

# Set (true, false, or null to clear back to the environment default)
curl -X PUT http://localhost:8080/api/admin/users/2/features \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"h1Green": true}'
```

Response — `200 OK`:

```json
{
  "userId": 2,
  "username": "bob",
  "h1Green": true,
  "h1GreenOverride": true
}
```

Same UI at `/toggles` has an admin-only section (visible when `/api/me` reports `isAdmin: true`) for looking up a user by id and setting this.

## 10. Follow a short link

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
| GET | `/api/links` | Yes (Bearer token) | List your own links |
| PUT | `/api/links/{id}` | Yes (Bearer token) | Edit a link you own |
| GET | `/api/features` | No (per-user override with a Bearer token) | Get effective feature flags |
| PUT | `/api/features` | Yes (Bearer token) | Set your own feature-flag override |
| GET | `/api/me` | Yes (Bearer token) | Check your username and admin status |
| GET | `/api/admin/users/{userId}/features` | Yes (`ADMIN` role) | View any user's feature-flag override by id |
| PUT | `/api/admin/users/{userId}/features` | Yes (`ADMIN` role) | Set any user's feature-flag override by id |
| GET | `/{shortCode}` | No | Redirect to the original URL |

## Notes

- All authenticated requests use `Authorization: Bearer <token>` — no cookies or sessions are involved (the API is stateless).
- Every new account created via `/auth/register` gets the `USER` role; the only `ADMIN` account is seeded by a migration (see `docs/SECURITY.md`'s "Admin bootstrap" section) — there's no self-service or API path to become an admin.
- Links are scoped to the account that created them: `GET /api/links` and `PUT /api/links/{id}` only ever see/touch the calling user's own links.
- Feature flags resolve to an environment-level default (see `.env.example`'s `FEATURE_H1_GREEN`), available even to anonymous requests; an authenticated request gets that user's own override instead if one's been set, via `PUT /api/features` or the `/toggles` UI. An `ADMIN` can set any user's override by id via `/api/admin/users/{userId}/features`.
- This is a development/testing guide — treat any token or credential used here as a draft, and don't reuse example credentials for real accounts.
