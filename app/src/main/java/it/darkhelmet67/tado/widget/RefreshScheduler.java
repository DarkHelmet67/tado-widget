package it.darkhelmet67.tado.widget;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/** Schedules {@link RefreshWorker}. Every refresh costs one call of tado°'s small daily quota. */
public final class RefreshScheduler {
    private static final String PERIODIC = "tado-refresh-periodic";
    private static final String NOW = "tado-refresh-now";
    private static final long STALE_AFTER_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private RefreshScheduler() {
    }

    private static Constraints online() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    /** (Re)applies the interval from settings; 0 cancels periodic refreshing. */
    public static void schedulePeriodic(Context context) {
        WorkManager wm = WorkManager.getInstance(context);
        int minutes = new AppPrefs(context).getIntervalMinutes();
        if (minutes <= 0) {
            wm.cancelUniqueWork(PERIODIC);
            return;
        }
        // WorkManager does not allow periods below 15 minutes.
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                RefreshWorker.class, Math.max(minutes, 15), TimeUnit.MINUTES)
                .setConstraints(online())
                .build();
        wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void cancelPeriodic(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC);
    }

    public static void refreshNow(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RefreshWorker.class)
                .setConstraints(online())
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, request);
    }

    /** Used on widget add / reboot so we do not burn quota refreshing data that is already fresh. */
    public static void refreshIfStale(Context context) {
        WidgetState state = new AppPrefs(context).getState();
        boolean fresh = state.status == WidgetState.Status.OK
                && System.currentTimeMillis() - state.updatedAtMillis < STALE_AFTER_MILLIS;
        if (!fresh) refreshNow(context);
    }
}
