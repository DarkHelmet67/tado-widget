package it.darkhelmet67.tado.widget;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import it.darkhelmet67.tado.api.TadoApi;
import it.darkhelmet67.tado.api.TadoException;
import it.darkhelmet67.tado.api.ZoneState;
import it.darkhelmet67.tado.auth.SecureTokenStore;
import it.darkhelmet67.tado.util.AppLog;
import it.darkhelmet67.tado.util.NetworkDiagnostics;

/** Fetches the configured zone's state (one API call) and redraws the widgets. */
public class RefreshWorker extends Worker {

    public RefreshWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        TadoException.Kind failure = refresh(getApplicationContext());
        // No network right now (e.g. the system blocks background access): let WorkManager retry later.
        if (failure == TadoException.Kind.NETWORK && getRunAttemptCount() < 4) return Result.retry();
        return Result.success();
    }

    /**
     * Blocking refresh, shared by the periodic worker and the tap-to-refresh path.
     *
     * @return the kind of failure, or null if the refresh worked
     */
    public static TadoException.Kind refresh(Context context) {
        Context app = context.getApplicationContext();
        AppPrefs prefs = new AppPrefs(app);
        SecureTokenStore tokens = new SecureTokenStore(app);
        WidgetState last = prefs.getState();
        AppLog.d("REFRESH", "start " + NetworkDiagnostics.describe(app));

        WidgetState next;
        TadoException.Kind failureKind = null;
        if (tokens.load() == null) {
            next = last.withStatus(WidgetState.Status.NEEDS_LOGIN);
        } else if (!prefs.hasZone()) {
            next = last.withStatus(WidgetState.Status.NOT_CONFIGURED);
        } else {
            TadoApi api = new TadoApi(tokens, AppLog.network());
            ZoneState zone = null;
            TadoException failure = null;
            // Mobile networks often need a moment (e.g. right after waking up): retry network errors once.
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    zone = api.zoneState(prefs.getHomeId(), prefs.getZoneId());
                    failure = null;
                    break;
                } catch (TadoException e) {
                    failure = e;
                    if (e.kind != TadoException.Kind.NETWORK) break;
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            if (zone != null) {
                next = WidgetState.fromZone(zone, System.currentTimeMillis());
            } else if (failure != null && failure.kind == TadoException.Kind.AUTH_EXPIRED) {
                next = last.withStatus(WidgetState.Status.NEEDS_LOGIN);
            } else if (failure != null && failure.kind == TadoException.Kind.RATE_LIMITED) {
                next = last.withStatus(WidgetState.Status.RATE_LIMITED);
            } else {
                next = last.withStatus(WidgetState.Status.ERROR);
            }
            if (failure != null) {
                failureKind = failure.kind;
                AppLog.d("REFRESH", "failed: " + failure.kind + " " + failure.getMessage());
            }
        }
        AppLog.d("REFRESH", "result " + next.status);
        prefs.setState(next);
        TadoWidgetProvider.updateAll(app);
        return failureKind;
    }
}
