# CLAUDE.md

Context for AI assistants working on this repo. See README.md for the user-facing description.

## What this is
Unofficial Android widget for tado° thermostats, by DarkHelmet67. Written 2015–16, started from the "SimpleAndroidWidget" tutorial sample (hence `LICENSE` says "obaro"; README.md originally was that sample's). Dormant since 2016.

## Toolchain (very old, will not build in current Android Studio)
- Root `build.gradle`: Android Gradle plugin 1.5.0, `jcenter()` (shut down), Gradle wrapper 2.2.1.
- `app`: compileSdk/targetSdk 22, minSdk 16, buildTools 22.0.1, versionName 1.0.5 (code 6), deps use legacy `compile` (appcompat-v7/design 22.2.1, `:volley`).
- `volley/`: vendored Volley module (Google, Apache 2.0), included via `settings.gradle` (`:app`, `:volley`). Don't edit it; its test sources live in `volley/src/test`.
- Pure Java, no Kotlin. No tests for `app`. No CI.
- Modernising means: new wrapper + AGP, `google()`/`mavenCentral()`, `implementation`, AndroidX, replace Volley dep, targetSdk bump (needs `PendingIntent` mutability flags, exact-alarm/background limits, runtime behaviour of `AlarmManager.setRepeating` with RTC, cleartext/network config).

## Architecture (`app/src/main/java/it/darkhelmet67/tado/`)
- `TadoWidgetProvider` — AppWidgetProvider. Actions: AUTO_UPDATE, REFRESH, NEXT, PREV, CLICK, OPEN_CONFIG. `onUpdate` calls `getCurrentState`, stores `insideTemp`, `setPointTemp`, `operation`, `controlPhase`, then `updateWidget` builds RemoteViews (collection widget via `TadoWidgetService`). Handles resize (`onAppWidgetOptionsChanged`) and text auto-size pref.
- `TadoWidgetService` — RemoteViewsService/factory rendering page 1 / page 2 (`layout/tado_widget_page_{1,2}.xml`) and "last update" label; values are passed in via intent extras.
- `ConfigActivity` (~570 lines) — login + device registration UI (declared as the widget's `APPWIDGET_CONFIGURE` activity). Flow: login → `getDevices` (list existing app users) → on save either `claimAppUser` (existing nickname) or `createAppUser` (new); stores device-specific username/password. Also sets update interval and text-size switch.
- `AppWidgetAlarm` — `AlarmManager.setRepeating(RTC)` broadcasting `AUTO_UPDATE` every N minutes (N from prefs; 0 = manual).
- `network/` — `VolleyFunctions` (getDevices, claimAppUser, createAppUser, getCurrentState), `CustomRequest`, `JsonObjectRequestStatus`, `VolleyCallback`.
- `utils/` — `Prefs` (SharedPreferences wrapper; account creds + device creds + update interval + text-size switch), `DLog` (debug logging gated by `R.bool.isDebug`, currently `true`), `UI`.
- `AutoResizeTextView` — third-party-style auto-fitting TextView.
- Strings (incl. API URL templates with `%USERNAME`/`%PASSWORD` placeholders) in `res/values/strings.xml`; Italian in `values-it`.

## tado° legacy API (`https://my.tado.com`)
Endpoints under `/mobile/1.6/`: `getAppUsers`, `claimAppUser`, `createAppUser`, `getCurrentState`, all GET/POST with `username` & `password` as **query params**. `getCurrentState` returns JSON with `success`, `insideTemp`, `setPointTemp`, `operation`, `controlPhase`. Whether this API still exists is **unverified** — likely replaced by tado°'s newer OAuth API (`my.tado.com/api/v2`). Don't assume it works.

## Known issues / gotchas
- Credentials stored in plaintext SharedPreferences and sent in URLs. `AndroidManifest` has `android:debuggable="true"` (also for release in `build.gradle`) and `isDebug=true`.
- `appVersion=2.5.0` and `locale=it` are hardcoded in the URL templates.
- `AppWidgetAlarm` is created per provider instance; stop/start logic may leak alarms.
- Unused/commented code in `ConfigActivity` (device position prefs).

## Repo hygiene — IMPORTANT
- A local-only folder `misc files/` (git-ignored) holds the owner's **real tado° credentials, API logs and screenshots**. Never read it into commits, never un-ignore it, never quote its contents in docs/issues. The owner was advised to rotate that password.
- `.gitignore` also excludes `.idea/`, `*.iml`, `.gradle/`, `build/`, `local.properties`.
- Remote: https://github.com/DarkHelmet67/tado-widget (public, branch `main`). Commit messages end with the Co-Authored-By trailer configured for this session.

## Editor
VS Code extensions installed for browsing: Java pack (redhat.java etc.), Gradle for Java, XML, Android (adelphes). VS Code can't build this; use Android SDK + old Gradle/JDK 7–8.
