package it.darkhelmet67.tado.utils;

import android.util.Log;

public final class DLog {

    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;

    public static boolean isDebugEnabled = true;
    public static boolean isDebugEnabled() {
        return isDebugEnabled;
    }
    public static void setIsDebugEnabled(boolean isDebugEnabled) {
        DLog.isDebugEnabled = isDebugEnabled;
    }

    public static void d(String tag, String msg) {
        if(isDebugEnabled)
            Log.d(tag, msg);
    }
    public static void d(String tag, String msg, Throwable tr) {
        if(isDebugEnabled)
            Log.d(tag, msg, tr);
    }

    public static void v(String tag, String msg) {
        Log.v(tag, msg);
    }
    public static void v(String tag, String msg, Throwable tr) {
        Log.v(tag, msg, tr);
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
    }
    public static void i(String tag, String msg, Throwable tr) {
        Log.i(tag, msg, tr);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
    }
    public static void w(String tag, String msg, Throwable tr) {
        Log.w(tag, msg, tr);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }
    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
    }
}