package it.darkhelmet67.tado;

import android.app.Application;

import it.darkhelmet67.tado.util.AppLog;

public class TadoApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.init(this);
    }
}
