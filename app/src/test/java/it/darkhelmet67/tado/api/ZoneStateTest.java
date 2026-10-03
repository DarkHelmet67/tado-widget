package it.darkhelmet67.tado.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class ZoneStateTest {

    private static final String SCHEDULE_HEATING = "{"
            + "\"tadoMode\":\"HOME\",\"overlayType\":null,"
            + "\"setting\":{\"type\":\"HEATING\",\"power\":\"ON\",\"temperature\":{\"celsius\":21.5}},"
            + "\"sensorDataPoints\":{\"insideTemperature\":{\"celsius\":19.8},\"humidity\":{\"percentage\":45}},"
            + "\"activityDataPoints\":{\"heatingPower\":{\"percentage\":60}}}";

    @Test
    public void parsesScheduleWithHeating() throws Exception {
        ZoneState s = ZoneState.fromJson(new JSONObject(SCHEDULE_HEATING));
        assertEquals(19.8, s.insideTemp, 0.001);
        assertEquals(21.5, s.targetTemp, 0.001);
        assertEquals(ZoneState.Mode.HOME, s.mode);
        assertTrue(s.heating);
        assertEquals(45.0, s.humidity, 0.001);
        assertEquals(60.0, s.heatingPower, 0.001);
    }

    @Test
    public void overlayMeansManual() throws Exception {
        JSONObject json = new JSONObject(SCHEDULE_HEATING).put("overlayType", "MANUAL");
        assertEquals(ZoneState.Mode.MANUAL, ZoneState.fromJson(json).mode);
    }

    @Test
    public void awayModeWithoutOverlay() throws Exception {
        JSONObject json = new JSONObject(SCHEDULE_HEATING).put("tadoMode", "AWAY");
        assertEquals(ZoneState.Mode.AWAY, ZoneState.fromJson(json).mode);
    }

    @Test
    public void powerOffHasNoTargetAndIsOff() throws Exception {
        JSONObject json = new JSONObject("{\"tadoMode\":\"HOME\",\"overlayType\":\"MANUAL\","
                + "\"setting\":{\"type\":\"HEATING\",\"power\":\"OFF\",\"temperature\":null},"
                + "\"sensorDataPoints\":{\"insideTemperature\":{\"celsius\":17.0}},"
                + "\"activityDataPoints\":{\"heatingPower\":{\"percentage\":0}}}");
        ZoneState s = ZoneState.fromJson(json);
        assertEquals(ZoneState.Mode.OFF, s.mode);
        assertNull(s.targetTemp);
        assertFalse(s.heating);
    }

    @Test
    public void toleratesMissingSections() throws Exception {
        ZoneState s = ZoneState.fromJson(new JSONObject("{}"));
        assertNull(s.insideTemp);
        assertNull(s.targetTemp);
        assertEquals(ZoneState.Mode.HOME, s.mode);
        assertFalse(s.heating);
        assertNull(s.humidity);
        assertNull(s.heatingPower);
    }
}
