package app.space.feature.calendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;
import java.util.Locale;

/**
 * Rings a calendar event's reminder. The editor stores the offset; this is the half that makes
 * the promise real: an exact alarm fires {@code reminderMinutes} before the event and posts a
 * notification that opens MySpace.
 *
 * <p>Deliberately framework-only (no androidx): the shell still builds with useAndroidX=false,
 * and Sprint 4 owns the migration. Boot rescheduling is wired from the shell's BootReceiver,
 * which may reach in because :app depends on this module -- never the other way round.
 */
public final class EventReminderReceiver extends BroadcastReceiver {

    static final String ACTION = "app.space.feature.calendar.EVENT_REMINDER";
    private static final String CHANNEL = "myspace_calendar_reminders_v1";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return;
        String title = intent.getStringExtra("title");
        long when = intent.getLongExtra("when", 0L);
        String id = intent.getStringExtra("id");
        if (title == null || title.isEmpty()) title = "Calendar event";
        post(context, title, when, id);
    }

    /** Schedules the reminder for one event; a no-op when the moment has passed or none was set. */
    public static void schedule(Context context, String eventId, String title, long when,
                                int reminderMinutes) {
        if (context == null || eventId == null || eventId.isEmpty()) return;
        cancel(context, eventId);
        if (reminderMinutes < 0 || when <= 0) return;
        long fireAt = when - reminderMinutes * 60000L;
        if (fireAt <= System.currentTimeMillis()) return;
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        PendingIntent fire = pending(context, eventId, title, when);
        if (canScheduleExact(alarm)) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, fire);
        else alarm.set(AlarmManager.RTC_WAKEUP, fireAt, fire);
    }

    /** Drops a scheduled reminder; safe to call for events that never had one. */
    public static void cancel(Context context, String eventId) {
        if (context == null || eventId == null || eventId.isEmpty()) return;
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        alarm.cancel(pending(context, eventId, "", 0L));
    }

    /** Re-arms every future reminder after a reboot; called from the shell's BootReceiver. */
    public static void rescheduleAll(Context context) {
        if (context == null) return;
        List<CalendarRules.Event> events = CalendarStore.load(context);
        for (CalendarRules.Event e : events) {
            if (e.reminderMinutes >= 0 && e.when > System.currentTimeMillis()) {
                schedule(context, e.id, e.title, e.when, e.reminderMinutes);
            }
        }
    }

    private static PendingIntent pending(Context context, String eventId, String title, long when) {
        Intent intent = new Intent(context, EventReminderReceiver.class).setAction(ACTION)
                .putExtra("id", eventId)
                .putExtra("title", title).putExtra("when", when);
        return PendingIntent.getBroadcast(context, requestCode(eventId), intent,
                immutable(PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private static int immutable(int flags) {
        return Build.VERSION.SDK_INT >= 23 ? flags | PendingIntent.FLAG_IMMUTABLE : flags;
    }

    private static boolean canScheduleExact(AlarmManager alarm) {
        return Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms();
    }

    private static int requestCode(String eventId) {
        // Stable per event so schedule-after-cancel replaces rather than doubles up.
        return (int) (0x43000000L ^ (eventId.hashCode() & 0x3fffffffL));
    }

    private static void post(Context context, String title, long when, String id) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (manager.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL,
                    "Calendar reminders", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Alerts for MySpace calendar events");
            manager.createNotificationChannel(channel);
        }
        Intent open = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        Notification.Builder builder = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(when > 0
                        ? CalendarRules.timeLabel(when, java.util.TimeZone.getDefault())
                                + " \u00b7 " + dayText(when)
                        : "Starting soon")
                .setCategory(Notification.CATEGORY_REMINDER)
                .setAutoCancel(true);
        if (open != null) {
            builder.setContentIntent(PendingIntent.getActivity(context, 941234,
                    open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), immutable(0)));
        }
        manager.notify(requestCode(id == null || id.isEmpty() ? title + when : id),
                builder.build());
    }

    private static String dayText(long when) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(when);
        return String.format(Locale.getDefault(), "%d %s",
                c.get(java.util.Calendar.DAY_OF_MONTH),
                c.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.SHORT,
                        Locale.getDefault()));
    }
}
