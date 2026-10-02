package it.darkhelmet67.tado.widget;

import it.darkhelmet67.tado.api.ZoneState;

import org.json.JSONException;
import org.json.JSONObject;

/** What the widget shows: the last known zone values plus the outcome of the last refresh. */
public final class WidgetState {
    public enum Status { NOT_CONFIGURED, NEEDS_LOGIN, OK, RATE_LIMITED, ERROR }

    public final Status status;
    public final Double insideTemp;
    public final Double targetTemp;
    public final ZoneState.Mode mode;
    public final boolean heating;
    public final long updatedAtMillis;

    public WidgetState(Status status, Double insideTemp, Double targetTemp,
                       ZoneState.Mode mode, boolean heating, long updatedAtMillis) {
        this.status = status;
        this.insideTemp = insideTemp;
        this.targetTemp = targetTemp;
        this.mode = mode;
        this.heating = heating;
        this.updatedAtMillis = updatedAtMillis;
    }

    public static WidgetState initial() {
        return new WidgetState(Status.NOT_CONFIGURED, null, null, ZoneState.Mode.HOME, false, 0);
    }

    public static WidgetState fromZone(ZoneState zone, long nowMillis) {
        return new WidgetState(Status.OK, zone.insideTemp, zone.targetTemp, zone.mode, zone.heating, nowMillis);
    }

    /** Keeps the last known values but records why the latest refresh did not succeed. */
    public WidgetState withStatus(Status newStatus) {
        return new WidgetState(newStatus, insideTemp, targetTemp, mode, heating, updatedAtMillis);
    }

    public String toJson() {
        try {
            JSONObject j = new JSONObject();
            j.put("status", status.name());
            j.put("inside", insideTemp == null ? JSONObject.NULL : insideTemp);
            j.put("target", targetTemp == null ? JSONObject.NULL : targetTemp);
            j.put("mode", mode.name());
            j.put("heating", heating);
            j.put("updatedAt", updatedAtMillis);
            return j.toString();
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    public static WidgetState fromJson(String json) {
        if (json == null) return initial();
        try {
            JSONObject j = new JSONObject(json);
            return new WidgetState(
                    Status.valueOf(j.getString("status")),
                    j.isNull("inside") ? null : j.getDouble("inside"),
                    j.isNull("target") ? null : j.getDouble("target"),
                    ZoneState.Mode.valueOf(j.getString("mode")),
                    j.optBoolean("heating"),
                    j.optLong("updatedAt"));
        } catch (JSONException | IllegalArgumentException e) {
            return initial();
        }
    }
}
