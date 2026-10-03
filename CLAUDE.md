# CLAUDE.md

Context for AI assistants working on this repo. User-facing docs: README.md, docs/tado-api.md, docs/releasing.md, PRIVACY.md, CONTRIBUTING.md. **Update the docs and this file whenever behaviour or architecture changes** (the owner wants this to be a well-documented open-source project).

## What this is
Unofficial Android widget for tado° thermostats by DarkHelmet67 (https://github.com/DarkHelmet67/tado-widget, public, branch `main`). Original 1.x (2015-16) used tado°'s private `/mobile/1.6` API with the password in URLs; that stopped working in March 2025. **2.0 is a rewrite** on the OAuth device-code flow + `v2` REST API, aimed at re-publishing on Google Play. The 1.x code is in git history (first commits).

## Environment constraints
- The owner has **no local Android SDK or JDK**. Verify everything through GitHub Actions: push, then `gh run list` / `gh run watch <id> --exit-status` / `gh run view <id> --log-failed`. A first-try green build is not guaranteed for new code; iterate on CI logs.
- Auto-mode may block bulk deletions (`git rm` of many files); ask the user to run them.
- Commit messages end with the Co-Authored-By trailer from the session attribution reminder.

## Toolchain
Gradle 8.10.2 (wrapper), AGP 8.7.3, Groovy DSL, compile/target SDK 35, min SDK 26, Java 17, pure Java (no Kotlin). Deps: appcompat, Material 3, WorkManager, OkHttp 4.12, security-crypto 1.1.0-alpha06 (EncryptedSharedPreferences; the library is deprecated upstream but still the simplest option). Tests: JUnit4, org.json (needed so `org.json` works in JVM tests), OkHttp MockWebServer.

## Architecture (`app/src/main/java/it/darkhelmet67/tado/`)
- `api/TadoApi` — blocking OkHttp client: device-code login (`startDeviceAuth`, `pollToken`), `homes()`, `zones()`, `zoneState()`. Handles token refresh transparently: refresh if <60 s left, retry once on 401, static `REFRESH_LOCK` because **refresh tokens rotate** (reuse = signed out). `invalid_grant` clears the store. Errors map to `TadoException.Kind` (AUTH_EXPIRED, RATE_LIMITED on 429, PENDING/SLOW_DOWN/DENIED/EXPIRED for the login poll, NETWORK, API). Pure Java, no Android classes, so it is unit-testable.
- `api/ZoneState` — parses zone state JSON; Mode derivation order: OFF, MANUAL (overlay), AWAY, HOME.
- `auth/` — `TokenStore` interface, `SecureTokenStore` (encrypted prefs), `Tokens`.
- `widget/AppPrefs` — plain prefs: homeId, zoneId, zone name, interval minutes (default 60, 0 = manual), last `WidgetState` as JSON.
- `widget/WidgetState` — status (NOT_CONFIGURED, NEEDS_LOGIN, OK, RATE_LIMITED, ERROR) + last values; failed refreshes keep old values via `withStatus`.
- `widget/RefreshWorker` — one `zoneState` call, saves state, `TadoWidgetProvider.updateAll`. `RefreshScheduler` — periodic (min 15 min, UI offers 30+), one-time (`KEEP`), `refreshIfStale` (5 min guard so adding/rebooting does not burn quota).
- `widget/TadoWidgetProvider` — RemoteViews rendering from stored state; icon tap → `ConfigActivity`, elsewhere → `ACTION_REFRESH` (or config if setup/login needed). All PendingIntents are `FLAG_IMMUTABLE`.
- `ConfigActivity` — both the widget configure activity and the LAUNCHER activity. Groups: sign-in → pending (device code, polls on a single-thread executor) → setup (zone + interval spinners). Sets `RESULT_CANCELED` up front so backing out does not place the widget.
- Resources: single `layout/tado_widget.xml`; strings in `values/` and `values-it/` (keep both in sync); `dimens`/`colors` partly legacy.

## tado° API facts (details in docs/tado-api.md)
No official API reference exists. Auth: `https://login.tado.com/oauth2/{device_authorize,token}`, public client id `1bb50063-6b0c-4d11-bd99-387f4a91cc46`, scope `offline_access`; access token ~10 min, refresh ≤30 days rotating. Data: `https://my.tado.com/api/v2/{me, homes/{id}/zones, homes/{id}/zones/{id}/state}`. **Free accounts ≈100 requests/day** (429 when exceeded), subscribers ≈20,000 — keep one call per refresh.

## Logging
`util/AppLog` (init in `TadoApp`) writes through `util/RotatingFileLog` (pure Java, tested) to `getExternalFilesDir/logs/tado-widget.log` (256 KB x 3 files). `api/LoggingInterceptor` logs `METHOD host/path -> status ms` (never the query string, headers or success bodies; error bodies truncated to 200 chars). The settings screen has **Share log** (FileProvider `${applicationId}.logs`, combines files into `tado-widget-log-export.txt`). Never log tokens, device codes or credentials.

## Widget behaviour notes
Tap -> `ACTION_REFRESH` handled with `goAsync()` + a thread (not WorkManager) so it is immediate; state goes `REFRESHING` then OK/ERROR. The click PendingIntent is set on every widget view. Two pages per widget (`AppPrefs.getPage(widgetId)`, `ACTION_PAGE`): temperatures / humidity + heating power. Network errors are retried once in `RefreshWorker.refresh`.

## Debugging the login on a device (no adb available to the owner)
`ConfigActivity.fail()` prints `<message> (<kind>: <detail>)` in the status line; API errors include HTTP status, path and a body snippet; `SecureTokenStore.lastError()` explains Keystore failures (a login that cannot be stored raises an API error instead of silently looking signed-out). Found on a real phone: a transient NETWORK error right after returning from the browser used to cancel the login; NETWORK is now treated as retryable in the poll loop, the one-off check and `loadZones`. Tado's live endpoints were checked with curl: `device_authorize` returns user code + `verification_uri_complete`, and polling `token` returns 400 `authorization_pending` as implemented.

## Status / TODO
- Done: build upgrade, API client, login, widget, settings, tests, CI (`build.yml`), release workflow (`release.yml`, needs signing secrets), docs, privacy policy.
- Not yet verified on a real device or against the live tado° API (CI only runs unit tests with a mock server). First real-world test should confirm the device flow, the `/me` and zone-state fields, and widget rendering.
- APKs live in `versions/` (old 1.x ones moved there; `release.yml` on tag `v*` builds, commits the new APK to `versions/` on `main` and creates a GitHub release with `gh`). Version is `2.1.0` / code 11.
- Open items: unused legacy drawables (`progressbar.xml`, `selector_btn_green.xml`, `shape_rounded_corners_alpha.xml`, `devices.png`, `settings_device.png`, arrows) and `app/app.iml`; launcher icon exists only in `mipmap-xxhdpi` (no adaptive icon); Play listing assets; LICENSE copyright still credits the tutorial author (“obaro”); possible per-widget zones, humidity, dark theme polish.

## Repo hygiene — IMPORTANT
- The owner's real tado° credentials/logs/screenshots are in a local, git-ignored folder (`.private files/`, formerly `misc files/`). Never un-ignore it, read it into commits/docs/issues, or quote it. The owner was advised to rotate that password.
- Never commit keystores, tokens or `local.properties`. `.gitignore` also excludes `.idea/`, `*.iml`, `.gradle/`, `build/`.
