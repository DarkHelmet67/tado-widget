package it.darkhelmet67.tado.api;

import org.json.JSONObject;

/** The parts of {@code GET /homes/{id}/zones/{id}/state} the widget cares about. */
public final class ZoneState {
    public enum Mode { HOME, AWAY, MANUAL, OFF }

    /** Measured room temperature in °C, or null if the zone reports none. */
    public final Double insideTemp;
    /** Target temperature in °C, or null when the zone is off. */
    public final Double targetTemp;
    public final Mode mode;
    public final boolean heating;

    public ZoneState(Double insideTemp, Double targetTemp, Mode mode, boolean heating) {
        this.insideTemp = insideTemp;
        this.targetTemp = targetTemp;
        this.mode = mode;
        this.heating = heating;
    }

    public static ZoneState fromJson(JSONObject json) {
        Double inside = celsius(json.optJSONObject("sensorDataPoints"), "insideTemperature");
        JSONObject setting = json.optJSONObject("setting");
        Double target = setting == null ? null : celsiusOf(setting.optJSONObject("temperature"));
        boolean powerOff = setting != null && "OFF".equalsIgnoreCase(setting.optString("power"));
        boolean overlay = !json.isNull("overlayType") && json.has("overlayType");

        Mode mode;
        if (powerOff) mode = Mode.OFF;
        else if (overlay) mode = Mode.MANUAL;
        else if ("AWAY".equalsIgnoreCase(json.optString("tadoMode"))) mode = Mode.AWAY;
        else mode = Mode.HOME;

        JSONObject activity = json.optJSONObject("activityDataPoints");
        JSONObject heatingPower = activity == null ? null : activity.optJSONObject("heatingPower");
        boolean heating = heatingPower != null && heatingPower.optDouble("percentage", 0) > 0;

        return new ZoneState(inside, target, mode, heating);
    }

    private static Double celsius(JSONObject parent, String key) {
        return parent == null ? null : celsiusOf(parent.optJSONObject(key));
    }

    private static Double celsiusOf(JSONObject temperature) {
        if (temperature == null || !temperature.has("celsius") || temperature.isNull("celsius")) return null;
        return temperature.optDouble("celsius");
    }
}
