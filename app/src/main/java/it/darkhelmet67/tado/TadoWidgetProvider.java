package it.darkhelmet67.tado;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import it.darkhelmet67.tado.network.VolleyCallback;
import it.darkhelmet67.tado.network.VolleyFunctions;
import it.darkhelmet67.tado.utils.DLog;
import it.darkhelmet67.tado.utils.Prefs;

public class TadoWidgetProvider extends AppWidgetProvider {

    // WIDGET ACTIONS
    public static final String ACTION_AUTO_UPDATE = "AUTO_UPDATE";
    public static final String ACTION_REFRESH = "REFRESH";
    public static final String ACTION_NEXT = "NEXT";
    public static final String ACTION_PREV = "PREV";
    public static final String ACTION_CLICK = "CLICK";
    public static final String ACTION_OPEN_CONFIG = "OPEN_CONFIG";

    private static final String TAG = AppWidgetProvider.class.getSimpleName();
    private Context context = null;

    // ALARM - Every "n" seconds calls AUTO_UPDATE action!
    private AppWidgetAlarm appWidgetAlarm = null;

    // TADO JSON RESPONSE values
    private double tadoInsideTemp = 0, tadoSetPointTemp = 0;
    private String tadoOperation = "", tadoControlPhase = "";

    @Override
    public void onEnabled(Context context) {
        // LOG DEBUG
        DLog.setIsDebugEnabled(context.getResources().getBoolean(R.bool.isDebug));
        DLog.d(TAG, "onEnabled @TadoWidgetProvider");

        if (this.context == null)
            this.context = context;

        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        // LOG DEBUG
        DLog.setIsDebugEnabled(context.getResources().getBoolean(R.bool.isDebug));
        DLog.d(TAG, "onDisabled @TadoWidgetProvider");
//        requestQueue.cancelAll();

        // stop alarm
        if (appWidgetAlarm != null)
            appWidgetAlarm.stopAlarm();

        super.onDisabled(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // LOG DEBUG
        DLog.setIsDebugEnabled(context.getResources().getBoolean(R.bool.isDebug));
        DLog.d(TAG, "onDeleted @TadoWidgetProvider");
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // LOG DEBUG
        final String action = intent.getAction();
        DLog.setIsDebugEnabled(context.getResources().getBoolean(R.bool.isDebug));
        DLog.d(TAG, "onReceive @TadoWidgetProvider - action=" + action);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        switch(action) {

            case ACTION_AUTO_UPDATE:
                // WIDGET REFRESH
                int[] appWidgetIds = appWidgetManager
                        .getAppWidgetIds(new ComponentName(context, TadoWidgetProvider.class));
                DLog.i(TAG, "onReceive @TadoWidgetProvider - appWidgetIds=" + appWidgetIds.toString());
                onUpdate(context, appWidgetManager, appWidgetIds);
                break;

            case ACTION_NEXT:
                RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.tado_widget);
                // ANIMATION
//                remoteView.setInt(R.id.pageFlipper, "setInAnimation", R.animator.left_in);
//                remoteView.setInt(R.id.pageFlipper, "setOutAnimation", R.animator.right_out);
                remoteView.showNext(R.id.pageFlipper);
                appWidgetManager.partiallyUpdateAppWidget(intent.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID), remoteView);
                break;

            case ACTION_PREV:
                remoteView = new RemoteViews(context.getPackageName(), R.layout.tado_widget);
                // ANIMATION
//                remoteView.setInt(R.id.pageFlipper, "setInAnimation", R.animator.right_in);
//                remoteView.setInt(R.id.pageFlipper, "setOutAnimation", R.animator.left_out);
                remoteView.showPrevious(R.id.pageFlipper);
                appWidgetManager.partiallyUpdateAppWidget(intent.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID), remoteView);
                break;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onAppWidgetOptionsChanged (Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        // This is how you get your changes.
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        // CALC GRID SIZE
        int cols = (minWidth - 30) / 70;
        int rows = (minHeight - 30) / 70 + 1;
        int cols_2 = (minWidth - 2) / 74;
        int rows_2 = (minHeight - 2) / 74 + 1;

        DLog.i(TAG, "onAppWidgetOptionsChanged @TadoWidgetProvider - widgetSize=" + cols + "x" + rows
                + ", widgetSize_2=" + cols_2 + "x" + rows_2 + ", minWidth=" + minWidth + ", maxWidth=" + maxWidth
                + ", minHeight=" + minHeight + ", maxHeight=" + maxHeight);

        // TODO 2015-12-13 - TEXTVIEW SIZE
        /*
        if(Prefs.getSwitchTextSize(context)) {

            // GET WIDGET VIEW
            RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.tado_widget);
            remoteView.setFloat(R.id.textViewInsideTemp, "setTextSize", (rows_2 == 1 ? 32f : 48f));
            remoteView.setFloat(R.id.textViewSetPointTemp, "setTextSize", (rows_2 == 1 ? 12f : 18f));
            remoteView.setFloat(R.id.textViewLastUpdate, "setTextSize", (rows_2 == 1 ? 9f : 12f));
            appWidgetManager.updateAppWidget(appWidgetId, remoteView);
        }
        */
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        // LOG DEBUG
        DLog.setIsDebugEnabled(context.getResources().getBoolean(R.bool.isDebug));
        DLog.i(TAG, "onUpdate @TadoWidgetProvider - appWidgetIds=" + appWidgetIds.toString());

        if (this.context == null)
            this.context = context;

        // CHECK if ALARM MANAGER is RUNNING
        if (appWidgetAlarm == null) {
            DLog.d(TAG, "onUpdate @TadoWidgetProvider - NEW startAlarm()");
            appWidgetAlarm = new AppWidgetAlarm(context.getApplicationContext());
        } else {
            // STOP and UPDATE with NEW VALUE
            DLog.d(TAG, "onUpdate @TadoWidgetProvider - stopAlarm()");
            appWidgetAlarm.stopAlarm();
        }
        DLog.d(TAG, "onUpdate @TadoWidgetProvider - startAlarm()");
        appWidgetAlarm.startAlarm();

        // LC - GET USERNAME/PASSWORD for SELECTED DEVICE
        String deviceUsername = Prefs.getDeviceUsername(context);
        String devicePassword = Prefs.getDevicePassword(context);

        // LC - 1st run -> NO username/password!
        if (deviceUsername.length() == 0 && devicePassword.length() == 0) {
            DLog.w(TAG, "onUpdate @TadoWidgetProvider - no username/password!");
            // OPEN CONFIG ACTIVITY?
            /*
            for (int i = 0; i < appWidgetIds.length; i++) {
                int widgetId = appWidgetIds[i];
                PendingIntent pendingIntent = runConfigActivity(context, widgetId);
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e1) {
                    DLog.e(TAG, "onUpdate @TadoWidgetProvider - ERROR=" + e1.getMessage());
                    e1.printStackTrace();
                }
            }
            */
        } else {

            // LC - show PROGRESSBAR
            DLog.d(TAG, "onUpdate @TadoWidgetProvider - deviceUsername=" + deviceUsername);
            updateWidget(context, appWidgetManager, appWidgetIds, true);

            // LC - GET DATA from WS
            VolleyFunctions volleyFunctions = new VolleyFunctions(context);
            volleyFunctions.getCurrentState(deviceUsername, devicePassword, new VolleyCallback() {
                @Override
                public void onSuccess(String result) {
                    DLog.d(TAG, "onUpdate @TadoWidgetProvider - result=" + result);
                }

                @Override
                public void onResponse(JSONObject response) {
                    DLog.d(TAG, "onUpdate @TadoWidgetProvider - response=" + response.toString());

                    // STORE DEVICE USERNAME/PASSWORD
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            tadoInsideTemp = response.getDouble("insideTemp");
                            tadoSetPointTemp = response.getDouble("setPointTemp");
                            tadoOperation = response.getString("operation");
                            tadoControlPhase = response.getString("controlPhase");
                            DLog.d(TAG, "onUpdate @TadoWidgetProvider - insideTemp="
                                    + tadoInsideTemp + ", setPointTemp=" + tadoSetPointTemp
                                    + ", operation=" + tadoOperation + ", controlPhase=" + tadoControlPhase);
                            updateWidget(context, appWidgetManager, appWidgetIds, false);
                        }
                    } catch (JSONException e) {
                        String error = e.getMessage();
                        DLog.e(TAG, "onUpdate @TadoWidgetProvider - JSONException=" + error);
                        e.printStackTrace();
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(VolleyError e) {
                    // String data = error.getMessage();
                    String error = e.toString();
                    DLog.e(TAG, "onUpdate @TadoWidgetProvider - ERROR=" + error);
                    e.printStackTrace();
                    /*
                    // FIXME LC 2015-11-24 - GET HTTP STATUS!!!
                    if (error.contains("AuthFailureError")) {
                        // SHOW CONFIG ACTIVITY!
                        Toast.makeText(context, context.getString(R.string.error_authorization), Toast.LENGTH_LONG).show();
                        for (int i = 0; i < appWidgetIds.length; i++) {
                            int widgetId = appWidgetIds[i];
                            DLog.d(TAG, "onUpdate @TadoWidgetProvider - Show CONFIG ACTIVITY for widgetId=" + widgetId);
                            // OPEN CONFIG ACTIVITY
                            PendingIntent pendingIntent = runConfigActivity(context, widgetId);
                            try {
                                pendingIntent.send();
                            } catch (PendingIntent.CanceledException e1) {
                                DLog.e(TAG, "onUpdate @TadoWidgetProvider - ERROR=" + e1.getMessage());
                                e1.printStackTrace();
                            }
                        }
                    } else if (error.contains("TimeoutError")) {
                        Toast.makeText(context, "Network Timeout", Toast.LENGTH_LONG).show();
                        updateWidget(context, appWidgetManager, appWidgetIds, false);
                    } else
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                    */
                }
            });
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public static PendingIntent runConfigActivity(Context context, int appWidgetId) {
        DLog.i(TAG, "runConfig @TadoWidgetProvider - appWidgetId=" + appWidgetId);

        Intent intent = new Intent(context, ConfigActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, boolean isLoading) {
        DLog.d(TAG, "updateWidget @TadoWidgetProvider - operation="
                + tadoOperation + ", insideTemp=" + tadoInsideTemp + ", isLoading=" + isLoading);

        for (int i = 0; i < appWidgetIds.length; i++) {
            int widgetId = appWidgetIds[i];

            // GET WIDGET VIEW
            RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.tado_widget);

            // FIXME LC 2016-01-30 - WIDGET BG COLOR
            int color = 0;
            if (isLoading)
                color = R.color.tadoStandby;
            else {
                switch (tadoOperation.toUpperCase()) {
                    case "AWAY":
                        color = R.color.tadoAway;
                        break;
                    case "HOME":
                        color = R.color.tadoHome;
                        break;
                    case "SLEEP":
                        color = R.color.tadoSleep;
                        break;
                    case "MANUAL":
                        color = R.color.tadoManual;
                        break;
                    case "NO_FREEZE":
                        color = R.color.tadoStandby;
                        break;
                }
                remoteView.setInt(R.id.imageViewBackground, "setColorFilter",
                        context.getResources().getColor(color));
            }

            // FIXME LC 2016-01-30 - SHOW/HIDE PROGRESS BAR
            remoteView.setViewVisibility(R.id.pageFlipper, isLoading ? View.GONE : View.VISIBLE);
            remoteView.setViewVisibility(R.id.progressBarLoading, isLoading ? View.VISIBLE : View.GONE);

            // Specify the service to provide data for the collection widget.
            // Note that we need to embed the appWidgetId via the data otherwise it will be ignored.
            Intent intent = new Intent(context, TadoWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

            // FIXME LC 2016-01-29 - pass DATA from PROVIDER to SERVICE
            intent.putExtra("tadoInsideTemp", tadoInsideTemp);
            intent.putExtra("tadoSetPointTemp", tadoSetPointTemp);
            intent.putExtra("tadoOperation", tadoOperation);
            intent.putExtra("tadoControlPhase", tadoControlPhase);
            intent.putExtra("tadoIsLoading", isLoading);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            remoteView.setRemoteAdapter(R.id.pageFlipper, intent);

            // FIXME LC 2016-01-30 - LEFT/RIGHT buttons
            final Intent nextIntent = new Intent(context, TadoWidgetProvider.class);
            nextIntent.setAction(TadoWidgetProvider.ACTION_NEXT);
            nextIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            final PendingIntent nextPendingIntent = PendingIntent
                    .getBroadcast(context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteView.setOnClickPendingIntent(R.id.imageViewArrowRight, nextPendingIntent);

            final Intent prevIntent = new Intent(context, TadoWidgetProvider.class);
            prevIntent.setAction(TadoWidgetProvider.ACTION_PREV);
            prevIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            final PendingIntent prevPendingIntent = PendingIntent
                    .getBroadcast(context, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteView.setOnClickPendingIntent(R.id.imageViewArrowLeft, prevPendingIntent);

//            if(isLoading)
//                // CREATE
                appWidgetManager.updateAppWidget(widgetId, remoteView);
//            else
//                // UPDATE
//                appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.pageFlipper);

            /*
            if (isLoading) {

                // LC - show PROGRESSBAR
                remoteView.setViewVisibility(R.id.textViewInsideTemp, View.GONE);
                remoteView.setViewVisibility(R.id.progressBarInsideTemp, View.VISIBLE);
                // remoteView.setInt(R.id.progressBarInsideTemp, "setColorFilter", Color.WHITE);

            } else {

                // SET WIDGET ICON/COLOR
                int color = 0;
                int drawable = 0;
                switch (tadoOperation.toUpperCase()) {
                    case "AWAY":
                        color = R.color.tadoAway;
                        drawable = R.drawable.away_bar_white;
                        break;
                    case "HOME":
                        color = R.color.tadoHome;
                        drawable = R.drawable.house_bar_white;
                        break;
                    case "SLEEP":
                        color = R.color.tadoSleep;
                        drawable = R.drawable.userbed_bar_white;
                        break;
                    case "MANUAL":
                        color = R.color.tadoManual;
                        drawable = R.drawable.hand_bar_white;
                        break;
                    case "NO_FREEZE":
                        color = R.color.tadoStandby;
                        drawable = R.drawable.standby_bar_white;
                        break;
                }
                DLog.d(TAG, "updateWidget @TadoWidgetProvider - widgetId=" + widgetId + ", color=" + color + ", drawable=" + drawable);
                // remoteView.setInt(R.id.layoutWidget, "setBackgroundColor", context.getResources().getColor(color));
                remoteView.setInt(R.id.imageViewBackground, "setColorFilter", context.getResources().getColor(color));
                remoteView.setImageViewResource(R.id.imageViewIcon, drawable);

                // SET WIDGET TEMP (clickable)
                remoteView.setTextViewText(R.id.textViewInsideTemp, String.format("%.1f", tadoInsideTemp) + "°");
                remoteView.setViewVisibility(R.id.textViewInsideTemp, View.VISIBLE);
                remoteView.setViewVisibility(R.id.progressBarInsideTemp, View.GONE);

                // SET POINT TEMP
                remoteView.setTextViewText(R.id.textViewSetPointTemp, String.format("%.1f", tadoSetPointTemp) + "°");

                // SET CONTROL PHASE
                remoteView.setViewVisibility(R.id.imageViewPhase,
                        tadoControlPhase.equalsIgnoreCase("HEATUP") ? View.VISIBLE : View.INVISIBLE);
                // remoteView.setInt(R.id.imageViewPhase, "setVisibility", tadoControlPhase.equalsIgnoreCase("HEATUP") ? View.VISIBLE : View.INVISIBLE);

                // LAST UPDATE
                remoteView.setTextViewText(R.id.textViewLastUpdate, lastUpdate);

                // CLICK on CONTROL PHASE TEMP to REFRESH WIDGET
                intent = new Intent(context, TadoWidgetProvider.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                // WIDGET TEMP (clickable)
                // remoteView.setOnClickPendingIntent(R.id.textViewInsideTemp, pendingIntent);
                remoteView.setOnClickPendingIntent(R.id.layoutRefresh, pendingIntent);

                // CLICK on ICON to OPEN CONFIG ACTIVITY
                pendingIntent = runConfigActivity(context, widgetId);
                remoteView.setOnClickPendingIntent(R.id.imageViewIcon, pendingIntent);

                // CLICK on CURRENT TEMP to OPEN TADO APP
                intent = context.getPackageManager().getLaunchIntentForPackage("com.tado");
                if (intent != null) {
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                    intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    pendingIntent = PendingIntent.getActivity(context,
                            0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    remoteView.setOnClickPendingIntent(R.id.textViewInsideTemp, pendingIntent);
                }
            }
            appWidgetManager.updateAppWidget(widgetId, remoteView);
            */
        }
    }
}
