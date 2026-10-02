package it.darkhelmet67.tado.widget;

import android.content.Context;
import android.content.SharedPreferences;

/** Non-secret settings and the last widget state. Tokens live in SecureTokenStore instead. */
public class AppPrefs {
    public static final int DEFAULT_INTERVAL_MINUTES = 60;

    private static final String FILE = "tado_widget";
    private static final String KEY_HOME_ID = "homeId";
    private static final String KEY_ZONE_ID = "zoneId";
    private static final String KEY_ZONE_NAME = "zoneName";
    private static final String KEY_INTERVAL = "intervalMinutes";
    private static final String KEY_STATE = "state";

    private final SharedPreferences prefs;

    public AppPrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public boolean hasZone() {
        return prefs.contains(KEY_HOME_ID) && prefs.contains(KEY_ZONE_ID);
    }

    public long getHomeId() {
        return prefs.getLong(KEY_HOME_ID, 0);
    }

    public long getZoneId() {
        return prefs.getLong(KEY_ZONE_ID, 0);
    }

    public String getZoneName() {
        return prefs.getString(KEY_ZONE_NAME, "");
    }

    public void setZone(long homeId, long zoneId, String zoneName) {
        prefs.edit().putLong(KEY_HOME_ID, homeId).putLong(KEY_ZONE_ID, zoneId)
                .putString(KEY_ZONE_NAME, zoneName).apply();
    }

    /** Refresh interval in minutes; 0 means manual (tap to refresh). */
    public int getIntervalMinutes() {
        return prefs.getInt(KEY_INTERVAL, DEFAULT_INTERVAL_MINUTES);
    }

    public void setIntervalMinutes(int minutes) {
        prefs.edit().putInt(KEY_INTERVAL, minutes).apply();
    }

    public WidgetState getState() {
        return WidgetState.fromJson(prefs.getString(KEY_STATE, null));
    }

    public void setState(WidgetState state) {
        prefs.edit().putString(KEY_STATE, state.toJson()).apply();
    }

    public void clearZoneAndState() {
        prefs.edit().remove(KEY_HOME_ID).remove(KEY_ZONE_ID).remove(KEY_ZONE_NAME).remove(KEY_STATE).apply();
    }
}
