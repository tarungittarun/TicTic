package app.tictic.clock;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

/** Short, explicitly bounded audition of a selected alarm or reminder tone. */
final class TonePreview {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Ringtone ringtone;
    private final Runnable stopTask = this::stop;

    boolean play(Context context, Uri uri, int usage, long maxMillis) {
        stop();
        if (uri == null) return false;
        try {
            Context app = context.getApplicationContext();
            Ringtone next = RingtoneManager.getRingtone(app, uri);
            if (next == null) return false;
            if (Build.VERSION.SDK_INT >= 21) {
                next.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            }
            ringtone = next;
            next.play();
            long duration = Math.max(1000L, Math.min(8000L, maxMillis));
            handler.postDelayed(stopTask, duration);
            return true;
        } catch (RuntimeException failure) {
            stop();
            return false;
        }
    }

    void stop() {
        handler.removeCallbacks(stopTask);
        Ringtone active = ringtone;
        ringtone = null;
        if (active != null) {
            try { if (active.isPlaying()) active.stop(); }
            catch (RuntimeException ignored) { }
        }
    }
}
