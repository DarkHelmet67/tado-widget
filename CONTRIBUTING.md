# Contributing

Thanks for helping! This is a small Java Android app (see [README](README.md) and [docs/tado-api.md](docs/tado-api.md)).

- **No local setup needed:** open a pull request and GitHub Actions builds, lints and tests it. Locally you need JDK 17 and the Android SDK (platform 35) for `./gradlew lintDebug testDebugUnitTest assembleDebug`.
- **Keep API calls cheap.** Free tado° accounts get about 100 requests per day; do not add polling or extra calls per refresh without a good reason.
- **Tests:** API and parsing code is plain Java and tested with JUnit and MockWebServer. Please add a test with a change.
- **Never commit secrets:** no passwords, tokens, keystores, or logs/screenshots showing account data.
- **Docs:** update the README, `docs/` and `CLAUDE.md` when behaviour or architecture changes.
- **Translations:** English is the default (`values/`), Italian is `values-it/`. Every translatable string must exist in both (`StringsParityTest` enforces it). To add a language create `values-xx/strings.xml`, list it in `res/xml/locales_config.xml` and in `resourceConfigurations` in `app/build.gradle`, and translate the user guide. The app follows the phone's language and falls back to English.
