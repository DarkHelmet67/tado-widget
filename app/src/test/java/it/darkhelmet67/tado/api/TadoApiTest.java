package it.darkhelmet67.tado.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.darkhelmet67.tado.auth.TokenStore;
import it.darkhelmet67.tado.auth.Tokens;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TadoApiTest {

    private static class MemoryStore implements TokenStore {
        Tokens tokens;

        @Override public Tokens load() { return tokens; }
        @Override public void save(Tokens t) { tokens = t; }
        @Override public void clear() { tokens = null; }
    }

    private MockWebServer server;
    private MemoryStore store;
    private TadoApi api;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String base = server.url("/").toString().replaceAll("/$", "");
        store = new MemoryStore();
        api = new TadoApi(new OkHttpClient(), base, base + "/api/v2", store);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code).setBody(body);
    }

    private static Tokens validTokens() {
        return new Tokens("access-1", "refresh-1", System.currentTimeMillis() + 600_000);
    }

    @Test
    public void deviceFlowPendingThenApproved() throws Exception {
        server.enqueue(json(200, "{\"device_code\":\"dc\",\"user_code\":\"ABCD\","
                + "\"verification_uri\":\"https://login.tado.com/device\","
                + "\"verification_uri_complete\":\"https://login.tado.com/device?user_code=ABCD\","
                + "\"expires_in\":300,\"interval\":5}"));
        server.enqueue(json(400, "{\"error\":\"authorization_pending\"}"));
        server.enqueue(json(200, "{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":600}"));

        DeviceAuth auth = api.startDeviceAuth();
        assertEquals("ABCD", auth.userCode);
        assertEquals(5, auth.intervalSeconds);

        try {
            api.pollToken(auth.deviceCode);
            fail("expected pending");
        } catch (TadoException e) {
            assertEquals(TadoException.Kind.PENDING, e.kind);
        }
        Tokens tokens = api.pollToken(auth.deviceCode);
        assertEquals("a", tokens.accessToken);
        assertEquals("r", store.load().refreshToken);

        RecordedRequest start = server.takeRequest();
        assertEquals("/oauth2/device_authorize", start.getPath());
        String body = start.getBody().readUtf8();
        assertTrue(body.contains("client_id=" + TadoApi.CLIENT_ID));
        assertTrue(body.contains("scope=offline_access"));
    }

    @Test
    public void expiredAccessTokenIsRefreshedAndRotated() throws Exception {
        store.save(new Tokens("old", "refresh-1", System.currentTimeMillis() - 1000));
        server.enqueue(json(200, "{\"access_token\":\"new\",\"refresh_token\":\"refresh-2\",\"expires_in\":600}"));
        server.enqueue(json(200, "{\"homes\":[{\"id\":42,\"name\":\"Home\"}]}"));

        List<Home> homes = api.homes();

        assertEquals(42L, homes.get(0).id);
        assertEquals("refresh-2", store.load().refreshToken);
        assertEquals("/oauth2/token", server.takeRequest().getPath());
        RecordedRequest me = server.takeRequest();
        assertEquals("/api/v2/me", me.getPath());
        assertEquals("Bearer new", me.getHeader("Authorization"));
    }

    @Test
    public void unauthorizedTriggersOneRefreshAndRetry() throws Exception {
        store.save(validTokens());
        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(json(200, "{\"access_token\":\"fresh\",\"refresh_token\":\"refresh-2\",\"expires_in\":600}"));
        server.enqueue(json(200, "[{\"id\":1,\"name\":\"Living room\",\"type\":\"HEATING\"}]"));

        List<Zone> zones = api.zones(42);

        assertEquals("Living room", zones.get(0).name);
        assertEquals("/api/v2/homes/42/zones", server.takeRequest().getPath());
        assertEquals("/oauth2/token", server.takeRequest().getPath());
        assertEquals("Bearer fresh", server.takeRequest().getHeader("Authorization"));
    }

    @Test
    public void rejectedRefreshTokenSignsOut() throws Exception {
        store.save(new Tokens("old", "dead", System.currentTimeMillis() - 1000));
        server.enqueue(json(400, "{\"error\":\"invalid_grant\"}"));

        try {
            api.homes();
            fail("expected auth error");
        } catch (TadoException e) {
            assertEquals(TadoException.Kind.AUTH_EXPIRED, e.kind);
        }
        assertNull(store.load());
    }

    @Test
    public void notSignedInIsAuthExpired() {
        try {
            api.homes();
            fail("expected auth error");
        } catch (TadoException e) {
            assertEquals(TadoException.Kind.AUTH_EXPIRED, e.kind);
        }
    }

    @Test
    public void http429IsRateLimited() throws Exception {
        store.save(validTokens());
        server.enqueue(new MockResponse().setResponseCode(429));
        try {
            api.zoneState(1, 2);
            fail("expected rate limit");
        } catch (TadoException e) {
            assertEquals(TadoException.Kind.RATE_LIMITED, e.kind);
        }
        assertNotNull(store.load());
    }

    @Test
    public void parsesZoneState() throws Exception {
        store.save(validTokens());
        server.enqueue(json(200, "{\"tadoMode\":\"HOME\",\"overlayType\":null,"
                + "\"setting\":{\"power\":\"ON\",\"temperature\":{\"celsius\":20.0}},"
                + "\"sensorDataPoints\":{\"insideTemperature\":{\"celsius\":18.4}}}"));
        ZoneState state = api.zoneState(42, 7);
        assertEquals(18.4, state.insideTemp, 0.001);
        assertEquals("/api/v2/homes/42/zones/7/state", server.takeRequest().getPath());
    }
}
