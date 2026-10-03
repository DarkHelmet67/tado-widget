# Releasing and publishing to Google Play

Builds run in GitHub Actions, so no local Android SDK is needed.

## One-time setup

1. Create an upload keystore (keep it and its passwords safe, outside the repo):
   ```
   keytool -genkeypair -v -keystore release.jks -alias tado-widget -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Add these repository secrets (Settings > Secrets and variables > Actions):
   `TADO_KEYSTORE_BASE64` (`base64 -i release.jks`), `TADO_KEYSTORE_PASSWORD`, `TADO_KEY_ALIAS`, `TADO_KEY_PASSWORD`.
3. Prefer Play App Signing: Google holds the app signing key and this keystore is only the upload key.

## Each release

1. Bump `versionCode` and `versionName` in `app/build.gradle` (the code must increase every upload). Current: 2.2 (code 13).
2. Commit and push, then `git tag v2.0 && git push --tags`.
3. The `Release` workflow builds the APK (signed APK and AAB if the secrets above exist, otherwise a debug-signed APK named `...-debug.apk`), commits it to `versions/` on `main`, and attaches the files to a GitHub release (created with the `gh` CLI).
4. Upload the `.aab` in Play Console.

`versions/` also holds the archived 1.x APKs. Pull before your next push, because the workflow adds a commit to `main`.

## Play Console checklist

- Package name `it.darkhelmet67.tado` (the existing listing, if you still own it, needs a higher versionCode than the old 1.0.5 / code 6).
- Privacy policy URL: link to `PRIVACY.md` on GitHub.
- Permissions: INTERNET and ACCESS_NETWORK_STATE only. Do not add REQUEST_IGNORE_BATTERY_OPTIMIZATIONS (Play policy); the app links to system settings instead.
- Data safety form: the app collects no data; account credentials are handled by tado°'s login, tokens stay on device.
- Target API level: Play requires a recent level (currently 35 when this was written). Check the Play requirements before each release.
- Store listing: screenshots, short and full description. State clearly that the app is unofficial and requires a tado° account. Do not use tado°'s logo as the app icon.
- Reviewers need a tado° account to test: provide test credentials in the "App access" section, or a demo video.
- Tado's terms and its shared OAuth client id may change; consider asking tado° for confirmation that a third-party widget is acceptable.
