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

public class TadoWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "it.darkhelmet67.tado.REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) render(context, manager, id);
        RefreshScheduler.refreshIfStale(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) RefreshScheduler.refreshNow(context);
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
        WidgetState state = new AppPrefs(context).getState();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.tado_widget);

        boolean hasData = state.insideTemp != null;
        views.setTextViewText(R.id.textViewInsideTemp, hasData ? formatTemp(state.insideTemp) : "--");
        views.setTextViewText(R.id.textViewSetPointTemp,
                state.targetTemp != null ? formatTemp(state.targetTemp) : "");
        views.setViewVisibility(R.id.imageViewPhase, state.heating ? View.VISIBLE : View.INVISIBLE);
        views.setImageViewResource(R.id.imageViewIcon, iconFor(state.mode));
        views.setInt(R.id.imageViewBackground, "setColorFilter",
                ContextCompat.getColor(context, colorFor(state)));
        views.setTextViewText(R.id.textViewLastUpdate, statusLine(context, state));

        // Tapping the icon opens settings; tapping anywhere else refreshes (or opens settings if
        // the widget cannot refresh yet).
        PendingIntent config = configIntent(context, widgetId);
        views.setOnClickPendingIntent(R.id.imageViewIcon, config);
        boolean needsSetup = state.status == WidgetState.Status.NOT_CONFIGURED
                || state.status == WidgetState.Status.NEEDS_LOGIN;
        views.setOnClickPendingIntent(R.id.layoutWidget, needsSetup ? config : refreshIntent(context));

        manager.updateAppWidget(widgetId, views);
    }

    private static String formatTemp(double celsius) {
        return String.format(Locale.getDefault(), "%.1f°", celsius);
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

    private static PendingIntent configIntent(Context context, int widgetId) {
        Intent intent = new Intent(context, ConfigActivity.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getActivity(context, widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
