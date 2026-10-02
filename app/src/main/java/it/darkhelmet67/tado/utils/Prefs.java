package it.darkhelmet67.tado.utils;

import android.content.Context;
import android.preference.PreferenceManager;

public class Prefs {

    public static void setUsername(Context context, String username) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(key.PREFERENCE_KEY_LOGIN_USERNAME, username).commit();
    }

    public static String getUsername(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key.PREFERENCE_KEY_LOGIN_USERNAME, "");
    }

    public static void setPassword(Context context, String password) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(key.PREFERENCE_KEY_LOGIN_PASSWORD, password).commit();
    }

    public static String getPassword(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key.PREFERENCE_KEY_LOGIN_PASSWORD, "");
    }

    public static void setDevicePosition(Context context, int position) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putInt(key.PREFERENCE_KEY_DEVICE_POSITION, position).commit();
    }

    public static int getDevicePosition(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(key.PREFERENCE_KEY_DEVICE_POSITION, -1);
    }

    public static void setDeviceValue(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(key.PREFERENCE_KEY_DEVICE_VALUE, value).commit();
    }

    public static String getDeviceValue(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key.PREFERENCE_KEY_DEVICE_VALUE, "");
    }

    public static void setDeviceUsername(Context context, String username) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(key.PREFERENCE_KEY_DEVICE_USERNAME, username).commit();
    }

    public static String getDeviceUsername(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key.PREFERENCE_KEY_DEVICE_USERNAME, "");
    }

    public static void setDeviceName(Context context, String device) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(key.PREFERENCE_KEY_DEVICE_NAME, device).commit();
    }

    public static String getDeviceName(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key.PREFERENCE_KEY_DEVICE_NAME, "");
    }

    public static void setDevicePassword(Context context, String password) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(key.PREFERENCE_KEY_DEVICE_PASSWORD, password).commit();
    }

    public static String getDevicePassword(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key.PREFERENCE_KEY_DEVICE_PASSWORD, "");
    }

    public static void setUpdatePosition(Context context, int position) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putInt(key.PREFERENCE_KEY_UPDATE_POSITION, position).commit();
    }

    public static int getUpdatePosition(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(key.PREFERENCE_KEY_UPDATE_POSITION, 0);
    }

    public static void setUpdateValue(Context context, int value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putInt(key.PREFERENCE_KEY_UPDATE_VALUE, value).commit();
    }

    public static int getUpdateValue(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(key.PREFERENCE_KEY_UPDATE_VALUE, 0);
    }

    public static void setUpdateDateTime(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(key.PREFERENCE_KEY_UPDATE_DATETIME, value).commit();
    }

    public static String getUpdateDateTime(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key.PREFERENCE_KEY_UPDATE_DATETIME, "");
    }

    public static void setSwitchTextSize(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(key.PREFERENCE_KEY_WIDGET_TEXTSIZE, value).commit();
    }

    public static boolean getSwitchTextSize(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(key.PREFERENCE_KEY_WIDGET_TEXTSIZE, false);
    }

    // LC - KEYS for SHARED PREFRENCES
    private static class key {
        private static final String PREFERENCE_KEY_LOGIN_USERNAME = "PREFERENCE_KEY_LOGIN_USERNAME";
        private static final String PREFERENCE_KEY_LOGIN_PASSWORD = "PREFERENCE_KEY_LOGIN_PASSWORD";
        private static final String PREFERENCE_KEY_DEVICE_POSITION = "PREFERENCE_KEY_DEVICE_POSITION";
        private static final String PREFERENCE_KEY_DEVICE_VALUE = "PREFERENCE_KEY_DEVICE_VALUE";
        private static final String PREFERENCE_KEY_DEVICE_USERNAME = "PREFERENCE_KEY_DEVICE_USERNAME";
        private static final String PREFERENCE_KEY_DEVICE_PASSWORD = "PREFERENCE_KEY_DEVICE_PASSWORD";
        private static final String PREFERENCE_KEY_DEVICE_NAME = "PREFERENCE_KEY_DEVICE_NAME";
        private static final String PREFERENCE_KEY_UPDATE_POSITION = "PREFERENCE_KEY_UPDATE_POSITION";
        private static final String PREFERENCE_KEY_UPDATE_VALUE = "PREFERENCE_KEY_UPDATE_VALUE";
        private static final String PREFERENCE_KEY_UPDATE_DATETIME = "PREFERENCE_KEY_UPDATE_DATETIME";
        private static final String PREFERENCE_KEY_WIDGET_TEXTSIZE = "PREFERENCE_KEY_WIDGET_TEXTSIZE";
    }

}
