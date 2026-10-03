package it.darkhelmet67.tado.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import it.darkhelmet67.tado.api.NetworkLog;

/**
 * App-wide log written to {@code Android/data/<package>/files/logs/tado-widget.log} (about 256 KB x 3
 * files, oldest rotated out). Contains request paths, status codes, timings and app events, never
 * tokens or passwords. The settings screen can share it.
 */
public final class AppLog {
    private static final String TAG = "TadoWidget";
    private static final String FILE_NAME = "tado-widget.log";
    private static final String EXPORT_NAME = "tado-widget-log-export.txt";
    private static final long MAX_BYTES = 256 * 1024;
    private static final int FILES = 3;

    private static volatile RotatingFileLog file;
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private AppLog() {
    }

    public static synchronized void init(Context context) {
        if (file != null) return;
        Context app = context.getApplicationContext();
        File base = app.getExternalFilesDir(null);
        if (base == null) base = app.getFilesDir();
        file = new RotatingFileLog(new File(base, "logs"), FILE_NAME, MAX_BYTES, FILES);
        d("APP", "Log started");
    }

    public static void d(String tag, String message) {
        Log.d(TAG, tag + ": " + message);
        RotatingFileLog f = file;
        if (f == null) return;
        String time;
        synchronized (FORMAT) {
            time = FORMAT.format(new Date());
        }
        f.append(time + " " + tag + " " + message);
    }

    public static NetworkLog network() {
        return line -> d("NET", line);
    }

    public static String location() {
        RotatingFileLog f = file;
        return f == null ? "" : f.directory().getAbsolutePath();
    }

    /** Combines the rotated files into one shareable text file. */
    public static File export() throws IOException {
        RotatingFileLog f = file;
        if (f == null) throw new IOException("Log not initialised");
        return f.exportTo(new File(f.directory(), EXPORT_NAME));
    }
}
