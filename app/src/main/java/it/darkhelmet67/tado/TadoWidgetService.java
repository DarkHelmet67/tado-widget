package it.darkhelmet67.tado;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.Date;

import it.darkhelmet67.tado.utils.DLog;

public class TadoWidgetService extends RemoteViewsService {

    private int[] layoutIds = {
            R.layout.tado_widget_page_1,
            R.layout.tado_widget_page_2
    };
    private final String TAG = TadoWidgetService.class.getSimpleName();

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        DLog.d(TAG, "onGetViewFactory() @TadoWidgetService - intent=" + intent.toString());
        return new ViewFactory(this.getApplicationContext(), intent);
    }

    private class ViewFactory implements RemoteViewsService.RemoteViewsFactory {

        // THIS WIDGET ID
        private int widgetId;
        private Context context;

        // TADO JSON RESPONSE values
        private double tadoInsideTemp = 0, tadoSetPointTemp = 0;
        private String tadoOperation = "", tadoControlPhase = "";
        private boolean tadoIsLoading = false;
        private int tadoIconId = 0;

        public ViewFactory(Context context, Intent intent) {
            DLog.d(TAG, "ViewFactory() @TadoWidgetService - extras="
                    + intent.getExtras().toString() + ", context=" + context.toString());
            this.context = context;
            widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            DLog.d(TAG, "ViewFactory() @TadoWidgetService - widgetId=" + widgetId);
            tadoInsideTemp = intent.getDoubleExtra("tadoInsideTemp", 0);
            tadoSetPointTemp = intent.getDoubleExtra("tadoSetPointTemp", 0);
            tadoOperation = intent.getStringExtra("tadoOperation");
            tadoControlPhase = intent.getStringExtra("tadoControlPhase");
            tadoIsLoading = intent.getBooleanExtra("tadoIsLoading", false);
        }

        @Override
        public void onCreate() {
            DLog.d(TAG, "onCreate() @TadoWidgetService");
        }

        @Override
        public void onDataSetChanged() {
            // REFRESH DATA
            DLog.d(TAG, "onDataSetChanged() @TadoWidgetService - operation="
                    + tadoOperation + ", insideTemp=" + tadoInsideTemp + ", isLoading=" + tadoIsLoading);

            // SET WIDGET ICON
            switch (tadoOperation.toUpperCase()) {
                case "AWAY":
                    tadoIconId = R.drawable.away_bar_white;
                    break;
                case "HOME":
                    tadoIconId = R.drawable.house_bar_white;
                    break;
                case "SLEEP":
                    tadoIconId = R.drawable.userbed_bar_white;
                    break;
                case "MANUAL":
                    tadoIconId = R.drawable.hand_bar_white;
                    break;
                case "NO_FREEZE":
                    tadoIconId = R.drawable.standby_bar_white;
                    break;
            }
            DLog.d(TAG, "onDataSetChanged @TadoWidgetService - drawable=" + tadoIconId);
        }

        @Override
        public void onDestroy() {
            DLog.d(TAG, "onDestroy() @TadoWidgetService");
        }

        @Override
        public int getCount() {
            DLog.d(TAG, "getCount() @TadoWidgetService - length=" + layoutIds.length);
            return layoutIds.length;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews remoteView = new RemoteViews(context.getPackageName(), layoutIds[position]);
            DLog.d(TAG, "getViewAt() @TadoWidgetService - position="
                    + position + ", remoteView=" + remoteView.toString());

            // FIXME LC 2016-01-29 - LAYOUT REFRESH
            switch (position) {
                case 1:
                    break;

                default:
                    remoteView.setImageViewResource(R.id.imageViewIcon, tadoIconId);

                    // SET WIDGET TEMP (clickable)
                    remoteView.setTextViewText(R.id.textViewInsideTemp, String.format("%.1f", tadoInsideTemp) + "°");

                    // SET POINT TEMP
                    remoteView.setTextViewText(R.id.textViewSetPointTemp, String.format("%.1f", tadoSetPointTemp) + "°");

                    // SET CONTROL PHASE
                    remoteView.setViewVisibility(R.id.imageViewPhase,
                            tadoControlPhase.equalsIgnoreCase("HEATUP") ? View.VISIBLE : View.INVISIBLE);

                    // LAST UPDATE
                    String lastUpdate = context.getString(R.string.label_last_update)
                            + " " + new SimpleDateFormat("dd-MM HH:mm:ss").format(new Date());
                    remoteView.setTextViewText(R.id.textViewLastUpdate, lastUpdate);


                    // CLICK on ICON to OPEN CONFIG ACTIVITY
                    Bundle extras = new Bundle();
                    extras.putInt(TadoWidgetProvider.ACTION_CLICK, R.id.imageViewIcon);
                    Intent fillInIntent = new Intent();
                    fillInIntent.putExtras(extras);
                    remoteView.setOnClickFillInIntent(R.id.imageViewIcon, fillInIntent);
                    DLog.d(TAG, "getViewAt(0) @TadoWidgetService - DONE!");
//                    PendingIntent pendingIntent = TadoWidgetProvider.runConfigActivity(context, widgetId);
//                    remoteView.setOnClickPendingIntent(R.id.imageViewIcon, pendingIntent);
//                    Intent intent = new Intent(TadoWidgetProvider.ACTION_OPEN_CONFIG);
//                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
//                    remoteView.setOnClickFillInIntent(R.id.imageViewIcon, intent);
//                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
/*
                        // CLICK on CONTROL PHASE TEMP to REFRESH WIDGET
                        Intent intent = new Intent(context, TadoWidgetProvider.class);
                        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                        // WIDGET TEMP (clickable)
                        // remoteViews.setOnClickPendingIntent(R.id.textViewInsideTemp, pendingIntent);
                        remoteView.setOnClickPendingIntent(R.id.layoutRefresh, pendingIntent);

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
*/
                    break;
            }
            return remoteView;
        }

        @Override
        public RemoteViews getLoadingView() {
            DLog.d(TAG, "getLoadingView() @TadoWidgetService");
            return null;
//            return new RemoteViews(getPackageName(), R.layout.tado_loading);
        }

        @Override
        public int getViewTypeCount() {
            DLog.d(TAG, "getViewTypeCount() @TadoWidgetService - count=" + layoutIds.length);
            return layoutIds.length;
        }

        @Override
        public long getItemId(int position) {
            DLog.d(TAG, "getItemId() @TadoWidgetService - position=" + position);
            return position;
        }

        @Override
        public boolean hasStableIds() {
            DLog.d(TAG, "hasStableIds() @TadoWidgetService");
            return true;
        }
    }
}
