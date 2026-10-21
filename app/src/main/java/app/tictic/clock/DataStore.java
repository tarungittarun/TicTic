package app.tictic.clock;

import android.content.Context;
import android.content.SharedPreferences;
import app.space.core.data.AppActivityLog;
import app.space.core.data.AppFormat;
import app.space.core.data.AppPrefs;
import app.space.core.data.AppTheme;
import app.space.core.data.UsageTracker;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;

final class DataStore {
    /**
     * References {@link AppPrefs#PREF} rather than repeating the literal, so the file
     * name has one source of truth across modules. Kept because 135 external call sites
     * read preferences through {@code DataStore.prefs(...)} and the migrations below use
     * it directly.
     */
    static final String PREF = AppPrefs.PREF;
    static SharedPreferences prefs(Context c) { return AppPrefs.prefs(c); }
    /** Migrate the former shared action to independent keys: Control volume becomes Snooze; Turn off becomes Stop. Values: 1=Snooze, 2=Stop. */
    static void migrateRingingControls(Context c) {
        SharedPreferences p=prefs(c);if(p.getBoolean("ringing_controls_v111_migrated",false))return;
        int old=p.getInt("alarm_volume_buttons",1);int action=(old==2||old==3)?2:1;SharedPreferences.Editor e=p.edit();
        if(!p.contains("alarm_volume_up_action"))e.putInt("alarm_volume_up_action",action);
        if(!p.contains("alarm_volume_down_action"))e.putInt("alarm_volume_down_action",action);
        if(!p.contains("alarm_ringing_actions"))e.putInt("alarm_ringing_actions",0);
        e.putBoolean("ringing_controls_v111_migrated",true).apply();
    }
    static int alarmVolumeAction(Context c,boolean volumeUp) {
        migrateRingingControls(c);SharedPreferences p=prefs(c);String key=volumeUp?"alarm_volume_up_action":"alarm_volume_down_action";int value=p.getInt(key,1);return value==2?2:1;
    }
    /** Compatibility accessor: the Volume Up choice. */
    static int alarmVolumeAction(Context c){return alarmVolumeAction(c,true);}
    static boolean showRingingSnoozeAction(Context c){migrateRingingControls(c);return prefs(c).getInt("alarm_ringing_actions",0)==0;}
    static JSONArray array(Context c, String key) { return AppPrefs.array(c, key); }
    static void putArray(Context c, String key, JSONArray a) { AppPrefs.putArray(c, key, a); }
    /** v1.9.2 one-time migration: old app defaults were 10 min / alarm tone. New default is a 30-minute notification ping. */
    static void migrateReminderDefaults(Context c) {
        SharedPreferences p=prefs(c);if(p.getBoolean("reminder_defaults_v192_migrated",false))return;
        JSONArray alarms=array(c,"alarms");boolean changed=false;
        for(int i=0;i<alarms.length();i++){JSONObject alarm=alarms.optJSONObject(i);if(alarm==null)continue;try{
            if(!alarm.has("reminder_minutes")||alarm.optInt("reminder_minutes",10)==10){alarm.put("reminder_minutes",30);changed=true;}
            String sound=alarm.optString("reminder_sound","");if(sound.isEmpty()||"same".equals(sound)){alarm.put("reminder_sound","ping");changed=true;}
        }catch(Exception ignored){}}
        if(changed)putArray(c,"alarms",alarms);p.edit().putBoolean("reminder_defaults_v192_migrated",true).apply();
    }
    static JSONObject find(Context c, String key, long id) { return AppPrefs.find(c, key, id); }
    static void log(Context c, String type, String title, String detail) { AppActivityLog.log(c, type, title, detail); }
    static String formatTime(Context c, int hour, int minute) { return AppFormat.formatTime(c, hour, minute); }
    static String formatDate(long millis) { return AppFormat.formatDate(millis); }
    static long nextOccurrence(JSONObject alarm, long from) {
        Calendar now=Calendar.getInstance(); now.setTimeInMillis(from);
        int hr=alarm.optInt("hour",7), mn=alarm.optInt("minute",0), days=alarm.optInt("days",0);
        if(days==0) {
            long savedDate=alarm.optLong("date_millis",0);
            Calendar candidate=(Calendar)now.clone();
            if(savedDate>0){Calendar selected=(Calendar)now.clone();selected.setTimeInMillis(savedDate);candidate.set(Calendar.YEAR,selected.get(Calendar.YEAR));candidate.set(Calendar.MONTH,selected.get(Calendar.MONTH));candidate.set(Calendar.DAY_OF_MONTH,selected.get(Calendar.DAY_OF_MONTH));}
            candidate.set(Calendar.HOUR_OF_DAY,hr); candidate.set(Calendar.MINUTE,mn); candidate.set(Calendar.SECOND,0); candidate.set(Calendar.MILLISECOND,0);
            if(candidate.getTimeInMillis()<=from) candidate.add(Calendar.DAY_OF_YEAR,1);
            return candidate.getTimeInMillis();
        }
        for(int add=0;add<8;add++) {
            Calendar candidate=(Calendar)now.clone(); candidate.add(Calendar.DAY_OF_YEAR,add); candidate.set(Calendar.HOUR_OF_DAY,hr); candidate.set(Calendar.MINUTE,mn); candidate.set(Calendar.SECOND,0); candidate.set(Calendar.MILLISECOND,0);
            int bit=candidate.get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY;
            if((days & (1<<bit))!=0 && candidate.getTimeInMillis()>from) return candidate.getTimeInMillis();
        }
        return from+86400000L;
    }
    static String daysLabel(int mask) {
        if(mask==0) return "Once";
        if(mask==127) return "Every day";
        String[] names={"S","M","T","W","T","F","S"}; StringBuilder b=new StringBuilder();
        for(int i=0;i<7;i++) if((mask&(1<<i))!=0) { if(b.length()>0)b.append(" · "); b.append(names[i]); }
        return b.toString();
    }
    static String nextAlarmText(Context c) {
        JSONArray a=array(c,"alarms"); JSONObject best=null; long bestAt=Long.MAX_VALUE;
        for(int i=0;i<a.length();i++){ JSONObject o=a.optJSONObject(i); if(o!=null&&o.optBoolean("enabled",true)){long t=nextOccurrence(o,System.currentTimeMillis());if(t<bestAt){bestAt=t;best=o;}} }
        if(best==null)return "No alarm set";
        return formatTime(c,best.optInt("hour"),best.optInt("minute"))+"  ·  "+formatDate(bestAt);
    }
    static int themeMode(Context c) { return AppTheme.themeMode(c); } // 0 system, 1 light, 2 dark
    static boolean dark(Context c) { return AppTheme.dark(c); }
    /** @deprecated read the palette from {@link AppTheme#ACCENT_COLORS}; kept so existing call sites compile during the 1d extraction. */
    static final int[] ACCENT_COLORS=AppTheme.ACCENT_COLORS;
    static int accent(Context c) { return AppTheme.accent(c); }
}
