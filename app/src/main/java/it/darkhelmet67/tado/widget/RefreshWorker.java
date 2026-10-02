package it.darkhelmet67.tado.widget;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import it.darkhelmet67.tado.api.TadoApi;
import it.darkhelmet67.tado.api.TadoException;
import it.darkhelmet67.tado.api.ZoneState;
import it.darkhelmet67.tado.auth.SecureTokenStore;

/** Fetches the configured zone's state (one API call) and redraws the widgets. */
public class RefreshWorker extends Worker {

    public RefreshWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppPrefs prefs = new AppPrefs(context);
        SecureTokenStore tokens = new SecureTokenStore(context);
        WidgetState last = prefs.getState();

        WidgetState next;
        if (tokens.load() == null) {
            next = last.withStatus(WidgetState.Status.NEEDS_LOGIN);
        } else if (!prefs.hasZone()) {
            next = last.withStatus(WidgetState.Status.NOT_CONFIGURED);
        } else {
            try {
                ZoneState zone = new TadoApi(tokens).zoneState(prefs.getHomeId(), prefs.getZoneId());
                next = WidgetState.fromZone(zone, System.currentTimeMillis());
            } catch (TadoException e) {
                switch (e.kind) {
                    case AUTH_EXPIRED:
                        next = last.withStatus(WidgetState.Status.NEEDS_LOGIN);
                        break;
                    case RATE_LIMITED:
                        next = last.withStatus(WidgetState.Status.RATE_LIMITED);
                        break;
                    default:
                        next = last.withStatus(WidgetState.Status.ERROR);
                        break;
                }
            }
        }
        prefs.setState(next);
        TadoWidgetProvider.updateAll(context);
        return Result.success();
    }
}
