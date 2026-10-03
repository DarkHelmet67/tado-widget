# tado° Widget

An unofficial Android home-screen widget that shows the current temperature, target temperature and mode of one [tado°](https://www.tado.com) heating zone at a glance.

[![Build](https://github.com/DarkHelmet67/tado-widget/actions/workflows/build.yml/badge.svg)](https://github.com/DarkHelmet67/tado-widget/actions/workflows/build.yml)

> This project is not affiliated with or endorsed by tado° GmbH. It reads data only; it never changes your heating.

## Features

- Room temperature, target temperature, heating indicator and mode icon (home, away, manual override, off), tinted by mode
- Secure sign-in with tado°'s OAuth device-code login: you approve on tado°'s website and the app never sees your password
- Choose which home and zone to show
- Two pages (switch with the arrows): temperatures, then humidity and heating power
- Refresh manually (tap the widget) or every 30 minutes to 4 hours
- English and Italian

## Install

Download the latest APK from the [Releases](https://github.com/DarkHelmet67/tado-widget/releases) page (every build, including the old 1.x ones, is also kept in [`versions/`](versions)), or build it yourself (below). Requires Android 8.0 (API 26) or newer and a tado° account.

Add the widget from your launcher's widget picker, sign in, pick a zone, and save. Tapping the mode icon reopens the settings; tapping anywhere else refreshes.

## About tado°'s request limit

Since 2025 tado° limits free accounts to about **100 API requests per day** (about 20,000 with an Auto-Assist subscription). Every refresh uses one request, so the default is hourly. When the limit is hit the widget keeps the last values and shows "Daily tado° limit reached". Details in [docs/tado-api.md](docs/tado-api.md).

## Building

The project builds in GitHub Actions on every push (see `.github/workflows/build.yml`), and the debug APK is attached to each run. To build locally you need JDK 17 and the Android SDK (platform 35); put its path in `local.properties` (`sdk.dir=...`, git-ignored) and run:

```
./gradlew assembleDebug
./gradlew lintDebug testDebugUnitTest
```

## Troubleshooting and logs

The app writes a small rotating log (about 256 KB, 3 files) of network requests (method, path, status, duration, errors) and app events. It never contains passwords, tokens or response bodies. Open the app and press **Share log** to send it, or find it in `Android/data/it.darkhelmet67.tado/files/logs/` (newer Android versions hide `Android/data` from file managers; use Share log or a USB connection). Please attach it when reporting a problem.

### "Update failed" in the background

Some phones block network access for apps that are not on screen (Data Saver, "restricted" battery mode, vendor battery savers). The widget then fails with an instant "Unable to resolve host" error while the app works fine when opened. Open the app and press **Allow background access**, then set mobile data and battery use for the app to *unrestricted*. The log's `REFRESH start` lines show the relevant system state (`dataSaver`, `batteryOptimized`, `bgRestricted`, `standbyBucket`).

## How it works

| Part | File |
|------|------|
| tado° REST + OAuth client | `app/src/main/java/it/darkhelmet67/tado/api/TadoApi.java` |
| Encrypted token storage | `auth/SecureTokenStore.java` |
| Widget rendering and taps | `widget/TadoWidgetProvider.java` |
| Background refresh (WorkManager) | `widget/RefreshWorker.java`, `RefreshScheduler.java` |
| Sign-in and settings screen | `ConfigActivity.java` |

More documentation: [tado° API notes](docs/tado-api.md), [releasing and Google Play](docs/releasing.md), [privacy policy](PRIVACY.md), [contributing](CONTRIBUTING.md).

## Version history

- **2.1.1**: refreshes that fail because Android blocks background network access are retried by WorkManager; the log records the restriction (Data Saver, battery, standby bucket); settings show a warning and a shortcut to allow background access; zones are loaded once after sign-in.
- **2.1.0**: tap refreshes immediately with an "Updating…" state; second page (arrows) with humidity and heating power; network log with a Share button; network errors retried once.
- **2.0.3**: a temporary network error while returning from the browser no longer cancels the sign-in; loading zones retries.
- **2.0.2**: sign-in can be continued with a button or by returning to the app, and shows the real error if the login cannot be completed or stored.
- **2.0.1**: sign-in survives the settings screen being recreated, clearer errors and user-code instructions.
- **2.0**: rewritten for current Android and tado°'s OAuth/v2 API (the 1.x private mobile API and password login no longer work). One widget layout, one zone, no collection pages.
- **1.x** (2015–2016): original app using tado°'s private mobile API.

## Known limitations

- All widgets show the same zone and settings (per-widget zones are a possible future change).
- "Sleep" mode from 1.x has no equivalent in the current API data.
- The app relies on tado°'s shared public OAuth client id and undocumented v2 endpoints, which tado° may change.

## License

MIT, see [LICENSE](LICENSE). The widget originated from the *SimpleAndroidWidget* tutorial sample.
