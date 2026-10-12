# Showfilix for Android TV (unofficial)

A native **Android TV** client for [showfilix.com](https://showfilix.com), written in
Kotlin with Jetpack Compose. Modelled on the community project
[`msharifi99/30nama-tizenos`](https://github.com/msharifi99/30nama-tizenos) (a Tizen TV
client for 30nama) but rebuilt as a native Android TV app that talks to showfilix's
own REST API.

> **Disclaimer:** this is an unofficial, unaffiliated project. It is not endorsed by or
> connected to showfilix. A valid showfilix subscription is required for playback.
> No content is bundled with this app.

---

## Download

| Artifact | Link |
|---|---|
| APK (debug, installable) | [`dist/showfilix-tv-1.0.5-debug.apk`](dist/showfilix-tv-1.0.5-debug.apk) |
| Release | [Releases page](../../releases/latest) |

```bash
adb connect <tv-ip>:5555
adb install -r showfilix-tv-1.0.5-debug.apk
adb shell am start -n com.showfilix.tv/.MainActivity
```

The app appears under Android TV apps as **شوفیلیکس**.

## Features

- **Home** — hero billboard plus content rows, from `/api/media/home`.
- **Browse** — paginated poster grid for movies and series with sort filters.
- **Search** — on-screen keyboard with live results.
- **Details** — full metadata, cast, and "similar titles".
- **Playback picker** — pressing Play opens a picker with every available
  **version** (دوبله فارسی / نسخه اصلی), **season**, **episode** and **quality**
  (1080p/720p/480p …) reported by the API, then plays the chosen stream.
- **Playback** — ExoPlayer through the site's `/api/media/stream` proxy. Streams are
  progressive MKV/MP4 and are read from `downloadGroups` / `seriesDownloadGroups`
  (the `playLinks` array is usually empty). The title/quality overlay fades out a few
  seconds after playback starts and comes back when paused.
- **Login** — e-mail + password (`POST /api/auth/login`), matching the website's own
  form. On-screen keyboard with Shift / caps-lock, and clipboard **paste** / **copy**
  for both the e-mail and password fields.
- **TV-first UI** — leanback launcher entry, D-pad navigation, focus rings.

## Screens & flow

```
Home ──▶ Details ──▶ Playback picker ──▶ Player
 │        └── version ▸ season ▸ episode ▸ quality
 │  └──▶ Browse (movies / series) ──▶ Details
 └──▶ Search ──▶ Details
Login (e-mail + password) is reachable from Home and Details.
```

## Reverse-engineered API

showfilix publishes no public API; the endpoints were recovered from the live
front-end's client bundles. The full map is in
[`showfilix-api-findings.md`](showfilix-api-findings.md). Highlights:

| Endpoint | Auth | Purpose |
|---|---|---|
| `GET /api/media/home` | no | Home rows + hero |
| `GET /api/media/browse?type=&page=&sortBy=&genre=&country=&dub=&sub=&from=&to=` | no | Paginated catalog |
| `GET /api/media/search?q=` | no | Search |
| `GET /api/media/{type}/{imdbId}` | no | Details (`tt…` ids) |
| `GET /api/media/{type}/{id}/similar` | no | Related titles |
| `GET /api/media/{type}/{id}/links` | **yes** | Playback links — streams are in `downloadGroups`/`seriesDownloadGroups[].items[].streamUrl` |
| `GET /api/media/stream?url=` | — | Stream proxy |
| `POST /api/auth/login` | — | E-mail + password sign-in (`{email,password,deviceId}` → `{accessToken,refreshToken,expiresIn}`) |
| `GET /api/auth/me`, `POST /api/auth/refresh`, `/logout` | — | Session |

## Build from source

Requires **JDK 17** and the Android SDK (**platform 35**, **build-tools 35.0.0**).

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug        # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # model tests against captured payloads
```

`local.properties` must point at the SDK (`sdk.dir=...`), or set `ANDROID_HOME`.

## Project layout

```
app/src/main/java/com/showfilix/tv/
  MainActivity.kt        entry point, fullscreen immersive
  ShowfilixApp.kt        Application + manual DI container
  data/                  API client, models, catalog repository
  auth/                  session storage + OTP auth repository
  ui/                    theme, navigator, screens, components
app/src/test/            unit tests over real captured API payloads
```

## Testing

`app/src/test/java/com/showfilix/tv/data/ModelsTest.kt` decodes **real payloads
captured from the live API** (saved under `app/src/test/resources/`) with the app's
`@Serializable` models, so the tests fail if the wire format drifts.

```
./gradlew :app:testDebugUnitTest   # 5 tests, 0 failures
```

## Known limitations

- **Sign-in is e-mail + password only** (matching the website). Google OAuth is not
  implemented, and the phone-OTP endpoints the backend also exposes are not surfaced.
- Account **registration** is not implemented in-app; create the account on the website.
- Sign-in and the full playback-link chain have been exercised against a real
  account over the API: login → `links` (`hasAccess: true`) → version/season/episode/
  quality selection → signed stream URL → `206` byte range with real video data
  (movies are MKV, episodes are MP4) through the proxy. What has **not** been run is
  the in-app ExoPlayer decode, because no Android TV device/emulator was available.
- Playback requires an account with access; otherwise the app shows a
  "subscription required" notice.
- Access tokens last ~5 minutes; the client refreshes them automatically on a 401.
- Season/episode/quality selection is implemented, but the last choice is not
  remembered between sessions. Watchlist and continue-watching are not implemented.
- The player's on-screen controls are the stock ExoPlayer controller; a purpose-built
  TV overlay (seek bar, subtitle/audio menus) is not built yet.
- The shipped APK is a **debug** build (signed with the Android debug key). Build a
  release variant with your own keystore for distribution.
