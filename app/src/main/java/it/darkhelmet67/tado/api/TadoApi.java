package it.darkhelmet67.tado.api;

import it.darkhelmet67.tado.auth.TokenStore;
import it.darkhelmet67.tado.auth.Tokens;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Minimal client for the tado° REST API (v2) and its OAuth2 device-code login.
 * See docs/tado-api.md. Methods block, so call them from a background thread.
 */
public class TadoApi {
    /** The public client id tado° documents for hobby / open-source use. */
    public static final String CLIENT_ID = "1bb50063-6b0c-4d11-bd99-387f4a91cc46";
    public static final String DEFAULT_AUTH_BASE = "https://login.tado.com";
    public static final String DEFAULT_API_BASE = "https://my.tado.com/api/v2";

    private static final String SCOPE = "offline_access";
    private static final String DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";
    private static final long REFRESH_MARGIN_MILLIS = 60_000;
    /** Refresh tokens rotate, so only one refresh may be in flight across the whole process. */
    private static final Object REFRESH_LOCK = new Object();

    private final OkHttpClient http;
    private final String authBase;
    private final String apiBase;
    private final TokenStore store;

    public TadoApi(TokenStore store) {
        this(new OkHttpClient(), DEFAULT_AUTH_BASE, DEFAULT_API_BASE, store);
    }

    public TadoApi(OkHttpClient http, String authBase, String apiBase, TokenStore store) {
        this.http = http;
        this.authBase = authBase;
        this.apiBase = apiBase;
        this.store = store;
    }

    // ---- Login (device-code flow) ----

    public DeviceAuth startDeviceAuth() throws TadoException {
        RequestBody form = new FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("scope", SCOPE)
                .build();
        String body = execute(new Request.Builder().url(authBase + "/oauth2/device_authorize").post(form).build());
        try {
            JSONObject j = new JSONObject(body);
            String uri = j.getString("verification_uri");
            return new DeviceAuth(j.getString("device_code"), j.getString("user_code"), uri,
                    j.optString("verification_uri_complete", uri),
                    j.optInt("interval", 5), j.optInt("expires_in", 300));
        } catch (JSONException e) {
            throw new TadoException(TadoException.Kind.API, "Unexpected device_authorize response", e);
        }
    }

    /**
     * Asks once whether the user approved the login.
     *
     * @throws TadoException {@link TadoException.Kind#PENDING} or {@link TadoException.Kind#SLOW_DOWN}
     *                       while waiting; DENIED / EXPIRED when the flow is over
     */
    public Tokens pollToken(String deviceCode) throws TadoException {
        RequestBody form = new FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("grant_type", DEVICE_GRANT)
                .add("device_code", deviceCode)
                .build();
        Tokens tokens = parseTokens(execute(tokenRequest(form)), null);
        store.save(tokens);
        if (store.load() == null) {
            // Login worked but the tokens cannot be kept: do not pretend the user is signed in.
            throw new TadoException(TadoException.Kind.API, "Could not store the login: " + store.lastError());
        }
        return tokens;
    }

    public void signOut() {
        store.clear();
    }

    public boolean isSignedIn() {
        return store.load() != null;
    }

    // ---- Data ----

    public List<Home> homes() throws TadoException {
        try {
            JSONArray homes = new JSONObject(authedGet(apiBase + "/me")).getJSONArray("homes");
            List<Home> result = new ArrayList<>();
            for (int i = 0; i < homes.length(); i++) {
                JSONObject h = homes.getJSONObject(i);
                result.add(new Home(h.getLong("id"), h.optString("name")));
            }
            return result;
        } catch (JSONException e) {
            throw new TadoException(TadoException.Kind.API, "Unexpected /me response", e);
        }
    }

    public List<Zone> zones(long homeId) throws TadoException {
        try {
            JSONArray zones = new JSONArray(authedGet(apiBase + "/homes/" + homeId + "/zones"));
            List<Zone> result = new ArrayList<>();
            for (int i = 0; i < zones.length(); i++) {
                JSONObject z = zones.getJSONObject(i);
                result.add(new Zone(z.getLong("id"), z.optString("name"), z.optString("type")));
            }
            return result;
        } catch (JSONException e) {
            throw new TadoException(TadoException.Kind.API, "Unexpected zones response", e);
        }
    }

    public ZoneState zoneState(long homeId, long zoneId) throws TadoException {
        try {
            return ZoneState.fromJson(new JSONObject(
                    authedGet(apiBase + "/homes/" + homeId + "/zones/" + zoneId + "/state")));
        } catch (JSONException e) {
            throw new TadoException(TadoException.Kind.API, "Unexpected zone state response", e);
        }
    }

    // ---- Plumbing ----

    private String authedGet(String url) throws TadoException {
        try {
            return execute(get(url, accessToken(false)));
        } catch (TadoException e) {
            if (e.kind != TadoException.Kind.AUTH_EXPIRED || store.load() == null) throw e;
            // The access token may have been revoked early: refresh once and retry.
            return execute(get(url, accessToken(true)));
        }
    }

    private static Request get(String url, String accessToken) {
        return new Request.Builder().url(url).header("Authorization", "Bearer " + accessToken).build();
    }

    private String accessToken(boolean forceRefresh) throws TadoException {
        synchronized (REFRESH_LOCK) {
            Tokens tokens = store.load();
            if (tokens == null) throw new TadoException(TadoException.Kind.AUTH_EXPIRED, "Not signed in");
            boolean stale = tokens.expiresAtMillis - System.currentTimeMillis() < REFRESH_MARGIN_MILLIS;
            if (forceRefresh || stale) {
                RequestBody form = new FormBody.Builder()
                        .add("client_id", CLIENT_ID)
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", tokens.refreshToken)
                        .build();
                try {
                    tokens = parseTokens(execute(tokenRequest(form)), tokens);
                } catch (TadoException e) {
                    if (e.kind == TadoException.Kind.AUTH_EXPIRED) store.clear();
                    throw e;
                }
                store.save(tokens);
            }
            return tokens.accessToken;
        }
    }

    private Request tokenRequest(RequestBody form) {
        return new Request.Builder().url(authBase + "/oauth2/token").post(form).build();
    }

    private static Tokens parseTokens(String body, Tokens previous) throws TadoException {
        try {
            JSONObject j = new JSONObject(body);
            String refresh = j.optString("refresh_token", previous == null ? "" : previous.refreshToken);
            long expiresAt = System.currentTimeMillis() + j.optLong("expires_in", 600) * 1000;
            return new Tokens(j.getString("access_token"), refresh, expiresAt);
        } catch (JSONException e) {
            throw new TadoException(TadoException.Kind.API, "Unexpected token response", e);
        }
    }

    private String execute(Request request) throws TadoException {
        try (Response response = http.newCall(request).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            if (response.isSuccessful()) return body;
            throw mapError(response.code(), body, response.request().url().encodedPath());
        } catch (IOException e) {
            throw new TadoException(TadoException.Kind.NETWORK, "Network error", e);
        }
    }

    private static TadoException mapError(int status, String body, String path) {
        String error = "";
        try {
            error = new JSONObject(body).optString("error");
        } catch (JSONException ignored) {
            // not an OAuth error body
        }
        switch (error) {
            case "authorization_pending": return new TadoException(TadoException.Kind.PENDING, error);
            case "slow_down": return new TadoException(TadoException.Kind.SLOW_DOWN, error);
            case "access_denied": return new TadoException(TadoException.Kind.DENIED, error);
            case "expired_token": return new TadoException(TadoException.Kind.EXPIRED, error);
            case "invalid_grant": return new TadoException(TadoException.Kind.AUTH_EXPIRED, error);
            default: break;
        }
        if (status == 429) return new TadoException(TadoException.Kind.RATE_LIMITED, "Rate limited");
        if (status == 401) return new TadoException(TadoException.Kind.AUTH_EXPIRED, "Unauthorized");
        String snippet = body.length() > 200 ? body.substring(0, 200) : body;
        return new TadoException(TadoException.Kind.API, "HTTP " + status + " " + path + " " + snippet);
    }
}
