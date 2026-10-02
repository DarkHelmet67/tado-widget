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
    private static final String KEY_PENDING_DEVICE = "pendingDevice";
    private static final String KEY_PENDING_USER = "pendingUser";
    private static final String KEY_PENDING_URL = "pendingUrl";
    private static final String KEY_PENDING_INTERVAL = "pendingInterval";
    private static final String KEY_PENDING_EXPIRES = "pendingExpires";

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

    /** A device-code login the user may still approve; survives the settings screen being recreated. */
    public void setPendingLogin(String deviceCode, String userCode, String url, int intervalSeconds, long expiresAtMillis) {
        prefs.edit().putString(KEY_PENDING_DEVICE, deviceCode).putString(KEY_PENDING_USER, userCode)
                .putString(KEY_PENDING_URL, url).putInt(KEY_PENDING_INTERVAL, intervalSeconds)
                .putLong(KEY_PENDING_EXPIRES, expiresAtMillis).apply();
    }

    /** @return the pending login, or null if there is none or it has expired */
    public String[] getPendingLogin() {
        String device = prefs.getString(KEY_PENDING_DEVICE, null);
        if (device == null || prefs.getLong(KEY_PENDING_EXPIRES, 0) < System.currentTimeMillis()) return null;
        return new String[]{device, prefs.getString(KEY_PENDING_USER, ""), prefs.getString(KEY_PENDING_URL, ""),
                String.valueOf(prefs.getInt(KEY_PENDING_INTERVAL, 5)),
                String.valueOf(prefs.getLong(KEY_PENDING_EXPIRES, 0))};
    }

    public void clearPendingLogin() {
        prefs.edit().remove(KEY_PENDING_DEVICE).remove(KEY_PENDING_USER).remove(KEY_PENDING_URL)
                .remove(KEY_PENDING_INTERVAL).remove(KEY_PENDING_EXPIRES).apply();
    }

    public void clearZoneAndState() {
        prefs.edit().remove(KEY_HOME_ID).remove(KEY_ZONE_ID).remove(KEY_ZONE_NAME).remove(KEY_STATE).apply();
    }
}
