# showfilix.com — Reverse-engineered API (findings)

Evidence gathered 2026-10-12 by inspecting the live site (Next.js App Router front-end)
and its client JS bundles. All calls below are same-origin and were verified live with
`curl`.

## Base

- Site: `https://showfilix.com`
- API base: `https://showfilix.com/api` (NestJS — errors look like
  `{"message","error","statusCode"}`).
- Common headers: `Accept: application/json`, `Content-Type: application/json`,
  `x-visitor-id: <uuid>` (client sends a per-device visitor id; optional).
- Auth header for protected routes: `Authorization: Bearer <accessToken>`.

## Catalog (public — no auth required)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/media/home` | Home page. `{latestUpdates:{title,groups[]}, sections[]}`. Each group: `{pageKey,persianName,mediaType,items[]}`. ~425 KB. |
| GET | `/api/media/latest` | `{items[]}` newest additions. |
| GET | `/api/media/browse` | Params: `type=movie|series`, `page`, `limit` (default 48), `sortBy` (e.g. `3`), `genre` (csv slugs), `country` (csv slugs), `anyCountry`, `dub=1`, `sub=1`, `from`, `to`, `exclude`. Response `{items[], meta:{page,pageSize,total,totalPages}}`. |
| GET | `/api/media/search` | Params: `q`, `type`, `page`, `limit` (default 24). Response `{query, items[]}`. |
| GET | `/api/media/{type}/{externalId}` | Details. `type` = `movie|series`, `externalId` = IMDb id (`tt…`). |
| GET | `/api/media/{type}/{externalId}/similar` | `{items[]}`. |
| GET | `/api/media/{type}/{externalId}/comments` | Comments. |
| GET | `/api/media/for-you` | `{personalized, taste, continueWatching[], sections[]}`. |
| GET | `/api/media/hero-backdrop`, `/api/media/hero-trailer` | Hero art (may be null). |

### Media object (catalog/detail)

```
id, type(movie|series), siteKey, providers[], externalId("tt0110912"), slug,
title (fa), titleEn, year, posterUrl, trailerUrl, quality("BluRay 1080p"),
ageRating("17+"), durationMinutes, plot, genres[], countries[],
people[{personId,name,role,avatarUrl}],
ratings{imdb, imdbVotes, userScore, criticsScore},
availability{hasSubtitle,hasDubbed,hasOnlinePlay,hasBilingual,...},
network, airDay, airingStatus  (series only)
```

## Playback links (requires login/subscription)

`GET /api/media/{type}/{externalId}/links?siteKey=<provider>` →

```
{ type, externalId, siteKey, playLinks[], downloadGroups[],
  seasons[], seriesDownloadGroups[],
  access:{ hasAccess, isExempt, subscription, subscriptionsRequired } }
```

When logged out: `playLinks:[]`, `access.hasAccess:false`. Playback therefore
requires a subscription account.

Stream proxy used by the web player: `GET /api/media/stream?url=<https url>`
(external URL must be https). Subtitles: `GET /api/media/subtitle/<...>?format=vtt`.

## Auth (`/api/auth/*`, NestJS)

The website's login form (`/auth/login`) authenticates with **e-mail + password** —
its own copy reads: "با ایمیل و رمز عبور وارد شوید" (sign in with e-mail and password).
Phone-OTP endpoints also exist on the backend but are not the login form.

| Method | Path | Body |
|---|---|---|
| POST | `/api/auth/login` | `{ email, password, deviceId }` → session |
| POST | `/api/auth/register` | `{ email, phone, password, displayName?, deviceId }` |
| GET | `/api/auth/me` | Bearer access token |
| POST | `/api/auth/refresh` | `{ refreshToken, deviceId }` |
| POST | `/api/auth/logout` | `{ refreshToken }` |
| POST | `/api/auth/otp/send` | `{ phone }` (SMS OTP — not the login form) |
| POST | `/api/auth/otp/login` | `{ phone, code, deviceId }` |
| POST | `/api/auth/forgot-password` `/reset-password` `/otp/reset-password` `/otp/verify-reset` | password reset |

**Session envelope** (verified against the client bundle's `applyAuthSession`, which
reads `e.accessToken`, `e.refreshToken`, `e.expiresIn`):

```json
{ "accessToken": "…", "refreshToken": "…", "expiresIn": "1h" }
```

**Verified live** (2026-10-12):

```
POST /api/auth/login {"email":"nobody@example.com","password":"WrongPass123!","deviceId":"…"}
 → 404 {"statusCode":404,"code":"ACCOUNT_NOT_FOUND","message":"حسابی با این ایمیل یافت نشد."}

POST /api/auth/login {"identifier":"…","password":"x"}
 → 400 {"message":["property identifier should not exist",
                   "password must be longer than or equal to 8 characters"]}
```

So the field is `email` (not `identifier`), passwords must be ≥ 8 characters, and
errors carry a `code` plus a Persian `message` (validation errors return an array).
Token storage client-side uses `showfilix_access_token`, `showfilix_refresh_token` and
`showfilix_access_expires_at`.

## Panel (auth)

`/api/panel/continue-watching`, `/api/panel/notifications`,
`/api/panel/subscription`, `/api/panel/subscription/purchases`, `/api/panel/comments`,
`/api/watchtime` — user library / progress.

## Notes / limitations

- Content pages (`/movies`, `/series`, `/movies/<tt…>`, `/search`) are server-rendered
  Next.js; a native client uses the JSON API above instead.
- `robots.txt` disallows `/api/`, `/auth/`, `/panel/`, `/feed`, `/titles/`.
- This is an unofficial client; a valid subscription is required for playback.
