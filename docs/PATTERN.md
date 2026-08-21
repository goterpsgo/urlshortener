# PATTERN USED


The full typical set of items for a Spring Boot API endpoint with persistence:

```
┌────────────┬─────────────────────────────────────────────────────────────────────┬─────────────────────────────────┐
│   Layer    │                                Role                                 │          Your example           │
├────────────┼─────────────────────────────────────────────────────────────────────┼─────────────────────────────────┤
│ Controller │ HTTP routing — maps requests to service calls, DTOs in/out          │ LinkController                  │
├────────────┼─────────────────────────────────────────────────────────────────────┼─────────────────────────────────┤
│ DTOs       │ Shape of data in/out over the wire (request + response)             │ CreateLinkRequest, LinkResponse │
├────────────┼─────────────────────────────────────────────────────────────────────┼─────────────────────────────────┤
│ Service    │ Business logic, orchestrates repository calls, transaction boundary │ LinkService                     │
├────────────┼─────────────────────────────────────────────────────────────────────┼─────────────────────────────────┤
│ Repository │ Data access (JPA queries)                                           │ LinkRepository                  │
├────────────┼─────────────────────────────────────────────────────────────────────┼─────────────────────────────────┤
│ Entity     │ Maps to the DB table                                                │ Link                            │
└────────────┴─────────────────────────────────────────────────────────────────────┴─────────────────────────────────┘
```

## Request flow:

&emsp;_Controller_

&emsp;&emsp;→ receives a _DTO_

&emsp;&emsp;→ calls _Service_

&emsp;&emsp;→ Service uses _Repository_ to load/save the _Entity_

&emsp;&emsp;→ Service/Controller maps the Entity back into a response DTO.

Not every endpoint needs all five from day one (e.g., a stateless computation endpoint has no entity/repository), but for anything reading or writing data — like your Link feature — this is the standard shape, and it's exactly what you already have in com.example.urlshortener.link.

## Ownership-scoped reads/writes

`GET /api/links` and `PUT /api/links/{id}` follow a pattern worth reusing for any future per-user resource: the controller pulls the caller's username straight off `Authentication.getName()` (no separate user lookup needed, since `JwtAuthenticationFilter` already put it there) and passes it into the service alongside the id. `LinkRepository.findByIdAndOwnerUsername(id, ownerUsername)` does the ownership check as part of the query itself, so a link that exists but belongs to someone else looks identical to one that doesn't exist — both come back `404`, never `403`, which avoids leaking which ids are in use.

# Frontend pattern

The React app (`frontend/src/`) is a flat, page-per-route SPA with no external state library — plain React built-ins throughout.

```
┌────────────────┬─────────────────────────────────────────────────────┬──────────────────────────┐
│     Layer      │                        Role                         │        Your example      │
├────────────────┼─────────────────────────────────────────────────────┼──────────────────────────┤
│ Entry point    │ Mounts the app                                      │ main.jsx                 │
├────────────────┼─────────────────────────────────────────────────────┼──────────────────────────┤
│ Router         │ Route table + auth gate                             │ App.jsx                  │
├────────────────┼─────────────────────────────────────────────────────┼──────────────────────────┤
│ Auth context   │ Global auth state via React Context, not a store    │ AuthContext.jsx          │
├────────────────┼─────────────────────────────────────────────────────┼──────────────────────────┤
│ API client     │ Thin fetch wrapper — auth header, error handling    │ api.js                   │
├────────────────┼─────────────────────────────────────────────────────┼──────────────────────────┤
│ Pages          │ One component per route, own local state + form     │ Login, Register,         │
│                │ handling, calls the API client directly             │ Dashboard, Links         │
└────────────────┴─────────────────────────────────────────────────────┴──────────────────────────┘
```

## State management

No Redux/Zustand/Recoil/MobX/Jotai. Two kinds of state, each handled at the layer where it's needed:

- **Global (auth token)** — `AuthContext.jsx` holds the token in `useState`, exposes `login`/`register`/`logout`, and persists the token to `localStorage` via `api.js` (`getToken`/`setToken`/`clearToken`) so it survives a page reload. `App.jsx`'s `RequireAuth` wrapper reads `useAuth().token` to gate the `Dashboard` route, redirecting to `/login` if absent.
- **Local (everything else)** — each page owns its own form/UI state with `useState` (e.g. `Dashboard`'s `originalUrl`/`links`/`error`, `Login`'s `username`/`password`/`error`, `Links`'s fetched `links` list plus its `editingId`/`editValue` inline-edit state). Nothing is lifted higher than the page that uses it, and no state is shared between pages except the auth token — `Links` re-fetches its own list on mount rather than reading anything `Dashboard` created.

## API access

`api.js` is a single thin wrapper around `fetch`: `request()` attaches the `Authorization: Bearer <token>` header when a token is present, throws on non-2xx responses, and JSON-decodes the body. Pages call the exported functions (`login`, `register`, `createLink`, `listLinks`, `updateLink`) directly — there's no data-fetching library (React Query, SWR) and no caching layer; each page fetches what it needs on demand and holds the result in local state.

## Routing

`react-router-dom`'s `BrowserRouter` (mounted with `basename="/app"` to match the backend's `SpaForwardingController` prefix) plus a flat `Routes` table in `App.jsx` — one route per page, no nested/layout routes yet.

This is proportionate to the app's current size (three pages, one piece of shared state). Reach for a dedicated store or data-fetching library only once state needs to be shared across more pages or server data needs caching/revalidation beyond a single fetch.
