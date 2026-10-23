package app.tictic.clock;
import android.content.*;
public class BootReceiver extends BroadcastReceiver { @Override public void onReceive(Context c,Intent i){DataStore.migrateReminderDefaults(c);AlarmScheduler.rescheduleAll(c);ReminderScheduler.rescheduleAll(c);RoutineReceiver.scheduleAll(c);app.space.feature.calendar.EventReminderReceiver.rescheduleAll(c);WidgetEngine.refreshAll(c);} }
