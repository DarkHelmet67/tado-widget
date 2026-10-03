package it.darkhelmet67.tado.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import it.darkhelmet67.tado.ConfigActivity;
import it.darkhelmet67.tado.R;
import it.darkhelmet67.tado.api.ZoneState;
import it.darkhelmet67.tado.util.AppLog;

public class TadoWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "it.darkhelmet67.tado.REFRESH";
    public static final String ACTION_PAGE = "it.darkhelmet67.tado.PAGE";
    private static final int PAGES = 2;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) render(context, manager, id);
        RefreshScheduler.refreshIfStale(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_REFRESH.equals(action)) {
            AppLog.d("WIDGET", "tap: refresh");
            // Refresh right away on a background thread instead of queueing WorkManager, so a tap
            // gives immediate feedback.
            final PendingResult pending = goAsync();
            final Context app = context.getApplicationContext();
            new Thread(() -> {
                try {
                    AppPrefs prefs = new AppPrefs(app);
                    prefs.setState(prefs.getState().withStatus(WidgetState.Status.REFRESHING));
                    updateAll(app);
                    RefreshWorker.refresh(app);
                } finally {
                    pending.finish();
                }
            }).start();
        } else if (ACTION_PAGE.equals(action)) {
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                AppPrefs prefs = new AppPrefs(context);
                prefs.setPage(id, (prefs.getPage(id) + 1) % PAGES);
                render(context, AppWidgetManager.getInstance(context), id);
            }
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        AppPrefs prefs = new AppPrefs(context);
        for (int id : appWidgetIds) prefs.removePage(id);
    }

    @Override
    public void onEnabled(Context context) {
        RefreshScheduler.schedulePeriodic(context);
    }

    @Override
    public void onDisabled(Context context) {
        RefreshScheduler.cancelPeriodic(context);
    }

    /** Redraws every placed widget from the stored state. */
    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, TadoWidgetProvider.class));
        for (int id : ids) render(context, manager, id);
    }

    private static void render(Context context, AppWidgetManager manager, int widgetId) {
        AppPrefs prefs = new AppPrefs(context);
        WidgetState state = prefs.getState();
        int page = prefs.getPage(widgetId);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.tado_widget);

        // Page 1: temperatures
        views.setTextViewText(R.id.textViewInsideTemp,
                state.insideTemp != null ? formatTemp(state.insideTemp) : "--");
        views.setTextViewText(R.id.textViewSetPointTemp,
                state.targetTemp != null ? formatTemp(state.targetTemp) : "");
        views.setViewVisibility(R.id.imageViewPhase, state.heating ? View.VISIBLE : View.INVISIBLE);
        views.setImageViewResource(R.id.imageViewIcon, iconFor(state.mode));

        // Page 2: humidity and heating power
        views.setTextViewText(R.id.textViewHumidity, formatPercent(state.humidity));
        views.setTextViewText(R.id.textViewHeatingPower, formatPercent(state.heatingPower));
        views.setImageViewResource(R.id.imageViewIcon2, iconFor(state.mode));

        views.setViewVisibility(R.id.pageMain, page == 0 ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.pageDetails, page == 1 ? View.VISIBLE : View.GONE);

        views.setInt(R.id.imageViewBackground, "setColorFilter",
                ContextCompat.getColor(context, colorFor(state)));
        views.setTextViewText(R.id.textViewLastUpdate, statusLine(context, state));

        // Icon opens settings, arrows flip the page, everything else refreshes (or opens settings
        // if the widget cannot refresh yet). The tap target is set on every view so it works
        // wherever the user touches the widget.
        PendingIntent config = configIntent(context, widgetId);
        views.setOnClickPendingIntent(R.id.imageViewIcon, config);
        views.setOnClickPendingIntent(R.id.imageViewIcon2, config);
        PendingIntent pageIntent = pageIntent(context, widgetId);
        views.setOnClickPendingIntent(R.id.imageViewArrowLeft, pageIntent);
        views.setOnClickPendingIntent(R.id.imageViewArrowRight, pageIntent);

        boolean needsSetup = state.status == WidgetState.Status.NOT_CONFIGURED
                || state.status == WidgetState.Status.NEEDS_LOGIN;
        PendingIntent tap = needsSetup ? config : refreshIntent(context);
        for (int id : new int[]{R.id.layoutWidget, R.id.imageViewBackground, R.id.pageMain, R.id.pageDetails,
                R.id.textViewInsideTemp, R.id.textViewSetPointTemp, R.id.imageViewPhase,
                R.id.textViewHumidity, R.id.textViewHeatingPower, R.id.textViewLastUpdate}) {
            views.setOnClickPendingIntent(id, tap);
        }

        manager.updateAppWidget(widgetId, views);
    }

    private static String formatTemp(double celsius) {
        return String.format(Locale.getDefault(), "%.1f°", celsius);
    }

    private static String formatPercent(Double value) {
        return value == null ? "--" : String.format(Locale.getDefault(), "%.0f%%", value);
    }

    private static int iconFor(ZoneState.Mode mode) {
        switch (mode) {
            case AWAY: return R.drawable.away_bar_white;
            case MANUAL: return R.drawable.hand_bar_white;
            case OFF: return R.drawable.standby_bar_white;
            default: return R.drawable.house_bar_white;
        }
    }

    private static int colorFor(WidgetState state) {
        if (state.status == WidgetState.Status.NOT_CONFIGURED
                || state.status == WidgetState.Status.NEEDS_LOGIN) return R.color.tadoStandby;
        switch (state.mode) {
            case AWAY: return R.color.tadoAway;
            case MANUAL: return R.color.tadoManual;
            case OFF: return R.color.tadoStandby;
            default: return R.color.tadoHome;
        }
    }

    private static String statusLine(Context context, WidgetState state) {
        switch (state.status) {
            case NOT_CONFIGURED: return context.getString(R.string.status_not_configured);
            case NEEDS_LOGIN: return context.getString(R.string.status_needs_login);
            case REFRESHING: return context.getString(R.string.status_refreshing);
            case RATE_LIMITED: return context.getString(R.string.status_rate_limited);
            case ERROR: return context.getString(R.string.status_error);
            default:
                return context.getString(R.string.label_last_update) + " "
                        + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(state.updatedAtMillis));
        }
    }

    private static PendingIntent refreshIntent(Context context) {
        Intent intent = new Intent(context, TadoWidgetProvider.class).setAction(ACTION_REFRESH);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent pageIntent(Context context, int widgetId) {
        Intent intent = new Intent(context, TadoWidgetProvider.class).setAction(ACTION_PAGE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getBroadcast(context, widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent configIntent(Context context, int widgetId) {
        Intent intent = new Intent(context, ConfigActivity.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getActivity(context, widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
