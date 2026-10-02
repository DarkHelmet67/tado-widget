# tado° Widget

An unofficial Android home-screen widget that shows the state of your [tado°](https://www.tado.com) smart thermostat at a glance: current room temperature, target temperature, operation mode and control phase, plus the time of the last update.

> **Status: legacy / archived-quality.** The widget was written in 2015–2016 against tado°'s old `my.tado.com/mobile/1.6` web services and targets Android API 16–22. That API may no longer work, and the build tooling is very old (see [Building](#building)). Contributions to modernise it are welcome.

This project is not affiliated with or endorsed by tado° GmbH.

## Features

- Home-screen widget (resizable, two swipeable pages: *Next* / *Previous* buttons)
- Shows inside temperature, set-point temperature, operation and control phase
- Configurable refresh: manual, 1, 5, 15, 30 or 60 minutes
- Tap to refresh manually
- Optional automatic text resizing when the widget is resized
- English and Italian UI

## How it works

1. When you add the widget, a configuration screen asks for your tado° account email and password.
2. The app lists the "app users" (devices) already registered on your account, and registers a new one under a nickname you choose (`claimAppUser` / `createAppUser`).
3. From then on the widget polls `getCurrentState` using that device-specific credential and renders the result.

## Building

Requirements (the versions the project was written for):

- Android SDK platform 22 and build-tools 22.0.1
- Gradle 2.2.1 (wrapper included) with Android Gradle plugin 1.5.0
- A JDK compatible with that Gradle version (Java 7/8)

Create `local.properties` with your SDK path (it is git-ignored):

```
sdk.dir=/path/to/Android/sdk
```

Then:

```
./gradlew assembleDebug
```

Modern Android Studio will refuse to open the project as-is; you'll need to upgrade the Gradle wrapper, the Android Gradle plugin and replace the removed `jcenter()` repository and the `compile` configuration. Doing so is a good first contribution.

## Project layout

| Path | Purpose |
|------|---------|
| `app/` | The widget application (package `it.darkhelmet67.tado`) |
| `volley/` | Bundled copy of Google's [Volley](https://github.com/google/volley) HTTP library |

## Privacy and security

Your tado° credentials are entered on-device and sent only to `https://my.tado.com`. Be aware of known limitations in this old code: credentials are stored in plain `SharedPreferences`, and the legacy API takes them as URL query parameters. Don't reuse a password you care about elsewhere.

## Contributing

Issues and pull requests are welcome. Never commit real credentials, API logs or screenshots containing account details.

## License

MIT, see [LICENSE](LICENSE). The widget skeleton originated from the *SimpleAndroidWidget* tutorial sample; Volley is licensed under Apache 2.0.
