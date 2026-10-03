package it.darkhelmet67.tado.util;

import android.app.ActivityManager;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.PowerManager;

/**
 * Describes why the app might not get network access in the background (Data Saver, battery
 * restrictions, Doze). Android reports such blocking as an instant UnknownHostException.
 */
public final class NetworkDiagnostics {
    private NetworkDiagnostics() {
    }

    /** One-line summary for the log. */
    public static String describe(Context context) {
        try {
            StringBuilder sb = new StringBuilder();
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = cm.getActiveNetwork();
            NetworkCapabilities caps = network == null ? null : cm.getNetworkCapabilities(network);
            String type = "none";
            if (caps != null) {
                type = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? "wifi"
                        : caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? "cellular" : "other";
            }
            sb.append("network=").append(type);
            sb.append(" validated=").append(caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
            sb.append(" dataSaver=").append(dataSaver(cm));
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            sb.append(" batteryOptimized=").append(!pm.isIgnoringBatteryOptimizations(context.getPackageName()));
            sb.append(" powerSave=").append(pm.isPowerSaveMode());
            sb.append(" doze=").append(pm.isDeviceIdleMode());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                sb.append(" bgRestricted=").append(am.isBackgroundRestricted());
                UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                sb.append(" standbyBucket=").append(usm.getAppStandbyBucket());
            }
            return sb.toString();
        } catch (RuntimeException e) {
            return "diagnostics unavailable: " + e;
        }
    }

    /** True when Data Saver or "restricted" battery mode is likely to block background network access. */
    public static boolean backgroundLikelyRestricted(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED) return true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                return am.isBackgroundRestricted();
            }
        } catch (RuntimeException ignored) {
            // fall through
        }
        return false;
    }

    public static boolean dataSaverBlocksApp(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            return cm.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String dataSaver(ConnectivityManager cm) {
        switch (cm.getRestrictBackgroundStatus()) {
            case ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED: return "ON(app blocked)";
            case ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED: return "on(app allowed)";
            default: return "off";
        }
    }
}
