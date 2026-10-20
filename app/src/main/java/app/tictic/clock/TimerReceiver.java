package app.tictic.clock;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import app.space.core.domain.TimeMath;

/** Durable timer deadline, monotonic countdown display, and completion notification. */
public class TimerReceiver extends BroadcastReceiver {
    private static final int REQUEST_CODE = 800001;
    private static final String TIMER_URI = "tictic://timer";
    private static final String CHANNEL = "tictic_timer_v199";

    static int bootCount(Context context) {
        if (Build.VERSION.SDK_INT < 24) return -1;
        try { return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1); }
        catch (RuntimeException ignored) { return -1; }
    }

    static long remaining(Context context) {
        android.content.SharedPreferences prefs = DataStore.prefs(context);
        long wallEnd = prefs.getLong("timer_end", 0L);
        if (wallEnd <= 0L) {
            return prefs.getLong("timer_remaining",
                    prefs.getLong("timer_duration", prefs.getLong("timer_default_duration", 300000L)));
        }
        long nowElapsed = SystemClock.elapsedRealtime();
        long nowWall = System.currentTimeMillis();
        int currentBoot = bootCount(context);
        int savedBoot = prefs.getInt("timer_boot_count", -1);
        long elapsedEnd = prefs.getLong("timer_end_elapsed", 0L);
        long left = TimeMath.countdownRemaining(prefs.getLong("timer_remaining", 0L), wallEnd,
                elapsedEnd, nowWall, nowElapsed, savedBoot, currentBoot);
        // Migrate v1.9.8 wall-clock-only timers and re-anchor after a reboot. Keep the
        // RTC wall deadline for AlarmManager; the monotonic deadline drives the display.
        if (left > 0L && (elapsedEnd <= 0L || savedBoot != currentBoot)) {
            prefs.edit().putLong("timer_end_elapsed", nowElapsed + left)
                    .putInt("timer_boot_count", currentBoot).apply();
        }
        return left;
    }

    static boolean isRunning(Context context) {
        return DataStore.prefs(context).getLong("timer_end", 0L) > 0L && remaining(context) > 0L;
    }

    static void start(Context context, long durationMillis) {
        long duration = Math.max(1000L, durationMillis);
        long wallEnd = System.currentTimeMillis() + duration;
        long elapsedEnd = SystemClock.elapsedRealtime() + duration;
        int boot = bootCount(context);
        DataStore.prefs(context).edit().putLong("timer_end", wallEnd)
                .putLong("timer_end_elapsed", elapsedEnd).putInt("timer_boot_count", boot)
                .putLong("timer_remaining", duration).apply();
        cancelAlarm(context);
        scheduleAt(context, wallEnd);
    }

    static void pause(Context context, long remainingMillis) {
        cancelAlarm(context);
        DataStore.prefs(context).edit().putLong("timer_end", 0L)
                .remove("timer_end_elapsed").remove("timer_boot_count")
                .putLong("timer_remaining", Math.max(0L, remainingMillis)).apply();
    }

    static void reset(Context context, long durationMillis) {
        pause(context, durationMillis);
    }

    static void cancelAlarm(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(pending(context));
    }

    static void scheduleRemaining(Context context, long remainingMillis) {
        long left = Math.max(0L, remainingMillis);
        if (left <= 0L) return;
        long wallEnd = System.currentTimeMillis() + left;
        long elapsedEnd = SystemClock.elapsedRealtime() + left;
        int boot = bootCount(context);
        DataStore.prefs(context).edit().putLong("timer_end", wallEnd)
                .putLong("timer_end_elapsed", elapsedEnd).putInt("timer_boot_count", boot)
                .putLong("timer_remaining", left).apply();
        cancelAlarm(context);
        scheduleAt(context, wallEnd);
    }

    private static PendingIntent pending(Context context) {
        Intent intent = new Intent(context, TimerReceiver.class).setData(Uri.parse(TIMER_URI));
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags);
    }

    private static void scheduleAt(Context context, long wallEnd) {
        AlarmScheduler.set(context, wallEnd, pending(context));
    }

    @Override public void onReceive(Context context, Intent intent) {
        long wallEnd = DataStore.prefs(context).getLong("timer_end", 0L);
        if (wallEnd <= 0L) return; // stale alarm after pause/reset
        long left = remaining(context);
        if (left > 0L) { // RTC delivery can be early after a clock change or inexact scheduling.
            scheduleRemaining(context, left);
            return;
        }

        long duration = DataStore.prefs(context).getLong("timer_duration",
                DataStore.prefs(context).getLong("timer_default_duration", 300000L));
        DataStore.prefs(context).edit().putLong("timer_end", 0L).remove("timer_end_elapsed")
                .remove("timer_boot_count").putLong("timer_remaining", duration).apply();
        DataStore.log(context, "Timer", "Timer completed", "Your countdown finished");
        if (!DataStore.prefs(context).getBoolean("timer_alerts_enabled", true)) return;

        boolean vibrate = DataStore.prefs(context).getBoolean("timer_vibrate_enabled", true);
        String channel = CHANNEL + (vibrate ? "_vibrate" : "_quiet");
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && manager != null && manager.getNotificationChannel(channel) == null) {
            NotificationChannel created = new NotificationChannel(channel,
                    vibrate ? "TicTic timers · vibration" : "TicTic timers · quiet",
                    NotificationManager.IMPORTANCE_HIGH);
            created.enableVibration(vibrate);
            if (vibrate) created.setVibrationPattern(new long[]{0, 350, 180, 350});
            manager.createNotificationChannel(created);
        }
        Intent open = new Intent(context, MainActivity.class).putExtra("tab", "timer");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent content = PendingIntent.getActivity(context, 8001, open, flags);
        Notification.Builder notification = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, channel) : new Notification.Builder(context);
        notification.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Timer complete").setContentText("Your TicTic countdown is done")
                .setAutoCancel(true).setContentIntent(content).setPriority(Notification.PRIORITY_HIGH);
        if (Build.VERSION.SDK_INT < 26) {
            if (vibrate) notification.setVibrate(new long[]{0, 350, 180, 350});
            else notification.setVibrate(new long[]{0});
            notification.setDefaults(Notification.DEFAULT_SOUND);
        }
        if (manager != null) manager.notify(8001, notification.build());
    }
}
