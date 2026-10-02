package it.darkhelmet67.tado;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

import it.darkhelmet67.tado.utils.Prefs;

public class AppWidgetAlarm {

    private static final String TAG = AppWidgetAlarm.class.getSimpleName();
    private final int ALARM_ID = 0;
    private int INTERVAL_MILLIS = 1 * 60 * 1000;   // DEFAULT = 1 minute

    private Context mContext;

    public AppWidgetAlarm(Context context) {
        mContext = context;
        // SET INTERVAL from PREFS
        INTERVAL_MILLIS = Prefs.getUpdateValue(context) * 60 * 1000;
        Log.d(TAG, "init @AppWidgetAlarm - INTERVAL_MILLIS=" + INTERVAL_MILLIS);
    }

    public void startAlarm() {

        if (INTERVAL_MILLIS > 0) {
            Log.d(TAG, "startAlarm @AppWidgetAlarm - INTERVAL_MILLIS=" + INTERVAL_MILLIS);

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MILLISECOND, INTERVAL_MILLIS);

            Intent alarmIntent = new Intent(TadoWidgetProvider.ACTION_AUTO_UPDATE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, ALARM_ID, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            // RTC does not wake the device up
            alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), INTERVAL_MILLIS, pendingIntent);
        }
    }

    public void stopAlarm() {
        Log.d(TAG, "stopAlarm @AppWidgetAlarm");

        Intent alarmIntent = new Intent(TadoWidgetProvider.ACTION_AUTO_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, ALARM_ID, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }
}
