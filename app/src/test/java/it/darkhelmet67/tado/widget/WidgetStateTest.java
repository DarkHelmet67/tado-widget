package it.darkhelmet67.tado.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import it.darkhelmet67.tado.api.ZoneState;

import org.junit.Test;

public class WidgetStateTest {

    @Test
    public void jsonRoundTrip() {
        WidgetState s = new WidgetState(WidgetState.Status.OK, 20.5, null,
                ZoneState.Mode.OFF, true, 48.0, 60.0, 1234L);
        WidgetState back = WidgetState.fromJson(s.toJson());
        assertEquals(WidgetState.Status.OK, back.status);
        assertEquals(20.5, back.insideTemp, 0.001);
        assertNull(back.targetTemp);
        assertEquals(ZoneState.Mode.OFF, back.mode);
        assertTrue(back.heating);
        assertEquals(48.0, back.humidity, 0.001);
        assertEquals(60.0, back.heatingPower, 0.001);
        assertEquals(1234L, back.updatedAtMillis);
    }

    @Test
    public void garbageFallsBackToInitial() {
        assertEquals(WidgetState.Status.NOT_CONFIGURED, WidgetState.fromJson("{nope").status);
        assertEquals(WidgetState.Status.NOT_CONFIGURED, WidgetState.fromJson(null).status);
    }

    @Test
    public void withStatusKeepsLastValues() {
        WidgetState s = WidgetState.fromZone(new ZoneState(19.0, 21.0, ZoneState.Mode.HOME, false, 50.0, null), 99L);
        WidgetState limited = s.withStatus(WidgetState.Status.RATE_LIMITED);
        assertEquals(WidgetState.Status.RATE_LIMITED, limited.status);
        assertEquals(19.0, limited.insideTemp, 0.001);
        assertEquals(99L, limited.updatedAtMillis);
        assertEquals(50.0, limited.humidity, 0.001);
    }
}
