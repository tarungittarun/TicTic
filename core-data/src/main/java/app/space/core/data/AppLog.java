package app.space.core.data;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The diagnostics log: everything the app does, while the owner has it switched on.
 *
 * <p>Added because two consecutive Play Protect blocks and one settings crash were diagnosed
 * from screenshots alone. A screenshot shows one frame; the log shows the sequence that produced
 * it, including the stack trace of anything that died. Crashes are written even when logging is
 * off, because a crash is exactly the moment nobody thought to enable logging.
 *
 * <p>The file lives under {@code files/logs/}, so the Files app can see it and it travels with
 * the rest of app-private storage. It rotates at one megabyte, keeping one previous generation,
 * so an enabled log cannot grow without bound on a device nobody is watching.
 */
public final class AppLog {

    private static final String KEY_ENABLED = "log_enabled";
    private static final int MAX_BYTES = 1000000;
    private static final int MAX_STACK_LINES = 25;

    private AppLog() { }

    public static boolean enabled(Context c) {
        return c != null && AppPrefs.prefs(c).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context c, boolean on) {
        if (c != null) AppPrefs.prefs(c).edit().putBoolean(KEY_ENABLED, on).apply();
    }

    public static File file(Context c) {
        return new File(new File(c.getFilesDir(), "logs"), "myspace.log");
    }

    /** One timestamped line. No-op unless logging is enabled. */
    public static void event(Context c, String tag, String message) {
        // Round 33: mirror every outcome line to logcat. On a phone this changes
        // nothing he can see; on CI it is the only way the app's own transcript
        // survives a run, because the instrumented log carries only the test
        // framework's stdout.
        try {
            android.util.Log.i("TicTicCalc", tag + " " + message);
        } catch (Throwable ignored) {
            // logging must never be able to break the action it records
        }
        if (!enabled(c)) return;
        queue(c, tag + " " + message);
    }

    /**
     * A stack trace, written even when logging is off.
     *
     * <p>Deliberately not gated on {@link #enabled}: the failure worth recording most is the one
     * that happened before anyone thought to turn the log on.
     */
    public static void crash(Context c, Throwable t) {
        if (c == null || t == null) return;
        StringBuilder b = new StringBuilder();
        b.append("CRASH ").append(t.getClass().getName())
                .append(": ").append(String.valueOf(t.getMessage()));
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length && i < MAX_STACK_LINES; i++) {
            b.append("\n  at ").append(st[i]);
        }
        queue(c, b.toString());
    }

    public static void clear(Context c) {
        File f = file(c);
        if (f.exists()) f.delete();
        File old = new File(f.getParentFile(), "myspace.log.1");
        if (old.exists()) old.delete();
    }

    /** The most recent text, for sharing. Capped so an ACTION_SEND text extra stays sane. */
    public static String tail(Context c, int maxChars) {
        try {
            File f = file(c);
            if (!f.exists()) return "";
            String s = new String(readAll(f), "UTF-8");
            return s.length() <= maxChars ? s : s.substring(s.length() - maxChars);
        } catch (Exception e) {
            return "";
        }
    }

    private static byte[] readAll(File f) throws Exception {
        FileInputStream in = new FileInputStream(f);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        in.close();
        return bos.toByteArray();
    }

    // Round 16: "still the app feels little laggy or when pressed buttons on and on."
    // Every key press logs a line, and write() opened, appended and closed a file on
    // the CALLING thread -- the UI thread, twelve times a second under a fast thumb.
    // Lines now queue to one background writer; the UI thread only formats a string.
    private static android.os.HandlerThread writerThread;
    private static android.os.Handler writer;

    private static synchronized void queue(Context c, String line) {
        if (writerThread == null) {
            writerThread = new android.os.HandlerThread("AppLog");
            writerThread.start();
            writer = new android.os.Handler(writerThread.getLooper());
        }
        final Context app = c.getApplicationContext();
        final String text = line;
        writer.post(new Runnable() {
            @Override public void run() { write(app, text); }
        });
    }

    private static synchronized void write(Context c, String line) {
        try {
            File f = file(c);
            File dir = f.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            if (f.exists() && f.length() > MAX_BYTES) {
                File old = new File(dir, "myspace.log.1");
                if (old.exists()) old.delete();
                f.renameTo(old);
            }
            String stamp = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            OutputStream out = new FileOutputStream(f, true);
            out.write((stamp + " " + line + "\n").getBytes("UTF-8"));
            out.close();
        } catch (Exception ignored) {
            // A diagnostics facility that can crash the app it diagnoses is worse than none.
        }
    }
}
