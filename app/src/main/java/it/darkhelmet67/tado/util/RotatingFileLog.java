package it.darkhelmet67.tado.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A plain text log that never grows without bound: when the current file would exceed
 * {@code maxBytes} it becomes {@code name.1}, the old {@code name.1} becomes {@code name.2}, and so on;
 * at most {@code files} files exist. Pure Java so it can be unit-tested.
 */
public final class RotatingFileLog {
    private final File dir;
    private final String name;
    private final long maxBytes;
    private final int files;

    public RotatingFileLog(File dir, String name, long maxBytes, int files) {
        this.dir = dir;
        this.name = name;
        this.maxBytes = maxBytes;
        this.files = Math.max(files, 1);
    }

    public synchronized void append(String line) {
        byte[] bytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) return;
            File current = new File(dir, name);
            if (current.length() > 0 && current.length() + bytes.length > maxBytes) rotate();
            try (OutputStream out = new FileOutputStream(current, true)) {
                out.write(bytes);
            }
        } catch (IOException | RuntimeException ignored) {
            // Logging must never crash the app.
        }
    }

    private void rotate() {
        new File(dir, name + "." + (files - 1)).delete();
        for (int i = files - 2; i >= 1; i--) {
            new File(dir, name + "." + i).renameTo(new File(dir, name + "." + (i + 1)));
        }
        if (files > 1) new File(dir, name).renameTo(new File(dir, name + ".1"));
        else new File(dir, name).delete();
    }

    /** Writes all log files, oldest first, to {@code target}. */
    public synchronized File exportTo(File target) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create " + dir);
        try (OutputStream out = new FileOutputStream(target, false)) {
            for (int i = files - 1; i >= 0; i--) {
                File f = new File(dir, i == 0 ? name : name + "." + i);
                if (!f.isFile()) continue;
                try (InputStream in = new FileInputStream(f)) {
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
                }
            }
        }
        return target;
    }

    public File directory() {
        return dir;
    }
}
