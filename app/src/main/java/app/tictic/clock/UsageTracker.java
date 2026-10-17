package app.tictic.clock;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Optional, local-only usage counters. No battery sampling or external service. */
final class UsageTracker {
    private UsageTracker(){}
    private static boolean enabled(Context c){return DataStore.prefs(c).getBoolean("usage_metrics_enabled",true);}
    static void purgeLegacyBatterySnapshots(Context c){SharedPreferences p=DataStore.prefs(c);if(p.getBoolean("legacy_battery_metrics_removed",false))return;SharedPreferences.Editor e=p.edit();for(String key:p.getAll().keySet())if(key.startsWith("battery_first_")||key.startsWith("battery_last_")||key.equals("battery_last_sample_ms"))e.remove(key);e.putBoolean("legacy_battery_metrics_removed",true).apply();}
    static String dayKey(long millis){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date(millis));}
    private static String dateAtOffset(int offset){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-offset);return dayKey(c.getTimeInMillis());}
    static void addForeground(Context c,long totalMs,long clockMs){if(!enabled(c)||totalMs<=0)return;String day=dayKey(System.currentTimeMillis());SharedPreferences p=DataStore.prefs(c);p.edit().putLong("usage_ms_"+day,p.getLong("usage_ms_"+day,0)+totalMs).putLong("clock_ms_"+day,p.getLong("clock_ms_"+day,0)+Math.max(0,Math.min(totalMs,clockMs))).apply();}
    static void recordWidgetRefresh(Context c){if(!enabled(c))return;String day=dayKey(System.currentTimeMillis());SharedPreferences p=DataStore.prefs(c);p.edit().putInt("widget_refresh_"+day,p.getInt("widget_refresh_"+day,0)+1).apply();}
    static void recordEvent(Context c,String type){if(!enabled(c))return;String day=dayKey(System.currentTimeMillis());String key=type==null?"other":type.toLowerCase(Locale.US).replaceAll("[^a-z0-9]","_");SharedPreferences p=DataStore.prefs(c);p.edit().putInt("event_count_"+day,p.getInt("event_count_"+day,0)+1).putInt("event_"+key+"_"+day,p.getInt("event_"+key+"_"+day,0)+1).apply();}
    static long usageMillis(Context c,int days){long total=0;SharedPreferences p=DataStore.prefs(c);for(int i=0;i<days;i++)total+=p.getLong("usage_ms_"+dateAtOffset(i),0);return total;}
    static long clockMillis(Context c,int days){long total=0;SharedPreferences p=DataStore.prefs(c);for(int i=0;i<days;i++)total+=p.getLong("clock_ms_"+dateAtOffset(i),0);return total;}
    static int widgetRefreshes(Context c,int days){int total=0;SharedPreferences p=DataStore.prefs(c);for(int i=0;i<days;i++)total+=p.getInt("widget_refresh_"+dateAtOffset(i),0);return total;}
    static int eventCount(Context c,int days){int total=0;SharedPreferences p=DataStore.prefs(c);for(int i=0;i<days;i++)total+=p.getInt("event_count_"+dateAtOffset(i),0);return total;}
    static int eventCount(Context c,int days,String type){int total=0;String key=type.toLowerCase(Locale.US).replaceAll("[^a-z0-9]","_");SharedPreferences p=DataStore.prefs(c);for(int i=0;i<days;i++)total+=p.getInt("event_"+key+"_"+dateAtOffset(i),0);return total;}
    static String formatDuration(long ms){long mins=Math.max(0,ms/60000);long hours=mins/60;long rem=mins%60;if(hours>0)return hours+"h "+rem+"m";if(mins>0)return mins+" min";return "<1 min";}
}
