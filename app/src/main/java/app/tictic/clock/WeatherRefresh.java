package app.tictic.clock;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

/** Opt-in per-widget weather jobs; Android may defer network-constrained work. */
final class WeatherRefresh {
    private static final int PERIODIC_BASE = 120000;
    private static final int ONE_SHOT_BASE = 150000;
    private WeatherRefresh() {}

    static void configure(Context context, int widgetId, boolean enabled) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return;
        int periodicId = PERIODIC_BASE + (widgetId & 0x3fff);
        int oneShotId = ONE_SHOT_BASE + (widgetId & 0x3fff);
        scheduler.cancel(periodicId);
        scheduler.cancel(oneShotId);
        if (!enabled) return;
        String key = "widget_" + widgetId + "_";
        int minutes = TicTicPrefs.interval(context, key + "weather_interval");
        ComponentName service = new ComponentName(context, WeatherJobService.class);
        long period = minutes * 60_000L;
        JobInfo periodic = new JobInfo.Builder(periodicId, service)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(period)
                .setPersisted(true)
                .build();
        scheduler.schedule(periodic);
        JobInfo immediate = new JobInfo.Builder(oneShotId, service)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(1_000L)
                .setOverrideDeadline(30_000L)
                .build();
        scheduler.schedule(immediate);
    }

    static void cancel(Context context, int widgetId) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) {
            scheduler.cancel(PERIODIC_BASE + (widgetId & 0x3fff));
            scheduler.cancel(ONE_SHOT_BASE + (widgetId & 0x3fff));
        }
    }

    private static final class TicTicPrefs {
        static int interval(Context c, String key) {
            int value = app.tictic.clock.DataStore.prefs(c).getInt(key, 60);
            return value == 30 || value == 120 ? value : 60;
        }
    }
}
