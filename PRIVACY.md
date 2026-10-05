# Privacy policy

*tado° Widget* is an unofficial, open-source app. It is not affiliated with tado° GmbH.

**What the app does with your data**

- You sign in on tado°'s own website (OAuth device-code login). The app never receives or stores your password.
- The app stores an access token and a refresh token on your device, encrypted with a key held in the Android Keystore, plus the home/zone you chose and the last temperature values it fetched.
- The app talks only to `login.tado.com` and `my.tado.com` to read the temperature of the zone you selected. It does not control your heating.

**Debug log**

The app keeps a small rotating log on your device (request paths, status codes, timings, app events; never passwords, tokens or response contents). It leaves the device only if you press "Share log" and choose where to send it, for example by attaching it to a public issue on GitHub when reporting a problem. A shared log contains tado° home and zone numbers and request times, so check it before posting.

**What the app does not do**

- It has no servers, analytics, advertising or crash reporting, and it sends nothing to the developer or any third party.
- It does not collect location, contacts or device identifiers.

**Your control**

"Sign out" in the app deletes the tokens and settings from the device. Uninstalling the app removes everything. You can also revoke access from your tado° account.

**Contact**

Open an issue at <https://github.com/DarkHelmet67/tado-widget/issues>.
