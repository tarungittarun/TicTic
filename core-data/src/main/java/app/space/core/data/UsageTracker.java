package app.space.core.data;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Optional, local-only usage counters. No battery sampling or external service.
 *
 * <p>Moved from {@code app.tictic.clock} into {@code :core-data} in step 1d-2
 * (SUITE-PLAN.md §6.1). Usage metrics are suite-level, not clock-specific: every
 * MySpace app will report foreground time and action counts through these counters,
 * and the Usage screen aggregates across all of them.
 *
 * <p>It moved in the same slice as {@link AppPrefs} because the two are mutually
 * coupled — {@code AppActivityLog} records an event here, and this class reads its
 * enabled flag and every counter through preferences. Splitting them across slices
 * would have left a cross-module cycle through {@code :app}.
 *
 * <p>Became {@code public} for the module boundary; logic is unchanged. The one
 * clock-era member, {@link #purgeLegacyBatterySnapshots}, is retained because it
 * cleans up removed v1.3 battery preferences and is still called on first launch.
 */
public final class UsageTracker {
    private UsageTracker(){}

    private static boolean enabled(Context c){return AppPrefs.prefs(c).getBoolean("usage_metrics_enabled",true);}

    /** Removes unused v1.3 battery snapshot preferences on first launch after upgrade. */
    public static void purgeLegacyBatterySnapshots(Context c){SharedPreferences p=AppPrefs.prefs(c);if(p.getBoolean("legacy_battery_metrics_removed",false))return;SharedPreferences.Editor e=p.edit();for(String key:p.getAll().keySet())if(key.startsWith("battery_first_")||key.startsWith("battery_last_")||key.equals("battery_last_sample_ms"))e.remove(key);e.putBoolean("legacy_battery_metrics_removed",true).apply();}

    public static String dayKey(long millis){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date(millis));}

    private static String dateAtOffset(int offset){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-offset);return dayKey(c.getTimeInMillis());}

    public static void addForeground(Context c,long totalMs,long clockMs){if(!enabled(c)||totalMs<=0)return;String day=dayKey(System.currentTimeMillis());SharedPreferences p=AppPrefs.prefs(c);p.edit().putLong("usage_ms_"+day,p.getLong("usage_ms_"+day,0)+totalMs).putLong("clock_ms_"+day,p.getLong("clock_ms_"+day,0)+Math.max(0,Math.min(totalMs,clockMs))).apply();}

    public static void recordWidgetRefresh(Context c){if(!enabled(c))return;String day=dayKey(System.currentTimeMillis());SharedPreferences p=AppPrefs.prefs(c);p.edit().putInt("widget_refresh_"+day,p.getInt("widget_refresh_"+day,0)+1).apply();}

    public static void recordEvent(Context c,String type){if(!enabled(c))return;String day=dayKey(System.currentTimeMillis());String key=type==null?"other":type.toLowerCase(Locale.US).replaceAll("[^a-z0-9]","_");SharedPreferences p=AppPrefs.prefs(c);p.edit().putInt("event_count_"+day,p.getInt("event_count_"+day,0)+1).putInt("event_"+key+"_"+day,p.getInt("event_"+key+"_"+day,0)+1).apply();}

    public static long usageMillis(Context c,int days){long total=0;SharedPreferences p=AppPrefs.prefs(c);for(int i=0;i<days;i++)total+=p.getLong("usage_ms_"+dateAtOffset(i),0);return total;}

    public static long clockMillis(Context c,int days){long total=0;SharedPreferences p=AppPrefs.prefs(c);for(int i=0;i<days;i++)total+=p.getLong("clock_ms_"+dateAtOffset(i),0);return total;}

    public static int widgetRefreshes(Context c,int days){int total=0;SharedPreferences p=AppPrefs.prefs(c);for(int i=0;i<days;i++)total+=p.getInt("widget_refresh_"+dateAtOffset(i),0);return total;}

    public static int eventCount(Context c,int days){int total=0;SharedPreferences p=AppPrefs.prefs(c);for(int i=0;i<days;i++)total+=p.getInt("event_count_"+dateAtOffset(i),0);return total;}

    public static int eventCount(Context c,int days,String type){int total=0;String key=type.toLowerCase(Locale.US).replaceAll("[^a-z0-9]","_");SharedPreferences p=AppPrefs.prefs(c);for(int i=0;i<days;i++)total+=p.getInt("event_"+key+"_"+dateAtOffset(i),0);return total;}

    public static String formatDuration(long ms){long mins=Math.max(0,ms/60000);long hours=mins/60;long rem=mins%60;if(hours>0)return hours+"h "+rem+"m";if(mins>0)return mins+" min";return "<1 min";}
}
