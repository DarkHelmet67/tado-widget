# tado° API notes

tado° does **not** publish a public API reference. What exists:

- An official support article describing login: <https://support.tado.com/en/articles/8565472-how-do-i-authenticate-to-access-the-rest-api>
- An official article about request limits: <https://support.tado.com/en/articles/12165739-limitation-for-rest-api-usage>
- Community-documented endpoints, for example <https://shkspr.mobi/blog/2019/02/tado-api-guide-updated-for-2019/> and the open-source clients `python-tado` and `node-tado-client`.

The endpoint shapes below are therefore reverse-engineered and may change without notice. Everything is implemented in `app/src/main/java/it/darkhelmet67/tado/api/TadoApi.java` and covered by `TadoApiTest` (which runs against a mock server, not the real service).

## History: why the old app broke

Version 1.x used the private mobile API `https://my.tado.com/mobile/1.6/...` with the username and password as URL query parameters. tado° removed password-based access in March 2025, so 1.x no longer works. Version 2 uses the OAuth device-code flow and the `v2` REST API.

## Login: OAuth2 device-code flow

| Step | Request | Notes |
|------|---------|-------|
| 1 | `POST https://login.tado.com/oauth2/device_authorize` (form: `client_id`, `scope=offline_access`) | Returns `device_code`, `user_code`, `verification_uri`, `verification_uri_complete`, `interval`, `expires_in` |
| 2 | User opens `verification_uri_complete` in a browser and approves | The app never sees the password |
| 3 | `POST https://login.tado.com/oauth2/token` (form: `client_id`, `grant_type=urn:ietf:params:oauth:grant-type:device_code`, `device_code`) every `interval` seconds | `400 authorization_pending` while waiting, `slow_down` means poll slower, `access_denied` / `expired_token` end the flow |
| 4 | Same endpoint with `grant_type=refresh_token` | Access tokens last ~10 minutes. Refresh tokens last up to 30 days and **rotate**: the old one is revoked on use |

- `client_id` is the public id tado° documents for hobby and open-source use: `1bb50063-6b0c-4d11-bd99-387f4a91cc46`. It is shared, so tado° can change or revoke it.
- Because refresh tokens rotate, refreshing must be serialised (`TadoApi.REFRESH_LOCK`) and the new token saved immediately, otherwise the user is signed out.
- A rejected refresh token (`invalid_grant`) means the user must sign in again.

## Data endpoints used

Base URL `https://my.tado.com/api/v2`, header `Authorization: Bearer <access token>`.

| Endpoint | Used for |
|----------|----------|
| `GET /me` | List of homes (`homes[].id`, `name`) |
| `GET /homes/{homeId}/zones` | Zones of a home (`id`, `name`, `type`) |
| `GET /homes/{homeId}/zones/{zoneId}/state` | Current values (see below) |

Fields read from the zone state:

- `sensorDataPoints.insideTemperature.celsius`: room temperature
- `setting.temperature.celsius`: target (null when the zone is off); `setting.power`: `ON` / `OFF`
- `overlayType`: non-null when a manual override is active
- `tadoMode`: `HOME` / `AWAY`
- `activityDataPoints.heatingPower.percentage`: above 0 means heating

The widget derives its mode from these: OFF, then MANUAL (override), then AWAY, then HOME. The 1.x "sleep" mode has no equivalent in this data.

## Rate limits

Free accounts get about **100 requests per day**; Auto-Assist subscribers about 20,000. HTTP 429 means the quota is used up. The widget therefore:

- uses exactly one request per refresh (home and zone ids are stored after setup),
- offers refresh intervals of 30 minutes or more, plus manual,
- guards against needless refreshes when widgets are added or the phone reboots,
- shows "Daily tado° limit reached" on a 429 and keeps the last values.

Token requests go to `login.tado.com` and are separate from the data quota as far as we know (not verified).
