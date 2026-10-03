# Quick user guide

*tado° Widget* shows the temperature of one tado° zone on your Android home screen. It is unofficial and read-only: it never changes your heating. Requires Android 8.0 or newer and a tado° account. English and Italian are supported; the app follows your phone's language (Italian on Italian devices, English otherwise).

## 1. Add the widget

1. Install the app (see the [Releases](https://github.com/DarkHelmet67/tado-widget/releases) page, or Google Play once published).
2. Long-press an empty spot on your home screen, choose **Widgets**, find **tado° Widget** and drag it to the screen. It is 4 cells wide and can be resized.

## 2. Sign in

1. The settings screen opens. Tap **Sign in with tado°**.
2. Your browser opens tado°'s login page. The page asks for a **user code**: it is already filled in (the app shows the same code). Press **Submit**, sign in with your tado° account and approve.
3. Return to the app. It continues by itself; if not, tap **I have approved, continue**.

Your password is typed only on tado°'s website. The app keeps an encrypted token on your phone.

## 3. Choose a zone and refresh interval

- **Zone to show**: the room or zone of your tado° home.
- **Refresh interval**: manual, or every 30 minutes, 1, 2 or 4 hours. tado° allows free accounts only about 100 requests per day and every refresh uses one, so **every hour** is recommended.

Press **Save**. The widget appears and updates.

## 4. Using the widget

| What you see | Meaning |
|---|---|
| Large number | Current room temperature |
| Smaller number (right) | Target temperature (empty when the zone is off) |
| Flame icon | The zone is heating right now |
| Icon on the left | Mode: house = schedule, away, hand = manual override, standby = off |
| Small text at the bottom | Time of the last update, or a status such as "Updating…" |

- **Tap the widget** to refresh now.
- **Arrows** (left / right edge) switch to page 2: **humidity** and **heating power**.
- **Tap the mode icon** to open the settings (change zone or interval, sign out).

## 5. Troubleshooting

| Message | What to do |
|---|---|
| **Tap to sign in** | The login expired or was revoked. Tap the widget and sign in again. |
| **Update failed** | Usually Android blocks background network for the app. Open the app, press **Allow background access**, then set mobile data and battery use for the app to *unrestricted*. Some phones also have a manufacturer battery saver that must allow the app. |
| **Daily tado° limit reached** | tado° allows about 100 requests per day on free accounts. Use a longer refresh interval; the widget keeps the last values and recovers by itself. |
| Values look old | Tap the widget to refresh, then check the interval in the settings. |

If something still does not work, open the settings and press **Share log**. Use it **only to send a debug log to the widget author**; it contains request paths, status codes and timings, but never your password or tokens. You can attach it to an [issue](https://github.com/DarkHelmet67/tado-widget/issues).

## 6. Sign out and uninstall

Settings > **Sign out** removes the token and settings from your phone. Uninstalling removes everything. You can also revoke access from your tado° account.
